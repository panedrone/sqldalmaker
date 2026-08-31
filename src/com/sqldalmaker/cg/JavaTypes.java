/*
    Copyright 2011-2026 sqldalmaker@gmail.com
    SQL DAL Maker Website: https://sqldalmaker.sourceforge.net/
    Read LICENSE.txt in the root of this project/archive for details.
 */
package com.sqldalmaker.cg;

import java.sql.Time;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

/*
 * Java type names as they arrive from JDBC metadata.
 */
public class JavaTypes {

    // http://stackoverflow.com/questions/5032898/how-to-instantiate-class-class-for-a-primitive-type
    public static final Map<String, Class<?>> PRIMITIVE_CLASSES = new HashMap<String, Class<?>>();
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
    private static String java_primitive_name_to_class_name(String name) {
        Class<?> clazz = PRIMITIVE_CLASSES.get(name);
        if (clazz != null) {
            return clazz.getName();
        } else {
            return name;
        }
    }
    public static String refine_java_type_name(String java_type_name) throws ClassNotFoundException {
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
}
