/*
    Copyright 2011-2023 sqldalmaker@gmail.com
    SQL DAL Maker Website: https://sqldalmaker.sourceforge.net/
    Read LICENSE.txt in the root of this project/archive for details.
 */
package com.sqldalmaker.cg;

import com.sqldalmaker.jaxb.dto.DtoClass;
import com.sqldalmaker.jaxb.dto.DtoClasses;
import com.sqldalmaker.jaxb.settings.Settings;

import java.sql.*;
import java.util.*;

/**
 * @author sqldalmaker@gmail.com
 */
public class JdbcUtils {

    private final Connection conn;
    private final FieldNamesMode dto_field_names_mode;
    private final FieldNamesMode method_params_names_mode;

    private final JaxbMacros global_markers;
    private final JaxbTypeMap type_map;

    private final String sql_root_abs_path;

    public JdbcUtils(Connection conn,
                     FieldNamesMode dto_field_names_mode,
                     FieldNamesMode method_params_names_mode,
                     Settings jaxb_settings,
                     String sql_root_abs_path) throws Exception {

        this.conn = conn;
        this.dto_field_names_mode = dto_field_names_mode;
        this.method_params_names_mode = method_params_names_mode;

        this.global_markers = new JaxbMacros(jaxb_settings.getMacros());
        this.type_map = new JaxbTypeMap(jaxb_settings.getTypeMap());

        this.sql_root_abs_path = sql_root_abs_path;
    }

    // Public Utils --------------------------------------------
    //
    // ---------------------------------------------------------

    public FieldNamesMode get_dto_field_names_mode() {
        return this.dto_field_names_mode;
    }

    public String get_target_type_by_type_map(String detected) {
        String target_type_name = type_map.get_target_type_name(detected);
        String[] parts = target_type_name.split("->");
        return parts[0].trim();
    }

    public static ResultSet get_tables_rs(Connection conn,
                                          String schema_name,
                                          boolean include_views) throws SQLException {
        String table_name_pattern;
        if (schema_name == null) {
            table_name_pattern = "%";
        } else {
            table_name_pattern = schema_name + ".%";
        }
        return JdbcTableInfo.get_tables_rs(conn, table_name_pattern, include_views);
    }

    public static List<String> get_schema_names(Connection con) throws SQLException {
        List<String> res = new ArrayList<String>();
        DatabaseMetaData db_info = con.getMetaData();
        ResultSet rs;
        rs = db_info.getSchemas();
        try {
            while (rs.next()) {
                res.add(rs.getString("TABLE_SCHEM"));
            }
        } finally {
            rs.close();
        }
        return res;
    }

    // DTO -----------------------------------------------------
    //
    // ---------------------------------------------------------

    public Map<String, FieldInfo> get_dto_field_info(DtoClass jaxb_dto_class,
                                                     String sql_root_abs_path,
                                                     List<FieldInfo> res_dto_fields) throws Exception {

        DtoClassInfo info = new DtoClassInfo(conn, type_map, global_markers, dto_field_names_mode);
        return info.get_dto_field_info(false, jaxb_dto_class, sql_root_abs_path, res_dto_fields);
    }

    public List<FieldInfo> get_field_info_for_wizard(DtoClass jaxb_dto_class,
                                                     String sql_root_abs_path) throws Exception {

        DtoClassInfo info = new DtoClassInfo(conn, type_map, global_markers, dto_field_names_mode);
        List<FieldInfo> res_dto_fields = info.get_field_info_for_wizard(jaxb_dto_class, sql_root_abs_path);
        return res_dto_fields;
    }

    // DAO. Raw-SQL -------------------------------------------
    //
    // ---------------------------------------------------------

    public String get_dao_query_info(String sql_root_abs_path,
                                     String dao_jaxb_ref,
                                     String dto_param_type,
                                     String[] method_param_descriptors,
                                     String jaxb_dto_or_return_type,
                                     boolean jaxb_return_type_is_dto,
                                     DtoClasses jaxb_dto_classes,
                                     List<FieldInfo> res_fields,
                                     List<FieldInfo> res_params) throws Exception {

        DaoClassInfo info = new DaoClassInfo(conn, dto_field_names_mode, method_params_names_mode, global_markers, type_map);
        String dao_query_jdbc_sql = info.get_dao_query_info(sql_root_abs_path, dao_jaxb_ref, dto_param_type,
                method_param_descriptors, jaxb_dto_or_return_type,
                jaxb_return_type_is_dto, jaxb_dto_classes, res_fields, res_params);
        return dao_query_jdbc_sql;
    }

    public void get_dao_exec_dml_info(String dao_jdbc_sql,
                                      String dto_param_type,
                                      String[] method_param_descriptors,
                                      List<FieldInfo> res_params) throws Exception {

        FieldNamesMode param_names_mode;
        if (dto_param_type == null || dto_param_type.length() == 0) {
            param_names_mode = method_params_names_mode;
        } else {
            param_names_mode = dto_field_names_mode;
        }
        JdbcSqlParamInfo.get_jdbc_sql_params_info(conn, type_map, dao_jdbc_sql, param_names_mode, method_param_descriptors, res_params);
    }

    // DAO. CRUD -----------------------------------------------
    //
    // ---------------------------------------------------------

    public String get_dao_crud_create_info(String dao_table_name,
                                           DtoClass jaxb_dto_class,
                                           String generated_col_names,
                                           List<FieldInfo> res_dao_fields_not_generated,
                                           List<FieldInfo> res_dao_fields_generated) throws Exception {

        res_dao_fields_not_generated.clear();
        res_dao_fields_generated.clear();
        HashSet<String> dao_crud_generated_set = new HashSet<String>();
        if (!("*".equals(generated_col_names))) {
            String[] gen_keys_arr = Helpers.get_listed_items(generated_col_names, false);
            Helpers.check_duplicates(gen_keys_arr);
            for (String k : gen_keys_arr) {
                dao_crud_generated_set.add(k.toLowerCase());
            }
        }
        DaoClassInfo info = new DaoClassInfo(conn, dto_field_names_mode, method_params_names_mode, global_markers, type_map);
        info.get_dao_fields_for_crud_create(jaxb_dto_class, dao_table_name,
                dao_crud_generated_set, res_dao_fields_not_generated, res_dao_fields_generated);
        return SqlUtils.create_crud_create_sql(dao_table_name, res_dao_fields_not_generated);
    }

    // 1) if I locate this cache in DaoClassInfo, there should be only one instance of DaoClassInfo in JdbcUtils
    // 2) FieldInfo from this cashe must be used as is without modyfying

    private final Map<String, JdbcTableInfo> _table_info = new HashMap<String, JdbcTableInfo>();

    private JdbcTableInfo _get_table_info_for_crud(DtoClass jaxb_dto_class,
                                                   String table_name,
                                                   String explicit_pk) throws Exception {

        String key = String.format("%s|%s", table_name, explicit_pk);
        if (_table_info.containsKey(key)) {
            return _table_info.get(key);
        }
        DaoClassInfo info = new DaoClassInfo(conn, dto_field_names_mode, method_params_names_mode, global_markers, type_map);
        JdbcTableInfo t_info = info.get_dao_fields_for_crud(jaxb_dto_class, table_name, explicit_pk, sql_root_abs_path);
        _table_info.put(key, t_info);
        return t_info;
    }

    public String get_dao_crud_read_info(String dao_table_name,
                                         DtoClass jaxb_dto_class,
                                         boolean fetch_list,
                                         String explicit_pk,
                                         List<FieldInfo> res_dao_fields_all,
                                         List<FieldInfo> res_dao_fields_pk) throws Exception {
        res_dao_fields_all.clear();
        res_dao_fields_pk.clear();
        JdbcTableInfo tfi = _get_table_info_for_crud(jaxb_dto_class, dao_table_name, explicit_pk);
        res_dao_fields_all.addAll(tfi.fields_all);
        res_dao_fields_pk.addAll(tfi.fields_pk);
        return SqlUtils.create_crud_read_sql(dao_table_name, res_dao_fields_pk, fetch_list);
    }

    public String get_dao_crud_update_info(String dao_table_name,
                                           DtoClass jaxb_dto_class,
                                           String explicit_pk,
                                           List<FieldInfo> res_fields_not_pk,
                                           List<FieldInfo> res_fields_pk) throws Exception {
        res_fields_not_pk.clear();
        res_fields_pk.clear();
        JdbcTableInfo tfi = _get_table_info_for_crud(jaxb_dto_class, dao_table_name, explicit_pk);
        res_fields_not_pk.addAll(tfi.fields_not_pk);
        res_fields_pk.addAll(tfi.fields_pk);
        if (res_fields_not_pk.isEmpty()) {
            return null; // just render info comment instead of method
        }
        if (res_fields_pk.isEmpty()) {
            return null; // just render info comment instead of method
        }
        return SqlUtils.create_crud_update_sql(dao_table_name, res_fields_not_pk, res_fields_pk);
    }

    public String get_dao_crud_delete_info(String dao_table_name,
                                           DtoClass jaxb_dto_class,
                                           String explicit_pk,
                                           List<FieldInfo> res_fields_pk) throws Exception {

        JdbcTableInfo tfi = _get_table_info_for_crud(jaxb_dto_class, dao_table_name, explicit_pk);
        res_fields_pk.clear();
        res_fields_pk.addAll(tfi.fields_pk);
        if (res_fields_pk.isEmpty()) {
            return null; // just render info comment instead of method
        }
        return SqlUtils.create_crud_delete_sql(dao_table_name, res_fields_pk);
    }

    public static PreparedStatement prepare_jdbc_sql(Connection conn,
                                                     String jdbc_sql) throws SQLException {

        boolean is_sp = SqlUtils.is_jdbc_stored_proc_call(jdbc_sql);
        if (is_sp) {
            return conn.prepareCall(jdbc_sql);
        } else {
            // For MySQL, prepareStatement doesn't throw Exception for
            // invalid SQL statements and doesn't return null as well
            return conn.prepareStatement(jdbc_sql);
        }
    }
}
