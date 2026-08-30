/*
    Copyright 2011-2026 sqldalmaker@gmail.com
    SQL DAL Maker Website: https://sqldalmaker.sourceforge.net/
    Read LICENSE.txt in the root of this project/archive for details.
 */
package com.sqldalmaker.cg.go;

import com.sqldalmaker.cg.*;
import com.sqldalmaker.common.Const;
import com.sqldalmaker.jaxb.sdm.*;
import com.sqldalmaker.jaxb.settings.Settings;

import java.io.StringWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.util.*;

/*
 * @author sqldalmaker@gmail.com
 *
 * 16.06.2024 02:56 1.301 [+] <dto-class...<custom...
 * 25.04.2024 05:15 1.297
 * 14.02.2024 18:50 1.294 <dao-class ref="...
 * 16.12.2023 09:01 1.292 sdm.xml
 * 08.10.2023 19:37 1.290
 * 20.09.2023 14:36 1.289
 * 11.05.2023 10:46 1.283
 * 09.04.2023 20:31 1.282
 * 27.03.2023 10:03 optional "<crud table"
 * 23.02.2023 14:11 1.279
 * 19.01.2023 20:57 1.276
 * 16.11.2022 08:02 1.269
 * 25.10.2022 09:26 crud pk --> dto-class pk
 * 25.10.2022 03:46 - crud generated; + dao-class auto
 * 06.08.2022 08:37 1.261 no 'crud-auto' anymore, just empty 'crud' instead
 * 09.07.2022 23:10 dto macro, dao macro
 * 12.05.2022 21:39 + gorm
 * 21.04.2022 17:15 1.225
 * 01.05.2021 22:33 JSON and XML comments for Go
 * 22.03.2021 21:19 TitleCase for method names
 *
 */
public class GoCG {

    private static String _get_package_name(String scope) throws Exception {
        String pkg;
        if (scope.trim().isEmpty()) {
            throw new Exception(Const.GOLANG_SCOPES_ERR);
        } else {
            Path p = Paths.get(scope);
            String dto_scope_last_segment = p.getFileName().toString();
            if (dto_scope_last_segment.equals(scope)) {
                pkg = scope;
            } else {
                pkg = dto_scope_last_segment;
            }
        }
        return pkg;
    }

    public static class DTO implements IDtoCG {

        private final String dto_package;

        private final String sql_root_abs_path;
        private final List<DtoClass> jaxb_dto_classes;
        private final TemplateEngine te;
        private final JdbcUtils db_utils;

        public DTO(
                Sdm sdm,
                Settings jaxb_settings,
                Connection connection,
                String sql_root_abs_path,
                FieldNamesMode field_names_mode,
                String vm_template) throws Exception {

            String dto_scope = jaxb_settings.getDto().getScope().replace('\\', '/').trim();
            this.dto_package = _get_package_name(dto_scope);
            this.jaxb_dto_classes = sdm.getDtoClass();
            this.sql_root_abs_path = sql_root_abs_path;
            te = Helpers.create_template_engine(vm_template, "go", "golang");
            db_utils = new JdbcUtils(connection, field_names_mode, FieldNamesMode.LOWER_CAMEL_CASE, jaxb_settings, sql_root_abs_path);
        }

        @Override
        public String[] translate(String dto_class_name) throws Exception {
            DtoClass jaxb_dto_class = JaxbUtils.find_jaxb_dto_class(dto_class_name, jaxb_dto_classes);
            List<FieldInfo> fields = db_utils.get_dto_fields(jaxb_dto_class, sql_root_abs_path);
            Map<String, Object> context = new HashMap<String, Object>();
            int model_name_end_index = dto_class_name.indexOf('-');
            if (model_name_end_index != -1) {
                String model = dto_class_name.substring(0, model_name_end_index);
                dto_class_name = dto_class_name.substring(model_name_end_index + 1);
                context.put("model", model);
            }
            context.put("package", dto_package);
            String header = jaxb_dto_class.getHeader();
            context.put("header", header);
            List<FormattedField> formatted_fields = GoStructFormatter.format_fields(jaxb_dto_class, fields); // !!! before imports
            Set<String> imports_set = new HashSet<String>();
            for (FieldInfo fi : fields) {
                String type_import = GoTypes.get_type_import(fi);
                if (type_import != null) {
                    imports_set.add(type_import);
                }
            }
            String[] imports_arr = imports_set.toArray(new String[0]);
            Arrays.sort(imports_arr);
            context.put("imports", imports_arr);
            context.put("class_name", dto_class_name);
            context.put("ref", jaxb_dto_class.getRef());
            context.put("fields", formatted_fields);
            context.put("mode", "dto_class");
            String ref = jaxb_dto_class.getRef();
            if (SqlUtils.is_table_ref(ref)) {
                context.put("table", ref);
            } else {
                context.put("table", "");
            }
            StringWriter sw = new StringWriter();
            te.merge(context, sw);
            String text = sw.toString();
            // seems like Go fmt makes \n
            text = text.replace("\r\n", "\n");
            return new String[]{text};
        }

    }

    public static class DAO implements IDaoCG {

        private final String sql_root_abs_path;
        private final List<DtoClass> jaxb_dto_classes;
        private final TemplateEngine te;
        private final JdbcUtils db_utils;

        private final String dto_package;
        private final String dao_package;
        private final Set<String> imports = new HashSet<String>();
        private final Settings settings;

        public DAO(
                List<DtoClass> jaxb_dto_classes,
                Settings jaxb_settings,
                Connection connection,
                String sql_root_abs_path,
                FieldNamesMode field_names_mode,
                String vm_template) throws Exception {

            this.jaxb_dto_classes = jaxb_dto_classes;
            this.sql_root_abs_path = sql_root_abs_path;
            this.te = Helpers.create_template_engine(vm_template, "go", "golang");
            this.db_utils = new JdbcUtils(connection, field_names_mode, FieldNamesMode.LOWER_CAMEL_CASE, jaxb_settings, sql_root_abs_path);
            this.settings = jaxb_settings;
            String dto_scope = jaxb_settings.getDto().getScope().replace('\\', '/').trim();
            this.dto_package = _get_package_name(dto_scope);
            String dao_scope = jaxb_settings.getDao().getScope().replace('\\', '/').trim();
            this.dao_package = _get_package_name(dao_scope);
        }

        @Override
        public String[] translate(DaoClass dao_class) throws Exception {
            imports.clear();
            String dao_class_name = dao_class.getName();
            List<String> methods = new ArrayList<String>();
            for (DaoMethodInfo mi : JaxbUtils.plan_dao_methods(
                    dao_class, jaxb_dto_classes, db_utils.get_dto_field_names_mode())) {
                try {
                    methods.add(_render(mi).toString());
                } catch (Throwable e) {
                    throw new Exception(Helpers.get_error_message(mi.get_error_context(), e));
                }
            }
            for (int i = 0; i < methods.size(); i++) {
                String m = methods.get(i).replace("    //", "//");
                methods.set(i, m);
            }
            Map<String, Object> context = new HashMap<String, Object>();
            context.put("package", dao_package);
            imports.add("context");
            String[] imports_arr = imports.toArray(new String[0]);
            Arrays.sort(imports_arr);
            context.put("imports", imports_arr);
            context.put("class_name", dao_class_name);
            context.put("methods", methods);
            context.put("mode", "dao_class");
            StringWriter sw = new StringWriter();
            te.merge(context, sw);
            String text = sw.toString();
            // seems that Go fmt makes \n
            text = text.replace("\r\n", "\n");
            return new String[]{text};
        }

        private StringBuilder _render(DaoMethodInfo mi) throws Exception {
            if (mi instanceof DaoQueryMethodInfo) {
                return _render_jaxb_query((DaoQueryMethodInfo) mi);
            }
            if (mi instanceof DaoExecDmlMethodInfo) {
                return _render_jaxb_exec_dml((DaoExecDmlMethodInfo) mi);
            }
            if (mi instanceof DaoCrudMethodInfo) {
                return _render_crud((DaoCrudMethodInfo) mi);
            }
            throw new Exception("Unexpected kind of DAO method: " + mi.getClass().getName());
        }

        private StringBuilder _render_crud(DaoCrudMethodInfo mi) throws Exception {
            _process_dto_class_name(mi.dto_class_name);
            switch (mi.kind) {
                case CREATE:
                    return _render_crud_create(mi.method_name, mi.table_name, mi.dto_class_name,
                            mi.fetch_generated, mi.auto_column);
                case READ:
                    return _render_crud_read(mi.method_name, mi.table_name, mi.dto_class_name,
                            mi.explicit_pk, mi.fetch_list);
                case UPDATE:
                    return _render_crud_update(mi.method_name, mi.table_name, mi.explicit_pk,
                            mi.dto_class_name, false);
                case DELETE:
                    return _render_crud_delete(mi.dto_class_name, mi.method_name, mi.table_name,
                            mi.explicit_pk);
            }
            throw new Exception("Unexpected kind of CRUD method: " + mi.kind);
        }

        private StringBuilder _render_jaxb_query(DaoQueryMethodInfo mi) throws Exception {
            if (mi.return_type_is_dto) {
                _process_dto_class_name(mi.dto_or_return_type);
            }
            JdbcUtils.DaoSqlInfo _q = db_utils.get_dao_query_info(sql_root_abs_path, mi.ref, "",
                    mi.param_descriptors, mi.dto_or_return_type, mi.return_type_is_dto, jaxb_dto_classes);
            return _render_query(_q.sql, mi.external_sql, mi.dto_or_return_type, mi.return_type_is_dto,
                    mi.fetch_list, mi.method_name, null, _q.fields, _q.params);
        }

        private StringBuilder _render_jaxb_exec_dml(DaoExecDmlMethodInfo mi) throws Exception {
            String dao_jdbc_sql = SqlUtils.jdbc_sql_by_exec_dml_ref(mi.ref, sql_root_abs_path);
            StringBuilder buff = new StringBuilder();
            _render_exec_dml(buff, dao_jdbc_sql, mi.external_sql, mi.method_name,
                    mi.param_descriptors, mi.xml_node_name, mi.ref);
            return buff;
        }

        // called from both '_render_jaxb_query' and '_render_crud_read'
        private StringBuilder _render_query(
                String dao_query_jdbc_sql,
                boolean is_external_sql,
                String dto_or_scalar_return_type,
                boolean return_type_is_dto,
                boolean fetch_list,
                String method_name,
                // String dto_param_type,
                String crud_table,
                List<FieldInfo> fields,
                List<FieldInfo> params) throws Exception {

            if (dao_query_jdbc_sql == null) {
                return Helpers.get_no_pk_warning(method_name);
            }
            String returned_type_name;
            if (return_type_is_dto) {
                returned_type_name = _get_rendered_dto_class_name(dto_or_scalar_return_type);
            } else {
                FieldInfo ret_fi = fields.get(0);
                String imp = GoTypes.get_type_import(ret_fi);
                if (imp != null) {
                    imports.add(imp);
                }
                returned_type_name = GoTypes.get_type_without_import_and_tag(ret_fi);
                ret_fi.refine_rendered_type(returned_type_name);
            }
            String go_sql_str = SqlUtils.format_jdbc_sql_for_go(dao_query_jdbc_sql);
            Map<String, Object> context = new HashMap<String, Object>();
            if (fetch_list) {
                if (return_type_is_dto) {
                    context.put("mode", "dao_query_all_dto");
                } else {
                    context.put("mode", "dao_query_all");
                }
            } else {
                if (return_type_is_dto) {
                    context.put("mode", "dao_query_dto");
                } else {
                    context.put("mode", "dao_query");
                }
            }
            if (return_type_is_dto) {
                _set_model(dto_or_scalar_return_type, context);
            } else {
                context.put("model", "");
            }
            int fam = settings.getDao().getFieldAssignMode();
            context.put("assign_mode", fam);
            context.put("fields", fields);
            method_name = _refine_method_name(method_name);
            context.put("method_name", method_name);
            if (crud_table == null) {
                crud_table = "";
            }
            if (crud_table.isEmpty()) {
                context.put("method_type", "");
            } else {
                context.put("method_type", "READ");
            }
            context.put("ref", crud_table);
            context.put("table_name", crud_table);
            context.put("sql", go_sql_str);
            context.put("is_external_sql", is_external_sql);
            // context.put("use_dto", return_type_is_dto);
            context.put("returned_type_name", returned_type_name);
            _assign_params_and_imports(params, "", context);
            StringWriter sw = new StringWriter();
            te.merge(context, sw);
            String text = sw.toString();
            // seems that Go fmt makes \n
            text = text.replace("\r\n", "\n");
            StringBuilder buff = new StringBuilder();
            buff.append(text);
            return buff;
        }

        private String _refine_method_name(String method_name) {
            return Names.get_method_name(method_name, db_utils.get_dto_field_names_mode());
        }

        private String _get_rendered_dto_class_name(String dto_class_name) throws Exception {
            DtoClass jaxb_dto_class = JaxbUtils.find_jaxb_dto_class(dto_class_name, jaxb_dto_classes);
            String dto_class_nm = jaxb_dto_class.getName();
            int model_end = dto_class_nm.indexOf('-');
            if (model_end != -1) {
                dto_class_nm = dto_class_nm.substring(model_end + 1);
            }
            if (this.dto_package.equals(dao_package)) {
                return dto_class_nm;
            }
            return dto_package + "." + dto_class_nm;
        }

        private void _process_dto_class_name(String dto_class_name) {
            if (this.dto_package.equals(dao_package)) {
                return;
            }
            String dto_scope = settings.getDto().getScope();
            String dto_import;
            String module = settings.getFolders().getTarget();
            if (module.trim().isEmpty()) {
                dto_import = dto_scope;
            } else {
                dto_import = Helpers.concat_path(module, dto_scope);
            }
            if (imports.contains(dto_import)) {
                return;
            }
            imports.add(dto_import);
        }

        private void _render_exec_dml(
                StringBuilder buffer,
                String jdbc_dao_sql,
                boolean is_external_sql,
                String method_name,
                String[] param_descriptors,
                String xml_node_name,
                String sql_path) throws Exception {

            List<FieldInfo> _params = db_utils.get_dao_exec_dml_params(jdbc_dao_sql, "", param_descriptors);
            for (FieldInfo pi : _params) {
                String imp = GoTypes.get_type_import(pi);
                if (imp != null) {
                    imports.add(imp);
                }
                String just_type = GoTypes.get_type_without_import_and_tag(pi);
                pi.refine_rendered_type(just_type);
            }
            String go_sql = SqlUtils.format_jdbc_sql_for_go(jdbc_dao_sql);
            List<MappingInfo> m_list = new ArrayList<MappingInfo>();
            List<FieldInfo> method_params = new ArrayList<FieldInfo>();
            List<FieldInfo> exec_dml_params = new ArrayList<FieldInfo>();
            for (ExecDmlParamSlot slot : ExecDmlParams.parse(param_descriptors)) {
                if (slot.kind == ExecDmlParamSlot.Kind.CURSOR_ARRAY) {
                    List<String> cb_elements = new ArrayList<String>();
                    for (String[] mapping : slot.cursor_mappings) {
                        MappingInfo m = _create_mapping(mapping);
                        m_list.add(m);
                        String func_type = String.format("func(*%s)", m.dto_class_name);
                        method_params.add(FieldInfo.parameter(FieldNamesMode.LOWER_CAMEL_CASE, func_type, m.method_param_name));
                        cb_elements.add(m.exec_dml_param_name);
                    }
                    // no "(Object) " cast in here: unlike Java varargs, a Go variadic
                    // parameter needs no help to accept a slice as one argument
                    String exec_xml_param = "[]interface{}{" + String.join(", ", cb_elements) + "}";
                    exec_dml_params.add(FieldInfo.parameter(FieldNamesMode.AS_IS, "[]", exec_xml_param));
                } else if (slot.kind == ExecDmlParamSlot.Kind.MAPPED) {
                    FieldInfo p = _params.get(slot.index);
                    MappingInfo m = _create_mapping(slot.mapping);
                    m_list.add(m);
                    String func_type = String.format("func(*%s)", m.dto_class_name);
                    method_params.add(FieldInfo.parameter(FieldNamesMode.LOWER_CAMEL_CASE, func_type, m.method_param_name));
                    String target_type_name = this.db_utils.get_target_type_by_type_map(p.getType());
                    exec_dml_params.add(FieldInfo.parameter(FieldNamesMode.AS_IS, target_type_name, m.exec_dml_param_name));
                } else {
                    FieldInfo p = _params.get(slot.index);
                    method_params.add(p);
                    exec_dml_params.add(p);
                }
            }
            Map<String, Object> context = new HashMap<String, Object>();
            context.put("params2", exec_dml_params);
            context.put("mappings", m_list);
            method_name = _refine_method_name(method_name);
            context.put("method_name", method_name);
            context.put("sql", go_sql);
            context.put("xml_node_name", xml_node_name);
            context.put("sql_path", sql_path);
            context.put("is_external_sql", is_external_sql);
            context.put("mode", "dao_exec_dml");
            context.put("model", "");
            int fam = settings.getDao().getFieldAssignMode();
            context.put("assign_mode", fam);
            _assign_params_and_imports(method_params, "", context);
            StringWriter sw = new StringWriter();
            te.merge(context, sw);
            buffer.append(sw.getBuffer());
        }

        private MappingInfo _create_mapping(String[] parts) throws Exception {
            MappingInfo m = new MappingInfo();
            m.method_param_name = Names.lower_camel_case(parts[0].trim());
            String cb_param_name = String.format("%sMapper", m.method_param_name);
            m.exec_dml_param_name = cb_param_name;
            String declared_dto_class_name = parts[1].trim();
            DtoClass jaxb_dto_class = JaxbUtils.find_jaxb_dto_class(declared_dto_class_name, jaxb_dto_classes);
            _process_dto_class_name(jaxb_dto_class.getName()); // extends imports
            List<FieldInfo> fields = db_utils.get_dto_fields(jaxb_dto_class, sql_root_abs_path);
            m.dto_class_name = _get_rendered_dto_class_name(declared_dto_class_name);
            m.fields.addAll(fields);
            return m;
        }

        private void _assign_params_and_imports(List<FieldInfo> params, String dto_param_type, Map<String, Object> context) throws Exception {
            int params_count = params.size();
            boolean plain_params;
            if (dto_param_type != null && !dto_param_type.isEmpty()) {
                if (params_count == 0) {
                    throw new Exception("DTO parameter specified but SQL-query does not contain any parameters");
                }
                _process_dto_class_name(dto_param_type);
                String rendered_dto_class_name = _get_rendered_dto_class_name(dto_param_type);
                context.put("dto_param", rendered_dto_class_name);
                plain_params = false;
            } else {
                context.put("dto_param", "");
                plain_params = true;
            }
            context.put("plain_params", plain_params);
            if (plain_params) {
                for (FieldInfo pi : params) {
                    String imp = GoTypes.get_type_import(pi);
                    if (imp != null) {
                        imports.add(imp);
                    }
                    String just_type = GoTypes.get_type_without_import_and_tag(pi);
                    pi.refine_rendered_type(just_type);
                }
            }
            context.put("params", params);
            if (context.get("imports") != null) {
                throw new Exception("Invalid assignment of 'imports'");
            }
            context.put("imports", imports);
        }

        private static void _set_model(String dto_class_name, Map<String, Object> context) {
            String model = "";
            int model_name_end_index = dto_class_name.indexOf('-');
            if (model_name_end_index != -1) {
                model = dto_class_name.substring(0, model_name_end_index);
            }
            context.put("model", model);
        }

        private StringBuilder _render_crud_create(
                String method_name,
                String table_name,
                String dto_class_name,
                boolean fetch_generated,
                String generated) throws Exception {

            DtoClass jaxb_dto_class = JaxbUtils.find_jaxb_dto_class(dto_class_name, jaxb_dto_classes);
            JdbcUtils.DaoSqlInfo _c = db_utils.get_dao_crud_create_info(table_name, jaxb_dto_class, generated);
            String dao_jdbc_sql = _c.sql;
            List<FieldInfo> fields_not_ai = _c.fields;
            List<FieldInfo> fields_ai = _c.params;
            String go_sql_str = SqlUtils.format_jdbc_sql_for_go(dao_jdbc_sql);
            Map<String, Object> context = new HashMap<String, Object>();
            context.put("method_type", "CREATE");
            context.put("crud", true);
            context.put("table_name", table_name);
            _set_model(dto_class_name, context);
            context.put("sql", go_sql_str);
            context.put("method_name", method_name);
            context.put("params", fields_not_ai);
            context.put("dto_param", _get_rendered_dto_class_name(dto_class_name));
            if (fetch_generated && !fields_ai.isEmpty()) {
                List<String> ai_names = new ArrayList<String>();
                for (FieldInfo ai : fields_ai) {
                    ai_names.add(ai.getColumnName());
                }
                String ai_names_str = String.join(",", ai_names);
                context.put("ai_names", ai_names_str);
                context.put("fields_ai", fields_ai);
                context.put("mode", "dao_create");
            } else {
                // context.put("plain_params", true);
                context.put("is_external_sql", false);
                context.put("mode", "dao_exec_dml");
            }
            context.put("plain_params", false); // anyway
            StringWriter sw = new StringWriter();
            te.merge(context, sw);
            StringBuilder buffer = new StringBuilder();
            buffer.append(sw.getBuffer());
            return buffer;
        }

        private StringBuilder _render_crud_read(
                String method_name,
                String dao_table_name,
                String dto_class_name,
                String explicit_pk,
                boolean fetch_list) throws Exception {

            DtoClass jaxb_dto_class = JaxbUtils.find_jaxb_dto_class(dto_class_name, jaxb_dto_classes);
            JdbcUtils.DaoSqlInfo _c = db_utils.get_dao_crud_read_info(dao_table_name, jaxb_dto_class, fetch_list, explicit_pk);
            String dao_jdbc_sql = _c.sql;
            List<FieldInfo> fields_all = _c.fields;
            List<FieldInfo> fields_pk = _c.params;
            return _render_query(dao_jdbc_sql, false, dto_class_name, true, fetch_list, method_name, dao_table_name,
                    fields_all, fields_pk);
        }

        private StringBuilder _render_crud_update(
                String method_name,
                String table_name,
                String explicit_pk,
                String dto_class_name,
                boolean scalar_params) throws Exception {

            DtoClass jaxb_dto_class = JaxbUtils.find_jaxb_dto_class(dto_class_name, jaxb_dto_classes);
            JdbcUtils.DaoSqlInfo _c = db_utils.get_dao_crud_update_info(table_name, jaxb_dto_class, explicit_pk);
            String dao_jdbc_sql = _c.sql;
            List<FieldInfo> fields_not_pk = _c.fields;
            List<FieldInfo> fields_pk = _c.params;
            if (fields_pk.isEmpty()) {
                return Helpers.get_no_pk_warning(method_name);
            }
            if (fields_not_pk.isEmpty()) {
                return Helpers.get_only_pk_warning(method_name);
            }
            String go_sql_str = SqlUtils.format_jdbc_sql_for_go(dao_jdbc_sql);
            Map<String, Object> context = new HashMap<String, Object>();
            context.put("mode", "dao_exec_dml");
            context.put("method_type", "UPDATE");
            _set_model(dto_class_name, context);
            context.put("table_name", table_name);
            context.put("method_name", method_name);
            context.put("is_external_sql", false);
            context.put("sql", go_sql_str);
            List<FieldInfo> params = new ArrayList<FieldInfo>();
            params.addAll(fields_not_pk);
            params.addAll(fields_pk);
            String dto_param = scalar_params ? "" : dto_class_name;
            _assign_params_and_imports(params, dto_param, context);
            StringWriter sw = new StringWriter();
            te.merge(context, sw);
            StringBuilder buffer = new StringBuilder();
            buffer.append(sw.getBuffer());
            return buffer;
        }

        private StringBuilder _render_crud_delete(
                String dto_class_name,
                String method_name,
                String table_name,
                String explicit_pk) throws Exception {

            DtoClass jaxb_dto_class = JaxbUtils.find_jaxb_dto_class(dto_class_name, jaxb_dto_classes);
            JdbcUtils.DaoSqlInfo _c = db_utils.get_dao_crud_delete_info(table_name, jaxb_dto_class, explicit_pk);
            String dao_jdbc_sql = _c.sql;
            List<FieldInfo> fields_pk = _c.params;
            if (fields_pk.isEmpty()) {
                return Helpers.get_no_pk_warning(method_name);
            }
            String go_sql_str = SqlUtils.format_jdbc_sql_for_go(dao_jdbc_sql);
            Map<String, Object> context = new HashMap<String, Object>();
            context.put("mode", "dao_exec_dml");
            context.put("table_name", table_name);
            context.put("method_type", "DELETE");
            context.put("method_name", method_name);
            context.put("sql", go_sql_str);
            context.put("is_external_sql", false);
            _assign_params_and_imports(fields_pk, dto_class_name, context);
            _set_model(dto_class_name, context);
            StringWriter sw = new StringWriter();
            te.merge(context, sw);
            StringBuilder buffer = new StringBuilder();
            buffer.append(sw.getBuffer());
            return buffer;
        }
    }
}
