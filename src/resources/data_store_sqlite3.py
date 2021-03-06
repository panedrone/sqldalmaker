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
        self._con = sqlite3.connect('dal/thesaurus.sqlite')

    def close(self):
        if self._con:
            self._con.close()

    def start_transaction(self):
        self._con.execute('begin')

    def commit(self):
        self._con.execute('commit')

    def rollback(self):
        self._con.execute("rollback")

    def insert_row(self, sql, params, ai_values):
        """
        Returns:
            None
        Arguments:
            param1 (string): SQL statement.
            param2 (array, optional): Values of SQL parameters.
            param2 (array, optional): Array like [["o_id", 1], ...] for auto-increment values.
        Raises:
            Exception: if no rows inserted.
        """
        cur = self._con.cursor()
        cur.execute(sql, params)
        if len(ai_values) > 0:
            ai_values[0][1] = cur.lastrowid
        if cur.rowcount == 0:
            raise Exception('No rows inserted')

    def exec_dml(self, sql, params):
        """
        Arguments:
            param1 (string): SQL statement.
            param2 (array, optional): Values of SQL parameters.
        Returns:
            Number of updated rows.
        """
        cur = self._con.cursor()
        cur.execute(sql, params)
        return cur.rowcount

    def query_scalar(self, sql, params):
        """
        Returns:
            Single scalar value.
        Arguments:
            param1 (string): SQL statement.
            param2 (array, optional): Values of SQL parameters if needed.
        Raises:
            Exception: if amount of rows != 1.
        """
        arr = self.query_scalar_array(sql, params)
        if len(arr) == 0:
            raise Exception('No rows')
        if len(arr) > 1:
            raise Exception('More than 1 row exists')
        return arr[0]

    def query_scalar_array(self, sql, params):
        """
        Returns:
            array of scalar values
        Arguments:
            param1 (string): SQL statement.
            param2 (array, optional): Values of SQL parameters if needed.
        """
        res = []
        cursor = self._con.cursor()
        cursor.execute(sql, params)
        row = cursor.fetchone()
        while row is not None:
            res.append(row[0])
            row = cursor.fetchone()
        return res

    def query_single_row(self, sql, params):
        """
        Returns:
            Single row
        Arguments:
            param1 (string): SQL statement.
            param2 (array, optional): Values of SQL parameters if needed.
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
            param1 (string): SQL statement.
            param2 (array, optional): Values of SQL parameters if needed.
        """

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
        cursor = self._con.cursor()
        cursor.execute(sql, params)
        row = cursor.fetchone()
        while row is not None:
            callback(row)
            row = cursor.fetchone()
