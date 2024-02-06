/*
    Copyright 2011-2023 sqldalmaker@gmail.com
    SQL DAL Maker Website: https://sqldalmaker.sourceforge.net/
    Read LICENSE.txt in the root of this project/archive for details.
 */
package com.sqldalmaker.cg;

import java.sql.Connection;
import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
import java.util.List;

/*
 * 07.05.2023 15:37
 * 09.04.2023 14:52 1.282
 * 16.11.2022 08:02 1.269
 * 25.10.2022 09:26
 * 10.05.2022 19:27 1.239
 * 26.04.2022 15:44 1.230
 * 17.04.2022 11:25 1.219
 *
 */
class JdbcSqlParamInfo {

    static void get_jdbc_sql_params_info(
            Connection conn,
            JaxbTypeMap type_map,
            String dao_jdbc_sql,
            FieldNamesMode param_names_mode,
            String[] method_param_descriptors,
            List<FieldInfo> res_params) throws Exception {

        Helpers.check_duplicates(method_param_descriptors);
        PreparedStatement ps = JdbcUtils.prepare_jdbc_sql(conn, dao_jdbc_sql);
        try {
            get_params_info(ps, type_map, param_names_mode, method_param_descriptors, res_params);
        } finally {
            ps.close();
        }
    }

    static void get_params_info(
            PreparedStatement ps,
            JaxbTypeMap type_map,
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
            JaxbTypeMap type_map,
            FieldNamesMode param_names_mode,
            String[] method_param_descriptors,
            List<FieldInfo> res_params) throws Exception {

        res_params.clear();
        for (String param_descriptor : method_param_descriptors) {
            FieldInfo pi = create_param_info(type_map, param_names_mode, param_descriptor, Object.class.getName());
            res_params.add(pi);
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
            java_class_name = Helpers.refine_java_type_name(java_class_name);
        } catch (Exception ex) {
            java_class_name = Object.class.getName();
        }
        return java_class_name;
    }

    static FieldInfo create_param_info(
            JaxbTypeMap type_map,
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
        FieldInfo res = new FieldInfo(param_names_mode, param_type_name, param_name, "parameter");
        param_type_name = type_map.get_target_type_name(param_type_name);
        res.refine_rendered_type(param_type_name);
        return res;
    }
}
