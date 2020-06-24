import psycopg2


class OutParam:
    def __init__(self):
        self.value = None

    def __init__(self, value):
        self.value = value


class DataStore:
    """
    SQL DAL Maker Website: http://sqldalmaker.sourceforge.net
    Contact: sqldalmaker@gmail.com

    This is an example of how to implement DataStore in Python + psycopg2.
    Copy-paste this code to your project and change it for your needs.
    """
    conn = None

    def open(self):
        self.conn = psycopg2.connect(host="localhost", database="test", user="postgres", password="sa")
        # print(self.conn.autocommit)

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
        sql = _format_sql(sql)

        # http://zetcode.com/python/psycopg2/
        if len(ai_values) > 0:
            sql += ' RETURNING ' + ai_values[0][0]
        cursor = self.conn.cursor()
        try:
            cursor.execute(sql, params)
            if len(ai_values) > 0:
                ai_values[0][1] = cursor.fetchone()[0]
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
        sp_sql = _get_sp_sql(sql, params)
        if sp_sql is not None:
            sql = sp_sql
        sql = _format_sql(sql)
        cursor = self.conn.cursor()
        try:
            if sp_sql is None:
                cursor.execute(sql, params)
                return cursor.rowcount
            else:
                out_params = []
                call_params = []
                for p in params:
                    if isinstance(p, OutParam):
                        call_params.append(p.value)
                        out_params.append(p)
                    else:
                        call_params.append(p)
                    cursor.execute(sql, call_params)
                    row0 = cursor.fetchone()
                    i = 0
                    for value in row0:
                        out_params[i].value = value
                        i += 1
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
        sp_sql = _get_sp_sql(sql, params)
        if sp_sql is not None:
            sql = sp_sql
        sql = _format_sql(sql)
        res = []
        cursor = self.conn.cursor()
        try:
            cursor.execute(sql, params)
            rows = cursor.fetchall()
            for r in rows:
                res.append(r[0])
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
        sp_sql = _get_sp_sql(sql, params)
        if sp_sql is not None:
            sql = sp_sql
        sql = _format_sql(sql)
        cursor = self.conn.cursor()
        try:
            cursor.execute(sql, params)
            rows = cursor.fetchall()
            columns = [column[0] for column in cursor.description]
            for r in rows:
                row_dict = {}
                for ci in range(len(columns)):
                    row_dict[columns[ci]] = r[ci]
                callback(row_dict)
        # if cursor.nextset(): # psycopg2.NotSupportedError: not supported by PostgreSQL
        finally:
            cursor.close()


def _get_sp_sql(sql, params):
    parts = sql.split()
    if len(parts) >= 2 and parts[0].strip().lower() == "call":
        return sql
    return None


def _format_sql(sql):
    return sql.replace('?', '%s')
