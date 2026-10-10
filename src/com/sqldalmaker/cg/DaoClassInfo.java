/*
    Copyright 2011-2026 sqldalmaker@gmail.com
    SQL DAL Maker Website: https://sqldalmaker.sourceforge.net/
    Read LICENSE.txt in the root of this project/archive for details.
 */
package com.sqldalmaker.cg;

import com.sqldalmaker.cg.JdbcUtils.DaoSqlInfo;
import com.sqldalmaker.jaxb.sdm.DtoClass;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/*
 * @author sqldalmaker@gmail.com
 *
 * 10.10.2026 1.333 Claude: reads through IDbMetadata, returns values, owns CRUD SQL info
 * 30.08.2026 14:00 1.331 Claude refactor
 * 30.05.2024 20:00 1.299
 * 17.10.2023 12:14 1.290
 * 29.09.2023 09:58 1.289
 * 09.04.2023 20:31 1.282
 * 20.01.2023 11:45 1.276
 * 16.11.2022 08:02 1.269
 * 10.05.2022 19:27 1.239
 * 06.05.2022 22:05 1.236
 */
class DaoClassInfo {

    private final IDbMetadata md;
    private final DtoClassInfo dto_class_info;

    private final FieldNamesMode dto_field_names_mode;
    private final FieldNamesMode method_params_names_mode;

    private final JaxbMacros jaxb_macros;
    private final JaxbTypeMap jaxb_type_map;

    DaoClassInfo(
            IDbMetadata md,
            DtoClassInfo dto_class_info,
            FieldNamesMode dto_field_names_mode,
            FieldNamesMode method_params_names_mode,
            JaxbMacros jaxb_macros,
            JaxbTypeMap jaxb_type_map) {

        this.md = md;
        this.dto_class_info = dto_class_info;
        this.dto_field_names_mode = dto_field_names_mode;
        this.method_params_names_mode = method_params_names_mode;
        this.jaxb_macros = jaxb_macros;
        this.jaxb_type_map = jaxb_type_map;
    }

    // Raw-SQL ---------------------------------------------------------

    DaoSqlInfo get_dao_query_info(
            String sql_root_abs_path,
            String dao_jaxb_ref,
            String dto_param_type,
            String[] method_param_descriptors,
            String jaxb_dto_or_return_type,
            boolean jaxb_return_type_is_dto,
            List<DtoClass> jaxb_dto_classes) throws Exception {

        MethodDeclarations.check_duplicates(method_param_descriptors);
        dao_jaxb_ref = _refine_dao_ref(jaxb_dto_or_return_type, jaxb_return_type_is_dto, jaxb_dto_classes, dao_jaxb_ref);
        String dao_query_jdbc_sql = SqlUtils.jdbc_sql_by_ref(dao_jaxb_ref, sql_root_abs_path);
        FieldNamesMode param_names_mode = _refine_method_params_names_mode(dto_param_type);
        List<FieldInfo> fields;
        List<ParamInfo> params;
        if (SqlUtils.is_sql_shortcut_ref(dao_jaxb_ref)) {
            SqlUtils.SqlShortcut shc = SqlUtils.parse_sql_shortcut_ref(dao_jaxb_ref); // class name is not available here
            DtoClass jaxb_dto_class = null;
            if (jaxb_return_type_is_dto) {
                jaxb_dto_class = JaxbUtils.find_jaxb_dto_class(jaxb_dto_or_return_type, jaxb_dto_classes);
            }
            TableFields table = TableFields.read(md, jaxb_type_map, dto_field_names_mode, "", shc.table_name, "*", "");
            params = _get_shortcut_params(shc, param_names_mode, method_param_descriptors, table.fields_all);
            fields = _get_shortcut_fields(shc, sql_root_abs_path, jaxb_dto_class, jaxb_dto_or_return_type, table.fields_all);
        } else {
            fields = _get_custom_sql_fields(sql_root_abs_path, dao_query_jdbc_sql,
                    jaxb_dto_or_return_type, jaxb_return_type_is_dto, jaxb_dto_classes);
            params = ParamFields.read(md, jaxb_type_map, jaxb_macros, dao_query_jdbc_sql, param_names_mode, method_param_descriptors);
        }
        List<FieldInfo> res_fields = new ArrayList<>();
        for (FieldInfo fi : fields) {
            String type_name = jaxb_type_map.get_target_type_name(fi.getType());
            res_fields.add(FieldInfo.with_rendered_type(fi, type_name));
        }
        return new DaoSqlInfo(dao_query_jdbc_sql, res_fields, params);
    }

    List<ParamInfo> get_dao_exec_dml_params(
            String dao_jdbc_sql,
            String dto_param_type,
            String[] method_param_descriptors) throws Exception {

        FieldNamesMode param_names_mode;
        if (dto_param_type == null || dto_param_type.isEmpty()) {
            param_names_mode = method_params_names_mode;
        } else {
            param_names_mode = dto_field_names_mode;
        }
        return ParamFields.read(md, jaxb_type_map, jaxb_macros, dao_jdbc_sql, param_names_mode, method_param_descriptors);
    }

    private FieldNamesMode _refine_method_params_names_mode(String dto_param_type) {
        // if it is something like <query method="get_some_value(MyDTO(m_id, m_date))",
        // use field_names_mode (???)
        return dto_param_type == null || dto_param_type.isEmpty() ? FieldNamesMode.AS_IS : method_params_names_mode;
    }

    private static String _refine_dao_ref(
            String jaxb_dto_or_return_type,
            boolean jaxb_return_type_is_dto,
            List<DtoClass> jaxb_dto_classes,
            String dao_jaxb_ref) throws Exception {

        if (dao_jaxb_ref == null || dao_jaxb_ref.trim().isEmpty()) { // empty "ref"
            if (!jaxb_return_type_is_dto) {
                throw new Exception("Empty 'ref' is not allowed here");
            }
            DtoClass jaxb_dto = JaxbUtils.find_jaxb_dto_class(jaxb_dto_or_return_type, jaxb_dto_classes);
            String dto_jaxb_ref = jaxb_dto.getRef();
            if (dto_jaxb_ref == null || dto_jaxb_ref.trim().isEmpty()) {
                throw new Exception("Both DAO 'ref' and DTO 'ref' are empty");
            }
            dao_jaxb_ref = dto_jaxb_ref;
        }
        return dao_jaxb_ref.trim();
    }

    private FieldInfo _get_returned_field_info(String explicit_ret_type, List<FieldInfo> dao_fields) throws Exception {
        String ret_col_name = "ret_value";
        String ret_type_name;
        if (explicit_ret_type != null && !explicit_ret_type.trim().isEmpty()) {
            ret_type_name = explicit_ret_type;
        } else if (dao_fields.isEmpty()) {
            ret_type_name = Object.class.getName();
        } else {
            ret_col_name = dao_fields.get(0).getColumnName();
            ret_type_name = dao_fields.get(0).getType();
        }
        return new FieldInfo(dto_field_names_mode, ret_type_name, ret_col_name, "returned-value");
    }

    // DAO fields that the DTO has, with types and comments of the DTO; the others are excluded
    private List<FieldInfo> _refine_dao_fields_by_dto_fields(
            String sql_root_abs_path,
            DtoClass jaxb_dto_class,
            List<FieldInfo> dao_fields) throws Exception {

        boolean ignore_model = true;
        Map<String, FieldInfo> dto_fields = DtoClassInfo.by_column_name(
                dto_class_info.get_dto_fields(ignore_model, jaxb_dto_class, sql_root_abs_path));
        String object_target_type_name = jaxb_type_map.get_target_type_name(Object.class.getTypeName());
        List<FieldInfo> res = new ArrayList<>();
        for (FieldInfo dao_fi : dao_fields) {
            FieldInfo dto_fi = dto_fields.get(dao_fi.getColumnName());
            if (dto_fi == null) {
                continue; // excluded
            }
            FieldInfo fi = dao_fi;
            // Always prefer DTO field type:
            //      1. if its original name is not Object
            //      2. if target type name of DTO field is not "object"
            if (!Object.class.getTypeName().equals(dto_fi.getOriginalType()) ||
                    (!object_target_type_name.isEmpty() && !object_target_type_name.equals(dto_fi.getType()))) {
                fi = FieldInfo.with_rendered_type(fi, dto_fi.getType());
                fi = FieldInfo.with_assign_func(fi, dto_fi.getAssignFunc());
            }
            res.add(FieldInfo.with_comment(fi, dto_fi.getComment() + " <- " + dao_fi.getComment()));
        }
        return res;
    }

    // SQL-shortcut ----------------------------------------------------

    private List<ParamInfo> _get_shortcut_params(
            SqlUtils.SqlShortcut shc,
            FieldNamesMode param_names_mode,
            String[] method_param_descriptors,
            List<FieldInfo> table_fields) throws Exception {

        List<ParamInfo> res = new ArrayList<>();
        if (shc.params == null) {
            return res;
        }
        String[] filter_col_names = MethodDeclarations.get_listed_items(shc.params, false);
        MethodDeclarations.check_duplicates(filter_col_names);
        Map<String, FieldInfo> table_columns = DtoClassInfo.by_column_name(table_fields);
        List<FieldInfo> fields_filter = new ArrayList<>();
        for (String fcn : filter_col_names) {
            if (!table_columns.containsKey(fcn)) {
                throw new Exception("'" + fcn + "' not found among table columns ["
                        + _get_column_names(table_fields) + "]. Ensure upper/lower case.");
            }
            fields_filter.add(table_columns.get(fcn));
        }
        // assign param types from table!! without dto-refinement!!!
        if (method_param_descriptors.length != fields_filter.size()) {
            throw new Exception("Invalid SQL-shortcut: method params declared: " + method_param_descriptors.length
                    + ", but SQL params expected: " + fields_filter.size());
        }
        for (int i = 0; i < method_param_descriptors.length; i++) {
            res.add(ParamFields.create_param_info(jaxb_type_map, jaxb_macros, param_names_mode,
                    method_param_descriptors[i], fields_filter.get(i)));
        }
        return res;
    }

    private List<FieldInfo> _get_shortcut_fields(
            SqlUtils.SqlShortcut shc,
            String sql_root_abs_path,
            DtoClass jaxb_dto_class, // null == scalar
            String jaxb_return_type,
            List<FieldInfo> table_fields) throws Exception {

        List<FieldInfo> res = new ArrayList<>();
        if (shc.col_names == null) {
            res.addAll(table_fields);
        } else {
            String[] cc = MethodDeclarations.get_listed_items(shc.col_names, false);
            Set<String> col_names = new HashSet<>(Arrays.asList(cc));
            for (FieldInfo fi : table_fields) {
                if (col_names.contains(fi.getColumnName())) {
                    res.add(fi);
                }
            }
        }
        if (jaxb_dto_class != null) {
            // 1) dto class name is available 2) dto info considers jaxb from <field...
            res = _refine_dao_fields_by_dto_fields(sql_root_abs_path, jaxb_dto_class, res);
        } else {
            // return type is scalar, so dto class name is not available
            res.add(_get_returned_field_info(jaxb_return_type, table_fields)); // need to enable checks even if no columns in record set
        }
        if (!res.isEmpty()) {
            FieldInfo fi = res.get(0);
            res.set(0, FieldInfo.with_comment(fi, fi.getComment() + " [INFO] SQL-shortcut"));
        }
        return res;
    }

    // custom SQL ------------------------------------------------------

    private List<FieldInfo> _get_custom_sql_fields(
            String sql_root_abs_path,
            String dao_query_jdbc_sql,
            String jaxb_dto_or_return_type,
            boolean jaxb_return_type_is_dto,
            List<DtoClass> jaxb_dto_classes) throws Exception {

        List<FieldInfo> dao_fields_jdbc = new ArrayList<>();
        StringBuilder error = new StringBuilder();
        try {
            // no model!
            dao_fields_jdbc.addAll(ColumnFields.create(md.read_sql_columns(dao_query_jdbc_sql), "", dto_field_names_mode, ""));
            for (int i = 0; i < dao_fields_jdbc.size(); i++) {
                FieldInfo fi = dao_fields_jdbc.get(i);
                String target_type = jaxb_type_map.get_target_type_name(fi.getType());
                target_type = jaxb_macros.parse_target_type_name(target_type, fi);
                dao_fields_jdbc.set(i, FieldInfo.with_rendered_type(fi, target_type));
            }
        } catch (Exception e) {
            error.append(e.getMessage());
        }
        List<FieldInfo> res = new ArrayList<>();
        if (!jaxb_return_type_is_dto) {
            res.add(_get_returned_field_info(jaxb_dto_or_return_type, dao_fields_jdbc));
            return res;
        }
        // not only tables, se use DtoClassInfo instead of TableFields
        DtoClass jaxb_dto_class = JaxbUtils.find_jaxb_dto_class(jaxb_dto_or_return_type, jaxb_dto_classes);
        List<FieldInfo> dto_fields = dto_class_info.get_dto_fields(true, jaxb_dto_class, sql_root_abs_path);
        if (dao_fields_jdbc.isEmpty()) {
            // no fields from DAO SQL (e.g. for CALL statement) --> just use fields of DTO class:
            res.addAll(dto_fields);
            if (error.length() > 0 && !res.isEmpty()) {
                _append_comment_0(res, " [INFO] " + error.toString().trim().replace('\r', ' ').replace('\n', ' '));
            }
        } else if (ResultSet.class.getName().equals(dao_fields_jdbc.get(0).getType())) {
            // the story about PostgreSQL + 'select * from get_tests_by_rating_rc(?)' (UDF
            // returning REF_CURSOR)
            res.addAll(dto_fields);
            String comment = " [INFO] Column 0 is of type ResultSet";
            if (error.length() > 0) {
                comment += ", " + error;
            }
            _append_comment_0(res, comment);
        } else {
            Map<String, FieldInfo> dto_fields_map = DtoClassInfo.by_column_name(dto_fields);
            for (FieldInfo dao_fi : dao_fields_jdbc) {
                FieldInfo dto_fi = dto_fields_map.get(dao_fi.getColumnName());
                if (dto_fi != null) {
                    res.add(FieldInfo.with_comment(dto_fi, dto_fi.getComment() + " <- " + dao_fi.getComment()));
                }
            }
        }
        if (res.isEmpty()) {
            throw new Exception("DAO columns [" + _get_column_names(dao_fields_jdbc) + "] not found among DTO columns ["
                    + _get_column_names(dto_fields) + "].");
        }
        return res;
    }

    private static void _append_comment_0(List<FieldInfo> fields, String comment) {
        FieldInfo fi = fields.get(0);
        fields.set(0, FieldInfo.with_comment(fi, fi.getComment() + comment));
    }

    private static String _get_column_names(List<FieldInfo> fields) {
        return String.join(", ", ColumnFields.column_names(fields));
    }

    // CRUD ------------------------------------------------------------

    // read/update/delete of one <crud> ask for the same table one after another
    private final Map<String, TableFields> crud_tables = new HashMap<>();

    // 'fields' are the columns of the INSERT, 'params' are their values, 'generated' are the generated ones
    DaoSqlInfo get_dao_crud_create_info(
            String dao_table_name,
            DtoClass jaxb_dto_class,
            String generated_col_names) throws Exception {

        Set<String> auto_columns = new HashSet<>();
        if (!("*".equals(generated_col_names))) {
            String[] gen_keys_arr = MethodDeclarations.get_listed_items(generated_col_names, false);
            MethodDeclarations.check_duplicates(gen_keys_arr);
            for (String k : gen_keys_arr) {
                auto_columns.add(k.toLowerCase());
            }
        }
        TableFields table = TableFields.read(md, jaxb_type_map, dto_field_names_mode, "", dao_table_name, "*",
                jaxb_dto_class.getAuto());
        List<FieldInfo> fields_not_generated = new ArrayList<>();
        List<FieldInfo> fields_generated = new ArrayList<>();
        for (FieldInfo fi : table.fields_all) {
            String gen_col_name = fi.getColumnName().toLowerCase();
            if (auto_columns.remove(gen_col_name)) { // it must become empty in the end
                fields_generated.add(FieldInfo.with_ai(fi, true));
            } else if (fi.isAI()) {
                fields_generated.add(fi);
            } else {
                fields_not_generated.add(fi);
            }
        }
        if (!auto_columns.isEmpty()) { // not processed column names remain!
            throw new Exception("Unknown columns are listed as 'auto': " + auto_columns);
        }
        String sql = SqlUtils.create_crud_create_sql(dao_table_name, fields_not_generated);
        return new DaoSqlInfo(sql, fields_not_generated, ParamInfo.of(fields_not_generated), fields_generated);
    }

    // 'fields' are all the columns of the table, 'params' are the PK ones
    DaoSqlInfo get_dao_crud_read_info(
            String dao_table_name,
            DtoClass jaxb_dto_class,
            boolean fetch_list,
            String explicit_pk,
            String sql_root_abs_path) throws Exception {

        TableFields table = _get_crud_table(jaxb_dto_class, dao_table_name, explicit_pk, sql_root_abs_path);
        String sql = SqlUtils.create_crud_read_sql(dao_table_name, table.fields_pk, fetch_list);
        return new DaoSqlInfo(sql, table.fields_all, ParamInfo.of(table.fields_pk));
    }

    // 'fields' are the assigned columns, 'params' are the PK ones
    DaoSqlInfo get_dao_crud_update_info(
            String dao_table_name,
            DtoClass jaxb_dto_class,
            String explicit_pk,
            String sql_root_abs_path) throws Exception {

        TableFields table = _get_crud_table(jaxb_dto_class, dao_table_name, explicit_pk, sql_root_abs_path);
        String sql = null; // null == just render info comment instead of the method
        if (!table.fields_not_pk.isEmpty() && !table.fields_pk.isEmpty()) {
            sql = SqlUtils.create_crud_update_sql(dao_table_name, table.fields_not_pk, table.fields_pk);
        }
        return new DaoSqlInfo(sql, table.fields_not_pk, ParamInfo.of(table.fields_pk));
    }

    // 'fields' is empty, 'params' are the PK columns
    DaoSqlInfo get_dao_crud_delete_info(
            String dao_table_name,
            DtoClass jaxb_dto_class,
            String explicit_pk,
            String sql_root_abs_path) throws Exception {

        TableFields table = _get_crud_table(jaxb_dto_class, dao_table_name, explicit_pk, sql_root_abs_path);
        String sql = null; // null == just render info comment instead of the method
        if (!table.fields_pk.isEmpty()) {
            sql = SqlUtils.create_crud_delete_sql(dao_table_name, table.fields_pk);
        }
        return new DaoSqlInfo(sql, new ArrayList<FieldInfo>(), ParamInfo.of(table.fields_pk));
    }

    private TableFields _get_crud_table(
            DtoClass jaxb_dto_class,
            String table_name,
            String explicit_pk,
            String sql_root_abs_path) throws Exception {

        String key = jaxb_dto_class.getName() + "|" + table_name + "|" + explicit_pk;
        TableFields res = crud_tables.get(key);
        if (res == null) {
            res = _create_crud_table(jaxb_dto_class, table_name, explicit_pk, sql_root_abs_path);
            crud_tables.put(key, res);
        }
        return res;
    }

    private TableFields _create_crud_table(
            DtoClass jaxb_dto_class,
            String table_name,
            String explicit_pk,
            String sql_root_abs_path) throws Exception {

        TableFields table = TableFields.read(md, jaxb_type_map, dto_field_names_mode, "", table_name, explicit_pk,
                jaxb_dto_class.getAuto());
        List<FieldInfo> all = _refine_dao_fields_by_dto_fields(sql_root_abs_path, jaxb_dto_class, table.fields_all);
        // the columns missing in DTO are excluded from 'fields_all' only: 'fields_pk' and
        // 'fields_not_pk' keep them as they are
        Map<String, FieldInfo> refined = DtoClassInfo.by_column_name(all);
        return new TableFields(all, _take_refined(table.fields_pk, refined), _take_refined(table.fields_not_pk, refined));
    }

    private static List<FieldInfo> _take_refined(List<FieldInfo> fields, Map<String, FieldInfo> refined) {
        List<FieldInfo> res = new ArrayList<>();
        for (FieldInfo fi : fields) {
            FieldInfo refined_fi = refined.get(fi.getColumnName());
            res.add(refined_fi == null ? fi : refined_fi);
        }
        return res;
    }
}
