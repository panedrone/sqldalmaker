/*
 * Copyright 2011-2018 sqldalmaker@gmail.com
 * SQL DAL Maker Website: http://sqldalmaker.sourceforge.net
 * Read LICENSE.txt in the root of this project/archive for details.
 */
package com.sqldalmaker.cg;

import com.sqldalmaker.jaxb.dto.DtoClass;
import com.sqldalmaker.jaxb.dto.DtoClasses;

import javax.xml.bind.annotation.XmlRootElement;
import java.io.*;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author sqldalmaker@gmail.com
 *
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

    public static String camel_case_to_lower_under_scores(String src) {

        // http://stackoverflow.com/questions/10310321/regex-for-converting-camelcase-to-camel-case-in-java
        String regex = "([a-z])([A-Z]+)";

        String replacement = "$1_$2";

        return src.replaceAll(regex, replacement).toLowerCase();
    }

    public static String convert_to_ruby_file_name(String class_name) {

        // http://stackoverflow.com/questions/221320/standard-file-naming-conventions-in-ruby
        // In Rails the convention of using underscores is necessary (almost).
        return Helpers.camel_case_to_lower_under_scores(class_name) + ".rb";
    }

    private static String try_convert_primitive(String name) {

        if (PRIMITIVE_CLASSES.containsKey(name)) {

            return PRIMITIVE_CLASSES.get(name).getName();

        } else {

            return name;
        }
    }

    public static String process_class_name(String java_class_name) throws ClassNotFoundException {

        java_class_name = try_convert_primitive(java_class_name);

        // does not throw Exception for "[B"; returns byte[]
        Class<?> cl = Class.forName(java_class_name);

        if (cl.isArray()) {

            // Returns the simple name of the underlying class as given in
            // the source code. Returns an empty string if the underlying
            // class is anonymous. The simple name of an array is the simple
            // name of the component type with "[]" appended.
            // In particular the simple name of an array whose component
            // type is anonymous is "[]".
            java_class_name = cl.getSimpleName();

        } else if (java.sql.Date.class.equals(cl) || Time.class.equals(cl) || Timestamp.class.equals(cl)) {

            // JDBC date-time types will be rendered as java.util.Date.
            // To assign parameter of type java.util.Date it should be
            // converted to java.sql.Timestamp
            return java.util.Date.class.getName();
        }

        return java_class_name;
    }

    public static String get_error_message(String msg, Throwable e) {

        return msg + ": " + e.getMessage();
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

    public static String get_xml_node_name(Object element) {

        XmlRootElement attr = element.getClass().getAnnotation(XmlRootElement.class);

        return attr.name();
    }

    public static String sql_to_java_str(StringBuilder sql_buff) {

        return sql_to_java_str(sql_buff.toString());
    }

    public static String sql_to_java_str(String sql) {

        String parts[] = sql.split("(\\n|\\r)+");

        // "\n" it is OK for Eclipse debugger window:
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

            if (i < parts.length - 1) {

                String s = " \" " + new_line + "\t\t\t\t + \"";

                res.append(s);
            }
        }

        return res.toString();
    }

    public static String sql_to_php_str(StringBuilder sql_buff) {

        return sql_to_php_str(sql_buff.toString());
    }

    public static String sql_to_php_str(String sql) {

        String parts[] = sql.split("(\\n|\\r)+");

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

            if (i < parts.length - 1) {

                String s = " \" " + new_line + "\t\t\t\t . \"";

                res.append(s);
            }
        }

        return res.toString();
    }

    public static String sql_to_python_string(StringBuilder sql_buff) {

        return sql_to_python_string(sql_buff.toString());
    }

    public static String sql_to_ruby_string(StringBuilder sql_buff) {

        return sql_to_ruby_string(sql_buff.toString());
    }

    public static String sql_to_ruby_string(String sql) {

        return sql_to_python_string(sql);
    }

    public static String sql_to_python_string(String sql) {

        String parts[] = sql.split("(\\n|\\r)+");

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

    public static String sql_to_cpp_str(StringBuilder sql_buff) {

        return sql_to_cpp_str(sql_buff.toString());
    }

    public static String sql_to_cpp_str(String sql) {

        String parts[] = sql.split("(\\n|\\r)+");

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

    //
    // http://www.java2s.com/Tutorial/Java/0180__File/LoadatextfilecontentsasaString.htm
    //
    public static String load_text_from_file(String file_path) throws IOException {

        File file = new File(file_path);

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

    public static String[] get_listed_items(String list) {

        if (list != null && list.length() > 0) {

            String[] items;

            items = list.split("[,]");

            for (int i = 0; i < items.length; i++) {

                items[i] = items[i].trim();
            }

            return items;
        }

        return new String[]{};
    }

    public static String get_dao_class_name(String dao_xml_path) throws Exception {

        String[] parts = dao_xml_path.split("[/\\\\]");

        if (parts.length == 0) {

            throw new Exception(IFN + dao_xml_path);
        }

        parts = parts[parts.length - 1].split("\\.");

        String class_name;

        if (parts.length == 2) {

            class_name = parts[0];

            if (!parts[1].equals("xml")) {

                throw new Exception(IFN + dao_xml_path);
            }

        } else if (parts.length == 3) {

            if (parts[0].equals("dao")) {

                class_name = parts[1];

            } else {

                class_name = parts[0];
            }

            if (!parts[2].equals("xml")) {

                throw new Exception(IFN + dao_xml_path);
            }

        } else {

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

    public static DtoClass find_dto_class(String dto_attr, DtoClasses dto_classes) throws Exception {

        if (dto_attr == null || dto_attr.length() == 0) {

            throw new Exception("Invalid name of DTO class: " + dto_attr);
        }

        DtoClass res = null;

        int found = 0;

        for (DtoClass cls : dto_classes.getDtoClass()) {

            String name = cls.getName();

            if (name != null && name.equals(dto_attr)) {

                res = cls;

                found++;
            }
        }

        if (found == 0) {

            throw new Exception("DTO XML element not found: '" + dto_attr + "'");

        } else if (found > 1) {

            throw new Exception("Duplicate DTO XML elements for name='" + dto_attr + "' found.");
        }

        return res;
    }

    public static InputStream get_resource_as_stream_2(String res_path) throws Exception {

        // swing app wants 'resources/' but plug-in wants '/resources/' WHY?
        //
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

    public static boolean is_table_ref(String ref) {

        if (ref == null || ref.length() == 0) {

            return false;
        }

        if (is_sql_file_ref(ref)) {

            return false;
        }

        if (is_sql_shortcut_ref(ref)) {

            return false;
        }

        final char[] ILLEGAL_CHARACTERS = {'/', '\n', '\r', '\t', '\0', '\f', '`', '?', '*', '\\', '<', '>', '|',
            '\"'/* , ':' */, ';', ','};

        for (char c : ILLEGAL_CHARACTERS) {

            if (ref.contains(Character.toString(c))) {

                return false;
            }
        }

        // no empty strings separated by dots
        //
        String parts[] = ref.split("\\.", -1); // -1 to leave empty strings

        for (String s : parts) {

            if (s.length() == 0) {

                return false;
            }
        }

        return true;
    }

    public static boolean is_sql_shortcut_ref(String ref) {

        return ref != null && ref.length() >= 4 && ref.contains("(") && ref.trim().endsWith(")");
    }

    public static boolean is_sql_file_ref(String ref) {

        return ref != null && ref.length() > 4 && ref.endsWith(".sql");
    }

    private static boolean is_class_of(String type, Class<?> clazz) {

        // getSimpleName is used for types of parameters that are declared in XML
        //
        return (type.equals(clazz.getName()) || type.equals(clazz.getSimpleName()));
    }

    private static boolean is_java_type(String type) {

        try {

            Class.forName(type);

            return true;

        } catch (ClassNotFoundException e) {

            return false;
        }
    }

    public static void convert_to_python_type_names(List<FieldInfo> fields) {

        for (FieldInfo fi : fields) {

            String type = fi.getType();

            type = get_python_type_name(type);

            fi.setType(type);
        }
    }

    public static String get_python_type_name(String type) {

        if (is_class_of(type, String.class)) {

            return "basestring";
        }

        if (is_class_of(type, Boolean.class)) {

            return "bool";
        }

        if (is_class_of(type, java.util.Date.class)) {

            return "basestring"; // built-in, datetime.datetime can be used instead
        }

        if (is_class_of(type, Float.class)) {

            return "float"; // built-in
        }

        if (is_class_of(type, Double.class)) {

            return "float"; // built-in, no double
        }

        if (is_class_of(type, java.math.BigDecimal.class)) {

            return "float"; // built-in, maybe decimal.Decimal is better?
        }

        if (is_class_of(type, Integer.class)) {

            return "int"; // built-in
        }

        if (is_class_of(type, Long.class)) {

            return "long"; // built-in
        }

        // last chance for PythonRuby types specified in XML: datetime.datetime,
        // decimal.Decimal, etc.
        // (bool, int, long, float, basestring are processed by previous code)
        //
        if (!is_java_type(type)) {

            return type;
        }

        return "object";
    }

    public static void convert_to_ruby_type_names(List<FieldInfo> fields) {

    }

    public static String get_no_pk_message(String method_name) {

        return "\t// INFO: " + method_name + " is omitted because PK is not detected.";
    }

    public static String get_only_pk_message(String method_name) {

        return "\t// INFO: " + method_name + " is omitted because all columns are part of PK.";
    }

    public static void build_warning_comment(StringBuilder buffer, String msg) {

        String ls = System.getProperty("line.separator");
        buffer.append(ls);
        buffer.append(msg);
        buffer.append(ls);
    }

}
