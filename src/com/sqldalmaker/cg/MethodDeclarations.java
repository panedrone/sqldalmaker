/*
    Copyright 2011-2026 sqldalmaker@gmail.com
    SQL DAL Maker Website: https://sqldalmaker.sourceforge.net/
    Read LICENSE.txt in the root of this project/archive for details.
 */
package com.sqldalmaker.cg;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

/*
 * The syntax of "method=..." in the XML meta-program:
 * the method name, the list of parameters and the checks on it.
 */
public class MethodDeclarations {

    public static String[] parse_method_params(String src) throws Exception {

        String before_brackets;
        String inside_brackets;
        src = src.trim();
        int pos = src.indexOf('(');
        if (pos == -1) {
            before_brackets = src;
            inside_brackets = "";
        } else {
            if (!src.endsWith(")")) {
                throw new Exception("')' expected");
            }
            before_brackets = src.substring(0, pos);
            inside_brackets = src.substring(before_brackets.length() + 1, src.length() - 1);
        }
        return new String[]{before_brackets, inside_brackets};
    }

    // "get_something(param_1, param_2)" --> {"get_something", "param_1, param_2"}
    public static String[] parse_method_declaration(String method_text) throws Exception {
        String param_descriptors = "";
        String[] parts = parse_method_params(method_text);
        String method_name = parts[0];
        if (!("".equals(parts[1]))) {
            parts = parse_method_params(parts[1]);
            if (!("".equals(parts[1]))) {
                throw new Exception("Invalid params: " + method_text);
            }
            param_descriptors = parts[0];
        }
        return new String[]{method_name, param_descriptors};
    }

    // "on_dto:Dto" or "on_dto~Dto" --> {"on_dto", "Dto"}; null if it is a plain parameter
    public static String[] split_mapped_param_descriptor(String param_descriptor) {
        String[] parts = null;
        if (param_descriptor.contains("~")) {
            parts = param_descriptor.split("~");
        }
        if (param_descriptor.contains(":")) {
            parts = param_descriptor.split(":");
        }
        return parts;
    }

    public static String[] get_listed_items(String list, boolean allow_semicolon) throws Exception {
        if (list == null || list.trim().isEmpty()) {
            return new String[]{};
        }
        list = list.trim();
        int pos = list.indexOf('[');
        String last_arr = null;
        if (pos != -1) {
            if (list.endsWith("]") == false) {
                throw new Exception("Ending ']' expected");
            }
            last_arr = list.substring(pos); // keep []
            if (pos == 0) {
                return new String[]{last_arr};
            }
            list = list.substring(0, pos);
            list = list.trim();
            if (list.endsWith(",")) {
                list = list.substring(0, list.length() - 1); // remove ending ','
            }
        }
        String[] items;
        items = list.split(",");
        for (int i = 0; i < items.length; i++) {
            items[i] = items[i].trim();
            String[] parts = items[i].split("\\s+");
            String name;
            if (parts.length == 1) {
                name = parts[0];
            } else if (parts.length == 2) {
                name = parts[1];
            } else {
                throw new Exception("The item is null or empty: " + list);
            }
            check_item(name/* , is_sp */, allow_semicolon);
        }
        if (last_arr != null) {
            int n = items.length;
            String[] new_arr = new String[n + 1];
            for (int i = 0; i < n; i++)
                new_arr[i] = items[i];
            new_arr[n] = last_arr;
            return new_arr;
        } else {
            return items;
        }
    }

    private static void check_item(String name, boolean allow_semicolon) throws Exception {
        if (name == null || name.isEmpty()) {
            throw new Exception("Item name is null or empty");
        }
        char ch_0 = name.charAt(0);
        boolean is_letter_at_0 = Character.isLetter(ch_0);
        if (!is_letter_at_0 || ch_0 == '$') {
            if (ch_0 != '_') {
                throw new Exception("Invalid starting character in the name of item: " + name);
            }
        }
        for (int i = 1; i < name.length(); i++) {
            // Google: java is letter
            // A character is considered to be a Java letter or digit if and only if it is a
            // letter or a digit or the dollar sign "$" or the underscore "_".
            char ch = name.charAt(i);
            if (!allow_semicolon && ch == ':') {
                throw new Exception("':' is not allowed in this context");
            }
            boolean is_letter_or_digit = Character.isLetterOrDigit(ch);
            // SQL parameter name may be detected like 'column1:0' and renamed to column1_0
            if (!is_letter_or_digit &&
                    ch != '$' &&
                    ch != '_' &&
                    ch != '~' &&
                    ch != ':' &&
                    ch != '-' // separator for model
            ) {
                throw new Exception("Invalid character in the name of item: " + name);
            }
        }
    }

    // "java.lang.Integer p_id" --> {"java.lang.Integer", "p_id"}; type is null if absent
    static String[] split_param_type_and_name(String param_descriptor) {
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
        return new String[]{param_type_name, param_name};
    }

    static void check_duplicates(String[] param_names) throws Exception {
        if (param_names == null) {
            return;
        }
        Set<String> set = new HashSet<String>();
        for (String param_name : param_names) {
            if (set.contains(param_name)) {
                throw new SQLException("Duplicated parameter names");
            }
            set.add(param_name);
        }
    }

    public static void check_required_attr(String node_name, String method_name_attr) throws Exception {
        if (method_name_attr == null || method_name_attr.trim().isEmpty()) {
            throw new Exception("<" + node_name + "...\n'method' is not set.");
        }
    }
}
