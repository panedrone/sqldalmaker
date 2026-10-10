/*
    Copyright 2011-2026 sqldalmaker@gmail.com
    SQL DAL Maker Website: https://sqldalmaker.sourceforge.net/
    Read LICENSE.txt in the root of this project/archive for details.
 */
package com.sqldalmaker.cg;

import com.sqldalmaker.jaxb.settings.Type;
import com.sqldalmaker.jaxb.settings.TypeMap;

import java.util.HashMap;
import java.util.Map;

/*
 * 30.08.2026 14:00 1.331 Claude refactor
 * 25.10.2022 09:26
 */
class JaxbTypeMap {

    private final Map<String, String> detected = new HashMap<String, String>();
    private final String default_type;

    JaxbTypeMap(TypeMap jaxb_type_map) {
        if (jaxb_type_map == null) {
            default_type = null;
            return;
        }
        default_type = jaxb_type_map.getDefault();
        for (Type t : jaxb_type_map.getType()) {
            detected.put(t.getDetected(), t.getTarget());
        }
    }

    // 'detected' in here means
    //      1) detected using JDBC or
    //      2) detected from explicit declarations in XML meta-program

    String get_target_type_name(String detected_type_name) {
        if (detected.isEmpty()) {
            // if no re-definitions, pass any type as-is (independently of 'default')
            return detected_type_name;
        }
        if (detected.containsKey(detected_type_name)) {
            return detected.get(detected_type_name);
        }
        if (default_type == null || default_type.trim().isEmpty()) {
            return detected_type_name; // rendered as-is if not found besides of "detected"
        }
        return default_type;
    }

    // A target type may be "type->assign_func": the type to render and the function that
    // reads a value of it from a record (e.g. "Integer->getInteger" in Java).
    // Returns {type, assign_func}; assign_func is null if there is none.
    static String[] split_target_type(String target_type_name) {
        String[] assign_parts = target_type_name.split("->");
        if (assign_parts.length > 1) {
            return new String[]{assign_parts[0].trim(), assign_parts[1].trim()};
        }
        return new String[]{target_type_name.trim(), null};
    }

}
