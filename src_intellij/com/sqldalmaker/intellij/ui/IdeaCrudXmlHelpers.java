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
import java.util.*;

/**
 * The class to control DTO XML assistant, DAO XML assistant, FK access XML
 * assistant
 * <p>
 * Created with IntelliJ IDEA.
 * User: sqldalmaker@gmail.com
 * Date: 21.06.12
 */
public class IdeaCrudXmlHelpers {

    public static void get_crud_dto_xml(final Project project, final VirtualFile root_file) {

        ISelectDbSchemaCallback callback = new ISelectDbSchemaCallback() {

            @Override
            public void process_ok(boolean schema_in_xml, String selected_schema, boolean skip_used, boolean include_views,
                                   boolean plural_to_singular, boolean crud_auto, boolean add_fk_access) {

                try {

                    Settings settings = IdeaHelpers.load_settings(root_file);

                    com.sqldalmaker.jaxb.dto.ObjectFactory object_factory = new com.sqldalmaker.jaxb.dto.ObjectFactory();

                    DtoClasses root;

                    Connection connection = IdeaHelpers.get_connection(project, settings);

                    try {

                        Set<String> in_use; // !!!! after 'try'

                        if (skip_used) {

                            in_use = find_tables_used_in_dto_xml(root_file);

                        } else {

                            in_use = new HashSet<String>();
                        }

                        root = SdmUtils.get_crud_dto_xml(object_factory, connection, in_use, schema_in_xml, selected_schema, include_views,
                                plural_to_singular);

                    } finally {

                        connection.close();
                    }

                    IdeaEditorHelpers.open_dto_xml_in_editor(object_factory, project, root, false);

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

    private static Set<String> find_tables_used_in_dto_xml(VirtualFile root_file) throws Exception {

        Set<String> res = new HashSet<String>();

        String xml_configs_folder_full_path = root_file.getParent().getPath();

        String dto_xml_abs_path = xml_configs_folder_full_path + "/" + Const.DTO_XML;

        String dto_xsd_abs_path = xml_configs_folder_full_path + "/" + Const.DTO_XSD;

        List<DtoClass> list = SdmUtils.get_dto_classes(dto_xml_abs_path, dto_xsd_abs_path);

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

            Set<String> dao_tables = SdmUtils.find_tables_in_use(xml_parser, xml_file_abs_path);

            res.addAll(dao_tables);
        }

        return res;
    }

    public static void get_crud_dao_xml(final Project project, final VirtualFile root_file) {

        ISelectDbSchemaCallback callback = new ISelectDbSchemaCallback() {

            @Override
            public void process_ok(boolean schema_in_xml, String selected_schema, boolean skip_used, boolean include_views,
                                   boolean plural_to_singular, boolean crud_auto, boolean add_fk_access) {

                try {

                    boolean underscores_needed = IdeaTargetLanguageHelpers.underscores_needed(root_file);

                    Settings settings = IdeaHelpers.load_settings(root_file);

                    Connection connection = IdeaHelpers.get_connection(project, settings);

                    com.sqldalmaker.jaxb.dao.ObjectFactory object_factory = new com.sqldalmaker.jaxb.dao.ObjectFactory();

                    DaoClass root;

                    try {

                        Set<String> in_use; // !!!! after 'try'

                        if (skip_used) {

                            in_use = find_tables_used_in_dao_xml(root_file);

                        } else {

                            in_use = new HashSet<String>();
                        }

                        root = SdmUtils.create_crud_xml_DaoClass(object_factory,
                                connection, in_use, schema_in_xml, selected_schema,
                                include_views, crud_auto, add_fk_access,
                                plural_to_singular, underscores_needed);

                    } finally {

                        connection.close();
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

    public static void get_fk_access_xml(final Project project, final VirtualFile root_file) {

        ISelectDbSchemaCallback callback = new ISelectDbSchemaCallback() {

            @Override
            public void process_ok(boolean schema_in_xml, String selected_schema, boolean skip_used, boolean include_views,
                                   boolean plural_to_singular, boolean crud_auto, boolean add_fk_access) {

                try {

                    boolean underscores_needed = IdeaTargetLanguageHelpers.underscores_needed(root_file);

                    com.sqldalmaker.jaxb.dao.ObjectFactory object_factory = new com.sqldalmaker.jaxb.dao.ObjectFactory();

                    DaoClass root;

                    Settings settings = IdeaHelpers.load_settings(root_file);

                    Connection connection = IdeaHelpers.get_connection(project, settings);

                    try {

                        root = SdmUtils.get_fk_access_xml(connection, object_factory, schema_in_xml, selected_schema, plural_to_singular,
                                underscores_needed);

                    } finally {

                        connection.close();
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