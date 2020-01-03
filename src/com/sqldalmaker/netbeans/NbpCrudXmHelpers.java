/*
 * Copyright 2011-2019 sqldalmaker@gmail.com
 * SQL DAL Maker Website: http://sqldalmaker.sourceforge.net
 * Read LICENSE.txt in the root of this project/archive for details.
 *
 */
package com.sqldalmaker.netbeans;

import com.sqldalmaker.cg.DbUtils;
import com.sqldalmaker.cg.Helpers;
import com.sqldalmaker.common.Const;
import com.sqldalmaker.common.FileSearchHelpers;
import com.sqldalmaker.common.XmlParser;
import com.sqldalmaker.jaxb.dao.DaoClass;
import com.sqldalmaker.jaxb.dto.DtoClass;
import com.sqldalmaker.jaxb.dto.DtoClasses;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import com.sqldalmaker.common.ISelectDbSchemaCallback;
import com.sqldalmaker.common.SdmUtils;

/**
 * The class to control DTO XML assistant, DAO XML assistant, FK access XML
 * assistant
 *
 * @author sqldalmaker@gmail.com
 *
 */
public class NbpCrudXmHelpers {

    public static void get_crud_dto_xml(final SdmDataObject obj) throws Exception {

        ISelectDbSchemaCallback callback = new ISelectDbSchemaCallback() {

            @Override
            public void process_ok(boolean schema_in_xml, String selected_schema, boolean skip_used,
                    boolean include_views, boolean plural_to_singular, boolean crud_auto, boolean add_fk_access) {

                try {

                    com.sqldalmaker.jaxb.dto.ObjectFactory object_factory = new com.sqldalmaker.jaxb.dto.ObjectFactory();

                    DtoClasses root;

                    Connection connection = NbpHelpers.get_connection(obj);

                    try {

                        Set<String> in_use;

                        if (skip_used) {

                            in_use = find_tables_used_in_dto_xml(obj);

                        } else {

                            in_use = new HashSet<String>();
                        }

                        root = SdmUtils.get_crud_dto_xml(object_factory, connection, in_use,
                                schema_in_xml, selected_schema, include_views,
                                plural_to_singular);

                    } finally {

                        connection.close();
                    }

                    NbpIdeEditorHelpers.open_dto_in_editor_async(object_factory, root, false);

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

            } else if (DbUtils.is_table_ref(ref)) {

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

        res.addAll(elements.getDtoClass());

        return res;
    }

    public static void get_crud_dao_xml(final SdmDataObject obj) throws Exception {

        ISelectDbSchemaCallback callback = new ISelectDbSchemaCallback() {

            @Override
            public void process_ok(boolean schema_in_xml, String selected_schema, boolean skip_used,
                    boolean include_views, boolean plural_to_singular, boolean use_crud_auto, boolean add_fk_access) {

                try {

                    boolean underscores_needed = NbpTargetLanguageHelpers.underscores_needed(obj);

                    com.sqldalmaker.jaxb.dao.ObjectFactory object_factory = new com.sqldalmaker.jaxb.dao.ObjectFactory();

                    DaoClass root;

                    Connection connection = NbpHelpers.get_connection(obj);

                    try {

                        Set<String> in_use;

                        if (skip_used) {

                            in_use = find_tables_used_in_dao_xml(obj);

                        } else {

                            in_use = new HashSet<String>();
                        }

                        root = SdmUtils.create_crud_xml_DaoClass(object_factory,
                                connection, in_use, schema_in_xml, selected_schema,
                                include_views, use_crud_auto, add_fk_access,
                                plural_to_singular, underscores_needed);

                    } finally {

                        connection.close();
                    }

                    NbpIdeEditorHelpers.open_dao_in_editor_async(object_factory, root);

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

            Set<String> dao_tables = SdmUtils.find_tables_in_use(xml_parser, metaprogram_abs_path + "/" + file_name);

            res.addAll(dao_tables);
        }

        return res;
    }

    public static void get_fk_access_xml(final SdmDataObject obj) throws Exception {

        ISelectDbSchemaCallback callback = new ISelectDbSchemaCallback() {

            @Override
            public void process_ok(boolean schema_in_xml, String selected_schema, boolean skip_used,
                    boolean include_views, boolean plural_to_singular, boolean crud_auto, boolean add_fk_access) {

                try {

                    boolean underscores_needed = NbpTargetLanguageHelpers.underscores_needed(obj);

                    com.sqldalmaker.jaxb.dao.ObjectFactory object_factory = new com.sqldalmaker.jaxb.dao.ObjectFactory();

                    DaoClass root;

                    Connection conn = NbpHelpers.get_connection(obj);

                    try {

                        root = SdmUtils.get_fk_access_xml(conn, object_factory, schema_in_xml, selected_schema, 
                                plural_to_singular,
                                underscores_needed);

                    } finally {

                        conn.close();
                    }

                    NbpIdeEditorHelpers.open_fk_dao_in_editor_async(object_factory, root);

                } catch (Exception e) {

                    NbpIdeMessageHelpers.show_error_in_ui_thread(e);
                }
            }
        };

        UIDialogSelectDbSchema.open(obj, callback, false, true);
    }
}
