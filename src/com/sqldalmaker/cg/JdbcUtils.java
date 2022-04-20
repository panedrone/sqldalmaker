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

        this.global_markers = new JaxbUtils.JaxbMacros(jaxb_settings.getGlobalMacros());
        this.type_map = new JaxbUtils.JaxbTypeMap(jaxb_settings.getTypeMap());
    }

    public FieldNamesMode get_dto_field_names_mode() {
        return this.dto_field_names_mode;
    }

    private void _refine_dao_fields_by_dto_fields(DtoClass jaxb_dto_class,
                                                  String sql_root_abs_path,
                                                  List<FieldInfo> dao_fields_all) throws Exception {

        List<FieldInfo> dto_fields = new ArrayList<FieldInfo>();
        Map<String, FieldInfo> dto_fields_map = get_dto_field_info(jaxb_dto_class, sql_root_abs_path, dto_fields);
        for (FieldInfo dao_fi : dao_fields_all) {
            String dao_col_name = dao_fi.getColumnName();
            if (dto_fields_map.containsKey(dao_col_name) == false) {
                throw new Exception("DAO column '" + dao_col_name + "' not found among DTO columns ["
                        + _get_column_names(dto_fields) + "]. Ensure upper/lower case.");
            }
            FieldInfo dto_fi = dto_fields_map.get(dao_col_name);
            String dto_fi_target_type_name = dto_fi.getType();
            if (Object.class.getTypeName().equals(dto_fi_target_type_name) == false) {
                // prefer DTO if it is not Object.
                dao_fi.refine_rendered_type(dto_fi_target_type_name);
            }
            String dto_comment = dto_fi.getComment();
            String dao_comment = dao_fi.getComment();
            dao_fi.setComment(dto_comment + " <- " + dao_comment);
        }
    }

    private void _refine_dao_fields_by_dto_fields_for_crud_create(Map<String, FieldInfo> dto_fields_map,
                                                                  HashSet<String> dao_crud_generated_set,
                                                                  List<FieldInfo> dao_fields_all,
                                                                  List<FieldInfo> _dao_fields_not_generated,
                                                                  List<FieldInfo> _dao_fields_generated) throws Exception {
        for (FieldInfo dao_field : dao_fields_all) {
            String dao_col_name = dao_field.getColumnName();
            if (dto_fields_map.containsKey(dao_col_name) == false) {
                throw new Exception("Cannot create mapping for DAO column '" +
                        dao_col_name + "'. Ensure upper/lower case.");
            }
            dao_field.refine_rendered_type(dto_fields_map.get(dao_col_name).getType());
            String gen_dao_col_name = dao_col_name.toLowerCase();
            if (dao_crud_generated_set.contains(gen_dao_col_name)) {
                dao_field.setAI(true);
                _dao_fields_generated.add(dao_field);
                dao_crud_generated_set.remove(gen_dao_col_name); // it must become empty in the end
            } else {
                if (dao_field.isAI()) {
                    _dao_fields_generated.add(dao_field);
                } else {
                    _dao_fields_not_generated.add(dao_field);
                }
            }
        }
        if (dao_crud_generated_set.size() > 0) { // not processed column names remain!
            throw new SQLException("Unknown columns are listed as 'generated': " + dao_crud_generated_set);
        }
    }

    private static FieldInfo _get_ret_field_info(FieldNamesMode field_names_mode,
                                                 String exlicit_ret_type,
                                                 List<FieldInfo> dao_fields) throws Exception {
        String ret_type_name;
        if (exlicit_ret_type != null && exlicit_ret_type.trim().length() > 0) {
            ret_type_name = exlicit_ret_type;
        } else {
            if (dao_fields.isEmpty()) {
                // MySQL sakila example: dao_fields.isEmpty() for 'select inventory_in_stock(?)'
                ret_type_name = Object.class.getName();
            } else {
                ret_type_name = dao_fields.get(0).getType();
            }
        }
        String ret_col_name;
        if (dao_fields.isEmpty()) {
            // MySQL sakila example: dao_fields.isEmpty() for 'select inventory_in_stock(?)'
            ret_col_name = "ret_value";
        } else {
            ret_col_name = dao_fields.get(0).getName();
        }
        return new FieldInfo(field_names_mode, ret_type_name, ret_col_name, "ret-value");
    }

    private InfoDbTable _createTableInfo(String table_name,
                                         String explicit_pk) throws Exception {
        String model = ""; // no model
        return new InfoDbTable(model, conn, type_map, dto_field_names_mode, table_name, explicit_pk);
    }

    private void _get_sql_shortcut_info(String sql_root_abs_path,
                                        String dao_jaxb_ref,
                                        String[] method_param_descriptors,
                                        FieldNamesMode param_names_mode,
                                        String jaxb_dto_or_return_type,
                                        boolean jaxb_return_type_is_dto,
                                        DtoClasses jaxb_dto_classes,
                                        List<FieldInfo> _fields,
                                        List<FieldInfo> _params) throws Exception {

        String[] parts = SqlUtils.parse_sql_shortcut_ref(dao_jaxb_ref);
        String dao_table_name = parts[0];
        InfoDbTable tfi = _createTableInfo(dao_table_name, "*");
        if (jaxb_return_type_is_dto) {
            DtoClass jaxb_dto_class = JaxbUtils.find_jaxb_dto_class(jaxb_dto_or_return_type, jaxb_dto_classes);
            _refine_dao_fields_by_dto_fields(jaxb_dto_class, sql_root_abs_path, tfi.fields_all);
            _fields.addAll(tfi.fields_all);
        } else {
            _fields.add(_get_ret_field_info(dto_field_names_mode, jaxb_dto_or_return_type, tfi.fields_all));
        }
        if (_fields.size() > 0) {
            String comment = _fields.get(0).getComment();
            _fields.get(0).setComment(comment + " [INFO] SQL-shortcut");
        }
        String filter_col_names_str = parts[1];
        String[] filter_col_names = Helpers.get_listed_items(filter_col_names_str, false);
        Helpers.check_duplicates(filter_col_names);
        InfoSqlShortcut.get_all(type_map, param_names_mode,
                method_param_descriptors, tfi.fields_all, filter_col_names, _params);
    }

    private void _fill_by_dto(List<FieldInfo> dto_fields,
                              List<FieldInfo> _fields,
                              StringBuilder error) {

        // no fields from DAO SQL (e.g. for CALL statement) --> just use fields of DTO class:
        _fields.addAll(dto_fields);
        if (error.length() > 0 && _fields.size() > 0) {
            String comment = _fields.get(0).getComment();
            _fields.get(0).setComment(
                    comment + " [INFO] " + error.toString().trim().replace('\r', ' ').replace('\n', ' '));
        }
    }

    private static void _fill_by_dao_and_dto(Map<String, FieldInfo> dto_fields_map,
                                             List<FieldInfo> dto_fields,
                                             List<FieldInfo> dao_fields,
                                             List<FieldInfo> _fields,
                                             StringBuilder error) {

        if (ResultSet.class.getName().equals(dao_fields.get(0).getType())) {
            // the story about PostgreSQL + 'select * from get_tests_by_rating_rc(?)' (UDF
            // returning REFCURSOR)
            _fields.addAll(dto_fields);
            String comment = _fields.get(0).getComment() + " [INFO] Column 0 is of type ResultSet";
            if (error.length() > 0) {
                comment += ", " + error;
            }
            _fields.get(0).setComment(comment);
        } else {
            for (FieldInfo dao_fi : dao_fields) {
                String dao_col_name = dao_fi.getColumnName();
                if (dto_fields_map.containsKey(dao_col_name)) {
                    FieldInfo dto_fi = dto_fields_map.get(dao_col_name);
                    dto_fi.setComment(dto_fi.getComment() + " <- " + dao_fi.getComment());
                    _fields.add(dto_fi);
                }
            }
        }
    }

    private void _get_custom_sql_ret_field_info(String sql_root_abs_path,
                                                String jaxb_dto_or_return_type,
                                                DtoClasses jaxb_dto_classes,
                                                List<FieldInfo> _fields,
                                                List<FieldInfo> dao_fields,
                                                StringBuilder error) throws Exception {

        List<FieldInfo> dto_fields = new ArrayList<FieldInfo>();
        DtoClass jaxb_dto_class = JaxbUtils.find_jaxb_dto_class(jaxb_dto_or_return_type, jaxb_dto_classes);
        Map<String, FieldInfo> dto_fields_map = get_dto_field_info(jaxb_dto_class, sql_root_abs_path, dto_fields);
        if (dao_fields.isEmpty()) {
            _fill_by_dto(dto_fields, _fields, error);
        } else {
            _fill_by_dao_and_dto(dto_fields_map, dto_fields, dao_fields, _fields, error);
        }
        if (_fields.isEmpty()) {
            String msg = _get_mapping_error_msg(dto_fields, dao_fields);
            throw new Exception(msg);
        }
    }

    private void _get_custom_sql_fields_info(String sql_root_abs_path,
                                             String dao_query_jdbc_sql,
                                             String jaxb_dto_or_return_type,
                                             boolean jaxb_return_type_is_dto,
                                             DtoClasses jaxb_dto_classes,
                                             List<FieldInfo> _fields) throws Exception {

        Map<String, FieldInfo> dao_fields_map = new HashMap<String, FieldInfo>();
        List<FieldInfo> dao_fields = new ArrayList<FieldInfo>();
        StringBuilder error = new StringBuilder();
        try {
            // no model!
            InfoCustomSql.get_field_info_by_jdbc_sql("", conn, dto_field_names_mode, dao_query_jdbc_sql, dao_fields_map, dao_fields);
        } catch (Exception e) {
            error.append(e.getMessage());
        }
        if (jaxb_return_type_is_dto) {
            _get_custom_sql_ret_field_info(sql_root_abs_path, jaxb_dto_or_return_type, jaxb_dto_classes, _fields, dao_fields, error);
        } else {
            _fields.add(_get_ret_field_info(dto_field_names_mode, jaxb_dto_or_return_type, dao_fields));
        }
    }

    private static String _get_column_names(List<FieldInfo> fields) {
        List<String> col_names = new ArrayList<String>();
        for (FieldInfo fi : fields) {
            col_names.add(fi.getColumnName());
        }
        return String.join(", ", col_names);
    }

    private static String _get_mapping_error_msg(List<FieldInfo> dto_fields,
                                                 List<FieldInfo> dao_fields) {

        return "DAO columns [" + _get_column_names(dao_fields) + "] not found among DTO columns ["
                + _get_column_names(dto_fields) + "].";
    }

    private FieldNamesMode _refine_method_params_names_mode(String dto_param_type) {
        // if it is something like <query method="get_some_value(MyDTO(m_id, m_date))",
        // use field_names_mode (???)
        FieldNamesMode mode = dto_param_type == null || dto_param_type.length() == 0 ?
                FieldNamesMode.AS_IS : method_params_names_mode;
        return mode;
    }

    // Public Utils --------------------------------------------
    //
    // ---------------------------------------------------------

    public String get_target_type_by_type_map(String detected) {
        String target_type_name = type_map.get_target_type_name(detected);
        return target_type_name;
    }

    public static ResultSet get_tables_rs(Connection conn,
                                          DatabaseMetaData dbmd,
                                          String schema_name,
                                          boolean include_views) throws SQLException {

        String[] types;
        if (include_views) {
            types = new String[]{"TABLE", "VIEW"};
        } else {
            types = new String[]{"TABLE"};
        }
        ResultSet rs_tables;
        String catalog = conn.getCatalog();
        rs_tables = dbmd.getTables(catalog, schema_name, "%", types);
        return rs_tables;
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
        InfoDbTable.validate_table_name(conn, table_name);
    }

    // DTO -----------------------------------------------------
    //
    // ---------------------------------------------------------

    public Map<String, FieldInfo> get_dto_field_info(DtoClass jaxb_dto_class,
                                                     String sql_root_abs_path,
                                                     List<FieldInfo> _dto_fields) throws Exception {

        InfoDtoClass info = new InfoDtoClass(conn, type_map, global_markers, dto_field_names_mode);
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

        _fields.clear();
        _params.clear();
        Helpers.check_duplicates(method_param_descriptors);
        String dao_query_jdbc_sql = SqlUtils.jdbc_sql_by_query_ref(dao_jaxb_ref, sql_root_abs_path);
        FieldNamesMode param_names_mode = _refine_method_params_names_mode(dto_param_type);
        if (SqlUtils.is_sql_shortcut_ref(dao_jaxb_ref)) {
            _get_sql_shortcut_info(sql_root_abs_path, dao_jaxb_ref, method_param_descriptors, param_names_mode,
                    jaxb_dto_or_return_type, jaxb_return_type_is_dto, jaxb_dto_classes, _fields, _params);
        } else {
            _get_custom_sql_fields_info(sql_root_abs_path, dao_query_jdbc_sql,
                    jaxb_dto_or_return_type, jaxb_return_type_is_dto, jaxb_dto_classes, _fields);
            InfoCustomSql.get_jdbc_sql_params_info(conn, type_map, dao_query_jdbc_sql, param_names_mode, method_param_descriptors, _params);
        }
        for (FieldInfo fi : _fields) {
            String type_name = type_map.get_target_type_name(fi.getType());
            fi.refine_rendered_type(type_name);
        }
        return dao_query_jdbc_sql;
    }

    public void get_dao_exec_dml_info(String dao_jdbc_sql,
                                      String dto_param_type,
                                      String[] method_param_descriptors,
                                      List<FieldInfo> _params) throws Exception {

        FieldNamesMode param_names_mode = _refine_method_params_names_mode(dto_param_type);
        InfoCustomSql.get_jdbc_sql_params_info(conn, type_map, dao_jdbc_sql, param_names_mode, method_param_descriptors, _params);
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
        InfoDbTable tfi = _createTableInfo(dao_table_name, "");
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

        InfoDbTable tfi = _createTableInfo(dao_table_name, explicit_pk);
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

        InfoDbTable tfi = _createTableInfo(dao_table_name, explicit_pk);
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

        InfoDbTable tfi = _createTableInfo(dao_table_name, explicit_pk);
        _fields_pk.clear();
        _fields_pk.addAll(tfi.fields_pk);
        if (_fields_pk.isEmpty()) {
            return null; // just render info comment instead of method
        }
        _refine_dao_fields_by_dto_fields(jaxb_dto_class, sql_root_abs_path, _fields_pk);
        return SqlUtils.create_crud_delete_sql(dao_table_name, _fields_pk);
    }
}
