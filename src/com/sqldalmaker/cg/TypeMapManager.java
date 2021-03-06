package com.sqldalmaker.cg;

import com.sqldalmaker.jaxb.settings.Type;
import com.sqldalmaker.jaxb.settings.TypeMap;

import java.util.HashMap;
import java.util.Map;

class TypeMapManager {

    private final Map<String, String> detected = new HashMap<String, String>();
    private final String default_type;

    boolean is_defined() {
        if (detected.size() > 0) {
            return true;
        }
        return false;
    }

    TypeMapManager(TypeMap jaxb_type_map) throws Exception {
        if (jaxb_type_map == null) {
            default_type = null;
            return;
        }
        default_type = jaxb_type_map.getDefault();
        for (Type t : jaxb_type_map.getType()) {
            // no "hidden magic" anymore. only explicit specifications.
            // String detected_type = get_qualified_java_name(t.getDetected());
            String detected_type = t.getDetected();
            if (detected.containsKey(detected_type)) {
                throw new Exception("Duplicated in settings.xml -> type-map: " + detected_type);
            }
            String target_type = t.getTarget();
            detected.put(detected_type, target_type);
            // don't break, check to the end
        }
    }

    // 'detected' in here means
    //      1) detected using JDBC or
    //      2) detected from explicit declarations in XML meta-program

    public String get_rendered_type_name(String detected_type_name) throws Exception {
        if (detected.size() == 0) {
            // if no re-definitions, pass any type as-is (independently of 'default')
            return detected_type_name;
        }
        // no "hidden magic" anymore. only explicit specifications.
        // String detected_type_name = get_qualified_java_name(detected_type_name);
        if (detected.containsKey(detected_type_name)) {
            return detected.get(detected_type_name);
        }
        if (default_type == null || default_type.trim().length() == 0) {
            // if the type is not 'recognised', it is rendered as-is
            return detected_type_name;
        }
        return default_type;
    }

    // no "hidden magic" anymore. only explicit specifications.

//    private static String get_qualified_java_name(String java_class_name) {
//        java_class_name = java_class_name.replaceAll("\\s+", "");
//        String element_name;
//        boolean is_array;
//        if (java_class_name.contains("[")) {
//            element_name = java_class_name.replace('[', ' ').replace(']', ' ').trim();
//            is_array = true;
//        } else {
//            is_array = false;
//            element_name = java_class_name;
//        }
//        boolean is_primitive = Helpers.PRIMITIVE_CLASSES.containsKey(element_name);
//        if (!is_primitive && !java_class_name.contains(".")) {
//            element_name = "java.lang." + element_name;
//        }
//        java_class_name = element_name;
//        if (is_array) {
//            java_class_name += " []";
//        }
//        return java_class_name;
//    }
}
