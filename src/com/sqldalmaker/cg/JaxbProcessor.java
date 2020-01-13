package com.sqldalmaker;

import com.sqldalmaker.cg.Helpers;
import com.sqldalmaker.cg.IDaoCG;
import com.sqldalmaker.jaxb.dao.*;

import java.util.List;

public class JaxbProcessor {

    public static void process_jaxb_dao_class(IDaoCG dao_cg, DaoClass jaxb_dao_class, List<String> methods) throws Exception {

        if (jaxb_dao_class.getCrudOrCrudAutoOrQuery() != null) {

            for (int i = 0; i < jaxb_dao_class.getCrudOrCrudAutoOrQuery().size(); i++) {

                Object jaxb_element = jaxb_dao_class.getCrudOrCrudAutoOrQuery().get(i);

                if (jaxb_element instanceof Query || jaxb_element instanceof QueryList || jaxb_element instanceof QueryDto
                        || jaxb_element instanceof QueryDtoList) {

                    StringBuilder buf = dao_cg.render_jaxb_query(jaxb_element);

                    methods.add(buf.toString());

                } else if (jaxb_element instanceof ExecDml) {

                    StringBuilder buf = dao_cg.render_jaxb_exec_dml((ExecDml) jaxb_element);

                    methods.add(buf.toString());

                } else if (jaxb_element instanceof TypeCrud) {

                    StringBuilder buf = dao_cg.render_jaxb_crud((TypeCrud)jaxb_element);

                    methods.add(buf.toString());

                } else {

                    throw new Exception("Unexpected element found in DTO XML file");
                }
            }
        }
    }

    private static boolean process_jaxb_crud_create(IDaoCG dao_cg, TypeCrud jaxb_type_crud, String dto_class_name,
                                                    String table_name, boolean lower_under_scores, StringBuilder code_buff) throws Exception {

        String method_name = null;

        if (jaxb_type_crud.getCreate() != null) {

            method_name = jaxb_type_crud.getCreate().getMethod();

        } else {

            if (jaxb_type_crud instanceof CrudAuto) {

                method_name = "create" + dto_class_name;
            }
        }

        if (method_name == null) {

            return true;
        }

        if (lower_under_scores) {

            method_name = Helpers.camel_case_to_lower_under_scores(method_name);
        }

        boolean fetch_generated = jaxb_type_crud.isFetchGenerated();

        String generated = jaxb_type_crud.getGenerated();

        StringBuilder tmp = dao_cg.render_crud_create(null, method_name, table_name, dto_class_name,
                fetch_generated, generated);

        code_buff.append(tmp);

        return true;
    }

    private static boolean process_jaxb_crud_read_all(IDaoCG dao_cg, TypeCrud jaxb_type_crud, String dto_class_name,
                                                      String table_name, boolean lower_under_scores, StringBuilder code_buff) throws Exception {

        String method_name = null;

        if (jaxb_type_crud.getReadAll() != null) {

            method_name = jaxb_type_crud.getReadAll().getMethod();

        } else {

            if (jaxb_type_crud instanceof CrudAuto) {

                method_name = "read" + dto_class_name + "List";
            }
        }

        if (method_name == null) {

            return true;
        }

        if (lower_under_scores) {

            method_name = Helpers.camel_case_to_lower_under_scores(method_name);
        }

        StringBuilder tmp = dao_cg.render_crud_read(method_name, table_name, dto_class_name, true);

        code_buff.append(tmp);

        return true;
    }

    private static boolean process_jaxb_crud_read(IDaoCG dao_cg, TypeCrud jaxb_type_crud, String dto_class_name,
                                                  String table_name, boolean lower_under_scores, StringBuilder code_buff) throws Exception {

        String method_name = null;

        if (jaxb_type_crud.getRead() != null) {

            method_name = jaxb_type_crud.getRead().getMethod();

        } else {

            if (jaxb_type_crud instanceof CrudAuto) {

                method_name = "read" + dto_class_name;
            }
        }

        if (method_name == null) {

            return true;
        }

        if (lower_under_scores) {

            method_name = Helpers.camel_case_to_lower_under_scores(method_name);
        }

        StringBuilder tmp = dao_cg.render_crud_read(method_name, table_name, dto_class_name, false);

        code_buff.append(tmp);

        return true;
    }

    private static boolean process_jaxb_crud_update(IDaoCG dao_cg, TypeCrud jaxb_type_crud, String dto_class_name,
                                                    String table_name, boolean lower_under_scores, StringBuilder code_buff) throws Exception {

        String method_name = null;

        if (jaxb_type_crud.getUpdate() != null) {

            method_name = jaxb_type_crud.getUpdate().getMethod();

        } else {

            if (jaxb_type_crud instanceof CrudAuto) {

                method_name = "update" + dto_class_name;
            }
        }

        if (method_name == null) {

            return true;
        }

        if (lower_under_scores) {

            method_name = Helpers.camel_case_to_lower_under_scores(method_name);
        }

        StringBuilder tmp = dao_cg.render_crud_update(null, method_name, table_name, dto_class_name,
                false);

        code_buff.append(tmp);

        return true;
    }

    private static boolean process_jaxb_crud_delete(IDaoCG dao_cg, TypeCrud jaxb_type_crud, String dto_class_name,
                                                    String table_name, boolean lower_under_scores, StringBuilder code_buff) throws Exception {

        String method_name = null;

        if (jaxb_type_crud.getDelete() != null) {

            method_name = jaxb_type_crud.getDelete().getMethod();

        } else {

            if (jaxb_type_crud instanceof CrudAuto) {

                method_name = "delete" + dto_class_name;
            }
        }

        if (method_name == null) {

            return true;
        }

        if (lower_under_scores) {

            method_name = Helpers.camel_case_to_lower_under_scores(method_name);
        }

        StringBuilder tmp = dao_cg.render_crud_delete(null, method_name, table_name, dto_class_name);

        code_buff.append(tmp);

        return true;
    }

    public static StringBuilder process_jaxb_crud(IDaoCG dao_cg, boolean lower_under_scores, TypeCrud jaxb_type_crud,
                                                  String dto_class_name, String table_name) throws Exception {

        boolean is_empty = true;

        StringBuilder code_buff = new StringBuilder();

        if (process_jaxb_crud_create(dao_cg, jaxb_type_crud, dto_class_name, table_name, lower_under_scores, code_buff)) {

            is_empty = false;
        }

        if (process_jaxb_crud_read_all(dao_cg, jaxb_type_crud, dto_class_name, table_name, lower_under_scores, code_buff)) {

            is_empty = false;
        }

        if (process_jaxb_crud_read(dao_cg, jaxb_type_crud, dto_class_name, table_name, lower_under_scores, code_buff)) {

            is_empty = false;
        }

        if (process_jaxb_crud_update(dao_cg, jaxb_type_crud, dto_class_name, table_name, lower_under_scores, code_buff)) {

            is_empty = false;
        }

        if (process_jaxb_crud_delete(dao_cg, jaxb_type_crud, dto_class_name, table_name, lower_under_scores, code_buff)) {

            is_empty = false;
        }

        if ((jaxb_type_crud instanceof Crud) && is_empty) {

            String node_name = Helpers.get_jaxb_node_name(jaxb_type_crud);

            throw new Exception(
                    "Element '" + node_name + "' is empty. Add the method declarations or change to 'crud-auto'");
        }

        return code_buff;
    }
}
