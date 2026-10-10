/*
    Copyright 2011-2026 sqldalmaker@gmail.com
    SQL DAL Maker Website: https://sqldalmaker.sourceforge.net/
    Read LICENSE.txt in the root of this project/archive for details.
 */
package com.sqldalmaker.cg;

import java.util.ArrayList;
import java.util.List;

/*
 * 10.10.2026 1.333 Claude: interpretation part of JdbcSqlParamInfo
 *
 * Parameters of SQL statements as ParamInfo-s.
 */
class ParamFields {

    //
    // it should not be used for CRUD
    //
    static List<ParamInfo> read(
            IDbMetadata md,
            JaxbTypeMap type_map,
            JaxbMacros jaxb_macros,
            String jdbc_sql,
            FieldNamesMode param_names_mode,
            String[] method_param_descriptors) throws Exception {

        MethodDeclarations.check_duplicates(method_param_descriptors);
        if (method_param_descriptors == null) {
            method_param_descriptors = new String[]{};
        }
        IDbMetadata.Params params = md.read_sql_params(jdbc_sql, method_param_descriptors.length);
        return create(params, type_map, jaxb_macros, param_names_mode, method_param_descriptors);
    }

    static List<ParamInfo> create(
            IDbMetadata.Params params,
            JaxbTypeMap type_map,
            JaxbMacros jaxb_macros,
            FieldNamesMode param_names_mode,
            String[] method_param_descriptors) throws Exception {

        List<ParamInfo> res = new ArrayList<>();
        if (params.count_unknown) {
            for (String param_descriptor : method_param_descriptors) {
                res.add(create_param_info(type_map, jaxb_macros, param_names_mode, param_descriptor, null));
            }
            return res;
        }
        int not_cb_array_params_count = 0;
        for (int i = 0; i < method_param_descriptors.length; i++) {
            String param_descriptor = method_param_descriptors[i].trim();
            if (param_descriptor.startsWith("[")) {
                // array of callbacks to fetch implicit cursors
                // is included in method_param_descriptors,
                // but implicit cursors are not detected as JDBC parameters
                if (!param_descriptor.endsWith("]")) {
                    throw new Exception("Ending ']' expected");
                }
            } else {
                not_cb_array_params_count++;
                // a column-like FieldInfo: type-map macros expect a column
                String jdbc_param_type_name = params.java_types.get(i);
                FieldInfo base_fi = new FieldInfo(param_names_mode, jdbc_param_type_name, "base_fi", "base_fi");
                res.add(create_param_info(type_map, jaxb_macros, param_names_mode, param_descriptor, base_fi));
            }
        }
        if (params.count != not_cb_array_params_count) {
            throw new Exception("Parameters declared in method: " + method_param_descriptors.length
                    + ", detected by MetaData: " + params.count);
        }
        return res;
    }

    // 'base_fi' (nullable) is the column the parameter is compared with: its type is the default one
    static ParamInfo create_param_info(
            JaxbTypeMap jaxb_type_map,
            JaxbMacros jaxb_macros,
            FieldNamesMode param_names_mode,
            String param_descriptor,
            FieldInfo base_fi) throws Exception {

        String[] parts = MethodDeclarations.split_param_type_and_name(param_descriptor);
        String param_name = parts[1];
        String target_type_name;
        if (parts[0] == null || parts[0].trim().isEmpty()) {
            if (base_fi == null) {
                target_type_name = jaxb_type_map.get_target_type_name(Object.class.getName());
            } else {
                target_type_name = jaxb_type_map.get_target_type_name(base_fi.getType());
                target_type_name = jaxb_macros.parse_target_type_name(target_type_name, base_fi); // use base_fi for parsing
            }
        } else {
            target_type_name = jaxb_type_map.get_target_type_name(parts[0]);
        }
        ParamInfo res = new ParamInfo(param_names_mode, Object.class.getName(), param_name);
        return ParamInfo.with_type(res, target_type_name);
    }
}
