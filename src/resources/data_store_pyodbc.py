import pyodbc


class OutParams:
    pass


class DataStore:
    """
    SQL DAL Maker Website: http://sqldalmaker.sourceforge.net
    This is an example of how to implement DataStore in Python + pyodbc + SQL Server.
    Recent version: https://github.com/panedrone/sqldalmaker/blob/master/src/resources/data_store_pyodbc.py
    Copy-paste this code to your project and change it for your needs.
    Improvements are welcome: sqldalmaker@gmail.com
    """
    conn = None

    def open(self):
        # self.conn = pyodbc.connect('DRIVER={CData ODBC Driver for PostgreSQL};'
        #                            'User=postgres;Password=sa;Database=test;Server=127.0.0.1;Port=5432')

        # self.conn = pyodbc.connect('DRIVER={PostgreSQL Unicode};'
        #                            'UID=postgres;PWD=sa;DATABASE=test;Server=127.0.0.1;Port=5432')

        # self.conn = pyodbc.connect('Driver={SQL Server};Server=localhost\\SQLEXPRESS;'
        #                            'Database=AdventureWorks2014;Trusted_Connection=yes;')

        self.conn = pyodbc.connect('Driver={SQL Server Native Client 11.0};Server=localhost\\SQLEXPRESS;'
                                   'Database=AdventureWorks2014;Trusted_Connection=yes;')

    def close(self):
        if self.conn:
            self.conn.close()
            self.conn = None

    def start_transaction(self):
        self.conn.begin()

    def commit(self):
        self.conn.commit()

    def rollback(self):
        self.conn.rollback()

    def insert_row(self, sql, params, ai_values):
        """
        Returns:
            Nothing.
        Arguments:
            sql: SQL statement.
            params: Values of SQL parameters.
            ai_values: Array like [["o_id", 1], ...] for auto-increment values.
        Raises:
            Exception: if no rows inserted.
        """
        cursor = self.conn.cursor()
        try:
            cursor.execute(sql, params)
            if len(ai_values) > 0:
                # https://www.reddit.com/r/learnpython/comments/1h78gi/pyodbc_get_last_inserted_id/
                ai_values[0][1] = cursor.execute('SELECT @@IDENTITY AS id;').fetchone()[0]
                # ai_values[0][1] = result.fetchone()[0]
            if cursor.rowcount == 0:
                raise Exception('No rows inserted')
        finally:
            cursor.close()

    def exec_dml(self, sql, in_params):
        """
        Arguments:
            sql: SQL statement.
            in_params: Values of SQL parameters.
        Returns:
            Number of updated rows.
        """
        sp_sql = _get_sp_sql(sql, in_params)

        if sp_sql is not None:
            sql = sp_sql

        cursor = self.conn.cursor()
        try:
            cursor.execute(sql, in_params)
            return cursor.rowcount
        finally:
            cursor.close()

    def query_scalar(self, sql, in_params, out_params=None):
        """
        Returns:
            Single scalar value.
        Arguments:
            sql: SQL statement.
            in_params: Values of SQL parameters if needed.
            out_params: OutParams
        Raises:
            Exception: if amount of rows != 1.
        """
        rows = self.query_scalar_array(sql, in_params, out_params)

        if len(rows) == 0:
            raise Exception('No rows')

        if len(rows) > 1:
            raise Exception('More than 1 row exists')

        if isinstance(rows[0], list):
            return rows[0][0]
        else:
            return rows[0]  # 'select get_test_rating(?)' returns just scalar value, not array of arrays

    def query_scalar_array(self, sql, in_params, out_params=None):
        """
        Returns:
            array of scalar values
        Arguments:
            sql: SQL statement.
            in_params: Values of SQL parameters if needed.
            out_params: OutParams
        """
        sp_sql = _get_sp_sql(sql, in_params)

        if sp_sql is not None:
            sql = sp_sql

        res = []

        # https://github.com/mkleehammer/pyodbc/wiki/Calling-Stored-Procedures
        cursor = self.conn.cursor()
        try:
            cursor.execute(sql, in_params)
            rows = cursor.fetchall()
            while rows:
                columns = [column[0] for column in cursor.description]
                for r in rows:
                    for ci in range(len(columns)):
                        if hasattr(out_params, columns[ci]):
                            setattr(out_params, columns[ci], r[ci])
                        else:
                            if ci == 0:
                                res.append(r[0])
                if cursor.nextset():
                    rows = cursor.fetchall()
                else:
                    rows = None
        finally:
            cursor.close()

        return res

    def query_single_row(self, sql, in_params, out_params=None):
        """
        Returns:
            Single row
        Arguments:
            sql: SQL statement.
            in_params: Values of SQL parameters if needed.
            out_params: OutParams
        Raises:
            Exception: if amount of rows != 1.
        """
        rows = []

        def callback(row):
            rows.append(row)

        self.query_all_rows(sql, in_params, callback, out_params)

        if len(rows) == 0:
            raise Exception('No rows')

        if len(rows) > 1:
            raise Exception('More than 1 row exists')

        return rows[0]

    def query_all_rows(self, sql, in_params, callback, out_params=None):
        """
        Returns:
            None
        Arguments:
            sql: SQL statement.
            in_params: Values of SQL parameters if needed.
            callback
            out_params: OutParams
        """
        sp_sql = _get_sp_sql(sql, in_params)

        if sp_sql is not None:
            sql = sp_sql

        # https://github.com/mkleehammer/pyodbc/wiki/Calling-Stored-Procedures
        cursor = self.conn.cursor()
        try:
            cursor.execute(sql, in_params)
            rows = cursor.fetchall()
            while rows:
                columns = [column[0] for column in cursor.description]
                for r in rows:
                    row_dict = {}
                    for ci in range(len(columns)):
                        if hasattr(out_params, columns[ci]):
                            setattr(out_params, columns[ci], r[ci])
                        else:
                            row_dict[columns[ci]] = r[ci]
                    if len(row_dict) > 0:
                        callback(row_dict)
                if cursor.nextset():
                    rows = cursor.fetchall()
                else:
                    rows = None
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
