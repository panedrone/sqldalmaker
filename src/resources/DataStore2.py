from mysql import connector


class OutParam:
    value = None


class DataStore:
    """
    SQL DAL Maker Website: http://sqldalmaker.sourceforge.net
    This is an example of how to implement DataStore in Python + MySQL.
    Recent version: https://github.com/panedrone/sqldalmaker/blob/master/src/resources/DataStore2.py
    Copy-paste this code to your project and change it for your needs.
    Improvements are welcome: sqldalmaker@gmail.com
    """

    def __init__(self):
        self.connection = None

    def open(self):
        self.connection = connector.Connect(user='root', password='root',
                                            host='127.0.0.1',
                                            database='sakila')

    def close(self):
        if self.connection:
            self.connection.close()

    def start_transaction(self):
        self.connection.start_transaction()

    def commit(self):
        self.connection.commit()

    def rollback(self):
        self.connection.rollback()

    def insert_row(self, sql, params, ai_values):
        """
        Returns:
            Nothing.
        Arguments:
            sql (string): SQL statement.
            params (array, optional): Values of SQL parameters.
            ai_values (array, optional): Array like [["o_id", 1], ...] for auto-increment values.
        Raises:
            Exception: if no rows inserted.
        """
        sql = _prepare_sql(sql)
        with self.connection.cursor() as cursor:
            cursor.execute(sql, params)
            if len(ai_values) > 0:
                ai_values[0][1] = cursor.lastrowid
            if cursor.rowcount == 0:
                raise Exception('No rows inserted')

    def exec_dml(self, sql, params):
        """
        Arguments:
            sql (string): SQL statement.
            params (array, optional): Values of SQL parameters.
        Returns:
            Number of updated rows.
        """
        sql = _prepare_sql(sql)
        sp = _get_sp_name(sql)
        with self.connection.cursor() as cursor:
            if sp is None:
                cursor.execute(sql, params)
                return cursor.rowcount
            else:
                call_params = _get_call_params(params)
                # result_args: https://pynative.com/python-mysql-execute-stored-procedure/
                result_args = cursor.callproc(sp, call_params)
                for p in params:
                    if isinstance(p, list) and callable(p[0]):
                        i = 0 # MySQL SP returning result-sets
                        # http://www.mysqltutorial.org/calling-mysql-stored-procedures-python/
                        for result in cursor.stored_results():
                            cb = p[i]
                            _fetch_all(result, cb)
                            i += 1
                        break
                _assign_out_params(params, result_args)
                return 0

    def query_scalar(self, sql, params):
        """
        Returns:
            Single scalar value.
        Arguments:
            sql (string): SQL statement.
            params (array, optional): Values of SQL parameters if needed.
        Raises:
            Exception: if amount of rows != 1.
        """
        rows = self.query_scalar_array(sql, params)
        if len(rows) == 0:
            raise Exception('No rows')
        if len(rows) > 1:
            raise Exception('More than 1 row exists')
        if isinstance(rows[0], list):
            return rows[0][0]
        else:
            return rows[0]  # 'select get_test_rating(?)' returns just scalar value, not array of arrays

    def query_scalar_array(self, sql, params):
        """
        Returns:
            array of scalar values
        Arguments:
            sql (string): SQL statement.
            params (array, optional): Values of SQL parameters if needed.
        """
        sql = _prepare_sql(sql)
        res = []
        sp = _get_sp_name(sql)
        with self.connection.cursor() as cursor:
            if sp is None:
                cursor.execute(sql, params)
                # fetchone() in here throws 'Unread result found' in cursor.close()
                # fetchall() in here because it may break on None value in the first element of array (DBNull)
                for row in cursor:  # .fetchall():
                    res.append(row[0])
            else:
                call_params = _get_call_params(params)
                # result_args: https://pynative.com/python-mysql-execute-stored-procedure/
                result_args = cursor.callproc(sp, call_params)
                # http://www.mysqltutorial.org/calling-mysql-stored-procedures-python/
                for result in cursor.stored_results():
                    # fetchall() in here because it may break on None value in the first element of array (DBNull)
                    for row_values in result:  # .fetchall():
                        res.append(row_values[0])
                _assign_out_params(params, result_args)
        return res

    def query_single_row(self, sql, params):
        """
        Returns:
            Single row
        Arguments:
            sql (string): SQL statement.
            params (array, optional): Values of SQL parameters if needed.
        Raises:
            Exception: if amount of rows != 1.
        """
        rows = []

        def callback(row):
            rows.append(row)
        self.query_all_rows(sql, params, callback)
        if len(rows) == 0:
            raise Exception('No rows')
        if len(rows) > 1:
            raise Exception('More than 1 row exists')
        return rows[0]

    def query_all_rows(self, sql, params, callback):
        """
        Returns:
            None
        Arguments:
            sql (string): SQL statement.
            params (array, optional): Values of SQL parameters if needed.
            callback
        """
        sql = _prepare_sql(sql)
        sp = _get_sp_name(sql)
        # How to retrieve SQL result column value using column name in Python?
        # https://stackoverflow.com/questions/10195139/how-to-retrieve-sql-result-column-value-using-column-name-in-python
        with self.connection.cursor(dictionary=True) as cursor:
            if sp is None:
                cursor.execute(sql, params)
                # fetchone() in here throws 'Undead data' in cursor.close()
                for row in cursor:  # .fetchall():
                    callback(row)
            else:
                call_params = _get_call_params(params)
                # result_args: https://pynative.com/python-mysql-execute-stored-procedure/
                result_args = cursor.callproc(sp, call_params)
                # http://www.mysqltutorial.org/calling-mysql-stored-procedures-python/
                for result in cursor.stored_results():
                    _fetch_all(result, callback)
                _assign_out_params(params, result_args)


def _fetch_all(result, callback):
    # cursor(dictionary=True) does not help in here. workarounds:
    # https://stackoverflow.com/questions/34030020/mysql-python-connector-get-columns-names-from-select-statement-in-stored-procedu
    # https://kadler.github.io/2018/01/08/fetching-python-database-cursors-by-column-name.html#
    for row_values in result:  # .fetchall():
        row = {}
        i = 0
        for d in result.description:
            col_name = d[0]
            value = row_values[i]
            row[col_name] = value
            i = i + 1
        callback(row)


def _prepare_sql(sql):
    """
    @rtype : str
    """
    return sql.replace("?", "%s")


def _get_sp_name(sql):
    """
    @rtype : str
    """
    parts = sql.split()
    if len(parts) >= 2 and parts[0].strip().lower() == "call":
        name = parts[1]
        end = name.find("(")
        if end == -1:
            return name
        else:
            return name[0:end]
    return None


def _get_call_params(params):
    """
    COMMENT FROM SOURCES OF MySQL Connector => cursor.py:

    For OUT and INOUT parameters the user should provide the
    type of the parameter as well. The argument should be a
    tuple with first item as the value of the parameter to pass
    and second argument the type of the argument.
    """
    call_params = []
    for p in params:
        if isinstance(p, OutParam):
            call_params.append(p.value)
        elif isinstance(p, list) and callable(p[0]):
            pass # MySQL SP returning result-sets
        else:
            call_params.append(p)
    return call_params


def _assign_out_params(params, result_args):
    for i in range(len(params)):
        if isinstance(params[i], OutParam):
            params[i].value = result_args[i]
