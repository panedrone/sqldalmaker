/*
    Copyright 2011-2024 sqldalmaker@gmail.com
    SQL DAL Maker Website: https://sqldalmaker.sourceforge.net/
    Read LICENSE.txt in the root of this project/archive for details.
 */
package com.sqldalmaker.cg.cpp;

import com.sqldalmaker.cg.*;
import com.sqldalmaker.jaxb.sdm.*;
import com.sqldalmaker.jaxb.sdm.DtoClass;
import com.sqldalmaker.jaxb.settings.Settings;

import java.io.StringWriter;
import java.sql.Connection;
import java.util.*;

/**
 * @author sqldalmaker@gmail.com
 */
public class CppCG {

    private static String get_template_path() {
        return Helpers.class.getPackage().getName().replace('.', '/') + "/cpp/cpp.vm";
    }

    public static class DTO implements IDtoCG {

        private final String sql_root_abs_path;
        private final List<DtoClass> jaxb_dto_classes;
        private final TemplateEngine te;
        private final JdbcUtils db_utils;
        private final String dto_class_prefix;

        public DTO(
                Sdm sdm,
                Settings jaxb_settings,
                Connection connection,
                String sql_root_abs_path,
                String dto_class_prefix,
                String vm_template) throws Exception {

            this.jaxb_dto_classes = sdm.getDtoClass();
            this.dto_class_prefix = dto_class_prefix;
            this.sql_root_abs_path = sql_root_abs_path;
            if (vm_template == null) {
                te = new TemplateEngine(get_template_path(), false);
            } else {
                te = new TemplateEngine(vm_template, "cpp");
            }
            db_utils = new JdbcUtils(connection, FieldNamesMode.AS_IS, FieldNamesMode.AS_IS, jaxb_settings, sql_root_abs_path);
        }

        @Override
        public String[] translate(String dto_class_base_name) throws Exception {
            DtoClass jaxb_dto_class = JaxbUtils.find_jaxb_dto_class(dto_class_base_name, jaxb_dto_classes);
            List<FieldInfo> fields = new ArrayList<FieldInfo>();
            db_utils.get_dto_field_info(jaxb_dto_class, sql_root_abs_path, fields);
            Map<String, Object> context = new HashMap<String, Object>();
            context.put("class_name", dto_class_prefix + dto_class_base_name);
            context.put("fields", fields);
            context.put("mode", "dto_class");
            StringWriter sw = new StringWriter();
            te.merge(context, sw);
            String text = sw.toString();
            return new String[]{text};
        }
    }

    public static class DAO implements IDaoCG {

        private final String sql_root_abs_path;
        private final String class_prefix;
        private final List<DtoClass> jaxb_dto_classes;
        private final Set<String> imports = new HashSet<String>();
        private final TemplateEngine te;
        private final JdbcUtils db_utils;

        public DAO(
                List<DtoClass> jaxb_dto_classes,
                Settings jaxb_settings,
                Connection connection,
                String sql_root_abs_path,
                String class_prefix,
                String vm_template) throws Exception {

            this.jaxb_dto_classes = jaxb_dto_classes;
            this.sql_root_abs_path = sql_root_abs_path;
            this.class_prefix = class_prefix;
            if (vm_template == null) {
                te = new TemplateEngine(get_template_path(), false);
            } else {
                te = new TemplateEngine(vm_template, "cpp");
            }
            db_utils = new JdbcUtils(connection, FieldNamesMode.AS_IS, FieldNamesMode.AS_IS, jaxb_settings, sql_root_abs_path);
        }

        @Override
        public String[] translate(DaoClass dao_class) throws Exception {
            imports.clear();
            imports.add("DataStore.h");
            String dao_class_name = dao_class.getName();
            List<String> methods = new ArrayList<String>();
            JaxbUtils.process_jaxb_dao_class(this, dao_class_name, dao_class, methods);
            Map<String, Object> context = new HashMap<String, Object>();
            String[] arr = new String[imports.size()];
            String[] imports_arr = imports.toArray(arr);
            Arrays.sort(imports_arr);
            context.put("imports", imports_arr);
            context.put("class_name", class_prefix + dao_class_name);
            context.put("methods", methods);
            context.put("mode", "dao_class");
            StringWriter sw = new StringWriter();
            te.merge(context, sw);
            String text = sw.toString();
            return new String[]{text};
        }

        @Override
        public StringBuilder render_jaxb_query(Object jaxb_element) throws Exception {
            QueryMethodInfo mi = new QueryMethodInfo(jaxb_element);
            String xml_node_name = JaxbUtils.get_jaxb_node_name(jaxb_element);
            Helpers.check_required_attr(xml_node_name, mi.jaxb_method);
            try {
                if (mi.return_type_is_dto) {
                    _process_dto_class_name(mi.jaxb_dto_or_return_type);
                }
                String[] parsed = _parse_method_declaration2(mi.jaxb_method);
                String method_name = parsed[0];
                String param_descriptors = parsed[2];
                String[] method_param_descriptors = Helpers.get_listed_items(param_descriptors, false);
                List<FieldInfo> fields = new ArrayList<FieldInfo>();
                List<FieldInfo> params = new ArrayList<FieldInfo>();
                String dao_query_jdbc_sql = db_utils.get_dao_query_info(
                        sql_root_abs_path, mi.jaxb_ref, "", method_param_descriptors,
                        mi.jaxb_dto_or_return_type, mi.return_type_is_dto, jaxb_dto_classes, fields, params);
                return _render_query(dao_query_jdbc_sql, mi.jaxb_is_external_sql,
                        mi.jaxb_dto_or_return_type, mi.return_type_is_dto, mi.fetch_list,
                        method_name, null, fields, params);
            } catch (Exception e) {
                // e.printStackTrace();
                String msg = "<" + xml_node_name + " method=\"" + mi.jaxb_method + "\" ref=\"" + mi.jaxb_ref
                        + "\"...\n";
                throw new Exception(Helpers.get_error_message(msg, e));
            }
        }

        //////////////////////////////////////////////////////////////////
        //
        // this method is called from both 'render_jaxb_query' and 'render_crud_read'
        //
        private StringBuilder _render_query(
                String dao_query_jdbc_sql,
                boolean is_external_sql,
                String jaxb_dto_or_return_type,
                boolean jaxb_return_type_is_dto,
                boolean fetch_list,
                String method_name,
                String crud_table,
                List<FieldInfo> fields_all,
                List<FieldInfo> fields_pk) throws Exception {

            if (dao_query_jdbc_sql == null) {
                return Helpers.get_no_pk_warning(method_name);
            }
            String returned_type_name;
            if (jaxb_return_type_is_dto) {
                returned_type_name = _get_rendered_dto_class_name(jaxb_dto_or_return_type);
            } else {
                FieldInfo fi = fields_all.get(0);
                String curr_type = fi.getType();
                returned_type_name = this.db_utils.get_target_type_by_type_map(curr_type);
            }
            String cpp_sql_str = SqlUtils.jdbc_sql_to_cpp_str(dao_query_jdbc_sql);
            Map<String, Object> context = new HashMap<String, Object>();
            context.put("mode", "dao_query");
            context.put("fields", fields_all);
            context.put("method_name", method_name);
            context.put("crud", crud_table != null);
            context.put("ref", crud_table);
            context.put("sql", cpp_sql_str);
            context.put("return_type_is_dto", jaxb_return_type_is_dto);
            context.put("returned_type_name", returned_type_name);
            context.put("fetch_list", fetch_list);
            context.put("imports", imports);
            context.put("is_external_sql", is_external_sql);
            _assign_params(fields_pk, context);
            StringWriter sw = new StringWriter();
            te.merge(context, sw);
            StringBuilder buff = new StringBuilder();
            buff.append(sw.getBuffer());
            return buff;
        }

        private String _get_rendered_dto_class_name(String dto_class_name) throws Exception {
            DtoClass jaxb_dto_class = JaxbUtils.find_jaxb_dto_class(dto_class_name, jaxb_dto_classes);
            return class_prefix + jaxb_dto_class.getName();
        }

        private void _process_dto_class_name(String dto_class_name) throws Exception {
            DtoClass jaxb_dto_class = JaxbUtils.find_jaxb_dto_class(dto_class_name, jaxb_dto_classes);
            imports.add(jaxb_dto_class.getName() + ".h");
        }

        @Override
        public StringBuilder render_jaxb_exec_dml(ExecDml jaxb_exec_dml) throws Exception {
            String method = jaxb_exec_dml.getMethod();
            String ref = jaxb_exec_dml.getRef();
            String xml_node_name = JaxbUtils.get_jaxb_node_name(jaxb_exec_dml);
            Helpers.check_required_attr(xml_node_name, method);
            try {
                String dao_jdbc_sql = SqlUtils.jdbc_sql_by_exec_dml_ref(ref, sql_root_abs_path);
                String[] parsed = _parse_method_declaration2(method);
                String method_name = parsed[0]; // never is null
                String param_descriptors = parsed[1]; // never is null
                String[] method_param_descriptors = Helpers.get_listed_items(param_descriptors, true);
                boolean is_external_sql = jaxb_exec_dml.isExternalSql();
                StringBuilder buff = new StringBuilder();
                _render_exec_dml(buff, dao_jdbc_sql, is_external_sql, method_name, method_param_descriptors, xml_node_name, ref);
                return buff;
            } catch (Exception e) {
                // e.printStackTrace();
                String msg = "<" + xml_node_name + " method=\"" + method + "\" ref=\"" + ref + "\"...\n";
                throw new Exception(Helpers.get_error_message(msg, e));
            }
        }

        // it is used only in render_jaxb_exec_dml
        private void _render_exec_dml(
                StringBuilder buffer,
                String dao_jdbc_sql,
                boolean is_external_sql,
                String method_name,
                String[] param_descriptors,
                String xml_node_name,
                String sql_path) throws Exception {

            List<FieldInfo> params = new ArrayList<FieldInfo>();
            db_utils.get_dao_exec_dml_info(dao_jdbc_sql, "", param_descriptors, params);
            String sql_str = SqlUtils.jdbc_sql_to_cpp_str(dao_jdbc_sql);
            Map<String, Object> context = new HashMap<String, Object>();
            _assign_params(params, context);
            context.put("method_name", method_name);
            context.put("sql", sql_str);
            context.put("xml_node_name", xml_node_name);
            context.put("sql_path", sql_path);
            context.put("external_sql", is_external_sql);
            context.put("mode", "dao_exec_dml");
            StringWriter sw = new StringWriter();
            te.merge(context, sw);
            buffer.append(sw.getBuffer());
        }

        private void _assign_params(List<FieldInfo> params, Map<String, Object> context) {
            context.put("dto_param", "");
            context.put("params", params);
        }

        private String[] _parse_method_declaration2(String method_text) throws Exception {
            String dto_param_type = "";
            String param_descriptors = "";
            String method_name;
            String[] parts = Helpers.parse_method_params(method_text);
            method_name = parts[0];
            if (!("".equals(parts[1]))) {
                parts = Helpers.parse_method_params(parts[1]);
                if (!("".equals(parts[1]))) {
                    throw new Exception("Invalid params: " + method_text);
                } else {
                    param_descriptors = parts[0];
                }
            }
            return new String[]{method_name, dto_param_type, param_descriptors};
        }

        @Override
        public StringBuilder render_crud_create(
                String class_name,
                String method_name,
                String table_name,
                String dto_class_name,
                boolean fetch_generated,
                String generated) throws Exception {

            List<FieldInfo> fields_not_ai = new ArrayList<FieldInfo>();
            List<FieldInfo> fields_ai = new ArrayList<FieldInfo>();
            DtoClass jaxb_dto_class = JaxbUtils.find_jaxb_dto_class(dto_class_name, jaxb_dto_classes);
            String dao_jdbc_sql = db_utils.get_dao_crud_create_info(table_name, jaxb_dto_class, generated, fields_not_ai, fields_ai);
            String sql_str = SqlUtils.jdbc_sql_to_cpp_str(dao_jdbc_sql);
            Map<String, Object> context = new HashMap<String, Object>();
            context.put("method_type", "CREATE");
            context.put("table_name", table_name);
            context.put("class_name", class_name);
            context.put("sql", sql_str);
            context.put("method_name", method_name);
            context.put("params", fields_not_ai);
            context.put("dto_param", _get_rendered_dto_class_name(dto_class_name));
            if (fetch_generated && !fields_ai.isEmpty()) {
                context.put("keys", fields_ai);
                context.put("mode", "dao_create");
            } else {
                context.put("external_sql", false);
                context.put("mode", "dao_exec_dml");
            }
            StringWriter sw = new StringWriter();
            te.merge(context, sw);
            StringBuilder buffer = new StringBuilder();
            buffer.append(sw.getBuffer());
            return buffer;
        }

        @Override
        public StringBuilder render_crud_read(
                String method_name,
                String dao_table_name,
                String dto_class_name,
                String explicit_pk,
                boolean fetch_list) throws Exception {

            List<FieldInfo> fields_all = new ArrayList<FieldInfo>();
            List<FieldInfo> fields_pk = new ArrayList<FieldInfo>();
            DtoClass jaxb_dto_class = JaxbUtils.find_jaxb_dto_class(dto_class_name, jaxb_dto_classes);
            String dao_jdbc_sql = db_utils.get_dao_crud_read_info(dao_table_name, jaxb_dto_class, fetch_list, explicit_pk, fields_all, fields_pk);
            return _render_query(dao_jdbc_sql, false, dto_class_name, true, fetch_list,
                    method_name, dao_table_name, fields_all, fields_pk);
        }

        @Override
        public StringBuilder render_crud_update(
                String dao_class_name,
                String method_name,
                String table_name,
                String explicit_pk,
                String dto_class_name,
                boolean scalar_params) throws Exception {

            List<FieldInfo> updated_fields = new ArrayList<FieldInfo>();
            List<FieldInfo> fields_pk = new ArrayList<FieldInfo>();
            DtoClass jaxb_dto_class = JaxbUtils.find_jaxb_dto_class(dto_class_name, jaxb_dto_classes);
            String dao_jdbc_sql = db_utils.get_dao_crud_update_info(table_name, jaxb_dto_class, explicit_pk, updated_fields, fields_pk);
            if (fields_pk.isEmpty()) {
                return Helpers.get_no_pk_warning(method_name);
            }
            if (updated_fields.isEmpty()) {
                return Helpers.get_only_pk_warning(method_name);
            }
            String sql_str = SqlUtils.jdbc_sql_to_cpp_str(dao_jdbc_sql);
            updated_fields.addAll(fields_pk);
            Map<String, Object> context = new HashMap<String, Object>();
            context.put("mode", "dao_exec_dml");
            context.put("dao_class_name", dao_class_name);
            context.put("method_name", method_name);
            context.put("sql", sql_str);
            context.put("method_type", "UPDATE");
            context.put("table_name", table_name);
            context.put("comment", "Updates specified record of the table '" + table_name + "'.");
            context.put("dto_param", scalar_params ? "" : _get_rendered_dto_class_name(dto_class_name));
            context.put("params", updated_fields);
            context.put("external_sql", false);
            StringWriter sw = new StringWriter();
            te.merge(context, sw);
            StringBuilder buffer = new StringBuilder();
            buffer.append(sw.getBuffer());
            return buffer;
        }

        @Override
        public StringBuilder render_crud_delete(
                String dao_class_name,
                String dto_class_name,
                String method_name,
                String table_name,
                String explicit_pk) throws Exception {

            List<FieldInfo> fields_pk = new ArrayList<FieldInfo>();
            DtoClass jaxb_dto_class = JaxbUtils.find_jaxb_dto_class(dto_class_name, jaxb_dto_classes);
            String dao_jdbc_sql = db_utils.get_dao_crud_delete_info(table_name, jaxb_dto_class, explicit_pk, fields_pk);
            if (fields_pk.isEmpty()) {
                return Helpers.get_no_pk_warning(method_name);
            }
            String cpp_sql_str = SqlUtils.jdbc_sql_to_cpp_str(dao_jdbc_sql);
            Map<String, Object> context = new HashMap<String, Object>();
            context.put("mode", "dao_exec_dml");
            context.put("class_name", dao_class_name);
            context.put("method_name", method_name);
            context.put("sql", cpp_sql_str);
            context.put("method_type", "DELETE");
            context.put("table_name", table_name);
            context.put("comment", "Deletes specified record from the table '" + table_name + "'.");
            context.put("dto_param", "");
            context.put("params", fields_pk);
            context.put("external_sql", false);
            StringWriter sw = new StringWriter();
            te.merge(context, sw);
            StringBuilder buffer = new StringBuilder();
            buffer.append(sw.getBuffer());
            return buffer;
        }

        @Override
        public StringBuilder render_jaxb_crud(String dao_class_name, Crud jaxb_type_crud) throws Exception {
            String node_name = JaxbUtils.get_jaxb_node_name(jaxb_type_crud);
            String dto_class_name = jaxb_type_crud.getDto();
            if (dto_class_name.isEmpty()) {
                throw new Exception("<" + node_name + "...\nAttribute 'dto' is empty");
            }
            try {
                _process_dto_class_name(dto_class_name);
                DtoClass jaxb_dto_class = JaxbUtils.find_jaxb_dto_class(dto_class_name, jaxb_dto_classes);
                StringBuilder code_buff = JaxbUtils.process_jaxb_crud(this, db_utils.get_dto_field_names_mode(),
                        jaxb_type_crud, dao_class_name, jaxb_dto_class);
                return code_buff;
            } catch (Exception e) {
                // e.printStackTrace();
                String msg = "<" + node_name + " dto=\"" + dto_class_name + "\" table=\"" + jaxb_type_crud.getTable() + "\"...\n";
                throw new Exception(Helpers.get_error_message(msg, e));
            }
        }
    }
}