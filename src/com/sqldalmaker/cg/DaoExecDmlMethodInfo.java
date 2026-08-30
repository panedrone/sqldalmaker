/*
    Copyright 2011-2024 sqldalmaker@gmail.com
    SQL DAL Maker Website: https://sqldalmaker.sourceforge.net/
    Read LICENSE.txt in the root of this project/archive for details.
 */
package com.sqldalmaker.cg;

/*
 * One "<exec-dml..." to render.
 * Data only. It is created and filled by 'JaxbUtils.plan_dao_methods'.
 */
public class DaoExecDmlMethodInfo implements DaoMethodInfo {

    public String error_context;
    public String xml_node_name; // rendered into the generated comment
    public String method_name;
    public String[] param_descriptors;
    public String ref;
    public boolean external_sql;

    @Override
    public String get_error_context() {
        return error_context;
    }
}
