/*
    Copyright 2011-2026 sqldalmaker@gmail.com
    SQL DAL Maker Website: https://sqldalmaker.sourceforge.net/
    Read LICENSE.txt in the root of this project/archive for details.
 */
package com.sqldalmaker.cg;

import com.sqldalmaker.jaxb.sdm.DtoClass;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/*
 * @author sqldalmaker@gmail.com
 *
 * 10.10.2026 1.333 Claude: reads through IDbMetadata, returns values
 * 30.08.2026 14:00 1.331 Claude refactor
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
 */
class DtoClassInfo {

    private final IDbMetadata md;

    private final JaxbTypeMap jaxb_type_map;
    private final JaxbMacros jaxb_macros;

    private final FieldNamesMode dto_field_names_mode;

    DtoClassInfo(
            IDbMetadata md,
            JaxbTypeMap jaxb_type_map,
            JaxbMacros macros,
            FieldNamesMode dto_field_names_mode) {

        this.md = md;
        this.jaxb_type_map = jaxb_type_map;
        this.jaxb_macros = macros;
        this.dto_field_names_mode = dto_field_names_mode;
    }

    List<FieldInfo> get_field_info_for_wizard(DtoClass jaxb_dto_class, String sql_root_abs_path) throws Exception {
        List<FieldInfo> res = new ArrayList<>();
        for (FieldInfo fi : _get_fields_by_ref(false, jaxb_dto_class, sql_root_abs_path)) {
            String type_name = jaxb_type_map.get_target_type_name(fi.getType());
            res.add(FieldInfo.with_rendered_type(fi, type_name)); // field wizard wants rendered types
        }
        return Collections.unmodifiableList(res);
    }

    List<FieldInfo> get_dto_fields(
            boolean ignore_model,
            DtoClass jaxb_dto_class,
            String sql_root_abs_path) throws Exception {

        try {
            List<FieldInfo> fields = _get_fields_by_ref(ignore_model, jaxb_dto_class, sql_root_abs_path);
            fields = _refine_by_field_jaxb(jaxb_dto_class, fields);
            fields = _refine_scalar_types_by_type_map(fields);
            return Collections.unmodifiableList(fields);
        } catch (Exception e) {
            throw new Exception(String.format("<dto-class name=\"%s\"... %s: %s",
                    jaxb_dto_class.getName(), e.getClass().getName(), e.getMessage()));
        }
    }

    // "column name --> field"; the last one wins if a SQL returns the same column twice
    static Map<String, FieldInfo> by_column_name(List<FieldInfo> fields) {
        Map<String, FieldInfo> res = new HashMap<>();
        for (FieldInfo fi : fields) {
            res.put(fi.getColumnName(), fi);
        }
        return res;
    }

    private List<FieldInfo> _refine_scalar_types_by_type_map(List<FieldInfo> dto_fields) {
        List<FieldInfo> res = new ArrayList<>();
        for (FieldInfo fi : dto_fields) {
            String type_name = jaxb_type_map.get_target_type_name(fi.getScalarType());
            res.add(FieldInfo.with_scalar_type(fi, type_name));
        }
        return res;
    }

    private List<FieldInfo> _get_fields_by_ref(
            boolean ignore_model,
            DtoClass jaxb_dto_class,
            String sql_root_abs_path) throws Exception {

        String model = "";
        if (!ignore_model) {
            String dto_class_name = jaxb_dto_class.getName();
            int model_name_end_index = dto_class_name.indexOf('-');
            if (model_name_end_index != -1) {
                model = dto_class_name.substring(0, model_name_end_index + 1);
            }
        }
        String jaxb_dto_class_ref = jaxb_dto_class.getRef();
        if (SqlUtils.is_empty_ref(jaxb_dto_class_ref)) {
            return new ArrayList<>();
        }
        String jdbc_sql = SqlUtils.jdbc_sql_by_dto_class_ref(jaxb_dto_class_ref, sql_root_abs_path);
        // get fields from sql first to omit invisible/db-specific fields
        List<FieldInfo> sql_fields = ColumnFields.create(md.read_sql_columns(jdbc_sql), model,
                dto_field_names_mode, jaxb_dto_class.getPk());
        String table_name;
        if (SqlUtils.is_table_ref(jaxb_dto_class_ref)) {
            table_name = jaxb_dto_class_ref;
        } else if (SqlUtils.is_sql_shortcut_ref(jaxb_dto_class_ref)) {
            table_name = SqlUtils.parse_sql_shortcut_ref(jaxb_dto_class_ref).table_name;
        } else {
            return new ArrayList<>(sql_fields);
        }
        // the columns of the table that the SQL returns, but with the table metadata
        Set<String> sql_col_names = new HashSet<>(ColumnFields.column_names(sql_fields));
        TableFields table = TableFields.read(md, jaxb_type_map, dto_field_names_mode, model, table_name,
                "*", jaxb_dto_class.getAuto());
        List<FieldInfo> res = new ArrayList<>();
        for (FieldInfo fi : table.fields_all) {
            if (sql_col_names.contains(fi.getColumnName())) {
                res.add(fi);
            }
        }
        return res;
    }

    private List<FieldInfo> _refine_by_field_jaxb(DtoClass jaxb_dto_class, List<FieldInfo> dto_fields) throws Exception {
        List<FieldInfo> res = new ArrayList<>(dto_fields);
        // a column is refined where it occurs the last time (if a SQL returns it twice)
        Map<String, Integer> index_by_col_name = new HashMap<>();
        for (int i = 0; i < res.size(); i++) {
            index_by_col_name.put(res.get(i).getColumnName(), i);
        }
        Set<String> refined_by_field_jaxb = new HashSet<>();
        Set<Integer> excluded = new HashSet<>();
        List<DtoClass.Field> jaxb_fields = jaxb_dto_class.getField(); // not null!!
        for (DtoClass.Field jaxb_field : jaxb_fields) {
            String jaxb_field_col_name = jaxb_field.getColumn();
            String jaxb_field_type_name = jaxb_field.getType();
            Integer i = index_by_col_name.get(jaxb_field_col_name);
            if (i == null) { // add the field declared in XML, but missing in SQL
                res.add(new FieldInfo(dto_field_names_mode, jaxb_field_type_name, jaxb_field_col_name,
                        "xml(" + jaxb_field_col_name + ")"));
                i = res.size() - 1;
                index_by_col_name.put(jaxb_field_col_name, i);
            }
            FieldInfo fi = res.get(i);
            if (jaxb_field_type_name.trim().equals("-")) {
                excluded.add(i);
            }
            fi = _refine_fi_by_type_map_and_macros(fi, jaxb_field_type_name);
            res.set(i, FieldInfo.with_scalar_type(fi, jaxb_field_type_name));
            refined_by_field_jaxb.add(jaxb_field_col_name);
        }
        List<FieldInfo> refined = new ArrayList<>();
        for (int i = 0; i < res.size(); i++) {
            if (excluded.contains(i)) {
                continue;
            }
            FieldInfo fi = res.get(i);
            if (!refined_by_field_jaxb.contains(fi.getColumnName())) {
                String type_name = fi.getType().trim(); // !!! getType(), not getOriginalType()
                fi = _refine_fi_by_type_map_and_macros(fi, type_name);
            }
            refined.add(fi);
        }
        return refined;
    }

    private FieldInfo _refine_fi_by_type_map_and_macros(FieldInfo fi, String type_name) throws Exception {
        type_name = type_name.trim();
        String target_type_name;
        int local_field_type_params_start = type_name.indexOf('|');
        if (local_field_type_params_start == -1) { // explicit_type_name only, no parameters "int64"
            String type_map_target_type = jaxb_type_map.get_target_type_name(type_name);
            target_type_name = _parse_target_type(fi, type_map_target_type);
        } else {
            if (local_field_type_params_start == 0) { // "|0: hello0|1:hello1"
                String detected_jdbc_type_name = fi.getOriginalType(); // !!! original
                String type_map_target_type = jaxb_type_map.get_target_type_name(detected_jdbc_type_name);
                target_type_name = _parse_target_type(fi, type_map_target_type + type_name);
            } else { // "{type}|0:Integer|1:hello1"
                target_type_name = _parse_target_type(fi, type_name);
            }
        }
        return FieldInfo.with_rendered_type(fi, target_type_name);
    }

    private String _parse_target_type(FieldInfo fi, String target_type_name) throws Exception {
        return jaxb_macros.parse_target_type_name(target_type_name, fi);
    }
}
