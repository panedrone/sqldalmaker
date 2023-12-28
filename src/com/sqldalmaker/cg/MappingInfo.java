/*
    Copyright 2011-2023 sqldalmaker@gmail.com
    SQL DAL Maker Website: https://sqldalmaker.sourceforge.net/
    Read LICENSE.txt in the root of this project/archive for details.
 */
package com.sqldalmaker.cg;

import java.util.ArrayList;
import java.util.List;

/*
 * 16.11.2022 08:02 1.269
 * 16.04.2022 17:35 1.219
 * 08.05.2021 22:29 1.200
 *
 */
public class MappingInfo {

    public final List<FieldInfo> fields = new ArrayList<FieldInfo>();
    public String method_param_name;
    public String exec_dml_param_name;
    public String dto_class_name;

    public List<FieldInfo> get_fields() {
        return fields;
    }

    public String get_method_param_name() {
        return method_param_name;
    }

    public String get_exec_dml_param_name() {
        return exec_dml_param_name;
    }

    public String get_dto_class_name() {
        return dto_class_name;
    }
}
