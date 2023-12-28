/*
    Copyright 2011-2023 sqldalmaker@gmail.com
    SQL DAL Maker Website: https://sqldalmaker.sourceforge.net/
    Read LICENSE.txt in the root of this project/archive for details.
 */
package com.sqldalmaker.intellij.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.sqldalmaker.cg.FieldNamesMode;
import com.sqldalmaker.cg.Helpers;
import com.sqldalmaker.common.*;
import com.sqldalmaker.jaxb.sdm.DaoClass;
import com.sqldalmaker.jaxb.sdm.Sdm;
import com.sqldalmaker.jaxb.settings.Settings;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * The class to control
 * <p>
 * - DTO XML assistant
 * - DAO XML assistant
 * - FK access XML assistant
 * <p>
 * Created with IntelliJ IDEA.
 * User: sqldalmaker@gmail.com
 * Date: 21.06.12
 */
public class IdeaCrudXmlHelpers {

    public static void get_crud_sdm_xml(final Project project, final VirtualFile root_file) {

        ISelectDbSchemaCallback callback = new ISelectDbSchemaCallback() {
            @Override
            public void process_ok(boolean schema_in_xml, String selected_schema, boolean skip_used, boolean include_views,
                                   boolean plural_to_singular, boolean crud_auto, boolean add_fk_access) {
                try {
                    Settings settings = IdeaHelpers.load_settings(root_file);
                    com.sqldalmaker.jaxb.sdm.ObjectFactory object_factory = new com.sqldalmaker.jaxb.sdm.ObjectFactory();
                    Sdm root;
                    Connection connection = IdeaHelpers.get_connection(project, settings);
                    try {
                        Set<String> in_use; // !!!! after 'try'
                        if (skip_used) {
                            in_use = find_dto_declared_in_sdm_xml(root_file);
                        } else {
                            in_use = new HashSet<String>();
                        }
                        root = SdmUtils.get_crud_sdm_xml(object_factory, connection, in_use, schema_in_xml, selected_schema, include_views,
                                plural_to_singular);
                    } finally {
                        connection.close();
                    }
                    IdeaEditorHelpers.open_sdm_xml_in_editor(object_factory, project, root);
                } catch (Exception e) {
                    // e.printStackTrace();
                    IdeaMessageHelpers.show_error_in_ui_thread(e);
                }
            }
        };
        try {
            UIDialogSelectDbSchema.open(project, root_file, callback, true, false);
        } catch (Exception e) {
            // e.printStackTrace();
            IdeaMessageHelpers.show_error_in_ui_thread(e);
        }
    }

    private static Set<String> find_dto_declared_in_sdm_xml(VirtualFile root_file) throws Exception {

        String xml_configs_folder_full_path = root_file.getParent().getPath();
        String sdm_xml_abs_file_path = Helpers.concat_path(xml_configs_folder_full_path, Const.SDM_XML);
        String sdm_xsd_abs_file_path = Helpers.concat_path(xml_configs_folder_full_path, Const.SDM_XSD);
        Set<String> res = SdmUtils.get_dto_class_names_used_in_sdm_xml(sdm_xml_abs_file_path, sdm_xsd_abs_file_path);
        return res;
    }

    private static ArrayList<String> fill_dao_file_path_list(VirtualFile root_file) {

        final ArrayList<String> res = new ArrayList<String>();
        FileSearchHelpers.IFileList file_list = new FileSearchHelpers.IFileList() {
            @Override
            public void add(String file_path) {
                res.add(file_path);
            }
        };
        String xml_configs_folder_full_path = root_file.getParent().getPath();
        FileSearchHelpers.enum_dao_xml_file_names(xml_configs_folder_full_path, file_list);
        return res;
    }

    private static Set<String> find_dto_used_in_dao_xml_crud(VirtualFile root_file) throws Exception {

        ArrayList<String> dao_xml_file_name_list = fill_dao_file_path_list(root_file);
        String meta_program_folder_abs_path = root_file.getParent().getPath();
        Set<String> res = SdmUtils.find_dto_used_in_dao_xml_crud(meta_program_folder_abs_path, dao_xml_file_name_list);
        return res;
    }

    private static FieldNamesMode get_field_names_mode(VirtualFile root_file, Settings settings) {
        boolean force_snake_case = IdeaTargetLanguageHelpers.snake_case_needed(root_file);
        FieldNamesMode field_names_mode;
        if (force_snake_case) {
            field_names_mode = FieldNamesMode.SNAKE_CASE;
        } else {
            boolean force_lower_camel_case = IdeaTargetLanguageHelpers.lower_camel_case_needed(root_file);
            if (force_lower_camel_case) {
                field_names_mode = FieldNamesMode.LOWER_CAMEL_CASE;
            } else {
                int fnm = settings.getDto().getFieldNamesMode();
                if (fnm == 1) {
                    field_names_mode = FieldNamesMode.LOWER_CAMEL_CASE;
                } else {
                    field_names_mode = FieldNamesMode.SNAKE_CASE;
                }
            }
        }
        return field_names_mode;
    }

    public static void get_crud_dao_xml(final Project project, final VirtualFile root_file) {

        ISelectDbSchemaCallback callback = new ISelectDbSchemaCallback() {
            @Override
            public void process_ok(boolean schema_in_xml, String selected_schema, boolean skip_used, boolean include_views,
                                   boolean plural_to_singular, boolean crud_auto, boolean add_fk_access) {
                try {
                    Settings settings = IdeaHelpers.load_settings(root_file);
                    FieldNamesMode field_names_mode = get_field_names_mode(root_file, settings);
                    Connection connection = IdeaHelpers.get_connection(project, settings);
                    com.sqldalmaker.jaxb.sdm.ObjectFactory object_factory = new com.sqldalmaker.jaxb.sdm.ObjectFactory();
                    DaoClass root;
                    try {
                        Set<String> in_use; // !!!! after 'try'
                        if (skip_used) {
                            in_use = find_dto_used_in_dao_xml_crud(root_file);
                        } else {
                            in_use = new HashSet<String>();
                        }
                        root = SdmUtils.create_crud_xml_jaxb_dao_class(object_factory,
                                connection, in_use, schema_in_xml, selected_schema,
                                include_views, crud_auto, add_fk_access,
                                plural_to_singular, field_names_mode);
                    } finally {
                        connection.close();
                    }
                    IdeaEditorHelpers.open_dao_xml_in_editor(project, object_factory, "crud-dao.xml", root);
                } catch (Exception e) {
                    // e.printStackTrace();
                    IdeaMessageHelpers.show_error_in_ui_thread(e);
                }
            }
        };
        try {
            UIDialogSelectDbSchema.open(project, root_file, callback, false, false);
        } catch (Exception e) {
            // e.printStackTrace();
            IdeaMessageHelpers.show_error_in_ui_thread(e);
        }
    }

    public static void get_fk_access_xml(final Project project, final VirtualFile root_file) {

        ISelectDbSchemaCallback callback = new ISelectDbSchemaCallback() {
            @Override
            public void process_ok(boolean schema_in_xml, String selected_schema, boolean skip_used, boolean include_views,
                                   boolean plural_to_singular, boolean crud_auto, boolean add_fk_access) {
                try {
                    com.sqldalmaker.jaxb.sdm.ObjectFactory object_factory = new com.sqldalmaker.jaxb.sdm.ObjectFactory();
                    DaoClass root;
                    Settings settings = IdeaHelpers.load_settings(root_file);
                    FieldNamesMode field_names_mode = get_field_names_mode(root_file, settings);
                    Connection connection = IdeaHelpers.get_connection(project, settings);
                    try {
                        root = SdmUtils.get_fk_access_xml(connection, object_factory, schema_in_xml, selected_schema, plural_to_singular,
                                field_names_mode);
                    } finally {
                        connection.close();
                    }
                    IdeaEditorHelpers.open_dao_xml_in_editor(project, object_factory, "fk-dao.xml", root);
                } catch (Exception e) {
                    // e.printStackTrace();
                    IdeaMessageHelpers.show_error_in_ui_thread(e);
                }
            }
        };
        try {
            UIDialogSelectDbSchema.open(project, root_file, callback, false, true);
        } catch (Exception e) {
            // e.printStackTrace();
            IdeaMessageHelpers.show_error_in_ui_thread(e);
        }
    }
}