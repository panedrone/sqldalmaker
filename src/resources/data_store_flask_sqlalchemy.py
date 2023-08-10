"""
    This file is a part of SQL DAL Maker project: https://sqldalmaker.sourceforge.net
    It demonstrates how to implement an interface DataStore in Python/Flask-SQLAlchemy.
    More about DataStore: https://sqldalmaker.sourceforge.net/preconfig.html#ds
    Recent version: https://github.com/panedrone/sqldalmaker/blob/master/src/resources/data_store_flask_sqlalchemy.py

    Successfully tested with:
        - sqlite3 ---------------- built-in
        - postgresql ------------- pip install psycopg2
        - mysql+mysqlconnector --- pip install mysql-connector-python
        - cx_Oracle -------------- pip install cx_oracle

    Copy-paste it to your project and change it for your needs.
    Improvements are welcome: sqldalmaker@gmail.com
    Demo project: https://github.com/panedrone/sdm_demo_todolist_flask_sqlalchemy

"""

import flask_sqlalchemy
import sqlalchemy.orm


# from sqlalchemy.dialects import oracle

class OutParam:
    def __init__(self):
        self.value = None


class DataStore:

    def begin(self):
        pass

    def commit(self):
        pass

    def rollback(self):
        pass

    # ORM-based raw-SQL helpers

    def get_one_raw(self, cls, params=None):
        """
        :param cls: a model class containing static field SQL
        :param params: a tuple of SQL params
        :return: a model object or error string
        """
        pass

    def get_all_raw(self, cls, params=None) -> []:
        """
        :param cls: a model class containing static field SQL
        :param params: a tuple of SQL params
        :return: an array of model objects
        """
        pass

    # ORM-based helpers

    def get_query(self, cls):
        """
        :param cls: a model class
        """
        pass

    def filter(self, cls, params: dict):
        """
        :param cls: a model class
        :param params: dict of named filter params
        :return: a QuerySet
        """
        pass

    def delete_by_filter(self, cls, params: dict) -> int:
        """
        :param cls: a model class
        :param params: dict of named filter params
        :return: amount of rows affected
        """
        pass

    def update_by_filter(self, cls, data: dict, params: dict) -> int:
        """
        :param cls: a model class
        :param data: dict of column-value to update
        :param params: dict of filter params
        :return: amount of rows affected
        """
        pass

    # ORM-based CRUD

    def create_one(self, entity) -> None:
        """
        :param entity: a model object or serializer object
        :return: None
        """
        pass

    def read_all(self, cls) -> []:
        """
        :param cls: a model class
        :return: a list model objects
        """
        pass

    def read_one(self, cls, pk: dict):
        """
        :param cls: a model class
        :param pk: primary key as a dict of column-value pairs
        :return: a model object
        """
        pass

    def update_one(self, cls, data: dict, pk: dict) -> int:
        """
        :param cls: model class
        :param data: dict of column-value to update
        :param pk: primary key as a dict of column-value pairs
        :return: int, amount of rows affected
        """
        pass

    def delete_one(self, cls, pk: dict) -> int:
        """
        :param cls: model class
        :param pk: primary key as a dict of column-value pairs
        :return: int, amount of rows affected
        """
        pass

    # ORM-based methods for raw-SQL

    # === raw-SQL INSERT is not used with sqlalchemy: def insert_row(self, sql, params, ai_values): pass

    def exec_dml(self, sql, params) -> int:
        """
        :param sql: str, SQL statement
        :param params: dict, optional, SQL parameters
        :return: int: amount of affected rows
        """
        pass

    def query_scalar(self, sql, params):
        """
        :param sql: str, SQL statement
        :param params: dict, optional, SQL parameters
        :return single scalar value
        :raise Exception: if amount of fetched rows != 1
        """
        pass

    def query_all_scalars(self, sql, params) -> []:
        """
        :param sql: str, SQL statement
        :param params: dict, optional, SQL parameters
        :return array of scalar values
        """
        pass

    def query_row(self, sql, params):
        """
        :param sql: str, SQL statement
        :param params: dict, optional, SQL parameters
        :return single fetched row or error string
        """
        pass

    def query_all_rows(self, sql, params, callback):
        """
        :param sql: str, SQL statement
        :param params: dict, optional, SQL parameters.
        :param callback: Ð° function delivering fetched rows to caller
        :return: None
        """
        pass


Base = None

Column = None
ForeignKey = None

SmallInteger = None
Integer = None
BigInteger = None

Float = None

DateTime = None

String = None
Boolean = None
LargeBinary = None


#
# How to pre-configure flask_sqlalchemy (do it somewhere in __main__):
#
# flask_app = flask.Flask(__name__)
#
# dir_path = os.path.dirname(os.path.realpath(__file__))
# flask_app.config['SQLALCHEMY_DATABASE_URI'] = f"sqlite:///{dir_path}/todolist.sqlite"
#
# # add mysql-connector-python to requirements.txt
# # app.config['SQLALCHEMY_DATABASE_URI'] = 'mysql+mysqlconnector://root:sa@localhost/todolist'
#
# # add psycopg2 to requirements.txt
# # flask_app.config['SQLALCHEMY_DATABASE_URI'] = 'postgresql://postgres:sa@localhost/my-tests'
#
# # add cx_oracle to requirements.txt
# # if oracle:
# #     user = 'MY_TESTS'
# #     pwd = 'sa'
# #     dsn = cx_Oracle.makedsn(
# #         'localhost', 1521,
# #         service_name="orcl"
# #         # service_name='your_service_name_if_any'
# #     )
# # flask_app.config['SQLALCHEMY_DATABASE_URI'] = f'oracle+cx_oracle://{user}:{pwd}@{dsn}'
#
# # FSADeprecationWarning: SQLALCHEMY_TRACK_MODIFICATIONS adds
# # significant overhead and will be disabled by default in the future.
# # Set it to True or False to suppress this warning.
# flask_app.config['SQLALCHEMY_TRACK_MODIFICATIONS'] = False
#
# db = flask_sqlalchemy.SQLAlchemy(flask_app)
#
# init_ds(db)  # ===== call it only once at app start

def init_ds(db: flask_sqlalchemy.SQLAlchemy):
    _DS.orm_scoped_session = db.session

    global Base, Column, ForeignKey, \
        SmallInteger, Integer, BigInteger, Float, DateTime, String, Boolean, LargeBinary

    Base = db.Model

    Column = db.Column
    ForeignKey = db.ForeignKey

    # if oracle:
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
    #
    #     String = oracle.NVARCHAR
    #     Boolean = oracle.LONG
    #     LargeBinary = oracle.BLOB
    # else:
    SmallInteger = db.SmallInteger
    Integer = db.Integer
    BigInteger = db.BigInteger

    Float = db.Float

    DateTime = db.DateTime

    String = db.String
    Boolean = db.Boolean
    LargeBinary = db.LargeBinary


def scoped_ds() -> DataStore:
    return _DS()


class _DS(DataStore):
    class EngineType:
        sqlite3 = 1
        mysql = 2
        postgresql = 3
        oracle = 4

    orm_scoped_session: sqlalchemy.orm.scoped_session

    # ^^ orm_scoped_session is a static field. It is assigned only once at app start, and it happens in here:
    #
    #       def init_ds(db: flask_sqlalchemy.SQLAlchemy):
    #           _DS.orm_scoped_session = db.session
    #
    # ----------------------------------------------------------------------------------------------
    #
    # ======= Using of "scoped_session":
    #
    # https://docs.sqlalchemy.org/en/13/orm/contextual.html
    #
    # >>> session_factory = sessionmaker(bind=some_engine)
    # >>> Session = scoped_session(session_factory)
    #
    # The scoped_session object we've created will now call upon the sessionmaker when we "call" the registry:
    #
    # >>> some_session = Session()
    #
    # ----------------------------------------------------------------------------------------------
    #
    # === panedrone:
    #
    #   1) ^^^ "some_session" is of type "sqlalchemy.orm.Session"
    #   2) no need to call some_session.close() yourself because it is performed automatically in here:
    #           scoped_session.registry.remove()

    def __init__(self):

        self.orm_session: sqlalchemy.orm.session = _DS.orm_scoped_session()

        conn = self.orm_session.connection()

        driver_name = conn.engine.url.drivername.lower()

        if 'sqlite' in driver_name:
            self.engine_type = self.EngineType.sqlite3
            return
        if 'mysql' in driver_name:
            self.engine_type = self.EngineType.mysql
            return
        if 'postgresql' in driver_name:
            self.engine_type = self.EngineType.postgresql
            return
        if 'oracle' in driver_name:
            self.engine_type = self.EngineType.oracle
            return

        raise Exception('Unexpected: ' + driver_name)

    def begin(self):
        # https://docs.sqlalchemy.org/en/14/orm/session_transaction.html
        self.orm_session.begin()

    def commit(self):
        self.orm_session.commit()

    def rollback(self):
        # https://docs.sqlalchemy.org/en/14/orm/session_basics.html
        self.orm_session.rollback()

    def get_all_raw(self, cls, params=None) -> []:
        """
        :param cls: An __abstract_ model class or plain DTO class containing a static field "SQL"
        :param params: []: the values of SQL params
        :return: [cls]: an array of of 'cls' objects
        """
        if params is None:
            params = []

        cursor_result = self._exec(cls.SQL, params)
        try:
            cursor = cursor_result.cursor
            # === panedrone: lower() is required because of oracle column names are always in upper case
            col_names = [tup[0].lower() for tup in cursor.description]
            res = []
            for row in cursor_result:
                row_values = [i for i in row]
                row_as_dict = dict(zip(col_names, row_values))
                r = cls(**dict(row_as_dict))
                res.append(r)
            return res
        finally:
            cursor_result.close()

    def get_one_raw(self, cls, params=None):
        rows = self.get_all_raw(cls, params)
        if len(rows) == 1:
            return rows[0]
        if len(rows) == 0:
            return 'No rows'
        return 'More than 1 row exists'

    def get_query(self, cls):
        return self.orm_session.query(cls)

    def filter(self, cls, params: dict):
        return self.orm_session.query(cls).filter_by(**params)

    def delete_by_filter(self, cls, params: dict) -> int:
        found = self.filter(cls, params)
        #  :return: the count of rows matched as returned by the database's
        #           "row count" feature.
        return found.delete()  # found is a BaseQuery, no fetch!

    def update_by_filter(self, cls, data: dict, params: dict) -> int:
        found = self.filter(cls, params)
        return found.update(values=data)  # found is a BaseQuery, no fetch!

    def create_one(self, entity) -> None:
        self.orm_session.add(entity)  # return None
        self.orm_session.flush()

    def read_all(self, cls):
        return self.orm_session.query(cls).all()

    def read_one(self, cls, pk: dict):
        return self.orm_session.query(cls).get(pk)

    def update_one(self, cls, data: dict, pk: dict) -> int:
        rc = self.update_by_filter(cls, data, pk)
        self.orm_session.flush()
        return rc

    def delete_one(self, cls, pk: dict) -> int:
        # found = self.read_one(cls, params) # found is an entity of class cls
        # self.orm_session.delete(found)
        rc = self.delete_by_filter(cls, pk)  # no fetch!
        self.orm_session.flush()
        return rc

    def _exec(self, sql, params):
        """
        :param sql:
        :param params:
        :return:
                    "sqlalchemy.engine.cursor.CursorResult" while using "txt = sqlalchemy.text(sql)" and
                    "sqlalchemy.engine.cursor.LegacyCursorResult" while using "txt = sql"
        """
        pp = tuple(params)

        txt = sqlalchemy.text(sql)

        # while using "txt = sql", "execute(txt, pp): throws randomly this:
        #
        #       sqlalchemy.exc.ArgumentError: Textual SQL expression should be explicitly declared as text()

        # txt = sql

        return self.orm_session.execute(txt, pp)

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
        call_params = self._get_call_params(params)
        # https://stackoverflow.com/questions/45979950/sqlalchemy-error-when-calling-mysql-stored-procedure
        raw_conn = self.orm_session.connection()
        try:
            with raw_conn.cursor() as cursor:
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
        finally:
            raw_conn.close()

    def _query_sp_mysql(self, sp, on_result, params):
        call_params = self._get_call_params(params)
        # https://stackoverflow.com/questions/45979950/sqlalchemy-error-when-calling-mysql-stored-procedure
        raw_conn = self.orm_session.connection()
        try:
            with raw_conn.cursor() as cursor:
                # result_args: https://pynative.com/python-mysql-execute-stored-procedure/
                result_args = cursor.callproc(sp, call_params)
                for result in cursor.stored_results():
                    on_result(result)
                self._assign_out_params(params, result_args)
        finally:
            raw_conn.close()

    def exec_dml(self, sql, params) -> int:
        sql = self._format_sql(sql)
        sp = self._get_sp_name(sql)
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
        if sp is None:
            cursor_result = self._exec(sql, params)
            try:
                cursor = cursor_result.cursor
                # === panedrone:
                #       the same logic is used in "get_all_raw(...",
                #       but "tup[0].lower()" should not be used in "query_all_rows(..."
                #       because "col_names" must be used as-is:
                col_names = [tup[0] for tup in cursor.description]
                for row in cursor_result:
                    row_values = [i for i in row]
                    row_as_dict = dict(zip(col_names, row_values))
                    callback(row_as_dict)
                return
            finally:
                cursor_result.close()
        if self.engine_type != self.EngineType.mysql:
            raise Exception('Not supported for this engine')
        self._query_sp_mysql(sp, lambda result: self._fetch_all(result, callback), params)

    def _format_sql(self, sql):
        if self.engine_type == self.EngineType.sqlite3:
            return sql
        return sql.replace("?", "%s")

    @staticmethod
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
