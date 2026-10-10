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
 * 10.10.2026 1.333 Claude: a facade only; JDBC is in JdbcDbMetadata, the model is built
 *                          by DtoClassInfo/DaoClassInfo from what it reads
 * 30.08.2026 14:00 1.331 Claude refactor
 * 16.11.2022 08:02 1.269
 * 16.04.2022 17:35 1.219
 * 08.05.2021 22:29 1.200
 *
 */
public class JdbcUtils {

    private final FieldNamesMode dto_field_names_mode;

    private final JaxbTypeMap jaxb_type_map;

    private final String sql_root_abs_path;

    private final DtoClassInfo dto_class_info;
    private final DaoClassInfo dao_class_info;

    // JdbcUtils lives for one code generation run
    public JdbcUtils(
            Connection conn,
            FieldNamesMode dto_field_names_mode,
            FieldNamesMode method_params_names_mode,
            Settings jaxb_settings,
            String sql_root_abs_path) throws Exception {

        this.dto_field_names_mode = dto_field_names_mode;
        this.jaxb_type_map = new JaxbTypeMap(jaxb_settings.getTypeMap());
        this.sql_root_abs_path = sql_root_abs_path;
        JaxbMacros jaxb_macros = new JaxbMacros(jaxb_settings.getMacros());
        IDbMetadata md = new JdbcDbMetadata(conn);
        this.dto_class_info = new DtoClassInfo(md, jaxb_type_map, jaxb_macros, dto_field_names_mode);
        this.dao_class_info = new DaoClassInfo(md, dto_class_info, dto_field_names_mode, method_params_names_mode,
                jaxb_macros, jaxb_type_map);
    }

    /**
     * SQL of a DAO method plus the fields it returns and the parameters it takes.
     * 'sql' == null means "the method cannot be rendered, render a warning comment instead".
     * The lists are the caller's own: renderers put together what they need from them.
     */
    public static class DaoSqlInfo {

        public final String sql;
        public final List<FieldInfo> fields; // query, CRUD read: returned columns; CRUD create/update: assigned columns
        public final List<ParamInfo> params; // query: parameters; CRUD create: inserted values; CRUD read/update/delete: PK
        public final List<FieldInfo> generated; // CRUD create: generated columns

        DaoSqlInfo(String sql, List<FieldInfo> fields, List<ParamInfo> params) {
            this(sql, fields, params, new ArrayList<FieldInfo>());
        }

        DaoSqlInfo(String sql, List<FieldInfo> fields, List<ParamInfo> params, List<FieldInfo> generated) {
            this.sql = sql;
            this.fields = new ArrayList<>(fields);
            this.params = new ArrayList<>(params);
            this.generated = new ArrayList<>(generated);
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
        return JdbcDbMetadata.get_tables_rs(conn, table_name_pattern, include_views);
    }

    public static List<String> get_schema_names(Connection con) throws SQLException {
        List<String> res = new ArrayList<>();
        DatabaseMetaData db_info = con.getMetaData();
        ResultSet rs = db_info.getSchemas();
        try {
            while (rs.next()) {
                res.add(rs.getString("TABLE_SCHEM"));
            }
        } finally {
            rs.close();
        }
        return res;
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

    // DTO -----------------------------------------------------
    //
    // ---------------------------------------------------------

    public List<FieldInfo> get_dto_fields(DtoClass jaxb_dto_class, String sql_root_abs_path) throws Exception {
        return new ArrayList<>(dto_class_info.get_dto_fields(false, jaxb_dto_class, sql_root_abs_path));
    }

    public List<FieldInfo> get_field_info_for_wizard(DtoClass jaxb_dto_class, String sql_root_abs_path) throws Exception {
        return new ArrayList<>(dto_class_info.get_field_info_for_wizard(jaxb_dto_class, sql_root_abs_path));
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

        return dao_class_info.get_dao_query_info(sql_root_abs_path, dao_jaxb_ref, dto_param_type,
                method_param_descriptors, jaxb_dto_or_return_type, jaxb_return_type_is_dto, jaxb_dto_classes);
    }

    public List<ParamInfo> get_dao_exec_dml_params(
            String dao_jdbc_sql,
            String dto_param_type,
            String[] method_param_descriptors) throws Exception {

        return new ArrayList<>(dao_class_info.get_dao_exec_dml_params(dao_jdbc_sql, dto_param_type, method_param_descriptors));
    }

    // DAO. CRUD -----------------------------------------------
    //
    // ---------------------------------------------------------

    public DaoSqlInfo get_dao_crud_create_info(
            String dao_table_name,
            DtoClass jaxb_dto_class,
            String generated_col_names) throws Exception {

        return dao_class_info.get_dao_crud_create_info(dao_table_name, jaxb_dto_class, generated_col_names);
    }

    public DaoSqlInfo get_dao_crud_read_info(
            String dao_table_name,
            DtoClass jaxb_dto_class,
            boolean fetch_list,
            String explicit_pk) throws Exception {

        return dao_class_info.get_dao_crud_read_info(dao_table_name, jaxb_dto_class, fetch_list, explicit_pk, sql_root_abs_path);
    }

    public DaoSqlInfo get_dao_crud_update_info(
            String dao_table_name,
            DtoClass jaxb_dto_class,
            String explicit_pk) throws Exception {

        return dao_class_info.get_dao_crud_update_info(dao_table_name, jaxb_dto_class, explicit_pk, sql_root_abs_path);
    }

    public DaoSqlInfo get_dao_crud_delete_info(
            String dao_table_name,
            DtoClass jaxb_dto_class,
            String explicit_pk) throws Exception {

        return dao_class_info.get_dao_crud_delete_info(dao_table_name, jaxb_dto_class, explicit_pk, sql_root_abs_path);
    }
}
