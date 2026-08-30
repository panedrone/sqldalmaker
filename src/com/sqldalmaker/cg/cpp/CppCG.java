/*
    Copyright 2011-2026 sqldalmaker@gmail.com
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
            te = Helpers.create_template_engine(vm_template, "cpp", "cpp");
            db_utils = new JdbcUtils(connection, FieldNamesMode.AS_IS, FieldNamesMode.AS_IS, jaxb_settings, sql_root_abs_path);
        }

        @Override
        public String[] translate(String dto_class_base_name) throws Exception {
            DtoClass jaxb_dto_class = JaxbUtils.find_jaxb_dto_class(dto_class_base_name, jaxb_dto_classes);
            List<FieldInfo> fields = db_utils.get_dto_fields(jaxb_dto_class, sql_root_abs_path);
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
        private final List<DtoClass> jaxb_dto_classes;
        private final TemplateEngine te;
        private final JdbcUtils db_utils;

        private final String class_prefix;
        private final Set<String> imports = new HashSet<String>();

        public DAO(
                List<DtoClass> jaxb_dto_classes,
                Settings jaxb_settings,
                Connection connection,
                String sql_root_abs_path,
                String class_prefix,
                String vm_template) throws Exception {

            this.jaxb_dto_classes = jaxb_dto_classes;
            this.sql_root_abs_path = sql_root_abs_path;
            this.te = Helpers.create_template_engine(vm_template, "cpp", "cpp");
            this.db_utils = new JdbcUtils(connection, FieldNamesMode.AS_IS, FieldNamesMode.AS_IS, jaxb_settings, sql_root_abs_path);
            this.class_prefix = class_prefix;
        }

        @Override
        public String[] translate(DaoClass dao_class) throws Exception {
            imports.clear();
            imports.add("DataStore.h");
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

        //////////////////////////////////////////////////////////////////
        //
        // this method is called from both 'render_jaxb_query' and 'render_crud_read'
        //
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

        private void _render_exec_dml(
                StringBuilder buffer,
                String dao_jdbc_sql,
                boolean is_external_sql,
                String method_name,
                String[] param_descriptors,
                String xml_node_name,
                String sql_path) throws Exception {

            List<FieldInfo> params = db_utils.get_dao_exec_dml_params(dao_jdbc_sql, "", param_descriptors);
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
            String sql_str = SqlUtils.jdbc_sql_to_cpp_str(dao_jdbc_sql);
            Map<String, Object> context = new HashMap<String, Object>();
            context.put("method_type", "CREATE");
            context.put("table_name", table_name);
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
            List<FieldInfo> updated_fields = _c.fields;
            List<FieldInfo> fields_pk = _c.params;
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
            String cpp_sql_str = SqlUtils.jdbc_sql_to_cpp_str(dao_jdbc_sql);
            Map<String, Object> context = new HashMap<String, Object>();
            context.put("mode", "dao_exec_dml");
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
    }
}
