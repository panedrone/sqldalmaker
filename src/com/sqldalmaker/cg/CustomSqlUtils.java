/*
    Copyright 2011-2022 sqldalmaker@gmail.com
    SQL DAL Maker Website: http://sqldalmaker.sourceforge.net
    Read LICENSE.txt in the root of this project/archive for details.
 */
package com.sqldalmaker.cg;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author sqldalmaker@gmail.com
 */
class CustomSqlUtils {

    public static void get_field_info_by_jdbc_sql(String model,
                                                  Connection conn,
                                                  FieldNamesMode dto_field_names_mode,
                                                  String jdbc_sql,
                                                  Map<String, FieldInfo> fields_map,
                                                  List<FieldInfo> fields_all) throws Exception {
        fields_map.clear();
        fields_all.clear();
        PreparedStatement ps = _prepare_jdbc_sql(conn, jdbc_sql);
        try {
            ResultSetMetaData rsmd = _get_rs_md(ps);
            int column_count = _get_col_count(rsmd);
            for (int col_num = 1; col_num <= column_count; col_num++) {
                String col_name = _get_jdbc_col_name(rsmd, col_num);
                String type_name = model + _get_jdbc_col_type_name(rsmd, col_num);
                //FieldInfo field = new FieldInfo(dto_field_names_mode, type_name, col_name, "q(" + col_name + ")");
                FieldInfo field = new FieldInfo(dto_field_names_mode, type_name, col_name, "q");
                // === panedrone: it is nullable for all columns except PK (sqlite3)
                // field.setNullable(rsmd.isNullable(col_num) == ResultSetMetaData.columnNullable);
                boolean is_ai = rsmd.isAutoIncrement(col_num);
                field.setAI(is_ai);
                fields_map.put(col_name, field);
                fields_all.add(field);
            }
        } finally {
            ps.close();
        }
    }

    private static PreparedStatement _prepare_jdbc_sql(Connection conn, String jdbc_sql) throws SQLException {
        boolean is_sp = SqlUtils.is_jdbc_stored_proc_call(jdbc_sql);
        if (is_sp) {
            return conn.prepareCall(jdbc_sql);
        } else {
            // For MySQL, prepareStatement doesn't throw Exception for
            // invalid SQL statements and doesn't return null as well
            return conn.prepareStatement(jdbc_sql);
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

    // ------------------------------------------------------------

    public static void get_jdbc_sql_params_info(Connection conn,
                                                JaxbUtils.JaxbTypeMap type_map,
                                                String dao_jdbc_sql,
                                                FieldNamesMode param_names_mode,
                                                String[] method_param_descriptors,
                                                List<FieldInfo> res_params) throws Exception {

        Helpers.check_duplicates(method_param_descriptors);
        PreparedStatement ps = _prepare_jdbc_sql(conn, dao_jdbc_sql);
        try {
            CustomSqlUtils.get_params_info(ps, type_map, param_names_mode, method_param_descriptors, res_params);
        } finally {
            ps.close();
        }
    }

    public static void get_params_info(
            PreparedStatement ps,
            JaxbUtils.JaxbTypeMap type_map,
            FieldNamesMode param_names_mode,
            String[] method_param_descriptors,
            List<FieldInfo> _params) throws Exception {
        //
        // get_params_info should not be used for CRUD
        //
        if (method_param_descriptors == null) {
            method_param_descriptors = new String[]{};
        }
        _params.clear();
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
        // PostgeSQL -----------------------------------
        // ps.getParameterMetaData() throws SQLException for both PreparedStatement and
        // CallableStatement
        int jdbc_params_count;
        ParameterMetaData pm = null;
        try {
            // MS SQL Server: getParameterMetaData throws exception for SF without params
            pm = ps.getParameterMetaData();
            try {
                jdbc_params_count = pm.getParameterCount();
            } catch (Throwable e) { // including AbstractMethodError, SQLServerException, etc.
                _get_param_info_by_descriptors(type_map, param_names_mode, method_param_descriptors, _params);
                return;
            }
        } catch (Throwable e) { // including AbstractMethodError, SQLServerException, etc.
            jdbc_params_count = 0;
        }
        int not_cb_array_params_count = 0;
        for (int i = 0; i < method_param_descriptors.length; i++) {
            String param_descriptor = method_param_descriptors[i].trim();
            if (param_descriptor.startsWith("[")) {
                // array of callbacks to fetch implicit cursors
                // is included in method_param_descriptors,
                // but implicit cursors are not detected as JDBC parameters
                if (param_descriptor.endsWith("]") == false) {
                    throw new Exception("Ending ']' expected");
                }
            } else {
                not_cb_array_params_count++;
                String default_param_type_name = _get_jdbc_param_type_name(pm, i);
                FieldInfo pi = create_param_info(type_map, param_names_mode, param_descriptor, default_param_type_name);
                _params.add(pi);
            }
        }
        if (jdbc_params_count != not_cb_array_params_count) {
            throw new Exception("Parameters declared in method: " + method_param_descriptors.length
                    + ", detected by MetaData: " + jdbc_params_count);
        }
    }

    private static void _get_param_info_by_descriptors(
            JaxbUtils.JaxbTypeMap type_map,
            FieldNamesMode param_names_mode,
            String[] method_param_descriptors,
            List<FieldInfo> res_params) throws Exception {

        res_params.clear();
        for (String param_descriptor : method_param_descriptors) {
            FieldInfo pi = create_param_info(type_map, param_names_mode, param_descriptor, Object.class.getName());
            res_params.add(pi);
        }
    }

    private static String _get_jdbc_param_type_name(ParameterMetaData pm, int i) {
        if (pm == null) {
            return Object.class.getName();
        }
        String java_class_name;
        try {
            // 1) getParameterClassName throws exception in
            // mysql-connector-java-5.1.17-bin.jar:
            // 2) sometime it returns "[B": See comments for Class.getName() API
            java_class_name = pm.getParameterClassName(i + 1);
            java_class_name = Helpers.process_java_type_name(java_class_name);
        } catch (Exception ex) {
            java_class_name = Object.class.getName();
        }
        return java_class_name;
    }

    public static FieldInfo create_param_info(
            JaxbUtils.JaxbTypeMap type_map,
            FieldNamesMode param_names_mode,
            String param_descriptor,
            String default_param_type_name) throws Exception {

        String param_type_name;
        String param_name;
        String[] parts = Helpers.parse_param_descriptor(param_descriptor);
        if (parts[0] == null) {
            param_type_name = default_param_type_name;
            param_name = param_descriptor;
        } else {
            param_type_name = parts[0];
            param_name = parts[1];
        }
        param_type_name = type_map.get_target_type_name(param_type_name);
        return new FieldInfo(param_names_mode, param_type_name, param_name, "parameter");
    }

    public static void get_shortcut_info(JaxbUtils.JaxbTypeMap type_map,
                                         FieldNamesMode param_names_mode,
                                         String[] method_param_descriptors,
                                         List<FieldInfo> fields_all,
                                         String[] filter_col_names,
                                         List<FieldInfo> res_params) throws Exception {

        Map<String, FieldInfo> all_col_names_map = new HashMap<String, FieldInfo>();
        for (FieldInfo fi : fields_all) {
            String cn = fi.getColumnName();
            all_col_names_map.put(cn, fi);
        }
        List<FieldInfo> fields_filter = new ArrayList<FieldInfo>();
        for (String fcn : filter_col_names) {
            if (!all_col_names_map.containsKey(fcn))
                throw new Exception("Invalid SQL-shortcut. Table column '" + fcn + "' not found. Ensure upper/lower case.");
            FieldInfo fi = all_col_names_map.get(fcn);
            fields_filter.add(fi);
        }
        // assign param types from table!! without dto-refinement!!!
        if (method_param_descriptors.length != fields_filter.size()) {
            throw new Exception("Invalid SQL-shortcut. Methof parameters declared: " + method_param_descriptors.length
                    + ". SQL parameters expected: " + fields_filter.size());
        }
        for (int i = 0; i < method_param_descriptors.length; i++) {
            String param_descriptor = method_param_descriptors[i];
            FieldInfo fi = fields_filter.get(i);
            String curr_type = fi.getType();
            String default_param_type_name = get_target_type_by_type_map(type_map, curr_type);
            FieldInfo pi = CustomSqlUtils.create_param_info(type_map, param_names_mode, param_descriptor, default_param_type_name);
            res_params.add(pi);
        }
    }

    private static String get_target_type_by_type_map(JaxbUtils.JaxbTypeMap type_map, String detected) {
        String target_type_name = type_map.get_target_type_name(detected);
        return target_type_name;
    }
}
