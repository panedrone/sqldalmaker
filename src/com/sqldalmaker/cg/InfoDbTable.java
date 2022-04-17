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
class InfoDbTable {

    public final List<FieldInfo> fields_all = new ArrayList<FieldInfo>();
    public final List<FieldInfo> fields_not_pk = new ArrayList<FieldInfo>();
    public final List<FieldInfo> fields_pk = new ArrayList<FieldInfo>();

    public final Map<String, FieldInfo> fields_map = new HashMap<String, FieldInfo>();

    private final Connection conn;
    private final JaxbUtils.JaxbTypeMap type_map;
    private final FieldNamesMode dto_field_names_mode;

    private final String model;
    private final String table_name;
    private final String explicit_pk;

    public InfoDbTable(String model,
                       Connection conn,
                       JaxbUtils.JaxbTypeMap type_map,
                       FieldNamesMode dto_field_names_mode,
                       String table_name,
                       String explicit_pk) throws Exception {

        this.conn = conn;
        this.type_map = type_map;
        this.dto_field_names_mode = dto_field_names_mode;

        this.model = model;
        if (!SqlUtils.is_table_ref(table_name)) {
            throw new Exception("Table name expected: " + table_name);
        }
        this.table_name = table_name;
        this.explicit_pk = explicit_pk;

        _refine_field_info_by_jdbc_table();
    }

    private void _refine_field_info_by_jdbc_table() throws Exception {
        String jdbc_sql = _jdbc_sql_by_table_name(table_name);
        InfoCustomSql.get_field_info_by_jdbc_sql(model, conn, dto_field_names_mode, jdbc_sql, fields_map, fields_all);
        Set<String> lower_case_pk_col_names = _get_lower_case_pk_col_names(table_name, explicit_pk);
        for (FieldInfo fi : fields_all) {
            String col_name = fi.getColumnName();
            String lower_case_col_name = _get_pk_col_name_alias(col_name);
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

    private void _refine_field_info_by_table_metadata() throws Exception {
        DatabaseMetaData md = conn.getMetaData();
        ResultSet columns_rs = _get_columns_rs(md, table_name);
        try {
            while (columns_rs.next()) {
                String db_col_name = columns_rs.getString("COLUMN_NAME");
                if (fields_map.containsKey(db_col_name)) {
                    int type = columns_rs.getInt("DATA_TYPE");
                    String apache_java_type_name = TypesMapping.getJavaBySqlType(type);
                    FieldInfo fi = fields_map.get(db_col_name);
                    fi.refine_rendered_type(_get_type_name(apache_java_type_name));
                    fi.setComment("t(" + db_col_name + ")");
                }
            }
        } finally {
            columns_rs.close();
        }
    }

    private String _get_type_name(String type_name) {
        return model + type_name;
    }

    private static ResultSet _get_columns_rs(DatabaseMetaData md,
                                             String table_name) throws SQLException {

        String[] parts = table_name.split("\\.", -1); // -1 to leave empty strings
        ResultSet rs_columns;
        if (parts.length == 1) {
            rs_columns = md.getColumns(null, null, table_name, "%");
        } else {
            String schema_nm = table_name.substring(0, table_name.lastIndexOf('.'));
            String table_nm = parts[parts.length - 1];
            rs_columns = md.getColumns(null, schema_nm, table_nm, "%");
        }
        return rs_columns;
    }

    private static String _jdbc_sql_by_table_name(String table_name) {
        return "select * from " + table_name + " where 1 = 0";
    }

    private void _refine_by_type_map() {
        for (FieldInfo fi : fields_map.values()) {
            String type_name = fi.getType();
            type_name = get_target_type_by_type_map(type_name);
            fi.refine_rendered_type(type_name);
        }
    }

    private String get_target_type_by_type_map(String detected) {
        String target_type_name = type_map.get_target_type_name(detected);
        return target_type_name;
    }

    private Set<String> _get_lower_case_pk_col_names(String table_name, String explicit_pk) throws Exception {
        if ("*".equals(explicit_pk)) {
            return _get_pk_col_name_aliases_from_table(table_name);
        }
        return _get_pk_col_name_aliaces_from_jaxb(explicit_pk);
    }

    private ResultSet _get_pk_rs(DatabaseMetaData md, String table_name) throws Exception {
        if (table_name.contains(".")) {
            String[] parts = table_name.split("\\.");
            if (parts.length != 2) {
                throw new Exception("Unexpected table name: '" + table_name + "'");
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
                String pk_col_name_alias = _get_pk_col_name_alias(pk_col_name);
                if (res.contains(pk_col_name_alias)) {
                    throw new Exception("Duplickated PK column name alias: " + pk_col_name_alias);
                }
                res.add(pk_col_name_alias);
            }
            return res;
        } finally {
            rs.close();
        }
    }

    private String _get_pk_col_name_alias(String pk_col_name) {
        // === panederone: WHY ALIASES:
        //   1) xerial SQLite3: getPrimaryKeys may return pk_col_names in lower case
        //      For other JDBC drivers, it may differ.
        //   2) xerial SQLite3 returns pk_col_names in the format
        //     '[employeeid] asc' (compound PK)
        pk_col_name = pk_col_name.toLowerCase().replace("[", "").replace("]", "").trim();
        if (pk_col_name.endsWith(" asc")) {
            pk_col_name = pk_col_name.split(" asc")[0];
        }
        if (pk_col_name.endsWith(" desc")) {
            pk_col_name = pk_col_name.split(" desc")[1];
        }
        pk_col_name = pk_col_name.trim();
        return pk_col_name;
    }

    private Set<String> _get_pk_col_name_aliaces_from_jaxb(String explicit_pk) throws Exception {
        // if PK are specified explicitely, don't use getPrimaryKeys at all
        String[] gen_keys_arr = Helpers.get_listed_items(explicit_pk, false);
        Helpers.check_duplicates(gen_keys_arr);
        for (int i = 0; i < gen_keys_arr.length; i++) {
            gen_keys_arr[i] = _get_pk_col_name_alias(gen_keys_arr[i].toLowerCase());
        }
        return new HashSet<String>(Arrays.asList(gen_keys_arr));
    }
}
