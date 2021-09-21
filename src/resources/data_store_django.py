import os

import django.db
from django.db import transaction


class OutParam:
    value = None


class DataStore:
    """
        SQL DAL Maker Website: http://sqldalmaker.sourceforge.net
        This is an example of how to implement DataStore in Python + django.db -->
        Executing custom SQL directly https://docs.djangoproject.com/en/3.2/topics/db/sql/
        Recent version: https://github.com/panedrone/sqldalmaker/blob/master/src/resources/data_store_django.py
        Copy-paste this code to your project and change it for your needs.
        Improvements are welcome: sqldalmaker@gmail.com
        Successfully tested with:
        - 'django.db.backends.sqlite3'
        - 'django.db.backends.postgresql_psycopg2'
        - 'mysql.connector.django' instead of built-in 'django.db.backends.mysql' to enable cursor.stored_results()
           MySQL SP returning result-sets --> http://www.mysqltutorial.org/calling-mysql-stored-procedures-python/
           MySQL Connector/Python as Django Engine?
           https://stackoverflow.com/questions/26573984/django-how-to-install-mysql-connector-python-with-pip3
    """

    class Engine:
        sqlite3 = 1
        mysql = 2
        postgresql = 3

    def __init__(self):
        os.environ.setdefault('DJANGO_SETTINGS_MODULE', 'settings')
        self.conn = None
        self.engine = self.Engine.sqlite3

    def open(self):
        con = django.db.connections['default']
        engine = con.settings_dict["ENGINE"]
        if 'sqlite3' in engine:
            self.engine = self.Engine.sqlite3
        elif 'mysql' in engine:
            self.engine = self.Engine.mysql
        elif 'postgresql' in engine:
            self.engine = self.Engine.postgresql
        else:
            raise Exception('Unexpected: ' + engine)
        self.conn = con

    def close(self):
        if self.conn:
            self.conn.close()
            self.conn = None

    @staticmethod
    def start_transaction():
        transaction.set_autocommit(False)

    @staticmethod
    def commit():
        transaction.commit()

    @staticmethod
    def rollback():
        transaction.rollback()

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
        with self.conn.cursor() as cursor:
            sql = _format_sql(sql)
            if len(ai_values) > 0:
                if self.engine == self.Engine.postgresql:
                    sql += ' RETURNING ' + ai_values[0][0]
            cursor.execute(sql, params)
            if len(ai_values) > 0:
                if self.engine == self.Engine.postgresql:
                    ai_values[0][1] = cursor.fetchone()[0]
                else:
                    ai_values[0][1] = cursor.lastrowid
            if cursor.rowcount == 0:
                raise Exception('No rows inserted')

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

    @staticmethod
    def _exec_proc_mysql(cursor, sp, params):
        call_params = _get_call_params(params)
        # result_args: https://pynative.com/python-mysql-execute-stored-procedure/
        result_args = cursor.callproc(sp, call_params)
        for p in params:
            if isinstance(p, list) and callable(p[0]):
                i = 0  # MySQL only:
                for result in cursor.stored_results():
                    callback = p[i]
                    _fetch_all(result, callback)
                    i += 1
                break
        _assign_out_params(params, result_args)

    def exec_dml(self, sql, params):
        """
        Arguments:
            sql (string): SQL statement.
            params (array, optional): Values of SQL parameters.
        Returns:
            Number of updated rows.
        """
        sql = _format_sql(sql)
        sp = _get_sp_name(sql)
        with self.conn.cursor() as cursor:
            if sp is None:
                cursor.execute(sql, params)
                return cursor.rowcount
            if self.engine == self.Engine.postgresql:
                self._exec_proc_pg(cursor, sql, params)  # sql!
            elif self.engine == self.Engine.mysql:
                self._exec_proc_mysql(cursor, sp, params)  # sp!
            else:
                raise Exception('Not supported for this engine')
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
        sql = _format_sql(sql)
        res = []
        sp = _get_sp_name(sql)
        # https://docs.djangoproject.com/en/3.2/topics/db/sql/
        with self.conn.cursor() as cursor:
            if sp is None:
                cursor.execute(sql, params)
                for row in cursor:  # .fetchall():
                    res.append(row[0])
                return res
            if self.engine != self.Engine.mysql:
                raise Exception('Not supported for this engine')
            call_params = _get_call_params(params)
            # result_args: https://pynative.com/python-mysql-execute-stored-procedure/
            result_args = cursor.callproc(sp, call_params)
            for result in cursor.stored_results():  # MySQL only:
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
        sql = _format_sql(sql)
        sp = _get_sp_name(sql)
        with self.conn.cursor() as cursor:
            if sp is None:
                cursor.execute(sql, params)
                columns = [col[0] for col in cursor.description]
                for row in cursor:  # .fetchall():
                    # https://docs.djangoproject.com/en/3.2/topics/db/sql/
                    r = dict(zip(columns, row))
                    callback(r)
                return
            if self.engine != self.Engine.mysql:
                raise Exception('Not supported for this engine')
            call_params = _get_call_params(params)
            # result_args: https://pynative.com/python-mysql-execute-stored-procedure/
            result_args = cursor.callproc(sp, call_params)
            for result in cursor.stored_results():  # MySQL only:
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


def _format_sql(sql):
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
            pass  # MySQL SP returning result-sets
        else:
            call_params.append(p)
    return call_params


def _assign_out_params(params, result_args):
    for i in range(len(params)):
        if isinstance(params[i], OutParam):
            params[i].value = result_args[i]
