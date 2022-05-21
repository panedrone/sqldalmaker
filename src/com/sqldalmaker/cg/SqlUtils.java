/*
    Copyright 2011-2022 sqldalmaker@gmail.com
    SQL DAL Maker Website: http://sqldalmaker.sourceforge.net
    Read LICENSE.txt in the root of this project/archive for details.
 */
package com.sqldalmaker.cg;

import java.util.List;

/**
 * @author sqldalmaker@gmail.com
 */
public class SqlUtils {

    private static String[] get_sql_lines(String sql) {
        sql = sql.replace('\r', '\n');
        sql = sql.replace('\t', ' ');
        String[] parts = sql.split("\\n+");
        return parts;
    }

    public static String jdbc_sql_to_cpp_str(String jdbc_sql) {
        String[] parts = get_sql_lines(jdbc_sql);
        String new_line = System.getProperty("line.separator");
        String new_line_j = org.apache.commons.lang.StringEscapeUtils.escapeJava("\n");
        StringBuilder res = new StringBuilder();
        res.append("\"");
        for (int i = 0; i < parts.length; i++) {
            String j_str = parts[i].replace('\t', ' ');
            // packed into Velocity JAR:
            j_str = org.apache.commons.lang.StringEscapeUtils.escapeJava(j_str);
            // fix the bug in StringEscapeUtils:
            // case '/':
            // out.write('\\');
            // out.write('/');
            // break;
            j_str = j_str.replace("\\/", "/");
            res.append(j_str);
            if (i < parts.length - 1) {
                res.append(" ");
                res.append(new_line_j);
                res.append("\\");
                res.append(new_line);
                // res.append("\t\t");
            } else {
                res.append("\"");
            }
        }
        return res.toString();
    }

    public static String format_jdbc_sql_for_go(String jdbc_sql) {
        // return SqlUtils.format_jdbc_sql(jdbc_sql, true);
        String[] parts = get_sql_lines(jdbc_sql);
        String new_line = "\n"; // System.getProperty("line.separator");
        StringBuilder res = new StringBuilder();
        res.append("`");
        for (int i = 0; i < parts.length; i++) {
            String j_str = parts[i];
            // packed into Velocity JAR:
            //j_str = org.apache.commons.lang.StringEscapeUtils.escapeJava(j_str);
            // fix the bug in StringEscapeUtils:
            // case '/':
            // out.write('\\');
            // out.write('/');
            // break;
            // j_str = j_str.replace("\\/", "/");
            if (i == 0) {
                // j_str = j_str;
            } else {
                j_str = " " + new_line + "\t\t" + j_str;
            }
            res.append(j_str);
        }
        res.append("`");
        return res.toString();
    }

    public static String format_jdbc_sql_for_java(String jdbc_sql) {
        // return SqlUtils.format_jdbc_sql(jdbc_sql, false);
        String[] parts = get_sql_lines(jdbc_sql);
        String new_line = "\n"; // System.getProperty("line.separator");
        StringBuilder res = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            String j_str = parts[i];
            // packed into Velocity JAR:
            j_str = org.apache.commons.lang.StringEscapeUtils.escapeJava(j_str);
            // fix the bug in StringEscapeUtils:
            // case '/':
            // out.write('\\');
            // out.write('/');
            // break;
            j_str = j_str.replace("\\/", "/");
            if (i == 0) {
                j_str = "\"" + j_str + "\"";
            } else {
                // "\n" it is OK for debugger window:
                j_str = " +" + new_line + "        \"\\n " + j_str + "\"";
            }
            res.append(j_str);
        }
        return res.toString();
    }

    public static String format_jdbc_sql(String jdbc_sql, boolean tabs) {
        String[] parts = get_sql_lines(jdbc_sql);
        String new_line = "\n"; // System.getProperty("line.separator");
        StringBuilder res = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            String j_str = parts[i];
            // packed into Velocity JAR:
            j_str = org.apache.commons.lang.StringEscapeUtils.escapeJava(j_str);
            // fix the bug in StringEscapeUtils:
            // case '/':
            // out.write('\\');
            // out.write('/');
            // break;
            j_str = j_str.replace("\\/", "/");
            if (i == 0) {
                j_str = "\"" + j_str + "\"";
            } else {
                // "\n" it is OK for debugger window:
                if (tabs) {
                    j_str = " +" + new_line + "\t\t\"\\n " + j_str + "\"";
                } else {
                    j_str = " +" + new_line + "        \"\\n " + j_str + "\"";
                }
            }
            res.append(j_str);
        }
        return res.toString();
    }

    public static String jdbc_sql_to_php_str(String jdbc_sql) /* throws Exception */ {
        boolean is_sp = is_jdbc_stored_proc_call(jdbc_sql);
        String php_sql;
        if (is_sp) {
            php_sql = _jdbc_sp_call_to_php_sp_call(jdbc_sql);
        } else {
            php_sql = jdbc_sql;
        }
        return php_sql_to_php_str(php_sql);
    }

    private static String php_sql_to_php_str(String php_sql) {
        String[] parts = get_sql_lines(php_sql);
        String new_line = "\n"; // System.getProperty("line.separator");
        StringBuilder res = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            String j_str = parts[i];
            // packed into Velocity JAR:
            j_str = org.apache.commons.lang.StringEscapeUtils.escapeJava(j_str);
            // fix the bug in StringEscapeUtils:
            // case '/':
            // out.write('\\');
            // out.write('/');
            // break;
            j_str = j_str.replace("\\/", "/");
            if (i == 0) {
                j_str = "\"" + j_str + "\"";
            } else {
                // "\n" it is OK for debugger window:
                j_str = new_line + "            . \"\\n " + j_str + "\"";
            }
            res.append(j_str);
        }
        return res.toString();
    }

    public static String jdbc_sql_to_ruby_string(String jdbc_sql) /*throws Exception*/ {
        return jdbc_sql_to_python_string(jdbc_sql);
    }

    public static String jdbc_sql_to_python_string(String jdbc_sql) /*throws Exception*/ {
        return python_sql_to_python_string(jdbc_sql);
    }

    private static String python_sql_to_python_string(String python_sql) {
        String[] parts = get_sql_lines(python_sql);
        String new_line = "\n"; // System.getProperty("line.separator");
        StringBuilder res = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            String j_str = parts[i].replace('\t', ' ');
            // packed into Velocity JAR:
            j_str = org.apache.commons.lang.StringEscapeUtils.escapeJava(j_str);
            // fix the bug in StringEscapeUtils:
            // case '/':
            // out.write('\\');
            // out.write('/');
            // break;
            j_str = j_str.replace("\\/", "/");
            res.append(j_str);
            if (i < parts.length - 1) { // python wants 4 spaces instead of 1 tab
                String s = " " + new_line + "                ";
                res.append(s);
            }
        }
        return res.toString();
    }

    public static void throw_if_select_sql(String jdbc_dao_sql) /*throws Exception*/ {
// allow to execute SELECT in exec-dml
//
//        String trimmed = jdbc_dao_sql.toLowerCase().trim();
//
//        String[] parts = trimmed.split("\\s+");
//
//        if (parts.length > 0) {
//
//            if ("select".equals(parts[0])) {
//
//                throw new Exception("SELECT is not allowed here");
//            }
//        }
    }

    public static String jdbc_sql_by_dto_class_ref(String ref, String sql_root_abs_path) throws Exception {
        String[] parts = ref.split(":");
        String table_name = null;
        if (parts.length >= 2) {
            if ("table".compareTo(parts[0].toLowerCase().trim()) == 0) {
                table_name = ref.substring(parts[0].length() + 1);
            }
        } else if (is_jdbc_stored_proc_call(ref)) {
            return ref;
        } else if (is_sp_call_shortcut(ref)) {
            return ref;
        } else if (is_udf_call_shortcut(ref)) {
            return ref;
        } else if (is_sql_file_ref(ref)) {
            String sql_file_path = Helpers.concat_path(sql_root_abs_path, ref);
            return Helpers.load_text_from_file(sql_file_path);
        } else if (is_sql_shortcut_ref(ref)) {
            String res = _sql_shortcut_to_jdbc_sql(ref);
            return res;
        } else if (is_table_ref(ref)) {
            table_name = ref;
        } else {
            throw new Exception("Invalid ref: <dto-class ref=\"" + ref + "\"");
        }
        return "select * from " + table_name + " where 1 = 0";
    }

    static String jdbc_sql_by_query_ref(String ref, String sql_root_abs_path) throws Exception {
        /*if (is_jdbc_stored_proc_call(ref)) {
            return ref;
        } else if (is_stored_proc_call_shortcut(ref)) {
            return ref;
        } else if (is_stored_func_call_shortcut(ref)) {
            return ref;
        } else */
        if (is_sql_file_ref(ref)) {
            String sql_file_path = Helpers.concat_path(sql_root_abs_path, ref);
            return Helpers.load_text_from_file(sql_file_path);
        } else if (is_sql_shortcut_ref(ref)) {
            String res = _sql_shortcut_to_jdbc_sql(ref);
            return res;
        } /*else if (is_table_ref(ref)) { // is_table_ref returns true for 'select * from my_table'
            throw new Exception("Table names are not allowed here: ref=\"" + ref + "\"");
        } */ else {
            return ref;
        }
    }

    public static String jdbc_sql_by_exec_dml_ref(String ref, String sql_root_abs_path) throws Exception {
        /*if (is_jdbc_stored_proc_call(ref)) {
            return ref;
        } else if (is_stored_proc_call_shortcut(ref)) {
            return ref;
        } else if (is_stored_func_call_shortcut(ref)) {
            return ref;
        } else */
        if (is_sql_file_ref(ref)) {
            String sql_file_path = Helpers.concat_path(sql_root_abs_path, ref);
            return Helpers.load_text_from_file(sql_file_path);
        } else if (is_sql_shortcut_ref(ref)) {
            throw new Exception("SQL-shortcuts are not allowed here: ref=\"" + ref + "\"");
        } /* else if (is_table_ref(ref)) {  // is_table_ref returns true for 'delete from my_table'
            throw new Exception("Table names are not allowed here: ref=\"" + ref + "\"");
        } */ else {
            return ref;
        }
    }

    public static boolean is_sql_shortcut_ref(String ref) {
        if (ref == null || ref.length() == 0) {
            return false;
        }
        if (is_sql_file_ref_base(ref)) {
            return false;
        }
        String[] parts;
        try {
            parts = parse_sql_shortcut_ref(ref);
            String table_name = parts[0];
            if (!is_table_ref(table_name)) {
                return false;
            }
            String inside_brackets = parts[1];
            Helpers.get_listed_items(inside_brackets, false);
        } catch (Exception e) {
            return false;
        }
        return true;
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

    public static boolean is_sql_file_ref_base(String ref) {
        return ref != null && ref.length() > 4 && ref.endsWith(".sql");
    }

    public static boolean is_empty_ref(String ref) {
        return ref == null || ref.trim().length() == 0;
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
        if (is_sp_call_shortcut(ref)) {
            return false;
        }
        if (is_udf_call_shortcut(ref)) {
            return false;
        }
        final char[] ILLEGAL_CHARACTERS = {
                '/', '\n', '\r', '\t', '\0', '\f', /*'`',*/ '?', '*', '\\', '<', '>', '|',
                '\"'/* , ':' */, ';', ',', '(', ')', '{', '}'};
        for (char c : ILLEGAL_CHARACTERS) {
            if (ref.contains(Character.toString(c))) {
                return false;
            }
        }
        // no empty strings separated by dots
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
        if (jdbc_sql.startsWith("{") && jdbc_sql.endsWith("}")) {
            jdbc_sql = jdbc_sql.substring(1, jdbc_sql.length() - 1);
        } else if (jdbc_sql.startsWith("{") && !jdbc_sql.endsWith("}")
                || !jdbc_sql.startsWith("{") && jdbc_sql.endsWith("}")) {
            // throw new Exception("Invalid JDBC call: " + jdbc_sql);
            return false;
        }
        return is_sp_call_shortcut(jdbc_sql);
    }

    public static boolean is_sp_call_shortcut(String text) {
        String[] parts = text.split("\\s+");
        if (parts.length < 2) {

            return false;
        }
        String call = parts[0].trim();
        return call.compareToIgnoreCase("call") == 0;
    }

    public static boolean is_udf_call_shortcut(String sql) {
        String[] parts = sql.split("\\s+");
        if (parts.length < 2) {
            return false;
        }
        String select = parts[0];
        return select.compareToIgnoreCase("select") == 0;
    }

    //
    // keep initial syntax, modify it in DataStore.php if needed
    //
    private static String _jdbc_sp_call_to_php_sp_call(String jdbc_sql) /*throws java.lang.Exception*/ {
        return jdbc_sql;
//        jdbc_sql = jdbc_sql.trim();
//        if (is_jdbc_stored_proc_call(jdbc_sql)) { // confirms syntax {call sp_name(...)}
//            if (jdbc_sql.startsWith("{") && jdbc_sql.endsWith("}")) {
//                return jdbc_sql.substring(1, jdbc_sql.length() - 1).trim(); // converted to call sp_name(...)
//            } else {
//                return jdbc_sql;
//            }
//        } else if (is_stored_proc_call_shortcut(jdbc_sql)) {
//            return jdbc_sql;
//        } else {
//            throw new Exception("Unexpected syntax of CALL: " + jdbc_sql);
//        }
    }

    static String[] parse_sql_shortcut_ref(String ref) throws Exception {
        String before_brackets;
        String inside_brackets;
        ref = ref.trim();
        int pos = ref.indexOf('(');
        if (pos == -1) {
            throw new Exception("'(' expected in ref shortcut");
        } else {
            if (!ref.endsWith(")")) {
                throw new Exception("')' expected in ref shortcut");
            }
            before_brackets = ref.substring(0, pos);
            inside_brackets = ref.substring(before_brackets.length() + 1, ref.length() - 1);
        }
        return new String[]{before_brackets, inside_brackets};
    }

    private static String _sql_shortcut_to_jdbc_sql(String ref) throws Exception {
        String[] parts2 = parse_sql_shortcut_ref(ref);
        String table_name = parts2[0];
        // validate_table_name(table_name); // TODO: PostgreSQL JDBC prepareStatement
        // passes wrong table names
        String param_descriptors = parts2[1];
        if (param_descriptors.trim().length() == 0) {
            return "select * from " + table_name;
        }
        String[] param_arr = Helpers.get_listed_items(param_descriptors, false);
        if (param_arr.length < 1) {
            throw new Exception("Not empty list of parameters expected in SQL shortcut");
        }
        String params = param_arr[0] + "=?";
        for (int i = 1; i < param_arr.length; i++) {
            params += " and " + param_arr[i] + "=?";
        }
        return "select * from " + table_name + " where " + params;
    }

    public static String create_crud_create_sql(String dao_table_name, List<FieldInfo> fields_not_ai) throws Exception {
        if (fields_not_ai.isEmpty()) {
            throw new Exception("Columns not found. Ensure lower/upper case.");
        }
        String cols = fields_not_ai.get(0).getColumnName();
        String values = "?";
        for (int i = 1; i < fields_not_ai.size(); i++) {
            cols += ", " + fields_not_ai.get(i).getColumnName();
            values += ", ?";
        }
        String dao_jdbc_sql = "insert into " + dao_table_name + " (" + cols + ") values (" + values + ")";
        return dao_jdbc_sql;
    }

    public static String create_crud_read_sql(String dao_table_name, List<FieldInfo> fields_pk, boolean fetch_list) throws Exception {
        String dao_jdbc_sql = "select * from " + dao_table_name;
        if (!fetch_list) {
            if (fields_pk.isEmpty()) {
                throw new Exception("PK columns not found. Ensure lower/upper case.");
            } else {
                dao_jdbc_sql += " where " + fields_pk.get(0).getColumnName() + "=?";
                for (int i = 1; i < fields_pk.size(); i++) {
                    dao_jdbc_sql += " and " + fields_pk.get(i).getColumnName() + "=?";
                }
            }
        }
        return dao_jdbc_sql;
    }

    public static String create_crud_update_sql(String dao_table_name, List<FieldInfo> not_fields_pk, List<FieldInfo> fields_pk) {
        String dao_jdbc_sql = "update " + dao_table_name;
        dao_jdbc_sql += " set " + not_fields_pk.get(0).getColumnName() + "=?";
        for (int i = 1; i < not_fields_pk.size(); i++) {
            dao_jdbc_sql += ", " + not_fields_pk.get(i).getColumnName() + "=?";
        }
        dao_jdbc_sql += " where " + fields_pk.get(0).getColumnName() + "=?";
        for (int i = 1; i < fields_pk.size(); i++) {
            dao_jdbc_sql += " and " + fields_pk.get(i).getColumnName() + "=?";
        }
        return dao_jdbc_sql;
    }

    public static String create_crud_delete_sql(String dao_table_name, List<FieldInfo> fields_pk) {
        String dao_jdbc_sql = "delete from " + dao_table_name;
        dao_jdbc_sql += " where " + fields_pk.get(0).getColumnName() + "=?";
        for (int i = 1; i < fields_pk.size(); i++) {
            dao_jdbc_sql += " and " + fields_pk.get(i).getColumnName() + "=?";
        }
        return dao_jdbc_sql;
    }
}
