/*
    Copyright 2011-2024 sqldalmaker@gmail.com
    SQL DAL Maker Website: https://sqldalmaker.sourceforge.net/
    Read LICENSE.txt in the root of this project/archive for details.
 */
package com.sqldalmaker.intellij.ui;

import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.sqldalmaker.cg.Helpers;
import com.sqldalmaker.cg.IDaoCG;
import com.sqldalmaker.cg.IDtoCG;
import com.sqldalmaker.common.*;
import com.sqldalmaker.jaxb.sdm.DaoClass;
import com.sqldalmaker.jaxb.sdm.DtoClass;
import com.sqldalmaker.jaxb.settings.Settings;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

public class IdeaCG {

    public static void generate_all_sdm_dto(Project project, VirtualFile xml_file) {
        String name = xml_file.getName();
        if (!Helpers.is_sdm_xml(name)) {
            return;
        }
        VirtualFile xml_file_dir = xml_file.getParent();
        if (xml_file_dir == null) {
            return;
        }
        List<VirtualFile> root_files = IdeaTargetLanguageHelpers.find_root_files(xml_file_dir);
        if (root_files.isEmpty()) {
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
            public boolean occurred = false;
        }
        Error err = new Error();
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
                            String sdm_xml_abs_path = xml_file.getPath();
                            String sdm_xsd_abs_path = xml_mp_abs_path + "/" + Const.SDM_XSD;
                            List<DtoClass> dto_classes = SdmUtils.get_dto_classes(sdm_xml_abs_path, sdm_xsd_abs_path);
                            for (DtoClass cls : dto_classes) {
                                ProgressManager.progress(cls.getName());
                                String[] file_content = gen.translate(cls.getName());
                                IdeaTargetLanguageHelpers.prepare_generated_file_data(root_file, cls.getName(), file_content, list);
                            }
                        } catch (Throwable e) {
                            String msg = e.getMessage();
                            IdeaMessageHelpers.add_error_to_ide_log("ERROR", msg);
                            err.occurred = true;
                        }
                    } finally {
                        con.close();
                    }
                } catch (Exception e) {
                    IdeaMessageHelpers.add_error_to_ide_log("ERROR", e.getMessage());
                    // // e.printStackTrace();
                }
            }
        };
        ProgressManager.getInstance().runProcessWithProgressSynchronously(runnable, "DTO/Models", false, project);
        try { // outside Runnable
            if (!list.isEmpty()) {
                String xml_file_rel_path = IdeaHelpers.get_relative_path(project, xml_file);
                IdeaHelpers.run_write_action_to_generate_source_file(output_dir.toString(), list, project);
                if (!err.occurred) {
                    IdeaMessageHelpers.add_info_to_ide_log(xml_file_rel_path, Const.GENERATE_SDM_DTO_MODELS);
                }
            }
        } catch (Exception e) {
            // e.printStackTrace();
            IdeaMessageHelpers.show_error_in_ui_thread(e);
        }
    }

    public static void generate_all_sdm_dao(Project project, VirtualFile xml_file) {
        String name = xml_file.getName();
        if (!Helpers.is_sdm_xml(name)) {
            return;
        }
        VirtualFile xml_file_dir = xml_file.getParent();
        if (xml_file_dir == null) {
            return;
        }
        List<VirtualFile> root_files = IdeaTargetLanguageHelpers.find_root_files(xml_file_dir);
        if (root_files.isEmpty()) {
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
            public boolean occurred = false;
        }
        Error err = new Error();
        final Runnable runnable = new Runnable() {
            @Override
            public void run() {
                try {
                    Settings settings = SdmUtils.load_settings(xml_file_dir.getPath());
                    String xml_mp_abs_path = root_file.getParent().getPath();
                    Connection con = IdeaHelpers.get_connection(project, settings);
                    try {
                        IDaoCG gen = IdeaTargetLanguageHelpers.create_dao_cg(con, project, root_file, settings, output_dir);
                        try {
                            String sdm_xml_abs_path = xml_file.getPath();
                            String sdm_xsd_abs_path = xml_mp_abs_path + "/" + Const.SDM_XSD;
                            List<DaoClass> dao_classes = SdmUtils.get_dao_classes(sdm_xml_abs_path, sdm_xsd_abs_path);
                            for (DaoClass cls : dao_classes) {
                                ProgressManager.progress(cls.getName());
                                String[] file_content = generate_single_sdm_dao(project, root_file, gen, cls, settings);
                                IdeaTargetLanguageHelpers.prepare_generated_file_data(root_file, cls.getName(), file_content, list);
                            }
                        } catch (Throwable e) {
                            String msg = e.getMessage();
                            IdeaMessageHelpers.add_error_to_ide_log("ERROR", msg);
                            err.occurred = true;
                        }
                    } finally {
                        con.close();
                    }
                } catch (Exception e) {
                    IdeaMessageHelpers.add_error_to_ide_log("ERROR", e.getMessage());
                    // e.printStackTrace();
                }
            }
        };
        ProgressManager.getInstance().runProcessWithProgressSynchronously(runnable, "DAO Code Generation", false, project);
        try { // outside Runnable
            if (!list.isEmpty()) {
                String xml_file_rel_path = IdeaHelpers.get_relative_path(project, xml_file);
                IdeaHelpers.run_write_action_to_generate_source_file(output_dir.toString(), list, project);
                if (!err.occurred) {
                    IdeaMessageHelpers.add_info_to_ide_log(xml_file_rel_path, Const.GENERATE_SDM_DAO);
                }
            }
        } catch (Exception e) {
            // e.printStackTrace();
            IdeaMessageHelpers.show_error_in_ui_thread(e);
        }
    }

    public static void validate_all_sdm_dto(Project project, VirtualFile xml_file) {
        String name = xml_file.getName();
        if (!Helpers.is_sdm_xml(name)) {
            return;
        }
        VirtualFile xml_file_dir = xml_file.getParent();
        if (xml_file_dir == null) {
            return;
        }
        List<VirtualFile> root_files = IdeaTargetLanguageHelpers.find_root_files(xml_file_dir);
        if (root_files.isEmpty()) {
            IdeaMessageHelpers.add_error_to_ide_log("ERROR", "root-file not found");
            return;
        }
        VirtualFile root_file = root_files.get(0);
        if (root_files.size() > 1) {
            IdeaMessageHelpers.add_warning_to_ide_log("Several root-files found. " + root_file.getName() + "selected");
        }
        final Runnable runnable = new Runnable() {
            @Override
            public void run() {
                try {
                    Settings settings = SdmUtils.load_settings(xml_file_dir.getPath());
                    StringBuilder output_dir = new StringBuilder();
                    Connection con = IdeaHelpers.get_connection(project, settings);
                    try {
                        IDtoCG gen = IdeaTargetLanguageHelpers.create_dto_cg(con, project, root_file, settings, output_dir);
                        String sdm_xml_abs_path = xml_file.getPath();
                        String sdm_folder_abs_path = root_file.getParent().getPath();
                        String sdm_xsd_abs_path = sdm_folder_abs_path + "/" + Const.SDM_XSD;
                        List<DtoClass> dto_classes = SdmUtils.get_dto_classes(sdm_xml_abs_path, sdm_xsd_abs_path);
                        boolean error = false;
                        for (DtoClass cls : dto_classes) {
                            try {
                                ProgressManager.progress(cls.getName());
                                String[] file_content = gen.translate(cls.getName());
                                StringBuilder validationBuff = new StringBuilder();
                                IdeaTargetLanguageHelpers.validate_dto(project, root_file, settings, cls.getName(), file_content, validationBuff);
                                String status = validationBuff.toString();
                                if (!status.isEmpty()) {
                                    error = true;
                                    IdeaMessageHelpers.add_error_to_ide_log("ERROR", " DTO/Model '" + cls.getName() + "'. " + status);
                                }
                            } catch (Throwable e) {
                                error = true;
                                String msg = e.getMessage();
                                IdeaMessageHelpers.add_error_to_ide_log("ERROR", msg);
                            }
                        }
                        if (!error) {
                            String xml_file_rel_path = IdeaHelpers.get_relative_path(project, xml_file);
                            IdeaMessageHelpers.add_info_to_ide_log(xml_file_rel_path, Const.VALIDATE_SDM_DTO_MODELS);
                        }
                    } finally {
                        con.close();
                    }
                } catch (Exception e) {
                    IdeaMessageHelpers.add_error_to_ide_log("ERROR", e.getMessage());
                    // e.printStackTrace();
                }
            }
        };
        ProgressManager.getInstance().runProcessWithProgressSynchronously(runnable, "DTO/Models", false, project);
    }

    public static void validate_all_sdm_dao_action(Project project, VirtualFile xml_file) {
        String name = xml_file.getName();
        if (!Helpers.is_sdm_xml(name)) {
            return;
        }
        VirtualFile xml_file_dir = xml_file.getParent();
        if (xml_file_dir == null) {
            return;
        }
        List<VirtualFile> root_files = IdeaTargetLanguageHelpers.find_root_files(xml_file_dir);
        if (root_files.isEmpty()) {
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
                    String sdm_abs_path = root_file.getParent().getPath();
                    Connection con = IdeaHelpers.get_connection(project, settings);
                    try {
                        IDaoCG gen = IdeaTargetLanguageHelpers.create_dao_cg(con, project, root_file, settings, output_dir);
                        String sdm_xml_abs_path = xml_file.getPath();
                        String sdm_xsd_abs_path = sdm_abs_path + "/" + Const.SDM_XSD;
                        List<DaoClass> dao_classes = SdmUtils.get_dao_classes(sdm_xml_abs_path, sdm_xsd_abs_path);
                        boolean error = false;
                        for (DaoClass cls : dao_classes) {
                            try {
                                ProgressManager.progress(cls.getName());
                                String status = validate_single_sdm_dao(project, root_file, gen, cls, settings);
                                if (!status.isEmpty()) {
                                    error = true;
                                    IdeaMessageHelpers.add_error_to_ide_log("ERROR", " DAO class '" + cls.getName() + "'. " + status);
                                }
                            } catch (Throwable e) {
                                error = true;
                                String msg = e.getMessage();
                                IdeaMessageHelpers.add_error_to_ide_log("ERROR", msg);
                            }
                        }
                        if (!error && !dao_classes.isEmpty()) {
                            String xml_file_rel_path = IdeaHelpers.get_relative_path(project, xml_file);
                            IdeaMessageHelpers.add_info_to_ide_log(xml_file_rel_path, Const.VALIDATE_SDM_DAO);
                        }
                    } finally {
                        con.close();
                    }
                } catch (Exception e) {
                    IdeaMessageHelpers.add_error_to_ide_log("ERROR", e.getMessage());
                    // e.printStackTrace();
                }
            }
        };
        ProgressManager.getInstance().runProcessWithProgressSynchronously(runnable, "DAO", false, project);
    }

    public static void generate_dao(Project project, VirtualFile dao_xml_file) {
        String name = dao_xml_file.getName();
        if (!Helpers.is_dao_xml(name)) {
            return;
        }
        VirtualFile xml_file_dir = dao_xml_file.getParent();
        if (xml_file_dir == null) {
            return;
        }
        List<VirtualFile> root_files = IdeaTargetLanguageHelpers.find_root_files(xml_file_dir);
        if (root_files.isEmpty()) {
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
            String dao_xml_abs_path = dao_xml_file.getPath();
            Path path = Paths.get(dao_xml_abs_path);
            String dao_xml_file_name = path.getFileName().toString();
            String dao_class_name = Helpers.get_dao_class_name(dao_xml_file_name);
            Connection con = IdeaHelpers.get_connection(project, settings);
            try {
                IDaoCG gen = IdeaTargetLanguageHelpers.create_dao_cg(con, project, root_file, settings, output_dir);
                try {
                    DaoClass dao_class = xml_parser.unmarshal(dao_xml_abs_path);
                    String[] file_content = gen.translate(dao_class_name, dao_class);
                    IdeaTargetLanguageHelpers.prepare_generated_file_data(root_file, dao_class_name, file_content, list);
                } catch (Throwable e) {
                    String msg = e.getMessage();
                    IdeaMessageHelpers.add_error_to_ide_log(dao_xml_file_name, msg);
                    error = true;
                }
            } finally {
                con.close();
            }
            try {
                if (!list.isEmpty()) {
                    IdeaHelpers.run_write_action_to_generate_source_file(output_dir.toString(), list, project);
                    if (!error) {
                        IdeaMessageHelpers.add_info_to_ide_log(dao_xml_file_name, Const.GENERATE_DAO_XML);
                    }
                }
            } catch (Exception e) {
                // e.printStackTrace();
                IdeaMessageHelpers.add_error_to_ide_log(dao_xml_file_name, e.getMessage());
            }
        } catch (Exception e) {
            // e.printStackTrace();
            IdeaMessageHelpers.add_error_to_ide_log("ERROR", e.getMessage());
        }
    }

    public static String[] generate_single_sdm_dao(
            Project project,
            VirtualFile root_file,
            IDaoCG gen,
            DaoClass sdm_dao_class,
            Settings settings) throws Exception {

        String dao_class_name = sdm_dao_class.getName();
        String[] file_content;
        String ref = sdm_dao_class.getRef();
        if (ref == null || ref.trim().isEmpty()) { // nullable
            file_content = gen.translate(dao_class_name, sdm_dao_class);
        } else {
            DaoClass external_dao_class = load_external_dao_class(root_file, sdm_dao_class);
            file_content = gen.translate(dao_class_name, external_dao_class);
        }
        return file_content;
    }

    public static String validate_single_sdm_dao(
            Project project,
            VirtualFile root_file,
            IDaoCG gen,
            DaoClass sdm_dao_class,
            Settings settings) throws Exception {

        StringBuilder validation_buff = new StringBuilder();
        String dao_class_name = sdm_dao_class.getName();
        String[] file_content;
        String ref = sdm_dao_class.getRef();
        if (ref == null || ref.trim().isEmpty()) { // nullable
            file_content = gen.translate(dao_class_name, sdm_dao_class);
        } else {
            DaoClass external_dao_class = load_external_dao_class(root_file, sdm_dao_class);
            file_content = gen.translate(dao_class_name, external_dao_class);
        }
        IdeaTargetLanguageHelpers.validate_dao(project, root_file, settings, dao_class_name, file_content, validation_buff);
        return validation_buff.toString();
    }

    private static DaoClass load_external_dao_class(VirtualFile root_file, DaoClass sdm_dao_class) throws Exception {
        String local_abs_path = root_file.getParent().getPath();
        String dao_xml_rel_path = sdm_dao_class.getRef();
        String dao_xml_abs_path = Helpers.concat_path(local_abs_path, dao_xml_rel_path);
        String contextPath = DaoClass.class.getPackage().getName();
        XmlParser xml_parser = new XmlParser(contextPath, Helpers.concat_path(local_abs_path, Const.DAO_XSD));
        return xml_parser.unmarshal(dao_xml_abs_path);
    }

    public static void validate_external_dao_xml_action(Project project, VirtualFile xml_file) {
        String name = xml_file.getName();
        if (!Helpers.is_dao_xml(name)) {
            return;
        }
        VirtualFile xml_file_dir = xml_file.getParent();
        if (xml_file_dir == null) {
            return;
        }
        List<VirtualFile> root_files = IdeaTargetLanguageHelpers.find_root_files(xml_file_dir);
        if (root_files.isEmpty()) {
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
            String sdm_folder_abs_path = root_file.getParent().getPath();
            String contextPath = DaoClass.class.getPackage().getName();
            XmlParser xml_parser = new XmlParser(contextPath, Helpers.concat_path(sdm_folder_abs_path, Const.DAO_XSD));
            String dao_xml_abs_path = xml_file.getPath();
            Path path = Paths.get(dao_xml_abs_path);
            String dao_xml_file_name = path.getFileName().toString();
            Connection con = IdeaHelpers.get_connection(project, settings);
            try {
                IDaoCG gen = IdeaTargetLanguageHelpers.create_dao_cg(con, project, root_file, settings, output_dir);
                try {
                    DaoClass dao_class = xml_parser.unmarshal(dao_xml_abs_path);
                    String dao_class_name = Helpers.get_dao_class_name(dao_xml_file_name);
                    dao_class.setName(dao_class_name);
                    String status = validate_single_sdm_dao(project, root_file, gen, dao_class, settings);
                    if (status.isEmpty()) {
                        IdeaMessageHelpers.add_info_to_ide_log(dao_xml_file_name, Const.VALIDATE_DAO_XML);
                    } else {
                        IdeaMessageHelpers.add_error_to_ide_log(dao_xml_file_name, " " + xml_file.getNameWithoutExtension() + ". " + status);
                    }
                } catch (Throwable e) {
                    String msg = e.getMessage();
                    IdeaMessageHelpers.add_error_to_ide_log(dao_xml_file_name, msg);
                }
            } finally {
                con.close();
            }
        } catch (Exception e) {
            IdeaMessageHelpers.add_error_to_ide_log("ERROR", e.getMessage());
            // e.printStackTrace();
        }
    }
}
