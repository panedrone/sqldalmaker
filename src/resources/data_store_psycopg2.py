import psycopg2


class OutParam:
    """
    The class to work with both OUT and INOUT parameters
    """

    def __init__(self, value=None):
        self.value = value


class DataStore:
    """
    SQL DAL Maker Website: https://sqldalmaker.sourceforge.net/
    This is an example of how to implement DataStore in Python + psycopg2.
    Recent version: https://github.com/panedrone/sqldalmaker/blob/master/src/resources/data_store_psycopg2.py
    Copy-paste this code to your project and change it for your needs.
    Improvements are welcome: sqldalmaker@gmail.com
    """
    def __init__(self):
        self.conn = None

    def open(self):
        self.conn = psycopg2.connect(host="localhost", database="my-tests", user="postgres", password="sa")
        # print(self.conn.autocommit)

    def close(self):
        if self.conn:
            self.conn.close()
            self.conn = None

    def start_transaction(self):
        self.conn.begin()

    def commit(self):
        self.conn.commit()

    def rollback(self):
        self.conn.rollback()

    def insert_row(self, sql, params, ai_values) -> None:
        sql = _format_sql(sql)
        # http://zetcode.com/python/psycopg2/
        if len(ai_values) > 0:
            sql += ' RETURNING ' + ai_values[0][0]
        with self.conn.cursor() as cursor:
            cursor.execute(sql, params)
            if len(ai_values) > 0:
                ai_values[0][1] = cursor.fetchone()[0]
            if cursor.rowcount == 0:
                raise Exception('No rows inserted')

    def exec_dml(self, sql, params) -> int:
        """
        Returns number of updated rows.
        """
        sp_sql = _get_sp_sql(sql)
        if sp_sql is not None:
            sql = sp_sql
        sql = _format_sql(sql)
        with self.conn.cursor() as cursor:
            if sp_sql is None:
                cursor.execute(sql, params)
                return cursor.rowcount
            out_params = []
            call_params = []
            for p in params:
                if isinstance(p, OutParam):
                    call_params.append(p.value)
                    out_params.append(p)
                else:
                    call_params.append(p)
            cursor.execute(sql, call_params)
            if len(out_params) > 0:
                row0 = cursor.fetchone()
                i = 0
                for value in row0:
                    out_params[i].value = value
                    i += 1

    def query_scalar(self, sql, params):
        rows = self.query_scalar_array(sql, params)
        if len(rows) == 0:
            raise Exception('No rows')
        if len(rows) > 1:
            raise Exception('More than 1 row exists')
        if isinstance(rows[0], list):
            return rows[0][0]
        else:
            # call of UDF with syntax like 'select get_test_rating(?)'
            # returns just scalar value, not an array of arrays
            return rows[0]

    def query_scalar_array(self, sql, params) -> []:
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
        rows = []
        self.query_all_rows(sql, params, lambda row: rows.append(row))
        if len(rows) == 0:
            raise Exception('No rows')
        if len(rows) > 1:
            raise Exception('More than 1 row exists')
        return rows[0]

    def query_all_rows(self, sql, params, callback) -> None:
        sp_sql = _get_sp_sql(sql)
        if sp_sql is not None:
            sql = sp_sql
        sql = _format_sql(sql)
        with self.conn.cursor() as cursor:
            cursor.execute(sql, params)
            rows = cursor.fetchall()
            columns = [column[0] for column in cursor.description]
            for r in rows:
                row_dict = {}
                for ci in range(len(columns)):
                    row_dict[columns[ci]] = r[ci]
                callback(row_dict)


def _get_sp_sql(sql):
    parts = sql.split()
    if len(parts) >= 2 and parts[0].strip().lower() == "call":
        return sql
    return None


def _format_sql(sql):
    return sql.replace('?', '%s')
