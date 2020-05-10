/*
 * Copyright 2011-2020 sqldalmaker@gmail.com
 * SQL DAL Maker Website: http://sqldalmaker.sourceforge.net
 * Read LICENSE.txt in the root of this project/archive for details.
 */
package com.sqldalmaker.cg;

import com.sqldalmaker.jaxb.dto.DtoClass;
import com.sqldalmaker.jaxb.dto.DtoClasses;
import com.sqldalmaker.jaxb.settings.TypeMap;
import org.apache.cayenne.dba.TypesMapping;

import java.sql.*;
import java.util.*;

/**
 * @author sqldalmaker@gmail.com
 */
public class JdbcUtils {

    private final Connection conn;

    private final FieldNamesMode field_names_mode;

    private final TypeMap type_map;

    public JdbcUtils(Connection conn, FieldNamesMode field_names_mode, TypeMap type_map) {

        this.conn = conn;

        this.field_names_mode = field_names_mode;

        this.type_map = type_map;
    }

    private static String _get_column_name(ResultSetMetaData meta, int col) throws Exception {

        // H2: col. names are duplicated for SQL like'SELECT a as a1, a as a2 FROM...'
        //
        String column_name;

        try {

            column_name = meta.getColumnLabel(col);

        } catch (SQLException e) {

            column_name = null;
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

    public void validate_jdbc_sql(StringBuilder jdbc_sql_buf) throws SQLException {

        // CDRU SQL statements cannot be generated for the tables where all
        // columns are parts of PK
        //
        PreparedStatement s = _prepare_jdbc_sql(jdbc_sql_buf.toString());

        s.close();
    }

    private PreparedStatement _prepare_by_table_name(String table_name) throws SQLException {

        return conn.prepareStatement("SELECT * FROM " + table_name + " WHERE 1 = 0");
    }

    private PreparedStatement _prepare_jdbc_sql(String jdbc_sql) throws SQLException {

        boolean is_sp = SqlUtils.is_jdbc_stored_proc_call(jdbc_sql);

        if (is_sp) {

            return conn.prepareCall(jdbc_sql);

        } else {

            // For MySQL, prepareStatement doesn't throw Exception for
            // invalid SQL statements
            // and doesn't return null as well
            //
            return conn.prepareStatement(jdbc_sql);
        }
    }

    private static String _get_column_type_name(ResultSetMetaData rsmd, int i) {

        try {

            // sometime returns "[B":
            // See comments for Class.getName() API
            //
            String java_class_name = rsmd.getColumnClassName(i);

            return Helpers.process_java_type_name(java_class_name);

        } catch (Exception ex) {

            return Object.class.getName();
        }
    }

    private static String _get_param_type_name(ParameterMetaData pm, int i) {

        String java_class_name;

        try {

            // getParameterClassName throws exception in
            // mysql-connector-java-5.1.17-bin.jar:
            // sometime returns "[B":
            // See comments for Class.getName() API
            //
            java_class_name = pm.getParameterClassName(i + 1);

            java_class_name = Helpers.process_java_type_name(java_class_name);

        } catch (Exception ex) {

            java_class_name = Object.class.getName();
        }

        return java_class_name;
    }

    public void _get_table_fields(
            final String table_name, List<FieldInfo> all_fields,
            List<FieldInfo> not_pk_fields, String explicit_pk, List<FieldInfo> pk_fields) throws Exception {

        if (all_fields != null) {
            all_fields.clear();
        }

        if (not_pk_fields != null) {
            not_pk_fields.clear();
        }

        if (pk_fields != null) {
            pk_fields.clear();
        }

        //////////////////////////////////////////////////////////
        //
        Set<String> pk_col_names_set_lower_case;

        if ("*".equals(explicit_pk)) {

            List<String> pk_col_names = new ArrayList<String>();

            pk_col_names_set_lower_case = new HashSet<String>();

            // DatabaseMetaData.getPrimaryKeys returns pk_col_names in lower case. For other
            // JDBC drivers, it may differ. To ensure correct comparison of field names, do
            // it always in lower case
            //
            DatabaseMetaData db_info = conn.getMetaData();

            ResultSet rs;

            if (table_name.contains(".")) {

                String[] parts = table_name.split("\\.");

                if (parts.length != 2) {

                    throw new SQLException("Invalid table name: '" + table_name + "'");
                }

                rs = db_info.getPrimaryKeys(null, parts[0], parts[1]);

            } else {

                rs = db_info.getPrimaryKeys(null, null, table_name);
            }

            try {

                while (rs.next()) {

                    pk_col_names.add(rs.getString("COLUMN_NAME"));
                }

            } finally {

                rs.close();
            }

            /*
             * DatabaseMetaData.getPrimaryKeys may return pk_col_names in lower case
             * (SQLite3). For other JDBC drivers, it may differ. To ensure correct
             * comparison of field names, do it always in lower case
             */

            for (String pk_col_name : pk_col_names) {

                // xerial SQLite3 returns pk_col_names in the format '[employeeid] asc'
                // (compound PK)
                //
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

        } else { // if PK are specified explicitely, don't use getPrimaryKeys at all

            String[] gen_keys_arr = Helpers.get_listed_items(explicit_pk);

            Helpers.check_duplicates(gen_keys_arr);

            for (int i = 0; i < gen_keys_arr.length; i++) {

                gen_keys_arr[i] = gen_keys_arr[i].toLowerCase();
            }

            pk_col_names_set_lower_case = new HashSet<String>(Arrays.asList(gen_keys_arr));
        }

        Map<String, FieldInfo> fields_map = new HashMap<String, FieldInfo>();

        PreparedStatement ps = _prepare_by_table_name(table_name);

        try {

            ResultSetMetaData meta = ps.getMetaData();

            if (meta == null) {

                // jTDS returns null for invalid SQL
                throw new SQLException("PreparedStatement.getMetaData returns null for '" + table_name + "");
            }

            int column_count = meta.getColumnCount();

            for (int i = 1; i <= column_count; i++) {

                String col_name = _get_column_name(meta, i);

                String type_name = _get_column_type_name(meta, i);

                if (type_map != null) {

                    type_name = Helpers.get_cpp_class_name_from_java_class_name(type_map, type_name);
                }

                FieldInfo fi = new FieldInfo(field_names_mode, type_name, col_name);

                if (pk_col_names_set_lower_case.contains(col_name.toLowerCase())) {

                    pk_fields.add(fi);

                } else {

                    if (not_pk_fields != null) {

                        not_pk_fields.add(fi);
                    }
                }

                if (all_fields != null) {

                    all_fields.add(fi);
                }

                fields_map.put(col_name, fi);
            }

        } finally {

            ps.close();
        }

        _refine_fields_by_table_metadata(table_name, fields_map);
    }

    public void get_dao_crud_info(
            final DtoClass jaxb_dto_class, final String dao_jdbc_sql, final String dao_table_name,
            List<String> dao_table_key_col_names, List<FieldInfo> fields, List<FieldInfo> params) throws Exception {

        fields.clear();

        params.clear();

        // [col_name] -- [java-type]
        Map<String, String> explicit_dto_col_types = new HashMap<String, String>();

        for (DtoClass.Field field : jaxb_dto_class.getField()) {

            explicit_dto_col_types.put(field.getColumn(), field.getJavaType());
        }

        List<String> dao_table_col_names = new ArrayList<String>();

        Map<String, String> dao_table_col_types = new HashMap<String, String>();

        PreparedStatement ps = conn.prepareStatement(dao_jdbc_sql);

        try {

            ResultSetMetaData rsmd = ps.getMetaData();

            int column_count = rsmd.getColumnCount();

            for (int i = 1; i <= column_count; i++) {

                String col_name = rsmd.getColumnName(i);

                dao_table_col_names.add(col_name);

                dao_table_col_types.put(col_name, rsmd.getColumnClassName(i));
            }

        } finally {

            ps.close();
        }

        // [col_name] -- [java-type]
        Map<String, String> final_col_types = new HashMap<String, String>();

        String[] parts = dao_table_name.split("\\.", -1); // -1 to leave empty strings

        String schema_name = null;

        String table_name;

        if (parts.length == 1) {

            table_name = dao_table_name;

        } else {

            table_name = parts[parts.length - 1];

            schema_name = dao_table_name.substring(0, dao_table_name.lastIndexOf('.'));
        }

        DatabaseMetaData md = conn.getMetaData();

        // !!! DatabaseMetaData.getColumns may return system fields!!!

        ResultSet rs_columns = md.getColumns(null, schema_name, table_name, "%");

        try {

            while (rs_columns.next()) {

                String col_name = rs_columns.getString("COLUMN_NAME");

                if (explicit_dto_col_types.containsKey(col_name)) {

                    String java_type_name = explicit_dto_col_types.get(col_name);

                    final_col_types.put(col_name, java_type_name);

                } else if (dao_table_col_types.containsKey(col_name)) {

                    int type = rs_columns.getInt("DATA_TYPE");

                    String java_type_name = TypesMapping.getJavaBySqlType(type);

                    final_col_types.put(col_name, java_type_name);
                }
            }

            if (final_col_types.size() == 0) {

                throw new Exception("Cannnot detect columns for '" + dao_table_name
                        + "'. Check the name of table (e.g. lower/upper case).");
            }

        } finally {

            rs_columns.close();
        }

        ////////////////////////////////////////////////
        //

        for (String col_name : dao_table_col_names) {

            String java_type_name = final_col_types.get(col_name);

            if (java_type_name == null) {

                throw new Exception("Column '" + col_name + "' not found. Ensure lower/upper case.");
            }

            fields.add(new FieldInfo(field_names_mode, java_type_name, col_name));
        }

        for (String col_name : dao_table_key_col_names) {

            String java_type_name = final_col_types.get(col_name);

            if (java_type_name == null) {

                throw new Exception("Key column '" + col_name + "' not found. Ensure lower/upper case.");
            }

            params.add(new FieldInfo(field_names_mode, java_type_name, col_name));
        }
    }

    private void _get_fields_map_by_jdbc_sql(String jdbc_sql, Map<String, FieldInfo> fields_map, List<FieldInfo> fields) throws Exception {

        fields.clear();

        fields_map.clear();

        PreparedStatement ps = _prepare_jdbc_sql(jdbc_sql);

        try {

            ResultSetMetaData rsmd = ps.getMetaData();

            int column_count = rsmd.getColumnCount();

            for (int i = 1; i <= column_count; i++) {

                String col_name = _get_column_name(rsmd, i);

                // considers "[B", etc.
                //
                String java_type_name = _get_column_type_name(rsmd, i);

                FieldInfo field = new FieldInfo(field_names_mode, java_type_name, col_name);

                if (rsmd.isAutoIncrement(i)) {

                    field.setAutoIncrement(true);

                } else {

                    field.setAutoIncrement(false);
                }

                fields_map.put(col_name, field);

                fields.add(field);
            }

        } finally {

            ps.close();
        }
    }

    private void _get_dto_class_field_info(
            DtoClass jaxb_dto_class, String sql_root_abs_path,
            Map<String, FieldInfo> fields_map, List<FieldInfo> fields) throws Exception {

        fields.clear();

        fields_map.clear();

        String dto_ref = jaxb_dto_class.getRef();

        if (!SqlUtils.is_empty_ref(dto_ref)) {

            {
                String jdbc_sql = SqlUtils.jdbc_sql_by_dto_class_ref(dto_ref, sql_root_abs_path);

                _get_fields_map_by_jdbc_sql(jdbc_sql, fields_map, fields);
            }

            if (SqlUtils.is_table_ref(dto_ref)) {

                _refine_fields_by_table_metadata(dto_ref, fields_map);

            } else if (SqlUtils.is_sql_shortcut_ref(dto_ref)) {

                String[] parts = SqlUtils.parse_sql_shortcut_ref(dto_ref);

                String table_name = parts[0];

                // obtain fields the same way like in the case of table. params will be obtained from DAO SQL.
                // params are obtained below (considering DAO SQL and method_param_descriptors)

                _refine_fields_by_table_metadata(table_name, fields_map);
            }
        }

        _refine_field_info_by_jaxb_explicit_fields(jaxb_dto_class.getField(), fields_map, fields);
    }

    private void _refine_field_info_by_jaxb_explicit_fields(
            List<DtoClass.Field> jaxb_explicit_fields, Map<String, FieldInfo> fields_map, List<FieldInfo> fields) {

        if (jaxb_explicit_fields == null) {

            return;
        }

        for (DtoClass.Field jaxb_explicit_field : jaxb_explicit_fields) {

            String col_name = jaxb_explicit_field.getColumn();

            String java_type_name = jaxb_explicit_field.getJavaType();

            if (fields_map.containsKey(col_name)) {

                fields_map.get(col_name).setType(java_type_name);

            } else {

                FieldInfo explicit_field = new FieldInfo(field_names_mode, java_type_name, col_name);

                fields.add(explicit_field);

                fields_map.put(col_name, explicit_field);
            }
        }
    }

    private void _refine_fields_by_table_metadata(final String table_name, Map<String, FieldInfo> fields_map) throws Exception {

        if (!SqlUtils.is_table_ref(table_name)) {

            throw new Exception("Table name expected: " + table_name);
        }

        DatabaseMetaData md = conn.getMetaData();

        String[] parts = table_name.split("\\.", -1); // -1 to leave empty strings

        ResultSet rs_columns;

        if (parts.length == 1) {

            rs_columns = md.getColumns(null, null, table_name, "%");

        } else {

            String schema_nm = table_name.substring(0, table_name.lastIndexOf('.'));

            String table_nm = parts[parts.length - 1];

            rs_columns = md.getColumns(null, schema_nm, table_nm, "%");
        }

        try {

            while (rs_columns.next()) {

                String col_name = rs_columns.getString("COLUMN_NAME");

                if (fields_map.containsKey(col_name)) {

                    int type = rs_columns.getInt("DATA_TYPE");

                    String java_type_name = TypesMapping.getJavaBySqlType(type);

                    fields_map.get(col_name).setType(java_type_name);
                }
            }

        } finally {

            rs_columns.close();
        }
    }

    public void get_dto_field_info(DtoClass jaxb_dto_class, String sql_root_abs_path, List<FieldInfo> fields) throws Exception {

        JaxbUtils.validate_jaxb_dto_class(jaxb_dto_class);

        fields.clear();

        Map<String, FieldInfo> fields_map = new HashMap<String, FieldInfo>();

        _get_dto_class_field_info(jaxb_dto_class, sql_root_abs_path, fields_map, fields);
    }

    private void _get_fields_info(PreparedStatement ps, List<FieldInfo> fields) throws Exception {

        fields.clear();

        // PostgreSQL:
        // Initial SQL is '{call get_tests(4)}'; ps is CallableStatement.
        // ps.toString() returns something like 'select * from get_tests(4) as result'
        // and ps.getMetaData() throws SQLException.
        //
        ResultSetMetaData rsmd;

        try {

            // String prepared = ps.toString();
            //
            rsmd = ps.getMetaData();

        } catch (SQLException ex) {

            rsmd = null;
        }

        // it is null if no columns or _prepare was called instead of _prepare_call for SP
        //
        if (rsmd == null) {

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
            //
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

            String col_name = _get_column_name(rsmd, i);

            String col_class_name = _get_column_type_name(rsmd, i);

            if (type_map != null) {

                col_class_name = Helpers.get_cpp_class_name_from_java_class_name(type_map, col_class_name);
            }

            fields.add(new FieldInfo(field_names_mode, col_class_name, col_name));
        }
    }

    private static void _get_params_info(
            PreparedStatement ps, FieldNamesMode param_names_mode, TypeMap type_map,
            String[] method_param_descriptors, List<FieldInfo> params) {

        params.clear();

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
            //
            ParameterMetaData pm = ps.getParameterMetaData();

            int params_count;

            try {

                params_count = pm.getParameterCount();

            } catch (SQLException e) {

                params_count = 0;
            }

            if (method_param_descriptors == null && params_count > 0) {

                throw new SQLException("Specified parameters count: 0. Detected parameters count: " + params_count);
            }

            if (method_param_descriptors != null && params_count != method_param_descriptors.length) {

                throw new SQLException("Specified parameters count: " + method_param_descriptors.length
                        + ". Detected parameters count: " + params_count);
            }

            if (method_param_descriptors == null) {

                return;
            }

            for (int i = 0; i < params_count; i++) {

                String param_descriptor = method_param_descriptors[i];

                String param_type_name;

                String param_name;

                String[] parts = Helpers.parse_param_descriptor(param_descriptor);

                if (parts[0] == null) {

                    param_type_name = _get_param_type_name(pm, i);

                    param_name = param_descriptor;

                } else {

                    param_type_name = parts[0];

                    param_name = parts[1];
                }

                if (type_map != null) {

                    param_type_name = Helpers.get_cpp_class_name_from_java_class_name(type_map, param_type_name);
                }

                params.add(new FieldInfo(param_names_mode, param_type_name, param_name));
            }

        } catch (Exception ex) {

            if (method_param_descriptors == null) {

                return;
            }

            for (String param_descriptor : method_param_descriptors) {

                String param_type_name;

                String param_name;

                String[] parts = Helpers.parse_param_descriptor(param_descriptor);

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

                params.add(new FieldInfo(param_names_mode, param_type_name, param_name));
            }
        }
    }

    public static ResultSet get_tables_rs(
            Connection conn, DatabaseMetaData db_info, String schema_name,
            boolean include_views) throws SQLException {

        String[] types;

        if (include_views) {

            types = new String[]{"TABLE", "VIEW"};

        } else {

            types = new String[]{"TABLE"};
        }

        ResultSet rs_tables;

        String catalog = conn.getCatalog();

        rs_tables = db_info.getTables(catalog, schema_name, "%", types);

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

    /////////////////////////////////////////////////////////////////////////////
    //
    // Free-SQL

    public String get_dao_query_info(
            String sql_root_abs_path, String dao_jaxb_ref, String dto_param_type,
            String[] method_param_descriptors, String jaxb_dto_or_return_type, boolean jaxb_return_type_is_dto,
            DtoClasses jaxb_dto_classes, List<FieldInfo> _fields, List<FieldInfo> _params) throws Exception {

        _fields.clear();

        _params.clear();

        Helpers.check_duplicates(method_param_descriptors);

        if (SqlUtils.is_table_ref(dao_jaxb_ref)) {

            throw new Exception("Specifying table name as a value 'ref' is not allowed in <query...");
        }

        String dao_query_jdbc_sql = SqlUtils.jdbc_sql_by_dto_class_ref(dao_jaxb_ref, sql_root_abs_path);

        PreparedStatement ps = _prepare_jdbc_sql(dao_query_jdbc_sql);

        try {

            // For CALL statement, columns count is always 0. Even if the stored procedure
            // returns ResultSet (MySQL).
            // For statement 'select my_func(?)', columns count is also 0 (at least in
            // PostgreSQL).
            // Seems like Exception should be thrown only if columns cannot be detected
            // neither from DAO nor from DTO.

            if (jaxb_return_type_is_dto) {

                List<FieldInfo> dto_fields = new ArrayList<FieldInfo>();

                Map<String, FieldInfo> dto_fields_map = new HashMap<String, FieldInfo>();

                DtoClass jaxb_dto_class = JaxbUtils.find_jaxb_dto_class(jaxb_dto_or_return_type, jaxb_dto_classes);

                _get_dto_class_field_info(jaxb_dto_class, sql_root_abs_path, dto_fields_map, dto_fields);

                ////////////////////////////////////////////////

                List<FieldInfo> dao_fields = new ArrayList<FieldInfo>();

                _get_fields_info(ps, dao_fields);

                // no fields from DAO SQL (e.g. it may happen for CALL statement?). just use fields of DTO class
                //
                if (dao_fields.size() == 0) {

                    _fields.addAll(dto_fields);

                } else {

                    for (FieldInfo fi : dao_fields) {

                        String dao_col_name = fi.getColumnName();

                        if (dto_fields_map.containsKey(dao_col_name)) {

                            _fields.add(dto_fields_map.get(dao_col_name));
                        }
                    }
                }

                if (_fields.size() == 0) {

                    throw new Exception("Columns count is 0. Is SQL statement valid?");
                }

            } else { // jaxb_return_type_is_dto == false

                _get_fields_info(ps, _fields);

                String ret_type_name;

                if (jaxb_dto_or_return_type != null && jaxb_dto_or_return_type.trim().length() > 0) {

                    ret_type_name = jaxb_dto_or_return_type;

                } else {
                    //
                    // fields.size() == 0 for SQL statement 'select inventory_in_stock(?)'
                    // from MySQL sakila example
                    //
                    if (_fields.size() == 0) {

                        // throw new Exception("Columns count is < 1 . Is SQL statement valid?");
                        //
                        ret_type_name = Object.class.getName();

                    } else {

                        ret_type_name = _fields.get(0).getType();
                    }
                }

                // add single field

                if (_fields.isEmpty()) {

                    _fields.add(new FieldInfo(field_names_mode, ret_type_name, "ret_value"));

                } else {

                    _fields.get(0).setType(ret_type_name);
                }
            }

            if (type_map != null) {

                for (FieldInfo fi : _fields) {

                    fi.setType(Helpers.get_cpp_class_name_from_java_class_name(type_map, fi.getType()));
                }
            }

            /////////////////////////////
            //
            // if it is something like <query method="get_some_value(MyDTO(m_id, m_date))", use field_names_mode (???)
            //
            FieldNamesMode param_names_mode = dto_param_type == null || dto_param_type.length() == 0 ? FieldNamesMode.AS_IS : field_names_mode;

            _get_params_info(ps, param_names_mode, type_map, method_param_descriptors, _params);

        } finally {

            ps.close();
        }

        return dao_query_jdbc_sql;
    }

    public void get_dao_exec_dml_info(
            String dao_jdbc_sql, String dto_param_type,
            String[] method_param_descriptors, List<FieldInfo> params) throws Exception {

        Helpers.check_duplicates(method_param_descriptors);

        PreparedStatement ps = _prepare_jdbc_sql(dao_jdbc_sql);

        try {

            /////////////////////////////
            //
            // if it is something like <query method="get_some_value(MyDTO(m_id, m_date))", use field_names_mode (???)
            //
            FieldNamesMode param_names_mode = dto_param_type == null || dto_param_type.length() == 0 ? FieldNamesMode.AS_IS : field_names_mode;

            _get_params_info(ps, param_names_mode, type_map, method_param_descriptors, params);

        } finally {

            ps.close();
        }
    }

    /////////////////////////////////////////////////////////////////////////////
    //
    // CRUD

    public void get_dao_crud_create_info(
            DtoClass jaxb_dto_class, String sql_root_abs_path, String dao_crud_table_name, String dao_crud_generated,
            List<FieldInfo> not_ai_fields, List<FieldInfo> ai_fields) throws Exception {

        ai_fields.clear();

        not_ai_fields.clear();

        HashSet<String> dao_crud_generated_set = new HashSet<String>();

        if (!("*".equals(dao_crud_generated))) {

            String[] gen_keys_arr = Helpers.get_listed_items(dao_crud_generated);

            Helpers.check_duplicates(gen_keys_arr);

            for (String k : gen_keys_arr) {

                dao_crud_generated_set.add(k.toLowerCase());
            }
        }

        /////////////////////////////////////////

        List<FieldInfo> dto_fields = new ArrayList<FieldInfo>();

        Map<String, FieldInfo> dto_fields_map = new HashMap<String, FieldInfo>();

        _get_dto_class_field_info(jaxb_dto_class, sql_root_abs_path, dto_fields_map, dto_fields);

        /////////////////////////////////////////

        List<FieldInfo> dao_fields = new ArrayList<FieldInfo>();
        {
            String jdbc_sql = SqlUtils.jdbc_sql_by_dto_class_ref(dao_crud_table_name, sql_root_abs_path);

            Map<String, FieldInfo> dao_fields_map = new HashMap<String, FieldInfo>();

            _get_fields_map_by_jdbc_sql(jdbc_sql, dao_fields_map, dao_fields);

            _refine_fields_by_table_metadata(dao_crud_table_name, dao_fields_map);
        }

        /////////////////////////////////////////

        for (FieldInfo dao_field : dao_fields) {

            String dao_col_name = dao_field.getColumnName();

            if (dto_fields_map.containsKey(dao_col_name) == false) {

                throw new Exception("DTO column '" + dao_col_name + "' not found");
            }

            dao_field.setType(dto_fields_map.get(dao_col_name).getType());

            String gen_dao_col_name = dao_col_name.toLowerCase();

            if (dao_crud_generated_set.contains(gen_dao_col_name)) {

                dao_field.setAutoIncrement(true);

                ai_fields.add(dao_field);

                dao_crud_generated_set.remove(gen_dao_col_name); // it must become empty in the end

            } else {

                if (dao_field.isAutoIncrement()) {

                    ai_fields.add(dao_field);

                } else {

                    not_ai_fields.add(dao_field);
                }
            }
        }

        if (dao_crud_generated_set.size() > 0) { // not processed column names remain!

            throw new SQLException("Unknown columns are listed as 'generated': " + dao_crud_generated_set.toString());
        }
    }

    public String get_dao_crud_read_sql(
            String dao_table_name, boolean fetch_list, String explicit_pk,
            List<String> pk_col_names) throws Exception {

        String dao_jdbc_sql = "select * from " + dao_table_name;

        if (!fetch_list) {

            List<FieldInfo> pk_fields = new ArrayList<FieldInfo>();

            _get_table_fields(dao_table_name, null, null, explicit_pk, pk_fields);

            if (pk_fields.isEmpty()) {

                return null;

            } else {

                for (FieldInfo fi : pk_fields) {

                    pk_col_names.add(fi.getColumnName());
                }
            }

            dao_jdbc_sql += " where " + pk_col_names.get(0) + " = ?";

            for (int i = 1; i < pk_col_names.size(); i++) {

                dao_jdbc_sql += " and " + pk_col_names.get(i) + " = ?";
            }
        }

        return dao_jdbc_sql;
    }

    public void get_dao_crud_update_info(
            String dao_table_name,
            List<FieldInfo> not_pk_fields, String explicit_pk, List<FieldInfo> pk_fields,
            String dto_class_name, DtoClasses jaxb_dto_classes) throws Exception {

        not_pk_fields.clear();

        pk_fields.clear();

        _get_table_fields(dao_table_name, null, not_pk_fields, explicit_pk, pk_fields);

        /////////////////////////////////////////////////////////////

        // TODO: refine
        // DtoClass jaxb_dto_class = JaxbUtils.find_jaxb_dto_class(dto_class_name, jaxb_dto_classes);

    }

    public void get_dao_crud_delete_info(
            String dao_table_name, String explicit_pk, List<FieldInfo> pk_fields,
            String dto_class_name, DtoClasses jaxb_dto_classes) throws Exception {

        pk_fields.clear();

        _get_table_fields(dao_table_name, null, null, explicit_pk, pk_fields);

        /////////////////////////////////////////////////////////////

        // TODO: refine
        // DtoClass jaxb_dto_class = JaxbUtils.find_jaxb_dto_class(dto_class_name, jaxb_dto_classes);

    }
}
