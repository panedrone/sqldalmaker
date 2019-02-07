/*
 * Copyright 2011-2018 sqldalmaker@gmail.com
 * SQL DAL Maker Website: http://sqldalmaker.sourceforge.net
 * Read LICENSE.txt in the root of this project/archive for details.
 */
package com.sqldalmaker.cg.cpp;

import com.sqldalmaker.cg.*;
import com.sqldalmaker.jaxb.dao.*;
import com.sqldalmaker.jaxb.dto.DtoClass;
import com.sqldalmaker.jaxb.dto.DtoClasses;
import com.sqldalmaker.jaxb.settings.TypeMap;

import java.io.StringWriter;
import java.sql.Connection;
import java.util.*;

/**
 *
 * @author sqldalmaker@gmail.com
 *
 */
public class CppCG {

    private static String get_template_path() {

        return Helpers.class.getPackage().getName().replace('.', '/') + "/cpp/cpp.vm";
    }

    public static class DTO implements IDtoCG {

        private final String sql_root_abs_path;

        private final List<DtoClass> dto_classes;

        private final TemplateEngine te;

        private final DbUtils db_utils;

        private final String dto_class_prefix;

        public DTO(DtoClasses dto_classes, TypeMap type_map, Connection connection, String sql_root_abs_path,
                String dto_class_prefix, String vm_file_system_dir) throws Exception {

            this.dto_classes = dto_classes.getDtoClass();

            this.dto_class_prefix = dto_class_prefix;

            this.sql_root_abs_path = sql_root_abs_path;

            if (vm_file_system_dir == null) {

                te = new TemplateEngine(get_template_path(), false);

            } else {

                te = new TemplateEngine(vm_file_system_dir, true);
            }

            db_utils = new DbUtils(connection, FieldNamesMode.AS_IS, type_map);
        }

        @Override
        public String[] translate(String dto_class_base_name) throws Exception {

            DtoClass cls_element = null;

            for (DtoClass cls : dto_classes) {

                if (cls.getName().equals(dto_class_base_name)) {

                    cls_element = cls;

                    break;
                }
            }

            if (cls_element == null) {

                throw new Exception("XML element of DTO class '" + dto_class_base_name + "' not found");
            }

            ArrayList<FieldInfo> fields = db_utils.get_dto_field_info(sql_root_abs_path, cls_element);

            HashMap<String, Object> context = new HashMap<String, Object>();

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

        private final DtoClasses dto_classes;

        private final TypeMap type_map;

        private final Set<String> imports = new HashSet<String>();

        private final TemplateEngine te;

        private final DbUtils db_utils;

        public DAO(DtoClasses dto_classes, TypeMap type_map, Connection connection, String sql_root_abs_path,
                String class_prefix, String vm_file_system_dir) throws Exception {

            this.dto_classes = dto_classes;

            this.type_map = type_map;

            this.sql_root_abs_path = sql_root_abs_path;

            this.class_prefix = class_prefix;

            if (vm_file_system_dir == null) {

                te = new TemplateEngine(get_template_path(), false);

            } else {

                te = new TemplateEngine(vm_file_system_dir, true);
            }

            db_utils = new DbUtils(connection, FieldNamesMode.AS_IS, type_map);
        }

        @Override
        public String[] translate(String dao_class_name, DaoClass dao_class) throws Exception {

            imports.clear();

            imports.add("DataStore.h");

            List<String> methods = new ArrayList<String>();

            CodeGeneratorHelpers.process_element(this, dao_class, methods);

            HashMap<String, Object> context = new HashMap<String, Object>();

            String[] imports_arr = imports.toArray(new String[imports.size()]);
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
        public StringBuilder render_element_query(Object element) throws Exception {

            StringBuilder buff = new StringBuilder();

            String xml_node_name = Helpers.get_xml_node_name(element);
            String method;
            String ref;

            boolean is_external_sql;
            boolean return_type_is_dto = false;
            String return_type;
            boolean fetch_list = (element instanceof QueryDtoList) || (element instanceof QueryList);

            if (element instanceof Query) {

                Query q = (Query) element;
                method = q.getMethod();
                ref = q.getRef();
                is_external_sql = q.is_external_sql();
                return_type = q.getReturnType();

            } else if (element instanceof QueryList) {

                QueryList q = (QueryList) element;
                method = q.getMethod();
                ref = q.getRef();
                is_external_sql = q.is_external_sql();
                return_type = q.getReturnType();

            } else if (element instanceof QueryDto) {

                QueryDto q = (QueryDto) element;
                method = q.getMethod();
                ref = q.getRef();
                is_external_sql = q.is_external_sql();
                return_type = q.getDto();
                return_type_is_dto = true;

            } else if (element instanceof QueryDtoList) {

                QueryDtoList q = (QueryDtoList) element;
                method = q.getMethod();
                ref = q.getRef();
                is_external_sql = q.is_external_sql();
                return_type = q.getDto();
                return_type_is_dto = true;

            } else {

                throw new Exception("Unexpected XML element: " + xml_node_name);
            }

            check_required_attr(xml_node_name, method);

            try {

                if (return_type_is_dto) {

                    process_dto_class_name(return_type);
                }

                String sql = DbUtils.sql_by_ref(ref, sql_root_abs_path);

                String[] parsed = parse_method_declaration(method);

                String method_name = parsed[0];
                String dto_param_type = parsed[1];
                String param_descriptors = parsed[2];

                String[] param_descriptors_arr = Helpers.get_listed_items(param_descriptors);

                render_element_query(buff, sql, is_external_sql, return_type, return_type_is_dto, fetch_list,
                        method_name, dto_param_type, param_descriptors_arr, false, xml_node_name, ref);

            } catch (Throwable e) {

                // e.printStackTrace();
                String msg = "<" + xml_node_name + " method=\"" + method + "\" ref=\"" + ref + "\"...\n";

                throw new Exception(Helpers.get_error_message(msg, e));
            }

            return buff;
        }

        private String get_rendered_dto_class_name(String dto_class_name) throws Exception {

            DtoClass dto_def = Helpers.find_dto_class(dto_class_name, dto_classes);

            if (dto_def != null) {

                return class_prefix + dto_def.getName();

            } else {

                throw new Exception("Element not found: " + dto_class_name);
            }
        }

        private void process_dto_class_name(String dto_class_name) throws Exception {

            DtoClass dto_def = Helpers.find_dto_class(dto_class_name, dto_classes);

            if (dto_def != null) {

                imports.add(dto_def.getName() + ".h");
            }
        }

        @Override
        public StringBuilder render_element_exec_dml(ExecDml element) throws Exception {

            StringBuilder buff = new StringBuilder();

            String xml_node_name = Helpers.get_xml_node_name(element);
            String method = element.getMethod();
            String ref = element.getRef();

            check_required_attr(xml_node_name, method);

            try {

                String sql_file_name = Helpers.concat_path(sql_root_abs_path, ref);
                String sql = Helpers.load_text_from_file(sql_file_name);

                String[] parsed = parse_method_declaration(method);

                String method_name = parsed[0]; // never is null
                String dto_param_type = parsed[1]; // never is null
                String param_descriptors = parsed[2]; // never is null

                String[] param_descriptors_arr = Helpers.get_listed_items(param_descriptors);

                boolean is_external_sql = element.is_external_sql();

                render_element_exec_dml(buff, sql, is_external_sql, null, method_name, dto_param_type,
                        param_descriptors_arr, xml_node_name, ref);

            } catch (Throwable e) {

                // e.printStackTrace();
                String msg = "<" + xml_node_name + " method=\"" + method + "\" ref=\"" + ref + "\"...\n";

                throw new Exception(Helpers.get_error_message(msg, e));
            }

            return buff;
        }

        private void render_element_exec_dml(StringBuilder buffer, String sql, boolean is_external_sql,
                String class_name, String method_name, String dto_param_type, String[] param_descriptors,
                String xml_node_name, String sql_path) throws Exception {

            ArrayList<FieldInfo> fields = new ArrayList<FieldInfo>();

            ArrayList<FieldInfo> params = new ArrayList<FieldInfo>();

            db_utils.sql_to_metadata(sql, fields, dto_param_type, param_descriptors, params, "", dto_classes);

            // For Informix: ResultSetMetaData.getColumnCount() returns
            // value > 0 for some DML statements, e.g. for 'update orders set
            // dt_id = ? where o_id = ?' it considers that 'dt_id' is column.
            String trimmed = sql.toLowerCase().trim();
            String[] parts = trimmed.split("\\s+");

            if (parts.length > 0) {

                if ("select".equals(parts[0])) {

                    throw new Exception("SELECT is not allowed here");
                }
            }

            String sql_str = Helpers.sql_to_cpp_str(sql);

            HashMap<String, Object> context = new HashMap<String, Object>();

            assign_params(params, dto_param_type, context);

            // if (dto_param_type != null && dto_param_type.length() > 0) { FireBug
            // does not like it
            context.put("dto_param", dto_param_type);
            // }

            context.put("class_name", class_name);
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

        private void render_element_query(StringBuilder buff, String sql, boolean is_external_sql, String return_type,
                boolean return_type_is_dto, boolean fetch_list, String method_name, String dto_param_type,
                String[] param_descriptors, boolean crud, String xml_node_name, String ref) throws Exception {

            ArrayList<FieldInfo> fields = new ArrayList<FieldInfo>();

            ArrayList<FieldInfo> params = new ArrayList<FieldInfo>();

            db_utils.sql_to_metadata(sql, fields, dto_param_type, param_descriptors, params,
                    return_type_is_dto ? return_type : null, dto_classes);

            int col_count = fields.size();

            if (col_count == 0) {

                throw new Exception("Columns count is 0. Is SQL statement valid?");
            }

            if (return_type_is_dto) {

                return_type = get_rendered_dto_class_name(return_type);

            } else {

                if (return_type == null || return_type.length() == 0) {

                    if (col_count != 1) {

                        throw new Exception("ResultSet should contain 1 column. Is SQL statement valid?");
                    }

                    return_type = fields.get(0).getType();

                } else {

                    return_type = DbUtils.get_cpp_class_name_from_java_class_name(type_map, return_type);
                }
            }

            String sql_str = Helpers.sql_to_cpp_str(sql);

            HashMap<String, Object> context = new HashMap<String, Object>();

            assign_params(params, dto_param_type, context);

            context.put("fields", fields);
            context.put("method_name", method_name);
            context.put("crud", crud);
            context.put("xml_node_name", xml_node_name);
            context.put("ref", ref);
            context.put("sql", sql_str);
            context.put("return_type_is_dto", return_type_is_dto);
            context.put("returned_type_name", return_type);
            context.put("fetch_list", fetch_list);
            context.put("imports", imports);
            context.put("external_sql", is_external_sql);
            context.put("mode", "dao_query");

            StringWriter sw = new StringWriter();
            te.merge(context, sw);
            buff.append(sw.getBuffer());
        }

        private void assign_params(ArrayList<FieldInfo> params, String dto_param_type, HashMap<String, Object> context)
                throws Exception {

            int params_count = params.size();

            if (dto_param_type.length() > 0) {

                if (params_count == 0) {

                    throw new Exception("DTO parameter specified but SQL-query does not contain any parameters");
                }

                process_dto_class_name(dto_param_type);

                context.put("dto_param", get_rendered_dto_class_name(dto_param_type));

            } else {

                context.put("dto_param", "");
            }

            context.put("params", params);
        }

        private String[] parse_method_declaration(String method_text) throws Exception {

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

                        process_dto_class_name(dto_param_type);
                    }

                } else {

                    param_descriptors = parts[0];
                }
            }

            return new String[]{method_name, dto_param_type, param_descriptors};
        }

        private static void check_required_attr(String node_name, String method_name_attr) throws Exception {

            if (method_name_attr == null || method_name_attr.length() == 0) {

                throw new Exception("<" + node_name + "...\n'method' is not set.");
            }
        }

        @Override
        public StringBuilder render_element_crud_read(StringBuilder sql_buff, String method_name, String table_name,
                String ret_dto_type, boolean fetch_list) throws Exception {

            StringBuilder buffer = new StringBuilder();

            ArrayList<FieldInfo> keys = new ArrayList<FieldInfo>();

            if (!fetch_list) {

                // !!!! - type_map == null to return Java types: render_element_query
                // below translates them to C++ types
                db_utils.get_crud_info(table_name, keys, null, ret_dto_type, dto_classes, null);

                if (keys.isEmpty()) {

                    String msg = Helpers.get_no_pk_message(method_name);

                    Helpers.build_warning_comment(buffer, msg);

                    return buffer;
                }

                // if all values of the table are the parts of PK,
                // the query like 'select k1, k2 from table where k1=? and k2=?'
                // is useless
                // but maybe, somebody needs it...
            }

            HashMap<String, Object> context = new HashMap<String, Object>();
            context.put("keys", keys);

            String mode = fetch_list ? "crud_sql_read_all" : "crud_sql_read_single";

            StringWriter sw = new StringWriter();
            generate_sql(mode, context, table_name, sw);
            sql_buff.append(sw.getBuffer());

            ArrayList<String> desc = new ArrayList<String>();

            for (FieldInfo k : keys) {

                desc.add(k.getType() + " " + k.getName());
            }

            String[] param_descriptors_arr = desc.toArray(new String[desc.size()]);

            render_element_query(buffer, sql_buff.toString(), false, ret_dto_type, true, fetch_list, method_name, "",
                    param_descriptors_arr, true, null, table_name);

            return buffer;
        }

        @Override
        public StringBuilder render_element_crud_insert(StringBuilder sql_buff, String class_name, String method_name,
                String table_name, String dto_class_name, boolean fetch_generated, String generated) throws Exception {

            ArrayList<FieldInfo> params = new ArrayList<FieldInfo>();

            ArrayList<FieldInfo> keys = new ArrayList<FieldInfo>();

            List<String> sql_col_names = new ArrayList<String>();

            db_utils.get_crud_create_metadata(table_name, keys, sql_col_names, params, generated, dto_class_name,
                    dto_classes);

            // reuse buffer filled earlier
            if (sql_buff.length() == 0) {

                HashMap<String, Object> context = new HashMap<String, Object>();

                context.put("col_names", sql_col_names);

                StringWriter sw = new StringWriter();

                generate_sql("crud_sql_create", context, table_name, sw);

                sql_buff.append(sw.getBuffer());
            }

            String sql_str = Helpers.sql_to_cpp_str(sql_buff);

            HashMap<String, Object> context = new HashMap<String, Object>();

            context.put("method_type", "CREATE");
            context.put("table_name", table_name);
            context.put("class_name", class_name);
            context.put("sql", sql_str);
            context.put("method_name", method_name);
            context.put("params", params);
            context.put("dto_param", get_rendered_dto_class_name(dto_class_name));

            if (fetch_generated && keys.size() > 0) {

                // imports.add("java.util.Map");
                context.put("keys", keys);
                context.put("mode", "dao_create");

            } else {

                // Examples of situations when data table doesn't have
                // auto-increment keys:
                // 1) PK is the name or serial NO
                // 2) PK == FK of 1:1 relation
                // 2) unique PK is assigned by trigger
                context.put("external_sql", false);
                context.put("mode", "dao_exec_dml");
            }

            StringWriter sw = new StringWriter();
            te.merge(context, sw);

            StringBuilder buffer = new StringBuilder();
            buffer.append(sw.getBuffer());

            return buffer;
        }

        private void generate_sql(String mode, HashMap<String, Object> context, String table_name, StringWriter sw) {

            context.put("table_name", table_name);
            context.put("mode", mode);

            te.merge(context, sw);
        }

        @Override
        public StringBuilder render_element_crud_update(StringBuilder sql_buff, String class_name, String method_name,
                String table_name, String dto_class_name, boolean primitive_params) throws Exception {

            StringBuilder buffer = new StringBuilder();

            ArrayList<FieldInfo> keys = new ArrayList<FieldInfo>();

            ArrayList<FieldInfo> params = new ArrayList<FieldInfo>();

            db_utils.get_crud_info(table_name, keys, params, dto_class_name, dto_classes);

            if (keys.isEmpty()) {

                String msg = Helpers.get_no_pk_message(method_name);

                Helpers.build_warning_comment(buffer, msg);

                return buffer;
            }

            if (params.isEmpty()) {

                // if all values of the table are the parts of PK,
                // SQL will be invalid like ''UPDATE term_groups SET WHERE g_id
                // = ? AND t_id = ?'
                // (missing assignments between SET and WHERE)
                String msg = Helpers.get_only_pk_message(method_name);

                Helpers.build_warning_comment(buffer, msg);

                return buffer;
            }

            // reuse buffer filled earlier
            if (sql_buff.length() == 0) {

                HashMap<String, Object> context = new HashMap<String, Object>();
                context.put("params", params);
                context.put("keys", keys);

                StringWriter sw = new StringWriter();

                generate_sql("crud_sql_update", context, table_name, sw);

                sql_buff.append(sw.getBuffer());
            }

            // after rendering of SQL
            for (FieldInfo k : keys) {

                params.add(k);
            }

            String sql_str = Helpers.sql_to_cpp_str(sql_buff);

            HashMap<String, Object> context = new HashMap<String, Object>();

            context.put("class_name", class_name);
            context.put("method_name", method_name);
            context.put("sql", sql_str);
            context.put("method_type", "UPDATE");
            context.put("table_name", table_name);
            context.put("comment", "Updates specified record of the table '" + table_name + "'.");
            context.put("dto_param", primitive_params ? "" : get_rendered_dto_class_name(dto_class_name));
            context.put("params", params);
            context.put("external_sql", false);
            context.put("mode", "dao_exec_dml");

            StringWriter sw = new StringWriter();
            te.merge(context, sw);

            buffer.append(sw.getBuffer());

            return buffer;
        }

        @Override
        public StringBuilder render_element_crud_delete(StringBuilder sql_buff, String class_name, String method_name,
                String table_name, String dto_class_name) throws Exception {

            StringBuilder buffer = new StringBuilder();

            ArrayList<FieldInfo> keys = new ArrayList<FieldInfo>();

            db_utils.get_crud_info(table_name, keys, null, dto_class_name, dto_classes);

            if (keys.isEmpty()) {

                String msg = Helpers.get_no_pk_message(method_name);

                Helpers.build_warning_comment(buffer, msg);

                return buffer;
            }

            // reuse buffer filled earlier
            if (sql_buff.length() == 0) {

                HashMap<String, Object> context = new HashMap<String, Object>();
                context.put("keys", keys);

                StringWriter sw = new StringWriter();

                generate_sql("crud_sql_delete", context, table_name, sw);

                sql_buff.append(sw.getBuffer());
            }

            String sql_str = Helpers.sql_to_cpp_str(sql_buff);

            HashMap<String, Object> context = new HashMap<String, Object>();

            context.put("class_name", class_name);
            context.put("method_name", method_name);
            context.put("sql", sql_str);
            context.put("method_type", "DELETE");
            context.put("table_name", table_name);
            context.put("comment", "Deletes specified record from the table '" + table_name + "'.");

            // delete by PK
            context.put("dto_param", "");

            // context.put("dto_param", primitive_params ? ""
            // : getRenderedDtoClassName(dto_class_name));
            context.put("params", keys);
            context.put("external_sql", false);
            context.put("mode", "dao_exec_dml");

            StringWriter sw = new StringWriter();
            te.merge(context, sw);
            buffer.append(sw.getBuffer());

            return buffer;
        }

        @Override
        public StringBuilder render_element_crud(Object element) throws Exception {

            if (!(element instanceof TypeCrud)) {

                throw new Exception("Unexpected element found in DTO XML file");
            }

            TypeCrud crud = (TypeCrud) element;

            String node_name = Helpers.get_xml_node_name(element);

            String dto_class_name = crud.getDto();

            if (dto_class_name.length() == 0) {

                throw new Exception("<" + node_name + "...\nDTO class is not set");
            }

            String table_attr = crud.getTable();

            if (table_attr == null || table_attr.length() == 0) {

                throw new Exception("<" + node_name + "...\nRequired attribute is not set");
            }

            try {

                db_utils.validate_table_name(table_attr);

                return render_element_crud(crud, dto_class_name, table_attr);

            } catch (Throwable e) {

                // e.printStackTrace();
                String msg = "<" + node_name + " dto=\"" + dto_class_name + "\" table=\"" + table_attr + "\"...\n";

                throw new Exception(Helpers.get_error_message(msg, e));
            }
        }

        @Override
        public DbUtils get_db_utils() {

            return db_utils;
        }

        private StringBuilder render_element_crud(TypeCrud element, String dto_class_name, String table_attr)
                throws Exception {

            process_dto_class_name(dto_class_name);

            StringBuilder code_buff = CodeGeneratorHelpers.process_element_crud(this, true, element, dto_class_name, table_attr);

            return code_buff;
        }
    }
}
