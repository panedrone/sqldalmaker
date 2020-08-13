/*
 * Copyright 2011-2020 sqldalmaker@gmail.com
 * SQL DAL Maker Website: http://sqldalmaker.sourceforge.net
 * Read LICENSE.txt in the root of this project/archive for details.
 */
package com.sqldalmaker.cg;

import com.sqldalmaker.jaxb.dao.*;
import com.sqldalmaker.jaxb.dto.DtoClass;
import com.sqldalmaker.jaxb.dto.DtoClasses;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author sqldalmaker@gmail.com
 */
public class JaxbUtils {

    public static String get_jaxb_node_name(Object jaxb_node) {
        XmlRootElement attr = jaxb_node.getClass().getAnnotation(XmlRootElement.class);
        return attr.name();
    }

    public static DtoClass find_jaxb_dto_class(String dto_class_name, DtoClasses jaxb_dto_classes) throws Exception {
        if (dto_class_name == null || dto_class_name.length() == 0) {
            throw new Exception("Invalid name of DTO class: " + dto_class_name);
        }
        DtoClass res = null;
        int found = 0;
        for (DtoClass cls : jaxb_dto_classes.getDtoClass()) {
            String name = cls.getName();
            if (name != null && name.equals(dto_class_name)) {
                res = cls;
                found++;
            }
        }
        if (found == 0) {
            throw new Exception("DTO XML element not found: '" + dto_class_name + "'");
        } else if (found > 1) {
            throw new Exception("Duplicate DTO XML elements for name='" + dto_class_name + "' found.");
        }
        return res;
    }

    public static void process_jaxb_dao_class(IDaoCG dao_cg, DaoClass jaxb_dao_class, List<String> methods) throws Exception {
        if (jaxb_dao_class.getCrudOrCrudAutoOrQuery() != null) {
            for (int i = 0; i < jaxb_dao_class.getCrudOrCrudAutoOrQuery().size(); i++) {
                Object jaxb_element = jaxb_dao_class.getCrudOrCrudAutoOrQuery().get(i);
                if (jaxb_element instanceof Query || jaxb_element instanceof QueryList
                        || jaxb_element instanceof QueryDto || jaxb_element instanceof QueryDtoList) {
                    StringBuilder buf = dao_cg.render_jaxb_query(jaxb_element);
                    methods.add(buf.toString());
                } else if (jaxb_element instanceof ExecDml) {
                    StringBuilder buf = dao_cg.render_jaxb_exec_dml((ExecDml) jaxb_element);
                    methods.add(buf.toString());
                } else if (jaxb_element instanceof TypeCrud) {
                    StringBuilder buf = dao_cg.render_jaxb_crud((TypeCrud) jaxb_element);
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
        StringBuilder tmp = dao_cg.render_crud_create(null, method_name, table_name, dto_class_name, fetch_generated,
                generated);
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
        StringBuilder tmp = dao_cg.render_crud_read(method_name, table_name, dto_class_name, null, true);
        code_buff.append(tmp);
        return true;
    }

    private static boolean process_jaxb_crud_read(IDaoCG dao_cg, TypeCrud jaxb_type_crud, String dto_class_name,
                                                  String table_name, String explicit_pk, boolean lower_under_scores,
                                                  StringBuilder code_buff) throws Exception {
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
        StringBuilder tmp = dao_cg.render_crud_read(method_name, table_name, dto_class_name, explicit_pk, false);
        code_buff.append(tmp);
        return true;
    }

    private static boolean process_jaxb_crud_update(IDaoCG dao_cg, TypeCrud jaxb_type_crud, String dto_class_name,
                                                    String table_name, String explicit_primary_keys, boolean lower_under_scores,
                                                    StringBuilder code_buff) throws Exception {
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
        StringBuilder tmp = dao_cg.render_crud_update(null, method_name, table_name, explicit_primary_keys,
                dto_class_name, false);
        code_buff.append(tmp);
        return true;
    }

    private static boolean process_jaxb_crud_delete(IDaoCG dao_cg, TypeCrud jaxb_type_crud, String dto_class_name,
                                                    String table_name, String explicit_pk, boolean lower_under_scores,
                                                    StringBuilder code_buff) throws Exception {
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
        StringBuilder tmp = dao_cg.render_crud_delete(null, method_name, table_name, explicit_pk);
        code_buff.append(tmp);
        return true;
    }

    public static StringBuilder process_jaxb_crud(IDaoCG dao_cg, boolean lower_under_scores, TypeCrud jaxb_type_crud,
                                                  String dto_class_name) throws Exception {
        String table_name = jaxb_type_crud.getTable();
        String explicit_primary_keys = jaxb_type_crud.getPk();
        boolean is_empty = true;
        StringBuilder code_buff = new StringBuilder();
        if (process_jaxb_crud_create(dao_cg, jaxb_type_crud, dto_class_name, table_name, lower_under_scores,
                code_buff)) {
            is_empty = false;
        }
        if (process_jaxb_crud_read_all(dao_cg, jaxb_type_crud, dto_class_name, table_name, lower_under_scores,
                code_buff)) {
            is_empty = false;
        }
        if (process_jaxb_crud_read(dao_cg, jaxb_type_crud, dto_class_name, table_name, explicit_primary_keys,
                lower_under_scores, code_buff)) {
            is_empty = false;
        }
        if (process_jaxb_crud_update(dao_cg, jaxb_type_crud, dto_class_name, table_name, explicit_primary_keys,
                lower_under_scores, code_buff)) {
            is_empty = false;
        }
        if (process_jaxb_crud_delete(dao_cg, jaxb_type_crud, dto_class_name, table_name, explicit_primary_keys,
                lower_under_scores, code_buff)) {
            is_empty = false;
        }
        if ((jaxb_type_crud instanceof Crud) && is_empty) {
            String node_name = get_jaxb_node_name(jaxb_type_crud);
            throw new Exception("Element '" + node_name + "' is empty. Add the method declarations or change to 'crud-auto'");
        }
        return code_buff;
    }

    public static void validate_jaxb_dto_class(DtoClass jaxb_dto_class) throws Exception {
        List<DtoClass.Field> fields = jaxb_dto_class.getField();
        Set<String> col_names = new HashSet<String>();
        for (DtoClass.Field fe : fields) {
            String java_class_name = fe.getJavaType();
            Helpers.validate_java_type_name(java_class_name);
            String col = fe.getColumn();
            if (col == null || col.trim().length() == 0) {
                throw new Exception("Invalid column name: null");
            }
            if (col_names.contains(col)) {
                throw new Exception("Duplicate <field column='" + col + "'...");
            }
            col_names.add(col);
        }
    }
}
