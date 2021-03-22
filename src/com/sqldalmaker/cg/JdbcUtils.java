/*
 * Copyright 2011-2021 sqldalmaker@gmail.com
 * Read LICENSE.txt in the root of this project/archive.
 * Project web-site: http://sqldalmaker.sourceforge.net
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
    private final FieldNamesMode dto_field_names_mode;
    private final FieldNamesMode method_params_names_mode;

    private final TypeMapManager type_map;

    public JdbcUtils(Connection conn, FieldNamesMode field_names_mode, FieldNamesMode method_params_names_mode, TypeMap type_map) throws Exception {
        this.conn = conn;
        this.dto_field_names_mode = field_names_mode;
        this.method_params_names_mode = method_params_names_mode;
        this.type_map = new TypeMapManager(type_map);
    }

    public FieldNamesMode get_dto_field_names_mode() {
        return this.dto_field_names_mode;
    }

    private static String _get_jdbc_column_name(ResultSetMetaData rsmd, int col) throws Exception {
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
            throw new Exception("Column name cannot be detected. Try to specify column label. For example, 'SELECT COUNT(*) AS RES...'");
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
            return conn.prepareStatement(jdbc_sql);
        }
    }

    private static String _get_jdbc_column_type_name(ResultSetMetaData rsmd, int i) {
        try {
            // sometime, it returns "[B": See comments for Class.getName() API
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
            // 1) getParameterClassName throws exception in mysql-connector-java-5.1.17-bin.jar:
            // 2) sometime it returns "[B": See comments for Class.getName() API
            java_class_name = pm.getParameterClassName(i + 1);
            java_class_name = Helpers.process_java_type_name(java_class_name);
        } catch (Exception ex) {
            java_class_name = Object.class.getName();
        }
        return java_class_name;
    }

    private Map<String, FieldInfo> _get_table_field_info(final String table_name, String explicit_pk,
                                                         List<FieldInfo> _fields_all,
                                                         List<FieldInfo> _fields_not_pk,
                                                         List<FieldInfo> _fields_pk) throws Exception {
        if (_fields_all != null) {
            _fields_all.clear();
        }
        if (_fields_not_pk != null) {
            _fields_not_pk.clear();
        }
        if (_fields_pk != null) {
            _fields_pk.clear();
        }
        Set<String> pk_col_names_set_lower_case;
        if ("*".equals(explicit_pk)) {
            List<String> pk_col_names = new ArrayList<String>();
            pk_col_names_set_lower_case = new HashSet<String>();
            // DatabaseMetaData.getPrimaryKeys returns pk_col_names in lower case. For other
            // JDBC drivers, it may differ. To ensure correct comparison of field names, do
            // it always in lower case
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
        } else { // if PK are specified explicitely, don't use getPrimaryKeys at all
            String[] gen_keys_arr = Helpers.get_listed_items(explicit_pk, false);
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
                FieldInfo fi = new FieldInfo(dto_field_names_mode, type_name, col_name, "t(" + col_name + ")");
                fi.setAutoIncrement(rsmd.isAutoIncrement(i));
                if (pk_col_names_set_lower_case.contains(col_name.toLowerCase())) {
                    if (_fields_pk != null) {
                        _fields_pk.add(fi);
                    }
                } else {
                    if (_fields_not_pk != null) {
                        _fields_not_pk.add(fi);
                    }
                }
                if (_fields_all != null) {
                    _fields_all.add(fi);
                }
                fields_map.put(col_name, fi);
            }
        } finally {
            ps.close();
        }
        _refine_field_info_by_table_meta_data(table_name, fields_map);
        if (type_map.is_defined()) {
            for (FieldInfo fi : fields_map.values()) {
                String type_name = fi.getType();
                type_name = type_map.get_rendered_type_name(type_name);
                fi.set_type_by_map(type_name);
            }
        }
        return fields_map;
    }

    private void _get_fields_map_by_jdbc_sql(String jdbc_sql, Map<String, FieldInfo> _fields_map, List<FieldInfo> _fields, StringBuilder _error) throws Exception {
        _fields.clear();
        _fields_map.clear();
        PreparedStatement ps = _prepare_jdbc_sql(jdbc_sql);
        try {
            ResultSetMetaData rsmd;
            try {
                rsmd = ps.getMetaData(); // throws SQLException;
            } catch (Throwable th) {
                _error.append("Trying to obtain MetaData for this SQL throws: ");
                _error.append(th.getMessage());
                return;
            }
            // ps.getMetaData():
            // @return the description of a <code>ResultSet</code> object's columns or
            // <code>null</code> if the driver cannot return a <code>ResultSetMetaData</code> object
            if (rsmd == null) {
                _error.append("Cannot obtain MetaData for this SQL");
                return;
            }
            int column_count;
            try {
                column_count = rsmd.getColumnCount(); // throws SQLException;
                if (column_count < 1) {
                    _error.append("Cannot obtain column count for this SQL");
                    _error.append(column_count);
                }
            } catch (Throwable e) {
                _error.append("Trying to obtain column count for this SQL throws: ");
                _error.append(e.getMessage());
                return;
            }
            for (int i = 1; i <= column_count; i++) {
                String col_name = _get_jdbc_column_name(rsmd, i);
                // considers "[B", etc.
                String java_type_name = _get_jdbc_column_type_name(rsmd, i);
                FieldInfo field = new FieldInfo(dto_field_names_mode, java_type_name, col_name, "q(" + col_name + ")");
                if (rsmd.isAutoIncrement(i)) {
                    field.setAutoIncrement(true);
                } else {
                    field.setAutoIncrement(false);
                }
                _fields_map.put(col_name, field);
                _fields.add(field);
            }
        } finally {
            ps.close();
        }
    }

    private Map<String, FieldInfo> _get_dto_fields(DtoClass jaxb_dto_class, String sql_root_abs_path,
                                                   List<FieldInfo> _dto_fields) throws Exception {
        _dto_fields.clear();
        Map<String, FieldInfo> dto_fields_map = new HashMap<String, FieldInfo>();
        String jaxb_dto_ref = jaxb_dto_class.getRef();
        if (!SqlUtils.is_empty_ref(jaxb_dto_ref)) {
            String jdbc_sql = SqlUtils.jdbc_sql_by_dto_class_ref(jaxb_dto_ref, sql_root_abs_path);
            StringBuilder error = new StringBuilder();
            _get_fields_map_by_jdbc_sql(jdbc_sql, dto_fields_map, _dto_fields, error);
            if (error.length() > 0) {
                error.append("\r\n");
                error.append(jdbc_sql);
                throw new Exception(error.toString());
            }
            if (SqlUtils.is_table_ref(jaxb_dto_ref)) {
                String table_name = jaxb_dto_ref;
                _refine_field_info_by_table_meta_data(table_name, dto_fields_map);
            } else if (SqlUtils.is_sql_shortcut_ref(jaxb_dto_ref)) {
                String[] parts = SqlUtils.parse_sql_shortcut_ref(jaxb_dto_ref);
                String table_name = parts[0];
                _refine_field_info_by_table_meta_data(table_name, dto_fields_map);
            }
        }
        // field types may be redefined in <field type=...
        List<DtoClass.Field> jaxb_fields = jaxb_dto_class.getField();
        _refine_field_types_by_jaxb(jaxb_fields, dto_fields_map, _dto_fields);
        if (_dto_fields.isEmpty()) {
            String msg = "Cannot detect the fields for <dto-class name=\"" + jaxb_dto_class.getName() + "\"...";
            if (!SqlUtils.is_empty_ref(jaxb_dto_ref)) {
                msg += "\r\nCheck if the value of 'ref' is valid:\r\n" + jaxb_dto_ref;
            }
            throw new Exception(msg);
        }
        if (type_map.is_defined()) {
            for (FieldInfo fi : _dto_fields) {
                String type_name = fi.getType();
                type_name = type_map.get_rendered_type_name(type_name);
                fi.set_type_by_map(type_name);
            }
        }
        return dto_fields_map;
    }

    private void _refine_field_types_by_jaxb(List<DtoClass.Field> jaxb_explicit_fields,
                                             Map<String, FieldInfo> fields_map,
                                             List<FieldInfo> fields) throws Exception {
        if (jaxb_explicit_fields == null) {
            return;
        }
        for (DtoClass.Field jaxb_explicit_field : jaxb_explicit_fields) {
            String col_name = jaxb_explicit_field.getColumn();
            String type_name = jaxb_explicit_field.getType();
            if (fields_map.containsKey(col_name)) {
                fields_map.get(col_name).refine_type(type_name);
            } else {
                FieldInfo explicit_field = new FieldInfo(dto_field_names_mode, type_name, col_name, "xml(" + col_name + ")");
                fields.add(explicit_field);
                fields_map.put(col_name, explicit_field);
            }
        }
    }

    private void _refine_field_info_by_table_meta_data(final String table_name, Map<String, FieldInfo> fields_map) throws Exception {
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
                String db_col_name = rs_columns.getString("COLUMN_NAME");
                if (fields_map.containsKey(db_col_name)) {
                    int type = rs_columns.getInt("DATA_TYPE");
                    String java_type_name = TypesMapping.getJavaBySqlType(type);
                    FieldInfo fi = fields_map.get(db_col_name);
                    fi.refine_type(java_type_name);
                    fi.setComment("t(" + db_col_name + ")");
                }
            }
        } finally {
            rs_columns.close();
        }
    }

    private void _get_free_sql_params_info(String dao_jdbc_sql, FieldNamesMode param_names_mode,
                                           String[] method_param_descriptors, List<FieldInfo> params) throws Exception {
        Helpers.check_duplicates(method_param_descriptors);
        PreparedStatement ps = _prepare_jdbc_sql(dao_jdbc_sql);
        try {
            _get_free_sql_params_info(ps, param_names_mode, method_param_descriptors, params);
        } finally {
            ps.close();
        }
    }

    //
    // _get_free_sql_params_info should not be used for CRUD
    //
    private void _get_free_sql_params_info(PreparedStatement ps, FieldNamesMode param_names_mode,
                                           String[] method_param_descriptors, List<FieldInfo> _params) throws Exception {
        if (method_param_descriptors == null) {
            method_param_descriptors = new String[]{};
        }
        _params.clear();
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
        int jdbc_params_count;
        ParameterMetaData pm = null;
        try {
            // MS SQL Server: getParameterMetaData throws exception for SF without params
            pm = ps.getParameterMetaData();
            try {
                jdbc_params_count = pm.getParameterCount();
            } catch (Throwable e) { // including AbstractMethodError, SQLServerException, etc.
                _get_params_by_descriptors(param_names_mode, method_param_descriptors, _params);
                return;
            }
        } catch (Throwable e) { // including AbstractMethodError, SQLServerException, etc.
            jdbc_params_count = 0;
        }
        int not_cb_params_count = 0;
        for (int i = 0; i < method_param_descriptors.length; i++) {
            String param_descriptor = method_param_descriptors[i].trim();
            if (param_descriptor.startsWith("[")) {
                // implicit cursor callbacks
                if (param_descriptor.endsWith("]") == false) {
                    throw new Exception("Ending ']' expected");
                }
            } else {
                String[] parts = method_param_descriptors[i].split(":");
                if (parts.length == 1) {
                    not_cb_params_count++;
                } else {
                    String type = parts[1];
                    parts = type.split("\\s+");
                    // if the part after ':' contains spaces, than it is not on_test:Test
                    if (parts.length > 1) {
                        not_cb_params_count++;
                    }
                }
                String default_param_type_name = _get_jdbc_param_type_name(pm, i);
                FieldInfo pi = _create_param_info(param_names_mode, param_descriptor, default_param_type_name);
                _params.add(pi);
            }
        }
        if (jdbc_params_count != not_cb_params_count) {
            throw new Exception("Parameters declared in method: " + method_param_descriptors.length
                    + ", detected by MetaData: " + jdbc_params_count);
        }
    }

    private void _get_params_by_descriptors(FieldNamesMode param_names_mode, String[] method_param_descriptors, List<FieldInfo> _params) throws Exception {
        _params.clear();
        for (int i = 0; i < method_param_descriptors.length; i++) {
            String param_descriptor = method_param_descriptors[i];
            FieldInfo pi = _create_param_info(param_names_mode, param_descriptor, Object.class.getName());
            _params.add(pi);
        }
    }

    private FieldInfo _create_param_info(FieldNamesMode param_names_mode, String param_descriptor, String default_param_type_name) throws Exception {
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
        param_type_name = type_map.get_rendered_type_name(param_type_name);
        return new FieldInfo(param_names_mode, param_type_name, param_name, "parameter");
    }

    private void _refine_dao_fields_by_dto_fields(DtoClass jaxb_dto_class, String sql_root_abs_path, List<FieldInfo> dao_fields_all) throws Exception {
        List<FieldInfo> dto_fields = new ArrayList<FieldInfo>();
        Map<String, FieldInfo> dto_fields_map = _get_dto_fields(jaxb_dto_class, sql_root_abs_path, dto_fields);
        for (FieldInfo dao_fi : dao_fields_all) {
            String dao_col_name = dao_fi.getColumnName();
            if (dto_fields_map.containsKey(dao_col_name) == false) {
                throw new Exception("DAO column '" + dao_col_name
                        + "' not found among DTO columns [" + _get_column_names(dto_fields) + "]. Ensure lower/upper case.");
            }
            FieldInfo dto_fi = dto_fields_map.get(dao_col_name);
            String dto_col_type_name = dto_fi.getType();
            if (!dto_col_type_name.equals(Object.class.getTypeName())) {
                dao_fi.set_type_by_map(dto_col_type_name);
            }
            String dto_comment = dto_fi.getComment();
            String dao_comment = dao_fi.getComment();
            dao_fi.setComment(dto_comment + " <- " + dao_comment);
        }
    }

    private void _refine_dao_fields_by_dto_fields_for_crud_create(Map<String, FieldInfo> dto_fields_map,
                                                                  HashSet<String> dao_crud_generated_set,
                                                                  List<FieldInfo> dao_fields_all,
                                                                  List<FieldInfo> _dao_fields_not_generated,
                                                                  List<FieldInfo> _dao_fields_generated) throws Exception {
        for (FieldInfo dao_field : dao_fields_all) {
            String dao_col_name = dao_field.getColumnName();
            if (dto_fields_map.containsKey(dao_col_name) == false) {
                throw new Exception("Cannot create mapping for DAO column '" + dao_col_name + "'. Ensure lower/upper case.");
            }
            dao_field.set_type_by_map(dto_fields_map.get(dao_col_name).getType());
            String gen_dao_col_name = dao_col_name.toLowerCase();
            if (dao_crud_generated_set.contains(gen_dao_col_name)) {
                dao_field.setAutoIncrement(true);
                _dao_fields_generated.add(dao_field);
                dao_crud_generated_set.remove(gen_dao_col_name); // it must become empty in the end
            } else {
                if (dao_field.isAutoIncrement()) {
                    _dao_fields_generated.add(dao_field);
                } else {
                    _dao_fields_not_generated.add(dao_field);
                }
            }
        }
        if (dao_crud_generated_set.size() > 0) { // not processed column names remain!
            throw new SQLException("Unknown columns are listed as 'generated': " + dao_crud_generated_set.toString());
        }
    }

    private static FieldInfo _get_ret_field_info(FieldNamesMode field_names_mode, String exlicit_ret_type, List<FieldInfo> dao_fields) throws Exception {
        String ret_type_name;
        if (exlicit_ret_type != null && exlicit_ret_type.trim().length() > 0) {
            ret_type_name = exlicit_ret_type;
        } else {
            if (dao_fields.isEmpty()) {
                // MySQL sakila example: dao_fields.isEmpty() for 'select inventory_in_stock(?)'
                ret_type_name = Object.class.getName();
            } else {
                ret_type_name = dao_fields.get(0).getType();
            }
        }
        String ret_col_name;
        if (dao_fields.isEmpty()) {
            // MySQL sakila example: dao_fields.isEmpty() for 'select inventory_in_stock(?)'
            ret_col_name = "ret_value";
        } else {
            ret_col_name = dao_fields.get(0).getName();
        }
        return new FieldInfo(field_names_mode, ret_type_name, ret_col_name, "ret-value");
    }

    private void _get_sql_shortcut_info(String sql_root_abs_path, String dao_jaxb_ref,
                                        String[] method_param_descriptors, FieldNamesMode param_names_mode,
                                        String jaxb_dto_or_return_type, boolean jaxb_return_type_is_dto, DtoClasses jaxb_dto_classes,
                                        List<FieldInfo> _fields, List<FieldInfo> _params) throws Exception {
        String[] parts = SqlUtils.parse_sql_shortcut_ref(dao_jaxb_ref);
        String dao_table_name = parts[0];
        String explicit_keys = parts[1];
        List<FieldInfo> dao_all_fields = new ArrayList<FieldInfo>();
        List<FieldInfo> dao_key_fields = new ArrayList<>();
        _get_table_field_info(dao_table_name, explicit_keys, dao_all_fields, null, dao_key_fields);
        // assign param types from table!! without dto-refinement!!!
        if (method_param_descriptors.length != dao_key_fields.size()) {
            throw new Exception("Invalid SQL-shortcut. Keys declared: " + method_param_descriptors.length + ", keys expected: " + dao_key_fields.size());
        }
        for (int i = 0; i < method_param_descriptors.length; i++) {
            String param_descriptor = method_param_descriptors[i];
            String default_param_type_name = dao_key_fields.get(i).getType();
            FieldInfo pi = _create_param_info(param_names_mode, param_descriptor, default_param_type_name);
            _params.add(pi);
        }
        if (jaxb_return_type_is_dto) {
            DtoClass jaxb_dto_class = JaxbUtils.find_jaxb_dto_class(jaxb_dto_or_return_type, jaxb_dto_classes);
            _refine_dao_fields_by_dto_fields(jaxb_dto_class, sql_root_abs_path, dao_all_fields);
            _fields.addAll(dao_all_fields);
        } else {
            _fields.add(_get_ret_field_info(dto_field_names_mode, jaxb_dto_or_return_type, dao_all_fields));
        }
        if (_fields.size() > 0) {
            String comment = _fields.get(0).getComment();
            _fields.get(0).setComment(comment + " [INFO] SQL-shortcut");
        }
    }

    private void _get_free_sql_info(String sql_root_abs_path, String dao_query_jdbc_sql,
                                    String[] method_param_descriptors, FieldNamesMode param_names_mode,
                                    String jaxb_dto_or_return_type, boolean jaxb_return_type_is_dto, DtoClasses jaxb_dto_classes,
                                    List<FieldInfo> _fields, List<FieldInfo> _params) throws Exception {
        Map<String, FieldInfo> dao_fields_map = new HashMap<String, FieldInfo>();
        List<FieldInfo> dao_fields = new ArrayList<FieldInfo>();
        StringBuilder error = new StringBuilder();
        _get_fields_map_by_jdbc_sql(dao_query_jdbc_sql, dao_fields_map, dao_fields, error);
        // Columns count is 0:
        // 1) for 'call my_sp(...)' including SP returning ResultSet (MySQL).
        // 2) for 'begin ?:=my_udf_rc(...); end;' (Oracle).
        // 3) for 'select my_func(?)' (PostgreSQL). etc.
        if (jaxb_return_type_is_dto) {
            List<FieldInfo> dto_fields = new ArrayList<FieldInfo>();
            DtoClass jaxb_dto_class = JaxbUtils.find_jaxb_dto_class(jaxb_dto_or_return_type, jaxb_dto_classes);
            Map<String, FieldInfo> dto_fields_map = _get_dto_fields(jaxb_dto_class, sql_root_abs_path, dto_fields);
            // no fields from DAO SQL (e.g. for CALL statement). just use fields of DTO class:
            if (dao_fields.isEmpty()) {
                _fields.addAll(dto_fields);
                if (error.length() > 0 && _fields.size() > 0) {
                    String comment = _fields.get(0).getComment();
                    _fields.get(0).setComment(comment + " [INFO] " + error.toString().trim().replace('\r', ' ').replace('\n', ' '));
                }
            } else {
                if (ResultSet.class.getName().equals(dao_fields.get(0).getType())) {
                    // the story about PostgreSQL + 'select * from get_tests_by_rating_rc(?)' (UDF returning REFCURSOR)
                    _fields.addAll(dto_fields);
                    String comment = _fields.get(0).getComment() + " [INFO] Column 0 is of type ResultSet";
                    if (error.length() > 0) {
                        comment += ", " + error.toString();
                    }
                    _fields.get(0).setComment(comment);
                } else {
                    for (FieldInfo dao_fi : dao_fields) {
                        String dao_col_name = dao_fi.getColumnName();
                        if (dto_fields_map.containsKey(dao_col_name)) {
                            FieldInfo dto_fi = dto_fields_map.get(dao_col_name);
                            dto_fi.setComment(dto_fi.getComment() + " <- " + dao_fi.getComment());
                            _fields.add(dto_fi);
                        }
                    }
                }
            }
            if (_fields.isEmpty()) {
                String msg = _get_mapping_error_msg(dto_fields, dao_fields);
                throw new Exception(msg);
            }
        } else { // jaxb_return_type_is_dto == false
            _fields.add(_get_ret_field_info(dto_field_names_mode, jaxb_dto_or_return_type, dao_fields));
        }
        _get_free_sql_params_info(dao_query_jdbc_sql, param_names_mode, method_param_descriptors, _params);
    }

    private String _get_column_names(List<FieldInfo> fields) {
        List<String> col_names = new ArrayList<String>();
        for (FieldInfo fi : fields) {
            col_names.add(fi.getColumnName());
        }
        return String.join(", ", col_names);
    }

    private String _get_mapping_error_msg(List<FieldInfo> dto_fields, List<FieldInfo> dao_fields) {
        return "DAO columns [" + _get_column_names(dao_fields)
                + "] not found among DTO columns [" + _get_column_names(dto_fields) + "].";
    }

    /////////////////////////////////////////////////////////////////////////////
    //
    // Utils
    //
    public static ResultSet get_tables_rs(Connection conn, DatabaseMetaData db_info, String schema_name, boolean include_views) throws SQLException {
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

    public static void get_schema_names(Connection con, List<String> _schema_names) throws SQLException {
        DatabaseMetaData db_info = con.getMetaData();
        ResultSet rs;
        rs = db_info.getSchemas();
        try {
            while (rs.next()) {
                _schema_names.add(rs.getString("TABLE_SCHEM"));
            }
        } finally {
            rs.close();
        }
    }

    /////////////////////////////////////////////////////////////////////////////
    //
    // DTO
    //
    public void get_dto_field_info(DtoClass jaxb_dto_class, String sql_root_abs_path, List<FieldInfo> _fields) throws Exception {
        JaxbUtils.validate_jaxb_dto_class(jaxb_dto_class);
        _fields.clear();
        _get_dto_fields(jaxb_dto_class, sql_root_abs_path, _fields);
    }

    private FieldNamesMode _refine_method_params_names_mode(String dto_param_type) {
        // if it is something like <query method="get_some_value(MyDTO(m_id, m_date))", use field_names_mode (???)
        FieldNamesMode mode = dto_param_type == null || dto_param_type.length() == 0
                ? FieldNamesMode.AS_IS : method_params_names_mode;
        return mode;
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
        FieldNamesMode param_names_mode = _refine_method_params_names_mode(dto_param_type);
        /*if (SqlUtils.is_table_ref(dao_jaxb_ref)) {
            throw new Exception("Table name as a value of 'ref' is not allowed in <query...");
        } else*/
        if (SqlUtils.is_sql_shortcut_ref(dao_jaxb_ref)) {
            _get_sql_shortcut_info(sql_root_abs_path, dao_jaxb_ref, method_param_descriptors, param_names_mode,
                    jaxb_dto_or_return_type, jaxb_return_type_is_dto, jaxb_dto_classes, _fields, _params);
        } else {
            _get_free_sql_info(sql_root_abs_path, dao_query_jdbc_sql, method_param_descriptors, param_names_mode,
                    jaxb_dto_or_return_type, jaxb_return_type_is_dto, jaxb_dto_classes, _fields, _params);
        }
        if (type_map.is_defined()) {
            for (FieldInfo fi : _fields) {
                String type_name = type_map.get_rendered_type_name(fi.getType());
                fi.set_type_by_map(type_name);
            }
        }
        return dao_query_jdbc_sql;
    }

    public void get_dao_exec_dml_info(String dao_jdbc_sql, String dto_param_type, String[] method_param_descriptors,
                                      List<FieldInfo> _params) throws Exception {
        FieldNamesMode param_names_mode = _refine_method_params_names_mode(dto_param_type);
        _get_free_sql_params_info(dao_jdbc_sql, param_names_mode, method_param_descriptors, _params);
    }

    /////////////////////////////////////////////////////////////////////////////
    //
    // DAO. CRUD
    //
    public String get_dao_crud_create_info(DtoClass jaxb_dto_class, String sql_root_abs_path, String dao_table_name,
                                           String dao_crud_generated,
                                           List<FieldInfo> _dao_fields_not_generated, List<FieldInfo> _dao_fields_generated) throws Exception {
        _dao_fields_not_generated.clear();
        _dao_fields_generated.clear();
        HashSet<String> dao_crud_generated_set = new HashSet<String>();
        if (!("*".equals(dao_crud_generated))) {
            String[] gen_keys_arr = Helpers.get_listed_items(dao_crud_generated, false);
            Helpers.check_duplicates(gen_keys_arr);
            for (String k : gen_keys_arr) {
                dao_crud_generated_set.add(k.toLowerCase());
            }
        }
        List<FieldInfo> dto_fields = new ArrayList<FieldInfo>();
        Map<String, FieldInfo> dto_fields_map = _get_dto_fields(jaxb_dto_class, sql_root_abs_path, dto_fields);
        List<FieldInfo> dao_fields_all = new ArrayList<FieldInfo>();
        // "" because pk columns are not meaningful for crud create, only generated ones:
        _get_table_field_info(dao_table_name, "", dao_fields_all, null, null);
        _refine_dao_fields_by_dto_fields_for_crud_create(dto_fields_map, dao_crud_generated_set, dao_fields_all, _dao_fields_not_generated, _dao_fields_generated);
        return SqlUtils.create_crud_create_sql(dao_table_name, _dao_fields_not_generated);
    }

    public String get_dao_crud_read_info(boolean fetch_list, DtoClass jaxb_dto_class, String sql_root_abs_path,
                                         String dao_table_name, String explicit_pk,
                                         List<FieldInfo> _dao_fields_all, List<FieldInfo> _dao_fields_pk) throws Exception {
        _dao_fields_all.clear();
        _dao_fields_pk.clear();
        _get_table_field_info(dao_table_name, explicit_pk, _dao_fields_all, null, _dao_fields_pk);
        _refine_dao_fields_by_dto_fields(jaxb_dto_class, sql_root_abs_path, _dao_fields_all);
        return SqlUtils.create_crud_read_sql(dao_table_name, _dao_fields_pk, fetch_list);
    }

    public String get_dao_crud_update_info(String dao_table_name, DtoClass jaxb_dto_class, String sql_root_abs_path, String explicit_pk,
                                           List<FieldInfo> _fields_not_pk,
                                           List<FieldInfo> _fields_pk) throws Exception {
        _fields_not_pk.clear();
        _fields_pk.clear();
        List<FieldInfo> fields_all = new ArrayList<FieldInfo>();
        _get_table_field_info(dao_table_name, explicit_pk, fields_all, _fields_not_pk, _fields_pk);
        if (_fields_not_pk.isEmpty()) {
            return null; // just render info comment instead of method
        }
        if (_fields_pk.isEmpty()) {
            return null; // just render info comment instead of method
        }
        _refine_dao_fields_by_dto_fields(jaxb_dto_class, sql_root_abs_path, fields_all);
        return SqlUtils.create_crud_update_sql(dao_table_name, _fields_not_pk, _fields_pk);
    }

    public String get_dao_crud_delete_info(String dao_table_name, String explicit_pk, List<FieldInfo> _fields_pk) throws Exception {
        _fields_pk.clear();
        _get_table_field_info(dao_table_name, explicit_pk, null, null, _fields_pk);
        if (_fields_pk.isEmpty()) {
            return null; // just render info comment instead of method
        }
        return SqlUtils.create_crud_delete_sql(dao_table_name, _fields_pk);
    }
}
