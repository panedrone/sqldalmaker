/*
    Copyright 2011-2026 sqldalmaker@gmail.com
    SQL DAL Maker Website: https://sqldalmaker.sourceforge.net/
    Read LICENSE.txt in the root of this project/archive for details.
 */
package com.sqldalmaker.cg;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
 * 10.10.2026 1.333 Claude: JDBC reading of JdbcTableInfo, JdbcSqlFieldInfo and JdbcSqlParamInfo
 *
 * IDbMetadata over a JDBC connection. JDBC calls only: the drivers are inconsistent,
 * so every attribute that may fail is read on its own, and a failed one is just null.
 */
class JdbcDbMetadata implements IDbMetadata {

    private final Connection conn;

    JdbcDbMetadata(Connection conn) {
        this.conn = conn;
    }

    // SQL statement ---------------------------------------------------

    @Override
    public List<Column> read_sql_columns(String jdbc_sql) throws Exception {
        List<Column> res = new ArrayList<>();
        PreparedStatement ps = JdbcUtils.prepare_jdbc_sql(conn, jdbc_sql);
        try {
            ResultSetMetaData rs_md = _get_rs_md(ps);
            int column_count = _get_col_count(rs_md);
            for (int col_num = 1; col_num <= column_count; col_num++) {
                String col_name = _get_jdbc_col_name(rs_md, col_num);
                String type_name = _get_jdbc_col_type_name(rs_md, col_num);
                // === panedrone: it is nullable for all columns except PK (sqlite3)
                // field.setNullable(rsmd.isNullable(col_num) == ResultSetMetaData.columnNullable);
                boolean is_ai = rs_md.isAutoIncrement(col_num);
                int scale;
                try {
                    scale = rs_md.getScale(col_num);
                } catch (SQLException e) {
                    scale = 0;
                }
                int precision;
                try {
                    precision = rs_md.getPrecision(col_num);
                } catch (SQLException e) {
                    precision = 0;
                }
                int display_size;
                try {
                    // 2147483647 for all cols (SQLite)
                    display_size = rs_md.getColumnDisplaySize(col_num);
                } catch (SQLException e) {
                    display_size = 0;
                }
                res.add(new Column(col_name, type_name, is_ai, scale, precision, display_size));
            }
        } finally {
            ps.close();
        }
        return res;
    }

    private static ResultSetMetaData _get_rs_md(PreparedStatement ps) throws Exception {
        ResultSetMetaData rsmd;
        try {
            rsmd = ps.getMetaData();
        } catch (SQLException e) {
            throw new Exception("Cannot detect Prepared Statement MetaData: " + e.getMessage());
        }
        if (rsmd == null) {  // it is possible by javadocs
            throw new Exception("Cannot detect Prepared Statement MetaData");
        }
        return rsmd;
    }

    private static int _get_col_count(ResultSetMetaData rsmd) throws Exception {
        int column_count;
        try {
            column_count = rsmd.getColumnCount();
        } catch (SQLException e) {
            throw new Exception("Exception in getColumnCount(): " + e.getMessage());
        }
        if (column_count < 1) {
            // Columns count is 0:
            // 1) for 'call my_sp(...)' including SP returning ResultSet (MySQL).
            // 2) for 'begin ?:=my_udf_rc(...); end;' (Oracle).
            // 3) for 'select my_func(?)' (PostgreSQL). etc.
            throw new Exception("getColumnCount() == " + column_count);
        }
        return column_count;
    }

    private static String _get_jdbc_col_name(ResultSetMetaData rsmd, int col_num) throws Exception {
        String column_name;
        try {
            column_name = rsmd.getColumnLabel(col_num);
        } catch (SQLException e) {
            column_name = null;
        }
        if (column_name == null || column_name.isEmpty()) {
            column_name = rsmd.getColumnName(col_num);
        }
        if (column_name == null) {
            throw new Exception(
                    "Cannot detect column name. Try to specify column label like 'select count(*) as res from ...'");
        }
        if (column_name.isEmpty()) {
            column_name = "col_" + col_num; // MS SQL Server: column_name == "" for 'select dbo.ufnLeadingZeros(?)'
        }
        return column_name;
    }

    private static String _get_jdbc_col_type_name(ResultSetMetaData rsmd, int col_num) {
        try {
            // sometime, it returns "[B": See comments for Class.getName() API
            String java_class_name = rsmd.getColumnClassName(col_num);
            return JavaTypes.refine_java_type_name(java_class_name);
        } catch (ClassNotFoundException | SQLException ex) {
            return Object.class.getName();
        }
    }

    // SQL parameters --------------------------------------------------

    @Override
    public Params read_sql_params(String jdbc_sql, int type_count) throws Exception {
        PreparedStatement ps = JdbcUtils.prepare_jdbc_sql(conn, jdbc_sql);
        try {
            // Sybase ADS + adsjdbc.jar --------------------
            // ps.getParameterMetaData() throws java.lang.AbstractMethodError for all
            // statements
            //
            // SQL Server 2008 + sqljdbc4.jar --------------
            // ps.getParameterMetaData() throws
            // com.microsoft.sqlserver.jdbc.SQLServerException for
            // some statements SQL with parameters like 'SELECT count(*) FROM orders o WHERE
            // o_date BETWEEN ? AND ?'
            // and for SQL statements without parameters
            //
            // PostgreSQL -----------------------------------
            // ps.getParameterMetaData() throws SQLException for both PreparedStatement and
            // CallableStatement
            int count;
            ParameterMetaData pm = null;
            try {
                // MS SQL Server: getParameterMetaData throws exception for SF without params
                pm = ps.getParameterMetaData();
                try {
                    count = pm.getParameterCount();
                } catch (Throwable e) { // including AbstractMethodError, SQLServerException, etc.
                    return new Params(true, 0, new ArrayList<String>());
                }
            } catch (Throwable e) { // including AbstractMethodError, SQLServerException, etc.
                count = 0;
            }
            List<String> java_types = new ArrayList<>();
            for (int i = 0; i < type_count; i++) {
                java_types.add(_get_jdbc_param_type_name(pm, i));
            }
            return new Params(false, count, java_types);
        } finally {
            ps.close();
        }
    }

    private static String _get_jdbc_param_type_name(ParameterMetaData pm, int i_0_n) {
        if (pm == null) {
            return Object.class.getName();
        }
        String java_class_name;
        try {
            // 1) getParameterClassName throws exception in
            // mysql-connector-java-5.1.17-bin.jar:
            // 2) sometime it returns "[B": See comments for Class.getName() API
            java_class_name = pm.getParameterClassName(i_0_n + 1);
            java_class_name = JavaTypes.refine_java_type_name(java_class_name);
        } catch (Exception ex) {
            java_class_name = Object.class.getName();
        }
        return java_class_name;
    }

    // Table -----------------------------------------------------------

    @Override
    public Table read_table(String table_ref) throws Exception {
        if (!SqlUtils.is_table_ref(table_ref)) {
            throw new Exception("Table name expected: " + table_ref);
        }
        _validate_table_name(table_ref); // Oracle PK are not detected with lower case table name
        String jdbc_sql = SqlUtils.jdbc_sql_by_table_name(table_ref);
        List<Column> columns = read_sql_columns(jdbc_sql);
        List<TableColumn> table_columns = _read_table_columns(table_ref);
        return new Table(columns, table_columns);
    }

    private void _validate_table_name(String table_name_pattern) throws Exception {
        // 1. Oracle PK are not detected with lower case table names
        // 2. it may include schema like "public.%"
        ResultSet rs = get_tables_rs(conn, table_name_pattern, true);
        try {
            if (rs.next()) {
                return;
            }
        } finally {
            rs.close();
        }
        throw new Exception("Not found: '" + table_name_pattern + "'. The search is case-sensitive.");
    }

    static ResultSet get_tables_rs(
            Connection conn,
            String table_name_pattern, // it may include schema like "public.%"
            boolean include_views) throws SQLException {

        String schema_name = null;
        if (table_name_pattern.contains(".")) {
            String[] parts = table_name_pattern.split("\\.");
            if (parts.length != 2) {
                throw new SQLException("Invalid table name: '" + table_name_pattern + "'");
            }
            schema_name = parts[0];
            table_name_pattern = parts[1];
        }
        String[] types;
        if (include_views) {
            types = new String[]{"TABLE", "VIEW"};
        } else {
            types = new String[]{"TABLE"};
        }
        DatabaseMetaData db_md = conn.getMetaData();
        String catalog = conn.getCatalog();
        return db_md.getTables(catalog, schema_name, table_name_pattern, types);
    }

    private List<TableColumn> _read_table_columns(String table_name) throws Exception {
        String schema_nm;
        String table_nm;
        String[] parts = table_name.split("\\.", -1); // -1 to leave empty strings
        if (parts.length == 1) {
            schema_nm = null;
            table_nm = table_name;
        } else {
            schema_nm = table_name.substring(0, table_name.lastIndexOf('.'));
            table_nm = parts[parts.length - 1];
        }
        Map<String, String> lower_case_unique = _index_names_by_lo_case_col_names(schema_nm, table_name, true);
        Map<String, String> lower_case_indexed = _index_names_by_lo_case_col_names(schema_nm, table_name, false);
        Map<String, String[]> lower_case_fk = _get_lower_case_fk_col_names(schema_nm, table_name);
        List<TableColumn> res = new ArrayList<>();
        DatabaseMetaData md = conn.getMetaData();
        ResultSet columns_rs = md.getColumns(conn.getCatalog(), schema_nm, table_nm, "%");
        try {
            while (columns_rs.next()) {
                String db_col_name;
                try {
                    db_col_name = columns_rs.getString("COLUMN_NAME");
                } catch (SQLException e) {
                    System.err.println("COLUMN_NAME: " + e.getMessage());
                    continue;
                }
                // Exception: Invalid column Info
                // Object is_gk = columns_rs.getObject("IS_GENERATEDCOLUMN"); // "NO" for oracle IDENTITY
                // if ("yes".equals(is_gk) || "YES".equals(is_gk)) {
                //      fi.setGenerated(true);
                String lo_case_col_name = db_col_name.toLowerCase();
                String fk = null;
                if (lower_case_fk.containsKey(lo_case_col_name)) {
                    String[] pk = lower_case_fk.get(lo_case_col_name);
                    fk = String.format("%s.%s", pk[0], pk[1]);
                }
                res.add(new TableColumn(db_col_name,
                        _get_int(columns_rs, "DATA_TYPE"),
                        _get_int(columns_rs, "COLUMN_SIZE"),
                        _get_int(columns_rs, "DECIMAL_DIGITS"),
                        _get_is_ai(columns_rs),
                        _get_is_nullable(columns_rs),
                        fk,
                        lower_case_indexed.containsKey(lo_case_col_name),
                        lower_case_unique.containsKey(lo_case_col_name)));
            }
        } finally {
            columns_rs.close();
        }
        return res;
    }

    private static Integer _get_int(ResultSet columns_rs, String name) {
        try {
            return columns_rs.getInt(name);
        } catch (SQLException e) {
            System.err.println(name + ": " + e.getMessage());
            return null;
        }
    }

    private static Boolean _get_is_ai(ResultSet columns_rs) {
        try {
            // IS_AUTOINCREMENT may work incorrectly (sqlite-jdbc-3.19.3.jar).
            // So, it is used only if AI was not set by SQL columns metadata
            Object is_ai = columns_rs.getObject("IS_AUTOINCREMENT");
            return "yes".equals(is_ai) || "YES".equals(is_ai);
        } catch (SQLException e) {
            System.err.println("IS_AUTOINCREMENT: " + e.getMessage());
            return null;
        }
    }

    private static Boolean _get_is_nullable(ResultSet columns_rs) {
        try {
            // http://www.java2s.com/Code/Java/Database-SQL-JDBC/IsColumnNullable.htm
            return columns_rs.getInt("NULLABLE") == DatabaseMetaData.columnNullable;
        } catch (SQLException e) {
            System.err.println("NULLABLE: " + e.getMessage());
            return null;
        }
    }

    private Map<String, String> _index_names_by_lo_case_col_names(
            String schema,
            String table_name,
            boolean unique) throws SQLException {

        Map<String, String> res = new HashMap<>();
        DatabaseMetaData dm = conn.getMetaData();
        ResultSet rs = dm.getIndexInfo(null, schema, table_name, unique, true);
        try {
            while (rs.next()) {
                String index_name = rs.getString("INDEX_NAME");
                String column_name = rs.getString("COLUMN_NAME");
                if (column_name != null) { // === panedrone: hello from oracle
                    res.put(column_name.toLowerCase(), index_name);
                }
            }
        } finally {
            rs.close();
        }
        return res;
    }

    private Map<String, String[]> _get_lower_case_fk_col_names(
            String schema,
            String table_name) throws SQLException {

        DatabaseMetaData db_md = conn.getMetaData();
        ResultSet rs = db_md.getImportedKeys(conn.getCatalog(), schema, table_name);
        Map<String, String[]> map = new HashMap<>();
        try {
            while (rs.next()) {
                String pk_table_name = rs.getString("PKTABLE_NAME");
                String pk_column_name = rs.getString("PKCOLUMN_NAME");
                String fk_column_name = rs.getString("FKCOLUMN_NAME"); // FK column name in table_name
                map.put(fk_column_name.toLowerCase(), new String[]{pk_table_name, pk_column_name});
            }
        } finally {
            rs.close();
        }
        return map;
    }

    @Override
    public List<String> read_pk_col_names(String table_name) throws Exception {
        DatabaseMetaData md = conn.getMetaData(); // no close() method
        ResultSet rs = _get_pk_rs(md, table_name);
        try {
            List<String> res = new ArrayList<>();
            while (rs.next()) {
                res.add(rs.getString("COLUMN_NAME"));
            }
            return res;
        } finally {
            rs.close();
        }
    }

    private static ResultSet _get_pk_rs(DatabaseMetaData md, String table_name) throws Exception {
        if (table_name.contains(".")) {
            String[] parts = table_name.split("\\.");
            if (parts.length != 2) {
                throw new Exception("Invalid table name: '" + table_name + "'");
            }
            return md.getPrimaryKeys(null, parts[0], parts[1]);
        }
        return md.getPrimaryKeys(null, null, table_name);
    }
}
