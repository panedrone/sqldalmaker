/*
    Copyright 2011-2022 sqldalmaker@gmail.com
    SQL DAL Maker Website: http://sqldalmaker.sourceforge.net
    Read LICENSE.txt in the root of this project/archive for details.
 */
package com.sqldalmaker.cg;

import com.sqldalmaker.jaxb.dto.DtoClass;

import java.sql.Connection;
import java.util.*;

/**
 * @author sqldalmaker@gmail.com
 */
class InfoDtoClass {

    private final Connection conn;

    private final JaxbUtils.JaxbTypeMap type_map;
    private final JaxbUtils.JaxbMacros macros;

    private final FieldNamesMode dto_field_names_mode;

    public InfoDtoClass(Connection conn,
                        JaxbUtils.JaxbTypeMap type_map,
                        JaxbUtils.JaxbMacros macros,
                        FieldNamesMode dto_field_names_mode) {

        this.conn = conn;
        this.type_map = type_map;
        this.macros = macros;
        this.dto_field_names_mode = dto_field_names_mode;
    }

    public Map<String, FieldInfo> get_dto_field_info(DtoClass jaxb_dto_class,
                                                     String sql_root_abs_path,
                                                     List<FieldInfo> _dto_fields) throws Exception {

        Map<String, FieldInfo> fields_map = _prepare_detected(jaxb_dto_class, sql_root_abs_path, _dto_fields);
        if (type_map == null) {
            return fields_map; // DTO XML wizard
        }
        Set<String> refined_by_field_jaxb = _refine_by_jaxb_dto_class_fields(jaxb_dto_class, _dto_fields, fields_map);
        _refine_others(refined_by_field_jaxb, _dto_fields);
        _substitute_built_in_macros(_dto_fields);
        _substitute_type_params(_dto_fields);
        return fields_map;
    }

    private void _substitute_type_params(List<FieldInfo> _dto_fields) {
        for (FieldInfo fi : _dto_fields) {
            this.macros.substitute_type_params(fi);
        }
    }

    private Map<String, FieldInfo> _prepare_detected(DtoClass jaxb_dto_class,
                                                     String sql_root_abs_path,
                                                     List<FieldInfo> _dto_fields) throws Exception {

        String dto_class_name = jaxb_dto_class.getName();
        String model = "";
        int model_name_end_index = dto_class_name.indexOf('-');
        if (model_name_end_index != -1) {
            model = dto_class_name.substring(0, model_name_end_index + 1);
        }
        Map<String, FieldInfo> fields_map = new HashMap<String, FieldInfo>();
        _dto_fields.clear();
        String jaxb_dto_class_ref = jaxb_dto_class.getRef();
        if (!SqlUtils.is_empty_ref(jaxb_dto_class_ref)) {
            String jdbc_sql = SqlUtils.jdbc_sql_by_dto_class_ref(jaxb_dto_class_ref, sql_root_abs_path);
            InfoCustomSql.get_field_info_by_jdbc_sql(model, conn, dto_field_names_mode, jdbc_sql, fields_map, _dto_fields);
            if (SqlUtils.is_table_ref(jaxb_dto_class_ref)) {
                String table_name = jaxb_dto_class_ref;
                _fill_by_table(model, table_name, _dto_fields, fields_map);
            } else if (SqlUtils.is_sql_shortcut_ref(jaxb_dto_class_ref)) {
                String[] parts = SqlUtils.parse_sql_shortcut_ref(jaxb_dto_class_ref);
                String table_name = parts[0];
                _fill_by_table(model, table_name, _dto_fields, fields_map);
            }
        }
        return fields_map;
    }

    private void _refine_others(Set<String> refined_by_field_jaxb,
                                List<FieldInfo> _dto_fields) throws Exception {

        for (FieldInfo fi : _dto_fields) {
            if (refined_by_field_jaxb.contains(fi.getColumnName())) {
                continue;
            }
            String detected_jdbc_type_name = fi.getType().trim();
            String target_type = get_target_type_by_type_map(type_map, detected_jdbc_type_name);
            _refine_fi(fi, target_type);
        }
    }

    private Set<String> _refine_by_jaxb_dto_class_fields(DtoClass jaxb_dto_class,
                                                         List<FieldInfo> _dto_fields,
                                                         Map<String, FieldInfo> fields_map) throws Exception {

        Set<String> refined_by_field_jaxb = new HashSet<String>();
        List<DtoClass.Field> jaxb_fields = jaxb_dto_class.getField(); // not null!!
        for (DtoClass.Field jaxb_field : jaxb_fields) {
            String jaxb_field_col_name = jaxb_field.getColumn();
            String jaxb_field_type_name = jaxb_field.getType();
            FieldInfo fi;
            if (fields_map.containsKey(jaxb_field_col_name)) {
                fi = fields_map.get(jaxb_field_col_name);
            } else { // add the field declared in XML, but missing in SQL
                fi = new FieldInfo(dto_field_names_mode, jaxb_field_type_name, jaxb_field_col_name,
                        "xml(" + jaxb_field_col_name + ")");
                _dto_fields.add(fi);
                fields_map.put(jaxb_field_col_name, fi);
            }
            _refine_fi(fi, jaxb_field_type_name);
            refined_by_field_jaxb.add(fi.getColumnName());
        }
        return refined_by_field_jaxb;
    }

    private void _fill_by_table(String model,
                                String table_name,
                                List<FieldInfo> _dto_fields,
                                Map<String, FieldInfo> fields_map) throws Exception {

        String explicit_pk = "*";
        InfoDbTable info = new InfoDbTable(model, conn, type_map, dto_field_names_mode, table_name, explicit_pk);
        _dto_fields.clear();
        _dto_fields.addAll(info.fields_all);
        fields_map.clear();
        fields_map.putAll(info.fields_map);
    }

    private void _refine_fi(FieldInfo fi,
                            String type_name) throws Exception {

        type_name = type_name.trim();
        String target_type;
        int local_field_type_params_start = type_name.indexOf('|');
        if (local_field_type_params_start == -1) { // explicit_type_name only, no parameters "int64"
            String type_map_target_type = get_target_type_by_type_map(type_map, type_name);
            target_type = _parse_target_type(type_map_target_type, macros);
        } else {
            String local_field_type_explicit;
            if (local_field_type_params_start == 0) { // "|0: hello0|1:hello1"
                local_field_type_explicit = "";
            } else { // "{type}|0:Integer|1:hello1"
                local_field_type_explicit = type_name.substring(0, local_field_type_params_start).trim();
            }
            String parsed_target_type;
            if (local_field_type_explicit.length() == 0) { // only parameters "|0:hello"
                String detected_jdbc_type_name = fi.getType();
                String type_map_target_type = get_target_type_by_type_map(type_map, detected_jdbc_type_name);
                parsed_target_type = _parse_target_type(type_map_target_type, macros);
            } else { // local_field_type_explicit + parameters "int64{json}|0:hello"
                parsed_target_type = _parse_target_type(local_field_type_explicit, macros);
            }
            String local_field_type_params = type_name.substring(local_field_type_params_start);
            target_type = parsed_target_type + local_field_type_params;
        }
        fi.refine_rendered_type(target_type);
    }

    private String _parse_target_type(String target,
                                      JaxbUtils.JaxbMacros global_markers) throws Exception {

        return _parse_target_type_recursive(0, target, global_markers);
    }

    private static String _parse_target_type_recursive(int depth,
                                                       String target,
                                                       JaxbUtils.JaxbMacros global_markers) throws Exception {
        if (depth > 10) {
            throw new Exception("Depth > 10: " + target);
        }
        String res = target;
        boolean found = false;
        for (String m_name : global_markers.get_custom_names()) {
            if (res.contains(m_name)) {
                res = res.replace(m_name, global_markers.get_custom(m_name));
                found = true;
            }
        }
        if (!found) {
            return res;
        }
        res = _parse_target_type_recursive(depth + 1, res, global_markers);
        return res;
    }

    private static String get_target_type_by_type_map(JaxbUtils.JaxbTypeMap type_map,
                                                      String detected) {

        String target_type_name = type_map.get_target_type_name(detected);
        return target_type_name;
    }

    private interface IMacro {
        String exec(FieldInfo fi);
    }

    private static Map<String, IMacro> _get_macros() {
        Map<String, IMacro> macros = new HashMap<String, IMacro>();
        macros.put("${lower_snake_case(column)}", new IMacro() {
            @Override
            public String exec(FieldInfo fi) {
                return Helpers.camel_case_to_lower_snake_case(fi.getColumnName());
            }
        });
        macros.put("${camelCase(column)}", new IMacro() {
            @Override
            public String exec(FieldInfo fi) {
                return Helpers.to_lower_camel_or_title_case(fi.getColumnName(), false);
            }
        });
        macros.put("${TitleCase(column)}", new IMacro() {
            @Override
            public String exec(FieldInfo fi) {
                return Helpers.to_lower_camel_or_title_case(fi.getColumnName(), true);
            }
        });
        macros.put("${kebab-case(column)}", new IMacro() {
            @Override
            public String exec(FieldInfo fi) {
                return Helpers.to_kebab_case(fi.getColumnName());
            }
        });
        macros.put("${column}", new IMacro() {
            @Override
            public String exec(FieldInfo fi) {
                return fi.getColumnName();
            }
        });
        macros.put("${sqlalchemy-params}", new IMacro() {
            @Override
            public String exec(FieldInfo fi) {
                String res = "";
                if (fi.isPK()) {
                    res += ", primary_key=True";
                }
                if (fi.isAI()) {
                    res += ", autoincrement=True";
                }
                if (fi.isNullable()) {
                    res += ", nullable=True";
                }
                return res;
            }
        });
        return macros;
    }

    private static void _substitute_built_in_macros(List<FieldInfo> fields) {
        Map<String, IMacro> macro = _get_macros();
        for (FieldInfo fi : fields) {
            String curr_type = fi.getType();
            if (curr_type.length() == 0) {
                continue;
            }
            for (String name : macro.keySet()) {
                if (!curr_type.contains(name)) {
                    continue;
                }
                String value = macro.get(name).exec(fi);
                curr_type = curr_type.replace(name, value);
            }
            fi.refine_rendered_type(curr_type);
        }
    }
}
