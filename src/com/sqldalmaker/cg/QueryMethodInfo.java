/*
 * Copyright 2011-2020 sqldalmaker@gmail.com
 * SQL DAL Maker Website: http://sqldalmaker.sourceforge.net
 * Read LICENSE.txt in the root of this project/archive for details.
 *
 */
package com.sqldalmaker.cg;

import com.sqldalmaker.jaxb.dao.Query;
import com.sqldalmaker.jaxb.dao.QueryDto;
import com.sqldalmaker.jaxb.dao.QueryDtoList;
import com.sqldalmaker.jaxb.dao.QueryList;

/**
 *
 * @author sqldalmaker@gmail.com
 *
 */
public class QueryMethodInfo {

    public final String jaxb_method;
    public final String jaxb_ref;
    public final boolean jaxb_is_external_sql;
    public final boolean return_type_is_dto;
    public final String jaxb_dto_or_return_type;
    public final boolean fetch_list;

    public QueryMethodInfo(Object jaxb_query) throws Exception {

        fetch_list = (jaxb_query instanceof QueryDtoList) || (jaxb_query instanceof QueryList);

        return_type_is_dto = (jaxb_query instanceof QueryDto) || (jaxb_query instanceof QueryDtoList);
        
        if (jaxb_query instanceof Query) {

            Query q = (Query) jaxb_query;
            jaxb_method = q.getMethod();
            jaxb_ref = q.getRef();
            jaxb_is_external_sql = q.isExternalSql();
            jaxb_dto_or_return_type = q.getReturnType();

        } else if (jaxb_query instanceof QueryList) {

            QueryList q = (QueryList) jaxb_query;
            jaxb_method = q.getMethod();
            jaxb_ref = q.getRef();
            jaxb_is_external_sql = q.isExternalSql();
            jaxb_dto_or_return_type = q.getReturnType();

        } else if (jaxb_query instanceof QueryDto) {

            QueryDto q = (QueryDto) jaxb_query;
            jaxb_method = q.getMethod();
            jaxb_ref = q.getRef();
            jaxb_is_external_sql = q.isExternalSql();
            jaxb_dto_or_return_type = q.getDto();

        } else if (jaxb_query instanceof QueryDtoList) {

            QueryDtoList q = (QueryDtoList) jaxb_query;
            jaxb_method = q.getMethod();
            jaxb_ref = q.getRef();
            jaxb_is_external_sql = q.isExternalSql();
            jaxb_dto_or_return_type = q.getDto();

        } else {

            String jaxb_node_name = JaxbUtils.get_jaxb_node_name(jaxb_query);

            throw new Exception("Unexpected JAXB node: " + jaxb_node_name);
        }
    }
}
