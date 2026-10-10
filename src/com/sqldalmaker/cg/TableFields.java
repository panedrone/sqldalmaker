/*
    Copyright 2011-2026 sqldalmaker@gmail.com
    SQL DAL Maker Website: https://sqldalmaker.sourceforge.net/
    Read LICENSE.txt in the root of this project/archive for details.
 */
package com.sqldalmaker.cg;

import org.apache.cayenne.dba.TypesMapping;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/*
 * 10.10.2026 1.333 Claude: interpretation part of JdbcTableInfo
 *
 * Columns of a table as FieldInfo-s, all/PK/not PK. Immutable.
 * 'create' is pure: IDbMetadata.Table + settings --> FieldInfo-s.
 */
class TableFields {

    final List<FieldInfo> fields_all;
    final List<FieldInfo> fields_pk;
    final List<FieldInfo> fields_not_pk;

    TableFields(List<FieldInfo> fields_all, List<FieldInfo> fields_pk, List<FieldInfo> fields_not_pk) {
        this.fields_all = Collections.unmodifiableList(fields_all);
        this.fields_pk = Collections.unmodifiableList(fields_pk);
        this.fields_not_pk = Collections.unmodifiableList(fields_not_pk);
    }

    // 'explicit_pk' == "*" means "PK of the table"
    static TableFields read(
            IDbMetadata md,
            JaxbTypeMap type_map,
            FieldNamesMode field_names_mode,
            String model,
            String table_ref,
            String explicit_pk,
            String jaxb_explicit_auto_column) throws Exception {

        IDbMetadata.Table table = md.read_table(table_ref);
        Set<String> lower_case_pk_col_names;
        if ("*".equals(explicit_pk)) {
            lower_case_pk_col_names = new HashSet<>();
            for (String pk_col_name : md.read_pk_col_names(table_ref)) {
                // 2 'id' happened with MySQL!
                lower_case_pk_col_names.add(Names.get_pk_col_name_alias(pk_col_name));
            }
        } else {
            lower_case_pk_col_names = JaxbUtils.get_pk_col_name_aliases_from_jaxb(explicit_pk);
        }
        return create(table, lower_case_pk_col_names, type_map, field_names_mode, model, jaxb_explicit_auto_column);
    }

    static TableFields create(
            IDbMetadata.Table table,
            Set<String> lower_case_pk_col_names,
            JaxbTypeMap type_map,
            FieldNamesMode field_names_mode,
            String model,
            String jaxb_explicit_auto_column) throws Exception {

        if (model == null) {
            model = "";
        }
        String[] auto = _parse_auto_column(jaxb_explicit_auto_column);
        String auto_column_name = auto[0];
        String auto_column_generation_type = auto[1];
        // the columns are unique in a table, 'select *' returns each of them once
        Map<String, FieldInfo> by_col_name = new HashMap<>();
        for (FieldInfo fi : ColumnFields.create(table.columns, model, field_names_mode, "")) {
            by_col_name.put(fi.getColumnName(), fi);
        }
        for (IDbMetadata.TableColumn tc : table.table_columns) {
            FieldInfo fi = by_col_name.get(tc.name);
            if (fi != null) {
                by_col_name.put(tc.name, _refine_by_table_column(fi, tc, model));
            }
        }
        List<FieldInfo> all = new ArrayList<>();
        List<FieldInfo> pk = new ArrayList<>();
        List<FieldInfo> not_pk = new ArrayList<>();
        for (IDbMetadata.Column c : table.columns) {
            FieldInfo fi = by_col_name.get(c.name);
            String detected_type_name;
            if (c.name.equalsIgnoreCase(auto_column_name) && !model.isEmpty()) {
                detected_type_name = fi.getType() + "+" + auto_column_generation_type;
            } else {
                detected_type_name = fi.getType();
            }
            fi = FieldInfo.with_rendered_type(fi, type_map.get_target_type_name(detected_type_name));
            boolean is_pk = lower_case_pk_col_names.contains(Names.get_pk_col_name_alias(c.name));
            fi = FieldInfo.with_pk(fi, is_pk);
            all.add(fi);
            if (is_pk) {
                pk.add(fi);
            } else {
                not_pk.add(fi);
            }
        }
        return new TableFields(all, pk, not_pk);
    }

    // consider auto like "o_id:identity"; returns {column name, generation type}
    private static String[] _parse_auto_column(String jaxb_explicit_auto_column) {
        if (jaxb_explicit_auto_column == null) {
            jaxb_explicit_auto_column = "";
        } else {
            jaxb_explicit_auto_column = jaxb_explicit_auto_column.trim();
        }
        if (jaxb_explicit_auto_column.isEmpty()) {
            return new String[]{"", ""};
        }
        String[] auto_parts = jaxb_explicit_auto_column.split(":");
        if (auto_parts.length < 2) {
            // if it is default "*", then just nothing will happen
            return new String[]{jaxb_explicit_auto_column, "auto"};
        }
        return new String[]{auto_parts[0], auto_parts[1]};
    }

    private static FieldInfo _refine_by_table_column(FieldInfo fi, IDbMetadata.TableColumn tc, String model) {
        fi = FieldInfo.with_comment(fi, "t");
        fi = _refine_jdbc_type(fi, tc, model);
        fi = _refine_col_size(fi, tc);
        if (tc.decimal_digits != null) {
            // it is always -127 for Oracle NUMERIC-s and always 10 for all SQLite types
            // https://stackoverflow.com/questions/12931061/how-to-get-oracle-number-with-syncdb
            fi = FieldInfo.with_decimal_digits(fi, tc.decimal_digits);
        }
        // IS_AUTOINCREMENT may work incorrectly (sqlite-jdbc-3.19.3.jar).
        // So, use it only if AI was not set by SQL columns metadata
        if (!fi.isAI() && Boolean.TRUE.equals(tc.is_ai)) {
            fi = FieldInfo.with_ai(fi, true);
        }
        if (tc.is_nullable != null) {
            fi = FieldInfo.with_nullable(fi, tc.is_nullable);
        }
        if (tc.fk != null) {
            fi = FieldInfo.with_fk(fi, tc.fk);
        }
        if (tc.is_indexed) {
            fi = FieldInfo.with_indexed(fi, true);
        }
        if (tc.is_unique) {
            fi = FieldInfo.with_unique(fi, true);
        }
        return fi;
    }

    private static class Oracle {
        // Field descriptor #7 I
        // public static final int TIMESTAMP = 93; // === panedrone: 93 is known in "TypesMapping.getJavaBySqlType"

        // Field descriptor #7 I (deprecated)
        static final int TIMESTAMPNS = -100;

        // Field descriptor #7 I
        static final int TIMESTAMPTZ = -101;

        // Field descriptor #7 I
        static final int TIMESTAMPLTZ = -102;
    }

    private static FieldInfo _refine_jdbc_type(FieldInfo fi, IDbMetadata.TableColumn tc, String model) {
        String no_type = model + Object.class.getName();
        // don't re-define sql_type to avoid type conversions at run-time
        if (!no_type.equals(fi.getOriginalType()) || tc.data_type == null) {
            return fi;
        }
        int type = tc.data_type;
        String apache_java_type_name = TypesMapping.getJavaBySqlType(type);
        if (Object.class.getName().equals(apache_java_type_name)) {
            switch (type) {
                // case Oracle.TIMESTAMP:
                case Oracle.TIMESTAMPNS:
                    apache_java_type_name = java.util.Date.class.getName();
                    break;
                case Oracle.TIMESTAMPTZ:
                    apache_java_type_name = java.time.ZonedDateTime.class.getName();
                    break;
                case Oracle.TIMESTAMPLTZ:
                    apache_java_type_name = java.time.LocalDateTime.class.getName();
                    break;
            }
        }
        fi = FieldInfo.with_scalar_type(fi, apache_java_type_name);
        return FieldInfo.with_rendered_type(fi, model + apache_java_type_name);
    }

    private static FieldInfo _refine_col_size(FieldInfo fi, IDbMetadata.TableColumn tc) {
        if (!String.class.getName().equals(fi.getScalarType()) || tc.column_size == null) {
            return fi;
        }
        final int max_size = 0x3FFF;  // 16K
        int size = tc.column_size;
        if (size > max_size) { // sqlite3 2000000000
            size = 0;
        }
        int precision = fi.getPrecision();
        if (precision > 0 && precision < max_size) {
            // How to get the size of a column of a table using JDBC?
            // https://www.tutorialspoint.com/how-to-get-the-size-of-a-column-of-a-table-using-jdbc
            if (size == 0 || precision < size) {
                size = precision;
            }
        }
        return FieldInfo.with_column_size(fi, size);
    }
}
