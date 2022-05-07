/*
    Copyright 2011-2022 sqldalmaker@gmail.com
    SQL DAL Maker Website: http://sqldalmaker.sourceforge.net
    Read LICENSE.txt in the root of this project/archive for details.
 */
package com.sqldalmaker.cg;

import com.sqldalmaker.jaxb.dto.DtoClass;
import com.sqldalmaker.jaxb.dto.DtoClasses;

import java.sql.Connection;
import java.sql.ResultSet;
import java.util.*;

public class CustomSqlInfo {

    private final Connection conn;
    private final FieldNamesMode dto_field_names_mode;
    private final FieldNamesMode method_params_names_mode;

    private final JaxbUtils.JaxbMacros global_markers;
    private final JaxbUtils.JaxbTypeMap type_map;

    public CustomSqlInfo(Connection conn,
                         FieldNamesMode dto_field_names_mode,
                         FieldNamesMode method_params_names_mode,
                         JaxbUtils.JaxbMacros global_markers,
                         JaxbUtils.JaxbTypeMap type_map) {

        this.conn = conn;
        this.dto_field_names_mode = dto_field_names_mode;
        this.method_params_names_mode = method_params_names_mode;

        this.global_markers = global_markers;
        this.type_map = type_map;
    }

    private Map<String, FieldInfo> _get_dto_field_info(DtoClass jaxb_dto_class,
                                                       String sql_root_abs_path,
                                                       List<FieldInfo> _dto_fields) throws Exception {

        DtoClassInfo info = new DtoClassInfo(conn, type_map, global_markers, dto_field_names_mode);
        return info.get_dto_field_info(jaxb_dto_class, sql_root_abs_path, _dto_fields);
    }

    public void refine_dao_fields_by_dto_fields(DtoClass jaxb_dto_class,
                                                String sql_root_abs_path,
                                                List<FieldInfo> dao_fields_all) throws Exception {

        List<FieldInfo> dto_fields = new ArrayList<FieldInfo>();
        Map<String, FieldInfo> dto_fields_map = _get_dto_field_info(jaxb_dto_class, sql_root_abs_path, dto_fields);
        for (FieldInfo dao_fi : dao_fields_all) {
            String dao_col_name = dao_fi.getColumnName();
            if (dto_fields_map.containsKey(dao_col_name) == false) {
                throw new Exception("DAO column '" + dao_col_name + "' not found among DTO columns ["
                        + _get_column_names(dto_fields) + "]. Ensure upper/lower case.");
            }
            FieldInfo dto_fi = dto_fields_map.get(dao_col_name);
            // Always prefer DTO field type
            //      1. if ist original name is not Object
            //      2. if target type name of DTO field is not "object"
            String dto_fi_original_type_name = dto_fi.getOriginalType();
            String object_target_type_name = type_map.get_target_type_name(Object.class.getTypeName());
            if (!Object.class.getTypeName().equals(dto_fi_original_type_name) ||
                    (object_target_type_name.length() > 0 && !object_target_type_name.equals(dto_fi.getType()))) {
                String dto_fi_target_type_name = dto_fi.getType();
                dao_fi.refine_rendered_type(dto_fi_target_type_name);
            }
            String dto_comment = dto_fi.getComment();
            String dao_comment = dao_fi.getComment();
            dao_fi.setComment(dto_comment + " <- " + dao_comment);
        }
    }

    public void refine_dao_fields_by_dto_fields_for_crud_create(Map<String, FieldInfo> dto_fields_map,
                                                                 HashSet<String> dao_crud_generated_set,
                                                                 List<FieldInfo> dao_fields_all,
                                                                 List<FieldInfo> _dao_fields_not_generated,
                                                                 List<FieldInfo> _dao_fields_generated) throws Exception {
        for (FieldInfo dao_field : dao_fields_all) {
            String dao_col_name = dao_field.getColumnName();
            if (dto_fields_map.containsKey(dao_col_name) == false) {
                List<FieldInfo> dto_fields = new ArrayList<FieldInfo>(dto_fields_map.values());
                throw new Exception("DAO column '" + dao_col_name + "' not found among DTO columns ["
                        + _get_column_names(dto_fields) + "]. Ensure upper/lower case.");
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
            throw new Exception("Unknown columns are listed as 'generated': " + dao_crud_generated_set);
        }
    }

    private FieldInfo _get_ret_field_info(String exlicit_ret_type,
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
        return new FieldInfo(dto_field_names_mode, ret_type_name, ret_col_name, "ret-value");
    }

    private DatabaseTableInfo _createTableInfo(String table_name) throws Exception {
        String model = ""; // no model
        return new DatabaseTableInfo(model, conn, type_map, dto_field_names_mode, table_name, "*");
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
        DatabaseTableInfo tfi = _createTableInfo(dao_table_name);
        if (jaxb_return_type_is_dto) {
            DtoClass jaxb_dto_class = JaxbUtils.find_jaxb_dto_class(jaxb_dto_or_return_type, jaxb_dto_classes);
            refine_dao_fields_by_dto_fields(jaxb_dto_class, sql_root_abs_path, tfi.fields_all);
            _fields.addAll(tfi.fields_all);
        } else {
            _fields.add(_get_ret_field_info(jaxb_dto_or_return_type, tfi.fields_all));
        }
        if (_fields.size() > 0) {
            String comment = _fields.get(0).getComment();
            _fields.get(0).setComment(comment + " [INFO] SQL-shortcut");
        }
        String filter_col_names_str = parts[1];
        String[] filter_col_names = Helpers.get_listed_items(filter_col_names_str, false);
        Helpers.check_duplicates(filter_col_names);
        CustomSqlUtils.get_shortcut_info(type_map, param_names_mode,
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
        Map<String, FieldInfo> dto_fields_map = _get_dto_field_info(jaxb_dto_class, sql_root_abs_path, dto_fields);
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
            CustomSqlUtils.get_field_info_by_jdbc_sql("", conn, dto_field_names_mode, dao_query_jdbc_sql, dao_fields_map, dao_fields);
        } catch (Exception e) {
            error.append(e.getMessage());
        }
        if (jaxb_return_type_is_dto) {
            _get_custom_sql_ret_field_info(sql_root_abs_path, jaxb_dto_or_return_type, jaxb_dto_classes, _fields, dao_fields, error);
        } else {
            _fields.add(_get_ret_field_info(jaxb_dto_or_return_type, dao_fields));
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
        if (dao_jaxb_ref == null || dao_jaxb_ref.trim().length() == 0) { // empty "ref"
            if (!jaxb_return_type_is_dto) {
                throw new Exception("Empty 'ref' is not allowed here");
            }
            String dto_class_name = jaxb_dto_or_return_type;
            DtoClass jaxb_dto = JaxbUtils.find_jaxb_dto_class(dto_class_name, jaxb_dto_classes);
            String dto_jaxb_ref = jaxb_dto.getRef();
            if (dto_jaxb_ref == null || dto_jaxb_ref.trim().length() == 0) {
                throw new Exception("Both DAO 'ref' and DTO 'ref' are empty");
            }
            dao_jaxb_ref = dto_jaxb_ref;
            if (SqlUtils.is_table_ref(dao_jaxb_ref)) {
                dao_jaxb_ref += "()"; // process it as sql_shortcut
            }
        }
        String dao_query_jdbc_sql = SqlUtils.jdbc_sql_by_query_ref(dao_jaxb_ref, sql_root_abs_path);
        FieldNamesMode param_names_mode = _refine_method_params_names_mode(dto_param_type);
        if (SqlUtils.is_sql_shortcut_ref(dao_jaxb_ref)) {
            _get_sql_shortcut_info(sql_root_abs_path, dao_jaxb_ref, method_param_descriptors, param_names_mode,
                    jaxb_dto_or_return_type, jaxb_return_type_is_dto, jaxb_dto_classes, _fields, _params);
        } else {
            _get_custom_sql_fields_info(sql_root_abs_path, dao_query_jdbc_sql,
                    jaxb_dto_or_return_type, jaxb_return_type_is_dto, jaxb_dto_classes, _fields);
            CustomSqlUtils.get_jdbc_sql_params_info(conn, type_map, dao_query_jdbc_sql, param_names_mode, method_param_descriptors, _params);
        }
        for (FieldInfo fi : _fields) {
            String type_name = type_map.get_target_type_name(fi.getType());
            fi.refine_rendered_type(type_name);
        }
        return dao_query_jdbc_sql;
    }
}
