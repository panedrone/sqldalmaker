/*
    Copyright 2011-2021 sqldalmaker@gmail.com
    SQL DAL Maker Website: http://sqldalmaker.sourceforge.net
    Read LICENSE.txt in the root of this project/archive for details.
 */
package com.sqldalmaker.cg;

import com.sqldalmaker.jaxb.dao.DaoClass;
import com.sqldalmaker.jaxb.dao.ExecDml;
import com.sqldalmaker.jaxb.dao.TypeCrud;

/**
 * @author sqldalmaker@gmail.com
 */
public interface IDaoCG {

    String[] translate(String dao_class_name, DaoClass dao_class) throws Exception;

    StringBuilder render_jaxb_query(Object jaxb_query) throws Exception;

    StringBuilder render_jaxb_exec_dml(ExecDml jaxb_exec_dml) throws Exception;

    StringBuilder render_jaxb_crud(TypeCrud jaxb_type_crud) throws Exception;

    StringBuilder render_crud_create(String class_name, String method_name, String table_name, String dto_class_name,
                                     boolean fetch_generated, String generated) throws Exception;

    StringBuilder render_crud_read(String method_name, String dao_table_name, String dto_class_name,
                                   String explicit_pk, boolean fetch_list) throws Exception;

    StringBuilder render_crud_update(String class_name, String method_name, String dao_table_name,
                                     String explicit_pk, String dto_class_name, boolean primitive_params) throws Exception;

    StringBuilder render_crud_delete(String class_name, String method_name, String table_name,
                                     String explicit_pk) throws Exception;
}
