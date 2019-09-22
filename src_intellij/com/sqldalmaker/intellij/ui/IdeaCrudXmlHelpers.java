/*
 * Copyright 2011-2018 sqldalmaker@gmail.com
 * SQL DAL Maker Website: http://sqldalmaker.sourceforge.net
 * Read LICENSE.txt in the root of this project/archive for details.
 */
package com.sqldalmaker.intellij.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.sqldalmaker.cg.DbUtils;
import com.sqldalmaker.cg.Helpers;
import com.sqldalmaker.common.*;
import com.sqldalmaker.jaxb.dao.*;
import com.sqldalmaker.jaxb.dto.DtoClass;
import com.sqldalmaker.jaxb.dto.DtoClasses;
import com.sqldalmaker.jaxb.settings.Settings;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: sqldalmaker@gmail.com
 * Date: 21.06.12
 */
public class IdeaCrudXmlHelpers {

    private static Set<String> find_tables_used_in_dto_xml(VirtualFile root_file) throws Exception {

        Set<String> res = new HashSet<String>();

        String xml_configs_folder_full_path = root_file.getParent().getPath();

        String dto_xml_abs_path = xml_configs_folder_full_path + "/" + Const.DTO_XML;

        String dto_xsd_abs_path = xml_configs_folder_full_path + "/" + Const.DTO_XSD;

        List<DtoClass> list = IdeaHelpers.get_dto_classes(dto_xml_abs_path, dto_xsd_abs_path);

        for (DtoClass cls : list) {

            String ref = cls.getRef();

            if (ref.startsWith("table:")) {

                String[] parts = ref.split(":");
                String table_name = parts[1];
                res.add(table_name);

            } else if (DbUtils.is_table_ref(ref)) {

                res.add(ref);
            }
        }

        return res;
    }

    public static void get_crud_dto_xml(final Project project, final VirtualFile root_file) {

        ISelectDbSchemaCallback callback = new ISelectDbSchemaCallback() {

            @Override
            public void process_ok(String selected_schema, boolean skip_used, boolean include_views,
                                   boolean plural_to_singular, boolean crud_auto, boolean add_fk_access) {

                try {

                    Settings settings = IdeaHelpers.load_settings(root_file);

                    com.sqldalmaker.jaxb.dto.ObjectFactory object_factory = new com.sqldalmaker.jaxb.dto.ObjectFactory();

                    DtoClasses dto_classes = object_factory.createDtoClasses();

                    Connection con = IdeaHelpers.get_connection(project, settings);

                    try {

                        // !!!! after 'try'
                        Set<String> in_use;

                        if (skip_used) {

                            in_use = find_tables_used_in_dto_xml(root_file);

                        } else {

                            in_use = new HashSet<String>();
                        }

                        List<DtoClass> items = dto_classes.getDtoClass();

                        DatabaseMetaData db_info = con.getMetaData();

                        ResultSet rs = DbUtils.get_tables(con, db_info, selected_schema, include_views);

                        try {

                            while (rs.next()) {

                                // http://docs.oracle.com/javase/1.4.2/docs/api/java/sql/DatabaseMetaData.html

                                try {

                                    String table_type = rs.getString("TABLE_TYPE");

                                    if (!("TABLE".equalsIgnoreCase(table_type)
                                            || "VIEW".equalsIgnoreCase(table_type))) {

                                        continue;
                                    }

                                    String table_name = rs.getString("TABLE_NAME");

                                    if (!in_use.contains(table_name)) {

                                        DtoClass cls = object_factory.createDtoClass();
                                        String dto_class_name = IdeaHelpers.table_name_to_dto_class_name(
                                                table_name, plural_to_singular);
                                        cls.setName(dto_class_name);
                                        cls.setRef(/*"table:" +*/ table_name);
                                        items.add(cls);
                                    }

                                } catch (Throwable e) {

                                    // just skip
                                }
                            }

                        } finally {

                            rs.close();
                        }

                    } finally {

                        con.close();
                    }

                    IdeaEditorHelpers.open_dto_xml_in_editor(object_factory, project, dto_classes, false);

                } catch (Exception e) {

                    e.printStackTrace();

                    IdeaMessageHelpers.show_error_in_ui_thread(e);
                }
            }
        };

        try {

            UIDialogSelectDbSchema.open(project, root_file, callback, true, false);

        } catch (Exception e) {

            e.printStackTrace();

            IdeaMessageHelpers.show_error_in_ui_thread(e);
        }
    }

    private static ArrayList<String> fill_dao_file_path_list(VirtualFile root_file) {

        final ArrayList<String> res = new ArrayList<String>();

        FileSearchHelpers.IFile_List file_list = new FileSearchHelpers.IFile_List() {

            @Override
            public void add(String file_path) {
                res.add(file_path);
            }
        };

        String xml_configs_folder_full_path = root_file.getParent().getPath();

        FileSearchHelpers.enum_dao_xml_file_names(xml_configs_folder_full_path, file_list);

        return res;
    }

    private static Set<String> find_tables_used_in_dao_xml(VirtualFile root_file) throws Exception {

        Set<String> res = new HashSet<String>();

        ArrayList<String> dao_file_path_list = fill_dao_file_path_list(root_file);

        String meta_program_folder_abs_path = root_file.getParent().getPath();

        String context_path = DaoClass.class.getPackage().getName();

        XmlParser xml_parser = new XmlParser(context_path,
                Helpers.concat_path(meta_program_folder_abs_path, Const.DAO_XSD));

        for (String file_path : dao_file_path_list) {

            String xml_file_abs_path = Helpers.concat_path(meta_program_folder_abs_path, file_path);

            Set<String> dao_tables = find_tables_in_use(xml_parser, xml_file_abs_path);

            for (String t : dao_tables) {

                res.add(t);
            }

        }
        return res;
    }

    private static Set<String> find_tables_in_use(XmlParser xml_parser, String xml_file_abs_path) throws Exception {

        Set<String> res = new HashSet<String>();

        DaoClass dao_class = xml_parser.unmarshal(xml_file_abs_path);

        if (dao_class.getCrudOrCrudAutoOrQuery() != null) {

            for (int i = 0; i < dao_class.getCrudOrCrudAutoOrQuery().size(); i++) {

                Object element = dao_class.getCrudOrCrudAutoOrQuery().get(i);

                if (element instanceof Crud) {

                    Crud c = (Crud) element;

                    res.add(c.getTable());

                } else if (element instanceof CrudAuto) {

                    CrudAuto c = (CrudAuto) element;

                    res.add(c.getTable());
                }
            }
        }

        return res;
    }

    public static void get_crud_dao_xml(final Project project, final VirtualFile root_file) {

        ISelectDbSchemaCallback callback = new ISelectDbSchemaCallback() {

            @Override
            public void process_ok(String selected_schema, boolean skip_used, boolean include_views,
                                   boolean plural_to_singular, boolean crud_auto, boolean add_fk_access) {

                try {

                    com.sqldalmaker.jaxb.dao.ObjectFactory object_factory = new com.sqldalmaker.jaxb.dao.ObjectFactory();

                    DaoClass root = object_factory.createDaoClass();

                    Settings settings = IdeaHelpers.load_settings(root_file);

                    Connection conn = IdeaHelpers.get_connection(project, settings);

                    try {

                        // !!!! after 'try'
                        Set<String> in_use;

                        if (skip_used) {

                            in_use = find_tables_used_in_dao_xml(root_file);

                        } else {

                            in_use = new HashSet<String>();
                        }

                        List<Object> nodes = root.getCrudOrCrudAutoOrQuery();

                        DatabaseMetaData db_info = conn.getMetaData();

                        ResultSet rs = DbUtils.get_tables(conn, db_info, selected_schema, include_views);

                        try {

                            while (rs.next()) {

                                // http://docs.oracle.com/javase/1.4.2/docs/api/java/sql/DatabaseMetaData.html

                                try {

                                    String table_type = rs.getString("TABLE_TYPE");

                                    if (!("TABLE".equalsIgnoreCase(table_type)
                                            || "VIEW".equalsIgnoreCase(table_type))) {

                                        continue;
                                    }

                                    String table_name = rs.getString("TABLE_NAME");

                                    if (!in_use.contains(table_name)) {

                                        String dto_class_name = IdeaHelpers.table_name_to_dto_class_name(
                                                table_name, plural_to_singular);

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
                                                if (IdeaTargetLanguageHelpers.underscores_needed(root_file)) {
                                                    m = Helpers.camel_case_to_lower_under_scores(m);
                                                }
                                                tm.setMethod(m);
                                                crud.setCreate(tm);
                                            }
                                            {
                                                TypeMethod tm = new TypeMethod();
                                                String m = "read" + dto_class_name + "List";
                                                if (IdeaTargetLanguageHelpers.underscores_needed(root_file)) {
                                                    m = Helpers.camel_case_to_lower_under_scores(m);
                                                }
                                                tm.setMethod(m);
                                                crud.setReadAll(tm);
                                            }
                                            {
                                                TypeMethod tm = new TypeMethod();
                                                String m = "read" + dto_class_name;
                                                if (IdeaTargetLanguageHelpers.underscores_needed(root_file)) {
                                                    m = Helpers.camel_case_to_lower_under_scores(m);
                                                }
                                                tm.setMethod(m);
                                                crud.setRead(tm);
                                            }
                                            {
                                                TypeMethod tm = new TypeMethod();
                                                String m = "update" + dto_class_name;
                                                if (IdeaTargetLanguageHelpers.underscores_needed(root_file)) {
                                                    m = Helpers.camel_case_to_lower_under_scores(m);
                                                }
                                                tm.setMethod(m);
                                                crud.setUpdate(tm);
                                            }
                                            {
                                                TypeMethod tm = new TypeMethod();
                                                String m = "delete" + dto_class_name;
                                                if (IdeaTargetLanguageHelpers.underscores_needed(root_file)) {
                                                    m = Helpers.camel_case_to_lower_under_scores(m);
                                                }
                                                tm.setMethod(m);
                                                crud.setDelete(tm);
                                            }
                                            nodes.add(crud);
                                        }

                                        if (add_fk_access) {

                                            add_fk_access(root_file, conn, selected_schema,
                                                    db_info, rs, nodes, plural_to_singular);
                                        }

                                    }

                                } catch (Throwable e) {

                                    // just skip
                                }
                            }

                        } finally {

                            rs.close();
                        }

                    } finally {

                        conn.close();
                    }

                    open_dao_xml_in_editor(project, object_factory, "crud-dao.xml", root);

                } catch (Exception e) {

                    e.printStackTrace();

                    IdeaMessageHelpers.show_error_in_ui_thread(e);
                }
            }
        };

        try {

            UIDialogSelectDbSchema.open(project, root_file, callback, false, false);

        } catch (Exception e) {

            e.printStackTrace();

            IdeaMessageHelpers.show_error_in_ui_thread(e);
        }
    }

    private static void add_fk_access(final VirtualFile root_file, Connection conn, String selected_schema,
                                      DatabaseMetaData db_info, ResultSet rs_tables, List<Object> nodes,
                                      boolean plural_to_singular)
            throws SQLException {

        try {

            String table_type = rs_tables.getString("TABLE_TYPE");

            if (!("TABLE".equalsIgnoreCase(table_type) /*
             * || "VIEW". equalsIgnoreCase( tableType)
             */)) {
                return;
            }

        } catch (Throwable e) {

        }

        String fk_table_name = rs_tables.getString("TABLE_NAME");

        ResultSet rs = db_info.getImportedKeys(conn.getCatalog(), selected_schema, fk_table_name);

        // [pk_table_name - list of fk_column_name]

        HashMap<String, List<String>> map = new HashMap<String, List<String>>();

        try {

            while (rs.next()) {

                String pk_table_name = rs.getString("PKTABLE_NAME");

                String fk_column_name = rs.getString("FKCOLUMN_NAME");

                if (!map.containsKey(pk_table_name)) {

                    map.put(pk_table_name, new ArrayList<String>());
                }

                map.get(pk_table_name).add(fk_column_name);

                // int fkSequence = rs.getInt("KEY_SEQ");
                //
                // System.out.print(fk_table_name);
                // System.out.print("\t" + fk_column_name);
                // System.out.println("\t" + fkSequence);
            }

        } finally {

            rs.close();
        }

        for (String pk_table_name : map.keySet()) {

            List<String> fk_column_names = map.get(pk_table_name);

            String params = "";

            String columns = "";

            boolean first = true;

            for (String fk_column_name : fk_column_names) {

                String c = fk_column_name;

                // if (IdeaTargetLanguageHelpers.underscores_needed(editor2)) {
                c = Helpers.camel_case_to_lower_under_scores(c);
                // }

                if (first) {

                    params += c;

                    columns += fk_column_name;

                } else {

                    params += ", " + c;

                    columns += ", " + fk_column_name;
                }

                first = false;
            }

            String dto_class_name = IdeaHelpers.table_name_to_dto_class_name(fk_table_name, plural_to_singular);

            String method_name = "get" + IdeaHelpers.table_name_to_dto_class_name(fk_table_name, false) + "By"
                    + IdeaHelpers.table_name_to_dto_class_name(pk_table_name, true);

            QueryDtoList node = new QueryDtoList();

            node.setDto(dto_class_name);

            if (IdeaTargetLanguageHelpers.underscores_needed(root_file)) {

                method_name = Helpers.camel_case_to_lower_under_scores(method_name);
            }

            String method = method_name + "(" + params.toLowerCase() + ")";

            node.setMethod(method);

            node.setRef(fk_table_name + "(" + columns + ")");

            nodes.add(node);
        }
    }

    public static void get_fk_access_xml(final Project project, final VirtualFile root_file) {

        ISelectDbSchemaCallback callback = new ISelectDbSchemaCallback() {

            @Override
            public void process_ok(String selected_schema, boolean skip_used, boolean include_views,
                                   boolean plural_to_singular, boolean crud_auto, boolean add_fk_access) {

                try {

                    com.sqldalmaker.jaxb.dao.ObjectFactory object_factory = new com.sqldalmaker.jaxb.dao.ObjectFactory();

                    DaoClass root = object_factory.createDaoClass();

                    List<Object> nodes = root.getCrudOrCrudAutoOrQuery();

                    Settings settings = IdeaHelpers.load_settings(root_file);

                    Connection conn = IdeaHelpers.get_connection(project, settings);

                    try {

                        DatabaseMetaData db_info = conn.getMetaData();

                        ResultSet rs = DbUtils.get_tables(conn, db_info, selected_schema, false);

                        try {

                            while (rs.next()) {

                                add_fk_access(root_file, conn, selected_schema, db_info, rs, nodes, plural_to_singular);
                            }

                        } finally {

                            rs.close();
                        }

                    } finally {

                        conn.close();
                    }

                    open_dao_xml_in_editor(project, object_factory, "fk-dao.xml", root);

                } catch (Exception e) {

                    e.printStackTrace();

                    IdeaMessageHelpers.show_error_in_ui_thread(e);
                }
            }
        };

        try {

            UIDialogSelectDbSchema.open(project, root_file, callback, false, true);

        } catch (Exception e) {

            e.printStackTrace();

            IdeaMessageHelpers.show_error_in_ui_thread(e);
        }
    }

    private static void open_dao_xml_in_editor(Project project,
                                               com.sqldalmaker.jaxb.dao.ObjectFactory object_factory,
                                               String file_name, DaoClass root) throws Exception {

        String text = XmlHelpers.get_dao_xml_text(object_factory, root, true);

        IdeaEditorHelpers.open_text_in_new_editor(project, file_name, text);
    }
}