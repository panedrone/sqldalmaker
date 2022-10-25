package com.sqldalmaker.cg;

import com.sqldalmaker.jaxb.settings.Type;
import com.sqldalmaker.jaxb.settings.TypeMap;

import java.util.HashMap;
import java.util.Map;

public class JaxbTypeMap {

    private final Map<String, String> detected = new HashMap<String, String>();
    private final String default_type;

    public JaxbTypeMap(TypeMap jaxb_type_map) {

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

    public String get_target_type_name(String detected_type_name) {

        if (detected.isEmpty()) {
            // if no re-definitions, pass any type as-is (independently of 'default')
            return detected_type_name;
        }
        if (detected.containsKey(detected_type_name)) {
            return detected.get(detected_type_name);
        }
        if (default_type == null || default_type.trim().length() == 0) {
            return detected_type_name; // rendered as-is if not found besides of "detected"
        }
        return default_type;
    }

}
