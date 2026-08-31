/*
    Copyright 2011-2026 sqldalmaker@gmail.com
    SQL DAL Maker Website: https://sqldalmaker.sourceforge.net/
    Read LICENSE.txt in the root of this project/archive for details.
 */
package com.sqldalmaker.cg.python;

import com.sqldalmaker.cg.*;
import com.sqldalmaker.jaxb.sdm.*;
import com.sqldalmaker.jaxb.sdm.DtoClass;
import com.sqldalmaker.jaxb.settings.Settings;

import java.io.StringWriter;
import java.sql.Connection;
import java.util.*;

/*
 * @author sqldalmaker@gmail.com
 *
 * 30.08.2026 14:00 1.331 Claude refactor
 */
public class PythonCG {

    public static class DTO implements IDtoCG {

        private final String sql_root_abs_path;
        private final List<DtoClass> jaxb_dto_classes;
        private final TemplateEngine te;
        private final JdbcUtils db_utils;

        public DTO(
                Sdm sdm,
                Settings jaxb_settings,
                Connection conn,
                String sql_root_abs_path,
                String vm_template) throws Exception {

            this.jaxb_dto_classes = sdm.getDtoClass();
            this.sql_root_abs_path = sql_root_abs_path;
            te = Helpers.create_template_engine(vm_template, "python", "python");
            db_utils = new JdbcUtils(conn, FieldNamesMode.SNAKE_CASE, FieldNamesMode.SNAKE_CASE, jaxb_settings, sql_root_abs_path);
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
            String ref = jaxb_dto_class.getRef();
            if (SqlUtils.is_table_ref(ref)) {
                context.put("tablename", ref);
            } else if (!SqlUtils.is_empty_ref(ref)) {
                try {
                    String sql = SqlUtils.jdbc_sql_by_dto_class_ref(ref, sql_root_abs_path);
                    String python_sql_str = SqlUtils.jdbc_sql_to_python_string(sql);
                    context.put("sql", python_sql_str);
                } catch (Exception ex) {
                    System.err.println(ex.getMessage());
                }
            }
            String header = jaxb_dto_class.getHeader();
            context.put("header", header);
            context.put("ref", jaxb_dto_class.getRef());
            context.put("class_name", dto_class_name);
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

        public static class ImportItem {
            public String file_name;
            public String class_name;

            public ImportItem(String file_name, String class_name) {
                this.file_name = file_name;
                this.class_name = class_name;
            }

            public String getFileName() {
                return this.file_name;
            }

            public String getClassName() {
                return this.class_name;
            }
        }

        private final Map<String, ImportItem> imports = new HashMap<String, ImportItem>();

        public DAO(
                String dto_package,
                List<DtoClass> jaxb_dto_classes,
                Settings jaxb_settings,
                Connection conn,
                String sql_root_abs_path,
                String vm_template) throws Exception {

            this.jaxb_dto_classes = jaxb_dto_classes;
            this.sql_root_abs_path = sql_root_abs_path;
            this.te = Helpers.create_template_engine(vm_template, "python", "python");
            this.db_utils = new JdbcUtils(conn, FieldNamesMode.SNAKE_CASE, FieldNamesMode.SNAKE_CASE,
                    jaxb_settings, sql_root_abs_path);
            this.dto_package = dto_package;
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
                String m = methods.get(i).replace("\t", "    ").replace("//", "#");
                methods.set(i, m);
            }
            Map<String, Object> context = new HashMap<String, Object>();
            List<ImportItem> imp = new ArrayList<>(imports.values());
            imp.sort(new Comparator<ImportItem>() {
                @Override
                public int compare(ImportItem o1, ImportItem o2) {
                    // file_name includes package
                    return o1.file_name.compareTo(o2.file_name);
                }
            });
            context.put("imports", imp);
            context.put("class_name", dao_class_name);
            context.put("methods", methods);
            context.put("mode", "dao_class");
            StringWriter sw = new StringWriter();
            te.merge(context, sw);
            String text = sw.toString();
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
            // 'out_params' is a Python-only extension of the method declaration
            String[] param_descriptors = mi.param_descriptors;
            boolean out_params = param_descriptors.length > 0
                    && "out_params".equals(param_descriptors[param_descriptors.length - 1]);
            if (out_params) {
                param_descriptors = Arrays.copyOf(param_descriptors, param_descriptors.length - 1);
            }
            JdbcUtils.DaoSqlInfo _q = db_utils.get_dao_query_info(sql_root_abs_path, mi.ref, "",
                    param_descriptors, mi.dto_or_return_type, mi.return_type_is_dto, jaxb_dto_classes);
            return _render_query(_q.sql, mi.external_sql, mi.dto_or_return_type, mi.return_type_is_dto,
                    mi.fetch_list, mi.method_name, null, _q.fields, _q.params, out_params);
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
                List<FieldInfo> fields_all,
                List<FieldInfo> fields_pk,
                boolean out_params) throws Exception {

            if (dao_query_jdbc_sql == null) {
                return Helpers.get_no_pk_warning(method_name);
            }
            String returned_type_name;
            if (jaxb_return_type_is_dto) {
                returned_type_name = _get_rendered_dto_class_name(jaxb_dto_or_return_type, fetch_list); // import is needed only for read list
            } else {
                if (fields_all.isEmpty()) {
                    returned_type_name = "?";
                } else {
                    FieldInfo fi = fields_all.get(0);
                    String curr_type = fi.getType();
                    returned_type_name = this.db_utils.get_target_type_by_type_map(curr_type);
                }
            }
            String python_sql_str = SqlUtils.jdbc_sql_to_python_string(dao_query_jdbc_sql);
            Map<String, Object> context = new HashMap<String, Object>();
            context.put("mode", "dao_query");
            context.put("fields", fields_all);
            context.put("method_name", method_name);
            if (crud_table == null) {
                crud_table = "";
                context.put("method_type", "");
            } else {
                context.put("method_type", "READ");
            }
            context.put("ref", crud_table);
            context.put("sql", python_sql_str);
            String model = "";
            if (jaxb_return_type_is_dto) {
                model = _get_model(jaxb_dto_or_return_type);
                if (!model.isEmpty()) {
                    _get_rendered_dto_class_name(jaxb_dto_or_return_type, true); // add to import
                }
            }
            context.put("model", model);
            context.put("use_dto", jaxb_return_type_is_dto);
            context.put("returned_type_name", returned_type_name);
            context.put("fetch_list", fetch_list);
            context.put("imports", imports.values());
            context.put("is_external_sql", is_external_sql);
            context.put("out_params", out_params);
            _assign_params(fields_pk, context);
            StringWriter sw = new StringWriter();
            te.merge(context, sw);
            StringBuilder buff = new StringBuilder();
            buff.append(sw.getBuffer());
            return buff;
        }

        private String _get_rendered_dto_class_name(String dto_class_name, boolean add_to_import) throws Exception {
            DtoClass jaxb_dto_class = JaxbUtils.find_jaxb_dto_class(dto_class_name, jaxb_dto_classes);
            String dto_class_nm = jaxb_dto_class.getName();
            int model_end = dto_class_nm.indexOf('-');
            if (model_end != -1) {
                dto_class_nm = dto_class_nm.substring(model_end + 1);
            }
            if (add_to_import) {
                String python_fn = Names.camel_case_to_lower_snake_case(dto_class_nm);
                if (dto_package != null && !dto_package.isEmpty()) {
                    python_fn = dto_package + "." + python_fn;
                }
                ImportItem item = new ImportItem(python_fn, dto_class_nm);
                imports.put(dto_class_nm, item);
            }
            return dto_class_nm;
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
            List<MappingInfo> m_list = new ArrayList<MappingInfo>();
            List<FieldInfo> method_params = new ArrayList<FieldInfo>();
            List<FieldInfo> exec_dml_params = new ArrayList<FieldInfo>();
            for (ExecDmlParamSlot slot : ExecDmlParams.parse(param_descriptors)) {
                if (slot.kind == ExecDmlParamSlot.Kind.CURSOR_ARRAY) {
                    List<String> cb_elements = new ArrayList<String>();
                    for (String[] mapping : slot.cursor_mappings) {
                        MappingInfo m = _create_ref_cursor_mapping(mapping);
                        m_list.add(m);
                        method_params.add(FieldInfo.parameter(FieldNamesMode.SNAKE_CASE, "callable", m.method_param_name));
                        cb_elements.add(m.exec_dml_param_name);
                    }
                    String exec_xml_param = "[" + String.join(", ", cb_elements) + "]";
                    exec_dml_params.add(FieldInfo.parameter(FieldNamesMode.AS_IS, "[]", exec_xml_param));
                } else if (slot.kind == ExecDmlParamSlot.Kind.MAPPED) {
                    _params.get(slot.index); // as before: the position must exist among the SQL parameters
                    MappingInfo m = _create_ref_cursor_mapping(slot.mapping);
                    m_list.add(m);
                    method_params.add(FieldInfo.parameter(FieldNamesMode.SNAKE_CASE, "callable", m.method_param_name));
                    exec_dml_params.add(FieldInfo.parameter(FieldNamesMode.SNAKE_CASE, "callable", m.exec_dml_param_name));
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
            context.put("method_name", method_name);
            String sql_str = SqlUtils.jdbc_sql_to_python_string(jdbc_dao_sql);
            context.put("sql", sql_str);
            context.put("xml_node_name", xml_node_name);
            context.put("sql_path", sql_path);
            context.put("is_external_sql", is_external_sql);
            context.put("mode", "dao_exec_dml");
            context.put("model", "");
            StringWriter sw = new StringWriter();
            te.merge(context, sw);
            buffer.append(sw.getBuffer());
        }

        private MappingInfo _create_ref_cursor_mapping(String[] parts) throws Exception {
            String dto_class_name_with_model = parts[1].trim();
            DtoClass jaxb_dto_class = JaxbUtils.find_jaxb_dto_class(dto_class_name_with_model, jaxb_dto_classes);
            MappingInfo m = new MappingInfo();
            m.method_param_name = parts[0].trim();
            String cb_param_name = String.format("_map_cb_%s", m.method_param_name);
            m.exec_dml_param_name = cb_param_name;
            m.dto_class_name = _get_rendered_dto_class_name(dto_class_name_with_model, true); // extends imports;
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

        private static String _get_model(String dto_class_name) {
            String model = "";
            int model_name_end_index = dto_class_name.indexOf('-');
            if (model_name_end_index != -1) {
                model = dto_class_name.substring(0, model_name_end_index);
            }
            return model;
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
            String sql_str = SqlUtils.jdbc_sql_to_python_string(dao_jdbc_sql);
            Map<String, Object> context = new HashMap<String, Object>();
            context.put("method_type", "CREATE");
            context.put("table_name", table_name);
            context.put("sql", sql_str);
            context.put("method_name", method_name);
            context.put("params", fields_not_ai);
            String model = _get_model(dto_class_name);
            context.put("model", model);
            dto_class_name = _get_rendered_dto_class_name(dto_class_name, !model.isEmpty()); // "false" because it is only for comments
            context.put("dto_param", dto_class_name);
            if (fetch_generated && !fields_ai.isEmpty()) {
                context.put("keys", fields_ai);
                context.put("mode", "dao_create");
            } else {
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
                    method_name, dao_table_name, fields_all, fields_pk, false);
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
            List<FieldInfo> fi_updated = _c.fields;
            List<FieldInfo> fi_pk = _c.params;
            if (fi_pk.isEmpty()) {
                return Helpers.get_no_pk_warning(method_name);
            }
            if (fi_updated.isEmpty()) {
                return Helpers.get_only_pk_warning(method_name);
            }
            String sql_str = SqlUtils.jdbc_sql_to_python_string(dao_jdbc_sql);
            fi_updated.addAll(fi_pk);
            Map<String, Object> context = new HashMap<String, Object>();
            context.put("mode", "dao_exec_dml");
            context.put("method_type", "UPDATE");
            context.put("method_name", method_name);
            context.put("sql", sql_str);
            context.put("table_name", table_name);
            String model = _get_model(dto_class_name);
            context.put("model", model);
            dto_class_name = _get_rendered_dto_class_name(dto_class_name, false); // "false" because it is only for comments
            context.put("dto_class_name", dto_class_name);
            String dto_param = scalar_params || !model.isEmpty() ? "" : dto_class_name;
            context.put("dto_param", dto_param);
            if (!model.isEmpty()) {
                context.put("params", fi_pk);
            } else {
                context.put("params", fi_updated);
            }
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
            String python_sql_str = SqlUtils.jdbc_sql_to_python_string(dao_jdbc_sql);
            Map<String, Object> context = new HashMap<String, Object>();
            context.put("mode", "dao_exec_dml");
            String model = _get_model(dto_class_name);
            context.put("model", model);
            if (!model.isEmpty()) {
                dto_class_name = _get_rendered_dto_class_name(dto_class_name, true);
                context.put("dto_class_name", dto_class_name);
            }
            context.put("method_name", method_name);
            context.put("sql", python_sql_str);
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
