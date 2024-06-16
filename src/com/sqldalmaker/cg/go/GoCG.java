/*
    Copyright 2011-2024 sqldalmaker@gmail.com
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
            String header = jaxb_dto_class.getHeader();
            context.put("header", header);
            List<FormattedField> formatted_fields = _process_fields(jaxb_dto_class, fields); // !!! before imports
            Set<String> imports_set = new HashSet<String>();
            for (FieldInfo fi : fields) {
                String type_import = _get_type_import(fi);
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

        private static class FieldsBlock {
            int max_len = 0;
        }

        private static Map<String, FieldsBlock> _get_tp_blocks(Map<String, CustomField> custom_fields, List<FieldInfo> fields) {
            Map<String, FieldsBlock> res = new HashMap<String, FieldsBlock>();
            FieldsBlock current_block = null;
            for (FieldInfo fi : fields) {
                String just_type = _get_type_without_import_and_tag(fi);
                if (fi.getName().isEmpty()) {
                    continue;
                }
                int just_tp_len = just_type.length();
                String type_name = _get_type_without_import(fi.getType());
                boolean tag_exists = just_tp_len < type_name.length();
                String comment = null;
                CustomField cf = custom_fields.get(fi.getName());
                if (cf != null) {
                    comment = cf.comment;
                }
                if (!tag_exists && comment == null) {
                    current_block = null;
                } else {
                    if (current_block == null) {
                        current_block = new FieldsBlock();
                    }
                    if (just_tp_len > current_block.max_len) {
                        current_block.max_len = just_tp_len;
                    }
                }
                res.put(fi.getName(), current_block);
            }
            return res;
        }

        private static Map<String, FieldsBlock> _get_tag_blocks(Map<String, CustomField> custom_fields, List<FieldInfo> fields) {
            Map<String, FieldsBlock> res = new HashMap<String, FieldsBlock>();
            FieldsBlock current_block = null;
            for (FieldInfo fi : fields) {
                String just_type = _get_type_without_import_and_tag(fi);
                if (fi.getName().isEmpty()) {
                    continue;
                }
                String comment = null;
                CustomField cf = custom_fields.get(fi.getName());
                if (cf != null) {
                    comment = cf.comment;
                }
                if (comment == null) {
                    current_block = null;
                } else {
                    String type_name = _get_type_without_import(fi.getType());
                    int just_tp_len = just_type.length();
                    boolean tag_exists = just_tp_len < type_name.length();
                    if (tag_exists) {
                        if (current_block == null) {
                            current_block = new FieldsBlock();
                        }
                        String just_tag = type_name.substring(just_tp_len + 1).trim();
                        int just_tag_len = just_tag.length();
                        if (just_tag_len > current_block.max_len) {
                            current_block.max_len = just_tag_len;
                        }
                    }
                }
                res.put(fi.getName(), current_block);
            }
            return res;
        }

        private static List<FormattedField> _process_fields(DtoClass jaxb_dto_class, List<FieldInfo> fields) throws Exception {
            List<CustomField> custom_field_list = _get_custom_fields(jaxb_dto_class);
            _add_custom_fields(custom_field_list, fields);
            int max_name_len = -1;
            List<FormattedField> formatted_fields = new ArrayList<FormattedField>();
            for (FieldInfo fi : fields) {
                String just_type = _get_type_without_import_and_tag(fi);
                String name = fi.getName();
                if (name.isEmpty()) {
                    formatted_fields.add(new FormattedField(just_type));
                    continue;
                }
                int name_len = name.length();
                if (name_len > max_name_len) {
                    max_name_len = name_len;
                }
            }
            Map<String, CustomField> custom_fields = new HashMap<>();
            for (CustomField cf : custom_field_list) {
                if (!cf.name.isEmpty()) {
                    custom_fields.put(cf.name, cf);
                }
            }
            Map<String, FieldsBlock> tp_blocks = _get_tp_blocks(custom_fields, fields);
            Map<String, FieldsBlock> tag_blocks = _get_tag_blocks(custom_fields, fields);
            _set_formatted(formatted_fields, fields, max_name_len, tp_blocks, tag_blocks, custom_fields);
            return formatted_fields;
        }

        private static void _add_custom_fields(List<CustomField> custom_field_list, List<FieldInfo> fields) throws Exception {
            for (CustomField cf : custom_field_list) {
                if (cf.type.isEmpty()) {
                    continue;
                }
                FieldInfo fi = new FieldInfo(FieldNamesMode.AS_IS, "", "", "");
                fi.refine_name(cf.name);
                fi.refine_rendered_type(String.format("%s %s", cf.type, cf.tag));
                fields.add(fi);
            }
        }

        private static List<CustomField> _get_custom_fields(DtoClass jaxb_dto_class) {
            List<CustomField> res = new ArrayList<CustomField>();
            String custom = jaxb_dto_class.getCustom();
            if (custom != null) {
                _parse_custom(custom, res);
            }
            return res;
        }

        private static void _set_formatted(
                List<FormattedField> formatted_fields,
                List<FieldInfo> fields,
                int max_name_len,
                Map<String, FieldsBlock> tp_blocks,
                Map<String, FieldsBlock> tag_blocks,
                Map<String, CustomField> custom_fields) {

            final String name_format = "%-" + max_name_len + "." + max_name_len + "s";
            for (FieldInfo fi : fields) {
                String just_type = _get_type_without_import_and_tag(fi);
                if (fi.getName().isEmpty()) {
                    continue;
                }
                String just_tp_fmt;
                {
                    FieldsBlock block = tp_blocks.get(fi.getName());
                    int max_tp_len = block == null ? 0 : block.max_len;
                    if (max_tp_len == 0) {
                        just_tp_fmt = "%s";
                    } else {
                        just_tp_fmt = "%-" + max_tp_len + "." + max_tp_len + "s";
                    }
                }
                String type_name = _get_type_without_import(fi.getType());
                int just_tp_len = just_type.length();
                boolean tag_exists = just_tp_len < type_name.length();
                if (tag_exists) {
                    String type_format;
                    FieldsBlock block = tag_blocks.get(fi.getName());
                    int max_tag_len = block == null ? 0 : block.max_len;
                    if (max_tag_len > 0) {
                        type_format = just_tp_fmt + " %-" + max_tag_len + "." + max_tag_len + "s";
                    } else {
                        type_format = just_tp_fmt + " %s";
                    }
                    String just_tag = type_name.substring(just_tp_len + 1).trim();
                    type_name = String.format(type_format, just_type, just_tag); // no trim
                } else {
                    type_name = String.format(just_tp_fmt, just_type, ""); // no trim
                }
                String name = String.format(name_format, fi.getName());
                String fmt;
                String comment = null;
                CustomField cf = custom_fields.get(fi.getName());
                if (cf != null) {
                    comment = cf.comment;
                }
                if (comment == null) {
                    fmt = String.format("%s %s", name, type_name).trim();
                } else {
                    if (comment.isEmpty()) {
                        fmt = String.format("%s %s //", name, type_name);
                    } else {
                        fmt = String.format("%s %s // %s", name, type_name, comment.trim());
                    }
                }
                formatted_fields.add(new FormattedField(fmt));
            }
        }

        private static class CustomField {
            String name = "";
            String type = "";
            String tag = "";
            String comment = null;
        }

        private static void _parse(String line, CustomField cf) {
            String[] field_parts = line.split("\\s+");
            if (field_parts.length > 0) {
                cf.name = field_parts[0];
            }
            if (field_parts.length > 1) {
                cf.type = field_parts[1];
            }
            if (field_parts.length > 2) {
                cf.tag = field_parts[2];
            }
        }

        private static final String BASE = "{base}";

        private static void _parse_custom(String custom, List<CustomField> res) {
            String lines[] = custom.split("[\\r\\n]+");
            for (String line : lines) {
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }
                CustomField cf = new CustomField();
                if (line.startsWith(BASE)) {
                    String tp = line.substring(BASE.length()).trim();
                    if (tp.isEmpty()) {
                        continue;
                    }
                    cf.type = tp; // no comments for {base}
                    res.add(cf);
                    continue;
                }
                int pos = line.indexOf("//");
                if (pos >= 0) {
                    String field = line.substring(0, pos).trim();
                    _parse(field, cf);
                    cf.comment = line.substring(pos + 2);
                } else {
                    _parse(line, cf);
                }
                res.add(cf);
            }
        }
    }

    public static class DAO implements IDaoCG {

        private final String dto_package;
        private final String dao_package;

        private final String sql_root_abs_path;
        private final List<DtoClass> jaxb_dto_classes;
        private final Set<String> imports = new HashSet<String>();
        private final TemplateEngine te;
        private final JdbcUtils db_utils;
        private final Settings settings;

        private String dao_class_name;

        public DAO(
                List<DtoClass> jaxb_dto_classes,
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
        public String[] translate(DaoClass dao_class) throws Exception {
            imports.clear();
            this.dao_class_name = dao_class.getName();
            List<String> methods = new ArrayList<String>();
            JaxbUtils.process_jaxb_dao_class(this, dao_class_name, dao_class, methods);
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

        @Override
        public StringBuilder render_jaxb_query(Object jaxb_element) throws Exception {
            QueryMethodInfo mi = new QueryMethodInfo(jaxb_element);
            String xml_node_name = JaxbUtils.get_jaxb_node_name(jaxb_element);
            Helpers.check_required_attr(xml_node_name, mi.jaxb_method);
            try {
                if (mi.return_type_is_dto) {
                    _process_dto_class_name(mi.jaxb_dto_or_return_type);
                }
                String[] parsed = _parse_method_declaration2(mi.jaxb_method, dao_package);
                String method_name = parsed[0];
                String param_descriptors = parsed[1];
                String[] method_param_descriptors = Helpers.get_listed_items(param_descriptors, false);
                List<FieldInfo> fields = new ArrayList<FieldInfo>();
                List<FieldInfo> params = new ArrayList<FieldInfo>();
                String dao_query_jdbc_sql = db_utils.get_dao_query_info(sql_root_abs_path, mi.jaxb_ref, "",
                        method_param_descriptors, mi.jaxb_dto_or_return_type, mi.return_type_is_dto, jaxb_dto_classes,
                        fields, params);
                return _render_query(dao_query_jdbc_sql, mi.jaxb_is_external_sql, mi.jaxb_dto_or_return_type,
                        mi.return_type_is_dto, mi.fetch_list, method_name, // dto_param_type,
                        null, fields, params);
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
                String imp = _get_type_import(ret_fi);
                if (imp != null) {
                    imports.add(imp);
                }
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
                if (return_type_is_dto) {
                    context.put("mode", "dao_query_dto");
                } else {
                    context.put("mode", "dao_query");
                }
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
            return Helpers.get_method_name(method_name, db_utils.get_dto_field_names_mode());
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

        @Override
        public StringBuilder render_jaxb_exec_dml(ExecDml jaxb_exec_dml) throws Exception {
            String method = jaxb_exec_dml.getMethod();
            String ref = jaxb_exec_dml.getRef();
            String xml_node_name = JaxbUtils.get_jaxb_node_name(jaxb_exec_dml);
            Helpers.check_required_attr(xml_node_name, method);
            try {
                String dao_jdbc_sql = SqlUtils.jdbc_sql_by_exec_dml_ref(ref, sql_root_abs_path);
                String[] parsed = _parse_method_declaration2(method, dao_package);
                String method_name = parsed[0]; // never is null
                String param_descriptors = parsed[1]; // never is null
                String[] method_param_descriptors = Helpers.get_listed_items(param_descriptors, true);
                boolean is_external_sql = jaxb_exec_dml.isExternalSql();
                StringBuilder buff = new StringBuilder();
                _render_exec_dml(buff, dao_jdbc_sql, is_external_sql, method_name, // dto_param_type,
                        method_param_descriptors, xml_node_name, ref);
                return buff;

            } catch (Throwable e) {
                // e.printStackTrace();
                String msg = "<" + xml_node_name + " method=\"" + method + "\" ref=\"" + ref + "\"...\n";
                throw new Exception(Helpers.get_error_message(msg, e));
            }
        }

        // it is used only in render_jaxb_exec_dml
        private void _render_exec_dml(
                StringBuilder buffer,
                String jdbc_dao_sql,
                boolean is_external_sql,
                String method_name,
                String[] param_descriptors,
                String xml_node_name,
                String sql_path) throws Exception {

            List<FieldInfo> _params = new ArrayList<FieldInfo>();
            db_utils.get_dao_exec_dml_info(jdbc_dao_sql, "", param_descriptors, _params);
            for (FieldInfo pi : _params) {
                String imp = _get_type_import(pi);
                if (imp != null) {
                    imports.add(imp);
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
            context.put("class_name", dao_class_name);
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
                    String imp = _get_type_import(pi);
                    if (imp != null) {
                        imports.add(imp);
                    }
                    String just_type = _get_type_without_import_and_tag(pi);
                    pi.refine_rendered_type(just_type);
                }
            }
            context.put("params", params);
            if (context.get("imports") != null) {
                throw new Exception("Invalid assignment of 'imports'");
            }
            context.put("imports", imports);
        }

        private String[] _parse_method_declaration2(String method_text, String dto_package) throws Exception {
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
            return new String[]{method_name, param_descriptors};
        }

        private static void _set_model(String dto_class_name, Map<String, Object> context) {
            String model = "";
            int model_name_end_index = dto_class_name.indexOf('-');
            if (model_name_end_index != -1) {
                model = dto_class_name.substring(0, model_name_end_index);
            }
            context.put("model", model);
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
            return _render_query(dao_jdbc_sql, false, dto_class_name, true, fetch_list, method_name, dao_table_name, //dao_table_name,
                    fields_all, fields_pk);
        }

        @Override
        public StringBuilder render_crud_update(
                String dao_class_name,
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
        public StringBuilder render_jaxb_crud(String dao_class_name, Crud jaxb_type_crud) throws Exception {
            String node_name = JaxbUtils.get_jaxb_node_name(jaxb_type_crud);
            String dto_class_name = jaxb_type_crud.getDto();
            if (dto_class_name == null || dto_class_name.isEmpty()) {
                throw new Exception("<" + node_name + "...\nDTO class is not set");
            }
            try {
                _process_dto_class_name(dto_class_name);
                DtoClass jaxb_dto_class = JaxbUtils.find_jaxb_dto_class(dto_class_name, jaxb_dto_classes);
                StringBuilder code_buff = JaxbUtils.process_jaxb_crud(this, db_utils.get_dto_field_names_mode(),
                        jaxb_type_crud, dao_class_name, jaxb_dto_class);
                return code_buff;
            } catch (Throwable e) {
                // e.printStackTrace();
                String msg = "<" + node_name + " dto=\"" + dto_class_name + "\" table=\"" + jaxb_type_crud.getTable() + "\"...\n";
                throw new Exception(Helpers.get_error_message(msg, e));
            }
        }
    }
}
