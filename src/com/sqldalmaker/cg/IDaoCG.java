/*
    Copyright 2011-2023 sqldalmaker@gmail.com
    SQL DAL Maker Website: https://sqldalmaker.sourceforge.net/
    Read LICENSE.txt in the root of this project/archive for details.
 */
package com.sqldalmaker.cg;

import com.sqldalmaker.jaxb.sdm.DaoClass;
import com.sqldalmaker.jaxb.sdm.ExecDml;
import com.sqldalmaker.jaxb.sdm.Crud;

/*
 * 16.11.2022 08:02 1.269
 * 06.08.2022 08:37 1.261
 * 27.05.2022 01:17 1.246
 * 20.04.2022 09:58
 * 08.05.2021 22:29 1.200
 * 03.09.2019 15:55
 * 07.02.2019 19:50 initial commit
 *
 */
public interface IDaoCG {

    String[] translate(String dao_class_name, DaoClass dao_class) throws Exception;

    StringBuilder render_jaxb_query(Object jaxb_query) throws Exception;

    StringBuilder render_jaxb_exec_dml(ExecDml jaxb_exec_dml) throws Exception;

    StringBuilder render_jaxb_crud(String dao_class_name, Crud jaxb_type_crud) throws Exception;

    StringBuilder render_crud_create(
            String dao_class_name,
            String method_name,
            String table_name, String dto_class_name,
            boolean fetch_generated,
            String generated) throws Exception;

    StringBuilder render_crud_read(
            String method_name,
            String dao_table_name,
            String dto_class_name,
            String explicit_pk,
            boolean fetch_list) throws Exception;

    StringBuilder render_crud_update(
            String dao_class_name,
            String method_name,
            String dao_table_name,
            String explicit_pk,
            String dto_class_name,
            boolean scalar_params) throws Exception;

    StringBuilder render_crud_delete(
            String dao_class_name,
            String dto_class_name,
            String method_name,
            String table_name,
            String explicit_pk) throws Exception;
}
