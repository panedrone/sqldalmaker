/*
    Copyright 2011-2022 sqldalmaker@gmail.com
    SQL DAL Maker Website: https://sqldalmaker.sourceforge.net/
    Read LICENSE.txt in the root of this project/archive for details.
 */
package com.sqldalmaker.cg;

import com.sqldalmaker.jaxb.dto.DtoClass;
import com.sqldalmaker.jaxb.settings.Macros;

import java.io.StringWriter;
import java.sql.Connection;
import java.util.*;

/**
 * @author sqldalmaker@gmail.com
 */
class DtoClassInfo {

    private final Connection conn;

    private final JaxbTypeMap type_map;
    private final JaxbMacros macros;

    private final FieldNamesMode dto_field_names_mode;

    public DtoClassInfo(Connection conn,
                        JaxbTypeMap type_map,
                        JaxbMacros macros,
                        FieldNamesMode dto_field_names_mode) {

        this.conn = conn;
        this.type_map = type_map;
        this.macros = macros;

        this.dto_field_names_mode = dto_field_names_mode;
    }

    public Map<String, FieldInfo> get_field_info_for_wizard(DtoClass jaxb_dto_class,
                                                            String sql_root_abs_path,
                                                            List<FieldInfo> res_dto_fields) throws Exception {
        // boolean ignore_model = true;
        // it considers type-map internally
        Map<String, FieldInfo> fields_map = _prepare_by_jdbc(true, jaxb_dto_class, sql_root_abs_path, res_dto_fields);
        _refine_scalar_types_by_type_map(res_dto_fields);
        return fields_map;
    }

    public Map<String, FieldInfo> get_dto_field_info(boolean ignore_model,
                                                     DtoClass jaxb_dto_class,
                                                     String sql_root_abs_path,
                                                     List<FieldInfo> res_dto_fields) throws Exception {
        try {
            Map<String, FieldInfo> fields_map = _prepare_by_jdbc(ignore_model, jaxb_dto_class, sql_root_abs_path, res_dto_fields);
            refine_field_info(fields_map, jaxb_dto_class, res_dto_fields);
            return fields_map;
        } catch (Exception e) {
            throw new Exception(String.format("<dto-class name=\"%s\"... %s: %s",
                    jaxb_dto_class.getName(), e.getClass().getName(), e.getMessage()));
        }
    }

    public void refine_field_info(Map<String, FieldInfo> fields_map,
                                  DtoClass jaxb_dto_class,
                                  List<FieldInfo> res_dto_fields) throws Exception {

        _refine_by_field_jaxb(jaxb_dto_class, res_dto_fields, fields_map);
        _refine_scalar_types_by_type_map(res_dto_fields);
        _substitute_built_in_macros(res_dto_fields);
        _substitute_type_params(res_dto_fields);
    }

    private void _refine_scalar_types_by_type_map(List<FieldInfo> dto_fields) {
        for (FieldInfo fi : dto_fields) {
            String type_name = fi.getScalarType();
            type_name = type_map.get_target_type_name(type_name);
            fi.refine_scalar_type(type_name);
        }
    }

    private void _substitute_type_params(List<FieldInfo> _dto_fields) {
        for (FieldInfo fi : _dto_fields) {
            this.macros.substitute_type_params(fi);
        }
    }

    private Map<String, FieldInfo> _prepare_by_jdbc(boolean ignore_model,
                                                    DtoClass jaxb_dto_class,
                                                    String sql_root_abs_path,
                                                    List<FieldInfo> res_dto_fields) throws Exception {

        String dto_class_name = jaxb_dto_class.getName();
        String model = "";
        if (!ignore_model) {
            int model_name_end_index = dto_class_name.indexOf('-');
            if (model_name_end_index != -1) {
                model = dto_class_name.substring(0, model_name_end_index + 1);
            }
        }
        String jaxb_dto_class_ref = jaxb_dto_class.getRef();
        Map<String, FieldInfo> fields_map = _prepare_by_jdbc(model, jaxb_dto_class_ref,
                jaxb_dto_class.getAuto(), jaxb_dto_class.getPk(), sql_root_abs_path, res_dto_fields);
        return fields_map;
    }

    private Map<String, FieldInfo> _prepare_by_jdbc(String model,
                                                    String jaxb_dto_class_ref,
                                                    String jaxb_dto_class_auto,
                                                    String jaxb_dto_class_explicit_pk,
                                                    String sql_root_abs_path,
                                                    List<FieldInfo> res_dto_fields) throws Exception {

        Map<String, FieldInfo> fields_map = new HashMap<String, FieldInfo>();
        res_dto_fields.clear();
        if (!SqlUtils.is_empty_ref(jaxb_dto_class_ref)) {
            String jdbc_sql = SqlUtils.jdbc_sql_by_dto_class_ref(jaxb_dto_class_ref, sql_root_abs_path);
            // get fields from sql first to omit invisible/db-specific fields
            JdbcSqlFieldInfo.get_field_info_by_jdbc_sql(model, conn, dto_field_names_mode, jdbc_sql, jaxb_dto_class_explicit_pk,
                    fields_map, res_dto_fields);
            if (SqlUtils.is_table_ref(jaxb_dto_class_ref)) {
                String table_name = jaxb_dto_class_ref;
                _fill_by_table(jaxb_dto_class_auto, model, table_name, res_dto_fields, fields_map);
            } else if (SqlUtils.is_sql_shortcut_ref(jaxb_dto_class_ref)) {
                String[] parts = SqlUtils.parse_sql_shortcut_ref(jaxb_dto_class_ref);
                String table_name = parts[0];
                _fill_by_table(jaxb_dto_class_auto, model, table_name, res_dto_fields, fields_map);
            }
        }
        return fields_map;
    }

    private void _refine_by_field_jaxb(DtoClass jaxb_dto_class,
                                       List<FieldInfo> dto_fields,
                                       Map<String, FieldInfo> fields_map) throws Exception {

        Set<String> refined_by_field_jaxb = new HashSet<String>();
        Set<FieldInfo> excluded_fields = new HashSet<FieldInfo>();
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
                dto_fields.add(fi);
                fields_map.put(jaxb_field_col_name, fi);
            }
            if (jaxb_field.getType().trim().equals("-")) {
                excluded_fields.add(fi);
            }
            _refine_fi_by_type_map_and_macros(fi, jaxb_field_type_name);
            refined_by_field_jaxb.add(fi.getColumnName());
            fi.refine_scalar_type(jaxb_field_type_name);
        }
        for (FieldInfo fi : excluded_fields) {
            dto_fields.remove(fi);
        }
        for (FieldInfo fi : dto_fields) {
            if (refined_by_field_jaxb.contains(fi.getColumnName())) {
                continue;
            }
            String type_name = fi.getType().trim(); // !!! getType(), not getOriginalType()
            _refine_fi_by_type_map_and_macros(fi, type_name);
        }
    }

    private void _fill_by_table(String auto_column,
                                String model,
                                String table_name,
                                List<FieldInfo> res_dto_fields,
                                Map<String, FieldInfo> fields_map) throws Exception {

        String explicit_pk = "*";
        JdbcTableInfo info = new JdbcTableInfo(model, conn, type_map, dto_field_names_mode, table_name, explicit_pk, auto_column);
        res_dto_fields.clear();
        res_dto_fields.addAll(info.fields_all);
        fields_map.clear();
        fields_map.putAll(info.fields_map);
    }

    private void _refine_fi_by_type_map_and_macros(FieldInfo fi,
                                                   String type_name) throws Exception {
        type_name = type_name.trim();
        String target_type;
        int local_field_type_params_start = type_name.indexOf('|');
        if (local_field_type_params_start == -1) { // explicit_type_name only, no parameters "int64"
            String type_map_target_type = type_map.get_target_type_name(type_name);
            target_type = _parse_target_type(fi, type_map_target_type);
        } else {
            if (local_field_type_params_start == 0) { // "|0: hello0|1:hello1"
                String detected_jdbc_type_name = fi.getOriginalType(); // !!! original
                String type_map_target_type = type_map.get_target_type_name(detected_jdbc_type_name);
                target_type = _parse_target_type(fi, type_map_target_type + type_name);
            } else { // "{type}|0:Integer|1:hello1"
                String local_field_type_explicit = type_name.substring(0, local_field_type_params_start).trim();
                String parsed_target_type = _parse_target_type(fi, local_field_type_explicit);
                String local_field_type_params = type_name.substring(local_field_type_params_start);
                target_type = parsed_target_type + local_field_type_params;
            }
        }
        fi.refine_rendered_type(target_type);
    }

    private String _parse_target_type(FieldInfo fi,
                                      String target) throws Exception {

        return _parse_target_type_recursive(0, fi, target);
    }

    private String _parse_target_type_recursive(int depth,
                                                FieldInfo fi, // fi in here is used for vm templates
                                                String target_type_name) throws Exception {
        if (depth > 10) {
            throw new Exception("Depth > 10: " + target_type_name);
        }
        String res = target_type_name;
        boolean found = false;
        for (String m_name : macros.get_custom_names()) {
            if (res.contains(m_name)) { // single replacement in one pass
                res = res.replace(m_name, macros.get_custom(m_name));
                found = true;
            }
        }
        if (!found) {
            String tmp = _substitute_vm_macros(fi, res);
            if (tmp != null) {
                res = tmp; // single replacement in one pass
                found = true;
            }
        }
        if (!found) {
            return res;
        }
        res = _parse_target_type_recursive(depth + 1, fi, res);
        return res;
    }

    private String _substitute_vm_macros(FieldInfo fi, // fi in here is used for vm templates
                                         String target_type_name) throws Exception {

        for (String m_name : macros.get_vm_macro_names()) {
            if (!target_type_name.contains(m_name)) {
                continue;
            }
            String vm_template;
            Macros.Macro vm_macro = macros.get_vm_macro(m_name);
            if (vm_macro.getVm() != null) {
                vm_template = vm_macro.getVm().trim();
            } else if (vm_macro.getVmXml() != null) {
                vm_template = Xml2Vm.parse(vm_macro.getVmXml());
            } else {
                throw new Exception("Expected <vm> or <vm-xml> in " + m_name);
            }
            TemplateEngine te = new TemplateEngine(vm_template, m_name);
            Map<String, Object> values = new HashMap<String, Object>();
            values.put("fi", fi);
            StringWriter sw = new StringWriter();
            te.merge(values, sw);
            String value = sw.toString();
            target_type_name = target_type_name.replace(m_name, value);
            return target_type_name; // single replacement in one pass
        }
        return null;
    }

    private interface IMacro {
        String exec(FieldInfo fi);
    }

    private static Map<String, IMacro> _get_built_in_macros() {
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
        // deprecated, don't use:
        macros.put("${sqlalchemy-params}", new IMacro() {
            @Override
            public String exec(FieldInfo fi) {
                String res = "";
                if (fi.getFK() != null) {  // positional arguments must go 1st
                    res += String.format(", ForeignKey('%s')", fi.getFK());
                }
                if (fi.isPK()) {
                    res += ", primary_key=True";
                }
                if (fi.isAI()) {
                    res += ", autoincrement=True";
                }
                if (!fi.isPK()) {
                    if (fi.isIndexed()) {
                        res += ", index=True";
                    }
                    if (fi.isUnique()) {
                        res += ", unique=True";
                    }
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
        Map<String, IMacro> built_in_macros = _get_built_in_macros();
        for (FieldInfo fi : fields) {
            String curr_type = fi.getType();
            if (curr_type.length() == 0) {
                continue;
            }
            for (String name : built_in_macros.keySet()) {
                if (!curr_type.contains(name)) {
                    continue;
                }
                String value = built_in_macros.get(name).exec(fi);
                curr_type = curr_type.replace(name, value);
            }
            fi.refine_rendered_type(curr_type);
        }
    }
}
