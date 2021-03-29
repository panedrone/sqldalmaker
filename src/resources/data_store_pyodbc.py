import pyodbc


class DataStore:
    """
    SQL DAL Maker Website: http://sqldalmaker.sourceforge.net
    This is an example of how to implement DataStore in Python + pyodbc + SQL Server.
    Recent version: https://github.com/panedrone/sqldalmaker/blob/master/src/resources/data_store_pyodbc.py
    Copy-paste this code to your project and change it for your needs.
    Improvements are welcome: sqldalmaker@gmail.com
    """
    def __init__(self):
        self.conn = None

    def open(self) -> None:
        # self.conn = pyodbc.connect('DRIVER={CData ODBC Driver for PostgreSQL};'
        #                            'User=postgres;Password=sa;Database=test;Server=127.0.0.1;Port=5432')

        # self.conn = pyodbc.connect('DRIVER={PostgreSQL Unicode};'
        #                            'UID=postgres;PWD=sa;DATABASE=test;Server=127.0.0.1;Port=5432')

        # self.conn = pyodbc.connect('Driver={SQL Server};Server=localhost\\SQLEXPRESS;'
        #                            'Database=AdventureWorks2014;Trusted_Connection=yes;')

        self.conn = pyodbc.connect('Driver={SQL Server Native Client 11.0};Server=localhost\\SQLEXPRESS;'
                                   'Database=AdventureWorks2014;Trusted_Connection=yes;')

    def close(self) -> None:
        if self.conn:
            self.conn.close()
            self.conn = None

    def start_transaction(self) -> None:
        self.conn.begin()

    def commit(self) -> None:
        self.conn.commit()

    def rollback(self) -> None:
        self.conn.rollback()

    def insert_row(self, sql, params, ai_values) -> None:
        """
        Arguments:
            sql: SQL statement
            params: the values of SQL parameters
            ai_values: Array like [["o_id", 1], ...] for auto-increment values
        Returns:
            None
        Raises:
            Exception: if no rows inserted.
        """
        with self.conn.cursor() as cursor:
            cursor.execute(sql, params)
            if len(ai_values) > 0:
                # https://www.reddit.com/r/learnpython/comments/1h78gi/pyodbc_get_last_inserted_id/
                ai_values[0][1] = cursor.execute('SELECT @@IDENTITY AS id;').fetchone()[0]
            if cursor.rowcount == 0:
                raise Exception('No rows inserted')

    def exec_dml(self, sql, in_params) -> int:
        """
        Arguments:
            sql: SQL statement
            in_params: the values of SQL parameters
        Returns:
            The number of updated rows
        """
        sp_sql = _get_sp_sql(sql, in_params)
        if sp_sql is not None:
            sql = sp_sql
        with self.conn.cursor() as cursor:
            cursor.execute(sql, in_params)
            return cursor.rowcount

    def query_scalar(self, sql, in_params) -> object:
        """
        Arguments:
            sql: SQL statement
            in_params: the values of SQL parameters if needed
        Returns:
            single scalar value
        Raises:
            Exception: if amount of rows != 1
        """
        rows = self.query_scalar_array(sql, in_params)
        if len(rows) == 0:
            raise Exception('No rows')
        if len(rows) > 1:
            raise Exception('More than 1 row exists')
        if isinstance(rows[0], list):
            return rows[0][0]
        else:
            # 'select get_test_rating(?)' returns just scalar value, not array of arrays
            return rows[0]

    # -> "list":
    # https://www.geeksforgeeks.org/function-annotations-python/

    def query_scalar_array(self, sql, in_params) -> "list":  # -> typing.List[object]:
        """
        Arguments:
            sql: SQL statement.
            in_params: the values of SQL parameters if needed
        Returns:
            array of scalar values
        """
        sp_sql = _get_sp_sql(sql, in_params)
        if sp_sql is not None:
            sql = sp_sql
        res = []
        with self.conn.cursor() as cursor:
            cursor.execute(sql, in_params)
            columns = [column[0] for column in cursor.description]
            rows = cursor.fetchall()
            while rows:
                for r in rows:
                    for ci in range(len(columns)):
                        if ci == 0:
                            res.append(r[0])
                if cursor.nextset():
                    rows = cursor.fetchall()
                else:
                    rows = None
        return res

    # -> "dict":
    # https://www.geeksforgeeks.org/function-annotations-python/

    def query_single_row(self, sql, in_params) -> "dict":  # -> typing.Dict[str, object]:
        """
        Arguments:
            sql: SQL statement
            in_params: the values of SQL parameters if needed
        Returns:
            single row
        Raises:
            Exception if amount of rows != 1
        """
        rows = []
        self.query_all_rows(sql, in_params, lambda row: rows.append(row))
        if len(rows) == 0:
            raise Exception('No rows')  # in 'find' mode, use query_all_rows instead
        if len(rows) > 1:
            raise Exception('More than 1 row exists')
        return rows[0]

    def query_all_rows(self, sql, in_params, callback) -> None:
        """
        Arguments:
            sql: SQL statement
            in_params: the values of SQL parameters if needed
            callback: a function to process fetched rows
        Returns:
            None
        """
        sp_sql = _get_sp_sql(sql, in_params)
        if sp_sql is not None:
            sql = sp_sql
        with self.conn.cursor() as cursor:
            cursor.execute(sql, in_params)
            columns = [column[0] for column in cursor.description]
            rows = cursor.fetchall()
            while rows:
                for r in rows:
                    row_dict = {}
                    for ci in range(len(columns)):
                        row_dict[columns[ci]] = r[ci]
                    callback(row_dict)
                if cursor.nextset():
                    rows = cursor.fetchall()
                else:
                    rows = None


def _get_sp_sql(sql, params):
    parts = sql.split()
    if len(parts) >= 2 and parts[0].strip().lower() == "call":
        sp_name = parts[1].strip()
        if len(params) == 0:
            return '{call ' + sp_name + '}'
        else:
            pp = ['?' for _ in range(len(params))]
            pp = ', '.join(pp)
            return '{call ' + sp_name + '(' + pp + ')}'
    return None
