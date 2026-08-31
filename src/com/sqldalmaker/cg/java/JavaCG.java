/*
    Copyright 2011-2026 sqldalmaker@gmail.com
    SQL DAL Maker Website: https://sqldalmaker.sourceforge.net/
    Read LICENSE.txt in the root of this project/archive for details.
 */
package com.sqldalmaker.cg.java;

import com.sqldalmaker.cg.*;
import com.sqldalmaker.jaxb.sdm.*;
import com.sqldalmaker.jaxb.settings.Settings;

import java.io.StringWriter;
import java.sql.Connection;
import java.util.*;

/*
 * @author sqldalmaker@gmail.com
 *
 * 30.08.2026 14:00 1.331 Claude refactor
 * 16.12.2023 09:01 1.292 sdm.xml
 * 29.09.2023 09:58 1.289
 * 27.03.2023 10:03 optional "<crud table"
 * 19.01.2023 20:57 1.276
 * 16.11.2022 08:02 1.269
 * 25.10.2022 09:26 crud pk --> dto-class pk
 * 25.10.2022 03:46 - crud generated; + dao-class auto
 * 06.08.2022 08:37 1.261 no 'crud-auto' anymore, just empty 'crud' instead
 * 09.07.2022 23:10  + dto macro + dao macro
 * 27.05.2022 01:17 1.246
 * 21.04.2022 17:15 1.225 fixes for python and go
 * 10.05.2021 21:46 new XML attr "field-comment"
 * 01.05.2021 22:33 JSON and XML comments for Go
 * 22.03.2021 21:19 TitleCase for method names
 * 16.09.2020 02:23 reduced amount of warnings in java
 * 07.01.2020 22:19 Now you can declare in XML calculated DTO fields.
 * 03.09.2019 15:55 minor code refactoring
 * 07.02.2019 19:50 initial commit
 */
public class JavaCG {

    public static class DTO implements IDtoCG {

        private final String dto_package;
        private final String sql_root_abs_path;
        private final String dto_inheritance;
        private final List<DtoClass> jaxb_dto_classes;
        private final TemplateEngine te;
        private final JdbcUtils db_utils;

        public DTO(
                Sdm sdm,
                Settings jaxb_settings,
                Connection connection,
                String dto_package,
                String sql_root_abs_path,
                String dto_inheritance,
                FieldNamesMode field_names_mode,
                String vm_template) throws Exception {

            this.jaxb_dto_classes = sdm.getDtoClass();
            this.dto_package = dto_package;
            this.sql_root_abs_path = sql_root_abs_path;
            this.dto_inheritance = dto_inheritance;
            te = Helpers.create_template_engine(vm_template, "java", "java");
            db_utils = new JdbcUtils(connection, field_names_mode, FieldNamesMode.AS_IS, jaxb_settings, sql_root_abs_path);
        }

        @Override
        public String[] translate(String dto_class_name) throws Exception {
            DtoClass jaxb_dto_class = JaxbUtils.find_jaxb_dto_class(dto_class_name, jaxb_dto_classes);
            List<FieldInfo> fields = db_utils.get_dto_fields(jaxb_dto_class, sql_root_abs_path);
            Map<String, Object> context = new HashMap<String, Object>();
            String header = jaxb_dto_class.getHeader();
            context.put("header", header);
            context.put("package", dto_package);
            context.put("class_name", dto_class_name);
            context.put("ref", jaxb_dto_class.getRef());
            context.put("implements", dto_inheritance);
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
        private final List<DtoClass> jaxb_dto_classes;
        private final TemplateEngine te;
        private final JdbcUtils db_utils;

        private final String dto_package;
        private final String dao_package;
        private final Set<String> imports = new HashSet<String>();

        public DAO(
                List<DtoClass> jaxb_dto_classes,
                Settings jaxb_settings,
                Connection connection,
                String dto_package,
                String dao_package,
                String sql_root_abs_path,
                FieldNamesMode field_names_mode,
                String vm_template) throws Exception {

            this.jaxb_dto_classes = jaxb_dto_classes;
            this.sql_root_abs_path = sql_root_abs_path;
            this.te = Helpers.create_template_engine(vm_template, "java", "java");
            this.db_utils = new JdbcUtils(connection, field_names_mode, FieldNamesMode.AS_IS, jaxb_settings, sql_root_abs_path);
            this.dto_package = dto_package;
            this.dao_package = dao_package;
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
                String jaxb_dto_or_return_type,
                boolean jaxb_return_type_is_dto,
                boolean fetch_list,
                String method_name,
                String crud_table,
                List<FieldInfo> fields,
                List<FieldInfo> params) throws Exception {

            if (dao_query_jdbc_sql == null) {
                return Helpers.get_no_pk_warning(method_name);
            }
            String returned_type_name;
            if (fetch_list) {
                imports.add("java.util.List");
            }
            if (jaxb_return_type_is_dto) {
                if (fetch_list) {
                    imports.add("java.util.ArrayList");
                }
                returned_type_name = _get_rendered_dto_class_name(jaxb_dto_or_return_type);
            } else {
                if (fields.isEmpty()) {
                    returned_type_name = "?";
                } else {
                    FieldInfo fi = fields.get(0);
                    String curr_type = fi.getType();
                    returned_type_name = this.db_utils.get_target_type_by_type_map(curr_type);
                }
            }
            String java_sql_str = SqlUtils.format_jdbc_sql_for_java(dao_query_jdbc_sql);
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
            _assign_params(params, context);
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

        private void _process_dto_class_name(String dto_class_name) {
            if (dto_package != null && !dto_package.isEmpty()) {
                imports.add(dto_package + "." + dto_class_name);
            } else {
                imports.add(dto_class_name);
            }
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
            String java_sql = SqlUtils.format_jdbc_sql_for_java(jdbc_dao_sql);
            List<MappingInfo> m_list = new ArrayList<MappingInfo>();
            List<FieldInfo> method_params = new ArrayList<FieldInfo>();
            List<FieldInfo> exec_dml_params = new ArrayList<FieldInfo>();
            for (ExecDmlParamSlot slot : ExecDmlParams.parse(param_descriptors)) {
                if (slot.kind == ExecDmlParamSlot.Kind.CURSOR_ARRAY) {
                    List<String> cb_elements = new ArrayList<String>();
                    for (String[] mapping : slot.cursor_mappings) {
                        MappingInfo m = _create_mapping(mapping);
                        m_list.add(m);
                        method_params.add(FieldInfo.parameter(FieldNamesMode.AS_IS, String.format("final RecordHandler<%s>", m.dto_class_name), m.method_param_name));
                        cb_elements.add(m.exec_dml_param_name);
                    }
                    String exec_xml_param = "new RowHandler[]{" + String.join(", ", cb_elements) + "}";
                    if (slot.index == 0) {
                        exec_xml_param = "(Object) " + exec_xml_param;
                    }
                    exec_dml_params.add(FieldInfo.parameter(FieldNamesMode.AS_IS, "[]", exec_xml_param));
                } else if (slot.kind == ExecDmlParamSlot.Kind.MAPPED) {
                    FieldInfo p = _params.get(slot.index);
                    MappingInfo m = _create_mapping(slot.mapping);
                    m_list.add(m);
                    method_params.add(FieldInfo.parameter(FieldNamesMode.AS_IS, String.format("final RecordHandler<%s>", m.dto_class_name), m.method_param_name));
                    String target_type_name = this.db_utils.get_target_type_by_type_map(p.getType());
                    exec_dml_params.add(FieldInfo.parameter(FieldNamesMode.AS_IS, target_type_name, m.exec_dml_param_name));
                } else {
                    FieldInfo p = _params.get(slot.index);
                    method_params.add(p);
                    exec_dml_params.add(p);
                }
            }
            Map<String, Object> context = new HashMap<String, Object>();
            _assign_params(method_params, context);
            context.put("params2", exec_dml_params);
            context.put("mappings", m_list);
            context.put("plain_params", true);
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

        private MappingInfo _create_mapping(String[] parts) throws Exception {
            MappingInfo m = new MappingInfo();
            m.method_param_name = parts[0].trim();
            String cb_param_name = String.format("_map_cb_%s", m.method_param_name);
            m.exec_dml_param_name = cb_param_name;
            m.dto_class_name = parts[1].trim();
            DtoClass jaxb_dto_class = JaxbUtils.find_jaxb_dto_class(m.dto_class_name, jaxb_dto_classes);
            imports.add("com.sqldalmaker.DataStore.RowData");
            imports.add("com.sqldalmaker.DataStore.RowHandler");
            imports.add("com.sqldalmaker.DataStore.RecordHandler");
            _process_dto_class_name(jaxb_dto_class.getName()); // extends imports
            List<FieldInfo> fields = db_utils.get_dto_fields(jaxb_dto_class, sql_root_abs_path);
            if (!fields.isEmpty()) {
                fields.get(0).setComment(fields.get(0).getComment() + " [INFO] REF CURSOR");
            }
            m.fields.addAll(fields);
            return m;
        }

        private void _assign_params(List<FieldInfo> params, Map<String, Object> context) {
            context.put("dto_param", "");
            context.put("params", params);
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
            String java_sql_str = SqlUtils.format_jdbc_sql_for_java(dao_jdbc_sql);
            Map<String, Object> context = new HashMap<String, Object>();
            context.put("method_type", "CREATE");
            context.put("table_name", table_name);
            context.put("sql", java_sql_str);
            context.put("method_name", method_name);
            context.put("params", fields_not_ai);
            context.put("dto_param", _get_rendered_dto_class_name(dto_class_name));
            if (fetch_generated && !fields_ai.isEmpty()) {
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
            return _render_query(dao_jdbc_sql, false, dto_class_name, true, fetch_list,
                    method_name, dao_table_name, fields_all, fields_pk);
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
            String java_sql_str = SqlUtils.format_jdbc_sql_for_java(dao_jdbc_sql);
            fields_not_pk.addAll(fields_pk);
            Map<String, Object> context = new HashMap<String, Object>();
            context.put("mode", "dao_exec_dml");
            context.put("plain_params", true);
            context.put("method_name", method_name);
            context.put("sql", java_sql_str);
            context.put("method_type", "UPDATE");
            context.put("table_name", table_name);
            context.put("dto_param", scalar_params ? "" : _get_rendered_dto_class_name(dto_class_name));
            context.put("params", fields_not_pk);
            context.put("is_external_sql", false);
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
            String java_sql_str = SqlUtils.format_jdbc_sql_for_java(dao_jdbc_sql);
            Map<String, Object> context = new HashMap<String, Object>();
            context.put("mode", "dao_exec_dml");
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
    }
}
