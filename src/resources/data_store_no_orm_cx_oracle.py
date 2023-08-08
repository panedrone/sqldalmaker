"""
    This file is a part of SQL DAL Maker project: https://sqldalmaker.sourceforge.net
    It demonstrates how to implement an interface DataStore in Python/cx_oracle.
    More about DataStore: https://sqldalmaker.sourceforge.net/preconfig.html#ds
    Recent version: https://github.com/panedrone/sqldalmaker/blob/master/src/resources/data_store_no_orm_cx_oracle.py
    Copy-paste it to your project and change it for your needs.
    Improvements are welcome: sqldalmaker@gmail.com

"""

import cx_Oracle


class OutParam:
    """
    The class to access both OUT and INOUT parameters
    """

    def __init__(self, param_type, param_value=None):
        self.param_type = param_type
        self.param_value = param_value


class DataStore:

    def open(self): pass

    def close(self): pass

    def begin(self): pass

    def commit(self): pass

    def rollback(self): pass

    def insert_row(self, sql, params, ai_values):
        """
        :param sql: str
        :param params: array, values of SQL parameters
        :param ai_values: an array like [["o_id", 1], ...] to specify and obtain auto-incremented values
        :return: None
        :raise Exception if no rows inserted.
        """
        pass

    def exec_dml(self, sql, params):
        """
        :param sql: str
        :param params: array, values of SQL parameters
        :return: int, amount of rows affected
        """
        pass

    def query_scalar(self, sql, params):
        """
        :param sql: str
        :param params: array, values of SQL parameters
        :return single scalar value
        :raise Exception if amount of fetched rows != 1
        """
        pass

    def query_all_scalars(self, sql, params) -> []:
        """
        :param sql: str
        :param params: array, values of SQL parameters
        :return array of scalar values
        """
        pass

    def query_row(self, sql, params):
        """
        :param sql: str
        :param params: array, values of SQL parameters
        :return single fetched row or error string
        """
        pass

    def query_all_rows(self, sql, params, callback):
        """
        :param sql: str
        :param params: array, values of SQL parameters.
        :param callback: function delivering fetched rows to caller
        :return: None
        """
        pass


def create_ds() -> DataStore:
    ds = _DS()
    ds.open()
    return ds


class _DS(DataStore):
    def __init__(self):
        self.conn = None

    def open(self):
        self.conn = cx_Oracle.connect('MY_TESTS', 'sa', 'localhost:1521/xe', encoding='UTF-8')
        # print(self.conn.autocommit)

    def close(self):
        if self.conn is None:
            return
        self.conn.close()
        self.conn = None

    def begin(self):
        self.conn.begin()

    def commit(self):
        self.conn.commit()

    def rollback(self):
        self.conn.rollback()

    def insert_row(self, sql, params, ai_values):
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
        rows = self.query_all_scalars(sql, params)
        if len(rows) == 0:
            raise Exception('No rows')
        if len(rows) > 1:
            raise Exception('More than 1 row exists')
        if isinstance(rows[0], list):
            return rows[0][0]
        else:
            return rows[0]

    def query_all_scalars(self, sql, params):
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

    def query_row(self, sql, params):
        rows = []
        self.query_all_rows(sql, params, lambda row: rows.append(row))
        if len(rows) == 1:
            return rows[0]
        if len(rows) == 0:
            return 'No rows'
        return 'More than 1 row exists'

    def query_all_rows(self, sql, params, callback):
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
