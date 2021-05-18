/*
    Copyright 2011-2021 sqldalmaker@gmail.com
    SQL DAL Maker Website: http://sqldalmaker.sourceforge.net
    Read LICENSE.txt in the root of this project/archive for details.
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
    private final JaxbUtils.JaxbTypeMap type_map;

    public JdbcUtils(
            Connection conn,
            FieldNamesMode field_names_mode,
            FieldNamesMode method_params_names_mode,
            TypeMap type_map) throws Exception {

        this.conn = conn;
        this.dto_field_names_mode = field_names_mode;
        this.method_params_names_mode = method_params_names_mode;
        this.type_map = new JaxbUtils.JaxbTypeMap(type_map);
    }

    public FieldNamesMode get_dto_field_names_mode() {
        return this.dto_field_names_mode;
    }

    private String _jdbc_sql_by_table_name(String table_name) {
        return "select * from " + table_name + " where 1 = 0";
    }

    private PreparedStatement _prepare_jdbc_sql(String jdbc_sql) throws SQLException {
        boolean is_sp = SqlUtils.is_jdbc_stored_proc_call(jdbc_sql);
        if (is_sp) {
            return conn.prepareCall(jdbc_sql);
        } else {
            // For MySQL, prepareStatement doesn't throw Exception for
            // invalid SQL statements and doesn't return null as well
            return conn.prepareStatement(jdbc_sql);
        }
    }

    private static ResultSetMetaData _get_rs_md(PreparedStatement ps) throws Exception {
        ResultSetMetaData rsmd;
        try {
            rsmd = ps.getMetaData();
        } catch (SQLException e) {
            throw new Exception("Exception in getMetaData " + e.getMessage());
        }
        if (rsmd == null) {  // it is possible by javadocs
            throw new Exception("getMetaData() == null");
        }
        return rsmd;
    }

    private static int _get_col_count(ResultSetMetaData rsmd) throws Exception {
        int column_count;
        try {
            column_count = rsmd.getColumnCount();
        } catch (SQLException e) {
            throw new Exception("Exception in getColumnCount(): " + e.getMessage());
        }
        if (column_count < 1) {
            // Columns count is 0:
            // 1) for 'call my_sp(...)' including SP returning ResultSet (MySQL).
            // 2) for 'begin ?:=my_udf_rc(...); end;' (Oracle).
            // 3) for 'select my_func(?)' (PostgreSQL). etc.
            throw new Exception("getColumnCount() == " + column_count);
        }
        return column_count;
    }

    private void _get_field_info_by_jdbc_sql(
            String jdbc_sql,
            Map<String, FieldInfo> _fields_map,
            List<FieldInfo> _fields) throws Exception {

        PreparedStatement ps = _prepare_jdbc_sql(jdbc_sql);
        try {
            ResultSetMetaData rsmd = _get_rs_md(ps);
            int column_count = _get_col_count(rsmd);
            _fields.clear();
            _fields_map.clear();
            for (int i = 1; i <= column_count; i++) {
                String col_name = _get_jdbc_col_name(rsmd, i);
                String type_name = _get_jdbc_col_type_name(rsmd, i);
                FieldInfo field = new FieldInfo(dto_field_names_mode, type_name, col_name, "q(" + col_name + ")");
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

    private class TableFieldInfo {

        final List<FieldInfo> fields_all = new ArrayList<FieldInfo>();
        final List<FieldInfo> fields_not_pk = new ArrayList<FieldInfo>();
        final List<FieldInfo> fields_pk = new ArrayList<FieldInfo>();
        final Map<String, FieldInfo> fields_map = new HashMap<String, FieldInfo>();

        TableFieldInfo(final String table_name, String explicit_pk) throws Exception {
            _init_by_jdbc_sql(table_name, explicit_pk);
            _refine_field_info_by_jdbc_table(table_name, fields_map);
            _refine_by_type_map();
        }

        private void _init_by_jdbc_sql(String table_name, String explicit_pk) throws Exception {
            String jdbc_sql = _jdbc_sql_by_table_name(table_name);
            _get_field_info_by_jdbc_sql(jdbc_sql, fields_map, fields_all);
            Set<String> lower_case_pk_col_names = _get_lower_case_pk_col_names(table_name, explicit_pk);
            for (FieldInfo fi : fields_all) {
                String col_name = fi.getColumnName();
                String lower_case_col_name = _get_pk_col_name_alias(col_name);
                if (lower_case_pk_col_names.contains(lower_case_col_name)) {
                    fields_pk.add(fi);
                } else {
                    fields_not_pk.add(fi);
                }
            }
        }

        private void _refine_by_type_map() throws Exception {
            if (!type_map.is_defined()) {
                return;
            }
            for (FieldInfo fi : fields_map.values()) {
                String type_name = fi.calc_target_type_name();
                type_name = type_map.get_target_type_name(type_name);
                fi.set_target_type_by_map(type_name);
            }
        }

        private Set<String> _get_lower_case_pk_col_names(String table_name, String explicit_pk) throws Exception {
            if ("*".equals(explicit_pk)) {
                return _get_pk_col_name_aliases_from_table(table_name);
            }
            return _get_pk_col_name_aliaces_from_jaxb(explicit_pk);
        }

        private ResultSet _get_pk_rs(DatabaseMetaData md, String table_name) throws Exception {
            if (table_name.contains(".")) {
                String[] parts = table_name.split("\\.");
                if (parts.length != 2) {
                    throw new Exception("Unexpected table name: '" + table_name + "'");
                }
                return md.getPrimaryKeys(null, parts[0], parts[1]);
            }
            return md.getPrimaryKeys(null, null, table_name);
        }

        private Set<String> _get_pk_col_name_aliases_from_table(String table_name) throws Exception {
            DatabaseMetaData md = conn.getMetaData(); // no close() method
            ResultSet rs = _get_pk_rs(md, table_name);
            try {
                Set<String> res = new HashSet<String>();
                while (rs.next()) {
                    String pk_col_name = rs.getString("COLUMN_NAME");
                    String pk_col_name_alias = _get_pk_col_name_alias(pk_col_name);
                    if (res.contains(pk_col_name_alias)) {
                        throw new Exception("Duplickated PK column name alias: " + pk_col_name_alias);
                    }
                    res.add(pk_col_name_alias);
                }
                return res;
            } finally {
                rs.close();
            }
        }

        private String _get_pk_col_name_alias(String pk_col_name) {
            // === panederone: WHY ALIASES:
            //   1) xerial SQLite3: getPrimaryKeys may return pk_col_names in lower case
            //      For other JDBC drivers, it may differ.
            //   2) xerial SQLite3 returns pk_col_names in the format
            //     '[employeeid] asc' (compound PK)
            pk_col_name = pk_col_name.toLowerCase().replace("[", "").replace("]", "").trim();
            if (pk_col_name.endsWith(" asc")) {
                pk_col_name = pk_col_name.split(" asc")[0];
            }
            if (pk_col_name.endsWith(" desc")) {
                pk_col_name = pk_col_name.split(" desc")[1];
            }
            pk_col_name = pk_col_name.trim();
            return pk_col_name;
        }

        private Set<String> _get_pk_col_name_aliaces_from_jaxb(String explicit_pk) throws Exception {
            // if PK are specified explicitely, don't use getPrimaryKeys at all
            String[] gen_keys_arr = Helpers.get_listed_items(explicit_pk, false);
            Helpers.check_duplicates(gen_keys_arr);
            for (int i = 0; i < gen_keys_arr.length; i++) {
                gen_keys_arr[i] = _get_pk_col_name_alias(gen_keys_arr[i].toLowerCase());
            }
            return new HashSet<String>(Arrays.asList(gen_keys_arr));
        }

    } // class TableFieldInfo

    private static String _get_jdbc_col_name(ResultSetMetaData rsmd, int col_num) throws Exception {
        String column_name;
        try {
            column_name = rsmd.getColumnLabel(col_num);
        } catch (SQLException e) {
            column_name = null;
        }
        if (column_name == null || column_name.length() == 0) {
            column_name = rsmd.getColumnName(col_num);
        }
        if (column_name == null) {
            throw new Exception(
                    "Cannot detect column name. Try to specify column label like 'select count(*) as res from ...'");
        }
        if (column_name.length() == 0) {
            column_name = "col_" + col_num; // MS SQL Server: column_name == "" for 'select dbo.ufnLeadingZeros(?)'
        }
        return column_name;
    }

    private static String _get_jdbc_col_type_name(ResultSetMetaData rsmd, int i) {
        try {
            // sometime, it returns "[B": See comments for Class.getName() API
            String java_class_name = rsmd.getColumnClassName(i);
            return Helpers.process_java_type_name(java_class_name);
        } catch (Exception ex) {
            return Object.class.getName();
        }
    }

    private String _get_jdbc_param_type_name(ParameterMetaData pm, int i) {
        if (pm == null) {
            return Object.class.getName();
        }
        String java_class_name;
        try {
            // 1) getParameterClassName throws exception in
            // mysql-connector-java-5.1.17-bin.jar:
            // 2) sometime it returns "[B": See comments for Class.getName() API
            java_class_name = pm.getParameterClassName(i + 1);
            java_class_name = Helpers.process_java_type_name(java_class_name);
        } catch (Exception ex) {
            java_class_name = Object.class.getName();
        }
        return java_class_name;
    }

    private void _get_param_info_by_descriptors(
            FieldNamesMode param_names_mode,
            String[] method_param_descriptors,
            List<FieldInfo> _params) throws Exception {

        _params.clear();
        for (String param_descriptor : method_param_descriptors) {
            FieldInfo pi = _create_param_info(param_names_mode, param_descriptor, Object.class.getName());
            _params.add(pi);
        }
    }

    private void _get_param_info_for_sql_shortcut(
            FieldNamesMode param_names_mode,
            String[] method_param_descriptors,
            List<FieldInfo> fields_pk,
            List<FieldInfo> _params) throws Exception {

        // assign param types from table!! without dto-refinement!!!
        if (method_param_descriptors.length != fields_pk.size()) {
            throw new Exception("Invalid SQL-shortcut. Keys declared: " + method_param_descriptors.length
                    + ", keys expected: " + fields_pk.size());
        }
        for (int i = 0; i < method_param_descriptors.length; i++) {
            String param_descriptor = method_param_descriptors[i];
            String default_param_type_name = fields_pk.get(i).calc_target_type_name();
            FieldInfo pi = _create_param_info(param_names_mode, param_descriptor, default_param_type_name);
            _params.add(pi);
        }
    }

    private FieldInfo _create_param_info(
            FieldNamesMode param_names_mode,
            String param_descriptor,
            String default_param_type_name) throws Exception {

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
        param_type_name = type_map.get_target_type_name(param_type_name);
        return new FieldInfo(param_names_mode, param_type_name, param_name, "parameter");
    }

    private void _init_by_jaxb_dto_class_ref(
            DtoClass jaxb_dto_class,
            String sql_root_abs_path,
            List<FieldInfo> _dto_fields,
            Map<String, FieldInfo> dto_fields_map) throws Exception {

        String jaxb_dto_ref = jaxb_dto_class.getRef();
        String jdbc_sql = SqlUtils.jdbc_sql_by_dto_class_ref(jaxb_dto_ref, sql_root_abs_path);
        _get_field_info_by_jdbc_sql(jdbc_sql, dto_fields_map, _dto_fields);
        if (SqlUtils.is_table_ref(jaxb_dto_ref)) {
            String table_name = jaxb_dto_ref;
            _refine_field_info_by_jdbc_table(table_name, dto_fields_map);
        } else if (SqlUtils.is_sql_shortcut_ref(jaxb_dto_ref)) {
            String[] parts = SqlUtils.parse_sql_shortcut_ref(jaxb_dto_ref);
            String table_name = parts[0];
            _refine_field_info_by_jdbc_table(table_name, dto_fields_map);
        }
    }

    private Map<String, FieldInfo> _get_dto_fields(
            DtoClass jaxb_dto_class,
            String sql_root_abs_path,
            List<FieldInfo> _dto_fields) throws Exception {

        _dto_fields.clear();
        Map<String, FieldInfo> dto_fields_map = new HashMap<String, FieldInfo>();
        String jaxb_dto_ref = jaxb_dto_class.getRef();
        if (!SqlUtils.is_empty_ref(jaxb_dto_ref)) {
            _init_by_jaxb_dto_class_ref(jaxb_dto_class, sql_root_abs_path, _dto_fields, dto_fields_map);
        }
        _refine_field_types_by_jaxb(jaxb_dto_class, dto_fields_map, _dto_fields);
        if (type_map.is_defined()) {
            for (FieldInfo fi : _dto_fields) {
                String type_name = fi.calc_target_type_name();
                type_name = type_map.get_target_type_name(type_name);
                fi.set_target_type_by_map(type_name);
            }
        }
        return dto_fields_map;
    }

    private void _refine_field_info_by_jaxb(
            DtoClass jaxb_dto_class,
            Map<String, FieldInfo> fields_map,
            List<FieldInfo> fields) throws Exception {

        List<DtoClass.Field> jaxb_fields = jaxb_dto_class.getField();
        if (jaxb_fields == null) {
            return;
        }
        for (DtoClass.Field jaxb_field : jaxb_fields) {
            String jaxb_field_col_name = jaxb_field.getColumn();
            String jaxb_field_type_name = jaxb_field.getType();
            if (fields_map.containsKey(jaxb_field_col_name)) {
                FieldInfo fi = fields_map.get(jaxb_field_col_name);
                fi.assign_jaxb_type(jaxb_field_type_name);
            } else {
                FieldInfo fi = new FieldInfo(dto_field_names_mode, jaxb_field_type_name, jaxb_field_col_name,
                        "xml(" + jaxb_field_col_name + ")");
                fields.add(fi);
                fields_map.put(jaxb_field_col_name, fi);
            }
        }
    }

    private interface IMacro {
        String exec();
    }

    private void _refine_field_type_comments(
            DtoClass jaxb_dto_class,
            List<FieldInfo> fields) throws Exception {

        final String field_comment_template = jaxb_dto_class.getFieldComment();
        if (field_comment_template == null) {
            return;
        }
        for (FieldInfo fi : fields) {
            String type_name = fi.getType();
            if (type_name.length() == 0) {
                throw new Exception("<field type=... is empty");
            }
            String type_comment = fi.type_comment_from_jaxb_type_name();
            final String col_nm = fi.getColumnName();
            if (type_comment.length() != 0) {
                continue;
            }
            String field_comment = field_comment_template;
            Map<String, IMacro> macro = new HashMap<String, IMacro>();
            macro.put("{snake_case(column)}", new IMacro() {
                @Override
                public String exec() {
                    return Helpers.camel_case_to_snake_case(col_nm);
                }
            });
            macro.put("{camelCase(column)}", new IMacro() {
                @Override
                public String exec() {
                    return Helpers.to_lower_camel_or_title_case(col_nm, false);
                }
            });
            macro.put("{TitleCase(column)}", new IMacro() {
                @Override
                public String exec() {
                    return Helpers.to_lower_camel_or_title_case(col_nm, true);
                }
            });
            macro.put("{kebab-case(column)}", new IMacro() {
                @Override
                public String exec() {
                    return Helpers.to_kebab_case(col_nm);
                }
            });
            macro.put("{column}", new IMacro() {
                @Override
                public String exec() {
                    return col_nm;
                }
            });
            for (String k : macro.keySet()) {
                if (field_comment_template.contains(k)) {
                    String value = macro.get(k).exec();
                    field_comment = field_comment_template.replace(k, value);
                }
            }
            fi.assign_jaxb_type(type_name + " " + field_comment);
        }
    }

    private void _refine_field_types_by_jaxb(
            DtoClass jaxb_dto_class,
            Map<String, FieldInfo> fields_map,
            List<FieldInfo> fields) throws Exception {

        _refine_field_info_by_jaxb(jaxb_dto_class, fields_map, fields);
        _refine_field_type_comments(jaxb_dto_class, fields);
    }

    private static ResultSet _get_columns_rs(DatabaseMetaData md, String table_name) throws SQLException {
        String[] parts = table_name.split("\\.", -1); // -1 to leave empty strings
        ResultSet rs_columns;
        if (parts.length == 1) {
            rs_columns = md.getColumns(null, null, table_name, "%");
        } else {
            String schema_nm = table_name.substring(0, table_name.lastIndexOf('.'));
            String table_nm = parts[parts.length - 1];
            rs_columns = md.getColumns(null, schema_nm, table_nm, "%");
        }
        return rs_columns;
    }

    private void _refine_field_info_by_jdbc_table(
            String table_name,
            Map<String, FieldInfo> fields_map) throws Exception {

        if (!SqlUtils.is_table_ref(table_name)) {
            throw new Exception("Table name expected: " + table_name);
        }
        DatabaseMetaData md = conn.getMetaData();
        ResultSet columns_rs = _get_columns_rs(md, table_name);
        try {
            while (columns_rs.next()) {
                String db_col_name = columns_rs.getString("COLUMN_NAME");
                if (fields_map.containsKey(db_col_name)) {
                    int type = columns_rs.getInt("DATA_TYPE");
                    String java_type_name = TypesMapping.getJavaBySqlType(type);
                    FieldInfo fi = fields_map.get(db_col_name);
                    fi.refine_jdbc_java_type_name(java_type_name);
                    fi.setComment("t(" + db_col_name + ")");
                }
            }
        } finally {
            columns_rs.close();
        }
    }

    private void _get_free_sql_params_info(
            String dao_jdbc_sql,
            FieldNamesMode param_names_mode,
            String[] method_param_descriptors,
            List<FieldInfo> params) throws Exception {

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
    private void _get_free_sql_params_info(
            PreparedStatement ps,
            FieldNamesMode param_names_mode,
            String[] method_param_descriptors,
            List<FieldInfo> _params) throws Exception {

        if (method_param_descriptors == null) {
            method_param_descriptors = new String[]{};
        }
        _params.clear();
        // Sybase ADS + adsjdbc.jar --------------------
        // ps.getParameterMetaData() throws java.lang.AbstractMethodError for all
        // statements
        //
        // SQL Server 2008 + sqljdbc4.jar --------------
        // ps.getParameterMetaData() throws
        // com.microsoft.sqlserver.jdbc.SQLServerException for
        // some statements SQL with parameters like 'SELECT count(*) FROM orders o WHERE
        // o_date BETWEEN ? AND ?'
        // and for SQL statements without parameters
        //
        // PostgeSQL -----------------------------------
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
                _get_param_info_by_descriptors(param_names_mode, method_param_descriptors, _params);
                return;
            }
        } catch (Throwable e) { // including AbstractMethodError, SQLServerException, etc.
            jdbc_params_count = 0;
        }
        int not_cb_array_params_count = 0;
        for (int i = 0; i < method_param_descriptors.length; i++) {
            String param_descriptor = method_param_descriptors[i].trim();
            if (param_descriptor.startsWith("[")) {
                // array of callbacks to fetch implicit cursors
                // is included in method_param_descriptors,
                // but implicit cursors are not detected as JDBC parameters
                if (param_descriptor.endsWith("]") == false) {
                    throw new Exception("Ending ']' expected");
                }
            } else {
                not_cb_array_params_count++;
                String default_param_type_name = _get_jdbc_param_type_name(pm, i);
                FieldInfo pi = _create_param_info(param_names_mode, param_descriptor, default_param_type_name);
                _params.add(pi);
            }
        }
        if (jdbc_params_count != not_cb_array_params_count) {
            throw new Exception("Parameters declared in method: " + method_param_descriptors.length
                    + ", detected by MetaData: " + jdbc_params_count);
        }
    }

    private void _refine_dao_fields_by_dto_fields(
            DtoClass jaxb_dto_class,
            String sql_root_abs_path,
            List<FieldInfo> dao_fields_all) throws Exception {

        List<FieldInfo> dto_fields = new ArrayList<FieldInfo>();
        Map<String, FieldInfo> dto_fields_map = _get_dto_fields(jaxb_dto_class, sql_root_abs_path, dto_fields);
        for (FieldInfo dao_fi : dao_fields_all) {
            String dao_col_name = dao_fi.getColumnName();
            if (dto_fields_map.containsKey(dao_col_name) == false) {
                throw new Exception("DAO column '" + dao_col_name + "' not found among DTO columns ["
                        + _get_column_names(dto_fields) + "]. Ensure lower/upper case.");
            }
            FieldInfo dto_fi = dto_fields_map.get(dao_col_name);
            String dto_fi_target_type_name = dto_fi.calc_target_type_name();
            if (Object.class.getTypeName().equals(dto_fi_target_type_name) == false) {
                // prefer DTO if it is not Object.
                dao_fi.set_target_type_by_map(dto_fi_target_type_name);
            }
            String dto_comment = dto_fi.getComment();
            String dao_comment = dao_fi.getComment();
            dao_fi.setComment(dto_comment + " <- " + dao_comment);
        }
    }

    private void _refine_dao_fields_by_dto_fields_for_crud_create(
            Map<String, FieldInfo> dto_fields_map,
            HashSet<String> dao_crud_generated_set,
            List<FieldInfo> dao_fields_all,
            List<FieldInfo> _dao_fields_not_generated,
            List<FieldInfo> _dao_fields_generated) throws Exception {

        for (FieldInfo dao_field : dao_fields_all) {
            String dao_col_name = dao_field.getColumnName();
            if (dto_fields_map.containsKey(dao_col_name) == false) {
                throw new Exception("Cannot create mapping for DAO column '" +
                        dao_col_name + "'. Ensure lower/upper case.");
            }
            dao_field.set_target_type_by_map(dto_fields_map.get(dao_col_name).calc_target_type_name());
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
            throw new SQLException("Unknown columns are listed as 'generated': " + dao_crud_generated_set);
        }
    }

    private static FieldInfo _get_ret_field_info(
            FieldNamesMode field_names_mode,
            String exlicit_ret_type,
            List<FieldInfo> dao_fields) throws Exception {

        String ret_type_name;
        if (exlicit_ret_type != null && exlicit_ret_type.trim().length() > 0) {
            ret_type_name = exlicit_ret_type;
        } else {
            if (dao_fields.isEmpty()) {
                // MySQL sakila example: dao_fields.isEmpty() for 'select inventory_in_stock(?)'
                ret_type_name = Object.class.getName();
            } else {
                ret_type_name = dao_fields.get(0).calc_target_type_name();
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

    private void _get_sql_shortcut_info(
            String sql_root_abs_path,
            String dao_jaxb_ref,
            String[] method_param_descriptors,
            FieldNamesMode param_names_mode,
            String jaxb_dto_or_return_type,
            boolean jaxb_return_type_is_dto,
            DtoClasses jaxb_dto_classes,
            List<FieldInfo> _fields,
            List<FieldInfo> _params) throws Exception {

        String[] parts = SqlUtils.parse_sql_shortcut_ref(dao_jaxb_ref);
        String dao_table_name = parts[0];
        String explicit_keys = parts[1];
        TableFieldInfo tfi = new TableFieldInfo(dao_table_name, explicit_keys);
        if (jaxb_return_type_is_dto) {
            DtoClass jaxb_dto_class = JaxbUtils.find_jaxb_dto_class(jaxb_dto_or_return_type, jaxb_dto_classes);
            _refine_dao_fields_by_dto_fields(jaxb_dto_class, sql_root_abs_path, tfi.fields_all);
            _fields.addAll(tfi.fields_all);
        } else {
            _fields.add(_get_ret_field_info(dto_field_names_mode, jaxb_dto_or_return_type, tfi.fields_all));
        }
        if (_fields.size() > 0) {
            String comment = _fields.get(0).getComment();
            _fields.get(0).setComment(comment + " [INFO] SQL-shortcut");
        }
        _get_param_info_for_sql_shortcut(param_names_mode, method_param_descriptors, tfi.fields_pk, _params);
    }

    private void _fill_by_dto(
            List<FieldInfo> dto_fields,
            List<FieldInfo> _fields,
            StringBuilder error) {

        // no fields from DAO SQL (e.g. for CALL statement) --> just use fields of DTO class:
        _fields.addAll(dto_fields);
        if (error.length() > 0 && _fields.size() > 0) {
            String comment = _fields.get(0).getComment();
            _fields.get(0).setComment(
                    comment + " [INFO] " + error.toString().trim().replace('\r', ' ').replace('\n', ' '));
        }
    }

    private void _fill_by_dao_and_dto(
            Map<String, FieldInfo> dto_fields_map,
            List<FieldInfo> dto_fields,
            List<FieldInfo> dao_fields,
            List<FieldInfo> _fields,
            StringBuilder error) throws Exception {

        if (ResultSet.class.getName().equals(dao_fields.get(0).calc_target_type_name())) {
            // the story about PostgreSQL + 'select * from get_tests_by_rating_rc(?)' (UDF
            // returning REFCURSOR)
            _fields.addAll(dto_fields);
            String comment = _fields.get(0).getComment() + " [INFO] Column 0 is of type ResultSet";
            if (error.length() > 0) {
                comment += ", " + error;
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

    private void _get_free_sql_fields_info_ret_dto(
            String sql_root_abs_path,
            String jaxb_dto_or_return_type,
            DtoClasses jaxb_dto_classes,
            List<FieldInfo> _fields,
            List<FieldInfo> dao_fields,
            StringBuilder error) throws Exception {

        List<FieldInfo> dto_fields = new ArrayList<FieldInfo>();
        DtoClass jaxb_dto_class = JaxbUtils.find_jaxb_dto_class(jaxb_dto_or_return_type, jaxb_dto_classes);
        Map<String, FieldInfo> dto_fields_map = _get_dto_fields(jaxb_dto_class, sql_root_abs_path, dto_fields);
        if (dao_fields.isEmpty()) {
            _fill_by_dto(dto_fields, _fields, error);
        } else {
            _fill_by_dao_and_dto(dto_fields_map, dto_fields, dao_fields, _fields, error);
        }
        if (_fields.isEmpty()) {
            String msg = _get_mapping_error_msg(dto_fields, dao_fields);
            throw new Exception(msg);
        }
    }

    private void _get_free_sql_fields_info(
            String sql_root_abs_path,
            String dao_query_jdbc_sql,
            String jaxb_dto_or_return_type,
            boolean jaxb_return_type_is_dto,
            DtoClasses jaxb_dto_classes,
            List<FieldInfo> _fields) throws Exception {

        Map<String, FieldInfo> dao_fields_map = new HashMap<String, FieldInfo>();
        List<FieldInfo> dao_fields = new ArrayList<FieldInfo>();
        StringBuilder error = new StringBuilder();
        try {
            _get_field_info_by_jdbc_sql(dao_query_jdbc_sql, dao_fields_map, dao_fields);
        } catch (Exception e) {
            error.append(e.getMessage());
        }
        if (jaxb_return_type_is_dto) {
            _get_free_sql_fields_info_ret_dto(sql_root_abs_path, jaxb_dto_or_return_type, jaxb_dto_classes, _fields, dao_fields, error);
        } else {
            _fields.add(_get_ret_field_info(dto_field_names_mode, jaxb_dto_or_return_type, dao_fields));
        }
    }

    private String _get_column_names(List<FieldInfo> fields) {
        List<String> col_names = new ArrayList<String>();
        for (FieldInfo fi : fields) {
            col_names.add(fi.getColumnName());
        }
        return String.join(", ", col_names);
    }

    private String _get_mapping_error_msg(List<FieldInfo> dto_fields, List<FieldInfo> dao_fields) {
        return "DAO columns [" + _get_column_names(dao_fields) + "] not found among DTO columns ["
                + _get_column_names(dto_fields) + "].";
    }

    // Public Utils --------------------------------------------
    //
    // ---------------------------------------------------------

    public static ResultSet get_tables_rs(
            Connection conn,
            DatabaseMetaData dbmd,
            String schema_name,
            boolean include_views) throws SQLException {

        String[] types;
        if (include_views) {
            types = new String[]{"TABLE", "VIEW"};
        } else {
            types = new String[]{"TABLE"};
        }
        ResultSet rs_tables;
        String catalog = conn.getCatalog();
        rs_tables = dbmd.getTables(catalog, schema_name, "%", types);
        return rs_tables;
    }

    public static void get_schema_names(
            Connection con,
            List<String> _schema_names) throws SQLException {

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

    // DTO -----------------------------------------------------
    //
    // ---------------------------------------------------------

    public void get_dto_field_info(
            DtoClass jaxb_dto_class,
            String sql_root_abs_path,
            List<FieldInfo> _fields) throws Exception {

        JaxbUtils.validate_jaxb_dto_class(jaxb_dto_class);
        _get_dto_fields(jaxb_dto_class, sql_root_abs_path, _fields);
    }

    private FieldNamesMode _refine_method_params_names_mode(String dto_param_type) {
        // if it is something like <query method="get_some_value(MyDTO(m_id, m_date))",
        // use field_names_mode (???)
        FieldNamesMode mode = dto_param_type == null || dto_param_type.length() == 0 ?
                FieldNamesMode.AS_IS : method_params_names_mode;
        return mode;
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

    // DAO. Free-SQL -------------------------------------------
    //
    // ---------------------------------------------------------

    public String get_dao_query_info(
            String sql_root_abs_path,
            String dao_jaxb_ref, String dto_param_type,
            String[] method_param_descriptors,
            String jaxb_dto_or_return_type,
            boolean jaxb_return_type_is_dto,
            DtoClasses jaxb_dto_classes,
            List<FieldInfo> _fields,
            List<FieldInfo> _params) throws Exception {

        _fields.clear();
        _params.clear();
        Helpers.check_duplicates(method_param_descriptors);
        String dao_query_jdbc_sql = SqlUtils.jdbc_sql_by_query_ref(dao_jaxb_ref, sql_root_abs_path);
        FieldNamesMode param_names_mode = _refine_method_params_names_mode(dto_param_type);
        if (SqlUtils.is_sql_shortcut_ref(dao_jaxb_ref)) {
            _get_sql_shortcut_info(sql_root_abs_path, dao_jaxb_ref, method_param_descriptors, param_names_mode,
                    jaxb_dto_or_return_type, jaxb_return_type_is_dto, jaxb_dto_classes, _fields, _params);
        } else {
            _get_free_sql_fields_info(sql_root_abs_path, dao_query_jdbc_sql,
                    jaxb_dto_or_return_type, jaxb_return_type_is_dto, jaxb_dto_classes, _fields);
            _get_free_sql_params_info(dao_query_jdbc_sql, param_names_mode, method_param_descriptors, _params);
        }
        if (type_map.is_defined()) {
            for (FieldInfo fi : _fields) {
                String type_name = type_map.get_target_type_name(fi.calc_target_type_name());
                fi.set_target_type_by_map(type_name);
            }
        }
        return dao_query_jdbc_sql;
    }

    public void get_dao_exec_dml_info(
            String dao_jdbc_sql,
            String dto_param_type,
            String[] method_param_descriptors,
            List<FieldInfo> _params) throws Exception {

        FieldNamesMode param_names_mode = _refine_method_params_names_mode(dto_param_type);
        _get_free_sql_params_info(dao_jdbc_sql, param_names_mode, method_param_descriptors, _params);
    }

    // DAO. CRUD -----------------------------------------------
    //
    // ---------------------------------------------------------

    public String get_dao_crud_create_info(
            DtoClass jaxb_dto_class,
            String sql_root_abs_path,
            String dao_table_name,
            String dao_crud_generated,
            List<FieldInfo> _dao_fields_not_generated,
            List<FieldInfo> _dao_fields_generated) throws Exception {

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
        // "" because pk columns are not meaningful for crud create,
        // only generated ones:
        TableFieldInfo tfi = new TableFieldInfo(dao_table_name, "");
        List<FieldInfo> dao_fields_all = tfi.fields_all;
        _refine_dao_fields_by_dto_fields_for_crud_create(dto_fields_map, dao_crud_generated_set, dao_fields_all,
                _dao_fields_not_generated, _dao_fields_generated);
        return SqlUtils.create_crud_create_sql(dao_table_name, _dao_fields_not_generated);
    }

    public String get_dao_crud_read_info(
            boolean fetch_list,
            DtoClass jaxb_dto_class,
            String sql_root_abs_path,
            String dao_table_name,
            String explicit_pk,
            List<FieldInfo> _dao_fields_all,
            List<FieldInfo> _dao_fields_pk) throws Exception {

        TableFieldInfo tfi = new TableFieldInfo(dao_table_name, explicit_pk);
        _dao_fields_all.clear();
        _dao_fields_all.addAll(tfi.fields_all);
        _dao_fields_pk.clear();
        _dao_fields_pk.addAll(tfi.fields_pk);
        _refine_dao_fields_by_dto_fields(jaxb_dto_class, sql_root_abs_path, _dao_fields_all);
        return SqlUtils.create_crud_read_sql(dao_table_name, _dao_fields_pk, fetch_list);
    }

    public String get_dao_crud_update_info(
            String dao_table_name,
            DtoClass jaxb_dto_class,
            String sql_root_abs_path,
            String explicit_pk,
            List<FieldInfo> _fields_not_pk,
            List<FieldInfo> _fields_pk) throws Exception {

        TableFieldInfo tfi = new TableFieldInfo(dao_table_name, explicit_pk);
        _fields_not_pk.clear();
        _fields_not_pk.addAll(tfi.fields_not_pk);
        _fields_pk.clear();
        _fields_pk.addAll(tfi.fields_pk);
        if (_fields_not_pk.isEmpty()) {
            return null; // just render info comment instead of method
        }
        if (_fields_pk.isEmpty()) {
            return null; // just render info comment instead of method
        }
        _refine_dao_fields_by_dto_fields(jaxb_dto_class, sql_root_abs_path, tfi.fields_all);
        return SqlUtils.create_crud_update_sql(dao_table_name, _fields_not_pk, _fields_pk);
    }

    public String get_dao_crud_delete_info(
            String dao_table_name,
            String explicit_pk,
            List<FieldInfo> _fields_pk) throws Exception {

        TableFieldInfo tfi = new TableFieldInfo(dao_table_name, explicit_pk);
        _fields_pk.clear();
        _fields_pk.addAll(tfi.fields_pk);
        if (_fields_pk.isEmpty()) {
            return null; // just render info comment instead of method
        }
        return SqlUtils.create_crud_delete_sql(dao_table_name, _fields_pk);
    }
}
