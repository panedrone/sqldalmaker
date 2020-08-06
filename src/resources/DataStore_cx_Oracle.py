import cx_Oracle


class OutParam:
    """
    The class to work with both OUT and INOUT parameters
    """

    def __init__(self, ptype, pvalue=None):
        self.ptype = ptype
        self.pvalue = pvalue


class RefCursor:
    """
    The class to work with SYS_REFCURSOR parameters
    """
    pass


class DataStore:
    """
    SQL DAL Maker Website: http://sqldalmaker.sourceforge.net

    This is an example of how to implement DataStore in Python + cx_Oracle.
    Copy-paste this code to your project and change it for your needs.

    Improvements are welcome: sqldalmaker@gmail.com
    """
    conn = None

    def open(self):
        self.conn = cx_Oracle.connect('ORDERS', 'sa', 'localhost:1521/orcl', encoding='UTF-8')
        # print(self.conn.autocommit)

    def close(self):
        if self.conn is not None:
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
            gen_col_name = None
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
            Number of updated rows.
        """
        sp_sql = _get_sp_sql(sql, params)
        if sp_sql is not None:
            sql = sp_sql
        sql = _format_sql(sql)
        with self.conn.cursor() as cursor:
            if sp_sql is None:
                cursor.execute(sql, params)
                return cursor.rowcount
            else:
                call_params = []
                for p in params:
                    if isinstance(p, OutParam):
                        cp = cursor.var(p.ptype)
                        cp.setvalue(0, p.pvalue)
                        call_params.append(cp)
                    elif isinstance(p, RefCursor):
                        raise Exception("RefCursor-s are not enabled in exec_dml so far.")
                    else:
                        call_params.append(p)
                cursor.execute(sql, call_params)
                i = 0
                for p in params:
                    if isinstance(p, OutParam):
                        cp = call_params[i]
                        p.pvalue = cp.getvalue()
                    i += 1

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
        sp_sql = _get_sp_sql(sql, params)
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
            sql: SQL statement.
            params: Values of SQL parameters if needed.
            callback
        """
        sp_sql = _get_sp_sql(sql, params)
        if sp_sql is not None:
            sql = sp_sql
        sql = _format_sql(sql)
        with self.conn.cursor() as cursor:
            if sp_sql is None:
                cursor.execute(sql, params)
                _fetch_all(cursor, callback)
            else:
                # https://cx-oracle.readthedocs.io/en/latest/user_guide/plsql_execution.html
                out_cursors = False
                call_params = []
                for p in params:
                    if isinstance(p, OutParam):
                        cp = cursor.var(p.ptype)
                        cp.setvalue(0, p.pvalue)
                        call_params.append(cp)
                    elif isinstance(p, RefCursor):
                        cp = self.conn.cursor()
                        out_cursors = True
                        call_params.append(cp)
                    else:
                        call_params.append(p)
                cursor.execute(sql, call_params)
                if out_cursors:
                    i = 0
                    for p in params:
                        if isinstance(p, OutParam):
                            cp = call_params[i]
                            p.pvalue = cp.getvalue()
                        elif isinstance(p, RefCursor):
                            cp = call_params[i]
                            _fetch_all(cp, callback)
                            cp.close()
                        i += 1
                else: # implicit cursor if no cursors in 'params'
                    for implicit_cursor in cursor.getimplicitresults():
                        _fetch_all(implicit_cursor, callback)
                    i = 0
                    for p in params:
                        if isinstance(p, OutParam):
                            cp = call_params[i]
                            p.pvalue = cp.getvalue()
                        i += 1


def _get_sp_sql(sql, params):
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
