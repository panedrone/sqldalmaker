from mysql import connector


class DataStore:
    """
    SQL DAL Maker Website: http://sqldalmaker.sourceforge.net
    About DataStore: http://sqldalmaker.sourceforge.net/how-to-start.html
    Contact: sqldalmaker@gmail.com

    This is an example of how to implement DataStore in Python + MySQL.
    Copy-paste this code to your project and change it for your needs.
    """

    def __init__(self):
        self.connection = None

    def open(self):
        self.connection = connector.Connect(user='root', password='root',
                                            host='127.0.0.1',
                                            database='sakila')

    def close(self):
        if self.connection:
            self.connection.close()

    def start_transaction(self):
        self.connection.start_transaction()

    def commit(self):
        self.connection.commit()

    def rollback(self):
        self.connection.rollback()

    @staticmethod
    def prepare_sql(sql):
        """
        @rtype : str
        """
        return sql.replace("?", "%s")

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
        sql = self.prepare_sql(sql)

        cursor = self.connection.cursor()

        try:

            cursor.execute(sql, params)

            if len(ai_values) > 0:
                ai_values[0][1] = cursor.lastrowid

            if cursor.rowcount == 0:
                raise Exception('No rows inserted')

        finally:
            cursor.close()

    @staticmethod
    def get_sp_name(sql):

        parts = sql.split()

        if len(parts) >= 2 and parts[0].lower() == "call":
            name = parts[1]
            end = name.find("(")
            if end == -1:
                return name
            else:
                return name[0:end]

        return None

    def exec_dml(self, sql, params):
        """
        Arguments:
            sql (string): SQL statement.
            params (array, optional): Values of SQL parameters.
        Returns:
            Number of updated rows.
        """
        sql = self.prepare_sql(sql)

        cursor = self.connection.cursor()

        try:

            sp = self.get_sp_name(sql)

            if sp is None:
                cursor.execute(sql, params)
                return cursor.rowcount
            else:
                cursor.callproc(sp, params)
                return 0

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
        sql = self.prepare_sql(sql)

        res = []

        cursor = self.connection.cursor()

        try:

            sp = self.get_sp_name(sql)

            if sp is None:
                cursor.execute(sql, params)
                row = cursor.fetchone()
                while row is not None:
                    res.append(row[0])
                    row = cursor.fetchone()
            else:
                # http://www.mysqltutorial.org/calling-mysql-stored-procedures-python/
                cursor.callproc(sp, params)
                for result in cursor.stored_results():
                    row = result.fetchall()
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
        sql = self.prepare_sql(sql)

        # http://geert.vanderkelen.org/connectorpython-custom-cursors/
        # Fetching rows as dictionaries with MySQL Connector/Python

        # cursor = self.connection.cursor(cursor_class=MySQLCursorDict)

        # How to retrieve SQL result column value using column name in Python?
        # https://stackoverflow.com/questions/10195139/how-to-retrieve-sql-result-column-value-using-column-name-in-python

        cursor = self.connection.cursor(dictionary=True)

        try:

            sp = self.get_sp_name(sql)

            if sp is None:
                cursor.execute(sql, params)
                row = cursor.fetchone()
                while row is not None:
                    callback(row)
                    row = cursor.fetchone()
            else:
                # http://www.mysqltutorial.org/calling-mysql-stored-procedures-python/
                cursor.callproc(sp, params)

                for result in cursor.stored_results():
                    # cursor(dictionary=True) does not work here. workarounds:
                    # https://stackoverflow.com/questions/34030020/mysql-python-connector-get-columns-names-from-select-statement-in-stored-procedu
                    # https://kadler.github.io/2018/01/08/fetching-python-database-cursors-by-column-name.html#

                    row_values = result.fetchall()
                    row = {}
                    i = 0

                    for d in result.description:
                        col_name = d[0]
                        value = row_values[0][i]
                        row[col_name] = value
                        i = i + 1

                    callback(row)

        finally:
            cursor.close()
