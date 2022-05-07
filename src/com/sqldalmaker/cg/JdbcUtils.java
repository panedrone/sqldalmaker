/*
    Copyright 2011-2022 sqldalmaker@gmail.com
    SQL DAL Maker Website: http://sqldalmaker.sourceforge.net
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

    private final JaxbUtils.JaxbMacros global_markers;
    private final JaxbUtils.JaxbTypeMap type_map;

    public JdbcUtils(Connection conn,
                     FieldNamesMode field_names_mode,
                     FieldNamesMode method_params_names_mode,
                     Settings jaxb_settings) throws Exception {

        this.conn = conn;
        this.dto_field_names_mode = field_names_mode;
        this.method_params_names_mode = method_params_names_mode;

        this.global_markers = new JaxbUtils.JaxbMacros(jaxb_settings.getMacros());
        this.type_map = new JaxbUtils.JaxbTypeMap(jaxb_settings.getTypeMap());
    }

    public FieldNamesMode get_dto_field_names_mode() {
        return this.dto_field_names_mode;
    }

    private void _refine_dao_fields_by_dto_fields(DtoClass jaxb_dto_class,
                                                  String sql_root_abs_path,
                                                  List<FieldInfo> dao_fields_all) throws Exception {

        CustomSqlInfo info = new CustomSqlInfo(conn, dto_field_names_mode, method_params_names_mode, global_markers, type_map);
        info.refine_dao_fields_by_dto_fields(jaxb_dto_class, sql_root_abs_path, dao_fields_all);
    }

    private void _refine_dao_fields_by_dto_fields_for_crud_create(Map<String, FieldInfo> dto_fields_map,
                                                                  HashSet<String> dao_crud_generated_set,
                                                                  List<FieldInfo> dao_fields_all,
                                                                  List<FieldInfo> _dao_fields_not_generated,
                                                                  List<FieldInfo> _dao_fields_generated) throws Exception {

        CustomSqlInfo info = new CustomSqlInfo(conn, dto_field_names_mode, method_params_names_mode, global_markers, type_map);
        info.refine_dao_fields_by_dto_fields_for_crud_create(dto_fields_map, dao_crud_generated_set, dao_fields_all,
                _dao_fields_not_generated, _dao_fields_generated);
    }

    private DatabaseTableInfo _createTableInfo(String table_name,
                                               String explicit_pk) throws Exception {
        String model = ""; // no model
        return new DatabaseTableInfo(model, conn, type_map, dto_field_names_mode, table_name, explicit_pk);
    }

    // Public Utils --------------------------------------------
    //
    // ---------------------------------------------------------

    public String get_target_type_by_type_map(String detected) {
        String target_type_name = type_map.get_target_type_name(detected);
        return target_type_name;
    }

    public static ResultSet get_tables_rs(Connection conn,
                                          String schema_name,
                                          boolean include_views) throws SQLException {
        String table_name_parretn;
        if (schema_name == null) {
            table_name_parretn = "%";
        } else {
            table_name_parretn = schema_name + ".%";
        }
        return DatabaseTableInfo.get_tables_rs(conn, table_name_parretn, include_views);
    }

    public static void get_schema_names(Connection con,
                                        List<String> _schema_names) throws SQLException {

        DatabaseMetaData db_info = con.getMetaData();
        ResultSet rs;
        rs = db_info.getSchemas();
        try {
            while (rs.next()) {
                _schema_names.add(rs.getString("TABLE_SCHEM"));
            }
        } finally {
            rs.close();
        }
    }

    public void validate_table_name(String table_name) throws Exception {
        DatabaseTableInfo.validate_table_name(conn, table_name);
    }

    // DTO -----------------------------------------------------
    //
    // ---------------------------------------------------------

    public Map<String, FieldInfo> get_dto_field_info(DtoClass jaxb_dto_class,
                                                     String sql_root_abs_path,
                                                     List<FieldInfo> _dto_fields) throws Exception {

        DtoClassInfo info = new DtoClassInfo(conn, type_map, global_markers, dto_field_names_mode);
        return info.get_dto_field_info(jaxb_dto_class, sql_root_abs_path, _dto_fields);
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
                                     List<FieldInfo> _fields,
                                     List<FieldInfo> _params) throws Exception {

        CustomSqlInfo info = new CustomSqlInfo(conn, dto_field_names_mode, method_params_names_mode, global_markers, type_map);
        String dao_query_jdbc_sql = info.get_dao_query_info(sql_root_abs_path, dao_jaxb_ref, dto_param_type,
                method_param_descriptors, jaxb_dto_or_return_type,
                jaxb_return_type_is_dto, jaxb_dto_classes, _fields, _params);
        return dao_query_jdbc_sql;
    }

    public void get_dao_exec_dml_info(String dao_jdbc_sql,
                                      String dto_param_type,
                                      String[] method_param_descriptors,
                                      List<FieldInfo> _params) throws Exception {

        FieldNamesMode param_names_mode = dto_param_type == null || dto_param_type.length() == 0 ?
                FieldNamesMode.AS_IS : method_params_names_mode;
        CustomSqlUtils.get_jdbc_sql_params_info(conn, type_map, dao_jdbc_sql, param_names_mode, method_param_descriptors, _params);
    }

    // DAO. CRUD -----------------------------------------------
    //
    // ---------------------------------------------------------

    public String get_dao_crud_create_info(DtoClass jaxb_dto_class,
                                           String sql_root_abs_path,
                                           String dao_table_name,
                                           String dao_crud_generated,
                                           List<FieldInfo> _dao_fields_not_generated,
                                           List<FieldInfo> _dao_fields_generated) throws Exception {

        _dao_fields_not_generated.clear();
        _dao_fields_generated.clear();
        HashSet<String> dao_crud_generated_set = new HashSet<String>();
        if (!("*".equals(dao_crud_generated))) {
            String[] gen_keys_arr = Helpers.get_listed_items(dao_crud_generated, false);
            Helpers.check_duplicates(gen_keys_arr);
            for (String k : gen_keys_arr) {
                dao_crud_generated_set.add(k.toLowerCase());
            }
        }
        List<FieldInfo> dto_fields = new ArrayList<FieldInfo>();
        Map<String, FieldInfo> dto_fields_map = get_dto_field_info(jaxb_dto_class, sql_root_abs_path, dto_fields);
        // "" because pk columns are not meaningful for crud create,
        // only generated ones:
        DatabaseTableInfo tfi = _createTableInfo(dao_table_name, "");
        List<FieldInfo> dao_fields_all = tfi.fields_all;
        _refine_dao_fields_by_dto_fields_for_crud_create(dto_fields_map, dao_crud_generated_set, dao_fields_all,
                _dao_fields_not_generated, _dao_fields_generated);
        return SqlUtils.create_crud_create_sql(dao_table_name, _dao_fields_not_generated);
    }

    public String get_dao_crud_read_info(boolean fetch_list,
                                         DtoClass jaxb_dto_class,
                                         String sql_root_abs_path,
                                         String dao_table_name,
                                         String explicit_pk,
                                         List<FieldInfo> _dao_fields_all,
                                         List<FieldInfo> _dao_fields_pk) throws Exception {

        DatabaseTableInfo tfi = _createTableInfo(dao_table_name, explicit_pk);
        _dao_fields_all.clear();
        _dao_fields_all.addAll(tfi.fields_all);
        _dao_fields_pk.clear();
        _dao_fields_pk.addAll(tfi.fields_pk);
        _refine_dao_fields_by_dto_fields(jaxb_dto_class, sql_root_abs_path, _dao_fields_all);
        return SqlUtils.create_crud_read_sql(dao_table_name, _dao_fields_pk, fetch_list);
    }

    public String get_dao_crud_update_info(String dao_table_name,
                                           DtoClass jaxb_dto_class,
                                           String sql_root_abs_path,
                                           String explicit_pk,
                                           List<FieldInfo> _fields_not_pk,
                                           List<FieldInfo> _fields_pk) throws Exception {

        DatabaseTableInfo tfi = _createTableInfo(dao_table_name, explicit_pk);
        _fields_not_pk.clear();
        _fields_not_pk.addAll(tfi.fields_not_pk);
        _fields_pk.clear();
        _fields_pk.addAll(tfi.fields_pk);
        if (_fields_not_pk.isEmpty()) {
            return null; // just render info comment instead of method
        }
        if (_fields_pk.isEmpty()) {
            return null; // just render info comment instead of method
        }
        _refine_dao_fields_by_dto_fields(jaxb_dto_class, sql_root_abs_path, tfi.fields_all);
        return SqlUtils.create_crud_update_sql(dao_table_name, _fields_not_pk, _fields_pk);
    }

    public String get_dao_crud_delete_info(String dao_table_name,
                                           DtoClass jaxb_dto_class,
                                           String sql_root_abs_path,
                                           String explicit_pk,
                                           List<FieldInfo> _fields_pk) throws Exception {

        DatabaseTableInfo tfi = _createTableInfo(dao_table_name, explicit_pk);
        _fields_pk.clear();
        _fields_pk.addAll(tfi.fields_pk);
        if (_fields_pk.isEmpty()) {
            return null; // just render info comment instead of method
        }
        _refine_dao_fields_by_dto_fields(jaxb_dto_class, sql_root_abs_path, _fields_pk);
        return SqlUtils.create_crud_delete_sql(dao_table_name, _fields_pk);
    }
}
