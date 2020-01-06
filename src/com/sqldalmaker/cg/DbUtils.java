/*
 * Copyright 2011-2020 sqldalmaker@gmail.com
 * SQL DAL Maker Website: http://sqldalmaker.sourceforge.net
 * Read LICENSE.txt in the root of this project/archive for details.
 *
 */
package com.sqldalmaker.cg;

import com.sqldalmaker.jaxb.dto.DtoClass;
import com.sqldalmaker.jaxb.dto.DtoClasses;
import com.sqldalmaker.jaxb.settings.Type;
import com.sqldalmaker.jaxb.settings.TypeMap;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author sqldalmaker@gmail.com
 */
public class DbUtils {

	public static void check_if_select_sql(String dao_jdbc_sql) throws Exception {

		String trimmed = dao_jdbc_sql.toLowerCase().trim();

		String[] parts = trimmed.split("\\s+");

		if (parts.length > 0) {

			if ("select".equals(parts[0])) {

				throw new Exception("SELECT is not allowed here");
			}
		}
	}

	private final Connection conn;

	private final FieldNamesMode field_names_mode;

	private final TypeMap type_map;

	public DbUtils(Connection conn, FieldNamesMode field_names_mode, TypeMap type_map) {

		this.conn = conn;

		this.field_names_mode = field_names_mode;

		this.type_map = type_map;
	}

	@SuppressWarnings("unused")
	public FieldNamesMode get_field_names_mode() {

		return field_names_mode;
	}

	private static PreparedStatement create_prepared_statement(Connection conn, String table_name) throws SQLException {

		return prepare(conn, "SELECT * FROM " + table_name + " WHERE 1 = 0");
	}

	private static String get_column_name(ResultSetMetaData meta, int col) throws Exception {

		// in H2, for 'SELECT a as a1, a as a2 FROM...' will be retrieved
		// duplicate col. names
		String column_name = null;

		try {

			column_name = meta.getColumnLabel(col);

		} catch (SQLException e) {
			// remains null
		}

		if (null == column_name || 0 == column_name.length()) {

			column_name = meta.getColumnName(col);
		}

		if (null == column_name || 0 == column_name.length()) {

			throw new Exception(
					"Column name cannot be detected. Try to specify column label. For example, 'SELECT COUNT(*) AS RES...'");
		}

		return column_name;
	}

	/*
	 * DatabaseMetaData.getPrimaryKeys returns pk_col_names in lower case. For other
	 * JDBC drivers, it may differ. To ensure correct comparison of field names, do
	 * it always in lower case
	 */
	private static ArrayList<String> get_pk_col_names(Connection conn, String table_name) throws SQLException {

		ArrayList<String> res = new ArrayList<String>();

		DatabaseMetaData db_info = conn.getMetaData();

		String schema = null;

		if (table_name.contains(".")) {

			String[] parts = table_name.split("\\.");

			if (parts.length != 2) {
				throw new SQLException("Invalid table name: '" + table_name + "'");
			}

			schema = parts[0];
			table_name = parts[1];
		}

		ResultSet rs = db_info.getPrimaryKeys(null, schema, table_name);

		try {

			while (rs.next()) {

				res.add(rs.getString("COLUMN_NAME"));
			}

		} finally {

			rs.close();
		}

		return res;
	}

	public void validate_table_name(String table_name) throws SQLException {

		DatabaseMetaData db_info = conn.getMetaData();

		String schema = null;

		if (table_name.contains(".")) {

			String[] parts = table_name.split("\\.");

			if (parts.length != 2) {
				throw new SQLException("Invalid table name: '" + table_name + "'");
			}

			schema = parts[0];
			table_name = parts[1];
		}

		ResultSet rs = db_info.getTables(null, schema, table_name, null);

		try {

			if (rs.next()) {

				return;
			}

		} finally {

			rs.close();
		}

		throw new SQLException("Data table '" + table_name + "' not found. Table names may be case sensitive.");
	}

	private static String[] parse_ref(String src) throws Exception {

		String before_brackets;

		String inside_brackets;

		src = src.trim();

		int pos = src.indexOf('(');

		if (pos == -1) {

			throw new Exception("'(' expected in ref shortcut");

		} else {

			if (!src.endsWith(")")) {

				throw new Exception("')' expected in ref shortcut");
			}

			before_brackets = src.substring(0, pos);

			inside_brackets = src.substring(before_brackets.length() + 1, src.length() - 1);
		}

		return new String[] { before_brackets, inside_brackets };
	}

	private static void validate_element_dto_class(DtoClass dto_class) throws Exception {

		List<DtoClass.Field> fields = dto_class.getField();

		Set<String> col_names = new HashSet<String>();

		for (DtoClass.Field fe : fields) {

			String java_class_name = fe.getJavaType();

			validate_java_type_name(java_class_name);

			String col = fe.getColumn();

			if (col == null) {

				throw new Exception("Invalid column name: null");
			}

			if (col_names.contains(col)) {

				throw new Exception("Duplicate <field column='" + col + "'...");
			}

			col_names.add(col);
		}
	}

	private static void validate_java_type_name(final String java_type_name) throws Exception {

		String type;

		String[] arr_parts = java_type_name.split("\\[");

		if (arr_parts.length == 2 && "]".equals(arr_parts[1].trim())) {

			type = arr_parts[0].trim();

		} else {

			type = java_type_name;
		}

		try {

			Helpers.process_java_type_name(type);

		} catch (ClassNotFoundException e) {

			String java_class_name2 = "java.lang." + type;

			try {

				Helpers.process_java_type_name(java_class_name2);

			} catch (ClassNotFoundException e1) {

				throw new Exception("Invalid type name: " + java_type_name);
			}
		}
	}

	private static String[] parse_param_descriptor(String param_descriptor) {

		String param_type_name;

		String param_name;

		String[] parts = param_descriptor.split("\\s+");

		if (parts.length > 1) {

			param_name = parts[parts.length - 1];

			param_type_name = param_descriptor.substring(0, param_descriptor.length() - 1 - param_name.length()).trim();

		} else {

			param_name = param_descriptor;

			param_type_name = null;
		}

		return new String[] { param_type_name, param_name };
	}

	private static void check_duplicates(String[] param_names) throws SQLException {

		if (param_names != null) {

			Set<String> set = new HashSet<String>();

			for (String param_name : param_names) {

				if (set.contains(param_name)) {

					throw new SQLException("Duplicated parameter names");
				}

				set.add(param_name);
			}
		}
	}

	public void validate_sql(StringBuilder sql_buf) throws SQLException {

		if (sql_buf.length() > 0) {

			// CDRU SQL statements cannot be generated for the tables where all
			// columns are parts of PK
			PreparedStatement s = prepare(conn, sql_buf.toString());

			s.close();
		}
	}

	private static PreparedStatement prepare(Connection conn, String jdbc_sql) throws SQLException {

		// For MySQL, prepareStatement doesn't throw Exception for
		// invalid SQL statements
		// and doesn't return null as well
		return conn.prepareStatement(jdbc_sql);
	}

	private static CallableStatement prepare_call(Connection conn, String jdbc_sql) throws SQLException {

		return conn.prepareCall(jdbc_sql);
	}

	private static String get_qualified_name(String java_class_name) {

		java_class_name = java_class_name.replaceAll("\\s+", "");

		String element_name;

		boolean is_array;

		if (java_class_name.contains("[")) {

			element_name = java_class_name.replace('[', ' ').replace(']', ' ').trim();

			is_array = true;

		} else {

			is_array = false;

			element_name = java_class_name;
		}

		boolean is_primitive = Helpers.PRIMITIVE_CLASSES.containsKey(element_name);

		if (!is_primitive && !java_class_name.contains(".")) {

			element_name = "java.lang." + element_name;
		}

		java_class_name = element_name;

		if (is_array) {

			java_class_name += " []";
		}

		return java_class_name;
	}

	public static String get_cpp_class_name_from_java_class_name(TypeMap type_map, String java_class_name) {

		String s1 = get_qualified_name(java_class_name);

		for (Type t : type_map.getType()) {

			String s2 = get_qualified_name(t.getJava());

			if (s2.equals(s1)) {

				return t.getTarget();
			}
		}

		return type_map.getDefault();
	}

	private static String get_explicit_column_type_name(DtoClass dto_class, String col_name) {

		if (dto_class != null && dto_class.getField() != null) {

			for (DtoClass.Field c : dto_class.getField()) {

				if (col_name.equals(c.getColumn())) {

					return c.getJavaType();
				}
			}
		}

		return null;
	}

	private static String get_column_type_name(DtoClass dto_class, String col_name, ResultSetMetaData rsmd, int i) {

		String java_class_name = get_explicit_column_type_name(dto_class, col_name);

		if (java_class_name == null) {

			try {

				// sometime returns "[B":
				// See comments for Class.getName() API
				java_class_name = rsmd.getColumnClassName(i);

				java_class_name = Helpers.process_java_type_name(java_class_name);

			} catch (Exception ex) {

				java_class_name = Object.class.getName();
			}
		}
		return java_class_name;
	}

	private static String get_param_type_name(ParameterMetaData pm, int i) {

		String java_class_name;

		try {

			// getParameterClassName throws exception in
			// mysql-connector-java-5.1.17-bin.jar:
			// sometime returns "[B":
			// See comments for Class.getName() API
			java_class_name = pm.getParameterClassName(i + 1);

			java_class_name = Helpers.process_java_type_name(java_class_name);

		} catch (Exception ex) {

			java_class_name = Object.class.getName();
		}

		return java_class_name;
	}

	public FieldInfo[] get_table_columns_info(String table_name, String explicit_gen_keys, String dto_class_name,
			DtoClasses dto_classes) throws Exception {

		Set<String> gen_keys = new HashSet<String>();

		if (!("*".equals(explicit_gen_keys))) {

			String[] gen_keys_arr = Helpers.get_listed_items(explicit_gen_keys);

			check_duplicates(gen_keys_arr);

			for (String k : gen_keys_arr) {

				gen_keys.add(k.toLowerCase());
			}
		}

		DtoClass dto_class = Helpers.find_dto_class(dto_class_name, dto_classes);

		PreparedStatement ps = create_prepared_statement(conn, table_name);

		try {

			ArrayList<FieldInfo> list = new ArrayList<FieldInfo>();

			ResultSetMetaData meta = ps.getMetaData();

			int column_count = meta.getColumnCount();

			for (int i = 1; i <= column_count; i++) {

				String col_name = get_column_name(meta, i);

				String type_name = get_column_type_name(dto_class, col_name, meta, i);

				if (type_map != null) {

					type_name = get_cpp_class_name_from_java_class_name(type_map, type_name);
				}

				FieldInfo ci = new FieldInfo(field_names_mode, type_name, col_name);

				if (!("*".equals(explicit_gen_keys))) {

					String key = col_name.toLowerCase();

					if (gen_keys.contains(key)) {

						ci.setAutoIncrement(true);

						gen_keys.remove(key);
					}

				} else {

					boolean is_auto_inc = meta.isAutoIncrement(i);

					ci.setAutoIncrement(is_auto_inc);
				}

				list.add(ci);
			}

			if (gen_keys.size() > 0) {

				String msg = "Unknown column names are listed as 'generated':";

				for (String s : gen_keys) {

					msg += " " + s;
				}

				throw new SQLException(msg);
			}

			return list.toArray(new FieldInfo[list.size()]);

		} finally {

			ps.close();
		}
	}

	public void get_crud_info(String table_name, ArrayList<FieldInfo> columns, List<FieldInfo> params,
			String dto_class_name, DtoClasses dto_classes) throws Exception {

		get_crud_info(table_name, columns, params, dto_class_name, dto_classes, type_map);
	}

	public void get_crud_info(String table_name, ArrayList<FieldInfo> columns, List<FieldInfo> params,
			String dto_class_name, DtoClasses dto_classes, TypeMap type_map) throws Exception {

		ArrayList<String> pk_col_names = get_pk_col_names(conn, table_name);

		/*
		 * DatabaseMetaData.getPrimaryKeys may return pk_col_names in lower case
		 * (SQLite3). For other JDBC drivers, it may differ. To ensure correct
		 * comparison of field names, do it always in lower case
		 */
		Set<String> pk_col_names_set_lower_case = new HashSet<String>();

		for (String pk_col_name : pk_col_names) {

			// xerial SQLite3 returns pk_col_names in the format '[employeeid] asc'
			// (compound PK)
			pk_col_name = pk_col_name.toLowerCase();
			pk_col_name = pk_col_name.replace("[", "");
			pk_col_name = pk_col_name.replace("]", "");
			if (pk_col_name.endsWith(" asc")) {
				pk_col_name = pk_col_name.replace(" asc", "");
			}
			if (pk_col_name.endsWith(" desc")) {
				pk_col_name = pk_col_name.replace(" desc", "");
			}
			pk_col_name = pk_col_name.trim();

			pk_col_names_set_lower_case.add(pk_col_name);
		}

		DtoClass dto_class = Helpers.find_dto_class(dto_class_name, dto_classes);

		PreparedStatement ps = create_prepared_statement(conn, table_name);

		try {

			ResultSetMetaData meta = ps.getMetaData();

			if (meta == null) {

				// jTDS returns null for invalid SQL
				throw new SQLException("PreparedStatement.getMetaData returns null for '" + table_name + "");
			}

			int column_count = meta.getColumnCount();

			for (int i = 1; i <= column_count; i++) {

				String col_name = get_column_name(meta, i);

				String type_name = get_column_type_name(dto_class, col_name, meta, i);

				if (type_map != null) {

					type_name = get_cpp_class_name_from_java_class_name(type_map, type_name);
				}

				FieldInfo f = new FieldInfo(field_names_mode, type_name, col_name);

				if (pk_col_names_set_lower_case.contains(col_name.toLowerCase())) {

					columns.add(f);

				} else {

					if (params != null) {

						params.add(f);
					}
				}
			}

		} finally {

			ps.close();
		}
	}

	public void get_dto_field_info(String dto_jdbc_sql, DtoClass dto_class, ArrayList<FieldInfo> fields)
			throws Exception {

		fields.clear();

		String ref = dto_class.getRef();

		if (ref == null || ref.length() == 0) {

			for (DtoClass.Field c : dto_class.getField()) {

				String type_name = c.getJavaType();

				if (type_map != null) {

					type_name = get_cpp_class_name_from_java_class_name(type_map, type_name);
				}

				FieldInfo f = new FieldInfo(field_names_mode, type_name, c.getColumn());

				fields.add(f);
			}
		}

		if (dto_jdbc_sql == null || dto_jdbc_sql.trim().length() == 0) {
			return;
		}
		///////////////////////////////////////////

		PreparedStatement ps; // PreparedStatement is interface

		boolean is_sp = is_jdbc_stored_proc_call(dto_jdbc_sql);

		if (is_sp) {

			ps = prepare_call(conn, dto_jdbc_sql); // in MySql, getMetaData() for SP does not work, but maybe it works
													// with others :)

		} else {

			ps = prepare(conn, dto_jdbc_sql);
		}

		try {

			ResultSetMetaData md = ps.getMetaData();

			if (md == null) { // jTDS returns null for invalid SQL

				throw new Exception("PreparedStatement.getMetaData returns null for '" + ref + "");
			}

			int col_count = md.getColumnCount();

			if (col_count == 0) {

				throw new Exception("getColumnCount returned 0");
			}

			// ///////////////////////////////////////////////
			// create DTO-class even if single field is returned
			Set<String> col_names = new HashSet<String>();

			for (int i = 1; i <= col_count; i++) {

				String col_name = get_column_name(md, i);

				col_names.add(col_name);

				String type_name = get_column_type_name(dto_class, col_name, md, i);

				validate_element_dto_class(dto_class);

				if (type_map != null) {

					type_name = get_cpp_class_name_from_java_class_name(type_map, type_name);
				}

				FieldInfo f = new FieldInfo(field_names_mode, type_name, col_name);

				fields.add(f);
			}

			List<DtoClass.Field> field_list = dto_class.getField();

			for (DtoClass.Field fe : field_list) {

				String col = fe.getColumn();

				if (!col_names.contains(col)) {

					throw new Exception(
							"Duplicated column name in declaration of DTO class " + dto_class.getName() + ": " + col);
				}
			}

		} finally {

			ps.close();
		}
	}

	public static String jdbc_sql_by_ref_exec_dml(String ref, String sql_root_abs_path) throws Exception {

		if (is_jdbc_stored_proc_call(ref)) {

			return ref;

		} else if (is_stored_proc_call_shortcut(ref)) {

			return ref; // stored_proc_shortcut_to_jdbc_call(ref);

		} else if (is_sql_file_ref(ref)) {

			String sql_file_path = Helpers.concat_path(sql_root_abs_path, ref);

			return Helpers.load_text_from_file(sql_file_path);

		} else {

			throw new Exception("Invalid 'ref'': " + ref);
		}
	}

	private String query_shortcut_ref_to_jdbc_sql(String ref) throws Exception {

		String[] parts2 = parse_ref(ref);

		String table_name = parts2[0];

		validate_table_name(table_name); // PostgreSQL JDBC prepareStatement passes wrong table names

		String param_descriptors = parts2[1];

		String[] param_arr = Helpers.get_listed_items(param_descriptors);

		if (param_arr.length < 1) {

			throw new Exception("Not empty list of parameters expected in ref shortcut");
		}

		String params = param_arr[0] + "=?";

		for (int i = 1; i < param_arr.length; i++) {

			params += " AND " + param_arr[i] + "=?";
		}

		return "SELECT * FROM " + table_name + " WHERE " + params;
	}

	public String jdbc_sql_by_ref_query(String ref, String sql_root_abs_path) throws Exception {

		String[] parts = ref.split(":");

		String table_name = null;

		if (parts.length >= 2) {

			if ("table".compareTo(parts[0].toLowerCase().trim()) == 0) {

				table_name = ref.substring(parts[0].length() + 1);
			}

		} else if (is_jdbc_stored_proc_call(ref)) {

			return ref;

		} else if (is_stored_proc_call_shortcut(ref)) {

			return ref; // stored_proc_shortcut_to_jdbc_call(ref);

		} else if (is_stored_func_call_shortcut(ref)) {

			return ref;

		} else if (is_sql_file_ref(ref)) {

			String sql_file_path = Helpers.concat_path(sql_root_abs_path, ref);

			return Helpers.load_text_from_file(sql_file_path);

		} else if (is_sql_shortcut_ref(ref)) {

			String res = query_shortcut_ref_to_jdbc_sql(ref);

			return res;

		} else if (is_table_ref(ref)) {

			table_name = ref;

		} else if (/* ref == null || */ ref.trim().length() == 0) {

			return "";

		} else {

			throw new Exception("Invalid 'ref'': " + ref);
		}

		return "SELECT * FROM " + table_name + " WHERE 1 = 0";
	}

	public static boolean is_sql_shortcut_ref(String ref) {

		if (is_sql_file_ref_base(ref)) {

			return false;
		}

		return ref != null && ref.length() >= 4 && ref.contains("(") && ref.trim().endsWith(")");
	}

	public static boolean is_sql_file_ref(String ref) {

		if (is_sql_shortcut_ref(ref)) {

			return false;
		}

		if (is_jdbc_stored_proc_call(ref)) {

			return false;
		}

		return is_sql_file_ref_base(ref);
	}

	private static boolean is_sql_file_ref_base(String ref) {

		return ref != null && ref.length() > 4 && ref.endsWith(".sql");
	}

	public static boolean is_table_ref(String ref) {

		if (ref == null || ref.length() == 0) {

			return false;
		}

		if (is_sql_shortcut_ref(ref)) {

			return false;
		}

		if (is_jdbc_stored_proc_call(ref)) {

			return false;
		}

		if (is_sql_file_ref(ref)) {

			return false;
		}

		if (is_stored_proc_call_shortcut(ref)) {

			return false;
		}

		if (is_stored_func_call_shortcut(ref)) {

			return false;
		}

		final char[] ILLEGAL_CHARACTERS = { '/', '\n', '\r', '\t', '\0', '\f', '`', '?', '*', '\\', '<', '>', '|',
				'\"'/* , ':' */, ';', ',' };

		for (char c : ILLEGAL_CHARACTERS) {

			if (ref.contains(Character.toString(c))) {

				return false;
			}
		}

		// no empty strings separated by dots
		//
		String[] parts = ref.split("\\.", -1); // -1 to leave empty strings

		for (String s : parts) {

			if (s.length() == 0) {

				return false;
			}
		}

		return true;
	}

	public static boolean is_jdbc_stored_proc_call(String jdbc_sql) {

		jdbc_sql = jdbc_sql.trim();

		if (!jdbc_sql.startsWith("{") || !jdbc_sql.endsWith("}")) {
			return false;
		}

		// jdbc_sql = jdbc_sql.trim(); ^^ trimmed
		jdbc_sql = jdbc_sql.substring(1, jdbc_sql.length() - 1);

		return is_stored_proc_call_shortcut(jdbc_sql);
	}

	public static boolean is_stored_proc_call_shortcut(String text) {

		String[] parts = text.split("\\s+");

		if (parts.length < 2) {
			return false;
		}

		String call = parts[0];

		return call.compareToIgnoreCase("call") == 0;
	}

	public static boolean is_stored_func_call_shortcut(String text) {

		String[] parts = text.split("\\s+");

		if (parts.length < 2) {
			return false;
		}

		String call = parts[0];

		return call.compareToIgnoreCase("select") == 0;
	}

//	public static String stored_proc_shortcut_to_jdbc_call(String text) throws Exception {
//
//		if (is_stored_proc_call_shortcut(text)) {
//			return "{" + text + "}";
//		}
//		throw new Exception("This is not a shortcut of stored procedure call: " + text);
//	}

	public static String jdbc_to_php_stored_proc_call(final String jdbc_sql) throws java.lang.Exception {

		String sql = jdbc_sql.trim();

		if (is_jdbc_stored_proc_call(sql)) { // confirms syntax {call sp_name(...)}

			sql = sql.substring(1, sql.length() - 1).trim(); // converted to call sp_name(...)

		} else if (is_stored_proc_call_shortcut(sql)) {
			//

		} else {

			throw new Exception("Inexpected syntax of CALL: " + jdbc_sql);
		}

		return sql;
	}

	public static String jdbc_to_python_stored_proc_call(final String jdbc_sql) throws java.lang.Exception {

		String sql = jdbc_sql.trim();

		if (is_jdbc_stored_proc_call(sql)) { // confirms syntax {call sp_name(...)}

			sql = sql.substring(1, sql.length() - 1).trim(); // converted to call sp_name(...)

		} else if (is_stored_proc_call_shortcut(sql)) {
			//

		} else {

			throw new Exception("Inexpected syntax of CALL: " + jdbc_sql);
		}

		if (sql.contains("(")) {

			if (!sql.endsWith(")")) {

				throw new Exception("Inexpected syntax of CALL: " + jdbc_sql);
			}

			String[] parts = sql.split("[(]");

			sql = parts[0].trim();
		}

		return sql;
	}

	public String get_query_jdbc_sql_info(String sql_root_abs_path, String dao_jdbc_sql, ArrayList<FieldInfo> fields,
			String dto_param_type, String[] param_descriptors, ArrayList<FieldInfo> params, String return_type,
			boolean return_type_is_dto, DtoClasses dto_classes) throws Exception {

		check_duplicates(param_descriptors);

		PreparedStatement ps; // PreparedStatement is interface

		boolean is_sp = is_jdbc_stored_proc_call(dao_jdbc_sql);

		if (is_sp) {

			ps = prepare_call(conn, dao_jdbc_sql);

		} else {

			ps = prepare(conn, dao_jdbc_sql);
		}

		try {

			DtoClass dto_class;

			if (return_type_is_dto) {

				String dto_class_name = return_type;

				if (dto_class_name != null && dto_class_name.length() > 0) {

					dto_class = Helpers.find_dto_class(dto_class_name, dto_classes);

				} else {

					dto_class = null;
				}

				get_fields_info(ps, dto_class, fields);

				if (fields.size() == 0) {

					if (dto_class != null) {

						String dto_jdbc_sql = jdbc_sql_by_ref_query(dto_class.getRef(), sql_root_abs_path);

						get_dto_field_info(dto_jdbc_sql, dto_class, fields);
					}
				}

			} else { // return_type_is_dto == false

				fields.clear();

				if (return_type == null || return_type.trim().length() == 0) {

					return_type = "Object";
				}

				FieldInfo f = new FieldInfo(field_names_mode, return_type, "");

				fields.add(f);
			}

			get_params_info(ps, param_descriptors,
					dto_param_type == null || dto_param_type.length() == 0 ? FieldNamesMode.AS_IS : field_names_mode,
					params);

		} finally {

			ps.close();
		}

		return dao_jdbc_sql;
	}

	public void get_exec_dml_jdbc_sql_info(String sql, String dto_param_type, String[] param_descriptors,
			ArrayList<FieldInfo> params) throws Exception {

		check_duplicates(param_descriptors);

		PreparedStatement ps = prepare(conn, sql);

		try {

			get_params_info(ps, param_descriptors,
					dto_param_type == null || dto_param_type.length() == 0 ? FieldNamesMode.AS_IS : field_names_mode,
					params);

		} finally {

			ps.close();
		}
	}

	private void get_fields_info(PreparedStatement ps, DtoClass dto_class, List<FieldInfo> fields) throws Exception {

		fields.clear();

		// PostgreSQL:

		// Initial SQL is '{call get_tests(4)}'; ps is CallableStatement.
		// ps.toString() returns something like 'select * from get_tests(4) as result'
		// and ps.getMetaData() throws SQLException.

		ResultSetMetaData rsmd = null;

		try {

			// String prepared = ps.toString(); //

			rsmd = ps.getMetaData();

		} catch (SQLException ex) {

		}

		if (rsmd == null) { // null if no columns or prepare was called instead of prepare_call for stored
							// proc

			return;
		}

		int col_count;

		try {

			// Informix:
			// ResultSetMetaData.getColumnCount() returns
			// value > 0 for some DML statements, e.g. for 'update orders set
			// dt_id = ? where o_id = ?' it considers that 'dt_id' is column.
			// SQLite:
			// throws java.sql.SQLException: column 1 out
			// of bounds [1,0] if the statement is like INSERT
			col_count = rsmd.getColumnCount();

		} catch (Exception e) {

			col_count = 0;
		}

		if (col_count == 1) { // PostgreSQL: query to function 'select * from fn_get_tests(?, ?)' returns
								// ResultSet

			String java_class_name = rsmd.getColumnClassName(1); // it starts from 1!

			String rs_class_name = ResultSet.class.getName();

			if (java_class_name.equals(rs_class_name)) {

				return;
			}
		}

		for (int i = 1; i <= col_count; i++) {

			String col_name = get_column_name(rsmd, i);

			String type_name = get_column_type_name(dto_class, col_name, rsmd, i);

			if (type_map != null) {

				type_name = get_cpp_class_name_from_java_class_name(type_map, type_name);
			}

			FieldInfo f = new FieldInfo(field_names_mode, type_name, col_name);

			fields.add(f);
		}
	}

	private void get_params_info(PreparedStatement ps, String[] param_descriptors, FieldNamesMode field_names_mode,
			ArrayList<FieldInfo> params) throws SQLException {

		params.clear();

		ParameterMetaData pm;

		try {

			// Sybase ADS + adsjdbc.jar:
			// ------------------------

			// ps.getParameterMetaData() throws java.lang.AbstractMethodError for all
			// statements

			// SQL Server 2008 + sqljdbc4.jar:
			// -------------------------------

			// ps.getParameterMetaData() throws
			// com.microsoft.sqlserver.jdbc.SQLServerException for
			// some statements SQL with parameters like 'SELECT count(*) FROM orders o WHERE
			// o_date BETWEEN ? AND ?'
			// and for SQL statements without parameters

			// PostgeSQL:
			// ----------

			// ps.getParameterMetaData() throws SQLException for both PreparedStatement and
			// CallableStatement

			pm = ps.getParameterMetaData();

		} catch (Exception err) {

			if (param_descriptors == null) {

				return;
			}

			for (String param_descriptor : param_descriptors) {

				String param_type_name;

				String param_name;

				String[] parts = parse_param_descriptor(param_descriptor);

				if (parts[0] == null) {

					param_type_name = Object.class.getName();

					param_name = param_descriptor;

				} else {

					param_type_name = parts[0];

					param_name = parts[1];
				}

				if (type_map != null) {

					param_type_name = get_cpp_class_name_from_java_class_name(type_map, param_type_name);
				}

				FieldInfo f = new FieldInfo(field_names_mode, param_type_name, param_name);

				params.add(f);
			}

			return;
		}

		int params_count;

		try {

			params_count = pm.getParameterCount();

		} catch (SQLException e) {

			params_count = 0;
		}

		if (param_descriptors == null && params_count > 0) {

			throw new SQLException("Specified parameters count: 0. Detected parameters count: " + params_count);
		}

		if (param_descriptors != null && params_count != param_descriptors.length) {

			throw new SQLException("Specified parameters count: " + param_descriptors.length
					+ ". Detected parameters count: " + params_count);
		}

		if (param_descriptors == null) {

			return;
		}

		for (int i = 0; i < params_count; i++) {

			String param_descriptor = param_descriptors[i];

			String param_type_name;

			String param_name;

			String[] parts = parse_param_descriptor(param_descriptor);

			if (parts[0] == null) {

				param_type_name = get_param_type_name(pm, i);

				param_name = param_descriptor;

			} else {

				param_type_name = parts[0];

				param_name = parts[1];
			}

			if (type_map != null) {

				param_type_name = get_cpp_class_name_from_java_class_name(type_map, param_type_name);
			}

			FieldInfo f = new FieldInfo(field_names_mode, param_type_name, param_name);

			params.add(f);
		}
	}

	public void get_crud_create_metadata(String table_name, ArrayList<FieldInfo> keys, List<String> sql_col_names,
			ArrayList<FieldInfo> params, String generated, String dto_class_name, DtoClasses dto_classes)
			throws Exception {

		sql_col_names.clear();

		FieldInfo[] ci_arr = get_table_columns_info(table_name, generated, dto_class_name, dto_classes);

		for (FieldInfo tci : ci_arr) {

			if (tci.isAutoIncrement()) {

				keys.add(tci);

			} else {

				// original column name:
				sql_col_names.add(tci.getColumnName());

				params.add(tci);
			}
		}
	}

	public static ResultSet get_tables(Connection conn, DatabaseMetaData db_info, String schema, boolean include_views)
			throws SQLException {

		String[] types;

		if (include_views) {

			types = new String[] { "TABLE", "VIEW" };

		} else {

			types = new String[] { "TABLE" };
		}

		ResultSet rs_tables;

		String catalog = conn.getCatalog();

		rs_tables = db_info.getTables(catalog, schema, "%", types);

		return rs_tables;
	}

	public static void get_schema_names(Connection con, List<String> schema_names) throws SQLException {

		DatabaseMetaData db_info = con.getMetaData();

		ResultSet rs;

		rs = db_info.getSchemas();

		try {

			while (rs.next()) {

				schema_names.add(rs.getString("TABLE_SCHEM"));
			}

		} finally {

			rs.close();
		}
	}
}
