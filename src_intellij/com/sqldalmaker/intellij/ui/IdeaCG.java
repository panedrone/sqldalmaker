package com.sqldalmaker.intellij.ui;

import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.sqldalmaker.cg.Helpers;
import com.sqldalmaker.cg.IDaoCG;
import com.sqldalmaker.cg.IDtoCG;
import com.sqldalmaker.common.*;
import com.sqldalmaker.jaxb.dao.DaoClass;
import com.sqldalmaker.jaxb.dto.DtoClass;
import com.sqldalmaker.jaxb.settings.Settings;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

public class IdeaCG {

    public static void validate_all_dto(Project project, VirtualFile xml_file) {
        String name = xml_file.getName();
        if (!FileSearchHelpers.is_dto_xml(name)) {
            return;
        }
        VirtualFile xml_file_dir = xml_file.getParent();
        if (xml_file_dir == null) {
            return;
        }
        List<VirtualFile> root_files = IdeaTargetLanguageHelpers.find_root_files(xml_file_dir);
        if (root_files.size() == 0) {
            IdeaMessageHelpers.add_error_to_ide_log("ERROR", "Root-file not found");
            return;
        }
        VirtualFile root_file = root_files.get(0);
        if (root_files.size() > 1) {
            IdeaMessageHelpers.add_warning_to_ide_log("Several Root-files found. Selected " + root_file.getName());
        }
        final Runnable runnable = new Runnable() {
            @Override
            public void run() {
                try {
                    Settings settings = SdmUtils.load_settings(xml_file_dir.getPath());
                    StringBuilder output_dir = new StringBuilder();
                    String xml_metaprogram_abs_path = root_file.getParent().getPath();
                    Connection con = IdeaHelpers.get_connection(project, settings);
                    try {
                        IDtoCG gen = IdeaTargetLanguageHelpers.create_dto_cg(con, project, root_file, settings, output_dir);
                        String dto_xml_abs_path = xml_file.getPath();
                        String dto_xsd_abs_path = xml_metaprogram_abs_path + "/" + Const.DTO_XSD;
                        List<DtoClass> dto_classes = SdmUtils.get_dto_classes(dto_xml_abs_path, dto_xsd_abs_path);
                        boolean error = false;
                        for (DtoClass cls : dto_classes) {
                            try {
                                ProgressManager.progress(cls.getName());
                                String[] file_content = gen.translate(cls.getName());
                                StringBuilder validationBuff = new StringBuilder();
                                IdeaTargetLanguageHelpers.validate_dto(project, root_file, settings, cls.getName(), file_content, validationBuff);
                                String status = validationBuff.toString();
                                if (status.length() > 0) {
                                    error = true;
                                    IdeaMessageHelpers.add_error_to_ide_log("ERROR", " DTO class '" + cls.getName() + "'. " + status);
                                }
                            } catch (Throwable e) {
                                error = true;
                                String msg = e.getMessage();
                                IdeaMessageHelpers.add_error_to_ide_log("ERROR", msg);
                            }
                        }
                        if (!error) {
                            String xml_file_rel_path = IdeaHelpers.get_relative_path(project, xml_file);
                            IdeaMessageHelpers.add_info_to_ide_log(xml_file_rel_path + " -> OK");
                        }
                    } finally {
                        con.close();
                    }
                } catch (Exception e) {
                    IdeaMessageHelpers.add_error_to_ide_log("ERROR", e.getMessage());
                    e.printStackTrace();
                }
            }
        };
        ProgressManager.getInstance().runProcessWithProgressSynchronously(runnable, "Code validation", false, project);
    }

    public static void generate_all_dto(Project project, VirtualFile xml_file) {
        String name = xml_file.getName();
        if (!FileSearchHelpers.is_dto_xml(name)) {
            return;
        }
        VirtualFile xml_file_dir = xml_file.getParent();
        if (xml_file_dir == null) {
            return;
        }
        List<VirtualFile> root_files = IdeaTargetLanguageHelpers.find_root_files(xml_file_dir);
        if (root_files.size() == 0) {
            IdeaMessageHelpers.add_error_to_ide_log("ERROR", "Root-file not found");
            return;
        }
        VirtualFile root_file = root_files.get(0);
        if (root_files.size() > 1) {
            IdeaMessageHelpers.add_warning_to_ide_log("Several Root-files found. Selected " + root_file.getName());
        }
        List<IdeaHelpers.GeneratedFileData> list = new ArrayList<IdeaHelpers.GeneratedFileData>();
        StringBuilder output_dir = new StringBuilder();
        class Error {
            public boolean happend = false;
        }
        Error error = new Error();
        final Runnable runnable = new Runnable() {
            @Override
            public void run() {
                try {
                    Settings settings = SdmUtils.load_settings(xml_file_dir.getPath());
                    String xml_mp_abs_path = root_file.getParent().getPath();
                    Connection con = IdeaHelpers.get_connection(project, settings);
                    try {
                        IDtoCG gen = IdeaTargetLanguageHelpers.create_dto_cg(con, project, root_file, settings, output_dir);
                        try {
                            String dto_xml_abs_path = xml_file.getPath();
                            String dto_xsd_abs_path = xml_mp_abs_path + "/" + Const.DTO_XSD;
                            List<DtoClass> dto_classes = SdmUtils.get_dto_classes(dto_xml_abs_path, dto_xsd_abs_path);
                            for (DtoClass cls : dto_classes) {
                                ProgressManager.progress(cls.getName());
                                String[] file_content = gen.translate(cls.getName());
                                IdeaTargetLanguageHelpers.prepare_generated_file_data(root_file, cls.getName(), file_content, list);
                            }
                        } catch (Throwable e) {
                            String msg = e.getMessage();
                            IdeaMessageHelpers.add_error_to_ide_log("ERROR", msg);
                            error.happend = true;
                        }
                    } finally {
                        con.close();
                    }
                } catch (Exception e) {
                    IdeaMessageHelpers.add_error_to_ide_log("ERROR", e.getMessage());
                    e.printStackTrace();
                }
            }
        };
        ProgressManager.getInstance().runProcessWithProgressSynchronously(runnable, "Code generation", false, project);
        try { // outside of Runnable
            if (list.size() > 0) {
                String xml_file_rel_path = IdeaHelpers.get_relative_path(project, xml_file);
                IdeaHelpers.run_write_action_to_generate_source_file(output_dir.toString(), list, project);
                if (!error.happend) {
                    IdeaMessageHelpers.add_info_to_ide_log(xml_file_rel_path + " -> Generated successfully");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            IdeaMessageHelpers.show_error_in_ui_thread(e);
        }
    }

    public static void validate_dao(Project project, VirtualFile xml_file) {
        String name = xml_file.getName();
        if (!FileSearchHelpers.is_dao_xml(name)) {
            return;
        }
        VirtualFile xml_file_dir = xml_file.getParent();
        if (xml_file_dir == null) {
            return;
        }
        List<VirtualFile> root_files = IdeaTargetLanguageHelpers.find_root_files(xml_file_dir);
        if (root_files.size() == 0) {
            IdeaMessageHelpers.add_error_to_ide_log("ERROR", "Root-file not found");
            return;
        }
        VirtualFile root_file = root_files.get(0);
        if (root_files.size() > 1) {
            IdeaMessageHelpers.add_warning_to_ide_log("Several Root-files found. Selected " + root_file.getName());
        }
        try {
            Settings settings = SdmUtils.load_settings(xml_file_dir.getPath());
            StringBuilder output_dir = new StringBuilder();
            String xml_metaprogram_abs_path = root_file.getParent().getPath();
            String contextPath = DaoClass.class.getPackage().getName();
            XmlParser xml_parser = new XmlParser(contextPath, Helpers.concat_path(xml_metaprogram_abs_path, Const.DAO_XSD));
            Connection con = IdeaHelpers.get_connection(project, settings);
            try {
                IDaoCG gen = IdeaTargetLanguageHelpers.create_dao_cg(con, project, root_file, settings, output_dir);
                String dao_xml_rel_path = IdeaHelpers.get_relative_path(project, xml_file);
                try {
                    String dao_class_name = Helpers.get_dao_class_name(dao_xml_rel_path);
                    String dao_xml_abs_path = xml_file.getPath();//  Helpers.concat_path(local_abs_path, dao_xml_rel_path);
                    DaoClass dao_class = xml_parser.unmarshal(dao_xml_abs_path);
                    String[] file_content = gen.translate(dao_class_name, dao_class);
                    StringBuilder validationBuff = new StringBuilder();
                    IdeaTargetLanguageHelpers.validate_dao(project, root_file, settings, dao_class_name, file_content, validationBuff);
                    String status = validationBuff.toString();
                    if (status.length() == 0) {
                        String xml_file_rel_path = IdeaHelpers.get_relative_path(project, xml_file);
                        IdeaMessageHelpers.add_info_to_ide_log(xml_file_rel_path + " -> OK");
                    } else {
                        IdeaMessageHelpers.add_error_to_ide_log("ERROR", " " + xml_file.getNameWithoutExtension() + ". " + status);
                    }
                } catch (Throwable e) {
                    String msg = e.getMessage();
                    IdeaMessageHelpers.add_error_to_ide_log("ERROR", msg);
                }
            } finally {
                con.close();
            }
        } catch (Exception e) {
            IdeaMessageHelpers.add_error_to_ide_log("ERROR", e.getMessage());
            e.printStackTrace();
        }
    }

    public static void generate_dao(Project project, VirtualFile xml_file) {
        String name = xml_file.getName();
        if (!FileSearchHelpers.is_dao_xml(name)) {
            return;
        }
        VirtualFile xml_file_dir = xml_file.getParent();
        if (xml_file_dir == null) {
            return;
        }
        List<VirtualFile> root_files = IdeaTargetLanguageHelpers.find_root_files(xml_file_dir);
        if (root_files.size() == 0) {
            IdeaMessageHelpers.add_error_to_ide_log("ERROR", "Root-file not found");
            return;
        }
        VirtualFile root_file = root_files.get(0);
        if (root_files.size() > 1) {
            IdeaMessageHelpers.add_warning_to_ide_log("Several Root-files found. Selected " + root_file.getName());
        }
        try {
            Settings settings = SdmUtils.load_settings(xml_file_dir.getPath());
            List<IdeaHelpers.GeneratedFileData> list = new ArrayList<IdeaHelpers.GeneratedFileData>();
            StringBuilder output_dir = new StringBuilder();
            String xml_metaprogram_abs_path = root_file.getParent().getPath();
            String contextPath = DaoClass.class.getPackage().getName();
            XmlParser xml_parser = new XmlParser(contextPath, Helpers.concat_path(xml_metaprogram_abs_path, Const.DAO_XSD));
            boolean error = false;
            Connection con = IdeaHelpers.get_connection(project, settings);
            try {
                IDaoCG gen = IdeaTargetLanguageHelpers.create_dao_cg(con, project, root_file, settings, output_dir);
                String dao_xml_rel_path = IdeaHelpers.get_relative_path(project, xml_file);
                try {
                    String dao_class_name = Helpers.get_dao_class_name(dao_xml_rel_path);
                    String dao_xml_abs_path = xml_file.getPath();
                    DaoClass dao_class = xml_parser.unmarshal(dao_xml_abs_path);
                    String[] file_content = gen.translate(dao_class_name, dao_class);
                    IdeaTargetLanguageHelpers.prepare_generated_file_data(root_file, dao_class_name, file_content, list);
                } catch (Throwable e) {
                    String msg = e.getMessage();
                    IdeaMessageHelpers.add_error_to_ide_log("ERROR", msg);
                    error = true;
                }
            } finally {
                con.close();
            }
            try {
                if (list.size() > 0) {
                    IdeaHelpers.run_write_action_to_generate_source_file(output_dir.toString(), list, project);
                    if (!error) {
                        String xml_file_rel_path = IdeaHelpers.get_relative_path(project, xml_file);
                        IdeaMessageHelpers.add_info_to_ide_log(xml_file_rel_path + " -> Generated successfully");
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                IdeaMessageHelpers.add_error_to_ide_log("ERROR", e.getMessage());
            }
        } catch (Exception e) {
            e.printStackTrace();
            IdeaMessageHelpers.add_error_to_ide_log("ERROR", e.getMessage());
        }
    }
}
