/*
    Copyright 2011-2026 sqldalmaker@gmail.com
    SQL DAL Maker Website: https://sqldalmaker.sourceforge.net/
    Read LICENSE.txt in the root of this project/archive for details.
 */
package com.sqldalmaker.cg;

import java.util.ArrayList;
import java.util.List;

/*
 * 10.10.2026 1.333 Claude
 *
 * A parameter of a generated method, or a value bound to an SQL parameter: a name and a type.
 * Unlike FieldInfo, it is not a column: no PK, FK, sizes and so on.
 * A column that becomes a parameter (PK in CRUD, DTO properties in INSERT/UPDATE) is turned
 * into ParamInfo by 'of', keeping its rendered name.
 *
 * Immutable: getters only, 'with_type' makes a new one.
 */
public class ParamInfo {

    private final String rendered_name;
    private final String lower_camel_case;
    private final String rendered_type;
    private final String name_prefix;

    // The DAO renderers of implicit cursors put a whole expression where a parameter name
    // is expected: an array of row handlers in Java, a slice of callbacks in Go. Such a
    // "name" has to reach the template untouched.
    // TODO: those callers should pass the expression as such, not disguised as a name.
    private static boolean _is_rendered_expression(String name) {
        return name.contains("func(") || name.contains("new RowHandler[]");
    }

    public ParamInfo(FieldNamesMode names_mode, String type_name, String param_name) {
        if (param_name == null) {
            param_name = "";
        }
        String rendered_name = param_name;
        String lower_camel_case = null;
        String name_prefix = "";
        if (!rendered_name.isEmpty() && !_is_rendered_expression(rendered_name)) {
            rendered_name = rendered_name.replace("-", "_");
            rendered_name = rendered_name.replace(".", "_");
            rendered_name = rendered_name.replace(":", "_");
            lower_camel_case = Names.lower_camel_case(rendered_name);
            // don't apply to callback-params
            if (FieldNamesMode.LOWER_CAMEL_CASE.equals(names_mode)) {
                rendered_name = Names.lower_camel_case(rendered_name);
            } else if (FieldNamesMode.TITLE_CASE.equals(names_mode)) {
                rendered_name = Names.title_case(rendered_name);
            } else if (FieldNamesMode.SNAKE_CASE.equals(names_mode)) {
                rendered_name = Names.camel_case_to_lower_snake_case(rendered_name);
                name_prefix = "_";
            }
        }
        this.rendered_name = rendered_name;
        this.lower_camel_case = lower_camel_case;
        this.rendered_type = type_name;
        this.name_prefix = name_prefix;
    }

    private ParamInfo(String rendered_name, String lower_camel_case, String rendered_type, String name_prefix) {
        this.rendered_name = rendered_name;
        this.lower_camel_case = lower_camel_case;
        this.rendered_type = rendered_type;
        this.name_prefix = name_prefix;
    }

    // a column as a parameter: the same name and type
    public static ParamInfo of(FieldInfo fi) {
        return new ParamInfo(fi.getName(), fi.getLowerCamelCaseName(), fi.getType(), fi.getNamePrefix());
    }

    public static List<ParamInfo> of(List<FieldInfo> fields) {
        List<ParamInfo> res = new ArrayList<>();
        for (FieldInfo fi : fields) {
            res.add(of(fi));
        }
        return res;
    }

    // "type->assign_func" of type-map: a parameter needs the type only
    public static ParamInfo with_type(ParamInfo src, String type) {
        String rendered_type = JaxbTypeMap.split_target_type(type)[0];
        return new ParamInfo(src.rendered_name, src.lower_camel_case, rendered_type, src.name_prefix);
    }

    public String getName() { // this method is for use in VM templates
        return rendered_name;
    }

    public String getLowerCamelCaseName() { // for Golang VM template
        return lower_camel_case;
    }

    public String getType() { // this method is for use in VM templates
        return rendered_type;
    }

    // this method is for use in VM templates ONLY: the getter of a DTO property
    public String getterMethod() {
        String s = name_prefix + rendered_name;
        String X = Names.replace_char_at(s, 0, Character.toUpperCase(s.charAt(0)));
        return "get" + X;
    }
}
