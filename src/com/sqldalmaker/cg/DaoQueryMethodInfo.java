/*
    Copyright 2011-2026 sqldalmaker@gmail.com
    SQL DAL Maker Website: https://sqldalmaker.sourceforge.net/
    Read LICENSE.txt in the root of this project/archive for details.
 */
package com.sqldalmaker.cg;

/*
 * One "<query...", "<query-list...", "<query-dto..." or "<query-dto-list..." to render.
 * Data only. It is created and filled by 'JaxbUtils.plan_dao_methods'.
 */
public class DaoQueryMethodInfo implements DaoMethodInfo {

    public String error_context;
    public String method_name;
    public String[] param_descriptors;
    public String ref;
    public boolean external_sql;
    public boolean return_type_is_dto;
    public String dto_or_return_type;
    public boolean fetch_list;

    @Override
    public String get_error_context() {
        return error_context;
    }
}
