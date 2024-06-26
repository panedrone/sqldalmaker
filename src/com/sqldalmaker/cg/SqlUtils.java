/*
    Copyright 2011-2024 sqldalmaker@gmail.com
    SQL DAL Maker Website: https://sqldalmaker.sourceforge.net/
    Read LICENSE.txt in the root of this project/archive for details.
 */
package com.sqldalmaker.cg;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
 * 28.12.2023 08:29 1.292
 * 27.03.2023 10:03
 * 19.01.2023 20:57 1.276
 * 16.11.2022 08:02 1.269
 * 21.05.2022 10:42
 * 26.04.2022 15:44 1.230
 * 16.04.2022 17:35 1.219
 * 30.03.2022 12:18
 * 10.06.2021 13:03
 * 08.05.2021 22:29 1.200
 * 14.03.2021 22:09
 *
 */
@SuppressWarnings("removal")
public class SqlUtils {

    static class SqlShortcut {
        public String table_name;
        public String params;
        public String col_names;
    }

    private static String[] get_sql_lines(String sql) {
        sql = sql.replace('\r', '\n');
        sql = sql.replace('\t', ' ');
        String[] parts = sql.split("\\n+");
        return parts;
    }

    public static String jdbc_sql_to_cpp_str(String jdbc_sql) {
        String[] parts = get_sql_lines(jdbc_sql);
        String new_line = System.lineSeparator();
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
        return jdbc_sql_by_table_name(table_name);
    }

    public static String jdbc_sql_by_table_name(String table_name) {
        return "select * from " + table_name;
    }

    public static String jdbc_sql_by_ref(String ref, String sql_root_abs_path) throws Exception {
        if (is_table_ref(ref)) {
            return jdbc_sql_by_table_name(ref);
        }
        if (is_sql_file_ref(ref)) {
            String sql_file_path = Helpers.concat_path(sql_root_abs_path, ref);
            return Helpers.load_text_from_file(sql_file_path);
        }
        if (is_sql_shortcut_ref(ref)) {
            String res = _sql_shortcut_to_jdbc_sql(ref);
            return res;
        }
        return ref;
    }

    public static String jdbc_sql_by_exec_dml_ref(String ref, String sql_root_abs_path) throws Exception {
        if (is_sql_file_ref(ref)) {
            String sql_file_path = Helpers.concat_path(sql_root_abs_path, ref);
            return Helpers.load_text_from_file(sql_file_path);
        }
        if (is_sql_shortcut_ref(ref)) {
            throw new Exception("SQL-shortcuts are not allowed here: ref=\"" + ref + "\"");
        }
        return ref;
    }

    public static boolean is_sql_shortcut_ref(String ref) {
        if (ref == null) {
            return false;
        }
        ref = ref.trim();
        if (ref.isEmpty()) {
            return false;
        }
        if (is_sql_file_ref_base(ref)) {
            return false;
        }
        try {
            // table()
            // table() / field1, field2, ...
            // table / field1, field2, ...
            // table() -> field1, field2, ...
            // table -> field1, field2, ...
            // table(param1, param2, ...)
            // table(param1, param2, ...) / field1, field2, ...
            // table(param1, param2, ...) -> field1, field2, ...
            SqlShortcut shc = parse_sql_shortcut_ref(ref);
//            String table_name = parts[0];
//            if (!is_table_ref(table_name)) {
//                return false;
//            }
            if (shc.params != null) {
                Helpers.get_listed_items(shc.params, false);
            } else {
                if (shc.col_names == null) {
                    return false;
                }
            }
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
        return ref != null && ref.trim().length() > 4 && ref.endsWith(".sql");
    }

    public static boolean is_empty_ref(String ref) {
        return ref == null || ref.trim().isEmpty();
    }

    public static boolean is_table_ref(String ref) {
        if (ref == null) {
            return false;
        }
        ref = ref.trim();
        if (ref.isEmpty()) {
            return false;
        }
        // https://stackoverflow.com/questions/11488478/how-do-i-check-whether-input-string-contains-any-spaces
        Pattern space = Pattern.compile("\\s+");
        Matcher matcherSpace = space.matcher(ref);
        boolean containsSpace = matcherSpace.find();
        if (containsSpace) {
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
            if (s.isEmpty()) {
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
    }

    static SqlShortcut parse_sql_shortcut_ref(String ref) throws Exception {
        ref = ref.trim();
        String[] mm = ref.split("->");
        if (mm.length != 2) {
            mm = ref.split("/");
        }
        SqlShortcut res = new SqlShortcut();
        if (mm.length == 2) {
            ref = mm[0].trim();
            res.col_names = mm[1].trim();
        } else if (mm.length > 2) {
            throw new Exception("Invalid 'ref' in SQL-shortcut: " + ref);
        } else {
            res.col_names = null;
        }
        ref = ref.trim();
        int pos = ref.indexOf('(');
        if (ref.endsWith(")")) { // trimmed
            if (pos < 1) {
                throw new Exception("Invalid 'ref' in SQL-shortcut: '(' expected");
            }
            res.table_name = ref.substring(0, pos).trim();
            res.params = ref.substring(pos + 1, ref.length() - 1).trim();
            if (res.params.isEmpty()) {
                res.params = null;
            }
        } else {
            res.table_name = ref;
            res.params = null;
        }
        return res;
    }

    private static String _sql_shortcut_to_jdbc_sql(String ref) throws Exception {
        SqlShortcut shc = parse_sql_shortcut_ref(ref);
        String params = null;
        String param_descriptors = shc.params;
        if (param_descriptors != null && !param_descriptors.trim().isEmpty()) {
            String[] param_arr = Helpers.get_listed_items(param_descriptors, false);
            if (param_arr.length < 1) {
                throw new Exception("Not empty list of parameters expected in SQL shortcut");
            }
            params = param_arr[0] + "=?";
            for (int i = 1; i < param_arr.length; i++) {
                params += " and " + param_arr[i] + "=?";
            }
        }
        String sql;
        if (shc.col_names == null) {
            sql = jdbc_sql_by_table_name(shc.table_name);
        } else {
            sql = "select " + shc.col_names + " from " + shc.table_name;
        }
        if (params == null) {
            return sql;
        }
        sql = sql + " where " + params;
        return sql;
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
        String dao_jdbc_sql = jdbc_sql_by_table_name(dao_table_name);
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
