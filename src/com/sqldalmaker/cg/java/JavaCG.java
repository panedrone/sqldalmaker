/*
 * Copyright 2011-2020 sqldalmaker@gmail.com
 * SQL DAL Maker Website: http://sqldalmaker.sourceforge.net
 * Read LICENSE.txt in the root of this project/archive for details.
 */
package com.sqldalmaker.cg.java;

import com.sqldalmaker.cg.*;
import com.sqldalmaker.jaxb.dao.*;
import com.sqldalmaker.jaxb.dto.DtoClass;
import com.sqldalmaker.jaxb.dto.DtoClasses;

import java.io.StringWriter;
import java.sql.Connection;
import java.util.*;

/**
 * @author sqldalmaker@gmail.com
 */
public class JavaCG {

    private static String get_template_path() {
        return Helpers.class.getPackage().getName().replace('.', '/') + "/java/java.vm";
    }

    public static class DTO implements IDtoCG {

        private final String dto_package;
        private final String sql_root_abs_path;
        private final String dto_inheritance;
        private final List<DtoClass> jaxb_dto_classes;
        private final TemplateEngine te;
        private final JdbcUtils db_utils;

        public DTO(DtoClasses jaxb_dto_classes, Connection connection, String dto_package, String sql_root_abs_path,
                   String dto_inheritance, FieldNamesMode field_names_mode, String vm_file_system_dir) throws Exception {
            this.jaxb_dto_classes = jaxb_dto_classes.getDtoClass();
            this.dto_package = dto_package;
            this.sql_root_abs_path = sql_root_abs_path;
            this.dto_inheritance = dto_inheritance;
            if (vm_file_system_dir == null) {
                te = new TemplateEngine(get_template_path(), false);
            } else {
                te = new TemplateEngine(vm_file_system_dir, true);
            }
            db_utils = new JdbcUtils(connection, field_names_mode, null);
        }

        @Override
        public String[] translate(String dto_class_name) throws Exception {
            DtoClass jaxb_dto_class = null;
            for (DtoClass cls : jaxb_dto_classes) {
                if (cls.getName().equals(dto_class_name)) {
                    jaxb_dto_class = cls;
                    break;
                }
            }
            if (jaxb_dto_class == null) {
                throw new Exception("XML element of DTO class '" + dto_class_name + "' not found");
            }
            List<FieldInfo> fields = new ArrayList<FieldInfo>();
            db_utils.get_dto_field_info(jaxb_dto_class, sql_root_abs_path, fields);
            Map<String, Object> context = new HashMap<String, Object>();
            context.put("package", dto_package);
            context.put("class_name", dto_class_name);
            context.put("implements", dto_inheritance);
            context.put("fields", fields);
            context.put("mode", "dto_class");
            StringWriter sw = new StringWriter();
            te.merge(context, sw);
            String text = sw.toString();
            text = text.replace("java.lang.", "");
            return new String[]{text};
        }
    }

    public static class DAO implements IDaoCG {

        private final String dto_package;
        private final String dao_package;
        private final String sql_root_abs_path;
        private final DtoClasses jaxb_dto_classes;
        private final Set<String> imports = new HashSet<String>();
        private final TemplateEngine te;
        private final JdbcUtils db_utils;

        public DAO(DtoClasses jaxb_dto_classes, Connection connection, String dto_package, String dao_package,
                   String sql_root_abs_path, FieldNamesMode field_names_mode, String vm_file_system_dir) throws Exception {
            this.jaxb_dto_classes = jaxb_dto_classes;
            this.dto_package = dto_package;
            this.dao_package = dao_package;
            this.sql_root_abs_path = sql_root_abs_path;
            if (vm_file_system_dir == null) {
                te = new TemplateEngine(get_template_path(), false);
            } else {
                te = new TemplateEngine(vm_file_system_dir, true);
            }
            db_utils = new JdbcUtils(connection, field_names_mode, null);
        }

        @Override
        public String[] translate(String dao_class_name, DaoClass dao_class) throws Exception {
            imports.clear();
            List<String> methods = new ArrayList<String>();
            JaxbUtils.process_jaxb_dao_class(this, dao_class, methods);
            Map<String, Object> context = new HashMap<String, Object>();
            context.put("package", dao_package);
            String[] arr = new String[imports.size()];
            String[] imports_arr = imports.toArray(arr);
            Arrays.sort(imports_arr);
            context.put("imports", imports_arr);
            context.put("class_name", dao_class_name);
            context.put("methods", methods);
            context.put("mode", "dao_class");
            StringWriter sw = new StringWriter();
            te.merge(context, sw);
            String text = sw.toString();
            text = text.replace("java.lang.", "");
            return new String[]{text};
        }

        @Override
        public StringBuilder render_jaxb_query(Object jaxb_element) throws Exception {
            QueryMethodInfo mi = new QueryMethodInfo(jaxb_element);
            String xml_node_name = JaxbUtils.get_jaxb_node_name(jaxb_element);
            Helpers.check_required_attr(xml_node_name, mi.jaxb_method);
            try {
                if (mi.return_type_is_dto) {
                    _process_dto_class_name(dto_package, mi.jaxb_dto_or_return_type);
                }
                String[] parsed = _parse_method_declaration(mi.jaxb_method, dto_package);
                String method_name = parsed[0];
                String dto_param_type = parsed[1];
                String param_descriptors = parsed[2];
                String[] method_param_descriptors = Helpers.get_listed_items(param_descriptors, false);
                List<FieldInfo> fields = new ArrayList<FieldInfo>();
                List<FieldInfo> params = new ArrayList<FieldInfo>();
                String dao_query_jdbc_sql = db_utils.get_dao_query_info(
                        sql_root_abs_path, mi.jaxb_ref, dto_param_type, method_param_descriptors,
                        mi.jaxb_dto_or_return_type, mi.return_type_is_dto, jaxb_dto_classes, fields, params);
                return _render_query(dao_query_jdbc_sql, mi.jaxb_is_external_sql,
                        mi.jaxb_dto_or_return_type, mi.return_type_is_dto, mi.fetch_list,
                        method_name, dto_param_type, null, fields, params);
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
        private StringBuilder _render_query(String dao_query_jdbc_sql, boolean is_external_sql,
                                            String jaxb_dto_or_return_type, boolean jaxb_return_type_is_dto, boolean fetch_list,
                                            String method_name, String dto_param_type, String crud_table,
                                            List<FieldInfo> fields, List<FieldInfo> params) throws Exception {
            if (dao_query_jdbc_sql == null) {
                return Helpers.get_no_pk_warning(method_name);
            }
            String returned_type_name;
            if (jaxb_return_type_is_dto) {
                returned_type_name = _get_rendered_dto_class_name(jaxb_dto_or_return_type);
            } else {
                returned_type_name = fields.get(0).getType();
            }
            String java_sql_str = SqlUtils.jdbc_sql_to_java_str(dao_query_jdbc_sql);
            Map<String, Object> context = new HashMap<String, Object>();
            context.put("mode", "dao_query");
            context.put("fields", fields);
            context.put("method_name", method_name);
            context.put("crud", crud_table != null);
            context.put("ref", crud_table);
            context.put("sql", java_sql_str);
            context.put("use_dto", jaxb_return_type_is_dto);
            context.put("returned_type_name", returned_type_name);
            context.put("fetch_list", fetch_list);
            context.put("imports", imports);
            context.put("is_external_sql", is_external_sql);
            _assign_params(params, dto_param_type, context);
            StringWriter sw = new StringWriter();
            te.merge(context, sw);
            StringBuilder buff = new StringBuilder();
            buff.append(sw.getBuffer());
            return buff;
        }

        private String _get_rendered_dto_class_name(String dto_class_name) throws Exception {
            DtoClass jaxb_dto_class = JaxbUtils.find_jaxb_dto_class(dto_class_name, jaxb_dto_classes);
            return jaxb_dto_class.getName();
        }

        private void _process_dto_class_name(String dto_package, String dto_class_name) {
            if (dto_package != null && dto_package.length() > 0) {
                imports.add(dto_package + "." + dto_class_name);
            } else {
                imports.add(dto_class_name);
            }
        }

        @Override
        public StringBuilder render_jaxb_exec_dml(ExecDml jaxb_exec_dml) throws Exception {
            String method = jaxb_exec_dml.getMethod();
            String ref = jaxb_exec_dml.getRef();
            String xml_node_name = JaxbUtils.get_jaxb_node_name(jaxb_exec_dml);
            Helpers.check_required_attr(xml_node_name, method);
            try {
                String dao_jdbc_sql = SqlUtils.jdbc_sql_by_exec_dml_ref(ref, sql_root_abs_path);
                String[] parsed = _parse_method_declaration(method, dto_package);
                String method_name = parsed[0]; // never is null
                String dto_param_type = parsed[1]; // never is null
                String param_descriptors = parsed[2]; // never is null
                String[] method_param_descriptors = Helpers.get_listed_items(param_descriptors, true);
                boolean is_external_sql = jaxb_exec_dml.isExternalSql();
                StringBuilder buff = new StringBuilder();
                _render_exec_dml(buff, dao_jdbc_sql, is_external_sql, method_name, dto_param_type, method_param_descriptors, xml_node_name, ref);
                return buff;

            } catch (Throwable e) {
                // e.printStackTrace();
                String msg = "<" + xml_node_name + " method=\"" + method + "\" ref=\"" + ref + "\"...\n";
                throw new Exception(Helpers.get_error_message(msg, e));
            }
        }

        private void _render_exec_dml(StringBuilder buffer, String jdbc_dao_sql, boolean is_external_sql,
                                      String method_name, String dto_param_type, String[] param_descriptors,
                                      String xml_node_name, String sql_path) throws Exception {
            SqlUtils.throw_if_select_sql(jdbc_dao_sql);
            List<FieldInfo> _params = new ArrayList<FieldInfo>();
            db_utils.get_dao_exec_dml_info(jdbc_dao_sql, dto_param_type, param_descriptors, _params);
            String java_sql = SqlUtils.jdbc_sql_to_java_str(jdbc_dao_sql);
            List<MappingInfo> m_list = new ArrayList<MappingInfo>();
            List<FieldInfo> method_params = new ArrayList<FieldInfo>();
            List<FieldInfo> exec_dml_params = new ArrayList<FieldInfo>();
            for (int pd_i = 0; pd_i < param_descriptors.length; pd_i++) {
                FieldInfo p = _params.get(pd_i);
                String pd = param_descriptors[pd_i];
                if (pd.startsWith("[") && pd.endsWith("]")) {
                    String inner_list = pd.substring(1, pd.length() - 1);
                    String[] implicit_param_descriptors = Helpers.get_listed_items(inner_list, true);
                    List<String> cb_elements = new ArrayList<String>();
                    for (int ipd_i = 0; ipd_i < implicit_param_descriptors.length; ipd_i++) {
                        String ipd = implicit_param_descriptors[ipd_i];
                        String []parts = _parse_param_descriptor(ipd);
                        if (parts == null) {
                            throw new Exception("Implicit cursors are specified incorrectly."
                                    + " Expected syntax: [on_dto_1:Dto1, on_dto_2:Dto2, ...]. Specified: "
                                    + "[" + String.join(",", implicit_param_descriptors) + "]");
                        }
                        MappingInfo m = _create_mapping(parts);
                        m_list.add(m);
                        method_params.add(new FieldInfo(FieldNamesMode.AS_IS, String.format("RecordHandler<%s>", m.dto_class_name), m.method_param_name, "parameter"));
                        cb_elements.add(m.exec_dml_param_name);
                    }
                    String exec_xml_param = "new RowHandler2[] {" + String.join(",", cb_elements) + "}";
                    exec_dml_params.add(new FieldInfo(FieldNamesMode.AS_IS, p.getType(), exec_xml_param, "parameter"));
                } else {
                    String param_descriptor = param_descriptors[pd_i];
                    String[] parts = _parse_param_descriptor(param_descriptor);
                    if (parts != null) {
                        MappingInfo m = _create_mapping(parts);
                        m_list.add(m);
                        method_params.add(new FieldInfo(FieldNamesMode.AS_IS, String.format("RecordHandler<%s>", m.dto_class_name), m.method_param_name, "parameter"));
                        exec_dml_params.add(new FieldInfo(FieldNamesMode.AS_IS, p.getType(), m.exec_dml_param_name, "parameter"));
                    } else {
                        method_params.add(p);
                        exec_dml_params.add(p);
                    }
                }
            }
            Map<String, Object> context = new HashMap<String, Object>();
            _assign_params(method_params, dto_param_type, context);
            context.put("params2", exec_dml_params);
            context.put("mappings", m_list);
            boolean plain_params = dto_param_type.length() == 0;
            context.put("plain_params", plain_params);
            context.put("class_name", null);
            context.put("method_name", method_name);
            context.put("sql", java_sql);
            context.put("xml_node_name", xml_node_name);
            context.put("sql_path", sql_path);
            context.put("is_external_sql", is_external_sql);
            context.put("mode", "dao_exec_dml");
            StringWriter sw = new StringWriter();
            te.merge(context, sw);
            buffer.append(sw.getBuffer());
        }

        private static String[] _parse_param_descriptor(String param_descriptor) {
            String []parts = null;
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
            m.method_param_name = parts[0].trim();
            String cb_param_name = String.format("_map_cb_%s", m.method_param_name);
            m.exec_dml_param_name = cb_param_name;
            m.dto_class_name = parts[1].trim();
            List<FieldInfo> fields = new ArrayList<FieldInfo>();
            DtoClass jaxb_dto_class = JaxbUtils.find_jaxb_dto_class(m.dto_class_name, jaxb_dto_classes);
            imports.add("com.sqldalmaker.DataStore.RowData");
            imports.add("com.sqldalmaker.DataStore.RowHandler2");
            imports.add("com.sqldalmaker.DataStore.RecordHandler");
            _get_rendered_dto_class_name(jaxb_dto_class.getName()); // extends both imports and uses
            db_utils.get_dto_field_info(jaxb_dto_class, sql_root_abs_path, fields);
            if (fields.size() > 0) {
                fields.get(0).setComment(fields.get(0).getComment() + " [INFO] REF CURSOR");
            }
            m.fields.addAll(fields);
            return m;
        }

        private void _assign_params(List<FieldInfo> params, String dto_param_type, Map<String, Object> context) throws Exception {
            int params_count = params.size();
            if (dto_param_type.length() > 0) {
                if (params_count == 0) {
                    throw new Exception("DTO parameter specified but SQL-query does not contain any parameters");
                }
                _process_dto_class_name(dto_package, dto_param_type);
                context.put("dto_param", _get_rendered_dto_class_name(dto_param_type));
            } else {
                context.put("dto_param", "");
            }
            context.put("params", params);
        }

        private String[] _parse_method_declaration(String method_text, String dto_package) throws Exception {
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
                        _process_dto_class_name(dto_package, dto_param_type);
                    }
                } else {
                    param_descriptors = parts[0];
                }
            }
            return new String[]{method_name, dto_param_type, param_descriptors};
        }

        @Override
        public StringBuilder render_crud_create(String class_name, String method_name, String table_name,
                                                String dto_class_name, boolean fetch_generated, String generated) throws Exception {
            List<FieldInfo> fields_not_ai = new ArrayList<FieldInfo>();
            List<FieldInfo> fields_ai = new ArrayList<FieldInfo>();
            DtoClass jaxb_dto_class = JaxbUtils.find_jaxb_dto_class(dto_class_name, jaxb_dto_classes);
            String dao_jdbc_sql = db_utils.get_dao_crud_create_info(jaxb_dto_class, sql_root_abs_path, table_name, generated, fields_not_ai, fields_ai);
            String sql_str = SqlUtils.jdbc_sql_to_java_str(dao_jdbc_sql);
            Map<String, Object> context = new HashMap<String, Object>();
            context.put("method_type", "CREATE");
            context.put("table_name", table_name);
            context.put("class_name", class_name);
            context.put("sql", sql_str);
            context.put("method_name", method_name);
            context.put("params", fields_not_ai);
            context.put("dto_param", _get_rendered_dto_class_name(dto_class_name));
            if (fetch_generated && fields_ai.size() > 0) {
                context.put("keys", fields_ai);
                context.put("mode", "dao_create");
            } else {
                context.put("plain_params", true);
                context.put("is_external_sql", false);
                context.put("mode", "dao_exec_dml");
            }
            StringWriter sw = new StringWriter();
            te.merge(context, sw);
            StringBuilder buffer = new StringBuilder();
            buffer.append(sw.getBuffer());
            return buffer;
        }

        @Override
        public StringBuilder render_crud_read(String method_name, String dao_table_name, String dto_class_name,
                                              String explicit_pk, boolean fetch_list) throws Exception {
            List<FieldInfo> fields_all = new ArrayList<FieldInfo>();
            List<FieldInfo> fields_pk = new ArrayList<FieldInfo>();
            DtoClass jaxb_dto_class = JaxbUtils.find_jaxb_dto_class(dto_class_name, jaxb_dto_classes);
            String dao_jdbc_sql = db_utils.get_dao_crud_read_info(fetch_list, jaxb_dto_class, sql_root_abs_path,
                    dao_table_name, explicit_pk, fields_all, fields_pk);
            return _render_query(dao_jdbc_sql, false, dto_class_name, true, fetch_list,
                    method_name, "", dao_table_name, fields_all, fields_pk);
        }

        @Override
        public StringBuilder render_crud_update(String class_name, String method_name, String table_name,
                                                String explicit_pk, String dto_class_name, boolean primitive_params) throws Exception {
            List<FieldInfo> fields_not_pk = new ArrayList<FieldInfo>();
            List<FieldInfo> fields_pk = new ArrayList<FieldInfo>();
            DtoClass jaxb_dto_class = JaxbUtils.find_jaxb_dto_class(dto_class_name, jaxb_dto_classes);
            String dao_jdbc_sql = db_utils.get_dao_crud_update_info(table_name, jaxb_dto_class, sql_root_abs_path, explicit_pk, fields_not_pk, fields_pk);
            if (fields_pk.isEmpty()) {
                return Helpers.get_no_pk_warning(method_name);
            }
            if (fields_not_pk.isEmpty()) {
                return Helpers.get_only_pk_warning(method_name);
            }
            String sql_str = SqlUtils.jdbc_sql_to_java_str(dao_jdbc_sql);
            fields_not_pk.addAll(fields_pk);
            Map<String, Object> context = new HashMap<String, Object>();
            context.put("mode", "dao_exec_dml");
            context.put("class_name", class_name);
            context.put("plain_params", true);
            context.put("method_name", method_name);
            context.put("sql", sql_str);
            context.put("method_type", "UPDATE");
            context.put("table_name", table_name);
            context.put("dto_param", primitive_params ? "" : _get_rendered_dto_class_name(dto_class_name));
            context.put("params", fields_not_pk);
            context.put("is_external_sql", false);
            StringWriter sw = new StringWriter();
            te.merge(context, sw);
            StringBuilder buffer = new StringBuilder();
            buffer.append(sw.getBuffer());
            return buffer;
        }

        @Override
        public StringBuilder render_crud_delete(String class_name, String method_name, String table_name, String explicit_pk) throws Exception {
            List<FieldInfo> fields_pk = new ArrayList<FieldInfo>();
            String dao_jdbc_sql = db_utils.get_dao_crud_delete_info(table_name, explicit_pk, fields_pk);
            if (fields_pk.isEmpty()) {
                return Helpers.get_no_pk_warning(method_name);
            }
            String java_sql_str = SqlUtils.jdbc_sql_to_java_str(dao_jdbc_sql);
            Map<String, Object> context = new HashMap<String, Object>();
            context.put("mode", "dao_exec_dml");
            context.put("class_name", class_name);
            context.put("plain_params", true);
            context.put("method_name", method_name);
            context.put("sql", java_sql_str);
            context.put("method_type", "DELETE");
            context.put("table_name", table_name);
            context.put("dto_param", "");
            context.put("params", fields_pk);
            context.put("is_external_sql", false);
            StringWriter sw = new StringWriter();
            te.merge(context, sw);
            StringBuilder buffer = new StringBuilder();
            buffer.append(sw.getBuffer());
            return buffer;
        }

        @Override
        public StringBuilder render_jaxb_crud(TypeCrud jaxb_type_crud) throws Exception {
            String node_name = JaxbUtils.get_jaxb_node_name(jaxb_type_crud);
            String dto_class_name = jaxb_type_crud.getDto();
            if (dto_class_name.length() == 0) {
                throw new Exception("<" + node_name + "...\nDTO class is not set");
            }
            String table_attr = jaxb_type_crud.getTable();
            if (table_attr == null || table_attr.length() == 0) {
                throw new Exception("<" + node_name + "...\nRequired attribute is not set");
            }
            try {
                db_utils.validate_table_name(table_attr);
                _process_dto_class_name(dto_package, dto_class_name);
                StringBuilder code_buff = JaxbUtils.process_jaxb_crud(this, false, jaxb_type_crud, dto_class_name);
                return code_buff;
            } catch (Throwable e) {
                // e.printStackTrace();
                String msg = "<" + node_name + " dto=\"" + dto_class_name + "\" table=\"" + table_attr + "\"...\n";
                throw new Exception(Helpers.get_error_message(msg, e));
            }
        }
    }
}
