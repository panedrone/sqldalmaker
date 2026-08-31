/*
    Copyright 2011-2026 sqldalmaker@gmail.com
    SQL DAL Maker Website: https://sqldalmaker.sourceforge.net/
    Read LICENSE.txt in the root of this project/archive for details.
 */
package com.sqldalmaker.cg;

/*
 * Conversions between the naming conventions of databases and of target languages.
 */
public class Names {

    public static String camel_case_to_lower_snake_case(String src) {
        // http://stackoverflow.com/questions/10310321/regex-for-converting-camelcase-to-camel-case-in-java
        String regex = "([a-z])([A-Z]+)";
        String replacement = "$1_$2";
        return src.replaceAll(regex, replacement).toLowerCase();
    }

    public static String to_kebab_case(String src) {
        String sc = camel_case_to_lower_snake_case(src);
        String[] parts = sc.split("_");
        String kc = String.join("-", parts);
        return kc;
    }

    public static String convert_file_name_to_snake_case(String class_name, String ext) {
        // http://stackoverflow.com/questions/221320/standard-file-naming-conventions-in-ruby
        // In Rails the convention of using underscores is necessary (almost).
        return camel_case_to_lower_snake_case(class_name) + "." + ext;
    }

    public static String replace_char_at(String s, int pos, char c) {
        // http://www.rgagnon.com/javadetails/java-0030.html
        StringBuilder buf = new StringBuilder(s);
        buf.setCharAt(pos, c);
        return buf.toString();
    }

    public static boolean is_upper_case(String str) {
        for (int i = 0; i < str.length(); i++) {
            if (Character.isLowerCase(str.charAt(i))) {
                return false;
            }
        }
        return !str.isEmpty();
    }

    public static String get_method_name(String method_name, FieldNamesMode field_names_mode) {
        if (FieldNamesMode.LOWER_CAMEL_CASE.equals(field_names_mode)) {
            return lower_camel_case(method_name);
        } else if (FieldNamesMode.TITLE_CASE.equals(field_names_mode)) {
            return title_case(method_name);
        } else if (FieldNamesMode.SNAKE_CASE.equals(field_names_mode)) {
            return camel_case_to_lower_snake_case(method_name);
        }
        return method_name;
    }

    final static String[] _ids = new String[]{"Id", "Uuid"};

    public static String lower_camel_case(String str) {
        String res = _lower_camel_or_title_case(str, false);
        for (String id : _ids) {
            if (res.equals(id)) {
                return res.toUpperCase();
            }
            if (res.endsWith(id)) {
                return res.substring(0, res.length() - id.length()) + id.toUpperCase();
            }
        }
        return res;
    }

    public static String title_case(String str) {
        String res = _lower_camel_or_title_case(str, true);
        for (String id : _ids) {
            if (res.equals(id)) {
                return res.toUpperCase();
            }
            if (res.endsWith(id)) {
                return res.substring(0, res.length() - id.length()) + id.toUpperCase();
            }
        }
        return res;
    }

    private static String _lower_camel_or_title_case(String str, boolean title_case) {
        if (str.toUpperCase().equals(str)) {
            str = str.toLowerCase(); // "PROJECTS" --> "projects"
        }
        if (!str.contains("_")) {
            if (title_case) {
                return replace_char_at(str, 0, Character.toTitleCase(str.charAt(0)));
            } else {
                boolean all_is_upper_case = is_upper_case(str);
                if (all_is_upper_case) {
                    str = str.toLowerCase();
                    return str;
                } else {
                    return replace_char_at(str, 0, Character.toLowerCase(str.charAt(0)));
                }
            }
        }
        StringBuilder sb = new StringBuilder();
        String[] arr = str.split("_");
        for (int i = 0; i < arr.length; i++) {
            String s = arr[i];
            if (s.isEmpty()) {
                continue; // E.g. _ALL_FILE_GROUPS
            }
            char ch0 = s.charAt(0);
            if (i == 0) {
                if (title_case) {
                    ch0 = Character.toTitleCase(ch0);
                } else {
                    ch0 = Character.toLowerCase(ch0);
                }
            } else {
                ch0 = Character.toTitleCase(ch0);
            }
            sb.append(ch0);
            if (s.length() > 1) {
                String tail = s.substring(1);
                boolean all_is_upper_case = is_upper_case(tail);
                if (all_is_upper_case) {
                    sb.append(tail.toLowerCase());
                } else {
                    sb.append(tail);
                }
            }
        }
        return sb.toString();
    }

    public static String get_pk_col_name_alias(String pk_col_name) {
        // === panedrone: WHY ALIASES:
        //   1) xerial SQLite3 getPrimaryKeys may return pk_col_names in lower case
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
}
