/*
    Copyright 2011-2022 sqldalmaker@gmail.com
    SQL DAL Maker Website: http://sqldalmaker.sourceforge.net
    Read LICENSE.txt in the root of this project/archive for details.
 */
package com.sqldalmaker.cg;

import java.sql.Connection;
import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * @author sqldalmaker@gmail.com
 */
class InfoCustomSql {

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

    public static void get_field_info_by_jdbc_sql(String model,
                                                  Connection conn,
                                                  FieldNamesMode dto_field_names_mode,
                                                  String jdbc_sql,
                                                  Map<String, FieldInfo> fields_map,
                                                  List<FieldInfo> fields_all) throws Exception {

        PreparedStatement ps = _prepare_jdbc_sql(conn, jdbc_sql);
        try {
            List<FieldInfo> res = InfoFields.get_field_info_by_jdbc_sql(model, dto_field_names_mode, ps, fields_map);
            fields_all.clear();
            fields_all.addAll(res);
        } finally {
            ps.close();
        }
    }

    public static void get_jdbc_sql_params_info(Connection conn,
                                                JaxbUtils.JaxbTypeMap type_map,
                                                String dao_jdbc_sql,
                                                FieldNamesMode param_names_mode,
                                                String[] method_param_descriptors,
                                                List<FieldInfo> params) throws Exception {

        Helpers.check_duplicates(method_param_descriptors);
        PreparedStatement ps = _prepare_jdbc_sql(conn, dao_jdbc_sql);
        try {
            InfoCustomSql.get_params_info(ps, type_map, param_names_mode, method_param_descriptors, params);
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
            List<FieldInfo> _params) throws Exception {

        _params.clear();
        for (String param_descriptor : method_param_descriptors) {
            FieldInfo pi = create_param_info(type_map, param_names_mode, param_descriptor, Object.class.getName());
            _params.add(pi);
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
}
