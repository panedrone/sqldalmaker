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

public class DaoClassInfo {

    private final Connection conn;
    private final FieldNamesMode dto_field_names_mode;
    private final FieldNamesMode method_params_names_mode;

    private final JaxbMacros markers;
    private final JaxbTypeMap type_map;

    public DaoClassInfo(Connection conn,
                        FieldNamesMode dto_field_names_mode,
                        FieldNamesMode method_params_names_mode,
                        JaxbMacros markers,
                        JaxbTypeMap type_map) {

        this.conn = conn;
        this.dto_field_names_mode = dto_field_names_mode;
        this.method_params_names_mode = method_params_names_mode;

        this.markers = markers;
        this.type_map = type_map;
    }

    private FieldInfo _get_ret_field_info(String exlicit_ret_type,
                                          List<FieldInfo> dao_fields) throws Exception {
        String ret_col_name = "ret_value";
        String ret_type_name;
        if (exlicit_ret_type != null && exlicit_ret_type.trim().length() > 0) {
            ret_type_name = exlicit_ret_type;
        } else {
            if (dao_fields.isEmpty()) {
                ret_type_name = Object.class.getName();
            } else {
                ret_col_name = dao_fields.get(0).getColumnName();
                ret_type_name = dao_fields.get(0).getType();
            }
        }
        return new FieldInfo(dto_field_names_mode, ret_type_name, ret_col_name, "ret-value");
    }

    private void _refine_dao_fields_by_dto_fields(DtoClass jaxb_dto_class,
                                                  List<FieldInfo> dao_fields_all) throws Exception {

        Map<String, FieldInfo> dto_fields_map = new HashMap<String, FieldInfo>();
        for (FieldInfo dao_fi : dao_fields_all) {
            dto_fields_map.put(dao_fi.getColumnName(), dao_fi);
        }
        DtoClassInfo info = new DtoClassInfo(conn, type_map, markers, dto_field_names_mode);
        List<FieldInfo> dto_fields = new ArrayList<FieldInfo>();
        info.refine_field_info(dto_fields_map, jaxb_dto_class, dto_fields);
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

    private void _get_sql_shortcut_info(String dao_jaxb_ref,
                                        String sql_root_abs_path,
                                        String[] method_param_descriptors,
                                        FieldNamesMode param_names_mode,
                                        String jaxb_dto_or_return_type,
                                        boolean jaxb_return_type_is_dto,
                                        DtoClasses jaxb_dto_classes,
                                        List<FieldInfo> res_fields,
                                        List<FieldInfo> res_params) throws Exception {
        res_fields.clear();
        res_params.clear();
        String[] parts = SqlUtils.parse_sql_shortcut_ref(dao_jaxb_ref); // class name is not available here
        String dao_table_name = parts[0];
        String no_model = ""; // TODO - info about PK and FK is not needed for sql_shortcuts
        DtoClass jaxb_dto_class = JaxbUtils.find_jaxb_dto_class(jaxb_dto_or_return_type, jaxb_dto_classes);
        JdbcTableInfo t_info = new JdbcTableInfo(no_model, conn, type_map, dto_field_names_mode, dao_table_name, "*", jaxb_dto_class.getAuto());
        res_fields.addAll(t_info.fields_all); // always add them all to enable checks in _get_shortcut_info
        if (jaxb_return_type_is_dto) {
            // 1) dto class name is available 2) dto info considers jaxb from <field...
            _refine_dao_fields_by_dto_fields(jaxb_dto_class, res_fields);
        } else {
            // return type is scalar, so dto class name is not available
            FieldInfo ret_fi = _get_ret_field_info(jaxb_dto_or_return_type, t_info.fields_all);
            res_fields.add(ret_fi); // need to enable checks even if no columns in record set
        }
        if (res_fields.size() > 0) {
            String comment = res_fields.get(0).getComment();
            res_fields.get(0).setComment(comment + " [INFO] SQL-shortcut");
        }
        String filter_col_names_str = parts[1];
        String[] filter_col_names = Helpers.get_listed_items(filter_col_names_str, false);
        Helpers.check_duplicates(filter_col_names);
        _get_shortcut_info(param_names_mode, method_param_descriptors, res_fields, filter_col_names, res_params);
    }

    public void _get_shortcut_info(FieldNamesMode param_names_mode,
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
                throw new Exception("Column '" + fcn + "' not found among ["
                        + _get_column_names(fields_all) + "]. Ensure upper/lower case.");
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
            String default_param_type_name = type_map.get_target_type_name(curr_type);
            FieldInfo pi = JdbcSqlParamInfo.create_param_info(type_map, param_names_mode, param_descriptor, default_param_type_name);
            res_params.add(pi);
        }
    }

    private static void _fill_by_dao_and_dto(Map<String, FieldInfo> dto_fields_map,
                                             List<FieldInfo> dto_fields,
                                             List<FieldInfo> dao_fields,
                                             List<FieldInfo> res_fields,
                                             StringBuilder error) {

        if (ResultSet.class.getName().equals(dao_fields.get(0).getType())) {
            // the story about PostgreSQL + 'select * from get_tests_by_rating_rc(?)' (UDF
            // returning REFCURSOR)
            res_fields.addAll(dto_fields);
            String comment = res_fields.get(0).getComment() + " [INFO] Column 0 is of type ResultSet";
            if (error.length() > 0) {
                comment += ", " + error;
            }
            res_fields.get(0).setComment(comment);
        } else {
            for (FieldInfo dao_fi : dao_fields) {
                String dao_col_name = dao_fi.getColumnName();
                if (dto_fields_map.containsKey(dao_col_name)) {
                    FieldInfo dto_fi = dto_fields_map.get(dao_col_name);
                    dto_fi.setComment(dto_fi.getComment() + " <- " + dao_fi.getComment());
                    res_fields.add(dto_fi);
                }
            }
        }
    }

    private void _fill_by_dto(List<FieldInfo> dto_fields,
                              List<FieldInfo> res_fields,
                              StringBuilder error) {

        // no fields from DAO SQL (e.g. for CALL statement) --> just use fields of DTO class:
        res_fields.addAll(dto_fields);
        if (error.length() > 0 && res_fields.size() > 0) {
            String comment = res_fields.get(0).getComment();
            res_fields.get(0).setComment(
                    comment + " [INFO] " + error.toString().trim().replace('\r', ' ').replace('\n', ' '));
        }
    }

    private void _get_custom_sql_ret_field_info(String sql_root_abs_path,
                                                String jaxb_dto_or_return_type,
                                                DtoClasses jaxb_dto_classes,
                                                List<FieldInfo> dao_fields,
                                                List<FieldInfo> res_fields,
                                                StringBuilder error) throws Exception {

        // not only tables, se use DtoClassInfo instead of TableInfo
        DtoClassInfo info = new DtoClassInfo(conn, type_map, markers, dto_field_names_mode);
        List<FieldInfo> dto_fields = new ArrayList<FieldInfo>();
        DtoClass jaxb_dto_class = JaxbUtils.find_jaxb_dto_class(jaxb_dto_or_return_type, jaxb_dto_classes);
        Map<String, FieldInfo> dto_fields_map = info.get_dto_field_info(true, jaxb_dto_class, sql_root_abs_path, dto_fields);
        if (dao_fields.isEmpty()) {
            _fill_by_dto(dto_fields, res_fields, error);
        } else {
            _fill_by_dao_and_dto(dto_fields_map, dto_fields, dao_fields, res_fields, error);
        }
        if (res_fields.isEmpty()) {
            String msg = _get_mapping_error_msg(dto_fields, dao_fields);
            throw new Exception(msg);
        }
    }

    private void _get_custom_sql_fields_info(String sql_root_abs_path,
                                             String dao_query_jdbc_sql,
                                             String jaxb_dto_or_return_type,
                                             boolean jaxb_return_type_is_dto,
                                             DtoClasses jaxb_dto_classes,
                                             List<FieldInfo> res_fields) throws Exception {

        List<FieldInfo> dao_fields = new ArrayList<FieldInfo>();
        StringBuilder error = new StringBuilder();
        try {
            Map<String, FieldInfo> dao_fields_map = new HashMap<String, FieldInfo>();
            // no model!
            JdbcSqlFieldInfo.get_field_info_by_jdbc_sql("", conn, dto_field_names_mode, dao_query_jdbc_sql, "", dao_fields_map, dao_fields);
        } catch (Exception e) {
            error.append(e.getMessage());
        }
        if (jaxb_return_type_is_dto) {
            _get_custom_sql_ret_field_info(sql_root_abs_path, jaxb_dto_or_return_type, jaxb_dto_classes, dao_fields, res_fields, error);
        } else {
            res_fields.add(_get_ret_field_info(jaxb_dto_or_return_type, dao_fields));
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
                                     List<FieldInfo> res_fields,
                                     List<FieldInfo> res_params) throws Exception {
        res_fields.clear();
        res_params.clear();
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
            _get_sql_shortcut_info(dao_jaxb_ref, sql_root_abs_path, method_param_descriptors, param_names_mode,
                    jaxb_dto_or_return_type, jaxb_return_type_is_dto, jaxb_dto_classes, res_fields, res_params);
        } else {
            _get_custom_sql_fields_info(sql_root_abs_path, dao_query_jdbc_sql,
                    jaxb_dto_or_return_type, jaxb_return_type_is_dto, jaxb_dto_classes, res_fields);
            JdbcSqlParamInfo.get_jdbc_sql_params_info(conn, type_map, dao_query_jdbc_sql, param_names_mode, method_param_descriptors, res_params);
        }
        for (FieldInfo fi : res_fields) {
            String type_name = type_map.get_target_type_name(fi.getType());
            fi.refine_rendered_type(type_name);
        }
        return dao_query_jdbc_sql;
    }

    public void get_dao_fields_for_crud_create(DtoClass jaxb_dto_class,
                                               String table_name,
                                               HashSet<String> dao_crud_generated_set,
                                               List<FieldInfo> res_dao_fields_not_generated,
                                               List<FieldInfo> res_dao_fields_generated) throws Exception {

        // The istance of JdbcTableInfo for "crud-create"
        // should not be cached/shared, because of modyfying of FieldInfo

        String no_model = ""; // TODO - info about PK and FK is not needed for crud_create
        JdbcTableInfo t_info = new JdbcTableInfo(no_model, conn, type_map, dto_field_names_mode, table_name, "*", jaxb_dto_class.getAuto());
        List<FieldInfo> dao_fields_all = t_info.fields_all;
        for (FieldInfo dao_field : dao_fields_all) {
            String dao_col_name = dao_field.getColumnName();
            String gen_dao_col_name = dao_col_name.toLowerCase();
            if (dao_crud_generated_set.contains(gen_dao_col_name)) {
                dao_field.setAI(true);
                res_dao_fields_generated.add(dao_field);
                dao_crud_generated_set.remove(gen_dao_col_name); // it must become empty in the end
            } else {
                if (dao_field.isAI()) {
                    res_dao_fields_generated.add(dao_field);
                } else {
                    res_dao_fields_not_generated.add(dao_field);
                }
            }
        }
        if (dao_crud_generated_set.size() > 0) { // not processed column names remain!
            throw new Exception("Unknown columns are listed as 'generated': " + dao_crud_generated_set);
        }
    }

    public JdbcTableInfo get_dao_fields_for_crud(DtoClass jaxb_dto_class,
                                                 String table_name,
                                                 String explicit_pk) throws Exception {
        String no_model = "";
        JdbcTableInfo t_info = new JdbcTableInfo(no_model, conn, type_map, dto_field_names_mode, table_name, explicit_pk, jaxb_dto_class.getAuto());
        _refine_dao_fields_by_dto_fields(jaxb_dto_class, t_info.fields_all);
        return t_info;
    }
}
