/*
 * Copyright 2011-2018 sqldalmaker@gmail.com
 * SQL DAL Maker Website: http://sqldalmaker.sourceforge.net
 * Read LICENSE.txt in the root of this project/archive for details.
 */
package com.sqldalmaker.cg;

import com.sqldalmaker.jaxb.dao.DaoClass;
import com.sqldalmaker.jaxb.dao.ExecDml;

/**
 *
 * @author sqldalmaker@gmail.com
 *
 */
public interface IDaoCG {

    String[] translate(String dao_class_name, DaoClass dao_class) throws Exception;

    DbUtils get_db_utils();

    StringBuilder render_element_query(Object element) throws Exception;
    
    StringBuilder render_element_exec_dml(ExecDml element) throws Exception;
    
    StringBuilder render_element_crud(Object element) throws Exception;
    
    StringBuilder render_element_crud_insert(StringBuilder sql_buff, String class_name, String method_name,
                                             String table_name, String dto_class_name, boolean fetch_generated, String generated) throws Exception;

    StringBuilder render_element_crud_read(StringBuilder sql_buff, String method_name, String table_name,
                                           String ret_dto_type, boolean fetch_list) throws Exception;

    StringBuilder render_element_crud_update(StringBuilder sql_buff, String class_name, String method_name,
                                             String table_name, String dto_class_name, boolean primitive_params) throws Exception;

    StringBuilder render_element_crud_delete(StringBuilder sql_buff, String class_name, String method_name,
                                             String table_name, String dto_class_name) throws Exception;
}
