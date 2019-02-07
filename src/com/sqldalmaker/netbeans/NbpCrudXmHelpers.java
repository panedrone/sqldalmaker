/*
 * Copyright 2011-2018 sqldalmaker@gmail.com
 * SQL DAL Maker Website: http://sqldalmaker.sourceforge.net
 * Read LICENSE.txt in the root of this project/archive for details.
 */
package com.sqldalmaker.netbeans;

import com.sqldalmaker.cg.DbUtils;
import com.sqldalmaker.cg.Helpers;
import com.sqldalmaker.common.Const;
import com.sqldalmaker.common.FileSearchHelpers;
import com.sqldalmaker.common.XmlParser;
import com.sqldalmaker.jaxb.dao.Crud;
import com.sqldalmaker.jaxb.dao.CrudAuto;
import com.sqldalmaker.jaxb.dao.DaoClass;
import com.sqldalmaker.jaxb.dao.QueryDtoList;
import com.sqldalmaker.jaxb.dao.TypeMethod;
import com.sqldalmaker.jaxb.dto.DtoClass;
import com.sqldalmaker.jaxb.dto.DtoClasses;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import com.sqldalmaker.common.ISelectDbSchemaCallback;

/**
 *
 * @author sqldalmaker@gmail.com
 *
 */
public class NbpCrudXmHelpers {

    public static void get_crud_dto_xml(final SdmDataObject obj) throws Exception {

        ISelectDbSchemaCallback callback = new ISelectDbSchemaCallback() {

            @Override
            public void process_ok(String selected_schema, boolean skip_used,
                    boolean include_views, boolean plural_to_singular, boolean crud_auto, boolean add_fk_access) {

                try {

                    com.sqldalmaker.jaxb.dto.ObjectFactory object_factory = new com.sqldalmaker.jaxb.dto.ObjectFactory();

                    DtoClasses dto_classes = object_factory.createDtoClasses();

                    Connection con = NbpHelpers.get_connection(obj);

                    try {

                        Set<String> in_use;

                        if (skip_used) {

                            in_use = find_tables_used_in_dto_xml(obj);

                        } else {

                            in_use = new HashSet<String>();
                        }

                        List<DtoClass> items = dto_classes.getDtoClass();

                        DatabaseMetaData db_info = con.getMetaData();

                        ResultSet rs = DbUtils.get_tables(con, db_info, selected_schema, include_views);

                        try {

                            while (rs.next()) {

                                String table_name = rs.getString("TABLE_NAME"); // TABLE_NAME is OK for views too

                                if (!in_use.contains(table_name)) {

                                    DtoClass cls = object_factory.createDtoClass();
                                    String dto_class_name = NbpHelpers.table_name_to_dto_class_name(table_name,
                                            plural_to_singular);
                                    cls.setName(dto_class_name);
                                    cls.setRef(table_name);
                                    items.add(cls);
                                }
                            }

                        } finally {

                            rs.close();
                        }

                    } finally {

                        con.close();
                    }

                    NbpIdeEditorHelpers.open_dto_in_editor_async(object_factory, dto_classes, false);

                } catch (Exception e) {

                    NbpIdeMessageHelpers.show_error_in_ui_thread(e);
                }
            }
        };

        UIDialogSelectDbSchema.open(obj, callback, true, false);
    }

    private static Set<String> find_tables_used_in_dto_xml(SdmDataObject obj) throws Exception {

        Set<String> res = new HashSet<String>();

        String dto_xml_abs_path = NbpPathHelpers.get_dto_xml_abs_path(obj);

        String dto_xsd_abs_path = NbpPathHelpers.get_dto_xsd_abs_path(obj);

        List<DtoClass> list = get_dto_classes(dto_xml_abs_path, dto_xsd_abs_path);

        for (DtoClass cls : list) {

            String ref = cls.getRef();

            if (ref.startsWith("table:")) {

                String[] parts = ref.split(":");

                res.add(parts[1]);

            } else if (Helpers.is_table_ref(ref)) {

                res.add(ref);
            }
        }
        return res;
    }

    private static List<DtoClass> get_dto_classes(String dto_xml_abs_path, String dto_xsd_abs_path) throws Exception {

        List<DtoClass> res = new ArrayList<DtoClass>();

        String context_path = DtoClasses.class.getPackage().getName();

        XmlParser xml_parser = new XmlParser(context_path, dto_xsd_abs_path);

        DtoClasses elements = xml_parser.unmarshal(dto_xml_abs_path);

        for (DtoClass cls : elements.getDtoClass()) {

            res.add(cls);
        }

        return res;
    }

    public static void get_crud_dao_xml(final SdmDataObject obj) throws Exception {

        ISelectDbSchemaCallback callback = new ISelectDbSchemaCallback() {

            @Override
            public void process_ok(String selected_schema, boolean skip_used, 
                    boolean include_views, boolean plural_to_singular, boolean crud_auto, boolean add_fk_access) {

                try {

                    com.sqldalmaker.jaxb.dao.ObjectFactory object_factory = new com.sqldalmaker.jaxb.dao.ObjectFactory();

                    DaoClass dao_classes = object_factory.createDaoClass();

                    Connection conn = NbpHelpers.get_connection(obj);

                    try {

                        Set<String> in_use;

                        if (skip_used) {

                            in_use = find_tables_used_in_dao_xml(obj);

                        } else {

                            in_use = new HashSet<String>();
                        }

                        List<Object> nodes = dao_classes.getCrudOrCrudAutoOrQuery();

                        DatabaseMetaData db_info = conn.getMetaData();

                        ResultSet rs = DbUtils.get_tables(conn, db_info, selected_schema, include_views);

                        try {

                            while (rs.next()) {

                                String table_name = rs.getString("TABLE_NAME");

                                if (!in_use.contains(table_name)) {

                                    String dto_class_name = NbpHelpers.table_name_to_dto_class_name(table_name, 
                                            plural_to_singular);

                                    if (crud_auto) {

                                        CrudAuto ca = object_factory.createCrudAuto();
                                        ca.setTable(table_name);
                                        ca.setDto(dto_class_name);
                                        nodes.add(ca);

                                    } else {

                                        Crud crud = object_factory.createCrud();
                                        crud.setDto(dto_class_name);
                                        crud.setTable(table_name);

                                        {
                                            TypeMethod tm = new TypeMethod();
                                            String m = "create" + dto_class_name;
                                            if (NbpTargetLanguageHelpers.underscores_needed(obj)) {
                                                m = Helpers.camel_case_to_lower_under_scores(m);
                                            }
                                            tm.setMethod(m);
                                            crud.setCreate(tm);
                                        }
                                        {
                                            TypeMethod tm = new TypeMethod();
                                            String m = "read" + dto_class_name + "List";
                                            if (NbpTargetLanguageHelpers.underscores_needed(obj)) {
                                                m = Helpers.camel_case_to_lower_under_scores(m);
                                            }
                                            tm.setMethod(m);
                                            crud.setReadAll(tm);
                                        }
                                        {
                                            TypeMethod tm = new TypeMethod();
                                            String m = "read" + dto_class_name;
                                            if (NbpTargetLanguageHelpers.underscores_needed(obj)) {
                                                m = Helpers.camel_case_to_lower_under_scores(m);
                                            }
                                            tm.setMethod(m);
                                            crud.setRead(tm);
                                        }
                                        {
                                            TypeMethod tm = new TypeMethod();
                                            String m = "update" + dto_class_name;
                                            if (NbpTargetLanguageHelpers.underscores_needed(obj)) {
                                                m = Helpers.camel_case_to_lower_under_scores(m);
                                            }
                                            tm.setMethod(m);
                                            crud.setUpdate(tm);
                                        }
                                        {
                                            TypeMethod tm = new TypeMethod();
                                            String m = "delete" + dto_class_name;
                                            if (NbpTargetLanguageHelpers.underscores_needed(obj)) {
                                                m = Helpers.camel_case_to_lower_under_scores(m);
                                            }
                                            tm.setMethod(m);
                                            crud.setDelete(tm);
                                        }

                                        nodes.add(crud);
                                    }
                                }

                                if (add_fk_access) {

                                    add_fk_access(obj, conn, selected_schema, db_info, rs, nodes, plural_to_singular);
                                }
                            }

                        } finally {

                            rs.close();
                        }

                    } finally {

                        conn.close();
                    }

                    NbpIdeEditorHelpers.open_dao_in_editor_async(object_factory, dao_classes);

                } catch (Exception e) {

                    NbpIdeMessageHelpers.show_error_in_ui_thread(e);
                }
            }
        };

        UIDialogSelectDbSchema.open(obj, callback, false, false);
    }

    private static ArrayList<String> fill_dao_file_path_list(SdmDataObject obj) {

        final ArrayList<String> res = new ArrayList<String>();

        FileSearchHelpers.IFile_List file_list = new FileSearchHelpers.IFile_List() {

            @Override
            public void add(String file_name) {

                res.add(file_name);
            }
        };

        String xml_configs_folder_abs_path = NbpPathHelpers.get_metaprogram_abs_path(obj);

        FileSearchHelpers.enum_dao_xml_file_names(xml_configs_folder_abs_path, file_list);

        return res;
    }

    private static Set<String> find_tables_used_in_dao_xml(SdmDataObject obj) throws Exception {

        Set<String> res = new HashSet<String>();

        ArrayList<String> dao_file_path_list = fill_dao_file_path_list(obj);

        String metaprogram_abs_path = NbpPathHelpers.get_metaprogram_abs_path(obj);

        String context_path = DaoClass.class.getPackage().getName();

        XmlParser xml_parser = new XmlParser(context_path, Helpers.concat_path(metaprogram_abs_path, Const.DAO_XSD));

        for (String file_name : dao_file_path_list) {

            Set<String> dao_tables = find_tables_in_use(xml_parser, metaprogram_abs_path + "/" + file_name);

            for (String t : dao_tables) {

                res.add(t);
            }
        }

        return res;
    }

    private static Set<String> find_tables_in_use(XmlParser xml_parser, String file_name) throws Exception {

        Set<String> res = new HashSet<String>();

        DaoClass dao_class = xml_parser.unmarshal(file_name);

        if (dao_class.getCrudOrCrudAutoOrQuery() != null) {

            for (int i = 0; i < dao_class.getCrudOrCrudAutoOrQuery().size(); i++) {

                Object element = dao_class.getCrudOrCrudAutoOrQuery().get(i);

                if (element instanceof Crud) {

                    Crud c = (Crud) element;

                    res.add(c.getTable());

                } else if (element instanceof CrudAuto) {

                    CrudAuto c = (CrudAuto) element;

                    res.add(c.getTable());

                } else {

                    throw new Exception("Unexpected XML element: " + element.getClass().getTypeName());
                }
            }
        }

        return res;
    }

    private static void add_fk_access(final SdmDataObject obj, Connection conn, String selected_schema,
            DatabaseMetaData db_info, ResultSet rs_tables, List<Object> nodes, boolean plural_to_singular)
            throws SQLException {

        String fk_table_name = rs_tables.getString("TABLE_NAME");

        ResultSet rs = db_info.getImportedKeys(conn.getCatalog(), selected_schema, fk_table_name);

        // [pk_table_name - list of fk_column_name]
        //
        HashMap<String, List<String>> hm = new HashMap<String, List<String>>();

        try {

            while (rs.next()) {

                String pk_table_name = rs.getString("PKTABLE_NAME");

                String fk_column_name = rs.getString("FKCOLUMN_NAME");

                if (!hm.containsKey(pk_table_name)) {

                    hm.put(pk_table_name, new ArrayList<String>());
                }

                hm.get(pk_table_name).add(fk_column_name);

                // int fkSequence = rs.getInt("KEY_SEQ");
                //
                // System.out.print(fk_table_name);
                // System.out.print("\t" + fk_column_name);
                // System.out.println("\t" + fkSequence);
            }

        } finally {

            rs.close();
        }

        for (String pk_table_name : hm.keySet()) {

            List<String> fk_column_names = hm.get(pk_table_name);

            String params = "";

            String columns = "";

            boolean first = true;

            for (String fk_column_name : fk_column_names) {

                String c = fk_column_name;

                c = Helpers.camel_case_to_lower_under_scores(c);

                if (first) {

                    params += c;

                    columns += fk_column_name;

                    first = false;

                } else {

                    params += ", " + c;

                    columns += ", " + fk_column_name;
                }
            }

            String dto_class_name = NbpHelpers.table_name_to_dto_class_name(fk_table_name, plural_to_singular);

            String method_name = "get" + NbpHelpers.table_name_to_dto_class_name(fk_table_name, false) + "By"
                    + NbpHelpers.table_name_to_dto_class_name(pk_table_name, true);

            if (NbpTargetLanguageHelpers.underscores_needed(obj)) {

                method_name = Helpers.camel_case_to_lower_under_scores(method_name);
            }

            QueryDtoList node = new QueryDtoList();

            node.setDto(dto_class_name);

            String method = method_name + "(" + params.toLowerCase() + ")";

            node.setMethod(method);

            node.setRef(fk_table_name + "(" + columns + ")");

            nodes.add(node);
        }
    }

    public static void get_fk_access_xml(final SdmDataObject obj) throws Exception {

        ISelectDbSchemaCallback callback = new ISelectDbSchemaCallback() {

            @Override
            public void process_ok(String selected_schema, boolean skip_used, 
                    boolean include_views, boolean plural_to_singular, boolean crud_auto, boolean add_fk_access) {

                try {

                    com.sqldalmaker.jaxb.dao.ObjectFactory object_factory = new com.sqldalmaker.jaxb.dao.ObjectFactory();

                    DaoClass dao_classes = object_factory.createDaoClass();

                    List<Object> nodes = dao_classes.getCrudOrCrudAutoOrQuery();

                    Connection conn = NbpHelpers.get_connection(obj);

                    try {

                        DatabaseMetaData db_info = conn.getMetaData();

                        ResultSet rs = DbUtils.get_tables(conn, db_info, selected_schema, false);

                        try {

                            while (rs.next()) {

                                add_fk_access(obj, conn, selected_schema, db_info, rs, nodes, plural_to_singular);
                            }

                        } finally {

                            rs.close();
                        }

                    } finally {

                        conn.close();
                    }

                    NbpIdeEditorHelpers.open_fk_dao_in_editor_async(object_factory, dao_classes);

                } catch (Exception e) {

                    NbpIdeMessageHelpers.show_error_in_ui_thread(e);
                }
            }
        };

        UIDialogSelectDbSchema.open(obj, callback, false, true);
    }
}
