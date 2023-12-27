/*
    Copyright 2011-2023 sqldalmaker@gmail.com
    SQL DAL Maker Website: https://sqldalmaker.sourceforge.net/
    Read LICENSE.txt in the root of this project/archive for details.
 */
package com.sqldalmaker.cg;

import com.sqldalmaker.jaxb.sdm.DtoClass;

import java.sql.Connection;
import java.sql.ResultSet;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

/*
 * 17.10.2023 12:14 1.290
 * 29.09.2023 09:58 1.289
 * 09.04.2023 20:31 1.282
 * 20.01.2023 11:45 1.276
 * 16.11.2022 08:02 1.269
 * 10.05.2022 19:27 1.239
 * 06.05.2022 22:05 1.236
 *
 */
class DaoClassInfo {

    private final Connection conn;
    private final FieldNamesMode dto_field_names_mode;
    private final FieldNamesMode method_params_names_mode;

    private final JaxbMacros jaxb_macros;
    private final JaxbTypeMap jaxb_type_map;

    public DaoClassInfo(
            Connection conn,
            FieldNamesMode dto_field_names_mode,
            FieldNamesMode method_params_names_mode,
            JaxbMacros jaxb_macros,
            JaxbTypeMap jaxb_type_map) {

        this.conn = conn;
        this.dto_field_names_mode = dto_field_names_mode;
        this.method_params_names_mode = method_params_names_mode;
        this.jaxb_macros = jaxb_macros;
        this.jaxb_type_map = jaxb_type_map;
    }

    private FieldInfo _get_ret_field_info(String explicit_ret_type, List<FieldInfo> dao_fields) throws Exception {
        String ret_col_name = "ret_value";
        String ret_type_name;
        if (explicit_ret_type != null && !explicit_ret_type.trim().isEmpty()) {
            ret_type_name = explicit_ret_type;
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

    private void _refine_dao_fields_by_dto_fields(String sql_root_abs_path, DtoClass jaxb_dto_class, List<FieldInfo> dao_fields) throws Exception {

        DtoClassInfo dto_info = new DtoClassInfo(conn, jaxb_type_map, jaxb_macros, dto_field_names_mode);
        boolean ignore_model = true;
        Map<String, FieldInfo> dto_fields_map = dto_info.get_dto_field_info(ignore_model, jaxb_dto_class, sql_root_abs_path, new ArrayList<FieldInfo>());
        Set<FieldInfo> excluded_dao_fields = new HashSet<FieldInfo>();
        for (FieldInfo dao_fi : dao_fields) {
            String dao_col_name = dao_fi.getColumnName();
            if (dto_fields_map.containsKey(dao_col_name) == false) {
                excluded_dao_fields.add(dao_fi);
                continue;
            }
            FieldInfo dto_fi = dto_fields_map.get(dao_col_name);
            // Always prefer DTO field type
            //      1. if its original name is not Object
            //      2. if target type name of DTO field is not "object"
            String dto_fi_original_type_name = dto_fi.getOriginalType();
            String object_target_type_name = jaxb_type_map.get_target_type_name(Object.class.getTypeName());
            if (!Object.class.getTypeName().equals(dto_fi_original_type_name) ||
                    (!object_target_type_name.isEmpty() && !object_target_type_name.equals(dto_fi.getType()))) {
                String dto_fi_target_type_name = dto_fi.getType();
                dao_fi.refine_rendered_type(dto_fi_target_type_name);
                String dto_fi_assign_func = dto_fi.getAssignFunc();
                dao_fi.refine_assign_func(dto_fi_assign_func);
            }
            String dto_comment = dto_fi.getComment();
            String dao_comment = dao_fi.getComment();
            dao_fi.setComment(dto_comment + " <- " + dao_comment);
        }
        for (FieldInfo fi : excluded_dao_fields) {
            dao_fields.remove(fi);
        }
    }

    private void _get_sql_shortcut_info(
            String dao_jaxb_ref,
            String sql_root_abs_path,
            String[] method_param_descriptors,
            FieldNamesMode param_names_mode,
            String jaxb_dto_or_return_type,
            boolean jaxb_return_type_is_dto,
            List<DtoClass> jaxb_dto_classes,
            List<FieldInfo> res_fields,
            List<FieldInfo> res_params) throws Exception {

        res_fields.clear();
        res_params.clear();
        SqlUtils.SqlShortcut shc = SqlUtils.parse_sql_shortcut_ref(dao_jaxb_ref); // class name is not available here
        String dao_table_name = shc.table_name;
        String no_model = "";
        DtoClass jaxb_dto_class = JaxbUtils.find_jaxb_dto_class(jaxb_dto_or_return_type, jaxb_dto_classes);
        JdbcTableInfo table_info = JdbcTableInfo.forDao(no_model, conn, jaxb_type_map, dto_field_names_mode, dao_table_name, "*", jaxb_dto_class);
        String filter_col_names_str = shc.params;
        if (filter_col_names_str != null) {
            String[] filter_col_names = Helpers.get_listed_items(filter_col_names_str, false);
            Helpers.check_duplicates(filter_col_names);
            _get_shortcut_params(param_names_mode, method_param_descriptors, table_info.fields_all, filter_col_names, res_params);
        }
        String col_list = shc.col_names;
        if (col_list == null) {
            res_fields.addAll(table_info.fields_all);
        } else {
            String[] cc = Helpers.get_listed_items(col_list, false);
            Set<String> col_names = new HashSet<String>(Arrays.asList(cc));
            for (FieldInfo fi : table_info.fields_all) {
                if (col_names.contains(fi.getColumnName())) {
                    res_fields.add(fi);
                }
            }
        }
        if (jaxb_return_type_is_dto) {
            // 1) dto class name is available 2) dto info considers jaxb from <field...
            _refine_dao_fields_by_dto_fields(sql_root_abs_path, jaxb_dto_class, res_fields);
        } else {
            // return type is scalar, so dto class name is not available
            FieldInfo ret_fi = _get_ret_field_info(jaxb_dto_or_return_type, table_info.fields_all);
            res_fields.add(ret_fi); // need to enable checks even if no columns in record set
        }
        if (!res_fields.isEmpty()) {
            String comment = res_fields.get(0).getComment();
            res_fields.get(0).setComment(comment + " [INFO] SQL-shortcut");
        }
    }

    public void _get_shortcut_params(
            FieldNamesMode param_names_mode,
            String[] method_param_descriptors,
            List<FieldInfo> _table_fields,
            String[] filter_col_names,
            List<FieldInfo> res_params) throws Exception {

        Map<String, FieldInfo> table_columns = new HashMap<String, FieldInfo>();
        for (FieldInfo fi : _table_fields) {
            String cn = fi.getColumnName();
            table_columns.put(cn, fi);
        }
        List<FieldInfo> fields_filter = new ArrayList<FieldInfo>();
        for (String fcn : filter_col_names) {
            if (!table_columns.containsKey(fcn))
                throw new Exception("'" + fcn + "' not found among table columns ["
                        + _get_column_names(_table_fields) + "]. Ensure upper/lower case.");
            FieldInfo fi = table_columns.get(fcn);
            fields_filter.add(fi);
        }
        // assign param types from table!! without dto-refinement!!!
        if (method_param_descriptors.length != fields_filter.size()) {
            throw new Exception("Invalid SQL-shortcut: method params declared: " + method_param_descriptors.length
                    + ", but SQL params expected: " + fields_filter.size());
        }
        for (int i = 0; i < method_param_descriptors.length; i++) {
            String param_descriptor = method_param_descriptors[i];
            FieldInfo fi = fields_filter.get(i);
            String curr_type = fi.getType();
            String default_param_type_name = jaxb_type_map.get_target_type_name(curr_type);
            FieldInfo pi = JdbcSqlParamInfo.create_param_info(jaxb_type_map, param_names_mode, param_descriptor, default_param_type_name);
            res_params.add(pi);
        }
    }

    private static void _fill_by_dao_and_dto(
            Map<String, FieldInfo> dto_fields_map,
            List<FieldInfo> dto_fields,
            List<FieldInfo> dao_fields_jdbc,
            List<FieldInfo> dao_fields_res,
            StringBuilder error) {

        if (ResultSet.class.getName().equals(dao_fields_jdbc.get(0).getType())) {
            // the story about PostgreSQL + 'select * from get_tests_by_rating_rc(?)' (UDF
            // returning REF_CURSOR)
            dao_fields_res.addAll(dto_fields);
            String comment = dao_fields_res.get(0).getComment() + " [INFO] Column 0 is of type ResultSet";
            if (error.length() > 0) {
                comment += ", " + error;
            }
            dao_fields_res.get(0).setComment(comment);
        } else {
            for (FieldInfo dao_fi : dao_fields_jdbc) {
                String dao_col_name = dao_fi.getColumnName();
                if (dto_fields_map.containsKey(dao_col_name)) {
                    FieldInfo dto_fi = dto_fields_map.get(dao_col_name);
                    dto_fi.setComment(dto_fi.getComment() + " <- " + dao_fi.getComment());
                    dao_fields_res.add(dto_fi);
                }
            }
        }
    }

    private void _fill_by_dto(List<FieldInfo> dto_fields, List<FieldInfo> res_fields, StringBuilder error) {
        // no fields from DAO SQL (e.g. for CALL statement) --> just use fields of DTO class:
        res_fields.addAll(dto_fields);
        if (error.length() > 0 && !res_fields.isEmpty()) {
            String comment = res_fields.get(0).getComment();
            res_fields.get(0).setComment(
                    comment + " [INFO] " + error.toString().trim().replace('\r', ' ').replace('\n', ' '));
        }
    }

    private void _get_custom_sql_ret_field_info(
            String sql_root_abs_path,
            String jaxb_dto_or_return_type,
            List<DtoClass> jaxb_dto_classes,
            List<FieldInfo> dao_fields_jdbc,
            List<FieldInfo> dao_fields_res,
            StringBuilder error) throws Exception {

        // not only tables, se use DtoClassInfo instead of TableInfo
        DtoClassInfo info = new DtoClassInfo(conn, jaxb_type_map, jaxb_macros, dto_field_names_mode);
        List<FieldInfo> dto_fields = new ArrayList<FieldInfo>();
        DtoClass jaxb_dto_class = JaxbUtils.find_jaxb_dto_class(jaxb_dto_or_return_type, jaxb_dto_classes);
        Map<String, FieldInfo> dto_fields_map = info.get_dto_field_info(true, jaxb_dto_class, sql_root_abs_path, dto_fields);
        if (dao_fields_jdbc.isEmpty()) {
            _fill_by_dto(dto_fields, dao_fields_res, error);
        } else {
            _fill_by_dao_and_dto(dto_fields_map, dto_fields, dao_fields_jdbc, dao_fields_res, error);
        }
        if (dao_fields_res.isEmpty()) {
            String msg = _get_mapping_error_msg(dto_fields, dao_fields_jdbc);
            throw new Exception(msg);
        }
    }

    private void _get_custom_sql_fields_info(
            String sql_root_abs_path,
            String dao_query_jdbc_sql,
            String jaxb_dto_or_return_type,
            boolean jaxb_return_type_is_dto,
            List<DtoClass> jaxb_dto_classes,
            List<FieldInfo> dao_fields_res) throws Exception {

        List<FieldInfo> dao_fields_jdbc = new ArrayList<FieldInfo>();
        StringBuilder error = new StringBuilder();
        try {
            Map<String, FieldInfo> dao_fields_map = new HashMap<String, FieldInfo>();
            // no model!
            JdbcSqlFieldInfo.get_field_info_by_jdbc_sql("", conn, dto_field_names_mode, dao_query_jdbc_sql, "", dao_fields_map, dao_fields_jdbc);
        } catch (Exception e) {
            error.append(e.getMessage());
        }
        if (jaxb_return_type_is_dto) {
            _get_custom_sql_ret_field_info(sql_root_abs_path, jaxb_dto_or_return_type, jaxb_dto_classes, dao_fields_jdbc, dao_fields_res, error);
        } else {
            FieldInfo fi = _get_ret_field_info(jaxb_dto_or_return_type, dao_fields_jdbc);
            dao_fields_res.add(fi);
        }
    }

    private static String _get_column_names(List<FieldInfo> fields) {
        List<String> col_names = new ArrayList<String>();
        for (FieldInfo fi : fields) {
            col_names.add(fi.getColumnName());
        }
        return String.join(", ", col_names);
    }

    private static String _get_mapping_error_msg(List<FieldInfo> dto_fields, List<FieldInfo> dao_fields) {
        return "DAO columns [" + _get_column_names(dao_fields) + "] not found among DTO columns ["
                + _get_column_names(dto_fields) + "].";
    }

    private FieldNamesMode _refine_method_params_names_mode(String dto_param_type) {
        // if it is something like <query method="get_some_value(MyDTO(m_id, m_date))",
        // use field_names_mode (???)
        FieldNamesMode mode = dto_param_type == null || dto_param_type.isEmpty() ?
                FieldNamesMode.AS_IS : method_params_names_mode;
        return mode;
    }

    public String get_dao_query_info(
            String sql_root_abs_path,
            String dao_jaxb_ref,
            String dto_param_type,
            String[] method_param_descriptors,
            String jaxb_dto_or_return_type,
            boolean jaxb_return_type_is_dto,
            List<DtoClass> jaxb_dto_classes,
            List<FieldInfo> res_fields,
            List<FieldInfo> res_params) throws Exception {

        res_fields.clear();
        res_params.clear();
        Helpers.check_duplicates(method_param_descriptors);
        if (dao_jaxb_ref == null || dao_jaxb_ref.trim().isEmpty()) { // empty "ref"
            if (!jaxb_return_type_is_dto) {
                throw new Exception("Empty 'ref' is not allowed here");
            }
            String dto_class_name = jaxb_dto_or_return_type;
            DtoClass jaxb_dto = JaxbUtils.find_jaxb_dto_class(dto_class_name, jaxb_dto_classes);
            String dto_jaxb_ref = jaxb_dto.getRef();
            if (dto_jaxb_ref == null || dto_jaxb_ref.trim().isEmpty()) {
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
            JdbcSqlParamInfo.get_jdbc_sql_params_info(conn, jaxb_type_map, dao_query_jdbc_sql, param_names_mode, method_param_descriptors, res_params);
        }
        for (FieldInfo fi : res_fields) {
            String type_name = jaxb_type_map.get_target_type_name(fi.getType());
            fi.refine_rendered_type(type_name);
        }
        return dao_query_jdbc_sql;
    }

    public void get_dao_fields_for_crud_create(
            DtoClass jaxb_dto_class,
            String table_name,
            HashSet<String> dao_crud_auto_columns,
            List<FieldInfo> res_dao_fields_not_generated,
            List<FieldInfo> res_dao_fields_generated) throws Exception {

        // The instance of JdbcTableInfo for "crud-create"
        // should not be cached/shared, because of modifying of FieldInfo

        String no_model = ""; // TODO - info about PK and FK is not needed for crud_create
        JdbcTableInfo t_info = JdbcTableInfo.forDao(no_model, conn, jaxb_type_map, dto_field_names_mode, table_name, "*", jaxb_dto_class);
        List<FieldInfo> dao_fields_all = t_info.fields_all;
        for (FieldInfo dao_field : dao_fields_all) {
            String dao_col_name = dao_field.getColumnName();
            String gen_dao_col_name = dao_col_name.toLowerCase();
            if (dao_crud_auto_columns.contains(gen_dao_col_name)) {
                dao_field.setAI(true);
                res_dao_fields_generated.add(dao_field);
                dao_crud_auto_columns.remove(gen_dao_col_name); // it must become empty in the end
            } else {
                if (dao_field.isAI()) {
                    res_dao_fields_generated.add(dao_field);
                } else {
                    res_dao_fields_not_generated.add(dao_field);
                }
            }
        }
        if (!dao_crud_auto_columns.isEmpty()) { // not processed column names remain!
            throw new Exception("Unknown columns are listed as 'auto': " + dao_crud_auto_columns);
        }
    }

    JdbcTableInfo get_dao_fields_for_crud(
            DtoClass jaxb_dto_class,
            String table_name,
            String explicit_pk,
            String sql_root_abs_path) throws Exception {

        String no_model = "";
        JdbcTableInfo t_info = JdbcTableInfo.forDao(no_model, conn, jaxb_type_map, dto_field_names_mode, table_name, explicit_pk, jaxb_dto_class);
        _refine_dao_fields_by_dto_fields(sql_root_abs_path, jaxb_dto_class, t_info.fields_all);
        return t_info;
    }
}
