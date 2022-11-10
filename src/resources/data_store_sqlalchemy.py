"""

SQL DAL Maker Website: http://sqldalmaker.sourceforge.net

This is an example of how to implement DataStore in Python + SQLAlchemy.

Recent version: https://github.com/panedrone/sqldalmaker/blob/master/src/resources/data_store_sqlalchemy.py

Copy-paste this code to your project and change it for your needs.

Successfully tested with:

    - sqlite3 ---------------- built-in
    - postgresql ------------- pip install psycopg2
    - mysql+mysqlconnector --- pip install mysql-connector-python
    - cx_Oracle -------------- pip install cx_oracle

Improvements are welcome: sqldalmaker@gmail.com

"""

import flask_sqlalchemy


# import cx_Oracle


class OutParam:
    def __init__(self):
        self.value = None


class DataStore:

    def begin(self): pass

    def commit(self): pass

    def rollback(self): pass

    # raw-SQL

    def get_one_raw(self, cls, params=None): pass

    def get_all_raw(self, cls, params=None) -> []: pass

    # ORM helpers

    def filter(self, cls, params=None): pass

    def delete_by_filter(self, cls, params=None) -> int: pass

    # ORM-based CRUD

    def create_one(self, instance) -> None: pass

    def read_all(self, cls) -> []: pass

    def read_one(self, cls, params=None): pass

    def update_one(self, instance): pass

    def delete_one(self, cls, params=None) -> int: pass

    # the methods called by generated dao classes

    def insert_row(self, sql, params, ai_values): pass

    def exec_dml(self, sql, params): pass

    def query_scalar(self, sql, params): pass

    def query_all_scalars(self, sql, params) -> []: pass

    def query_row(self, sql, params): pass

    def query_all_rows(self, sql, params, callback) -> []: pass


if flask_sqlalchemy:
    Base = None

    Column = None
    ForeignKey = None

    # if not cx_Oracle:
    SmallInteger = None
    Integer = None
    BigInteger = None

    Float = None

    DateTime = None

    String = None
    Boolean = None
    LargeBinary = None

    # ======== Below is an example of pre-configuring for flask_sqlalchemy (do it somewhere in __main__). ========

    # flask_app = flask.Flask(__name__)
    #
    # dir_path = os.path.dirname(os.path.realpath(__file__))
    # flask_app.config['SQLALCHEMY_DATABASE_URI'] = f"sqlite:///{dir_path}/todolist.sqlite"
    #
    # # add mysql-connector-python to requirements.txt
    # # app.config['SQLALCHEMY_DATABASE_URI'] = 'mysql+mysqlconnector://root:sa@localhost/todolist'
    #
    # # add psycopg2 to requirements.txt
    # # app.config['SQLALCHEMY_DATABASE_URI'] = 'postgresql://postgres:sa@localhost/my-tests'
    #
    # # add cx_oracle to requirements.txt
    # # if cx_Oracle:
    # #     user = 'MY_TESTS'
    # #     pwd = 'sa'
    # #     dsn = cx_Oracle.makedsn(
    # #         'localhost', 1521,
    # #         service_name="orcl"
    # #         # service_name='your_service_name_if_any'
    # #     )
    # #     app.config['SQLALCHEMY_DATABASE_URI'] = f'oracle+cx_oracle://{user}:{pwd}@{dsn}'
    #
    # # FSADeprecationWarning: SQLALCHEMY_TRACK_MODIFICATIONS adds
    # # significant overhead and will be disabled by default in the future.
    # # Set it to True or False to suppress this warning.
    # flask_app.config['SQLALCHEMY_TRACK_MODIFICATIONS'] = False
    #
    # db = flask_sqlalchemy.SQLAlchemy(flask_app)
    #
    # MyApp.ds = create_ds(db) # DataStore as an singleton

    def create_ds(db: flask_sqlalchemy.SQLAlchemy) -> DataStore:  # factory

        global Base, Column, ForeignKey, \
            SmallInteger, Integer, BigInteger, Float, DateTime, String, Boolean, LargeBinary

        Base = db.Model

        Column = db.Column
        ForeignKey = db.ForeignKey

        # if not cx_Oracle:
        SmallInteger = db.SmallInteger
        Integer = db.Integer
        BigInteger = db.BigInteger

        Float = db.Float

        DateTime = db.DateTime

        String = db.String
        Boolean = db.Boolean
        LargeBinary = db.LargeBinary

        return _DS(db)


# else:
#     # the code below is for SQLAlchemy without Flask
#
#     import sqlalchemy.ext.declarative
#     from sqlalchemy.orm import declarative_base, sessionmaker
#
#     Base = declarative_base()
#
#     Column = sqlalchemy.Column
#     ForeignKey = sqlalchemy.ForeignKey
#
#     SmallInteger = sqlalchemy.SmallInteger
#     Integer = sqlalchemy.Integer
#     BigInteger = sqlalchemy.BigInteger
#
#     Float = sqlalchemy.Float
#
#     DateTime = sqlalchemy.DateTime
#
#     String = sqlalchemy.String
#     Boolean = sqlalchemy.Boolean
#     LargeBinary = sqlalchemy.LargeBinary


# if cx_Oracle:
#     from sqlalchemy.dialects import oracle
#
#     SmallInteger = oracle.NUMBER
#     Integer = oracle.NUMBER
#     BigInteger = oracle.NUMBER
#
#     Numeric = oracle.NUMBER
#     Float = oracle.NUMBER
#
#     # unlike default "Float", INSERT works correctly with IDENTITY columns like
#     # g_id = Column('G_ID', NUMBER, primary_key=True, autoincrement=True)
#     NUMBER = oracle.NUMBER
#
#     # https://stackoverflow.com/questions/64903159/convert-oracle-datatypes-to-sqlalchemy-types
#     # https://docs.sqlalchemy.org/en/14/dialects/oracle.html
#     # Provide the oracle DATE type.
#     #     This type has no special Python behavior, except that it subclasses
#     #     :class:`_types.DateTime`; this is to suit the fact that the Oracle
#     #     ``DATE`` type supports a time value.
#     DateTime = oracle.DATE  # (timezone=False)


class _DS(DataStore):
    class EngineType:
        sqlite3 = 1
        mysql = 2
        postgresql = 3
        oracle = 4

    # def __init__(self): # constructor for SQLAlchemy without Flask
    #     self.conn = None
    #     self.transaction = None
    #     self.engine = None
    #     #########################################
    #     # code below is for SQLAlchemy without flask
    #     #
    #     # self.engine = sqlalchemy.create_engine('sqlite:///todolist.sqlite')
    #     self.engine_type = self.EngineType.sqlite3
    #
    #     # self.engine = sqlalchemy.create_engine('postgresql://postgres:sa@localhost/my-tests')
    #     # self.engine_type = self.EngineType.postgresql
    #
    #     # https://www.tutorialguruji.com/dbms/how-do-i-execute-a-mysql-stored-procedure-in-a-sqlalchemy-scoped-session-to-return-a-single-result-set-of-data-for-flask-web-app/
    #     # self.engine = sqlalchemy.create_engine('mysql+mysqlconnector://root:root@localhost/sakila')
    #     # self.engine_type = self.EngineType.mysql
    #
    #     # user = 'MY_TESTS'
    #     # pwd = 'sa'
    #     # dsn = cx_Oracle.makedsn(
    #     #     'localhost', 1521,
    #     #     service_name="orcl"
    #     #     # service_name='your_service_name_if_any'
    #     # )
    #     # self.engine = sqlalchemy.create_engine(f'oracle+cx_oracle://{user}:{pwd}@{dsn}', echo=False)
    #     # self.engine_type = self.EngineType.oracle
    #
    #     # self.session = sessionmaker(bind=self.engine)()

    def __init__(self, db: flask_sqlalchemy.SQLAlchemy):
        self.conn = None
        self.transaction = None
        self.engine = None
        self.engine_type = self.EngineType.sqlite3
        self.session = db.session

    # code below is for SQLAlchemy without flask
    #
    # def open(self):
    #     self.conn = self.engine.connect()
    #
    # def close(self):
    #     if self.conn:
    #         self.conn.close()
    #         self.conn = None

    def begin(self):
        if self.transaction is None:
            # https://docs.sqlalchemy.org/en/14/orm/session_transaction.html
            self.session.begin()
            return
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

    def get_all_raw(self, cls, params=None) -> []:
        # https://stackoverflow.com/questions/17972020/how-to-execute-raw-sql-in-flask-sqlalchemy-app
        # user = session.query(User).from_statement(
        #     text("""SELECT * FROM users where name=:name""")
        # ).params(name="ed").all()

        # query = self.ds.engine.execute(GroupExModel.SQL) # it returns an array of tuples
        # return query.all()
        """
        :param cls: An __abstract_ model class or plain DTO class containing a static field "SQL"
        :param params: [] the values of SQL params
        :return: [dict]: an array of dict like [{'g_id': 21, 'g_name': 'Project 1'}, {'g_id': 22, 'g_name': 'Project 2']
        """
        # rows = self.engine.execute(cls.SQL)  # .fetchall()
        # performs -->
        # connection = self.connect(close_with_result=True) ---- no need because of connected
        if params is None:
            params = []

        # with self._exec(cls.SQL, params) as rows: # .fetchall() -- AttributeError: __enter__
        cursor = self._exec(cls.SQL, params)
        try:
            # https://stackoverflow.com/questions/31750441/generalised-insert-into-sqlalchemy-using-dictionary
            # https://stackoverflow.com/questions/3451779/how-to-dynamically-create-an-instance-of-a-class-in-python
            # https://stackoverflow.com/questions/1958219/how-to-convert-sqlalchemy-row-object-to-a-python-dict
            res = [cls(**dict(row)) for row in cursor]
            return res
        finally:
            cursor.close()

    def get_one_raw(self, cls, params=None):
        rows = self.get_all_raw(cls, params)
        if len(rows) == 0:
            raise Exception('No rows')
        if len(rows) > 1:
            raise Exception('More than 1 row exists')
        return rows[0]

    def filter(self, cls, params=None):
        if params:
            found = self.session.query(cls).filter_by(**params)
        else:
            found = self.session.query(cls)
        return found

    def delete_by_filter(self, cls, params=None) -> int:
        found = self.filter(cls, params)
        #  :return: the count of rows matched as returned by the database's
        #           "row count" feature.
        return found.delete()  # found is a BaseQuery, no fetch!

    def create_one(self, instance) -> None:
        self.session.add(instance)  # return None
        self.session.flush()

    def read_all(self, cls) -> []:
        return self.session.query(cls).all()

    def read_one(self, cls, params=None):
        return self.session.query(cls).get(params)

    def update_one(self, instance):
        self.session.flush()

    def delete_one(self, cls, params=None) -> int:
        # found = self.read_one(cls, params) # found is an entity of class cls
        # self.session.delete(found)
        rc = self.delete_by_filter(cls, params)  # no fetch!
        self.session.flush()
        return rc

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
        cursor = self._exec(sql, params)
        try:
            if len(ai_values) > 0:
                if self.engine_type == self.EngineType.postgresql:
                    ai_values[0][1] = cursor.fetchone()[0]
                else:
                    ai_values[0][1] = cursor.lastrowid
            if cursor.rowcount == 0:
                raise Exception('No rows inserted')
        finally:
            cursor.close()

    def _exec(self, sql, params):
        """
        :param sql:
        :param params:
        :return: <sqlalchemy.engine.cursor.LegacyCursorResult object at 0x00000243D83C5D00>
        """
        pp = tuple(params)
        txt = sql  # don't use sqlalchemy.text(sql) with '%' as params
        return self.session.execute(txt, pp)

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
        try:
            if len(out_params) > 0:
                row0 = cursor.fetchone()
                i = 0
                for value in row0:
                    out_params[i].value = value
                    i += 1
        finally:
            cursor.close()

    def _exec_sp_mysql(self, sp, params):
        call_params = _get_call_params(params)
        # https://stackoverflow.com/questions/45979950/sqlalchemy-error-when-calling-mysql-stored-procedure
        raw_conn = self.session.raw_connection()
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
        raw_conn = self.session.raw_connection()
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
        sql = self._format_sql(sql)
        sp = _get_sp_name(sql)
        if sp is None:
            cursor = self._exec(sql, params)
            try:
                return cursor.rowcount
            finally:
                cursor.close()
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
        sp = _get_sp_name(sql)
        if sp is None:
            cursor = self._exec(sql, params)
            try:
                for row in cursor:
                    res.append(row[0])
                return res
            finally:
                cursor.close()
        if self.engine_type != self.EngineType.mysql:
            raise Exception('Not supported for this engine')

        def on_result(result):
            for row_values in result:
                res.append(row_values[0])

        self._query_sp_mysql(sp, on_result, params)
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
        sp = _get_sp_name(sql)
        if sp is None:
            cursor = self._exec(sql, params)
            try:
                for row in cursor:
                    callback(row)
                return
            finally:
                cursor.close()
        if self.engine_type != self.EngineType.mysql:
            raise Exception('Not supported for this engine')
        self._query_sp_mysql(sp, lambda result: _fetch_all(result, callback), params)

    def _format_sql(self, sql):
        if self.engine_type == self.EngineType.sqlite3:
            return sql
        return sql.replace("?", "%s")


def _fetch_all(cursor, callback):
    # https://stackoverflow.com/questions/34030020/mysql-python-connector-get-columns-names-from-select-statement-in-stored-procedu
    # https://kadler.github.io/2018/01/08/fetching-python-database-cursors-by-column-name.html#
    for r in cursor:
        # https://stackoverflow.com/questions/1958219/how-to-convert-sqlalchemy-row-object-to-a-python-dict
        # How to convert SQLAlchemy row object to a Python dict?
        row = dict(r)
        # i = 0
        # for d in result.description:
        #     col_name = d[0]
        #     value = r[i]
        #     row[col_name] = value
        #     i = i + 1
        callback(row)


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
