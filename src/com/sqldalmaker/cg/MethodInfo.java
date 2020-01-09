package com.sqldalmaker.cg;

import com.sqldalmaker.jaxb.dao.Query;
import com.sqldalmaker.jaxb.dao.QueryDto;
import com.sqldalmaker.jaxb.dao.QueryDtoList;
import com.sqldalmaker.jaxb.dao.QueryList;

public class MethodInfo {

    public final String method;
    public final String ref;
    public final boolean is_external_sql;
    public final boolean return_type_is_dto;
    public final String return_type;
    public final boolean fetch_list;

    public MethodInfo(Object jaxb_element) throws Exception {

        this.fetch_list = (jaxb_element instanceof QueryDtoList) || (jaxb_element instanceof QueryList);

        this.return_type_is_dto = (jaxb_element instanceof QueryDto) || (jaxb_element instanceof QueryDtoList);
        
        if (jaxb_element instanceof Query) {

            Query q = (Query) jaxb_element;
            method = q.getMethod();
            ref = q.getRef();
            is_external_sql = q.isExternalSql();
            return_type = q.getReturnType();

        } else if (jaxb_element instanceof QueryList) {

            QueryList q = (QueryList) jaxb_element;
            method = q.getMethod();
            ref = q.getRef();
            is_external_sql = q.isExternalSql();
            return_type = q.getReturnType();

        } else if (jaxb_element instanceof QueryDto) {

            QueryDto q = (QueryDto) jaxb_element;
            method = q.getMethod();
            ref = q.getRef();
            is_external_sql = q.isExternalSql();
            return_type = q.getDto();

        } else if (jaxb_element instanceof QueryDtoList) {

            QueryDtoList q = (QueryDtoList) jaxb_element;
            method = q.getMethod();
            ref = q.getRef();
            is_external_sql = q.isExternalSql();
            return_type = q.getDto();

        } else {

            String xml_node_name = Helpers.get_jaxb_node_name(jaxb_element);

            throw new Exception("Unexpected XML element: " + xml_node_name);
        }
    }
}
