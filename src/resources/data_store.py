# uncomment one of the imports below to use without django.db

# import sqlite3
# import psycopg2
# import mysql.connector

# uncomment the code below to use with django.db:

import os

import django.db
from django.apps import AppConfig
from django.db import transaction
from django.db.backends.base.base import BaseDatabaseWrapper


class MyDjangoAppConfig(AppConfig):
    # default_auto_field = 'django.db.models.BigAutoField'
    default_auto_field = 'django.db.models.AutoField'
    name = 'dal'  # python package containing generated django models


# there should be "settings.py" in the project root
# Google --> django settings.py location
os.environ.setdefault('DJANGO_SETTINGS_MODULE', 'settings')

# django.setup() should be called before importing/using generated django models -->
# AppRegistryNotReady("Apps aren't loaded yet.")
django.setup()


class OutParam:
    def __init__(self):
        self.value = None


class DataStore:
    """
        SQL DAL Maker Website: http://sqldalmaker.sourceforge.net
        This is an example of how to implement DataStore in Python + sqlite3/psycopg2/mysql/django.db -->
        Executing custom SQL directly https://docs.djangoproject.com/en/3.2/topics/db/sql/
        Recent version: https://github.com/panedrone/sqldalmaker/blob/master/src/resources/data_store.py
        Copy-paste this code to your project and change it for your needs.
        Improvements are welcome: sqldalmaker@gmail.com
        Successfully tested in django projects:
        - 'django.db.backends.sqlite3' ---------------- built-in
        - 'django.db.backends.postgresql_psycopg2' ---- pip install psycopg2
        - 'mysql.connector.django' -------------------- pip install mysql-connector-python
           ^^ instead of built-in 'django.db.backends.mysql' to enable cursor.stored_results().
           MySQL SP returning result-sets --> http://www.mysqltutorial.org/calling-mysql-stored-procedures-python/
           MySQL Connector/Python as Django Engine? -->
           https://stackoverflow.com/questions/26573984/django-how-to-install-mysql-connector-python-with-pip3)
    """

    class EngineType:
        sqlite3 = 1
        mysql = 2
        postgresql = 3

    def __init__(self):
        self.conn = None
        self.engine_type = self.EngineType.sqlite3
        self.open()

    def open(self):
        # uncomment to use without django.db:

        # self.conn = sqlite3.connect('./task-tracker.sqlite')
        # self.engine_type = self.EngineType.sqlite3

        # self.conn = mysql.connector.Connect(user='root', password='root', host='127.0.0.1', database='sakila')
        # self.engine_type = self.EngineType.mysql

        # self.conn = psycopg2.connect(host="localhost", database="my-tests", user="postgres", password="sa")
        # self.engine_type = self.EngineType.postgresql

        # print(self.conn.autocommit)

        # uncomment to use with django.db:

        con = django.db.connections['default']
        engine = con.settings_dict["ENGINE"]
        if 'sqlite3' in engine:
            self.engine_type = self.EngineType.sqlite3
        elif 'mysql' in engine:
            self.engine_type = self.EngineType.mysql
        elif 'postgresql' in engine:
            self.engine_type = self.EngineType.postgresql
        else:
            raise Exception('Unexpected: ' + engine)
        self.conn = con

    def close(self):
        if self.conn:
            self.conn.close()
            self.conn = None

    @staticmethod
    def get_all(cls, params=None) -> []:
        if not params:
            params = ()
        raw_query_set = cls.objects.raw(cls.SQL, params)
        res = [r for r in raw_query_set]
        return res

    @staticmethod
    def get_one(cls, params=None):
        rows = DataStore.get_all(cls, params)
        if len(rows) == 0:
            raise Exception('No rows')
        if len(rows) > 1:
            raise Exception('More than 1 row exists')
        return rows[0]

    # uncomment to use without django.db:

    # def begin(self):
    #     self.conn.execute('begin')  # sqlite3
    #     self.conn.start_transaction() # mysql
    #     self.conn.begin() # psycopg2
    #
    # # uncomment to use without django.db:
    # def commit(self):
    #     self.conn.execute('commit')  # sqlite3
    #     self.conn.commit() # psycopg2, mysql
    #
    # # uncomment to use without django.db:
    # def rollback(self):
    #     self.conn.execute("rollback")  # sqlite3
    #     self.conn.rollback() # psycopg2, mysql

    # uncomment to use with django.db:

    @staticmethod
    def begin():
        django.db.transaction.set_autocommit(False)

    @staticmethod
    def commit():
        django.db.transaction.commit()

    @staticmethod
    def rollback():
        django.db.transaction.rollback()

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
        """
        Arguments:
            sql (string): SQL statement.
            params (array, optional): Values of SQL parameters.
        Returns:
            Number of updated rows.
        """
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
        """
        Returns:
            Single scalar value.
        Arguments:
            sql (string): SQL statement.
            params (array, optional): Values of SQL parameters if needed.
        Raises:
            Exception: if amount of rows != 1.
        """
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
        """
        Returns:
            array of scalar values
        Arguments:
            sql (string): SQL statement.
            params (array, optional): Values of SQL parameters if needed.
        """
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
            sql (string): SQL statement.
            params (array, optional): Values of SQL parameters if needed.
            callback
        """
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
        if isinstance(self.conn, BaseDatabaseWrapper):
            # https://stackoverflow.com/questions/8402898/how-can-i-access-the-low-level-psycopg2-connection-in-django
            with django.db.connection.cursor() as cursor:
                func(cursor)
            return
        # with self.conn.cursor() as cursor:  # sqlite3 error without django
        cursor = self.conn.cursor()
        try:
            func(cursor)
        finally:
            cursor.close()

    def _format_sql(self, sql):
        if isinstance(self.conn, BaseDatabaseWrapper):
            return sql.replace("?", "%s")
        return sql

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
