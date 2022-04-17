/*
    Copyright 2011-2022 sqldalmaker@gmail.com
    SQL DAL Maker Website: http://sqldalmaker.sourceforge.net
    Read LICENSE.txt in the root of this project/archive for details.
 */
package com.sqldalmaker.cg;

import java.sql.PreparedStatement;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author sqldalmaker@gmail.com
 */
class InfoFields {

    public static List<FieldInfo> get_field_info_by_jdbc_sql(String model,
                                                             FieldNamesMode dto_field_names_mode,
                                                             PreparedStatement ps,
                                                             Map<String, FieldInfo> _fields_map) throws Exception {

        List<FieldInfo> _fields = new ArrayList<FieldInfo>();
        ResultSetMetaData rsmd = _get_rs_md(ps);
        int column_count = _get_col_count(rsmd);
        _fields_map.clear();
        for (int col_num = 1; col_num <= column_count; col_num++) {
            String col_name = _get_jdbc_col_name(rsmd, col_num);
            String type_name = model + _get_jdbc_col_type_name(rsmd, col_num);
            FieldInfo field = new FieldInfo(dto_field_names_mode, type_name, col_name, "q(" + col_name + ")");
            if (rsmd.isAutoIncrement(col_num)) {
                field.setAI(true);
            } else {
                field.setAI(false);
            }
            _fields_map.put(col_name, field);
            _fields.add(field);
        }
        return _fields;
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

    private static String _get_jdbc_col_name(ResultSetMetaData rsmd,
                                             int col_num) throws Exception {
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

    private static String _get_jdbc_col_type_name(ResultSetMetaData rsmd, int col_num) {
        try {
            // sometime, it returns "[B": See comments for Class.getName() API
            String java_class_name = rsmd.getColumnClassName(col_num);
            return Helpers.process_java_type_name(java_class_name);
        } catch (Exception ex) {
            return Object.class.getName();
        }
    }
}
