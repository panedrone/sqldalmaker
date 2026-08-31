/*
    Copyright 2011-2026 sqldalmaker@gmail.com
    SQL DAL Maker Website: https://sqldalmaker.sourceforge.net/
    Read LICENSE.txt in the root of this project/archive for details.
 */
package com.sqldalmaker.cg;

import com.sqldalmaker.jaxb.sdm.DtoClass;
import com.sqldalmaker.jaxb.settings.Settings;

import java.sql.*;
import java.util.*;

/*
 * 30.08.2026 14:00 1.331 Claude refactor
 * 16.11.2022 08:02 1.269
 * 16.04.2022 17:35 1.219
 * 08.05.2021 22:29 1.200
 *
 */
public class JdbcUtils {

    private final Connection conn;
    private final FieldNamesMode dto_field_names_mode;
    private final FieldNamesMode method_params_names_mode;

    private final JaxbMacros jaxb_macros;
    private final JaxbTypeMap jaxb_type_map;

    private final String sql_root_abs_path;

    private final DtoClassInfo dto_class_info;
    private final DaoClassInfo dao_class_info;

    public JdbcUtils(
            Connection conn,
            FieldNamesMode dto_field_names_mode,
            FieldNamesMode method_params_names_mode,
            Settings jaxb_settings,
            String sql_root_abs_path) throws Exception {

        this.conn = conn;
        this.dto_field_names_mode = dto_field_names_mode;
        this.method_params_names_mode = method_params_names_mode;

        this.jaxb_macros = new JaxbMacros(jaxb_settings.getMacros());
        this.jaxb_type_map = new JaxbTypeMap(jaxb_settings.getTypeMap());

        this.sql_root_abs_path = sql_root_abs_path;

        // both are stateless, there is no reason to re-create them on every call
        this.dto_class_info = new DtoClassInfo(conn, jaxb_type_map, jaxb_macros, dto_field_names_mode);
        this.dao_class_info = new DaoClassInfo(conn, dto_field_names_mode, method_params_names_mode,
                jaxb_macros, jaxb_type_map);
    }

    /**
     * SQL of a DAO method plus the fields it returns and the parameters it takes.
     * 'sql' == null means "the method cannot be rendered, render a warning comment instead".
     */
    public static class DaoSqlInfo {

        public final String sql;
        public final List<FieldInfo> fields; // query: returned fields; CRUD create/update: assigned columns
        public final List<FieldInfo> params; // query: parameters; CRUD: generated columns or PK

        DaoSqlInfo(String sql, List<FieldInfo> fields, List<FieldInfo> params) {
            this.sql = sql;
            this.fields = fields;
            this.params = params;
        }
    }

    // Public Utils --------------------------------------------
    //
    // ---------------------------------------------------------

    public FieldNamesMode get_dto_field_names_mode() {
        return this.dto_field_names_mode;
    }

    public String get_target_type_by_type_map(String detected) {
        String target_type_name = jaxb_type_map.get_target_type_name(detected);
        String[] parts = target_type_name.split("->");
        return parts[0].trim();
    }

    public static ResultSet get_tables_rs(Connection conn, String schema_name, boolean include_views) throws SQLException {
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

    public List<FieldInfo> get_dto_fields(DtoClass jaxb_dto_class, String sql_root_abs_path) throws Exception {
        List<FieldInfo> res_dto_fields = new ArrayList<FieldInfo>();
        dto_class_info.get_dto_field_info(false, jaxb_dto_class, sql_root_abs_path, res_dto_fields);
        return res_dto_fields;
    }

    public List<FieldInfo> get_field_info_for_wizard(DtoClass jaxb_dto_class, String sql_root_abs_path) throws Exception {
        return dto_class_info.get_field_info_for_wizard(jaxb_dto_class, sql_root_abs_path);
    }

    // DAO. Raw-SQL -------------------------------------------
    //
    // ---------------------------------------------------------

    public DaoSqlInfo get_dao_query_info(
            String sql_root_abs_path,
            String dao_jaxb_ref,
            String dto_param_type,
            String[] method_param_descriptors,
            String jaxb_dto_or_return_type,
            boolean jaxb_return_type_is_dto,
            List<DtoClass> jaxb_dto_classes) throws Exception {

        List<FieldInfo> fields = new ArrayList<FieldInfo>();
        List<FieldInfo> params = new ArrayList<FieldInfo>();
        String sql = dao_class_info.get_dao_query_info(sql_root_abs_path, dao_jaxb_ref, dto_param_type,
                method_param_descriptors, jaxb_dto_or_return_type,
                jaxb_return_type_is_dto, jaxb_dto_classes, fields, params);
        return new DaoSqlInfo(sql, fields, params);
    }

    public List<FieldInfo> get_dao_exec_dml_params(
            String dao_jdbc_sql,
            String dto_param_type,
            String[] method_param_descriptors) throws Exception {

        FieldNamesMode param_names_mode;
        if (dto_param_type == null || dto_param_type.isEmpty()) {
            param_names_mode = method_params_names_mode;
        } else {
            param_names_mode = dto_field_names_mode;
        }
        List<FieldInfo> res_params = new ArrayList<FieldInfo>();
        JdbcSqlParamInfo.get_jdbc_sql_params_info(conn, jaxb_type_map, jaxb_macros, dao_jdbc_sql,
                param_names_mode, method_param_descriptors, res_params);
        return res_params;
    }

    // DAO. CRUD -----------------------------------------------
    //
    // ---------------------------------------------------------

    // 'fields' are the columns of the INSERT, 'params' are the generated ones
    public DaoSqlInfo get_dao_crud_create_info(
            String dao_table_name,
            DtoClass jaxb_dto_class,
            String generated_col_names) throws Exception {

        HashSet<String> dao_crud_generated_set = new HashSet<String>();
        if (!("*".equals(generated_col_names))) {
            String[] gen_keys_arr = MethodDeclarations.get_listed_items(generated_col_names, false);
            MethodDeclarations.check_duplicates(gen_keys_arr);
            for (String k : gen_keys_arr) {
                dao_crud_generated_set.add(k.toLowerCase());
            }
        }
        List<FieldInfo> fields_not_generated = new ArrayList<FieldInfo>();
        List<FieldInfo> fields_generated = new ArrayList<FieldInfo>();
        dao_class_info.get_dao_fields_for_crud_create(jaxb_dto_class, dao_table_name,
                dao_crud_generated_set, fields_not_generated, fields_generated);
        String sql = SqlUtils.create_crud_create_sql(dao_table_name, fields_not_generated);
        return new DaoSqlInfo(sql, fields_not_generated, fields_generated);
    }

    private JdbcTableInfo _get_table_info_for_crud(
            DtoClass jaxb_dto_class,
            String table_name,
            String explicit_pk) throws Exception {

        return dao_class_info.get_dao_fields_for_crud(jaxb_dto_class, table_name, explicit_pk, sql_root_abs_path);
    }

    // 'fields' are all the columns of the table, 'params' are the PK ones
    public DaoSqlInfo get_dao_crud_read_info(
            String dao_table_name,
            DtoClass jaxb_dto_class,
            boolean fetch_list,
            String explicit_pk) throws Exception {

        JdbcTableInfo tfi = _get_table_info_for_crud(jaxb_dto_class, dao_table_name, explicit_pk);
        List<FieldInfo> fields_all = new ArrayList<FieldInfo>(tfi.fields_all);
        List<FieldInfo> fields_pk = new ArrayList<FieldInfo>(tfi.fields_pk);
        String sql = SqlUtils.create_crud_read_sql(dao_table_name, fields_pk, fetch_list);
        return new DaoSqlInfo(sql, fields_all, fields_pk);
    }

    // 'fields' are the assigned columns, 'params' are the PK ones
    public DaoSqlInfo get_dao_crud_update_info(
            String dao_table_name,
            DtoClass jaxb_dto_class,
            String explicit_pk) throws Exception {

        JdbcTableInfo tfi = _get_table_info_for_crud(jaxb_dto_class, dao_table_name, explicit_pk);
        List<FieldInfo> fields_not_pk = new ArrayList<FieldInfo>(tfi.fields_not_pk);
        List<FieldInfo> fields_pk = new ArrayList<FieldInfo>(tfi.fields_pk);
        String sql = null; // null == just render info comment instead of the method
        if (!fields_not_pk.isEmpty() && !fields_pk.isEmpty()) {
            sql = SqlUtils.create_crud_update_sql(dao_table_name, fields_not_pk, fields_pk);
        }
        return new DaoSqlInfo(sql, fields_not_pk, fields_pk);
    }

    // 'fields' is empty, 'params' are the PK columns
    public DaoSqlInfo get_dao_crud_delete_info(
            String dao_table_name,
            DtoClass jaxb_dto_class,
            String explicit_pk) throws Exception {

        JdbcTableInfo tfi = _get_table_info_for_crud(jaxb_dto_class, dao_table_name, explicit_pk);
        List<FieldInfo> fields_pk = new ArrayList<FieldInfo>(tfi.fields_pk);
        String sql = null; // null == just render info comment instead of the method
        if (!fields_pk.isEmpty()) {
            sql = SqlUtils.create_crud_delete_sql(dao_table_name, fields_pk);
        }
        return new DaoSqlInfo(sql, new ArrayList<FieldInfo>(), fields_pk);
    }

    public static PreparedStatement prepare_jdbc_sql(Connection conn, String jdbc_sql) throws SQLException {
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
