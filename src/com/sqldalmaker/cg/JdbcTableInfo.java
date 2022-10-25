/*
    Copyright 2011-2022 sqldalmaker@gmail.com
    SQL DAL Maker Website: http://sqldalmaker.sourceforge.net
    Read LICENSE.txt in the root of this project/archive for details.
 */
package com.sqldalmaker.cg;

import org.apache.cayenne.dba.TypesMapping;

import java.sql.*;
import java.util.*;

/**
 * @author sqldalmaker@gmail.com
 */
class JdbcTableInfo {

    public final List<FieldInfo> fields_all = new ArrayList<FieldInfo>();
    public final List<FieldInfo> fields_not_pk = new ArrayList<FieldInfo>();
    public final List<FieldInfo> fields_pk = new ArrayList<FieldInfo>();

    public final Map<String, FieldInfo> fields_map = new HashMap<String, FieldInfo>();

    private final Connection conn;
    private final JaxbTypeMap type_map;

    private final String model;
    private final String table_name;

    private final String auto_column_name;
    private final String auto_column_generation_type;

    // private final Set<String> explicit_pk_column_names;

    public JdbcTableInfo(String model,
                         Connection conn,
                         JaxbTypeMap type_map,
                         FieldNamesMode dto_field_names_mode,
                         String table_name,
                         String jaxb_explicit_pk,
                         String jaxb_explicit_auto_column) throws Exception {

        this.conn = conn;
        this.type_map = type_map;

        if (model == null) {
            model = "";
        }
        this.model = model;
        if (!SqlUtils.is_table_ref(table_name)) {
            throw new Exception("Table name expected: " + table_name);
        }
        this.table_name = table_name;

        if (jaxb_explicit_auto_column == null) {
            jaxb_explicit_auto_column = "";
        } else {
            jaxb_explicit_auto_column = jaxb_explicit_auto_column.trim();
        }
        if (jaxb_explicit_auto_column.length() == 0) {
            auto_column_name = "";
            auto_column_generation_type = "";
        } else {
            // consider auto like "o_id:identity"
            String[] auto_parts = jaxb_explicit_auto_column.split(":");
            if (auto_parts.length < 2) {
                auto_column_name = jaxb_explicit_auto_column;
                auto_column_generation_type = "auto";
            } else {
                auto_column_name = auto_parts[0];
                auto_column_generation_type = auto_parts[1];
            }
        }
        validate_table_name(conn, table_name); // Oracle PK are not detected with lower case table name
        //////////////////////////////////////
        String jdbc_sql = _jdbc_sql_by_table_name(table_name);
        JdbcSqlFieldInfo.get_field_info_by_jdbc_sql(model, conn, dto_field_names_mode, jdbc_sql, "", fields_map, fields_all);
        Set<String> lower_case_pk_col_names = _get_lower_case_pk_col_names(table_name, jaxb_explicit_pk);
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
        _refine_field_info_by_table_metadata();
        _refine_by_type_map();
    }

    public static void validate_table_name(Connection conn,
                                           String table_name_parretn) throws Exception {

        // it may include schema like "public.%"
        ResultSet rs = get_tables_rs(conn, table_name_parretn, true);
        try {
            if (rs.next()) {
                return;
            }
        } finally {
            rs.close();
        }
        throw new Exception("Not found: '" + table_name_parretn + "'. The search is case-sensitive.");
    }

    public static ResultSet get_tables_rs(Connection conn,
                                          String table_name_parretn, // it may include schema like "public.%"
                                          boolean include_views) throws SQLException {
        String schema_name = null;
        if (table_name_parretn.contains(".")) {
            String[] parts = table_name_parretn.split("\\.");
            if (parts.length != 2) {
                throw new SQLException("Invalid table name: '" + table_name_parretn + "'");
            }
            schema_name = parts[0];
            table_name_parretn = parts[1];
        }
        String[] types;
        if (include_views) {
            types = new String[]{"TABLE", "VIEW"};
        } else {
            types = new String[]{"TABLE"};
        }
        DatabaseMetaData dbmd = conn.getMetaData();
        ResultSet rs_tables;
        String catalog = conn.getCatalog();
        rs_tables = dbmd.getTables(catalog, schema_name, table_name_parretn, types);
        return rs_tables;
    }

    private void _refine_field_info_by_table_metadata() throws Exception {
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
        Map<String, String> unique = _index_names_by_lo_case_col_names(conn, schema_nm, table_name, true);
        Map<String, String> indexed = _index_names_by_lo_case_col_names(conn, schema_nm, table_name, false);
        Map<String, String[]> fk = _get_lower_case_FK_col_names(conn, schema_nm, table_name);
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
                } catch (Exception e) {
                    System.err.println("COLUMN_NAME: " + e.getMessage());
                    continue;
                }
                FieldInfo fi = fields_map.get(db_col_name);
                String sql_type = fi.getOriginalType();
                String no_type = _get_type_name(Object.class.getName());
                if (no_type.equals(sql_type)) {
                    // don't re-define sql_type to avoid type conversions at run-time
                    try {
                        int type = columns_rs.getInt("DATA_TYPE");
                        String apache_java_type_name = TypesMapping.getJavaBySqlType(type);
                        fi.refine_scalar_type(apache_java_type_name);
                        String rendered_type_name = _get_type_name(apache_java_type_name);
                        fi.refine_rendered_type(rendered_type_name);
                    } catch (Exception e) {
                        System.err.println("DATA_TYPE: " + e.getMessage());
                    }
                }
                fi.setComment("t");
                String string_type = _get_type_name(String.class.getName());
                if (string_type.equals(fi.getType())) {
                    try {
                        int size = columns_rs.getInt("COLUMN_SIZE");
                        if (size > 0xffff) { // sqlite3 2000000000
                            size = 0xffff;
                        }
                        fi.setColumnSize(size);
                    } catch (Exception e) {
                        System.err.println("COLUMN_SIZE: " + e.getMessage());
                    }
                }
                if (!fi.isAI()) {
                    try {
                        // IS_AUTOINCREMENT may work incorrectly (sqlite-jdbc-3.19.3.jar).
                        // So, use it only if AI was not set by SQL colums metadata
                        Object is_ai = columns_rs.getObject("IS_AUTOINCREMENT");
                        if ("yes".equals(is_ai) || "YES".equals(is_ai)) {
                            fi.setAI(true);
                        }
                    } catch (Exception e) {
                        System.err.println("IS_AUTOINCREMENT: " + e.getMessage());
                    }
                }
                // Exception: Invalid column Info
//                Object is_gk = columns_rs.getObject("IS_GENERATEDCOLUMN"); // NO for oracle Indentity
//                if ("yes".equals(is_gk) || "YES".equals(is_gk)) {
//                    fi.setGenerated(true);
//                }
                try {
                    int nullable = columns_rs.getInt("NULLABLE");
                    if (nullable == DatabaseMetaData.columnNullable) {
                        // http://www.java2s.com/Code/Java/Database-SQL-JDBC/IsColumnNullable.htm
                        fi.setNullable(true);
                    } else {
                        fi.setNullable(false);
                    }
                } catch (Exception e) {
                    System.err.println("NULLABLE: " + e.getMessage());
                }
                String lo_case_col_name = db_col_name.toLowerCase();
                if (fk.containsKey(lo_case_col_name)) {
                    String[] res = fk.get(lo_case_col_name);
                    String pk_table_name = res[0];
                    String pk_column_name = res[1];
                    fi.setFK(String.format("%s.%s", pk_table_name, pk_column_name));
                }
                if (indexed.containsKey(lo_case_col_name)) {
                    //String res = indexed.get(lo_case_col_name);
                    fi.setIndexed(true);
                }
                if (unique.containsKey(lo_case_col_name)) {
                    //String res = unique.get(lo_case_col_name);
                    fi.setUnique(true);
                }
            }
        } finally {
            columns_rs.close();
        }
    }

    private Map<String, String> _index_names_by_lo_case_col_names(Connection conn,
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

    private static Map<String, String[]> _get_lower_case_FK_col_names(Connection conn,
                                                                      String schema,
                                                                      String table_name) throws SQLException {
        DatabaseMetaData dbmd = conn.getMetaData();
        ResultSet rs = dbmd.getImportedKeys(conn.getCatalog(), schema, table_name);
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

    private static String _jdbc_sql_by_table_name(String table_name) {
        return "select * from " + table_name + " where 1 = 0";
    }

    private void _refine_by_type_map() {
        for (FieldInfo fi : fields_map.values()) {
            String type_name;
            if (fi.getName().equals(auto_column_name) && model.length() > 0) {
                type_name = fi.getType() + "+" + auto_column_generation_type;
            } else {
                type_name = fi.getType();
            }
            type_name = get_target_type_by_type_map(type_name);
            fi.refine_rendered_type(type_name);
        }
    }

    private String get_target_type_by_type_map(String detected) {
        String target_type_name = type_map.get_target_type_name(detected);
        return target_type_name;
    }

    private Set<String> _get_lower_case_pk_col_names(String table_name,
                                                     String explicit_pk) throws Exception {
        if ("*".equals(explicit_pk)) {
            return _get_pk_col_name_aliases_from_table(table_name);
        }
        return JaxbUtils.get_pk_col_name_aliaces_from_jaxb(explicit_pk);
    }

    private ResultSet _get_pk_rs(DatabaseMetaData md,
                                 String table_name) throws Exception {

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
//                if (res.contains(pk_col_name_alias)) {
//                    throw new Exception("Multiple PK column name alias: " + pk_col_name_alias);
//                }
                res.add(pk_col_name_alias);
            }
            return res;
        } finally {
            rs.close();
        }
    }
}
