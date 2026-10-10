/*
    Copyright 2011-2026 sqldalmaker@gmail.com
    SQL DAL Maker Website: https://sqldalmaker.sourceforge.net/
    Read LICENSE.txt in the root of this project/archive for details.
 */
package com.sqldalmaker.cg;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/*
 * 10.10.2026 1.333 Claude: interpretation part of JdbcSqlFieldInfo
 *
 * Columns of a result set --> FieldInfo-s. No JDBC in here.
 */
class ColumnFields {

    static List<FieldInfo> create(
            List<IDbMetadata.Column> columns,
            String model,
            FieldNamesMode field_names_mode,
            String jaxb_explicit_pk) throws Exception {

        Set<String> lower_case_pk_col_names = null;
        if (jaxb_explicit_pk != null && !jaxb_explicit_pk.trim().isEmpty() && !"*".equals(jaxb_explicit_pk)) {
            lower_case_pk_col_names = JaxbUtils.get_pk_col_name_aliases_from_jaxb(jaxb_explicit_pk);
        }
        List<FieldInfo> res = new ArrayList<>();
        for (IDbMetadata.Column c : columns) {
            //FieldInfo field = new FieldInfo(dto_field_names_mode, type_name, col_name, "q(" + col_name + ")");
            FieldInfo fi = new FieldInfo(field_names_mode, model + c.java_type, c.name, "q");
            fi = FieldInfo.with_ai(fi, c.is_ai);
            fi = FieldInfo.with_decimal_digits(fi, c.scale);
            fi = FieldInfo.with_rs_md(fi, c.precision, c.scale, c.display_size);
            if (lower_case_pk_col_names != null
                    && lower_case_pk_col_names.contains(Names.get_pk_col_name_alias(c.name))) {
                fi = FieldInfo.with_pk(fi, true);
            }
            res.add(fi);
        }
        return Collections.unmodifiableList(res);
    }

    static List<String> column_names(List<FieldInfo> fields) {
        List<String> res = new ArrayList<>();
        for (FieldInfo fi : fields) {
            res.add(fi.getColumnName());
        }
        return res;
    }
}
