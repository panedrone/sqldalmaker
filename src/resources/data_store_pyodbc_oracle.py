import pyodbc


class OutParam:
    def __init__(self, value=None):
        self.value = value


class DataStore:
    """
    SQL DAL Maker Website: https://sqldalmaker.sourceforge.net/
    This is an example of how to implement DataStore in Python + pyodbc + Oracle.
    Recent version: https://github.com/panedrone/sqldalmaker/blob/master/src/resources/data_store_pyodbc_oracle.py
    Copy-paste this code to your project and change it for your needs.
    Improvements are welcome: sqldalmaker@gmail.com
    """
    conn = None

    def open(self):
        self.conn = pyodbc.connect('Driver={Oracle in OraDB12Home1};uid=ORDERS;pwd=sa')

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
                pass
                # ai_values[0][1] = result.fetchone()[0] TODO: implement and test
            if cursor.rowcount == 0:
                raise Exception('No rows inserted')
        finally:
            cursor.close()

    def exec_dml(self, sql, params):
        """
        Arguments:
            sql: SQL statement.
            params: Values of SQL parameters.
        Returns:
            Number of updated rows.
        """
        sp_sql = _get_sp_sql(sql)
        if sp_sql is not None:
            sql = sp_sql
        cursor = self.conn.cursor()
        try:
            cursor.execute(sql, params)
            return cursor.rowcount
        finally:
            cursor.close()

    def query_scalar(self, sql, params):
        """
        Returns:
            Single scalar value.
        Arguments:
            sql: SQL statement.
            params: Values of SQL parameters if needed.
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
            sql: SQL statement.
            params: Values of SQL parameters if needed.
        """
        sp_sql = _get_sp_sql(sql)
        if sp_sql is not None:
            sql = sp_sql
        res = []
        # https://github.com/mkleehammer/pyodbc/wiki/Calling-Stored-Procedures
        cursor = self.conn.cursor()
        try:
            rc = cursor.execute(sql, params)
            rows = cursor.fetchall()
            while rows:
                for r in rows:
                    res.append(r[0])
                if cursor.nextset():
                    rows = cursor.fetchall()
                else:
                    rows = None
        finally:
            cursor.close()

        return res

    def query_single_row(self, sql, params):
        """
        Returns:
            Single row
        Arguments:
            sql: SQL statement.
            params: Values of SQL parameters if needed.
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
            sql: SQL statement.
            params: Values of SQL parameters if needed.
            callback
        """
        sp_sql = _get_sp_sql(sql)
        if sp_sql is not None:
            sql = sp_sql
        # https://github.com/mkleehammer/pyodbc/wiki/Calling-Stored-Procedures
        cursor = self.conn.cursor()
        try:
            cursor.execute(sql, params)
            rows = cursor.fetchall()
            while rows:
                columns = [column[0] for column in cursor.description]
                for r in rows:
                    row_dict = {}
                    for ci in range(len(columns)):
                        row_dict[columns[ci]] = r[ci]
                    if len(row_dict) > 0:
                        callback(row_dict)
                if cursor.nextset():
                    rows = cursor.fetchall()
                else:
                    rows = None
        finally:
            cursor.close()


def _get_sp_sql(sql):
    parts = sql.split()
    if len(parts) >= 2 and parts[0].strip().lower() == "call":
        sp_name = parts[1].strip()
        return '{call ' + sp_name + '}'
    return None
