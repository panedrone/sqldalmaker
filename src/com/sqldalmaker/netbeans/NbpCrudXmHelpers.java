/*
    Copyright 2011-2023 sqldalmaker@gmail.com
    SQL DAL Maker Website: http://sqldalmaker.sourceforge.net
    Read LICENSE.txt in the root of this project/archive for details.
 */
package com.sqldalmaker.netbeans;

import com.sqldalmaker.cg.FieldNamesMode;
import com.sqldalmaker.common.FileSearchHelpers;
import com.sqldalmaker.jaxb.sdm.DaoClass;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import com.sqldalmaker.common.ISelectDbSchemaCallback;
import com.sqldalmaker.common.SdmUtils;
import com.sqldalmaker.jaxb.sdm.Sdm;
import com.sqldalmaker.jaxb.settings.Settings;
import java.util.List;

/**
 * The class to control
 *
 * - DTO XML assistant - DAO XML assistant - FK access XML assistant
 *
 * @author sqldalmaker@gmail.com
 *
 */
public class NbpCrudXmHelpers {

    public static void get_crud_dto_xml(final SdmDataObject obj) throws Exception {
        ISelectDbSchemaCallback callback = new ISelectDbSchemaCallback() {
            @Override
            public void process_ok(boolean schema_in_xml, String selected_schema, boolean omit_used,
                    boolean include_views, boolean plural_to_singular, boolean crud_auto, boolean add_fk_access) {
                try {
                    com.sqldalmaker.jaxb.sdm.ObjectFactory object_factory = new com.sqldalmaker.jaxb.sdm.ObjectFactory();
                    Sdm root;
                    Connection connection = NbpHelpers.get_connection(obj);
                    try {
                        Set<String> in_use;
                        if (omit_used) {
                            in_use = find_dto_declared_in_dto_xml(obj);
                        } else {
                            in_use = new HashSet<String>();
                        }
                        root = SdmUtils.get_crud_sdm_xml(object_factory, connection, in_use,
                                schema_in_xml, selected_schema, include_views, plural_to_singular);
                    } finally {
                        connection.close();
                    }
                    NbpIdeEditorHelpers.open_dto_in_editor_async(object_factory, root);
                } catch (Exception e) {
                    NbpIdeMessageHelpers.show_error_in_ui_thread(e);
                }
            }
        };
        UIDialogSelectDbSchema.open(obj, callback, true, false);
    }

    private static Set<String> find_dto_declared_in_dto_xml(SdmDataObject obj) throws Exception {
        String dto_xml_abs_file_path = NbpPathHelpers.get_dto_xml_abs_path(obj);
        String dto_xsd_abs_file_path = NbpPathHelpers.get_dto_xsd_abs_path(obj);
        Set<String> res = SdmUtils.get_dto_class_names_used_in_sdm_xml(dto_xml_abs_file_path, dto_xsd_abs_file_path);
        return res;
    }

    public static void get_crud_dao_xml(final SdmDataObject obj) throws Exception {
        ISelectDbSchemaCallback callback = new ISelectDbSchemaCallback() {
            @Override
            public void process_ok(boolean schema_in_xml, String selected_schema, boolean omit_used,
                    boolean include_views, boolean plural_to_singular, boolean use_crud_auto, boolean add_fk_access) {
                try {
                    Settings settings = NbpHelpers.load_settings(obj);
                    FieldNamesMode field_names_mode = get_field_names_mode(obj, settings);
                    com.sqldalmaker.jaxb.sdm.ObjectFactory object_factory = new com.sqldalmaker.jaxb.sdm.ObjectFactory();
                    DaoClass root;
                    Connection connection = NbpHelpers.get_connection(obj);
                    try {
                        Set<String> in_use;
                        if (omit_used) {
                            in_use = find_dto_used_in_dao_xml_crud(obj);
                        } else {
                            in_use = new HashSet<String>();
                        }
                        root = SdmUtils.create_crud_xml_jaxb_dao_class(object_factory, connection, in_use,
                                schema_in_xml, selected_schema, include_views, use_crud_auto, add_fk_access,
                                plural_to_singular, field_names_mode);
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
        FileSearchHelpers.IFileList file_list = new FileSearchHelpers.IFileList() {
            @Override
            public void add(String file_name) {
                res.add(file_name);
            }
        };
        String xml_configs_folder_abs_path = NbpPathHelpers.get_metaprogram_abs_path(obj);
        FileSearchHelpers.enum_dao_xml_file_names(xml_configs_folder_abs_path, file_list);
        return res;
    }

    private static Set<String> find_dto_used_in_dao_xml_crud(SdmDataObject obj) throws Exception {
        String metaprogram_abs_path = NbpPathHelpers.get_metaprogram_abs_path(obj);
        List<String> dao_xml_file_name_list = fill_dao_file_path_list(obj);
        Set<String> res = SdmUtils.find_dto_used_in_dao_xml_crud(metaprogram_abs_path, dao_xml_file_name_list);
        return res;
    }

    private static FieldNamesMode get_field_names_mode(SdmDataObject obj, Settings settings) {
        boolean force_snake_case = NbpTargetLanguageHelpers.snake_case_needed(obj);
        FieldNamesMode field_names_mode;
        if (force_snake_case) {
            field_names_mode = FieldNamesMode.SNAKE_CASE;
        } else {
            boolean force_lower_camel_case = NbpTargetLanguageHelpers.lower_camel_case_needed(obj);
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
    
    public static void get_fk_access_xml(final SdmDataObject obj) throws Exception {
        ISelectDbSchemaCallback callback = new ISelectDbSchemaCallback() {
            @Override
            public void process_ok(boolean schema_in_xml, String selected_schema, boolean omit_used,
                    boolean include_views, boolean plural_to_singular, boolean crud_auto, boolean add_fk_access) {
                try {
                    Settings settings = NbpHelpers.load_settings(obj);
                    FieldNamesMode field_names_mode = get_field_names_mode(obj, settings);
                    com.sqldalmaker.jaxb.sdm.ObjectFactory object_factory = new com.sqldalmaker.jaxb.sdm.ObjectFactory();
                    DaoClass root;
                    Connection conn = NbpHelpers.get_connection(obj);
                    try {
                        root = SdmUtils.get_fk_access_xml(conn, object_factory, schema_in_xml, selected_schema,
                                plural_to_singular, field_names_mode);
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
