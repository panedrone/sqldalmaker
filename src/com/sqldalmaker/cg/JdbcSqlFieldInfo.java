/*
    Copyright 2011-2024 sqldalmaker@gmail.com
    SQL DAL Maker Website: https://sqldalmaker.sourceforge.net/
    Read LICENSE.txt in the root of this project/archive for details.
 */
package com.sqldalmaker.cg;

import java.sql.*;
import java.util.List;
import java.util.Map;
import java.util.Set;

/*
 * 21.10.2023 09:42
 * 07.05.2023 15:37
 * 16.11.2022 08:02 1.269
 * 02.11.2022 06:51 1.267
 * 25.10.2022 09:26
 *
 */
class JdbcSqlFieldInfo {

    public static void get_field_info_by_jdbc_sql(
            String model,
            Connection conn,
            FieldNamesMode dto_field_names_mode,
            String jdbc_sql,
            String jaxb_explicit_pk,
            Map<String, FieldInfo> fields_map,
            List<FieldInfo> fields_all) throws Exception {

        fields_map.clear();
        fields_all.clear();
        PreparedStatement ps = JdbcUtils.prepare_jdbc_sql(conn, jdbc_sql);
        try {
            ResultSetMetaData rs_md = _get_rs_md(ps);
            int column_count = _get_col_count(rs_md);
            for (int col_num = 1; col_num <= column_count; col_num++) {
                String col_name = _get_jdbc_col_name(rs_md, col_num);
                String type_name = model + _get_jdbc_col_type_name(rs_md, col_num);
                //FieldInfo field = new FieldInfo(dto_field_names_mode, type_name, col_name, "q(" + col_name + ")");
                FieldInfo fi = new FieldInfo(dto_field_names_mode, type_name, col_name, "q");
                // === panedrone: it is nullable for all columns except PK (sqlite3)
                // field.setNullable(rsmd.isNullable(col_num) == ResultSetMetaData.columnNullable);
                boolean is_ai = rs_md.isAutoIncrement(col_num);
                fi.setAI(is_ai);
                fields_map.put(col_name, fi);
                fields_all.add(fi);
                try {
                    int scale = rs_md.getScale(col_num);
                    fi.setDecimalDigits(scale);
                    fi.setScale(scale);
                } catch (SQLException e) {
                    fi.setDecimalDigits(0);
                }
                try {
                    int precision = rs_md.getPrecision(col_num);
                    fi.setPrecision(precision);
                } catch (SQLException e) {
                    fi.setPrecision(0);
                }
                try {
                    // 2147483647 for all cols (SQLite)
                    int displaySize = rs_md.getColumnDisplaySize(col_num);
                    fi.setDisplaySize(displaySize);
                } catch (SQLException e) {
                    fi.setDisplaySize(0);
                }
            }
        } finally {
            ps.close();
        }
        if (jaxb_explicit_pk != null && !jaxb_explicit_pk.trim().isEmpty() && !"*".equals(jaxb_explicit_pk)) {
            Set<String> lower_case_pk_col_names = JaxbUtils.get_pk_col_name_aliases_from_jaxb(jaxb_explicit_pk);
            for (FieldInfo fi : fields_all) {
                String col_name = fi.getColumnName();
                String lower_case_col_name = Helpers.get_pk_col_name_alias(col_name);
                if (lower_case_pk_col_names.contains(lower_case_col_name)) {
                    fi.setPK(true);
                } /*else {
                fi.setPK(false);
            } */
            }
        }

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
            return Helpers.refine_java_type_name(java_class_name);
        } catch (ClassNotFoundException | SQLException ex) {
            return Object.class.getName();
        }
    }
}
