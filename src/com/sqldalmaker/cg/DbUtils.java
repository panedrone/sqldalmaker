/*
 * Copyright 2011-2020 sqldalmaker@gmail.com
 * SQL DAL Maker Website: http://sqldalmaker.sourceforge.net
 * Read LICENSE.txt in the root of this project/archive for details.
 *
 */
package com.sqldalmaker.cg;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.sqldalmaker.jaxb.dto.DtoClass;
import com.sqldalmaker.jaxb.dto.DtoClasses;
import com.sqldalmaker.jaxb.settings.TypeMap;

/**
 * @author sqldalmaker@gmail.com
 */
public class DbUtils {

	private final Connection conn;

	private final FieldNamesMode field_names_mode;

	private final TypeMap type_map;

	public DbUtils(Connection conn, FieldNamesMode field_names_mode, TypeMap type_map) {

		this.conn = conn;

		this.field_names_mode = field_names_mode;

		this.type_map = type_map;
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
	private static List<String> get_pk_col_names(Connection conn, String table_name) throws SQLException {

		List<String> res = new ArrayList<String>();

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

	private static void validate_jaxb_dto_class(DtoClass jaxb_dto_class) throws Exception {

		List<DtoClass.Field> fields = jaxb_dto_class.getField();

		Set<String> col_names = new HashSet<String>();

		for (DtoClass.Field fe : fields) {

			String java_class_name = fe.getJavaType();

			validate_java_type_name(java_class_name);

			String col = fe.getColumn();

			if (col == null || col.trim().length() == 0) {

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

	public void validate_jdbc_sql(StringBuilder jdbc_sql_buf) throws SQLException {

		// CDRU SQL statements cannot be generated for the tables where all
		// columns are parts of PK
		PreparedStatement s = prepare(conn, jdbc_sql_buf.toString());

		s.close();
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

	private static String get_column_type_name(DtoClass jaxb_dto_class, String col_name, ResultSetMetaData rsmd,
			int i) {

		String java_class_name = JaxbProcessor.get_jaxb_field_type_name(jaxb_dto_class, col_name);

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
			DtoClasses jaxb_dto_classes) throws Exception {

		Set<String> gen_keys = new HashSet<String>();

		if (!("*".equals(explicit_gen_keys))) {

			String[] gen_keys_arr = Helpers.get_listed_items(explicit_gen_keys);

			check_duplicates(gen_keys_arr);

			for (String k : gen_keys_arr) {

				gen_keys.add(k.toLowerCase());
			}
		}

		DtoClass jaxb_dto_class = JaxbProcessor.find_jaxb_dto_class(dto_class_name, jaxb_dto_classes);

		PreparedStatement ps = create_prepared_statement(conn, table_name);

		try {

			List<FieldInfo> list = new ArrayList<FieldInfo>();

			ResultSetMetaData meta = ps.getMetaData();

			int column_count = meta.getColumnCount();

			for (int i = 1; i <= column_count; i++) {

				String col_name = get_column_name(meta, i);

				String type_name = get_column_type_name(jaxb_dto_class, col_name, meta, i);

				if (type_map != null) {

					type_name = Helpers.get_cpp_class_name_from_java_class_name(type_map, type_name);
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

	public void get_crud_info(String table_name, String explicit_primary_keys, List<FieldInfo> columns,
			List<FieldInfo> params, String dto_class_name, DtoClasses jaxb_dto_classes) throws Exception {

		List<String> pk_col_names = get_pk_col_names(conn, table_name);

		if (!("*".equals(explicit_primary_keys))) {

			String[] gen_keys_arr = Helpers.get_listed_items(explicit_primary_keys);

			check_duplicates(gen_keys_arr);

			for (String k : gen_keys_arr) {

				pk_col_names.add(k/* .toLowerCase() */); // don't make lower case the explicit names of PK columns
			}
		}

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

		DtoClass jaxb_dto_class = JaxbProcessor.find_jaxb_dto_class(dto_class_name, jaxb_dto_classes);

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

				String type_name = get_column_type_name(jaxb_dto_class, col_name, meta, i);

				if (type_map != null) {

					type_name = Helpers.get_cpp_class_name_from_java_class_name(type_map, type_name);
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

	public void get_dto_field_info(String jdbc_dto_sql, DtoClass jaxb_dto_class, List<FieldInfo> fields)
			throws Exception {

		validate_jaxb_dto_class(jaxb_dto_class);

		fields.clear();

		Set<String> metadata_col_names = new HashSet<String>();

		if (jdbc_dto_sql != null && jdbc_dto_sql.trim().length() > 0) {

			PreparedStatement ps; // PreparedStatement is interface

			boolean is_sp = SqlUtils.is_jdbc_stored_proc_call(jdbc_dto_sql);

			if (is_sp) {

				ps = prepare_call(conn, jdbc_dto_sql); // in MySql, getMetaData() for SP does not work, but maybe it
				// works
				// with others :)

			} else {

				ps = prepare(conn, jdbc_dto_sql);
			}

			try {

				ResultSetMetaData md = ps.getMetaData();

				if (md == null) { // jTDS returns null for invalid SQL

					throw new Exception("PreparedStatement.getMetaData() returns null for '" + jdbc_dto_sql + "");
				}

				int col_count = md.getColumnCount();

				if (col_count == 0) {

					throw new Exception("ResultSetMetaData.getColumnCount() returns 0");
				}

				/////////////////////////////////////////////////
				//
				// create DTO-class even if single field is returned
				//
				for (int i = 1; i <= col_count; i++) {

					String col_name = get_column_name(md, i);
					String type_name = get_column_type_name(jaxb_dto_class, col_name, md, i);

					if (type_map != null) {

						type_name = Helpers.get_cpp_class_name_from_java_class_name(type_map, type_name);
					}

					FieldInfo f = new FieldInfo(field_names_mode, type_name, col_name);

					fields.add(f);

					metadata_col_names.add(col_name);
				}

			} finally {

				ps.close();
			}
		}

		////////////////////////////////////////////////
		//
		// If the field is missing in metadata,
		// it will be calculated outside of RDBMS.
		//
		for (DtoClass.Field field : jaxb_dto_class.getField()) {

			String col_name = field.getColumn();

			if (!metadata_col_names.contains(col_name)) {

				String type_name = field.getJavaType();

				if (type_map != null) {

					type_name = Helpers.get_cpp_class_name_from_java_class_name(type_map, type_name);
				}

				FieldInfo f = new FieldInfo(field_names_mode, type_name, col_name);

				fields.add(f);
			}
		}
	}

	public void get_jdbc_sql_info(String sql_root_abs_path, String jdbc_dao_sql, List<FieldInfo> fields,
			String dto_param_type, String[] param_descriptors, List<FieldInfo> params, String jaxb_dto_or_return_type,
			boolean jaxb_return_type_is_dto, DtoClasses jaxb_dto_classes) throws Exception {

		fields.clear();
		params.clear();

		check_duplicates(param_descriptors);

		PreparedStatement ps; // PreparedStatement is interface

		boolean is_sp = SqlUtils.is_jdbc_stored_proc_call(jdbc_dao_sql);

		if (is_sp) {

			ps = prepare_call(conn, jdbc_dao_sql);

		} else {

			ps = prepare(conn, jdbc_dao_sql);
		}

		try {

			if (jaxb_return_type_is_dto) {

				if (jaxb_dto_or_return_type == null || jaxb_dto_or_return_type.trim().length() == 0) {

					throw new Exception("Value of 'dto' is empty");
				}

				//
				// find_jaxb_dto_class(...) throws if no class or several classes
				//
				DtoClass jaxb_dto_class = JaxbProcessor.find_jaxb_dto_class(jaxb_dto_or_return_type, jaxb_dto_classes);

				get_fields_info(ps, jaxb_dto_class, fields);

				if (fields.size() == 0) { // no fields from DAO SQL, try to get fields from declaration of DTO

					String jdbc_dto_sql = SqlUtils.jdbc_sql_by_query_ref(jaxb_dto_class.getRef(), sql_root_abs_path);

					get_dto_field_info(jdbc_dto_sql, jaxb_dto_class, fields);
				}

				if (fields.size() == 0) {

					throw new Exception("Columns count is 0. Is SQL statement valid?");
				}

			} else { // jaxb_return_type_is_dto == false

				get_fields_info(ps, null, fields);

				String ret_type_name;

				if (jaxb_dto_or_return_type != null && jaxb_dto_or_return_type.trim().length() > 0) {

					ret_type_name = jaxb_dto_or_return_type;

				} else {
					//
					// fields.size() == 0 for SQL statement 'select inventory_in_stock(?)'
					// from MySQL sakila example
					//
					if (fields.size() < 1) {

						// throw new Exception("Columns count is < 1 . Is SQL statement valid?");
						ret_type_name = "Object";

					} else {

						ret_type_name = fields.get(0).getType();
					}
				}

				fields.clear();

				FieldInfo f = new FieldInfo(field_names_mode, ret_type_name, "ret_value");

				fields.add(f);
			}

			get_params_info(ps, param_descriptors,
					dto_param_type == null || dto_param_type.length() == 0 ? FieldNamesMode.AS_IS : field_names_mode,
					params);

		} finally {

			ps.close();
		}
	}

	public void get_exec_dml_jdbc_sql_info(String jdbc_sql, String dto_param_type, String[] param_descriptors,
			List<FieldInfo> params) throws Exception {

		check_duplicates(param_descriptors);

		PreparedStatement ps; // PreparedStatement is interface

		boolean is_sp = SqlUtils.is_jdbc_stored_proc_call(jdbc_sql);

		if (is_sp) {

			ps = prepare_call(conn, jdbc_sql);

		} else {

			ps = prepare(conn, jdbc_sql);
		}

		try {

			get_params_info(ps, param_descriptors,
					dto_param_type == null || dto_param_type.length() == 0 ? FieldNamesMode.AS_IS : field_names_mode,
					params);

		} finally {

			ps.close();
		}
	}

	private void get_fields_info(PreparedStatement ps, DtoClass jaxb_dto_class, List<FieldInfo> fields)
			throws Exception {

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

		//
		// PostgreSQL: query to function 'select * from fn_get_tests(?, ?)'
		// returns ResultSet
		//
		if (col_count == 1) {

			String java_class_name = rsmd.getColumnClassName(1); // it starts from 1!

			String rs_class_name = ResultSet.class.getName();

			if (java_class_name.equals(rs_class_name)) {

				return;
			}
		}

		for (int i = 1; i <= col_count; i++) {

			String col_name = get_column_name(rsmd, i);

			String type_name = get_column_type_name(jaxb_dto_class, col_name, rsmd, i);

			if (type_map != null) {

				type_name = Helpers.get_cpp_class_name_from_java_class_name(type_map, type_name);
			}

			FieldInfo f = new FieldInfo(field_names_mode, type_name, col_name);

			fields.add(f);
		}
	}

	private void get_params_info(PreparedStatement ps, String[] param_descriptors, FieldNamesMode field_names_mode,
			List<FieldInfo> params) throws SQLException {

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

		} catch (Exception ex) {

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

					param_type_name = Helpers.get_cpp_class_name_from_java_class_name(type_map, param_type_name);
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

				param_type_name = Helpers.get_cpp_class_name_from_java_class_name(type_map, param_type_name);
			}

			FieldInfo f = new FieldInfo(field_names_mode, param_type_name, param_name);

			params.add(f);
		}
	}

	public void get_crud_create_info(String table_name, List<FieldInfo> ai_fields, List<FieldInfo> not_ai_fields,
			String generated, String dto_class_name, DtoClasses jaxb_dto_classes) throws Exception {

		ai_fields.clear();
		not_ai_fields.clear();

		FieldInfo[] fi_arr = get_table_columns_info(table_name, generated, dto_class_name, jaxb_dto_classes);

		for (FieldInfo fi : fi_arr) {

			if (fi.isAutoIncrement()) {

				ai_fields.add(fi);

			} else {

				not_ai_fields.add(fi);
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
