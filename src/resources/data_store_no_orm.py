"""
    This file is a part of SQL DAL Maker project: https://sqldalmaker.sourceforge.net
    It demonstrates how to implement an interface DataStore in Python + (sqlite3|psycopg2|mysql)/no-orm-scenario.
    More about DataStore: https://sqldalmaker.sourceforge.net/preconfig.html#ds
    Recent version: https://github.com/panedrone/sqldalmaker/blob/master/src/resources/data_store_no_orm.py

    Successfully tested with:

        sqlite3 ------------------- built-in
        psycopg2 ------------------ pip install psycopg2
        mysql.connector ----------- pip install mysql-connector-python

    Copy-paste it to your project and change it for your needs.
    Improvements are welcome: sqldalmaker@gmail.com

    Demo project: https://github.com/panedrone/sdm_demo_fastapi_no_orm_scenario

"""


# =========== Example of usage:
#
# import psycopg2
#
# # import sqlite3
# # import mysql.connector
# from dbal.data_store import create_ds, DataStore
#
# conn = psycopg2.connect(host="127.0.0.1", database="my_tests", user="postgres", password="sa")
# # https://pynative.com/python-mysql-transaction-management-using-commit-rollback/
# conn.autocommit = False
#
#
# # Dependency
# def get_ds() -> DataStore:
#     ds = create_ds(conn)
#     try:
#         yield ds
#     except Exception as e:
#         ds.rollback()
#         raise e
#
#
# @app.get('/api/projects', tags=["ProjectList"], response_model=List[SchemaProjectLi])
# def get_all_projects(ds: DataStore = Depends(get_ds)):
#     return ProjectsDao(ds).get_projects()

class OutParam:
    def __init__(self):
        self.value = None


class DataStore:

    def in_transaction(self) -> bool:
        pass

    def begin(self):
        pass

    def commit(self):
        pass

    def rollback(self):
        pass

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
        :param callback: a callback function for delivering fetched rows to a caller
        :return: None
        """
        pass


def create_ds(conn) -> DataStore:
    return _DS(conn)


class _DS(DataStore):
    class EngineType:
        sqlite3 = 1
        mysql = 2
        postgresql = 3

    def __init__(self, conn):

        # conn = sqlite3.connect('./todolist.sqlite', check_same_thread=False)
        # conn = mysql.connector.Connect(user='root', password='sa', host='127.0.0.1', database='todolist')
        # conn = psycopg2.connect(host="localhost", database="todolist", user="postgres", password="sa")

        self.conn = conn

        conn_module = type(self.conn).__module__.lower()

        if 'sqlite' in conn_module:
            self.engine_type = self.EngineType.sqlite3
            return
        if 'mysql' in conn_module:
            self.engine_type = self.EngineType.mysql
            return
        if 'psycopg' in conn_module:
            self.engine_type = self.EngineType.postgresql
            return

        raise Exception(f"Unknown: {conn_module}")

    def in_transaction(self) -> bool:
        if self.engine_type == self.EngineType.postgresql:
            raise Exception(f"No 'self.conn.in_transaction' in psycopg2")
        if isinstance(self.conn.in_transaction, int):  # mysql
            return self.conn.in_transaction != 0
        return self.conn.in_transaction  # sqlite

    def begin(self):
        if self.engine_type == self.EngineType.sqlite3:
            self.conn.execute('begin')
            # self.conn.begin()  #  'sqlite3.Connection' object has no attribute 'begin'"
            return
        if self.engine_type == self.EngineType.mysql:
            self.conn.start_transaction()
            return
        if self.engine_type == self.EngineType.postgresql:
            raise Exception(f"No 'self.conn.begin()' in psycopg2")

        raise Exception(f"Unknown: {self.engine_type}")

    def commit(self):
        # PEP 249 – Python Database API Specification v2.0
        # https://peps.python.org/pep-0249/
        self.conn.commit()

    def rollback(self):
        # PEP 249 – Python Database API Specification v2.0
        # https://peps.python.org/pep-0249/
        self.conn.rollback()

    def insert_row(self, sql, params, ai_values):
        sql = self._format_sql(sql)
        if len(ai_values) > 0:
            if self.engine_type == self.EngineType.postgresql:
                sql += ' RETURNING ' + ai_values[0][0]

        def do_insert(cursor):
            cursor.execute(sql, params)
            if len(ai_values) > 0:
                if self.engine_type == self.EngineType.postgresql:
                    ai_values[0][1] = cursor.fetchone()[0]
                else:
                    ai_values[0][1] = cursor.lastrowid
            if cursor.rowcount == 0:
                raise Exception('No rows inserted')

        self._exec(do_insert)

    @staticmethod
    def _exec_proc_pg(cursor, sql, params):
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

    def _exec_proc_mysql(self, cursor, sp, params):
        call_params = self._get_call_params(params)
        # result_args: https://pynative.com/python-mysql-execute-stored-procedure/
        result_args = cursor.callproc(sp, call_params)
        for p in params:
            if isinstance(p, list) and callable(p[0]):
                i = 0
                for result in cursor.stored_results():
                    callback = p[i]
                    self._fetch_all(result, callback)
                    i += 1
                break
        self._assign_out_params(params, result_args)

    def _query_proc_mysql(self, cursor, sp, on_result, params):
        call_params = self._get_call_params(params)
        # result_args: https://pynative.com/python-mysql-execute-stored-procedure/
        result_args = cursor.callproc(sp, call_params)
        for result in cursor.stored_results():
            on_result(result)
        self._assign_out_params(params, result_args)

    def exec_dml(self, sql, params):
        sql = self._format_sql(sql)
        sp = self._get_sp_name(sql)

        def do_exec(cursor):
            if sp is None:
                cursor.execute(sql, params)
                return cursor.rowcount
            if self.engine_type == self.EngineType.postgresql:
                self._exec_proc_pg(cursor, sql, params)  # sql!
            elif self.engine_type == self.EngineType.mysql:
                self._exec_proc_mysql(cursor, sp, params)  # sp!
            else:
                raise Exception('Not supported for this engine')
            return 0

        self._exec(do_exec)

    def query_scalar(self, sql, params):
        rows = self.query_all_scalars(sql, params)
        if len(rows) == 0:
            raise Exception('No rows')
        if len(rows) > 1:
            raise Exception('More than 1 row exists')
        if isinstance(rows[0], list):
            return rows[0][0]
        else:
            return rows[0]  # 'select get_test_rating(?)' returns just scalar value, not array of arrays

    def query_all_scalars(self, sql, params):
        sql = self._format_sql(sql)
        res = []
        sp = self._get_sp_name(sql)

        def fetch_all(cursor):
            if sp is None:
                cursor.execute(sql, params)
                for row in cursor:
                    res.append(row[0])
                return res
            if self.engine_type != self.EngineType.mysql:
                raise Exception('Not supported for this engine')

            def on_result(result):
                for row_values in result:
                    res.append(row_values[0])

            self._query_proc_mysql(cursor, sp, on_result, params)

        self._exec(fetch_all)
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
        sql = self._format_sql(sql)
        sp = self._get_sp_name(sql)

        def fetch_all(cursor):
            if sp is None:
                cursor.execute(sql, params)
                columns = [col[0] for col in cursor.description]
                for row in cursor:  # .fetchall():
                    # https://docs.djangoproject.com/en/3.2/topics/db/sql/
                    r = dict(zip(columns, row))
                    callback(r)
                return
            if self.engine_type != self.EngineType.mysql:
                raise Exception('Not supported for this engine')
            self._query_proc_mysql(cursor, sp, lambda result: self._fetch_all(result, callback), params)

        self._exec(fetch_all)

    def _exec(self, func: callable):
        # with self.conn.cursor() as cursor:  # sqlite3 error
        cursor = self.conn.cursor()
        try:
            func(cursor)
        finally:
            cursor.close()

    def _format_sql(self, sql):
        if self.engine_type == self.EngineType.sqlite3:
            return sql
        return sql.replace("?", "%s")

    @staticmethod
    def _fetch_all(result, callback):
        # https://stackoverflow.com/questions/34030020/mysql-python-connector-get-columns-names-from-select-statement-in-stored-procedu
        # https://kadler.github.io/2018/01/08/fetching-python-database-cursors-by-column-name.html#
        for row_values in result:
            row = {}
            i = 0
            for d in result.description:
                col_name = d[0]
                value = row_values[i]
                row[col_name] = value
                i = i + 1
            callback(row)

    @staticmethod
    def _get_sp_name(sql):
        parts = sql.split()
        if len(parts) >= 2 and parts[0].strip().lower() == "call":
            name = parts[1]
            end = name.find("(")
            if end == -1:
                return name
            else:
                return name[0:end]
        return None

    @staticmethod
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
                pass  # MySQL SP returning result-sets
            else:
                call_params.append(p)
        return call_params

    @staticmethod
    def _assign_out_params(params, result_args):
        for i in range(len(params)):
            if isinstance(params[i], OutParam):
                params[i].value = result_args[i]
