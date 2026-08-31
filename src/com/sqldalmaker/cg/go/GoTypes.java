/*
    Copyright 2011-2026 sqldalmaker@gmail.com
    SQL DAL Maker Website: https://sqldalmaker.sourceforge.net/
    Read LICENSE.txt in the root of this project/archive for details.
 */
package com.sqldalmaker.cg.go;

import com.sqldalmaker.cg.FieldInfo;

/*
 * @author sqldalmaker@gmail.com
 *
 * Parsing of the Go type as it is written in 'settings.xml':
 *     "time:time.Time `json:\"t_date\"`" == import + type + struct tag
 */
class GoTypes {

    static String get_type_import(FieldInfo fi) {
        String initial_type = fi.getType();
        if (initial_type == null) {
            return null;
        }
        String[] type_parts = initial_type.trim().split("\\s+");
        if (type_parts.length < 1) {
            return null;
        }
        String type_part = type_parts[0]; // type_part = "time:time.Time"
        int import_end = type_part.indexOf(':');
        if (import_end == -1) {
            return null;
        }
        String res = type_part.substring(0, import_end);
        return res;
    }

    static String get_type_without_import(String type) {
        // "time:time.Time `json:"t_date"`{$0}"
        String initial_type = type.split("[$]")[0]; // it is needed for parameters
        int import_end = initial_type.split("\\s+")[0].indexOf(":");
        if (import_end == -1) {
            return initial_type;
        }
        String res = initial_type.substring(import_end + 1);
        return res;
    }

    static String get_type_without_import_and_tag(FieldInfo fi) {
        String initial_type = get_type_without_import(fi.getType()); // "time:time.Time `json:"t_date"`"
        String[] type_parts = initial_type.split("\\s+"); // it returns the whole string if there are no "\\s+"
        String type_part = type_parts[0]; // type_part = "time:time.Time"
        return type_part;
    }

}
