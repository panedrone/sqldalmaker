import sqlalchemy

import sqlalchemy.ext.declarative
from sqlalchemy.orm import declarative_base, sessionmaker

Base = declarative_base()


class OutParam:
    def __init__(self):
        self.value = None


class DataStore:
    """
    SQL DAL Maker Website: http://sqldalmaker.sourceforge.net
    This is an example of how to implement DataStore in Python + SQLAlchemy Raw SQL.
    Recent version: https://github.com/panedrone/sqldalmaker/blob/master/src/resources/data_store_sqlalchemy.py
    Copy-paste this code to your project and change it for your needs.
    Improvements are welcome: sqldalmaker@gmail.com

    How to Execute Raw SQL in SQLAlchemy
    https://chartio.com/resources/tutorials/how-to-execute-raw-sql-in-sqlalchemy/

    Successfully tested with:
    - sqlite3 ---------------- built-in
    - postgresql ------------- pip install psycopg2
    - mysql+mysqlconnector --- pip install mysql-connector-python

    """

    class EngineType:
        sqlite3 = 1
        mysql = 2
        postgresql = 3

    def __init__(self):
        self.conn = None
        self.transaction = None
        self.engine = sqlalchemy.create_engine('sqlite:///todo-list.sqlite')
        self.engine_type = self.EngineType.sqlite3
        # self.engine = sqlalchemy.create_engine('postgresql://postgres:sa@localhost/my-tests')
        # self.engine_type = self.EngineType.postgresql
        # https://www.tutorialguruji.com/dbms/how-do-i-execute-a-mysql-stored-procedure-in-a-sqlalchemy-scoped-session-to-return-a-single-result-set-of-data-for-flask-web-app/
        # self.engine = sqlalchemy.create_engine('mysql+mysqlconnector://root:root@localhost/sakila')
        # self.engine_type = self.EngineType.mysql
        self.session = sessionmaker(bind=self.engine)()

    def open(self):
        self.conn = self.engine.connect()

    def close(self):
        if self.conn:
            self.conn.close()
            self.conn = None

    def start_transaction(self):
        # https://docs.sqlalchemy.org/en/14/core/connections.html
        self.transaction = self.conn.begin()

    def commit(self):
        if self.transaction is None:
            self.session.commit()
            return
        # https://docs.sqlalchemy.org/en/14/core/connections.html
        self.transaction.commit()
        self.transaction = None

    def rollback(self):
        if self.transaction is None:
            # https://docs.sqlalchemy.org/en/14/orm/session_basics.html
            self.session.rollback()
            return
        # https://docs.sqlalchemy.org/en/14/core/connections.html
        self.transaction.rollback()
        self.transaction = None

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
        sql = _format_sql(sql)
        if len(ai_values) > 0:
            if self.engine_type == self.EngineType.postgresql:
                sql += ' RETURNING ' + ai_values[0][0]
        cursor = self._exec(sql, params)
        if len(ai_values) > 0:
            if self.engine_type == self.EngineType.postgresql:
                ai_values[0][1] = cursor.fetchone()[0]
            else:
                ai_values[0][1] = cursor.lastrowid
        if cursor.rowcount == 0:
            raise Exception('No rows inserted')

    def _exec(self, sql, params):
        pp = tuple(params)
        txt = sql  # don't use sqlalchemy.text(sql) with '%' as params
        return self.conn.execute(txt, pp)

    def _exec_proc_pg(self, sql, params):
        out_params = []
        call_params = []
        for p in params:
            if isinstance(p, OutParam):
                call_params.append(p.value)
                out_params.append(p)
            else:
                call_params.append(p)
        cursor = self._exec(sql, call_params)
        if len(out_params) > 0:
            row0 = cursor.fetchone()
            i = 0
            for value in row0:
                out_params[i].value = value
                i += 1

    def _exec_sp_mysql(self, sp, params):
        call_params = _get_call_params(params)
        # https://stackoverflow.com/questions/45979950/sqlalchemy-error-when-calling-mysql-stored-procedure
        raw_conn = self.engine.raw_connection()
        try:
            with raw_conn.cursor() as cursor:
                result_args = cursor.callproc(sp, call_params)
                for p in params:
                    if isinstance(p, list) and callable(p[0]):
                        i = 0
                        for result in cursor.stored_results():
                            callback = p[i]
                            _fetch_all(result, callback)
                            i += 1
                        break
                _assign_out_params(params, result_args)
        finally:
            raw_conn.close()

    def _query_sp_mysql(self, sp, on_result, params):
        call_params = _get_call_params(params)
        # https://stackoverflow.com/questions/45979950/sqlalchemy-error-when-calling-mysql-stored-procedure
        raw_conn = self.engine.raw_connection()
        try:
            with raw_conn.cursor() as cursor:
                # result_args: https://pynative.com/python-mysql-execute-stored-procedure/
                result_args = cursor.callproc(sp, call_params)
                for result in cursor.stored_results():
                    on_result(result)
                _assign_out_params(params, result_args)
        finally:
            raw_conn.close()

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
        if sp is None:
            cursor = self._exec(sql, params)
            return cursor.rowcount
        if self.engine_type == self.EngineType.postgresql:
            self._exec_proc_pg(sql, params)  # sql!
        elif self.engine_type == self.EngineType.mysql:
            self._exec_sp_mysql(sp, params)  # sp!
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
        if sp is None:
            cursor = self._exec(sql, params)
            for row in cursor:
                res.append(row[0])
            return res
        if self.engine_type != self.EngineType.mysql:
            raise Exception('Not supported for this engine')

        def on_result(result):
            for row_values in result:
                res.append(row_values[0])

        self._query_sp_mysql(sp, on_result, params)
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
        if sp is None:
            cursor = self._exec(sql, params)
            for row in cursor:
                callback(row)
            return
        if self.engine_type != self.EngineType.mysql:
            raise Exception('Not supported for this engine')
        self._query_sp_mysql(sp, lambda result: _fetch_all(result, callback), params)


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


def _format_sql(sql):
    return sql.replace("?", "%s")


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
