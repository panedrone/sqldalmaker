/*
 * Copyright 2011-2018 sqldalmaker@gmail.com
 * SQL DAL Maker Website: http://sqldalmaker.sourceforge.net
 * Read LICENSE.txt in the root of this project/archive for details.
 */
package com.sqldalmaker.cg;

import com.sqldalmaker.jaxb.dao.Crud;
import com.sqldalmaker.jaxb.dao.CrudAuto;
import com.sqldalmaker.jaxb.dao.DaoClass;
import com.sqldalmaker.jaxb.dao.ExecDml;
import com.sqldalmaker.jaxb.dao.Query;
import com.sqldalmaker.jaxb.dao.QueryDto;
import com.sqldalmaker.jaxb.dao.QueryDtoList;
import com.sqldalmaker.jaxb.dao.QueryList;
import com.sqldalmaker.jaxb.dao.TypeCrud;
import java.util.List;

/**
 *
 * @author sqldalmaker@gmail.com
 *
 */
public class CodeGeneratorHelpers {

    public static void process_element(IDaoCG dao_cg, DaoClass dao_class, List<String> methods) throws Exception {

        if (dao_class.getCrudOrCrudAutoOrQuery() != null) {

            for (int i = 0; i < dao_class.getCrudOrCrudAutoOrQuery().size(); i++) {

                Object element = dao_class.getCrudOrCrudAutoOrQuery().get(i);

                if (element instanceof Query || element instanceof QueryList || element instanceof QueryDto
                        || element instanceof QueryDtoList) {

                    StringBuilder buf = dao_cg.render_element_query(element);

                    methods.add(buf.toString());

                } else if (element instanceof ExecDml) {

                    StringBuilder buf = dao_cg.render_element_exec_dml((ExecDml) element);

                    methods.add(buf.toString());

                } else {

                    StringBuilder buf = dao_cg.render_element_crud(element);

                    methods.add(buf.toString());
                }
            }
        }
    }

    public static StringBuilder process_element_crud(IDaoCG grud_cg, boolean lower_under_scores, TypeCrud element,
            String dto_class_name, String table_attr) throws Exception {

        boolean is_empty = true;

        String node_name = Helpers.get_xml_node_name(element);

        StringBuilder code_buff = new StringBuilder();

        boolean fetch_generated = element.isFetchGenerated();

        String generated = element.getGenerated();

        DbUtils db_utils = grud_cg.get_db_utils();

        // ///////////////////////////////////////////////////////
        // CREATE
        {
            String method_name = null;

            if (element.getCreate() != null) {

                method_name = element.getCreate().getMethod();

            } else {

                if (element instanceof CrudAuto) {

                    method_name = "create" + dto_class_name;
                }
            }

            if (method_name != null) {

                if (lower_under_scores) {

                    method_name = Helpers.camel_case_to_lower_under_scores(method_name);
                }

                is_empty = false;

                StringBuilder sql_buff = new StringBuilder();

                StringBuilder tmp = grud_cg.render_element_crud_insert(sql_buff, null, method_name, table_attr,
                        dto_class_name, fetch_generated, generated);

                code_buff.append(tmp);

                db_utils.validate_sql(sql_buff);
            }
        }

        // ///////////////////////////////////////////////////////
        // READ-ALL
        {
            String method_name = null;

            if (element.getReadAll() != null) {

                method_name = element.getReadAll().getMethod();

            } else {

                if (element instanceof CrudAuto) {

                    method_name = "read" + dto_class_name + "List";
                }
            }

            if (method_name != null) {

                if (lower_under_scores) {

                    method_name = Helpers.camel_case_to_lower_under_scores(method_name);
                }

                is_empty = false;

                StringBuilder sql_buff = new StringBuilder();

                StringBuilder tmp = grud_cg.render_element_crud_read(sql_buff, method_name, table_attr, dto_class_name,
                        true);

                code_buff.append(tmp);

                db_utils.validate_sql(sql_buff);
            }
        }

        // ///////////////////////////////////////////////////////
        // READ
        {
            String method_name = null;

            if (element.getRead() != null) {

                method_name = element.getRead().getMethod();

            } else {

                if (element instanceof CrudAuto) {

                    method_name = "read" + dto_class_name;
                }
            }

            if (method_name != null) {

                if (lower_under_scores) {

                    method_name = Helpers.camel_case_to_lower_under_scores(method_name);
                }

                is_empty = false;

                StringBuilder sql_buff = new StringBuilder();

                StringBuilder tmp = grud_cg.render_element_crud_read(sql_buff, method_name, table_attr, dto_class_name,
                        false);

                code_buff.append(tmp);

                db_utils.validate_sql(sql_buff);
            }
        }

        // ///////////////////////////////////////////////////////
        // UPDATE
        {
            String method_name = null;

            if (element.getUpdate() != null) {

                method_name = element.getUpdate().getMethod();

            } else {

                if (element instanceof CrudAuto) {

                    method_name = "update" + dto_class_name;
                }
            }

            if (method_name != null) {

                if (lower_under_scores) {

                    method_name = Helpers.camel_case_to_lower_under_scores(method_name);
                }

                is_empty = false;

                StringBuilder sql_buff = new StringBuilder();

                StringBuilder tmp = grud_cg.render_element_crud_update(sql_buff, null, method_name, table_attr,
                        dto_class_name, false);

                code_buff.append(tmp);

                db_utils.validate_sql(sql_buff);
            }
        }

        // ///////////////////////////////////////////////////////
        // DELETE
        {
            String method_name = null;

            if (element.getDelete() != null) {

                method_name = element.getDelete().getMethod();

            } else {

                if (element instanceof CrudAuto) {

                    method_name = "delete" + dto_class_name;
                }
            }

            if (method_name != null) {

                if (lower_under_scores) {

                    method_name = Helpers.camel_case_to_lower_under_scores(method_name);
                }

                is_empty = false;

                StringBuilder sql_buff = new StringBuilder();

                StringBuilder tmp = grud_cg.render_element_crud_delete(sql_buff, null, method_name, table_attr,
                        dto_class_name);

                code_buff.append(tmp);

                db_utils.validate_sql(sql_buff);
            }
        }

        if ((element instanceof Crud) && is_empty) {

            throw new Exception(
                    "Element '" + node_name + "' is empty. Add the method declarations or change to 'crud-auto'");
        }

        return code_buff;
    }
}
