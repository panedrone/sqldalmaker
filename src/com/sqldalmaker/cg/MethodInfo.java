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

    public MethodInfo(Object element) throws Exception {

        fetch_list = (element instanceof QueryDtoList) || (element instanceof QueryList);

        boolean return_type_is_dto = false;

        if (element instanceof Query) {

            Query q = (Query) element;
            method = q.getMethod();
            ref = q.getRef();
            is_external_sql = q.is_external_sql();
            return_type = q.getReturnType();

        } else if (element instanceof QueryList) {

            QueryList q = (QueryList) element;
            method = q.getMethod();
            ref = q.getRef();
            is_external_sql = q.is_external_sql();
            return_type = q.getReturnType();

        } else if (element instanceof QueryDto) {

            QueryDto q = (QueryDto) element;
            method = q.getMethod();
            ref = q.getRef();
            is_external_sql = q.is_external_sql();
            return_type = q.getDto();
            return_type_is_dto = true;

        } else if (element instanceof QueryDtoList) {

            QueryDtoList q = (QueryDtoList) element;
            method = q.getMethod();
            ref = q.getRef();
            is_external_sql = q.is_external_sql();
            return_type = q.getDto();
            return_type_is_dto = true;

        } else {

            String xml_node_name = Helpers.get_xml_node_name(element);

            throw new Exception("Unexpected XML element: " + xml_node_name);
        }

        this.return_type_is_dto = return_type_is_dto;
    }
}
