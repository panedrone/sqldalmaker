/*
    Copyright 2011-2024 sqldalmaker@gmail.com
    SQL DAL Maker Website: https://sqldalmaker.sourceforge.net/
    Read LICENSE.txt in the root of this project/archive for details.
 */
package com.sqldalmaker.cg;

/*
 * One method of a "<crud..." to render. An empty "<crud/>" yields all the five
 * of them, with the default method names already resolved.
 * Data only. It is created and filled by 'JaxbUtils.plan_dao_methods'.
 */
public class DaoCrudMethodInfo implements DaoMethodInfo {

    public enum Kind {
        CREATE, READ, UPDATE, DELETE
    }

    public String error_context;
    public Kind kind;
    public String method_name;
    public String dto_class_name;
    public String table_name;
    public String explicit_pk; // null for "read all"
    public String auto_column;
    public boolean fetch_generated;
    public boolean fetch_list; // READ: "read all" or "read one"

    @Override
    public String get_error_context() {
        return error_context;
    }
}
