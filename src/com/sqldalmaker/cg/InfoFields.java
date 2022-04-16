/*
    Copyright 2011-2022 sqldalmaker@gmail.com
    SQL DAL Maker Website: http://sqldalmaker.sourceforge.net
    Read LICENSE.txt in the root of this project/archive for details.
 */
package com.sqldalmaker.cg;

import org.apache.cayenne.dba.TypesMapping;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author sqldalmaker@gmail.com
 */
class InfoFields {

    public static List<FieldInfo> get_field_info_by_jdbc_sql(
            FieldNamesMode dto_field_names_mode,
            PreparedStatement ps,
            Map<String, FieldInfo> _fields_map) throws Exception {

        List<FieldInfo> _fields = new ArrayList<FieldInfo>();
        ResultSetMetaData rsmd = _get_rs_md(ps);
        int column_count = _get_col_count(rsmd);
        _fields_map.clear();
        for (int i = 1; i <= column_count; i++) {
            String col_name = _get_jdbc_col_name(rsmd, i);
            String type_name = _get_jdbc_col_type_name(rsmd, i);
            FieldInfo field = new FieldInfo(dto_field_names_mode, type_name, col_name, "q(" + col_name + ")");
            if (rsmd.isAutoIncrement(i)) {
                field.setAI(true);
            } else {
                field.setAI(false);
            }
            _fields_map.put(col_name, field);
            _fields.add(field);
        }
        return _fields;
    }

    public static void refine_field_info_by_jdbc_table(
            Connection conn,
            String table_name,
            Map<String, FieldInfo> fields_map) throws Exception {

        if (!SqlUtils.is_table_ref(table_name)) {
            throw new Exception("Table name expected: " + table_name);
        }
        DatabaseMetaData md = conn.getMetaData();
        ResultSet columns_rs = _get_columns_rs(md, table_name);
        try {
            while (columns_rs.next()) {
                String db_col_name = columns_rs.getString("COLUMN_NAME");
                if (fields_map.containsKey(db_col_name)) {
                    int type = columns_rs.getInt("DATA_TYPE");
                    String apache_java_type_name = TypesMapping.getJavaBySqlType(type);
                    FieldInfo fi = fields_map.get(db_col_name);
                    fi.refine_rendered_type(apache_java_type_name);
                    fi.setComment("t(" + db_col_name + ")");
                }
            }
        } finally {
            columns_rs.close();
        }
    }

    private static ResultSet _get_columns_rs(
            DatabaseMetaData md,
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

    private static ResultSetMetaData _get_rs_md(PreparedStatement ps) throws Exception {
        ResultSetMetaData rsmd;
        try {
            rsmd = ps.getMetaData();
        } catch (SQLException e) {
            throw new Exception("Exception in getMetaData " + e.getMessage());
        }
        if (rsmd == null) {  // it is possible by javadocs
            throw new Exception("getMetaData() == null");
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
        if (column_name == null || column_name.length() == 0) {
            column_name = rsmd.getColumnName(col_num);
        }
        if (column_name == null) {
            throw new Exception(
                    "Cannot detect column name. Try to specify column label like 'select count(*) as res from ...'");
        }
        if (column_name.length() == 0) {
            column_name = "col_" + col_num; // MS SQL Server: column_name == "" for 'select dbo.ufnLeadingZeros(?)'
        }
        return column_name;
    }

    private static String _get_jdbc_col_type_name(ResultSetMetaData rsmd, int i) {
        try {
            // sometime, it returns "[B": See comments for Class.getName() API
            String java_class_name = rsmd.getColumnClassName(i);
            return Helpers.process_java_type_name(java_class_name);
        } catch (Exception ex) {
            return Object.class.getName();
        }
    }
}
