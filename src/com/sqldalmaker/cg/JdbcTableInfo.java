/*
    Copyright 2011-2024 sqldalmaker@gmail.com
    SQL DAL Maker Website: https://sqldalmaker.sourceforge.net/
    Read LICENSE.txt in the root of this project/archive for details.
 */
package com.sqldalmaker.cg;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.cayenne.dba.TypesMapping;

/*
 * @author sqldalmaker@gmail.com
 *
 * 30.05.2024 20:00 1.299
 * 30.09.2023 11:29 1.289
 * 07.05.2023 15:37
 * 06.04.2023 02:56 1.281
 * 27.03.2023 12:15 1.280
 * 16.11.2022 08:02 1.269
 * 02.11.2022 06:51 1.267
 * 25.10.2022 09:26
 * 19.07.2022 13:04
 * 28.05.2022 22:33 1.247
 * 10.05.2022 19:27 1.239
 * 24.04.2022 08:20 1.229
 * 17.04.2022 11:25 1.219
 *
 */
class JdbcTableInfo {

    final List<FieldInfo> fields_all = new ArrayList<FieldInfo>();
    final List<FieldInfo> fields_not_pk = new ArrayList<FieldInfo>();
    final List<FieldInfo> fields_pk = new ArrayList<FieldInfo>();

    final Map<String, FieldInfo> fields_map = new HashMap<String, FieldInfo>();

    private final Connection conn;
    private final JaxbTypeMap type_map;

    private final String model;
    private final String table_name;

    private final String explicit_auto_column_name;
    private final String explicit_auto_column_generation_type;

    public JdbcTableInfo(
            String model,
            Connection conn,
            JaxbTypeMap type_map,
            FieldNamesMode dto_field_names_mode,
            String table_ref,
            String jaxb_explicit_pk,
            String jaxb_explicit_auto_column) throws Exception {

        this.conn = conn;
        this.type_map = type_map;
        if (model == null) {
            model = "";
        }
        this.model = model;
        if (!SqlUtils.is_table_ref(table_ref)) {
            throw new Exception("Table name expected: " + table_ref);
        }
        //////////////////////////////////////
        _validate_table_name(table_ref); // Oracle PK are not detected with lower case table name
        this.table_name = table_ref;
        //////////////////////////////////////
        if (jaxb_explicit_auto_column == null) {
            jaxb_explicit_auto_column = "";
        } else {
            jaxb_explicit_auto_column = jaxb_explicit_auto_column.trim();
        }
        if (jaxb_explicit_auto_column.isEmpty()) {
            explicit_auto_column_name = "";
            explicit_auto_column_generation_type = "";
        } else {
            // consider auto like "o_id:identity"
            String[] auto_parts = jaxb_explicit_auto_column.split(":");
            if (auto_parts.length < 2) {
                explicit_auto_column_name = jaxb_explicit_auto_column; // if it is default "*", then just nothing will happen
                explicit_auto_column_generation_type = "auto";
            } else {
                explicit_auto_column_name = auto_parts[0];
                explicit_auto_column_generation_type = auto_parts[1];
            }
        }
        //////////////////////////////////////
        String jdbc_sql = SqlUtils.jdbc_sql_by_table_name(table_ref);
        JdbcSqlFieldInfo.get_field_info_by_jdbc_sql(model, conn, dto_field_names_mode, jdbc_sql, "", fields_map, fields_all);
        Set<String> lower_case_pk_col_names = _get_lower_case_pk_col_names(table_ref, jaxb_explicit_pk);
        for (FieldInfo fi : fields_all) {
            String col_name = fi.getColumnName();
            String lower_case_col_name = Helpers.get_pk_col_name_alias(col_name);
            if (lower_case_pk_col_names.contains(lower_case_col_name)) {
                fields_pk.add(fi);
                fi.setPK(true);
            } else {
                fields_not_pk.add(fi);
                fi.setPK(false);
            }
        }
        _refine_field_info_by_jdbc_table();
        _refine_by_type_map();
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

    public static ResultSet get_tables_rs(
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
        ResultSet rs_tables;
        String catalog = conn.getCatalog();
        rs_tables = db_md.getTables(catalog, schema_name, table_name_pattern, types);
        return rs_tables;
    }

    private void _refine_field_info_by_jdbc_table() throws Exception {
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
        Map<String, String> lower_case_unique = _index_names_by_lo_case_col_names(conn, schema_nm, table_name, true);
        Map<String, String> lower_case_indexed = _index_names_by_lo_case_col_names(conn, schema_nm, table_name, false);
        Map<String, String[]> lower_case_fk = _get_lower_case_FK_col_names(conn, schema_nm, table_name);
        DatabaseMetaData md = conn.getMetaData();
        ResultSet columns_rs = md.getColumns(conn.getCatalog(), schema_nm, table_nm, "%");
        try {
            while (columns_rs.next()) {
                String db_col_name;
                try {
                    db_col_name = columns_rs.getString("COLUMN_NAME");
                    if (!fields_map.containsKey(db_col_name)) {
                        continue;
                    }
                } catch (SQLException e) {
                    System.err.println("COLUMN_NAME: " + e.getMessage());
                    continue;
                }
                FieldInfo fi = fields_map.get(db_col_name);
                fi.setComment("t");
                _init_with_jdbc_type(fi, columns_rs);
                _set_col_size(fi, columns_rs);
                _set_decimal_digits(fi, columns_rs);
                _set_ai(fi, columns_rs);
                _set_nullable(fi, columns_rs);
                // Exception: Invalid column Info
                // Object is_gk = columns_rs.getObject("IS_GENERATEDCOLUMN"); // "NO" for oracle IDENTITY
                // if ("yes".equals(is_gk) || "YES".equals(is_gk)) {
                //      fi.setGenerated(true);
                String lo_case_col_name = db_col_name.toLowerCase();
                if (lower_case_fk.containsKey(lo_case_col_name)) {
                    String[] res = lower_case_fk.get(lo_case_col_name);
                    String pk_table_name = res[0];
                    String pk_column_name = res[1];
                    fi.setFK(String.format("%s.%s", pk_table_name, pk_column_name));
                }
                if (lower_case_indexed.containsKey(lo_case_col_name)) {
                    fi.setIndexed(true);
                }
                if (lower_case_unique.containsKey(lo_case_col_name)) {
                    fi.setUnique(true);
                }
            }
        } finally {
            columns_rs.close();
        }
    }

    private static void _set_nullable(FieldInfo fi, ResultSet columns_rs) {
        try {
            int nullable = columns_rs.getInt("NULLABLE");
            if (nullable == DatabaseMetaData.columnNullable) {
                // http://www.java2s.com/Code/Java/Database-SQL-JDBC/IsColumnNullable.htm
                fi.setNullable(true);
            } else {
                fi.setNullable(false);
            }
        } catch (SQLException e) {
            System.err.println("NULLABLE: " + e.getMessage());
        }
    }

    private static void _set_ai(FieldInfo fi, ResultSet columns_rs) {
        if (!fi.isAI()) {
            try {
                // IS_AUTOINCREMENT may work incorrectly (sqlite-jdbc-3.19.3.jar).
                // So, use it only if AI was not set by SQL columns metadata
                Object is_ai = columns_rs.getObject("IS_AUTOINCREMENT");
                if ("yes".equals(is_ai) || "YES".equals(is_ai)) {
                    fi.setAI(true);
                }
            } catch (SQLException e) {
                System.err.println("IS_AUTOINCREMENT: " + e.getMessage());
            }
        }
    }

    private static void _set_decimal_digits(FieldInfo fi, ResultSet columns_rs) {
        try {
            // it is always -127 for Oracle NUMERIC-s and always 10 for all SQLite types
            // https://stackoverflow.com/questions/12931061/how-to-get-oracle-number-with-syncdb
            int decimal_digits = columns_rs.getInt("DECIMAL_DIGITS");
            fi.setDecimalDigits(decimal_digits);
        } catch (SQLException e) {
            System.err.println("DECIMAL_DIGITS: " + e.getMessage());
        }
    }

    private static void _set_col_size(FieldInfo fi, ResultSet columns_rs) {
        String string_type = String.class.getName();
        String fi_type = fi.getScalarType();
        if (string_type.equals(fi_type)) {
            try {
                final int max_size = 0x3FFF;  // 16K
                int size = columns_rs.getInt("COLUMN_SIZE");
                if (size > max_size) { // sqlite3 2000000000
                    size = 0;
                }
                int precision = fi.getPrecision();
                if (precision > 0 && precision < max_size) {
                    // How to get the size of a column of a table using JDBC?
                    // https://www.tutorialspoint.com/how-to-get-the-size-of-a-column-of-a-table-using-jdbc
                    if (size == 0 || precision < size) {
                        size = precision;
                    }
                }
                fi.setColumnSize(size);
            } catch (SQLException e) {
                System.err.println("COLUMN_SIZE: " + e.getMessage());
            }
        }
    }

    class Oracle {
        // Field descriptor #7 I
        // public static final int TIMESTAMP = 93; // === panedrone: 93 is known in "TypesMapping.getJavaBySqlType"

        // Field descriptor #7 I (deprecated)
        public static final int TIMESTAMPNS = -100;

        // Field descriptor #7 I
        public static final int TIMESTAMPTZ = -101;

        // Field descriptor #7 I
        public static final int TIMESTAMPLTZ = -102;
    }

    private void _init_with_jdbc_type(FieldInfo fi, ResultSet columns_rs) {
        String sql_type = fi.getOriginalType();
        String no_type = _get_type_name(Object.class.getName());
        if (no_type.equals(sql_type)) {
            // don't re-define sql_type to avoid type conversions at run-time
            try {
                int type = columns_rs.getInt("DATA_TYPE");
                String apache_java_type_name = TypesMapping.getJavaBySqlType(type);
                if (Object.class.getName().equals(apache_java_type_name)) {
                    switch (type) {
                        // case Oracle.TIMESTAMP:
                        case Oracle.TIMESTAMPNS:
                            apache_java_type_name = java.util.Date.class.getName();
                            break;
                        case Oracle.TIMESTAMPTZ:
                            apache_java_type_name = java.time.ZonedDateTime.class.getName();
                            break;
                        case Oracle.TIMESTAMPLTZ:
                            apache_java_type_name = java.time.LocalDateTime.class.getName();
                            break;
                    }
                }
                fi.refine_scalar_type(apache_java_type_name);
                String rendered_type_name = _get_type_name(apache_java_type_name);
                fi.refine_rendered_type(rendered_type_name);
            } catch (SQLException e) {
                System.err.println("DATA_TYPE: " + e.getMessage());
            }
        }
    }

    private Map<String, String> _index_names_by_lo_case_col_names(
            Connection conn,
            String schema,
            String table_name,
            boolean unique) throws SQLException {

        Map<String, String> res = new HashMap<String, String>();
        DatabaseMetaData dm = conn.getMetaData();
        ResultSet rs = dm.getIndexInfo(null, schema, table_name, unique, true);
        while (rs.next()) {
            String indexName = rs.getString("INDEX_NAME");
            String columnName = rs.getString("COLUMN_NAME");
            if (columnName != null) { // === panedrone: hello from oracle
                res.put(columnName.toLowerCase(), indexName);
            }
        }
        return res;
    }

    private static Map<String, String[]> _get_lower_case_FK_col_names(
            Connection conn,
            String schema,
            String table_name) throws SQLException {

        DatabaseMetaData db_md = conn.getMetaData();
        ResultSet rs = db_md.getImportedKeys(conn.getCatalog(), schema, table_name);
        Map<String, String[]> map = new HashMap<String, String[]>();
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

    private String _get_type_name(String type_name) {
        return model + type_name;
    }

    private void _refine_by_type_map() {
        for (FieldInfo fi : fields_map.values()) {
            String detected_type_name;
            String db_col_name = fi.getColumnName();
            if (db_col_name.equalsIgnoreCase(explicit_auto_column_name) && !model.isEmpty()) {
                detected_type_name = fi.getType() + "+" + explicit_auto_column_generation_type;
            } else {
                detected_type_name = fi.getType();
            }
            String target_type_name = type_map.get_target_type_name(detected_type_name);
            fi.refine_rendered_type(target_type_name);
        }
    }

    private Set<String> _get_lower_case_pk_col_names(String table_name, String explicit_pk) throws Exception {
        if ("*".equals(explicit_pk)) {
            return _get_pk_col_name_aliases_from_table(table_name);
        }
        return JaxbUtils.get_pk_col_name_aliases_from_jaxb(explicit_pk);
    }

    private ResultSet _get_pk_rs(DatabaseMetaData md, String table_name) throws Exception {
        if (table_name.contains(".")) {
            String[] parts = table_name.split("\\.");
            if (parts.length != 2) {
                throw new Exception("Invalid table name: '" + table_name + "'");
            }
            return md.getPrimaryKeys(null, parts[0], parts[1]);
        }
        return md.getPrimaryKeys(null, null, table_name);
    }

    private Set<String> _get_pk_col_name_aliases_from_table(String table_name) throws Exception {
        DatabaseMetaData md = conn.getMetaData(); // no close() method
        ResultSet rs = _get_pk_rs(md, table_name);
        try {
            Set<String> res = new HashSet<String>();
            while (rs.next()) {
                String pk_col_name = rs.getString("COLUMN_NAME");
                String pk_col_name_alias = Helpers.get_pk_col_name_alias(pk_col_name);
                // 2 'id' happened with MySQL!
                //if (res.contains(pk_col_name_alias)) {
                //    throw new Exception("Multiple PK column name alias: " + pk_col_name_alias);
                res.add(pk_col_name_alias);
            }
            return res;
        } finally {
            rs.close();
        }
    }
}
