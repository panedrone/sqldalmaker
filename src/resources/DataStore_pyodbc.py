import pyodbc


class DataStore:
    """
    SQL DAL Maker Website: http://sqldalmaker.sourceforge.net
    Contact: sqldalmaker@gmail.com

    This is an example of how to implement DataStore in Python + pyodbc.
    Copy-paste this code to your project and change it for your needs.
    """

    def __init__(self):
        self.connection = None

    def open(self):
        self.connection = pyodbc.connect('Driver={SQL Server};'
                                         'Server=localhost\\SQLEXPRESS;'
                                         'Database=AdventureWorks2014;'
                                         'Trusted_Connection=yes;')

    def close(self):
        if self.connection:
            self.connection.close()

    def start_transaction(self):
        # https://stackoverflow.com/questions/6477992/using-begin-transaction-rollback-commit-over-various-cursors-connections
        self.connection.begin()

    def commit(self):
        self.connection.commit()

    def rollback(self):
        self.connection.rollback()

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
        cursor = self.connection.cursor()

        try:

            result = cursor.execute(sql, params)

            if len(ai_values) > 0:
                # https://www.reddit.com/r/learnpython/comments/1h78gi/pyodbc_get_last_inserted_id/
                ai_values[0][1] = cursor.execute('SELECT @@IDENTITY AS id;').fetchone()[0]
                # ai_values[0][1] = result.fetchone()[0]

            if cursor.rowcount == 0:
                raise Exception('No rows inserted')

        finally:
            cursor.close()

    def exec_dml(self, sql, params):
        """
        Arguments:
            sql (string): SQL statement.
            params (array, optional): Values of SQL parameters.
        Returns:
            Number of updated rows.
        """
        cursor = self.connection.cursor()

        try:

            sp_sql = _get_sp_sql(sql, params)

            if sp_sql is not None:
                sql = sp_sql

            cursor.execute(sql, params)

            return cursor.rowcount

        finally:
            cursor.close()

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
        sp_sql = _get_sp_sql(sql, params)

        if sp_sql is not None:
            sql = sp_sql

        res = []

        cursor = self.connection.cursor()

        try:
            cursor.execute(sql, params)
            for row in cursor.fetchall():
                res.append(row[0])
        finally:
            cursor.close()

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
            sql (string): SQL statement.
            params (array, optional): Values of SQL parameters if needed.
            callback
        """
        sp_sql = _get_sp_sql(sql, params)

        if sp_sql is not None:
            sql = sp_sql

        cursor = self.connection.cursor()

        try:
            cursor.execute(sql, params)
            columns = [column[0] for column in cursor.description]
            for row in cursor.fetchall():
                row2 = {}
                for i in range(len(columns)):
                    row2[columns[i]] = row[i]
                callback(row2)
        finally:
            cursor.close()


def _get_sp_sql(sql, params):
    parts = sql.split()

    if len(parts) >= 2 and parts[0].strip().lower() == "call":
        sp_name = parts[1].strip()
        if len(params) == 0:
            return '{call ' + sp_name + '}'
        else:
            pp = ['?' for _ in range(len(params))]
            pp = ', '.join(pp)
            return '{call ' + sp_name + '(' + pp + ')}'

    return None
