/*
 * Copyright 2011-2020 sqldalmaker@gmail.com
 * SQL DAL Maker Website: http://sqldalmaker.sourceforge.net
 * Read LICENSE.txt in the root of this project/archive for details.
 * 
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

		private final List<DtoClass> jaxb_dto_classes;

		private final TemplateEngine te;

		private final DbUtils db_utils;

		private final String dto_class_prefix;

		public DTO(DtoClasses jaxb_dto_classes, TypeMap type_map, Connection connection, String sql_root_abs_path,
				String dto_class_prefix, String vm_file_system_dir) throws Exception {

			this.jaxb_dto_classes = jaxb_dto_classes.getDtoClass();

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

			DtoClass jaxb_dto_class = null;

			for (DtoClass cls : jaxb_dto_classes) {

				if (cls.getName().equals(dto_class_base_name)) {

					jaxb_dto_class = cls;

					break;
				}
			}

			if (jaxb_dto_class == null) {

				throw new Exception("XML element of DTO class '" + dto_class_base_name + "' not found");
			}

			String jdbc_sql = SqlUtils.jdbc_sql_by_query_ref(jaxb_dto_class.getRef(), sql_root_abs_path);

			List<FieldInfo> fields = new ArrayList<FieldInfo>();

			db_utils.get_dto_field_info(jdbc_sql, jaxb_dto_class, fields);

			Map<String, Object> context = new HashMap<String, Object>();

			context.put("class_name", dto_class_prefix + dto_class_base_name);
			context.put("fields", fields);
			context.put("mode", "dto_class");

			StringWriter sw = new StringWriter();
			te.merge(context, sw);
			String text = sw.toString();

			return new String[] { text };
		}
	}

	public static class DAO implements IDaoCG {

		private final String sql_root_abs_path;

		private final String class_prefix;

		private final DtoClasses jaxb_dto_classes;

		private final TypeMap type_map;

		private final Set<String> imports = new HashSet<String>();

		private final TemplateEngine te;

		private final DbUtils db_utils;

		public DAO(DtoClasses jaxb_dto_classes, TypeMap type_map, Connection connection, String sql_root_abs_path,
				String class_prefix, String vm_file_system_dir) throws Exception {

			this.jaxb_dto_classes = jaxb_dto_classes;

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

			JaxbProcessor.process_jaxb_dao_class(this, dao_class, methods);

			Map<String, Object> context = new HashMap<String, Object>();

			String[] imports_arr = imports.toArray(new String[imports.size()]);
			Arrays.sort(imports_arr);

			context.put("imports", imports_arr);
			context.put("class_name", class_prefix + dao_class_name);
			context.put("methods", methods);
			context.put("mode", "dao_class");

			StringWriter sw = new StringWriter();
			te.merge(context, sw);
			String text = sw.toString();

			return new String[] { text };
		}

		@Override
		public StringBuilder render_jaxb_query(Object jaxb_element) throws Exception {

			QueryMethodInfo mi = new QueryMethodInfo(jaxb_element);

			String xml_node_name = JaxbProcessor.get_jaxb_node_name(jaxb_element);

			check_required_attr(xml_node_name, mi.jaxb_method);

			try {

				if (mi.return_type_is_dto) {

					process_dto_class_name(mi.jaxb_dto_or_return_type);
				}

				String dao_jdbc_sql = SqlUtils.jdbc_sql_by_query_ref(mi.jaxb_ref, sql_root_abs_path);

				String[] parsed = parse_method_declaration(mi.jaxb_method);

				String method_name = parsed[0];
				String dto_param_type = parsed[1];
				String param_descriptors = parsed[2];

				String[] method_param_descriptors = Helpers.get_listed_items(param_descriptors);

				StringBuilder buff = new StringBuilder();

				render_query(buff, dao_jdbc_sql, mi.jaxb_ref, mi.jaxb_is_external_sql,
						mi.jaxb_dto_or_return_type, mi.return_type_is_dto, mi.fetch_list, method_name, dto_param_type,
						method_param_descriptors, false, xml_node_name);

				return buff;

			} catch (Throwable e) {

				// e.printStackTrace();
				String msg = "<" + xml_node_name + " method=\"" + mi.jaxb_method + "\" ref=\"" + mi.jaxb_ref
						+ "\"...\n";

				throw new Exception(Helpers.get_error_message(msg, e));
			}
		}

		//////////////////////////////////////////////////////////////////
		//
		// this method is called from 'query...' and 'crud(-auto)->read'
		//
		private void render_query(StringBuilder buff, String jdbc_dao_sql, String ref, boolean is_external_sql,
				String jaxb_dto_or_return_type, boolean jaxb_return_type_is_dto, boolean fetch_list, String method_name,
				String dto_param_type, String[] param_descriptors, boolean crud, String xml_node_name)
				throws Exception {

			List<FieldInfo> fields = new ArrayList<FieldInfo>();
			List<FieldInfo> params = new ArrayList<FieldInfo>();

			db_utils.get_jdbc_sql_info(sql_root_abs_path, jdbc_dao_sql, fields, dto_param_type, param_descriptors,
					params, jaxb_dto_or_return_type, jaxb_return_type_is_dto, jaxb_dto_classes);

			String returned_type_name;

			if (jaxb_return_type_is_dto) {

				returned_type_name = get_rendered_dto_class_name(jaxb_dto_or_return_type);

			} else {

				returned_type_name = fields.get(0).getType();

				returned_type_name = Helpers.get_cpp_class_name_from_java_class_name(type_map, returned_type_name);
			}

			String sql_str = SqlUtils.sql_to_cpp_str(jdbc_dao_sql);

			Map<String, Object> context = new HashMap<String, Object>();

			context.put("mode", "dao_query");

			context.put("fields", fields);
			context.put("method_name", method_name);
			context.put("crud", crud);
			context.put("xml_node_name", xml_node_name);
			context.put("ref", ref);
			context.put("sql", sql_str);
			context.put("return_type_is_dto", jaxb_return_type_is_dto);
			context.put("returned_type_name", returned_type_name);
			context.put("fetch_list", fetch_list);
			context.put("imports", imports);
			context.put("external_sql", is_external_sql);

			assign_params(params, dto_param_type, context);

			StringWriter sw = new StringWriter();
			te.merge(context, sw);
			buff.append(sw.getBuffer());
		}

		private String get_rendered_dto_class_name(String dto_class_name) throws Exception {

			DtoClass jaxb_dto_class = JaxbProcessor.find_jaxb_dto_class(dto_class_name, jaxb_dto_classes);

			if (jaxb_dto_class != null) {

				return class_prefix + jaxb_dto_class.getName();

			} else {

				throw new Exception("Element not found: " + dto_class_name);
			}
		}

		private void process_dto_class_name(String dto_class_name) throws Exception {

			DtoClass jaxb_dto_class = JaxbProcessor.find_jaxb_dto_class(dto_class_name, jaxb_dto_classes);

			if (jaxb_dto_class != null) {

				imports.add(jaxb_dto_class.getName() + ".h");
			}
		}

		@Override
		public StringBuilder render_jaxb_exec_dml(ExecDml jaxb_exec_dml) throws Exception {

			String method = jaxb_exec_dml.getMethod();
			String ref = jaxb_exec_dml.getRef();

			String xml_node_name = JaxbProcessor.get_jaxb_node_name(jaxb_exec_dml);

			check_required_attr(xml_node_name, method);

			try {

				String dao_jdbc_sql = SqlUtils.jdbc_sql_by_exec_dml_ref(ref, sql_root_abs_path);

				String[] parsed = parse_method_declaration(method);

				String method_name = parsed[0]; // never is null
				String dto_param_type = parsed[1]; // never is null
				String param_descriptors = parsed[2]; // never is null

				String[] method_param_descriptors = Helpers.get_listed_items(param_descriptors);

				boolean is_external_sql = jaxb_exec_dml.isExternalSql();

				StringBuilder buff = new StringBuilder();

				render_element_exec_dml(buff, dao_jdbc_sql, is_external_sql, null, method_name, dto_param_type,
						method_param_descriptors, xml_node_name, ref);

				return buff;

			} catch (Throwable e) {

				// e.printStackTrace();
				String msg = "<" + xml_node_name + " method=\"" + method + "\" ref=\"" + ref + "\"...\n";

				throw new Exception(Helpers.get_error_message(msg, e));
			}
		}

		private void render_element_exec_dml(StringBuilder buffer, String dao_jdbc_sql, boolean is_external_sql,
				String class_name, String method_name, String dto_param_type, String[] param_descriptors,
				String xml_node_name, String sql_path) throws Exception {

			SqlUtils.throw_if_select_sql(dao_jdbc_sql);

			List<FieldInfo> params = new ArrayList<FieldInfo>();

			db_utils.get_exec_dml_jdbc_sql_info(dao_jdbc_sql, dto_param_type, param_descriptors, params);

			String sql_str = SqlUtils.sql_to_cpp_str(dao_jdbc_sql);

			Map<String, Object> context = new HashMap<String, Object>();

			assign_params(params, dto_param_type, context);

			context.put("dto_param", dto_param_type);
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

		private void assign_params(List<FieldInfo> params, String dto_param_type, Map<String, Object> context)
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

			return new String[] { method_name, dto_param_type, param_descriptors };
		}

		private static void check_required_attr(String node_name, String method_name_attr) throws Exception {

			if (method_name_attr == null || method_name_attr.length() == 0) {

				throw new Exception("<" + node_name + "...\n'method' is not set.");
			}
		}

		private void generate_sql(String mode, Map<String, Object> context, String table_name, StringWriter sw) {

			context.put("table_name", table_name);
			context.put("mode", mode);

			te.merge(context, sw);
		}

		@Override
		public StringBuilder render_crud_create(String class_name, String method_name, String table_name,
				String dto_class_name, boolean fetch_generated, String generated) throws Exception {

			List<FieldInfo> ai_fields = new ArrayList<FieldInfo>();
			List<FieldInfo> not_ai_fields = new ArrayList<FieldInfo>();

			db_utils.get_crud_create_info(table_name, ai_fields, not_ai_fields, generated, dto_class_name,
					jaxb_dto_classes);

			String sql_str;
			{
				Map<String, Object> context = new HashMap<String, Object>();

				List<String> sql_col_names = new ArrayList<String>();

				for (FieldInfo fi : not_ai_fields) {
					sql_col_names.add(fi.getColumnName()); // DB column name
				}

				context.put("col_names", sql_col_names);

				StringWriter sw = new StringWriter();

				generate_sql("crud_sql_create", context, table_name, sw);

				StringBuilder jdbc_sql_buff = new StringBuilder();
				jdbc_sql_buff.append(sw.getBuffer());
				db_utils.validate_jdbc_sql(jdbc_sql_buff);
				sql_str = SqlUtils.sql_to_cpp_str(jdbc_sql_buff);
			}

			Map<String, Object> context = new HashMap<String, Object>();

			context.put("method_type", "CREATE");
			context.put("table_name", table_name);
			context.put("class_name", class_name);
			context.put("sql", sql_str);
			context.put("method_name", method_name);
			context.put("params", not_ai_fields);
			context.put("dto_param", get_rendered_dto_class_name(dto_class_name));

			if (fetch_generated && ai_fields.size() > 0) {

				// imports.add("java.util.Map");
				context.put("keys", ai_fields);
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

		@Override
		public StringBuilder render_crud_read(String method_name, String table_name, String ret_dto_type,
				boolean fetch_list) throws Exception {

			StringBuilder buffer = new StringBuilder();

			List<FieldInfo> keys = new ArrayList<FieldInfo>();

			if (!fetch_list) {

				// !!!! - type_map == null to return Java types: render_element_query
				// below translates them to C++ types
				db_utils.get_crud_info(table_name, keys, null, ret_dto_type, jaxb_dto_classes, null);

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

			Map<String, Object> context = new HashMap<String, Object>();

			context.put("keys", keys);

			String mode = fetch_list ? "crud_sql_read_all" : "crud_sql_read_single";

			StringWriter sw = new StringWriter();
			generate_sql(mode, context, table_name, sw);
			StringBuilder jdbc_sql_buff = new StringBuilder();
			jdbc_sql_buff.append(sw.getBuffer());
			db_utils.validate_jdbc_sql(jdbc_sql_buff);

			List<String> desc = new ArrayList<String>();

			for (FieldInfo k : keys) {

				desc.add(k.getType() + " " + k.getName());
			}

			String[] param_descriptors_arr = desc.toArray(new String[desc.size()]);

			render_query(buffer, jdbc_sql_buff.toString(), table_name, false, ret_dto_type, true, fetch_list,
					method_name, "", param_descriptors_arr, true, null);

			return buffer;
		}

		@Override
		public StringBuilder render_crud_update(String class_name, String method_name, String table_name,
				String dto_class_name, boolean primitive_params) throws Exception {

			StringBuilder buffer = new StringBuilder();

			List<FieldInfo> keys = new ArrayList<FieldInfo>();
			List<FieldInfo> params = new ArrayList<FieldInfo>();

			db_utils.get_crud_info(table_name, keys, params, dto_class_name, jaxb_dto_classes);

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

			String sql_str;
			{
				Map<String, Object> context = new HashMap<String, Object>();

				context.put("params", params);
				context.put("keys", keys);

				StringWriter sw = new StringWriter();
				generate_sql("crud_sql_update", context, table_name, sw);
				StringBuilder jdbc_sql_buff = new StringBuilder();
				jdbc_sql_buff.append(sw.getBuffer());
				db_utils.validate_jdbc_sql(jdbc_sql_buff);
				sql_str = SqlUtils.sql_to_cpp_str(jdbc_sql_buff);
			}

			params.addAll(keys);

			Map<String, Object> context = new HashMap<String, Object>();

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
		public StringBuilder render_crud_delete(String class_name, String method_name, String table_name,
				String dto_class_name) throws Exception {

			StringBuilder buffer = new StringBuilder();

			List<FieldInfo> keys = new ArrayList<FieldInfo>();

			db_utils.get_crud_info(table_name, keys, null, dto_class_name, jaxb_dto_classes);

			if (keys.isEmpty()) {

				String msg = Helpers.get_no_pk_message(method_name);

				Helpers.build_warning_comment(buffer, msg);

				return buffer;
			}

			String sql_str;
			{
				Map<String, Object> context = new HashMap<String, Object>();

				context.put("keys", keys);

				StringWriter sw = new StringWriter();

				generate_sql("crud_sql_delete", context, table_name, sw);

				StringBuilder jdbc_sql_buff = new StringBuilder();
				jdbc_sql_buff.append(sw.getBuffer());
				db_utils.validate_jdbc_sql(jdbc_sql_buff);
				sql_str = SqlUtils.sql_to_cpp_str(jdbc_sql_buff);
			}

			Map<String, Object> context = new HashMap<String, Object>();

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
		public StringBuilder render_jaxb_crud(TypeCrud jaxb_type_crud) throws Exception {

			String node_name = JaxbProcessor.get_jaxb_node_name(jaxb_type_crud);

			String dto_class_name = jaxb_type_crud.getDto();

			if (dto_class_name.length() == 0) {

				throw new Exception("<" + node_name + "...\nAttribute 'dto' is empty");
			}

			String table_attr = jaxb_type_crud.getTable();

			if (table_attr == null || table_attr.length() == 0) {

				throw new Exception("<" + node_name + "...\nAttribute 'table' is empty");
			}

			try {

				db_utils.validate_table_name(table_attr);

				process_dto_class_name(dto_class_name);

				StringBuilder code_buff = JaxbProcessor.process_jaxb_crud(this, true, jaxb_type_crud, dto_class_name,
						table_attr);

				return code_buff;

			} catch (Throwable e) {

				// e.printStackTrace();
				String msg = "<" + node_name + " dto=\"" + dto_class_name + "\" table=\"" + table_attr + "\"...\n";

				throw new Exception(Helpers.get_error_message(msg, e));
			}
		}
	}
}
