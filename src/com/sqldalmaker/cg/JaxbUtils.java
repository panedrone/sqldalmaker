/*
    Copyright 2011-2023 sqldalmaker@gmail.com
    SQL DAL Maker Website: https://sqldalmaker.sourceforge.net/
    Read LICENSE.txt in the root of this project/archive for details.
 */
package com.sqldalmaker.cg;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.bind.annotation.XmlRootElement;

import com.sqldalmaker.jaxb.sdm.Crud;
import com.sqldalmaker.jaxb.sdm.DaoClass;
import com.sqldalmaker.jaxb.sdm.ExecDml;
import com.sqldalmaker.jaxb.sdm.Query;
import com.sqldalmaker.jaxb.sdm.QueryDto;
import com.sqldalmaker.jaxb.sdm.QueryDtoList;
import com.sqldalmaker.jaxb.sdm.QueryList;
import com.sqldalmaker.jaxb.sdm.TypeMethod;
import com.sqldalmaker.jaxb.sdm.DtoClass;

/**
 * @author sqldalmaker@gmail.com
 */
public class JaxbUtils {

    public static String get_jaxb_node_name(Object jaxb_node) {

        XmlRootElement attr = jaxb_node.getClass().getAnnotation(XmlRootElement.class);
        return attr.name();
    }

    public static DtoClass find_jaxb_dto_class(String dto_class_name,
                                               List<DtoClass> jaxb_dto_classes) throws Exception {

        if (dto_class_name == null || dto_class_name.length() == 0) {
            throw new Exception("Invalid DTO class name: " + dto_class_name);
        }
        for (DtoClass cls : jaxb_dto_classes) {
            String name = cls.getName();
            if (name != null && name.equals(dto_class_name)) {
                return cls;
            }
        }
        throw new Exception("DTO XML element not found: '" + dto_class_name + "'");
    }

    public static DaoClass find_jaxb_dao_class(String dao_class_name,
                                               List<DaoClass> jaxb_dao_classes) throws Exception {

        if (dao_class_name == null || dao_class_name.length() == 0) {
            throw new Exception("Invalid DAO class name: " + dao_class_name);
        }
        for (DaoClass cls : jaxb_dao_classes) {
            String name = cls.getName();
            if (name != null && name.equals(dao_class_name)) {
                return cls;
            }
        }
        throw new Exception("DTO XML element not found: '" + dao_class_name + "'");
    }

    public static void process_jaxb_dao_class(IDaoCG dao_cg,
                                              String dao_class_name,
                                              DaoClass jaxb_dao_class,
                                              List<String> methods) throws Exception {

        if (jaxb_dao_class.getCrudOrQueryOrQueryList() != null) {
            for (int i = 0; i < jaxb_dao_class.getCrudOrQueryOrQueryList().size(); i++) {
                Object jaxb_element = jaxb_dao_class.getCrudOrQueryOrQueryList().get(i);
                if (jaxb_element instanceof Query || jaxb_element instanceof QueryList
                        || jaxb_element instanceof QueryDto || jaxb_element instanceof QueryDtoList) {
                    StringBuilder buf = dao_cg.render_jaxb_query(jaxb_element);
                    methods.add(buf.toString());
                } else if (jaxb_element instanceof ExecDml) {
                    StringBuilder buf = dao_cg.render_jaxb_exec_dml((ExecDml) jaxb_element);
                    methods.add(buf.toString());
                } else if (jaxb_element instanceof Crud) {
                    StringBuilder buf = dao_cg.render_jaxb_crud(dao_class_name, (Crud) jaxb_element);
                    methods.add(buf.toString());
                } else {
                    throw new Exception("Unexpected element found in DTO XML file");
                }
            }
        }
    }

    private static boolean _process_jaxb_crud_create(IDaoCG dao_cg,
                                                     Crud jaxb_type_crud,
                                                     String dto_class_name,
                                                     String table_name,
                                                     String auto_column,
                                                     FieldNamesMode field_names_mode,
                                                     StringBuilder code_buff) throws Exception {
        if (jaxb_type_crud.getCreate() == null) {
            return false;
        }
        String method_name = jaxb_type_crud.getCreate().getMethod();
        if (method_name == null || "".equals(method_name)) {
            method_name = build_method_name("create", dto_class_name);
        }
        method_name = Helpers.get_method_name(method_name, field_names_mode);
        boolean fetch_generated = jaxb_type_crud.isFetchGenerated();
        StringBuilder tmp = dao_cg.render_crud_create(null, method_name, table_name, dto_class_name, fetch_generated, auto_column);
        code_buff.append(tmp);
        return true;
    }

    private static boolean _process_jaxb_crud_read_all(IDaoCG dao_cg,
                                                       Crud jaxb_type_crud,
                                                       String dto_class_name,
                                                       String table_name,
                                                       FieldNamesMode field_names_mode,
                                                       StringBuilder code_buff) throws Exception {
        if (jaxb_type_crud.getReadAll() == null) {
            return false;
        }
        String method_name = jaxb_type_crud.getReadAll().getMethod();
        if (method_name == null || "".equals(method_name)) {
            method_name = build_method_name("read", dto_class_name + "List");
        }
        method_name = Helpers.get_method_name(method_name, field_names_mode);
        StringBuilder tmp = dao_cg.render_crud_read(method_name, table_name, dto_class_name, null, true);
        code_buff.append(tmp);
        return true;
    }

    private static boolean _process_jaxb_crud_read(IDaoCG dao_cg,
                                                   Crud jaxb_type_crud,
                                                   String dto_class_name,
                                                   String table_name,
                                                   String explicit_pk,
                                                   FieldNamesMode field_names_mode,
                                                   StringBuilder code_buff) throws Exception {
        if (jaxb_type_crud.getRead() == null) {
            return false;
        }
        String method_name = jaxb_type_crud.getRead().getMethod();
        if (method_name == null || "".equals(method_name)) {
            method_name = build_method_name("read", dto_class_name);
        }
        method_name = Helpers.get_method_name(method_name, field_names_mode);
        StringBuilder tmp = dao_cg.render_crud_read(method_name, table_name, dto_class_name, explicit_pk, false);
        code_buff.append(tmp);
        return true;
    }

    private static boolean _process_jaxb_crud_update(IDaoCG dao_cg,
                                                     Crud jaxb_type_crud,
                                                     String dao_class_name,
                                                     String dto_class_name,
                                                     String table_name,
                                                     String explicit_primary_keys,
                                                     FieldNamesMode field_names_mode,
                                                     StringBuilder code_buff) throws Exception {
        if (jaxb_type_crud.getUpdate() == null) {
            return false;
        }
        String method_name = jaxb_type_crud.getUpdate().getMethod();
        if (method_name == null || "".equals(method_name)) {
            method_name = build_method_name("update", dto_class_name);
        }
        method_name = Helpers.get_method_name(method_name, field_names_mode);
        StringBuilder tmp = dao_cg.render_crud_update(dao_class_name, method_name, table_name, explicit_primary_keys,
                dto_class_name, false);
        code_buff.append(tmp);
        return true;
    }

    private static boolean _process_jaxb_crud_delete(IDaoCG dao_cg,
                                                     Crud jaxb_type_crud,
                                                     String dao_class_name,
                                                     String dto_class_name,
                                                     String table_name,
                                                     String explicit_pk,
                                                     FieldNamesMode field_names_mode,
                                                     StringBuilder code_buff) throws Exception {
        if (jaxb_type_crud.getDelete() == null) {
            return false;
        }
        String method_name = jaxb_type_crud.getDelete().getMethod();
        if (method_name == null || "".equals(method_name)) {
            method_name = build_method_name("delete", dto_class_name);
        }
        method_name = Helpers.get_method_name(method_name, field_names_mode);
        StringBuilder tmp = dao_cg.render_crud_delete(dao_class_name, dto_class_name, method_name, table_name, explicit_pk);
        code_buff.append(tmp);
        return true;
    }

    private static String build_method_name(String base,
                                            String dto_class_name) {

        int model_name_end_index = dto_class_name.indexOf('-');
        if (model_name_end_index == -1) {
            return base + dto_class_name;
        }
        dto_class_name = dto_class_name.substring(model_name_end_index + 1);
        return base + dto_class_name;
    }

    public static StringBuilder process_jaxb_crud(IDaoCG dao_cg,
                                                  FieldNamesMode field_names_mode,
                                                  Crud jaxb_type_crud,
                                                  String dao_class_name,
                                                  DtoClass jaxb_dto_class) throws Exception {

        String dto_class_name = jaxb_type_crud.getDto();
        String explicit_primary_keys = jaxb_dto_class.getPk();
        String explicit_auto_column = jaxb_dto_class.getAuto();
        String table_name = jaxb_type_crud.getTable();
        table_name = JaxbUtils.refine_table_name(jaxb_dto_class, table_name);

        boolean is_empty = true;
        StringBuilder code_buff = new StringBuilder();
        if (_process_jaxb_crud_create(dao_cg, jaxb_type_crud, dto_class_name, table_name, explicit_auto_column, field_names_mode, code_buff)) {
            is_empty = false;
        }
        if (_process_jaxb_crud_read_all(dao_cg, jaxb_type_crud, dto_class_name, table_name, field_names_mode,
                code_buff)) {
            is_empty = false;
        }
        if (_process_jaxb_crud_read(dao_cg, jaxb_type_crud, dto_class_name, table_name, explicit_primary_keys,
                field_names_mode, code_buff)) {
            is_empty = false;
        }
        if (_process_jaxb_crud_update(dao_cg, jaxb_type_crud, dao_class_name, dto_class_name, table_name, explicit_primary_keys,
                field_names_mode, code_buff)) {
            is_empty = false;
        }
        if (_process_jaxb_crud_delete(dao_cg, jaxb_type_crud, dao_class_name, dto_class_name, table_name, explicit_primary_keys,
                field_names_mode, code_buff)) {
            is_empty = false;
        }
        if (is_empty) {
            jaxb_type_crud.setCreate(new TypeMethod());
            jaxb_type_crud.setRead(new TypeMethod());
            jaxb_type_crud.setReadAll(new TypeMethod());
            jaxb_type_crud.setUpdate(new TypeMethod());
            jaxb_type_crud.setDelete(new TypeMethod());
            return process_jaxb_crud(dao_cg, field_names_mode, jaxb_type_crud, dao_class_name, jaxb_dto_class);
        }
        return code_buff;
    }

    public static Set<String> get_pk_col_name_aliaces_from_jaxb(String explicit_pk) throws Exception {
        // if PK are specified explicitely, don't use getPrimaryKeys at all
        String[] gen_keys_arr = Helpers.get_listed_items(explicit_pk, false);
        Helpers.check_duplicates(gen_keys_arr);
        for (int i = 0; i < gen_keys_arr.length; i++) {
            gen_keys_arr[i] = Helpers.get_pk_col_name_alias(gen_keys_arr[i].toLowerCase());
        }
        return new HashSet<String>(Arrays.asList(gen_keys_arr));
    }

    private static String refine_table_name(DtoClass jaxb_dto_class, String dao_table_name) throws Exception {
        if ("*".equals(dao_table_name)) {
            String dto_class_ref = jaxb_dto_class.getRef();
            if (!SqlUtils.is_table_ref(dto_class_ref)) {
                String dto_node_name = JaxbUtils.get_jaxb_node_name(jaxb_dto_class);
                String dto_class_name = jaxb_dto_class.getName();
                throw new Exception(", but 'ref' is not a table: <" + dto_node_name + " name=\"" + dto_class_name +
                        "\" ref=\"" + dto_class_ref + "\"..");
            }
            return dto_class_ref;
        }
        if (dao_table_name == null || dao_table_name.length() == 0) {
            throw new Exception("'table' is empty");
        }
        return dao_table_name;
    }
}
