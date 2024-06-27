/*
    Copyright 2011-2024 sqldalmaker@gmail.com
    SQL DAL Maker Website: https://sqldalmaker.sourceforge.net/
    Read LICENSE.txt in the root of this project/archive for details.
 */
package com.sqldalmaker.cg;

import com.sqldalmaker.jaxb.sdm.DtoClass;
import com.sqldalmaker.jaxb.settings.Macros;

import java.io.StringWriter;
import java.sql.Connection;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

/*
 * @author sqldalmaker@gmail.com
 *
 * 30.05.2024 20:00 1.299
 * 06.04.2023 02:56 1.281
 * 27.03.2023 10:03
 * 15.02.2023 14:11
 * 19.01.2023 20:57 1.276
 * 30.12.2022 23:48 1.273
 * 16.11.2022 08:02 1.269
 * 14.08.2022 12:46
 * 19.05.2022 09:45 1.241
 * 06.05.2022 22:05 1.236
 * 29.04.2022 18:39 1.231
 * 16.04.2022 17:35 1.219
 *
 */
class DtoClassInfo {

    private final Connection conn;

    private final JaxbTypeMap jaxb_type_map;
    private final JaxbMacros jaxb_macros;

    private final FieldNamesMode dto_field_names_mode;

    public DtoClassInfo(
            Connection conn,
            JaxbTypeMap jaxb_type_map,
            JaxbMacros macros,
            FieldNamesMode dto_field_names_mode) {

        this.conn = conn;
        this.jaxb_type_map = jaxb_type_map;
        this.jaxb_macros = macros;
        this.dto_field_names_mode = dto_field_names_mode;
    }

    public List<FieldInfo> get_field_info_for_wizard(DtoClass jaxb_dto_class, String sql_root_abs_path) throws Exception {
        List<FieldInfo> res_dto_fields = new ArrayList<FieldInfo>();
        _prepare_by_jdbc(false, jaxb_dto_class, sql_root_abs_path, res_dto_fields);
        for (FieldInfo fi : res_dto_fields) {
            String type_name = fi.getType();
            type_name = jaxb_type_map.get_target_type_name(type_name);
            fi.refine_rendered_type(type_name); // field wizard wants rendered types
        }
        return res_dto_fields;
    }

    public Map<String, FieldInfo> get_dto_field_info(
            boolean ignore_model,
            DtoClass jaxb_dto_class,
            String sql_root_abs_path,
            List<FieldInfo> res_dto_fields) throws Exception {

        try {
            Map<String, FieldInfo> fields_map = _prepare_by_jdbc(ignore_model, jaxb_dto_class, sql_root_abs_path, res_dto_fields);
            _refine_field_info(fields_map, jaxb_dto_class, res_dto_fields);
            return fields_map;
        } catch (Exception e) {
            throw new Exception(String.format("<dto-class name=\"%s\"... %s: %s",
                    jaxb_dto_class.getName(), e.getClass().getName(), e.getMessage()));
        }
    }

    private void _refine_field_info(
            Map<String, FieldInfo> fields_map,
            DtoClass jaxb_dto_class,
            List<FieldInfo> res_dto_fields) throws Exception {

        _refine_by_field_jaxb(jaxb_dto_class, res_dto_fields, fields_map);
        _refine_scalar_types_by_type_map(res_dto_fields);
    }

    private void _refine_scalar_types_by_type_map(List<FieldInfo> dto_fields) {
        for (FieldInfo fi : dto_fields) {
            String type_name = fi.getScalarType();
            type_name = jaxb_type_map.get_target_type_name(type_name);
            fi.refine_scalar_type(type_name);
        }
    }

    private Map<String, FieldInfo> _prepare_by_jdbc(
            boolean ignore_model,
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

    private Map<String, FieldInfo> _prepare_by_jdbc(
            String model,
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
                SqlUtils.SqlShortcut shc = SqlUtils.parse_sql_shortcut_ref(jaxb_dto_class_ref);
                _fill_by_table(jaxb_dto_class_auto, model, shc.table_name, res_dto_fields, fields_map);
            }
        }
        return fields_map;
    }

    private void _refine_by_field_jaxb(
            DtoClass jaxb_dto_class,
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
            String col_nm = fi.getColumnName();
            fields_map.remove(col_nm);
        }
        for (FieldInfo fi : dto_fields) {
            if (refined_by_field_jaxb.contains(fi.getColumnName())) {
                continue;
            }
            String type_name = fi.getType().trim(); // !!! getType(), not getOriginalType()
            _refine_fi_by_type_map_and_macros(fi, type_name);
        }
    }

    private void _fill_by_table(
            String auto_column,
            String model,
            String table_name,
            List<FieldInfo> res_dto_fields,
            Map<String, FieldInfo> fields_map) throws Exception {

        res_dto_fields.clear();
        String explicit_pk = "*";
        JdbcTableInfo table_info = new JdbcTableInfo(model, conn, jaxb_type_map, dto_field_names_mode, table_name, explicit_pk, auto_column);
        for (FieldInfo fi : table_info.fields_all) {
            String col_nm = fi.getColumnName();
            if (fields_map.containsKey(col_nm)) {
                res_dto_fields.add(fi);
                fields_map.replace(col_nm, fi);
            } else {
                fields_map.remove(col_nm);
            }
        }
    }

    private void _refine_fi_by_type_map_and_macros(FieldInfo fi, String type_name) throws Exception {
        type_name = type_name.trim();
        String target_type;
        int local_field_type_params_start = type_name.indexOf('|');
        if (local_field_type_params_start == -1) { // explicit_type_name only, no parameters "int64"
            String type_map_target_type = jaxb_type_map.get_target_type_name(type_name);
            target_type = _parse_target_type(fi, type_map_target_type);
        } else {
            if (local_field_type_params_start == 0) { // "|0: hello0|1:hello1"
                String detected_jdbc_type_name = fi.getOriginalType(); // !!! original
                String type_map_target_type = jaxb_type_map.get_target_type_name(detected_jdbc_type_name);
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

    private String _parse_target_type(FieldInfo fi, String target) throws Exception {
        return jaxb_macros.process_fi(fi, target);
    }
}
