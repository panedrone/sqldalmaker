import sqlite3


class DataStore:
    """
    SQL DAL Maker Website: http://sqldalmaker.sourceforge.net
    This is an example of how to implement DataStore in Python + SQLite3.
    Recent version: https://github.com/panedrone/sqldalmaker/blob/master/src/resources/data_store_sqlite3.py
    Copy-paste this code to your project and change it for your needs.
    Improvements are welcome: sqldalmaker@gmail.com
    """

    def __init__(self):
        self._con = None

    def open(self):
        self._con = sqlite3.connect('./task-tracker.sqlite')

    def close(self):
        if self._con:
            self._con.close()
            self._con = None

    def start_transaction(self):
        self._con.execute('begin')

    def commit(self):
        self._con.execute('commit')

    def rollback(self):
        self._con.execute("rollback")

    def insert_row(self, sql, params, ai_values):
        cursor = self._con.cursor()  # no 'with' in sqlite3
        try:
            cursor.execute(sql, params)
            if len(ai_values) > 0:
                ai_values[0][1] = cursor.lastrowid
            if cursor.rowcount == 0:
                raise Exception('No rows inserted')
        finally:
            cursor.close()

    def exec_dml(self, sql, params):
        cursor = self._con.cursor()  # no 'with' in sqlite3
        try:
            cursor.execute(sql, params)
            return cursor.rowcount
        finally:
            cursor.close()

    def query_scalar(self, sql, params):
        arr = self.query_scalar_array(sql, params)
        if len(arr) == 0:
            raise Exception('No rows')
        if len(arr) > 1:
            raise Exception('More than 1 row exists')
        return arr[0]

    def query_scalar_array(self, sql, params):
        res = []
        cursor = self._con.cursor()  # no 'with' in sqlite3
        try:
            cursor.execute(sql, params)
            row = cursor.fetchone()
            while row is not None:
                res.append(row[0])
                row = cursor.fetchone()
            return res
        finally:
            cursor.close()

    def query_single_row(self, sql, params):
        rows = []
        self.query_all_rows(sql, params, lambda row: rows.append(row))
        if len(rows) == 0:
            raise Exception('No rows')
        if len(rows) > 1:
            raise Exception('More than 1 row exists')
        return rows[0]

    def query_all_rows(self, sql, params, callback):
        # http://zetcode.com/db/sqlitepythontutorial/
        # We select a dictionary cursor. Now we can access records by the names of columns.
        self._con.row_factory = sqlite3.Row
        # An alternative:
        # http://stackoverflow.com/questions/3300464/how-can-i-get-dict-from-sqlite-query
        # def dict_factory(cursor, row):
        # d = {}
        #     for idx, col in enumerate(cursor.description):
        #         d[col[0]] = row[idx]
        #     return d
        # self._con.row_factory = dict_factory

        # === panedrone: seems that 'with' statement not working in sqlite3
        # SQLite cursor in Python with statement
        # https://stackoverflow.com/questions/16668623/sqlite-cursor-in-python-with-statement

        cursor = self._con.cursor()  # no 'with' in sqlite3
        try:
            cursor.execute(sql, params)
            row = cursor.fetchone()
            while row is not None:
                callback(row)
                row = cursor.fetchone()
        finally:
            cursor.close()
