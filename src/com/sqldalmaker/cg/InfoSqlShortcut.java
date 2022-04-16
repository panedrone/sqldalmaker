/*
    Copyright 2011-2022 sqldalmaker@gmail.com
    SQL DAL Maker Website: http://sqldalmaker.sourceforge.net
    Read LICENSE.txt in the root of this project/archive for details.
 */
package com.sqldalmaker.cg;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author sqldalmaker@gmail.com
 */
class InfoSqlShortcut {

    public static void get_all(
            JaxbUtils.JaxbTypeMap type_map,
            FieldNamesMode param_names_mode,
            String[] method_param_descriptors,
            List<FieldInfo> fields_all,
            String[] filter_col_names,
            List<FieldInfo> _params) throws Exception {

        Map<String, FieldInfo> all_col_names_map = new HashMap<String, FieldInfo>();
        for (FieldInfo fi : fields_all) {
            String cn = fi.getColumnName();
            all_col_names_map.put(cn, fi);
        }
        List<FieldInfo> fields_filter = new ArrayList<FieldInfo>();
        for (String fcn : filter_col_names) {
            if (!all_col_names_map.containsKey(fcn))
                throw new Exception("Invalid SQL-shortcut. Table column '" + fcn + "' not found. Ensure upper/lower case.");
            FieldInfo fi = all_col_names_map.get(fcn);
            fields_filter.add(fi);
        }
        // assign param types from table!! without dto-refinement!!!
        if (method_param_descriptors.length != fields_filter.size()) {
            throw new Exception("Invalid SQL-shortcut. Methof parameters declared: " + method_param_descriptors.length
                    + ". SQL parameters expected: " + fields_filter.size());
        }
        for (int i = 0; i < method_param_descriptors.length; i++) {
            String param_descriptor = method_param_descriptors[i];
            FieldInfo fi = fields_filter.get(i);
            String curr_type = fi.getType();
            String default_param_type_name = get_target_type_by_type_map(type_map, curr_type);
            FieldInfo pi = InfoCustomSql.create_param_info(type_map, param_names_mode, param_descriptor, default_param_type_name);
            _params.add(pi);
        }
    }

    private static String get_target_type_by_type_map(JaxbUtils.JaxbTypeMap type_map, String detected) {
        String target_type_name = type_map.get_target_type_name(detected);
        return target_type_name;
    }
}
