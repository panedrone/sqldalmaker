/*
    Copyright 2011-2023 sqldalmaker@gmail.com
    SQL DAL Maker Website: https://sqldalmaker.sourceforge.net/
    Read LICENSE.txt in the root of this project/archive for details.
 */
package com.sqldalmaker.cg.go;

import com.sqldalmaker.cg.*;
import com.sqldalmaker.common.Const;
import com.sqldalmaker.jaxb.dao.Crud;
import com.sqldalmaker.jaxb.dao.DaoClass;
import com.sqldalmaker.jaxb.dao.ExecDml;
import com.sqldalmaker.jaxb.dto.DtoClass;
import com.sqldalmaker.jaxb.dto.DtoClasses;
import com.sqldalmaker.jaxb.settings.Settings;

import java.io.StringWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.util.*;

/**
 * @author sqldalmaker@gmail.com
 */
public class GoCG {

    private static String get_template_path() {
        return Helpers.class.getPackage().getName().replace('.', '/') + "/go/go.vm";
    }

    private static String _get_type_import(FieldInfo fi) {
        String initial_type = fi.getType();
        if (initial_type == null) {
            return null;
        }
        String[] type_parts = initial_type.trim().split("\\s+");
        if (type_parts.length < 1) {
            return null;
        }
        String type_part = type_parts[0]; // type_part = "time:time.Time"
        int import_end = type_part.indexOf(':');
        if (import_end == -1) {
            return null;
        }
        String res = type_part.substring(0, import_end);
        return res;
    }

    private static String _get_type_without_import(String type) {
        // "time:time.Time `json:"t_date"`{$0}"
        String initial_type = type.split("[$]")[0]; // it is needed for parameters
        int import_end = initial_type.split("\\s+")[0].indexOf(":");
        if (import_end == -1) {
            return initial_type;
        }
        String res = initial_type.substring(import_end + 1);
        return res;
    }

    private static String _get_type_without_import_and_tag(FieldInfo fi) {
        String initial_type = _get_type_without_import(fi.getType()); // "time:time.Time `json:"t_date"`"
        String[] type_parts = initial_type.split("\\s+"); // it returns the whole string if there are no "\\s+"
        String type_part = type_parts[0]; // type_part = "time:time.Time"
        return type_part;
    }

    private static String _get_package_name(String scope) throws Exception {
        String pkg;
        if (scope.trim().length() == 0) {
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
        private final DtoClasses jaxb_dto_classes;
        private final TemplateEngine te;
        private final JdbcUtils db_utils;

        public DTO(DtoClasses jaxb_dto_classes,
                   Settings jaxb_settings,
                   Connection connection,
                   String sql_root_abs_path,
                   FieldNamesMode field_names_mode,
                   String vm_template) throws Exception {

            String dto_scope = jaxb_settings.getDto().getScope().replace('\\', '/').trim();
            this.dto_package = _get_package_name(dto_scope);
            this.jaxb_dto_classes = jaxb_dto_classes;
            this.sql_root_abs_path = sql_root_abs_path;
            if (vm_template == null) {
                te = new TemplateEngine(get_template_path(), false);
            } else {
                te = new TemplateEngine(vm_template, "golang");
            }
            db_utils = new JdbcUtils(connection, field_names_mode, FieldNamesMode.LOWER_CAMEL_CASE, jaxb_settings, sql_root_abs_path);
        }

        @Override
        public String[] translate(String dto_class_name) throws Exception {
            DtoClass jaxb_dto_class = JaxbUtils.find_jaxb_dto_class(dto_class_name, jaxb_dto_classes);
            List<FieldInfo> fields = new ArrayList<FieldInfo>();
            db_utils.get_dto_field_info(jaxb_dto_class, sql_root_abs_path, fields);
            Map<String, Object> context = new HashMap<String, Object>();
            int model_name_end_index = dto_class_name.indexOf('-');
            if (model_name_end_index != -1) {
                String model = dto_class_name.substring(0, model_name_end_index);
                dto_class_name = dto_class_name.substring(model_name_end_index + 1);
                context.put("model", model);
            }
            context.put("package", dto_package);
            Set<String> imports_set = new HashSet<String>();
            int max_name_len = -1;
            int max_type_name_len = -1;
            for (FieldInfo fi : fields) {
                String imp = _get_type_import(fi);
                if (imp != null) {
                    imports_set.add(imp);
                }
                int name_len = fi.getName().length();
                if (name_len > max_name_len) {
                    max_name_len = name_len;
                }
                String just_type = _get_type_without_import_and_tag(fi);
                if (just_type.length() > max_type_name_len) {
                    max_type_name_len = just_type.length();
                }
            }
            String name_format = "%-" + max_name_len + "." + max_name_len + "s";
            for (FieldInfo fi : fields) {
                String name = fi.getName();
                name = String.format(name_format, name);
                fi.setName(name);
                String type_name = _get_type_without_import(fi.getType());
                if (max_type_name_len > 0) {
                    String just_type = _get_type_without_import_and_tag(fi);
                    int just_type_len = just_type.length();
                    if (just_type_len < type_name.length()) {
                        String type_tag = type_name.substring(just_type_len + 1);
                        String type_format = "%-" + max_type_name_len + "." + max_type_name_len + "s %s";
                        type_name = String.format(type_format, just_type, type_tag);
                    }
                }
                fi.refine_rendered_type(type_name);
            }
            String header = jaxb_dto_class.getHeader();
            context.put("header", header);
            String[] imports_arr = imports_set.toArray(new String[0]);
            Arrays.sort(imports_arr);
            context.put("imports", imports_arr);
            context.put("class_name", dto_class_name);
            context.put("ref", jaxb_dto_class.getRef());
            context.put("fields", fields);
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

        private final String dto_package;
        private final String dao_package;

        private final String sql_root_abs_path;
        private final DtoClasses jaxb_dto_classes;
        private final Set<String> imports_set = new HashSet<String>();
        private final TemplateEngine te;
        private final JdbcUtils db_utils;
        private final Settings settings;

        private String dao_class_name;

        public DAO(DtoClasses jaxb_dto_classes,
                   Settings jaxb_settings,
                   Connection connection,
                   String sql_root_abs_path,
                   FieldNamesMode field_names_mode,
                   String vm_template) throws Exception {

            this.settings = jaxb_settings;
            String dto_scope = jaxb_settings.getDto().getScope().replace('\\', '/').trim();
            this.dto_package = _get_package_name(dto_scope);
            String dao_scope = jaxb_settings.getDao().getScope().replace('\\', '/').trim();
            this.dao_package = _get_package_name(dao_scope);
            this.jaxb_dto_classes = jaxb_dto_classes;
            this.sql_root_abs_path = sql_root_abs_path;
            if (vm_template == null) {
                te = new TemplateEngine(get_template_path(), false);
            } else {
                te = new TemplateEngine(vm_template, "golang");
            }
            db_utils = new JdbcUtils(connection, field_names_mode, FieldNamesMode.LOWER_CAMEL_CASE, jaxb_settings, sql_root_abs_path);
        }

        @Override
        public String[] translate(String dao_class_name,
                                  DaoClass dao_class) throws Exception {
            imports_set.clear();
            this.dao_class_name = dao_class_name;
            List<String> methods = new ArrayList<String>();
            JaxbUtils.process_jaxb_dao_class(this, dao_class_name, dao_class, methods);
            for (int i = 0; i < methods.size(); i++) {
                String m = methods.get(i).replace("    //", "//");
                methods.set(i, m);
            }
            Map<String, Object> context = new HashMap<String, Object>();
            context.put("package", dao_package);
            String[] imports_arr = imports_set.toArray(new String[0]);
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

        @Override
        public StringBuilder render_jaxb_query(Object jaxb_element) throws Exception {
            QueryMethodInfo mi = new QueryMethodInfo(jaxb_element);
            String xml_node_name = JaxbUtils.get_jaxb_node_name(jaxb_element);
            Helpers.check_required_attr(xml_node_name, mi.jaxb_method);
            try {
                if (mi.return_type_is_dto) {
                    _process_dto_class_name(mi.jaxb_dto_or_return_type);
                }
                String[] parsed = _parse_method_declaration(mi.jaxb_method, dao_package);
                String method_name = parsed[0];
                String dto_param_type = parsed[1];
                String param_descriptors = parsed[2];
                String[] method_param_descriptors = Helpers.get_listed_items(param_descriptors, false);
                List<FieldInfo> fields = new ArrayList<FieldInfo>();
                List<FieldInfo> params = new ArrayList<FieldInfo>();
                String dao_query_jdbc_sql = db_utils.get_dao_query_info(sql_root_abs_path, mi.jaxb_ref, dto_param_type,
                        method_param_descriptors, mi.jaxb_dto_or_return_type, mi.return_type_is_dto, jaxb_dto_classes,
                        fields, params);
                return _render_query(dao_query_jdbc_sql, mi.jaxb_is_external_sql, mi.jaxb_dto_or_return_type,
                        mi.return_type_is_dto, mi.fetch_list, method_name, dto_param_type, null, fields, params);
            } catch (Throwable e) {
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
        private StringBuilder _render_query(String dao_query_jdbc_sql,
                                            boolean is_external_sql,
                                            String dto_or_scalar_return_type,
                                            boolean return_type_is_dto,
                                            boolean fetch_list,
                                            String method_name,
                                            String dto_param_type,
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
                returned_type_name = _get_type_without_import_and_tag(ret_fi);
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
                context.put("mode", "dao_query_one");
            }
            context.put("class_name", dao_class_name);
            if (return_type_is_dto) {
                _set_model(dto_or_scalar_return_type, context);
            } else {
                context.put("model", "");
            }
            int fam = settings.getDao().getFieldAssignMode();
            context.put("assign_mode", fam);
            context.put("fields", fields);
            method_name = Helpers.get_method_name(method_name, db_utils.get_dto_field_names_mode());
            context.put("method_name", method_name);
            if (crud_table == null) {
                crud_table = "";
            }
            if (crud_table.length() == 0) {
                context.put("method_type", "");
            } else {
                context.put("method_type", "READ");
            }
            context.put("ref", crud_table);
            context.put("table_name", crud_table);
            context.put("sql", go_sql_str);
            context.put("is_external_sql", is_external_sql);
            context.put("use_dto", return_type_is_dto);
            context.put("returned_type_name", returned_type_name);
            _assign_params_and_imports(params, dto_param_type, context);
            StringWriter sw = new StringWriter();
            te.merge(context, sw);
            String text = sw.toString();
            // seems that Go fmt makes \n
            text = text.replace("\r\n", "\n");
            StringBuilder buff = new StringBuilder();
            buff.append(text);
            return buff;
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
            if (module.trim().length() == 0) {
                dto_import = dto_scope;
            } else {
                dto_import = Helpers.concat_path(module, dto_scope);
            }
            if (imports_set.contains(dto_import)) {
                return;
            }
            imports_set.add(dto_import);
        }

        @Override
        public StringBuilder render_jaxb_exec_dml(ExecDml jaxb_exec_dml) throws Exception {
            String method = jaxb_exec_dml.getMethod();
            String ref = jaxb_exec_dml.getRef();
            String xml_node_name = JaxbUtils.get_jaxb_node_name(jaxb_exec_dml);
            Helpers.check_required_attr(xml_node_name, method);
            try {
                String dao_jdbc_sql = SqlUtils.jdbc_sql_by_exec_dml_ref(ref, sql_root_abs_path);
                String[] parsed = _parse_method_declaration(method, dao_package);
                String method_name = parsed[0]; // never is null
                String dto_param_type = parsed[1]; // never is null
                String param_descriptors = parsed[2]; // never is null
                String[] method_param_descriptors = Helpers.get_listed_items(param_descriptors, true);
                boolean is_external_sql = jaxb_exec_dml.isExternalSql();
                StringBuilder buff = new StringBuilder();
                _render_exec_dml(buff, dao_jdbc_sql, is_external_sql, method_name, dto_param_type,
                        method_param_descriptors, xml_node_name, ref);
                return buff;

            } catch (Throwable e) {
                // e.printStackTrace();
                String msg = "<" + xml_node_name + " method=\"" + method + "\" ref=\"" + ref + "\"...\n";
                throw new Exception(Helpers.get_error_message(msg, e));
            }
        }

        private void _render_exec_dml(StringBuilder buffer,
                                      String jdbc_dao_sql,
                                      boolean is_external_sql,
                                      String method_name,
                                      String dto_param_type,
                                      String[] param_descriptors,
                                      String xml_node_name,
                                      String sql_path) throws Exception {

            SqlUtils.throw_if_select_sql(jdbc_dao_sql);
            List<FieldInfo> _params = new ArrayList<FieldInfo>();
            db_utils.get_dao_exec_dml_info(jdbc_dao_sql, dto_param_type, param_descriptors, _params);
            for (FieldInfo pi : _params) {
                String imp = _get_type_import(pi);
                if (imp != null) {
                    imports_set.add(imp);
                }
                String just_type = _get_type_without_import_and_tag(pi);
                pi.refine_rendered_type(just_type);
            }
            String go_sql = SqlUtils.format_jdbc_sql_for_go(jdbc_dao_sql);
            List<MappingInfo> m_list = new ArrayList<MappingInfo>();
            List<FieldInfo> method_params = new ArrayList<FieldInfo>();
            List<FieldInfo> exec_dml_params = new ArrayList<FieldInfo>();
            for (int pd_i = 0; pd_i < param_descriptors.length; pd_i++) {
                String pd = param_descriptors[pd_i].trim();
                if (pd.startsWith("[") && pd.endsWith("]")) {
                    String inner_list = pd.substring(1, pd.length() - 1);
                    String[] implicit_param_descriptors = Helpers.get_listed_items(inner_list, true);
                    List<String> cb_elements = new ArrayList<String>();
                    for (int ipd_i = 0; ipd_i < implicit_param_descriptors.length; ipd_i++) {
                        String ipd = implicit_param_descriptors[ipd_i];
                        String[] parts = _parse_param_descriptor(ipd);
                        if (parts == null) {
                            throw new Exception("Implicit cursors are specified incorrectly."
                                    + " Expected syntax: [on_dto_1:Dto1, on_dto_2:Dto2, ...]. Specified: " + "["
                                    + String.join(",", implicit_param_descriptors) + "]");
                        }
                        MappingInfo m = _create_mapping(parts);
                        m_list.add(m);
                        String func_type = String.format("func(*%s)", m.dto_class_name);
                        method_params.add(new FieldInfo(FieldNamesMode.LOWER_CAMEL_CASE, func_type, m.method_param_name, "parameter"));
                        cb_elements.add(m.exec_dml_param_name);
                    }
                    String exec_xml_param = "[]interface{}{" + String.join(", ", cb_elements) + "}";
                    if (pd_i == 0) {
                        exec_xml_param = "(Object) " + exec_xml_param;
                    }
                    exec_dml_params.add(new FieldInfo(FieldNamesMode.AS_IS, "[]", exec_xml_param, "parameter"));
                } else {
                    FieldInfo p = _params.get(pd_i);
                    String param_descriptor = param_descriptors[pd_i];
                    String[] parts = _parse_param_descriptor(param_descriptor);
                    if (parts != null) {
                        MappingInfo m = _create_mapping(parts);
                        m_list.add(m);
                        String func_type = String.format("func(*%s)", m.dto_class_name);
                        method_params.add(new FieldInfo(FieldNamesMode.LOWER_CAMEL_CASE, func_type, m.method_param_name, "parameter"));
                        String target_type_name = this.db_utils.get_target_type_by_type_map(p.getType());
                        exec_dml_params.add(new FieldInfo(FieldNamesMode.AS_IS, target_type_name, m.exec_dml_param_name, "parameter"));
                    } else {
                        method_params.add(p);
                        exec_dml_params.add(p);
                    }
                }
            }
            Map<String, Object> context = new HashMap<String, Object>();
            context.put("params2", exec_dml_params);
            context.put("mappings", m_list);
            boolean plain_params = dto_param_type.length() == 0;
            context.put("plain_params", plain_params);
            context.put("class_name", dao_class_name);
            method_name = Helpers.get_method_name(method_name, db_utils.get_dto_field_names_mode());
            context.put("method_name", method_name);
            context.put("sql", go_sql);
            context.put("xml_node_name", xml_node_name);
            context.put("sql_path", sql_path);
            context.put("is_external_sql", is_external_sql);
            context.put("mode", "dao_exec_dml");
            context.put("model", "");
            int fam = settings.getDao().getFieldAssignMode();
            context.put("assign_mode", fam);
            _assign_params_and_imports(method_params, dto_param_type, context);
            StringWriter sw = new StringWriter();
            te.merge(context, sw);
            buffer.append(sw.getBuffer());
        }

        private static String[] _parse_param_descriptor(String param_descriptor) {

            String[] parts = null;
            if (param_descriptor.contains("~")) {
                parts = param_descriptor.split("~");
            }
            if (param_descriptor.contains(":")) {
                parts = param_descriptor.split(":");
            }
            return parts;
        }

        private MappingInfo _create_mapping(String[] parts) throws Exception {

            MappingInfo m = new MappingInfo();
            m.method_param_name = Helpers.to_lower_camel_or_title_case(parts[0].trim(), false);
            String cb_param_name = String.format("%sMapper", m.method_param_name);
            m.exec_dml_param_name = cb_param_name;
            String declared_dto_class_name = parts[1].trim();
            List<FieldInfo> fields = new ArrayList<FieldInfo>();
            DtoClass jaxb_dto_class = JaxbUtils.find_jaxb_dto_class(declared_dto_class_name, jaxb_dto_classes);
            _process_dto_class_name(jaxb_dto_class.getName()); // extends imports
            db_utils.get_dto_field_info(jaxb_dto_class, sql_root_abs_path, fields);
            m.dto_class_name = _get_rendered_dto_class_name(declared_dto_class_name);
            m.fields.addAll(fields);
            return m;
        }

        private void _assign_params_and_imports(List<FieldInfo> params,
                                                String dto_param_type,
                                                Map<String, Object> context) throws Exception {
            int params_count = params.size();
            boolean plain_params;
            if (dto_param_type != null && dto_param_type.length() > 0) {
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
                    String imp = _get_type_import(pi);
                    if (imp != null) {
                        imports_set.add(imp);
                    }
                    String just_type = _get_type_without_import_and_tag(pi);
                    pi.refine_rendered_type(just_type);
                }
            }
            context.put("params", params);
            if (context.get("imports") != null) {
                throw new Exception("Invalid assignment of 'imports'");
            }
            context.put("imports", imports_set);
        }

        private String[] _parse_method_declaration(String method_text,
                                                   String dto_package) throws Exception {
            String dto_param_type = "";
            String param_descriptors = "";
            String method_name;
            String[] parts = Helpers.parse_method_params(method_text);
            method_name = parts[0];
            if (!("".equals(parts[1]))) {
                parts = Helpers.parse_method_params(parts[1]);
                if (!("".equals(parts[1]))) {
                    dto_param_type = parts[0];
                    param_descriptors = parts[1];
                    if (dto_param_type.length() > 0) {
                        _process_dto_class_name(dto_param_type);
                    }
                } else {
                    param_descriptors = parts[0];
                }
            }
            return new String[]{method_name, dto_param_type, param_descriptors};
        }

        private static void _set_model(String dto_class_name,
                                       Map<String, Object> context) {
            String model = "";
            int model_name_end_index = dto_class_name.indexOf('-');
            if (model_name_end_index != -1) {
                model = dto_class_name.substring(0, model_name_end_index);
            }
            context.put("model", model);
        }

        @Override
        public StringBuilder render_crud_create(String class_name,
                                                String method_name,
                                                String table_name,
                                                String dto_class_name,
                                                boolean fetch_generated,
                                                String generated) throws Exception {

            List<FieldInfo> fields_not_ai = new ArrayList<FieldInfo>();
            List<FieldInfo> fields_ai = new ArrayList<FieldInfo>();
            DtoClass jaxb_dto_class = JaxbUtils.find_jaxb_dto_class(dto_class_name, jaxb_dto_classes);
            String dao_jdbc_sql = db_utils.get_dao_crud_create_info(table_name, jaxb_dto_class, generated, fields_not_ai, fields_ai);
            String go_sql_str = SqlUtils.format_jdbc_sql_for_go(dao_jdbc_sql);
            Map<String, Object> context = new HashMap<String, Object>();
            context.put("method_type", "CREATE");
            context.put("crud", true);
            context.put("table_name", table_name);
            context.put("class_name", dao_class_name);
            _set_model(dto_class_name, context);
            context.put("sql", go_sql_str);
            context.put("method_name", method_name);
            context.put("params", fields_not_ai);
            context.put("dto_param", _get_rendered_dto_class_name(dto_class_name));
            if (fetch_generated && fields_ai.size() > 0) {
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

        @Override
        public StringBuilder render_crud_read(String method_name,
                                              String dao_table_name,
                                              String dto_class_name,
                                              String explicit_pk,
                                              boolean fetch_list) throws Exception {

            List<FieldInfo> fields_all = new ArrayList<FieldInfo>();
            List<FieldInfo> fields_pk = new ArrayList<FieldInfo>();
            DtoClass jaxb_dto_class = JaxbUtils.find_jaxb_dto_class(dto_class_name, jaxb_dto_classes);
            String dao_jdbc_sql = db_utils.get_dao_crud_read_info(dao_table_name, jaxb_dto_class, fetch_list, explicit_pk, fields_all, fields_pk);
            return _render_query(dao_jdbc_sql, false, dto_class_name, true, fetch_list, method_name, "", dao_table_name,
                    fields_all, fields_pk);
        }

        @Override
        public StringBuilder render_crud_update(String dao_class_name,
                                                String method_name,
                                                String table_name,
                                                String explicit_pk,
                                                String dto_class_name,
                                                boolean scalar_params) throws Exception {

            List<FieldInfo> fields_not_pk = new ArrayList<FieldInfo>();
            List<FieldInfo> fields_pk = new ArrayList<FieldInfo>();
            DtoClass jaxb_dto_class = JaxbUtils.find_jaxb_dto_class(dto_class_name, jaxb_dto_classes);
            String dao_jdbc_sql = db_utils.get_dao_crud_update_info(table_name, jaxb_dto_class, explicit_pk, fields_not_pk, fields_pk);
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
            context.put("class_name", this.dao_class_name);
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

        @Override
        public StringBuilder render_crud_delete(String dao_class_name,
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
            String go_sql_str = SqlUtils.format_jdbc_sql_for_go(dao_jdbc_sql);
            Map<String, Object> context = new HashMap<String, Object>();
            context.put("mode", "dao_exec_dml");
            context.put("class_name", dao_class_name);
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

        @Override
        public StringBuilder render_jaxb_crud(String dao_class_name,
                                              Crud jaxb_type_crud) throws Exception {

            String node_name = JaxbUtils.get_jaxb_node_name(jaxb_type_crud);
            String dto_class_name = jaxb_type_crud.getDto();
            if (dto_class_name == null || dto_class_name.length() == 0) {
                throw new Exception("<" + node_name + "...\nDTO class is not set");
            }
            String table_attr = jaxb_type_crud.getTable();
            if (table_attr == null || table_attr.length() == 0) {
                throw new Exception("<" + node_name + "...\nRequired attribute is not set");
            }
            try {
                db_utils.validate_table_name(table_attr);
                _process_dto_class_name(dto_class_name);
                DtoClass jaxb_dto_class = JaxbUtils.find_jaxb_dto_class(dto_class_name, jaxb_dto_classes);
                String explicit_auto_column = jaxb_dto_class.getAuto();
                String explicit_primary_keys = jaxb_dto_class.getPk();
                StringBuilder code_buff = JaxbUtils.process_jaxb_crud(this, db_utils.get_dto_field_names_mode(),
                        jaxb_type_crud, dao_class_name, dto_class_name, explicit_primary_keys, explicit_auto_column);
                return code_buff;
            } catch (Throwable e) {
                // e.printStackTrace();
                String msg = "<" + node_name + " dto=\"" + dto_class_name + "\" table=\"" + table_attr + "\"...\n";
                throw new Exception(Helpers.get_error_message(msg, e));
            }
        }
    }
}
