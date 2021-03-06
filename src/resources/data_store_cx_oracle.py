import cx_Oracle


class OutParam:
    """
    The class to access both OUT and INOUT parameters
    """

    def __init__(self, param_type, param_value=None):
        self.param_type = param_type
        self.param_value = param_value


class DataStore:
    """
    SQL DAL Maker Website: http://sqldalmaker.sourceforge.net
    This is an example of how to implement DataStore in Python + cx_Oracle.
    Recent version: https://github.com/panedrone/sqldalmaker/blob/master/src/resources/data_store_cx_oracle.py
    Copy-paste this code to your project and change it for your needs.
    Improvements are welcome: sqldalmaker@gmail.com
    """
    def __init__(self):
        self.conn = None

    def open(self):
        self.conn = cx_Oracle.connect('ORDERS', 'root', 'localhost:1521/orcl', encoding='UTF-8')
        # print(self.conn.autocommit)

    def close(self):
        if self.conn is None:
            return
        self.conn.close()
        self.conn = None

    def start_transaction(self):
        self.conn.begin()

    def commit(self):
        self.conn.commit()

    def rollback(self):
        self.conn.rollback()

    def insert_row(self, sql, params, ai_values):
        """
        Returns:
            Nothing.
        Arguments:
            sql: SQL statement.
            params: Values of SQL parameters.
            ai_values: an array like [["o_id", 1], ...] for generated keys.
        Raises:
            Exception: if no rows inserted.
        """
        with self.conn.cursor() as cursor:
            sql = _format_sql(sql)
            gen_col_param = None
            if len(ai_values) > 0:
                if len(ai_values) > 1:
                    raise Exception('Multiple generated keys are not allowed')
                gen_col_param = cursor.var(int)
                params.append(gen_col_param)
                gen_col_name = ai_values[0][0]
                sql += ' returning ' + gen_col_name + ' into :' + gen_col_name
            cursor.execute(sql, params)
            if gen_col_param is not None:
                ai_values[0][1] = gen_col_param.getvalue()[0]
            if cursor.rowcount == 0:
                raise Exception('No rows inserted')

    def exec_dml(self, sql, params):
        """
        Arguments:
            sql: SQL statement.
            params: Values of SQL parameters.
        Returns:
            Number of affected rows.
        """
        sp_sql = _get_sp_sql(sql)
        if sp_sql is not None:
            sql = sp_sql
        sql = _format_sql(sql)
        with self.conn.cursor() as cursor:
            if sp_sql is None:
                cursor.execute(sql, params)
            else:
                self._call_proc(sql, cursor, params)
            return cursor.rowcount

    def _prepare_call_params(self, cursor, params):
        out_cursors = False
        call_params = []
        for p in params:
            if isinstance(p, OutParam):
                cp = cursor.var(p.param_type)
                cp.setvalue(0, p.param_value)
                call_params.append(cp)
            elif callable(p):
                cp = self.conn.cursor()
                call_params.append(cp)
                out_cursors = True
            elif isinstance(p, list) and callable(p[0]):
                pass
            else:
                call_params.append(p)
        return call_params, out_cursors

    @staticmethod
    def _process_call_results(cursor, out_cursors, call_params, params):
        if out_cursors:
            i = 0
            for p in params:
                if callable(p):
                    cp = call_params[i]
                    _fetch_all(cp, p)
                    cp.close()
                i += 1
        else:
            for p in params:
                if isinstance(p, list) and callable(p[0]):
                    i = 0  # (exec-dml)+(SP call)+(list-param containing callback(s)) means 'implicit cursor'
                    for implicit_cursor in cursor.getimplicitresults():
                        _fetch_all(implicit_cursor, p[i])
                        i += 1
        i = 0
        for p in params:
            if isinstance(p, OutParam):
                cp = call_params[i]
                p.param_value = cp.getvalue()
            i += 1

    def _call_proc(self, sql, cursor, params):
        call_params, out_cursors = self._prepare_call_params(cursor, params)
        cursor.execute(sql, call_params)
        self._process_call_results(cursor, out_cursors, call_params, params)

    def query_scalar(self, sql, params):
        """
        Returns:
            Single scalar value.
        Arguments:
            sql: SQL statement.
            params: Values of SQL parameters if needed.
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
            return rows[0]

    def query_scalar_array(self, sql, params):
        """
        Returns:
            array of scalar values
        Arguments:
            sql: SQL statement.
            params: Values of SQL parameters if needed.
        """
        sp_sql = _get_sp_sql(sql)
        if sp_sql is not None:
            sql = sp_sql
        sql = _format_sql(sql)
        res = []
        with self.conn.cursor() as cursor:
            cursor.execute(sql, params)
            rows = cursor.fetchall()
            for r in rows:
                res.append(r[0])
        return res

    def query_single_row(self, sql, params):
        """
        Returns:
            Single row
        Arguments:
            sql: SQL statement.
            params: Values of SQL parameters if needed.
        Raises:
            Exception: if amount of rows != 1.
        """
        rows = []
        self.query_all_rows(sql, params, lambda row: rows.append(row))
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
            sql: SQL statement.
            params: Values of SQL parameters if needed.
            callback
        """
        sp_sql = _get_sp_sql(sql)
        if sp_sql is not None:
            sql = sp_sql
        sql = _format_sql(sql)
        with self.conn.cursor() as cursor:
            if sp_sql is None:
                cursor.execute(sql, params)
                _fetch_all(cursor, callback)
            else:
                raise Exception("SP are not allowed in 'query...', use 'exec-dml' instead")


def _get_sp_sql(sql):
    parts = sql.split()
    if len(parts) >= 2 and parts[0].strip().lower() == "begin":
        return sql
    return None


def _format_sql(sql):
    i = 1
    while True:
        pos = sql.find('?')
        if pos == -1:
            break
        str1 = sql[0:pos]
        str2 = sql[pos + 1:]
        sql = str1 + ':' + str(i) + str2
        i += 1
    return sql


def _fetch_all(cursor, callback):
    columns = [column[0] for column in cursor.description]
    for r in cursor:
        row_dict = {}
        for ci in range(len(columns)):
            row_dict[columns[ci]] = r[ci]
        callback(row_dict)
