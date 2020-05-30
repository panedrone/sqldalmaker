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

    private static String _get_jdbc_column_name(ResultSetMetaData rsmd, int col) throws Exception {

        // H2: col. names are duplicated for SQL like'SELECT a as a1, a as a2 FROM...'
        //
        String column_name;

        try {

            column_name = rsmd.getColumnLabel(col);

        } catch (SQLException e) {

            column_name = null;
        }

        if (column_name == null || column_name.length() == 0) {

            column_name = rsmd.getColumnName(col);
        }

        if (column_name == null) {

            throw new Exception(
                    "Column name cannot be detected. Try to specify column label. For example, 'SELECT COUNT(*) AS RES...'");
        }

        if (column_name.length() == 0) {

            column_name = "col_" + col; // MS SQL Server: column_name == "" for  'select dbo.ufnLeadingZeros(?)'
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

        if (s != null) {

            s.close();
        }
    }

    private PreparedStatement _prepare_by_table_name(String table_name) throws SQLException {

        return conn.prepareStatement("select * from " + table_name + " where 1 = 0");
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

    private static String _get_jdbc_column_type_name(ResultSetMetaData rsmd, int i) {

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

    private static String _get_jdbc_param_type_name(ParameterMetaData pm, int i) {

        if (pm == null) {

            return Object.class.getName();
        }

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

    private Map<String, FieldInfo> _get_table_field_info(final String table_name, List<FieldInfo> fields_all,
            List<FieldInfo> not_fields_pk, String explicit_pk, List<FieldInfo> fields_pk) throws Exception {

        if (fields_all != null) {

            fields_all.clear();
        }

        if (not_fields_pk != null) {

            not_fields_pk.clear();
        }

        if (fields_pk != null) {

            fields_pk.clear();
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

            // DatabaseMetaData.getPrimaryKeys may return pk_col_names in lower case
            // (SQLite3). For other JDBC drivers, it may differ. To ensure correct
            // comparison of field names, do it always in lower case
            //
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

            ResultSetMetaData rsmd = ps.getMetaData();

            if (rsmd == null) {

                // jTDS returns null for invalid SQL
                throw new Exception("PreparedStatement.getMetaData returns null for '" + table_name + "");
            }

            int column_count = rsmd.getColumnCount();

            for (int i = 1; i <= column_count; i++) {

                String col_name = _get_jdbc_column_name(rsmd, i);

                String type_name = _get_jdbc_column_type_name(rsmd, i);

                if (type_map != null) {

                    type_name = Helpers.get_cpp_class_name_from_java_class_name(type_map, type_name);
                }

                FieldInfo fi = new FieldInfo(field_names_mode, type_name, col_name);

                fi.setAutoIncrement(rsmd.isAutoIncrement(i));

                if (pk_col_names_set_lower_case.contains(col_name.toLowerCase())) {

                    if (fields_pk != null) {

                        fields_pk.add(fi);
                    }

                } else {

                    if (not_fields_pk != null) {

                        not_fields_pk.add(fi);
                    }
                }

                if (fields_all != null) {

                    fields_all.add(fi);
                }

                fields_map.put(col_name, fi);
            }

        } finally {

            ps.close();
        }

        _refine_fields_by_table_metadata(table_name, fields_map);

        return fields_map;
    }

    private void _get_fields_map_by_jdbc_sql(String jdbc_sql, Map<String, FieldInfo> fields_map, List<FieldInfo> fields)
            throws Exception {

        fields.clear();

        fields_map.clear();

        PreparedStatement ps = _prepare_jdbc_sql(jdbc_sql);

        try {

            ResultSetMetaData rsmd;

            try {

                rsmd = ps.getMetaData();

            } catch (Throwable th) {

                return; // === panedrone: just to avoid interruption and allow to obtain column info in other way
            }

            // === panedrone: it may return null for CALL in MySQL
            if (rsmd == null) {

                return;
            }

            int column_count = rsmd.getColumnCount();

            for (int i = 1; i <= column_count; i++) {

                String col_name = _get_jdbc_column_name(rsmd, i);

                // considers "[B", etc.
                //
                String java_type_name = _get_jdbc_column_type_name(rsmd, i);

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

    private Map<String, FieldInfo> _get_dto_class_field_info(DtoClass jaxb_dto_class, String sql_root_abs_path,
            List<FieldInfo> fields) throws Exception {

        fields.clear();

        Map<String, FieldInfo> fields_map = new HashMap<String, FieldInfo>();

        String dto_ref = jaxb_dto_class.getRef();

        if (!SqlUtils.is_empty_ref(dto_ref)) {

            String jdbc_sql = SqlUtils.jdbc_sql_by_dto_class_ref(dto_ref, sql_root_abs_path);

            _get_fields_map_by_jdbc_sql(jdbc_sql, fields_map, fields);

            if (SqlUtils.is_table_ref(dto_ref)) {

                _refine_fields_by_table_metadata(dto_ref, fields_map);

            } else if (SqlUtils.is_sql_shortcut_ref(dto_ref)) {

                String[] parts = SqlUtils.parse_sql_shortcut_ref(dto_ref);

                String table_name = parts[0];

                // obtain fields the same way like in the case of table. params will be obtained
                // from DAO SQL.
                // params are obtained below (considering DAO SQL and method_param_descriptors)
                //
                _refine_fields_by_table_metadata(table_name, fields_map);
            }
        }

        _refine_field_info_by_jaxb_explicit_fields(jaxb_dto_class.getField(), fields_map, fields);

        if (type_map != null) {

            for (FieldInfo fi : fields) {

                fi.setType(Helpers.get_cpp_class_name_from_java_class_name(type_map, fi.getType()));
            }
        }

        return fields_map;
    }

    private void _refine_field_info_by_jaxb_explicit_fields(List<DtoClass.Field> jaxb_explicit_fields,
            Map<String, FieldInfo> fields_map, List<FieldInfo> fields) {

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

    private void _refine_fields_by_table_metadata(final String table_name, Map<String, FieldInfo> fields_map)
            throws Exception {

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

    private void _get_free_sql_params_info(
            String dao_jdbc_sql, FieldNamesMode param_names_mode,
            String[] method_param_descriptors, List<FieldInfo> params) throws Exception {

        Helpers.check_duplicates(method_param_descriptors);

        PreparedStatement ps = _prepare_jdbc_sql(dao_jdbc_sql);

        try {

            _get_free_sql_params_info(ps, param_names_mode, type_map, method_param_descriptors, params);

        } finally {

            ps.close();
        }
    }

    //
    // _get_free_sql_params_info should not be used for CRUD
    //
    private static void _get_free_sql_params_info(PreparedStatement ps, FieldNamesMode param_names_mode, TypeMap type_map,
            String[] method_param_descriptors, List<FieldInfo> params) throws Exception {

        if (method_param_descriptors == null) {

            method_param_descriptors = new String[]{};
        }

        params.clear();

        // Sybase ADS + adsjdbc.jar:
        // ------------------------
        // ps.getParameterMetaData() throws java.lang.AbstractMethodError for all
        // statements
        //
        // SQL Server 2008 + sqljdbc4.jar:
        // -------------------------------
        // ps.getParameterMetaData() throws
        // com.microsoft.sqlserver.jdbc.SQLServerException for
        // some statements SQL with parameters like 'SELECT count(*) FROM orders o WHERE
        // o_date BETWEEN ? AND ?'
        // and for SQL statements without parameters
        //
        // PostgeSQL:
        // ----------
        // ps.getParameterMetaData() throws SQLException for both PreparedStatement and
        // CallableStatement
        //
        int params_count;

        ParameterMetaData pm = null;

        try {

            // MS SQL Server: getParameterMetaData throws exception for SF without params
            //
            pm = ps.getParameterMetaData();

            try {

                params_count = pm.getParameterCount();

            } catch (Throwable e) { // including AbstractMethodError, SQLServerException, etc.

                _get_params_info_by_descriptors(param_names_mode, type_map,
                        method_param_descriptors, params);

                return;
            }
            
        } catch (Throwable e) { // including AbstractMethodError, SQLServerException, etc.

            params_count = 0;
        }

        if (params_count != method_param_descriptors.length) {

            throw new Exception("Parameters count expected: " + method_param_descriptors.length
                    + ", detected: " + params_count);
        }

        for (int i = 0; i < method_param_descriptors.length; i++) {

            String param_descriptor = method_param_descriptors[i];

            String default_param_type_name = _get_jdbc_param_type_name(pm, i);

            FieldInfo pi = _create_param_info(param_names_mode, type_map, param_descriptor, default_param_type_name);

            params.add(pi);
        }
    }

    private static void _get_params_info_by_descriptors(FieldNamesMode param_names_mode, TypeMap type_map,
            String[] method_param_descriptors, List<FieldInfo> params) {

        for (int i = 0; i < method_param_descriptors.length; i++) {

            String param_descriptor = method_param_descriptors[i];

            FieldInfo pi = _create_param_info(param_names_mode, type_map, param_descriptor, Object.class.getName());

            params.add(pi);
        }
    }

    private static FieldInfo _create_param_info(
            FieldNamesMode param_names_mode, TypeMap type_map,
            String param_descriptor, String default_param_type_name) {

        String param_type_name;

        String param_name;

        String[] parts = Helpers.parse_param_descriptor(param_descriptor);

        if (parts[0] == null) {

            param_type_name = default_param_type_name;

            param_name = param_descriptor;

        } else {

            param_type_name = parts[0];

            param_name = parts[1];
        }

        if (type_map != null) {

            param_type_name = Helpers.get_cpp_class_name_from_java_class_name(type_map, param_type_name);
        }

        return new FieldInfo(param_names_mode, param_type_name, param_name);
    }

    private void _refine_dao_fields_by_dto_fields(DtoClass jaxb_dto_class, String sql_root_abs_path,
            List<FieldInfo> dao_fields_all) throws Exception {

        List<FieldInfo> dto_fields = new ArrayList<FieldInfo>();

        Map<String, FieldInfo> dto_fields_map = _get_dto_class_field_info(jaxb_dto_class, sql_root_abs_path,
                dto_fields);

        ////////////////////////////////////////////////
        //
        for (FieldInfo dao_fi : dao_fields_all) {

            String dao_col_name = dao_fi.getColumnName();

            if (dto_fields_map.containsKey(dao_col_name) == false) {

                throw new Exception(
                        "Cannot map DTO and DAO. DAO column '" + dao_col_name + "'. Ensure lower/upper case.");
            }

            FieldInfo dto_fi = dto_fields_map.get(dao_col_name);

            String dto_col_type_name = dto_fi.getType();

            if (!dto_col_type_name.equals(Object.class.getTypeName())) {

                dao_fi.setType(dto_col_type_name);
            }
        }
    }

    private static FieldInfo _get_ret_field_info(FieldNamesMode field_names_mode, String exlicit_ret_type, List<FieldInfo> dao_fields) {

        String ret_type_name;

        if (exlicit_ret_type != null && exlicit_ret_type.trim().length() > 0) {

            ret_type_name = exlicit_ret_type;

        } else {

            if (dao_fields.isEmpty()) {
                //
                // MySQL sakila example: dao_fields.isEmpty() for 'select inventory_in_stock(?)'
                //
                ret_type_name = Object.class.getName();

            } else {

                ret_type_name = dao_fields.get(0).getType();
            }
        }

        String ret_col_name;

        if (dao_fields.isEmpty()) {
            //
            // MySQL sakila example: dao_fields.isEmpty() for 'select inventory_in_stock(?)'
            //
            ret_col_name = "ret_value";

        } else {

            ret_col_name = dao_fields.get(0).getName();
        }

        return new FieldInfo(field_names_mode, ret_type_name, ret_col_name);
    }

    /////////////////////////////////////////////////////////////////////////////
    //
    // Utils
    //
    public static ResultSet get_tables_rs(Connection conn, DatabaseMetaData db_info, String schema_name,
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
    // DTO
    //
    public void get_dto_field_info(DtoClass jaxb_dto_class, String sql_root_abs_path, List<FieldInfo> fields)
            throws Exception {

        JaxbUtils.validate_jaxb_dto_class(jaxb_dto_class);

        fields.clear();

        _get_dto_class_field_info(jaxb_dto_class, sql_root_abs_path, fields);
    }

    /////////////////////////////////////////////////////////////////////////////
    //
    // DAO. Free-SQL
    //
    public String get_dao_query_info(String sql_root_abs_path, String dao_jaxb_ref, String dto_param_type,
            String[] method_param_descriptors, String jaxb_dto_or_return_type, boolean jaxb_return_type_is_dto,
            DtoClasses jaxb_dto_classes, List<FieldInfo> _fields, List<FieldInfo> _params) throws Exception {

        _fields.clear();

        _params.clear();

        Helpers.check_duplicates(method_param_descriptors);

        String dao_query_jdbc_sql = SqlUtils.jdbc_sql_by_query_ref(dao_jaxb_ref, sql_root_abs_path);

        /////////////////////////////
        //
        // if it is something like <query method="get_some_value(MyDTO(m_id, m_date))",
        // use field_names_mode (???)
        //
        FieldNamesMode param_names_mode = dto_param_type == null || dto_param_type.length() == 0
                ? FieldNamesMode.AS_IS
                : field_names_mode;

        if (SqlUtils.is_table_ref(dao_jaxb_ref)) {

            throw new Exception("Table name as a value of 'ref' is not allowed in <query...");

        } else if (SqlUtils.is_sql_shortcut_ref(dao_jaxb_ref)) {

            String[] parts = SqlUtils.parse_sql_shortcut_ref(dao_jaxb_ref);

            String dao_table_name = parts[0];

            String explicit_keys = parts[1];

            List<FieldInfo> dao_fields = new ArrayList<FieldInfo>();

            List<FieldInfo> dao_key_fields = new ArrayList<>();

            _get_table_field_info(dao_table_name, dao_fields, null, explicit_keys, dao_key_fields);

            /////////////////////////////////////////////////
            // assign param types from table!! without dto-refinement!!!
            //
            if (method_param_descriptors.length != dao_key_fields.size()) {

                throw new Exception("Invalid SQL-shortcut. Keys declared: " + method_param_descriptors.length + ", keys expected: " + dao_key_fields.size());
            }

            for (int i = 0; i < method_param_descriptors.length; i++) {

                String param_descriptor = method_param_descriptors[i];

                String default_param_type_name = dao_key_fields.get(i).getType();

                FieldInfo pi = _create_param_info(param_names_mode, type_map, param_descriptor, default_param_type_name);

                _params.add(pi);
            }

            /////////////////////////////////////////////////
            //
            if (jaxb_return_type_is_dto) {

                DtoClass jaxb_dto_class = JaxbUtils.find_jaxb_dto_class(jaxb_dto_or_return_type, jaxb_dto_classes);

                _refine_dao_fields_by_dto_fields(jaxb_dto_class, sql_root_abs_path, dao_fields);

                _fields.addAll(dao_fields);

            } else {

                _fields.add(_get_ret_field_info(field_names_mode, jaxb_dto_or_return_type, dao_fields));
            }

        } else {

            Map<String, FieldInfo> fields_map = new HashMap<String, FieldInfo>();

            List<FieldInfo> dao_fields = new ArrayList<FieldInfo>();

            _get_fields_map_by_jdbc_sql(dao_query_jdbc_sql, fields_map, dao_fields);

            // For CALL statement, columns count is always 0. Even if the stored procedure
            // returns ResultSet (MySQL).
            // For statement 'select my_func(?)', columns count is also 0 (at least in
            // PostgreSQL).
            // Seems like Exception should be thrown only if columns cannot be detected
            // neither from DAO nor from DTO.
            //
            if (jaxb_return_type_is_dto) {

                List<FieldInfo> dto_fields = new ArrayList<FieldInfo>();

                DtoClass jaxb_dto_class = JaxbUtils.find_jaxb_dto_class(jaxb_dto_or_return_type, jaxb_dto_classes);

                Map<String, FieldInfo> dto_fields_map = _get_dto_class_field_info(jaxb_dto_class, sql_root_abs_path,
                        dto_fields);

                // no fields from DAO SQL (e.g. it may happen for CALL statement?). just use
                // fields of DTO class
                //
                if (dao_fields.isEmpty()) {

                    _fields.addAll(dto_fields);

                } else {

                    for (FieldInfo fi : dao_fields) {

                        String dao_col_name = fi.getColumnName();

                        if (dto_fields_map.containsKey(dao_col_name)) {

                            _fields.add(dto_fields_map.get(dao_col_name));
                        }
                    }
                }

                if (_fields.isEmpty()) {

                    List<String> dao_col_names = new ArrayList<String>();

                    for (FieldInfo fi : dao_fields) {

                        dao_col_names.add(fi.getColumnName());
                    }

                    List<String> dto_col_names = new ArrayList<String>();

                    for (FieldInfo fi : dto_fields) {

                        dto_col_names.add(fi.getColumnName());
                    }

                    throw new Exception("DAO columns [" + String.join(", ", dao_col_names) + "] were not found among DTO columns ["
                            + String.join(", ", dto_col_names) + "]. Is SQL statement valid?");
                }

            } else { // jaxb_return_type_is_dto == false

                _fields.add(_get_ret_field_info(field_names_mode, jaxb_dto_or_return_type, dao_fields));
            }

            /////////////////////////////
            //
            _get_free_sql_params_info(dao_query_jdbc_sql, param_names_mode, method_param_descriptors, _params);
        }

        if (type_map != null) {

            for (FieldInfo fi : _fields) {

                fi.setType(Helpers.get_cpp_class_name_from_java_class_name(type_map, fi.getType()));
            }
        }

        return dao_query_jdbc_sql;
    }

    public void get_dao_exec_dml_info(String dao_jdbc_sql, String dto_param_type, String[] method_param_descriptors,
            List<FieldInfo> _params) throws Exception {

        /////////////////////////////
        //
        // if it is something like <query method="get_some_value(MyDTO(m_id, m_date))",
        ///////////////////////////// use field_names_mode (???)
        //
        FieldNamesMode param_names_mode = dto_param_type == null || dto_param_type.length() == 0
                ? FieldNamesMode.AS_IS
                : field_names_mode;

        _get_free_sql_params_info(dao_jdbc_sql, param_names_mode, method_param_descriptors, _params);
    }

    /////////////////////////////////////////////////////////////////////////////
    //
    // DAO. CRUD
    //
    public String get_dao_crud_create_info(DtoClass jaxb_dto_class, String sql_root_abs_path, String dao_table_name,
            String dao_crud_generated, List<FieldInfo> dao_fields_not_ai, List<FieldInfo> dao_fields_ai)
            throws Exception {

        dao_fields_not_ai.clear();

        dao_fields_ai.clear();

        /////////////////////////////////////////
        //
        HashSet<String> dao_crud_generated_set = new HashSet<String>();

        if (!("*".equals(dao_crud_generated))) {

            String[] gen_keys_arr = Helpers.get_listed_items(dao_crud_generated);

            Helpers.check_duplicates(gen_keys_arr);

            for (String k : gen_keys_arr) {

                dao_crud_generated_set.add(k.toLowerCase());
            }
        }

        /////////////////////////////////////////
        //
        List<FieldInfo> dto_fields = new ArrayList<FieldInfo>();

        Map<String, FieldInfo> dto_fields_map = _get_dto_class_field_info(jaxb_dto_class, sql_root_abs_path,
                dto_fields);

        /////////////////////////////////////////
        //
        List<FieldInfo> dao_fields_all = new ArrayList<FieldInfo>();

        _get_table_field_info(dao_table_name, dao_fields_all, null, "", null);

        for (FieldInfo dao_field : dao_fields_all) {

            String dao_col_name = dao_field.getColumnName();

            if (dto_fields_map.containsKey(dao_col_name) == false) {

                throw new Exception(
                        "Cannot map DTO and DAO. DAO column '" + dao_col_name + "'. Ensure lower/upper case.");
            }

            dao_field.setType(dto_fields_map.get(dao_col_name).getType());

            String gen_dao_col_name = dao_col_name.toLowerCase();

            if (dao_crud_generated_set.contains(gen_dao_col_name)) {

                dao_field.setAutoIncrement(true);

                dao_fields_ai.add(dao_field);

                dao_crud_generated_set.remove(gen_dao_col_name); // it must become empty in the end

            } else {

                if (dao_field.isAutoIncrement()) {

                    dao_fields_ai.add(dao_field);

                } else {

                    dao_fields_not_ai.add(dao_field);
                }
            }
        }

        if (dao_crud_generated_set.size() > 0) { // not processed column names remain!

            throw new SQLException("Unknown columns are listed as 'generated': " + dao_crud_generated_set.toString());
        }

        return SqlUtils.create_crud_create_sql(dao_table_name, dao_fields_not_ai);
    }

    public String get_dao_crud_read_info(boolean fetch_list, DtoClass jaxb_dto_class, String sql_root_abs_path,
            String dao_table_name, String explicit_pk, List<FieldInfo> fields_all, List<FieldInfo> fields_pk)
            throws Exception {

        fields_all.clear();

        fields_pk.clear();

        _get_table_field_info(dao_table_name, fields_all, null, explicit_pk, fields_pk);

        _refine_dao_fields_by_dto_fields(jaxb_dto_class, sql_root_abs_path, fields_all);

        return SqlUtils.create_crud_read_sql(dao_table_name, fields_pk, fetch_list);
    }

    public String get_dao_crud_update_info(String dao_table_name, List<FieldInfo> not_fields_pk, String explicit_pk,
            List<FieldInfo> fields_pk, DtoClass jaxb_dto_class, String sql_root_abs_path) throws Exception {

        not_fields_pk.clear();

        fields_pk.clear();

        List<FieldInfo> fields_all = new ArrayList<FieldInfo>();

        _get_table_field_info(dao_table_name, fields_all, not_fields_pk, explicit_pk, fields_pk);

        if (not_fields_pk.isEmpty()) {

            return null; // just render info comment instead of method
        }

        if (fields_pk.isEmpty()) {

            return null; // just render info comment instead of method
        }

        _refine_dao_fields_by_dto_fields(jaxb_dto_class, sql_root_abs_path, fields_all);

        return SqlUtils.create_crud_update_sql(dao_table_name, not_fields_pk, fields_pk);
    }

    public String get_dao_crud_delete_info(String dao_table_name, String explicit_pk, List<FieldInfo> fields_pk)
            throws Exception {

        fields_pk.clear();

        _get_table_field_info(dao_table_name, null, null, explicit_pk, fields_pk);

        if (fields_pk.isEmpty()) {

            return null; // just render info comment instead of method
        }

        return SqlUtils.create_crud_delete_sql(dao_table_name, fields_pk);
    }
}
