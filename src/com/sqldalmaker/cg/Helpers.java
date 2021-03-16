/*
 * Copyright 2011-2021 sqldalmaker@gmail.com
 * Read LICENSE.txt in the root of this project/archive.
 * Project web-site: http://sqldalmaker.sourceforge.net
 */
package com.sqldalmaker.cg;

import com.sqldalmaker.jaxb.settings.Settings;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author sqldalmaker@gmail.com
 */
public class Helpers {

    // http://stackoverflow.com/questions/5032898/how-to-instantiate-class-class-for-a-primitive-type
    public static final Map<String, Class<?>> PRIMITIVE_CLASSES = new HashMap<String, Class<?>>();

    private static final String IFN = "Invalid file name: ";

    static {
        // Use the wrapper variant if necessary, like Integer.class,
        // so that you can instantiate it.
        // http://www.idevelopment.info/data/Programming/java/miscellaneous_java/Java_Primitive_Types.html
        PRIMITIVE_CLASSES.put("byte", Byte.class);
        PRIMITIVE_CLASSES.put("short", Short.class);
        PRIMITIVE_CLASSES.put("char", Character.class);
        PRIMITIVE_CLASSES.put("int", Integer.class);
        PRIMITIVE_CLASSES.put("long", Long.class);
        PRIMITIVE_CLASSES.put("float", Float.class);
        PRIMITIVE_CLASSES.put("double", Double.class);
    }

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

    public static String camel_case_to_snake_case(String src) {
        // http://stackoverflow.com/questions/10310321/regex-for-converting-camelcase-to-camel-case-in-java
        String regex = "([a-z])([A-Z]+)";
        String replacement = "$1_$2";
        return src.replaceAll(regex, replacement).toLowerCase();
    }

    public static String convert_file_name_to_snake_case(String class_name, String ext) {
        // http://stackoverflow.com/questions/221320/standard-file-naming-conventions-in-ruby
        // In Rails the convention of using underscores is necessary (almost).
        return Helpers.camel_case_to_snake_case(class_name) + "." + ext;
    }

    private static String java_primitive_name_to_class_name(String name) {
        Class<?> clazz = PRIMITIVE_CLASSES.get(name);
        if (clazz != null) {
            return clazz.getName();
        } else {
            return name;
        }
    }

    public static String process_java_type_name(String java_type_name) throws ClassNotFoundException {
        java_type_name = java_primitive_name_to_class_name(java_type_name);
        // does not throw Exception for "[B"; returns byte[]
        Class<?> cl = Class.forName(java_type_name);
        if (cl.isArray()) {
            // Returns the simple name of the underlying class as given in
            // the source code. Returns an empty string if the underlying
            // class is anonymous. The simple name of an array is the simple
            // name of the component type with "[]" appended.
            // In particular the simple name of an array whose component
            // type is anonymous is "[]".
            java_type_name = cl.getSimpleName();

        } else if (java.sql.Date.class.equals(cl) || Time.class.equals(cl) || Timestamp.class.equals(cl)) {
            // JDBC date-time types will be rendered as java.util.Date.
            // To assign parameter of type java.util.Date it should be
            // converted to java.sql.Timestamp
            return java.util.Date.class.getName();
        }
        return java_type_name;
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
        return str.length() > 0;
    }

    public static String concat_path(String seg0, String seg1) {
        return seg0 + "/" + seg1;
    }

    public static String concat_path(String seg0, String seg1, String seg2) {
        String res = concat_path(seg0, seg1);
        return concat_path(res, seg2);
    }

    public static FieldNamesMode get_field_names_mode(Settings settings) {
        FieldNamesMode field_names_mode;
        int fnm = settings.getDto().getFieldNamesMode();
        if (fnm == 1) {
            field_names_mode = FieldNamesMode.LOWER_CAMEL_CASE;
        } else {
            field_names_mode = FieldNamesMode.SNAKE_CASE;
        }
        return field_names_mode;
    }

    // Java File exists Case sensitive
    // https://stackoverflow.com/questions/34603505/java-file-exists-case-sensitive-jpg-and-jpg

    public static boolean exists(File dir, String filename) {
        String[] files = dir.list();
        for (String file : files)
            if (file.equals(filename))
                return true;
        return false;
    }

    // http://www.java2s.com/Tutorial/Java/0180__File/LoadatextfilecontentsasaString.htm

    public static String load_text_from_file(String file_path) throws IOException {
        File file = new File(file_path);
        File dir = new File(file.getParent());
        String file_name = file.getName();
        if (!exists(dir, file_name)) {
            throw new IOException("File not found (case-sensitive): " + file_name);
        }
        FileReader reader = new FileReader(file);
        try {
            return load_text(reader);
        } finally {
            reader.close();
        }
    }

    public static String load_text(InputStreamReader reader) throws IOException {
        int len;
        char[] chr = new char[4096];
        StringBuilder buffer = new StringBuilder();
        while ((len = reader.read(chr)) > 0) {
            buffer.append(chr, 0, len);
        }
        return buffer.toString();
    }

    public static String[] get_listed_items(String list, boolean allow_semicolon) throws Exception {
        if (list != null && list.length() > 0) {
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
            items = list.split("[,]");
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
                String []newarr = new String[n + 1];
                for (int i = 0; i < n; i++)
                    newarr[i] = items[i];
                newarr[n] = last_arr;
                return newarr;
            } else {
                return items;
            }
        }
        return new String[]{};
    }

    private static void check_item(String name, boolean allow_semicolon) throws Exception {
        if (name == null || name.length() == 0) {
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
            if (!is_letter_or_digit && ch != '$' && ch != '_' && ch != '~' && ch != ':') {
                throw new Exception("Invalid character in the name of item: " + name);
            }
        }
    }

    public static String get_dao_class_name(String dao_xml_path) throws Exception {
        String[] parts = dao_xml_path.split("[/\\\\]");
        if (parts.length == 0) {
            throw new Exception(IFN + dao_xml_path);
        }
        parts = parts[parts.length - 1].split("\\.");
        String class_name;
        switch (parts.length) {
            case 2:
                class_name = parts[0];
                if (!parts[1].equals("xml")) {
                    throw new Exception(IFN + dao_xml_path);
                }
                break;
            case 3:
                if (parts[0].equals("dao")) {
                    class_name = parts[1];
                } else {
                    class_name = parts[0];
                }
                if (!parts[2].equals("xml")) {
                    throw new Exception(IFN + dao_xml_path);
                }
                break;
            default:
                throw new Exception(IFN + dao_xml_path);
        }
        if (class_name.length() == 0) {
            throw new Exception(IFN + dao_xml_path);
        }
        if (Character.isLowerCase(class_name.charAt(0))) {
            throw new Exception("Class name must start with an uppercase letter: " + class_name);
        }
        return class_name;
    }

    public static InputStream get_resource_as_stream_2(String res_path) throws Exception {
        // swing app wants 'resources/' but plug-in wants '/resources/' WHY?
        ClassLoader cl = Helpers.class.getClassLoader();
        InputStream is = cl.getResourceAsStream(res_path);
        if (is == null) {
            is = cl.getResourceAsStream("/" + res_path);
        }
        if (is == null) {
            throw new Exception("Resource not found: " + res_path);
        }
        return is;
    }

    //
    // http://www.devdaily.com/blog/post/java/read-text-file-from-jar-file
    //
    public static String read_from_jar_file_2(String res_name) throws Exception {
        InputStream is = get_resource_as_stream_2(res_name);
        try {
            InputStreamReader reader = new InputStreamReader(is);
            try {
                return load_text(reader);
            } finally {
                reader.close();
            }
        } finally {
            is.close();
        }
    }

//    private static boolean is_class_of(String type, Class<?> clazz) {
//        // getSimpleName is used for types of parameters that are declared in XML
//        return (type.equals(clazz.getName()) || type.equals(clazz.getSimpleName()));
//    }
//
//    private static boolean is_java_type(String type) {
//        try {
//            Class.forName(type);
//            return true;
//        } catch (ClassNotFoundException e) {
//            return false;
//        }
//    }

    public static void convert_to_ruby_type_names(List<FieldInfo> fields) {

    }

    public static StringBuilder get_only_pk_warning(String method_name) {
        // if all values of the table are the parts of PK,
        // SQL will be invalid like ''UPDATE term_groups SET WHERE g_id
        // = ? AND t_id = ?'
        // (missing assignments between SET and WHERE)
        String msg = Helpers.get_only_pk_message(method_name);
        StringBuilder buffer = new StringBuilder();
        Helpers.build_warning_comment(buffer, msg);
        return buffer;
    }

    public static StringBuilder get_no_pk_warning(String method_name) {
        String msg = Helpers.get_no_pk_message(method_name);
        StringBuilder buffer = new StringBuilder();
        Helpers.build_warning_comment(buffer, msg);
        return buffer;
    }

    public static void build_warning_comment(StringBuilder buffer, String msg) {
        String ls = System.getProperty("line.separator");
        buffer.append(ls);
        buffer.append(msg);
        buffer.append(ls);
    }

    public static String get_no_pk_message(String method_name) {
        return "    // INFO: " + method_name + " is omitted because PK is not detected.";
    }

    public static String get_only_pk_message(String method_name) {
        return "    // INFO: " + method_name + " is omitted because all columns are part of PK.";
    }

    public static String get_error_message(String msg, Throwable e) {
        return msg + " " + e.getMessage();
    }

    static void validate_java_type_name(final String type_name) throws Exception {
        if (type_name == null) {
            throw new Exception("Cannot detect type name. . Try to update XSD files from the tab 'Admin'. Then check existing XML to conform updates.");
        }
        String type;
        String[] arr_parts = type_name.split("\\[");
        if (arr_parts.length == 2 && "]".equals(arr_parts[1].trim())) {
            type = arr_parts[0].trim();
        } else {
            type = type_name;
        }
        try {
            Helpers.process_java_type_name(type);
        } catch (ClassNotFoundException e) {
            String java_class_name2 = "java.lang." + type;
            try {
                Helpers.process_java_type_name(java_class_name2);
            } catch (ClassNotFoundException e1) {
                throw new Exception("Invalid type name: " + type_name);
            }
        }
    }

    static String[] parse_param_descriptor(String param_descriptor) {
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

    public static void check_required_attr(String node_name, String method_name_attr) throws Exception {
        if (method_name_attr == null || method_name_attr.length() == 0) {
            throw new Exception("<" + node_name + "...\n'method' is not set.");
        }
    }

    public static String to_lower_camel_or_title_case(String str, boolean title_case) {
        if (!str.contains("_")) {
            if (title_case) {
                return Helpers.replace_char_at(str, 0, Character.toTitleCase(str.charAt(0)));
            } else {
                boolean all_is_upper_case = Helpers.is_upper_case(str);
                if (all_is_upper_case) {
                    str = str.toLowerCase();
                    return str;
                } else {
                    return Helpers.replace_char_at(str, 0, Character.toLowerCase(str.charAt(0)));
                }
            }
        }
        StringBuilder sb = new StringBuilder();
        String[] arr = str.split("_");
        for (int i = 0; i < arr.length; i++) {
            String s = arr[i];
            if (s.length() == 0) {
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
                boolean all_is_upper_case = Helpers.is_upper_case(tail);
                if (all_is_upper_case) {
                    sb.append(tail.toLowerCase());
                } else {
                    sb.append(tail);
                }
            }
        }
        return sb.toString();
    }
}
