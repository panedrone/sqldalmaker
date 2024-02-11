/*
    Copyright 2011-2023 sqldalmaker@gmail.com
    SQL DAL Maker Website: http://sqldalmaker.sourceforge.net
    Read LICENSE.txt in the root of this project/archive for details.
 */
package com.sqldalmaker.netbeans;

import com.sqldalmaker.cg.Helpers;
import com.sqldalmaker.cg.IDaoCG;
import com.sqldalmaker.cg.IDtoCG;
import com.sqldalmaker.common.Const;
import com.sqldalmaker.common.FileSearchHelpers;
import com.sqldalmaker.common.SdmUtils;
import com.sqldalmaker.common.XmlParser;
import com.sqldalmaker.jaxb.sdm.DaoClass;
import com.sqldalmaker.jaxb.sdm.DtoClass;
import com.sqldalmaker.jaxb.settings.Settings;
import java.sql.Connection;
import java.util.List;
import org.openide.filesystems.FileObject;
import org.openide.util.Exceptions;
import org.openide.util.RequestProcessor;

/*
 * 18.12.2023 02:47 1.292
 * 16.08.2023 03:03 1.285
 * 23.02.2023 15:42 1.279
 * 30.10.2022 08:03 1.266
 * 08.05.2021 22:29 1.200
 * 
 */
public class NbpCG {

    public static void generate_all_dto(final SdmDataObject root_data_object, final FileObject xml_file, final String xml_file_title) {
        String name_ext = xml_file.getNameExt();
        if (!FileSearchHelpers.is_sdm_xml(name_ext)) {
            return;
        }
        final FileObject xml_mp_dir = xml_file.getParent();
        if (xml_mp_dir == null) {
            return;
        }
        final String xml_metaprogram_abs_path = xml_mp_dir.getPath();
        final Settings settings;
        try {
            settings = SdmUtils.load_settings(xml_mp_dir.getPath());
        } catch (Exception ex) {
            Exceptions.printStackTrace(ex);
            return;
        }
        final NbpIdeConsoleUtil ide_log = new NbpIdeConsoleUtil(settings, root_data_object);
        RequestProcessor RP = new RequestProcessor("Generate DTO classes RP");
        // final ProgressHandle ph = ProgressHandle.createHandle("Generate DTO class(es)");
        RequestProcessor.Task task = RP.create(new Runnable() {
            @Override
            public void run() {
                try {
                    StringBuilder output_dir = new StringBuilder();
                    Connection conn = NbpHelpers.get_connection(root_data_object);
                    try {
                        IDtoCG gen = NbpTargetLanguageHelpers.create_dto_cg(conn, root_data_object, settings, output_dir);
                        String output_dir_rel_path = output_dir.toString();
                        String sdm_xml_abs_path = xml_file.getPath();
                        String sdm_xsd_abs_path = Helpers.concat_path(xml_metaprogram_abs_path, Const.SDM_XSD);
                        List<DtoClass> dto_classes = SdmUtils.get_dto_classes(sdm_xml_abs_path, sdm_xsd_abs_path);
                        boolean error = false;
                        for (DtoClass cls : dto_classes) {
                            try {
                                String[] file_content = gen.translate(cls.getName());
                                String file_name = NbpTargetLanguageHelpers.get_target_file_name(root_data_object, cls.getName());
                                NbpHelpers.save_text_to_file(root_data_object, output_dir_rel_path, file_name, file_content[0]);
                            } catch (Exception e) {
                                // Exceptions.printStackTrace(e); // === panedrone: it shows banner!!!
                                error = true;
                                ide_log.add_error_message(e);
                            }
                        }
                        if (!error) {
                            ide_log.add_success_message(xml_file_title + " -> Generate DTO/Model classes...OK");
                        }
                        //ide_log.add_debug_message("DONE");
                    } finally {
                        conn.close();
                    }
                } catch (Exception e) {
                    NbpIdeMessageHelpers.show_error_in_ui_thread(e);
                    // Exceptions.printStackTrace(e); // // === panedrone: it shows banner!!!
                }
            }
        });
        task.schedule(0);
    }

    public static void generate_all_dao(final SdmDataObject root_data_object, final FileObject xml_file, final String xml_file_title) {
        String name_ext = xml_file.getNameExt();
        if (!FileSearchHelpers.is_sdm_xml(name_ext)) {
            return;
        }
        final FileObject xml_mp_dir = xml_file.getParent();
        if (xml_mp_dir == null) {
            return;
        }
        final String sdm_folder_abs_path = xml_mp_dir.getPath();
        final Settings settings;
        try {
            settings = SdmUtils.load_settings(xml_mp_dir.getPath());
        } catch (Exception ex) {
            Exceptions.printStackTrace(ex);
            return;
        }
        final NbpIdeConsoleUtil ide_log = new NbpIdeConsoleUtil(settings, root_data_object);
        RequestProcessor RP = new RequestProcessor("Generate DAO classes RP");
        // final ProgressHandle ph = ProgressHandle.createHandle("Generate DTO class(es)");
        RequestProcessor.Task task = RP.create(new Runnable() {
            @Override
            public void run() {
                try {
                    StringBuilder output_dir = new StringBuilder();
                    Connection conn = NbpHelpers.get_connection(root_data_object);
                    try {
                        IDaoCG gen = NbpTargetLanguageHelpers.create_dao_cg(conn, root_data_object, settings, output_dir);
                        String output_dir_rel_path = output_dir.toString();
                        String sdm_xml_abs_path = xml_file.getPath();
                        String sdm_xsd_abs_path = Helpers.concat_path(sdm_folder_abs_path, Const.SDM_XSD);
                        List<DaoClass> dao_classes = SdmUtils.get_dao_classes(sdm_xml_abs_path, sdm_xsd_abs_path);
                        boolean error = false;
                        for (DaoClass cls : dao_classes) {
                            try {
                                String[] file_content = gen.translate(cls.getName(), cls);
                                String file_name = NbpTargetLanguageHelpers.get_target_file_name(root_data_object, cls.getName());
                                NbpHelpers.save_text_to_file(root_data_object, output_dir_rel_path, file_name, file_content[0]);
                            } catch (Exception e) {
                                // Exceptions.printStackTrace(e); // === panedrone: it shows banner!!!
                                error = true;
                                ide_log.add_error_message(e);
                            }
                        }
                        if (!error) {
                            ide_log.add_success_message(xml_file_title + " -> Generate DAO classes...OK");
                        }
                        //ide_log.add_debug_message("DONE");
                    } finally {
                        conn.close();
                    }
                } catch (Exception e) {
                    NbpIdeMessageHelpers.show_error_in_ui_thread(e);
                    // Exceptions.printStackTrace(e); // // === panedrone: it shows banner!!!
                }
            }
        });
        task.schedule(0);
    }

    public static void validate_all_dto(final SdmDataObject root_data_object, final FileObject xml_file, final String xml_file_title) {
        String name_ext = xml_file.getNameExt();
        if (!FileSearchHelpers.is_sdm_xml(name_ext)) {
            return;
        }
        final FileObject xml_mp_dir = xml_file.getParent();
        if (xml_mp_dir == null) {
            return;
        }
        RequestProcessor RP = new RequestProcessor("Validate DTO classes RP");
        // final ProgressHandle ph = ProgressHandle.createHandle("Generate DTO class(es)");
        RequestProcessor.Task task = RP.create(new Runnable() {
            @Override
            public void run() {
                try {
                    Settings settings = SdmUtils.load_settings(xml_mp_dir.getPath());
                    NbpIdeConsoleUtil ide_log = new NbpIdeConsoleUtil(settings, root_data_object);
//                    ide_log.add_debug_message("STARTED...");
                    StringBuilder output_dir = new StringBuilder();
                    String xml_metaprogram_abs_path = xml_mp_dir.getPath();
                    Connection conn = NbpHelpers.get_connection(root_data_object);
                    try {
                        IDtoCG gen = NbpTargetLanguageHelpers.create_dto_cg(conn, root_data_object, settings, output_dir);
                        String sdm_xml_abs_path = xml_file.getPath();
                        String sdm_xsd_abs_path = Helpers.concat_path(xml_metaprogram_abs_path, Const.SDM_XSD);
                        List<DtoClass> dto_classes = SdmUtils.get_dto_classes(sdm_xml_abs_path, sdm_xsd_abs_path);
                        boolean error = false;
                        for (DtoClass cls : dto_classes) {
                            try {
                                //ProgressManager.progress(cls.getName());
                                String[] file_content = gen.translate(cls.getName());
                                StringBuilder validation_buff = new StringBuilder();
                                NbpTargetLanguageHelpers.validate_dto(root_data_object, settings, cls.getName(), file_content, validation_buff);
                                String status = validation_buff.toString();
                                if (status.length() > 0) {
                                    error = true;
                                    ide_log.add_error_message(xml_file_title + " -> DTO class '" + cls.getName() + "'. " + status);
                                }
                            } catch (Exception e) {
                                // Exceptions.printStackTrace(e); // === panedrone: it shows banner!!!
                                error = true;
                                ide_log.add_error_message(e);
                            }
                        }
                        if (!error) {
                            ide_log.add_success_message(xml_file_title + " -> Validate DTO/Model classes...OK");
                        }
                    } finally {
                        conn.close();
//                        ide_log.add_debug_message("COMPLETED.");
                    }
                } catch (Exception e) {
                    NbpIdeMessageHelpers.show_error_in_ui_thread(e);
                    // Exceptions.printStackTrace(e); // // === panedrone: it shows banner!!!
                }
            }
        });
        task.schedule(0);
    }

    public static void validate_all_sdm_dao(final SdmDataObject root_data_object, final FileObject xml_file, final String xml_file_title) {
        String name_ext = xml_file.getNameExt();
        if (!FileSearchHelpers.is_sdm_xml(name_ext)) {
            return;
        }
        final FileObject xml_mp_dir = xml_file.getParent();
        if (xml_mp_dir == null) {
            return;
        }
        RequestProcessor RP = new RequestProcessor("Validate DAO classes RP");
        // final ProgressHandle ph = ProgressHandle.createHandle("Generate DTO class(es)");
        RequestProcessor.Task task = RP.create(new Runnable() {
            @Override
            public void run() {
                try {
                    Settings settings = SdmUtils.load_settings(xml_mp_dir.getPath());
                    NbpIdeConsoleUtil ide_log = new NbpIdeConsoleUtil(settings, root_data_object);
//                    ide_log.add_debug_message("STARTED...");
                    StringBuilder output_dir = new StringBuilder();
                    String xml_metaprogram_abs_path = xml_mp_dir.getPath();
                    Connection conn = NbpHelpers.get_connection(root_data_object);
                    try {
                        IDaoCG gen = NbpTargetLanguageHelpers.create_dao_cg(conn, root_data_object, settings, output_dir);
                        String dto_xml_abs_path = xml_file.getPath();
                        String dto_xsd_abs_path = Helpers.concat_path(xml_metaprogram_abs_path, Const.SDM_XSD);
                        List<DaoClass> dao_classes = SdmUtils.get_dao_classes(dto_xml_abs_path, dto_xsd_abs_path);
                        boolean error = false;
                        for (DaoClass cls : dao_classes) {
                            try {
                                //ProgressManager.progress(cls.getName());
                                String[] file_content = gen.translate(cls.getName(), cls);
                                StringBuilder validation_buff = new StringBuilder();
                                NbpTargetLanguageHelpers.validate_dao(root_data_object, settings, cls.getName(), file_content, validation_buff);
                                String status = validation_buff.toString();
                                if (status.length() > 0) {
                                    error = true;
                                    ide_log.add_error_message(xml_file_title + " -> DAO class '" + cls.getName() + "'. " + status);
                                }
                            } catch (Exception e) {
                                // Exceptions.printStackTrace(e); // === panedrone: it shows banner!!!
                                error = true;
                                ide_log.add_error_message(e);
                            }
                        }
                        if (!error) {
                            ide_log.add_success_message(xml_file_title + " -> Validate DAO classes...OK");
                        }
                    } finally {
                        conn.close();
//                        ide_log.add_debug_message("COMPLETED.");
                    }
                } catch (Exception e) {
                    NbpIdeMessageHelpers.show_error_in_ui_thread(e);
                    // Exceptions.printStackTrace(e); // // === panedrone: it shows banner!!!
                }
            }
        });
        task.schedule(0);
    }

    public static void generate_dao(final SdmDataObject root_data_object, final FileObject xml_file, final String xml_file_title) {
        final String name_ext = xml_file.getNameExt();
        if (!FileSearchHelpers.is_dao_xml(name_ext)) {
            return;
        }
        final FileObject xml_mp_dir = xml_file.getParent();
        if (xml_mp_dir == null) {
            return;
        }
        RequestProcessor RP = new RequestProcessor("Generate DAO class RP");
        // final ProgressHandle ph = ProgressHandle.createHandle("Generate DTO class(es)");
        RequestProcessor.Task task = RP.create(new Runnable() {
            @Override
            public void run() {
                try {
                    Settings settings = SdmUtils.load_settings(xml_mp_dir.getPath());
                    NbpIdeConsoleUtil ide_log = new NbpIdeConsoleUtil(settings, root_data_object);
//                    ide_log.add_debug_message("STARTED...");
                    StringBuilder output_dir = new StringBuilder();
                    String xml_mp_abs_path = xml_mp_dir.getPath();
                    Connection conn = NbpHelpers.get_connection(root_data_object);
                    try {
                        IDaoCG gen = NbpTargetLanguageHelpers.create_dao_cg(conn, root_data_object, settings, output_dir);
                        String output_dir_rel_path = output_dir.toString();
                        boolean error = false;
                        try {
                            String context_path = DaoClass.class.getPackage().getName();
                            XmlParser xml_parser = new XmlParser(context_path, Helpers.concat_path(xml_mp_abs_path, Const.DAO_XSD));
                            String dao_class_name = xml_file.getName();
                            String dao_xml_abs_path = Helpers.concat_path(xml_mp_abs_path, name_ext);
                            DaoClass dao_class = xml_parser.unmarshal(dao_xml_abs_path);
                            String[] file_content = gen.translate(dao_class_name, dao_class);
                            String file_name = NbpTargetLanguageHelpers.get_target_file_name(root_data_object, dao_class_name);
                            NbpHelpers.save_text_to_file(root_data_object, output_dir_rel_path, file_name, file_content[0]);
                        } catch (Exception e) {
                            // Exceptions.printStackTrace(e); // === panedrone: it shows banner!!!
                            error = true;
                            ide_log.add_error_message(e);
                        }
                        if (!error) {
                            ide_log.add_success_message(xml_file_title + " -> Generate DAO class...OK");
                        }
                    } finally {
                        conn.close();
//                        ide_log.add_debug_message("COMPLETED.");
                    }
                } catch (Exception e) {
                    NbpIdeMessageHelpers.show_error_in_ui_thread(e);
                    // Exceptions.printStackTrace(e); // // === panedrone: it shows banner!!!
                }
            }
        });
        task.schedule(0);
    }

    public static void validate_dao(final SdmDataObject root_data_object, final FileObject xml_file, final String xml_file_title) {
        final String name_ext = xml_file.getNameExt();
        if (!FileSearchHelpers.is_dao_xml(name_ext)) {
            return;
        }
        final FileObject xml_mp_dir = xml_file.getParent();
        if (xml_mp_dir == null) {
            return;
        }
        RequestProcessor RP = new RequestProcessor("Validate DAO class RP");
        RequestProcessor.Task task = RP.create(new Runnable() {
            @Override
            public void run() {
                try {
                    Settings settings = SdmUtils.load_settings(xml_mp_dir.getPath());
                    NbpIdeConsoleUtil ide_log = new NbpIdeConsoleUtil(settings, root_data_object);
                    StringBuilder output_dir = new StringBuilder();
                    String xml_mp_abs_path = xml_mp_dir.getPath();
                    Connection conn = NbpHelpers.get_connection(root_data_object);
                    try {
                        IDaoCG gen = NbpTargetLanguageHelpers.create_dao_cg(conn, root_data_object, settings, output_dir);
                        boolean error = false;
                        try {
                            String context_path = DaoClass.class.getPackage().getName();
                            XmlParser xml_parser = new XmlParser(context_path, Helpers.concat_path(xml_mp_abs_path, Const.DAO_XSD));
                            String dao_class_name = xml_file.getName();
                            String dao_xml_abs_path = Helpers.concat_path(xml_mp_abs_path, name_ext);
                            DaoClass dao_class = xml_parser.unmarshal(dao_xml_abs_path);
                            String[] file_content = gen.translate(dao_class_name, dao_class);
                            StringBuilder validation_buff = new StringBuilder();
                            NbpTargetLanguageHelpers.validate_dao(root_data_object, settings, dao_class_name, file_content, validation_buff);
                            String status = validation_buff.toString();
                            if (status.length() > 0) {
                                error = true;
                                ide_log.add_error_message(xml_file_title + " -> " + status);
                            }
                        } catch (Exception e) {
                            // Exceptions.printStackTrace(e); // === panedrone: it shows banner!!!
                            error = true;
                            ide_log.add_error_message(e);
                        }
                        if (!error) {
                            ide_log.add_success_message(xml_file_title + " -> Validate DAO class...OK");
                        }
                    } finally {
                        conn.close();
//                        ide_log.add_debug_message("DONE");
                    }
                } catch (Exception e) {
                    NbpIdeMessageHelpers.show_error_in_ui_thread(e);
                    // Exceptions.printStackTrace(e); // // === panedrone: it shows banner!!!
                }
            }
        });
        task.schedule(0);
    }
}
