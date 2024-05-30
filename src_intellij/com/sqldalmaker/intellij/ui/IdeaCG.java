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
import com.sqldalmaker.cg.JaxbUtils;
import com.sqldalmaker.common.*;
import com.sqldalmaker.jaxb.sdm.DaoClass;
import com.sqldalmaker.jaxb.sdm.DtoClass;
import com.sqldalmaker.jaxb.settings.Settings;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

/*
 * @author sqldalmaker@gmail.com
 *
 * 30.05.2024 20:00 1.299
 * 25.04.2024 08:46 1.297
 * 18.02.2024 18:38 1.294
 * 17.12.2023 02:16 1.292 sdm.xml
 * 16.11.2022 08:02 1.269
 * 06.04.2022 19:58 1.218 navigation from XML to generated files
 * 30.03.2022 20:33 intellij event log + balloons
 *
 */
public class IdeaCG {

    private static VirtualFile find_root_file(VirtualFile sdm_xml_folder) {
        List<VirtualFile> root_files = IdeaTargetLanguageHelpers.find_root_files(sdm_xml_folder);
        if (root_files.isEmpty()) {
            IdeaMessageHelpers.add_error_to_ide_log("ERROR", "root-file not found");
            return null;
        }
        VirtualFile root_file = root_files.get(0);
        if (root_files.size() > 1) {
            IdeaMessageHelpers.add_warning_to_ide_log("Several root-files found, '" + root_file.getName() + "' selected");
        }
        return root_file;
    }

    private static class ErrorSign {
        public boolean occurred = false;
    }

    public static class ProgressError {
        public Throwable error = null;
    }

    // IdeaActionGroup
    public static void action_generate_all_sdm_dto(Project project, VirtualFile sdm_xml_file) {
        String name = sdm_xml_file.getName();
        if (!Helpers.is_sdm_xml(name)) {
            return;
        }
        VirtualFile sdm_xml_folder = sdm_xml_file.getParent();
        if (sdm_xml_folder == null) {
            return;
        }
        VirtualFile root_file = find_root_file(sdm_xml_folder);
        if (root_file == null) {
            return;
        }
        List<IdeaHelpers.GeneratedFileData> list = new ArrayList<IdeaHelpers.GeneratedFileData>();
        StringBuilder output_dir = new StringBuilder();
        ErrorSign err = new ErrorSign();
        final Runnable runnable = new Runnable() {
            @Override
            public void run() {
                try {
                    Settings settings = SdmUtils.load_settings(sdm_xml_folder.getPath());
                    String sdm_xml_folder_abs_path = sdm_xml_folder.getPath();
                    Connection con = IdeaHelpers.get_connection(project, settings);
                    try {
                        IDtoCG gen = IdeaTargetLanguageHelpers.create_dto_cg(con, project, root_file, settings, output_dir);
                        try {
                            String sdm_xml_abs_path = sdm_xml_file.getPath();
                            String sdm_xsd_abs_path = sdm_xml_folder_abs_path + "/" + Const.SDM_XSD;
                            List<DtoClass> dto_classes = SdmUtils.get_dto_classes(sdm_xml_abs_path, sdm_xsd_abs_path);
                            for (DtoClass cls : dto_classes) {
                                ProgressManager.progress(cls.getName());
                                String[] file_content = gen.translate(cls.getName());
                                prepare_generated_file_data(root_file, cls.getName(), file_content, list);
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
                String xml_file_rel_path = IdeaHelpers.get_relative_path(project, sdm_xml_file);
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

    // IdeaActionGroup
    public static void action_validate_all_sdm_dto(Project project, VirtualFile sdm_xml_file) {
        String name = sdm_xml_file.getName();
        if (!Helpers.is_sdm_xml(name)) {
            return;
        }
        VirtualFile sdm_xml_folder = sdm_xml_file.getParent();
        if (sdm_xml_folder == null) {
            return;
        }
        VirtualFile root_file = find_root_file(sdm_xml_folder);
        if (root_file == null) {
            return;
        }
        final Runnable runnable = new Runnable() {
            @Override
            public void run() {
                try {
                    Settings settings = SdmUtils.load_settings(sdm_xml_folder.getPath());
                    StringBuilder output_dir = new StringBuilder();
                    Connection con = IdeaHelpers.get_connection(project, settings);
                    try {
                        IDtoCG gen = IdeaTargetLanguageHelpers.create_dto_cg(con, project, root_file, settings, output_dir);
                        String sdm_xml_abs_path = sdm_xml_file.getPath();
                        String sdm_xml_folder_abs_path = sdm_xml_folder.getPath();
                        String sdm_xsd_abs_path = sdm_xml_folder_abs_path + "/" + Const.SDM_XSD;
                        List<DtoClass> dto_classes = SdmUtils.get_dto_classes(sdm_xml_abs_path, sdm_xsd_abs_path);
                        boolean error = false;
                        for (DtoClass cls : dto_classes) {
                            try {
                                ProgressManager.progress(cls.getName());
                                String[] file_content = gen.translate(cls.getName());
                                StringBuilder err_buf = new StringBuilder();
                                validate_single_dto_ignoring_eol(project, root_file, settings, cls.getName(), file_content, err_buf);
                                String status = err_buf.toString();
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
                            String xml_file_rel_path = IdeaHelpers.get_relative_path(project, sdm_xml_file);
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

    // IdeaActionGroup
    public static void action_generate_all_sdm_dao(Project project, VirtualFile sdm_xml_file) {
        String name = sdm_xml_file.getName();
        if (!Helpers.is_sdm_xml(name)) {
            return;
        }
        VirtualFile sdm_xml_folder = sdm_xml_file.getParent();
        if (sdm_xml_folder == null) {
            return;
        }
        VirtualFile root_file = find_root_file(sdm_xml_folder);
        if (root_file == null) {
            return;
        }
        List<IdeaHelpers.GeneratedFileData> list = new ArrayList<IdeaHelpers.GeneratedFileData>();
        StringBuilder output_dir = new StringBuilder();
        ErrorSign err = new ErrorSign();
        final Runnable runnable = new Runnable() {
            @Override
            public void run() {
                try {
                    Settings settings = SdmUtils.load_settings(sdm_xml_folder.getPath());
                    String sdm_xml_folder_abs_path = sdm_xml_folder.getPath();
                    Connection con = IdeaHelpers.get_connection(project, settings);
                    try {
                        IDaoCG gen = IdeaTargetLanguageHelpers.create_dao_cg(con, project, root_file, settings, output_dir);
                        try {
                            List<DaoClass> dao_classes = IdeaHelpers.load_all_sdm_dao_classes(sdm_xml_file);
                            for (DaoClass cls : dao_classes) {
                                ProgressManager.progress(cls.getName());
                                String[] file_content = generate_single_sdm_dao(project, root_file, gen, cls, settings);
                                prepare_generated_file_data(root_file, cls.getName(), file_content, list);
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
                IdeaHelpers.run_write_action_to_generate_source_file(output_dir.toString(), list, project);
                if (!err.occurred) {
                    String sdm_xml_file_rel_path = IdeaHelpers.get_relative_path(project, sdm_xml_file);
                    IdeaMessageHelpers.add_info_to_ide_log(sdm_xml_file_rel_path, Const.GENERATE_SDM_DAO);
                }
            }
        } catch (Exception e) {
            // e.printStackTrace();
            IdeaMessageHelpers.show_error_in_ui_thread(e);
        }
    }

    // IdeaActionGroup
    public static void action_validate_all_sdm_dao(Project project, VirtualFile sdm_xml_file) {
        String name = sdm_xml_file.getName();
        if (!Helpers.is_sdm_xml(name)) {
            return;
        }
        VirtualFile sdm_xml_folder = sdm_xml_file.getParent();
        if (sdm_xml_folder == null) {
            return;
        }
        VirtualFile root_file = find_root_file(sdm_xml_folder);
        if (root_file == null) {
            return;
        }
        final Runnable runnable = new Runnable() {
            @Override
            public void run() {
                try {
                    Settings settings = SdmUtils.load_settings(sdm_xml_folder.getPath());
                    StringBuilder output_dir = new StringBuilder();
                    String sdm_xml_folder_abs_path = sdm_xml_folder.getPath();
                    Connection con = IdeaHelpers.get_connection(project, settings);
                    try {
                        IDaoCG gen = IdeaTargetLanguageHelpers.create_dao_cg(con, project, root_file, settings, output_dir);
                        List<DaoClass> dao_classes = IdeaHelpers.load_all_sdm_dao_classes(sdm_xml_file);
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
                            String xml_file_rel_path = IdeaHelpers.get_relative_path(project, sdm_xml_file);
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

    // IdeaActionGroup
    public static void toolbar_action_generate_external_dao_xml(Project project, VirtualFile external_dao_xml_file) {
        String name = external_dao_xml_file.getName();
        if (!Helpers.is_dao_xml(name)) {
            return;
        }
        VirtualFile sdm_xml_folder = external_dao_xml_file.getParent();
        if (sdm_xml_folder == null) {
            return;
        }
        VirtualFile root_file = find_root_file(sdm_xml_folder);
        if (root_file == null) {
            return;
        }
        try {
            Settings settings = SdmUtils.load_settings(sdm_xml_folder.getPath());
            List<IdeaHelpers.GeneratedFileData> list = new ArrayList<IdeaHelpers.GeneratedFileData>();
            StringBuilder output_dir = new StringBuilder();
            String sdm_xml_folder_abs_path = root_file.getParent().getPath();
            String contextPath = DaoClass.class.getPackage().getName();
            XmlParser xml_parser = new XmlParser(contextPath, Helpers.concat_path(sdm_xml_folder_abs_path, Const.DAO_XSD));
            boolean error = false;
            String dao_xml_abs_path = external_dao_xml_file.getPath();
            Path path = Paths.get(dao_xml_abs_path);
            String xml_file_path = external_dao_xml_file.getPath();
            List<DaoClass> jaxb_dao_classes = IdeaHelpers.load_all_sdm_dao_classes(root_file);
            String dao_class_name = JaxbUtils.get_dao_class_name_by_dao_xml_path(jaxb_dao_classes, xml_file_path);
            String dao_xml_file_name = path.getFileName().toString();
            Connection con = IdeaHelpers.get_connection(project, settings);
            try {
                IDaoCG gen = IdeaTargetLanguageHelpers.create_dao_cg(con, project, root_file, settings, output_dir);
                try {
                    DaoClass dao_class = xml_parser.unmarshal(dao_xml_abs_path);
                    dao_class.setName(dao_class_name);
                    String[] file_content = generate_single_sdm_dao(project, root_file, gen, dao_class, settings);
                    prepare_generated_file_data(root_file, dao_class_name, file_content, list);
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

    // IdeaActionGroup
    public static void toolbar_action_action_validate_external_dao_xml(Project project, VirtualFile external_dao_xml_file) {
        String name = external_dao_xml_file.getName();
        if (!Helpers.is_dao_xml(name)) {
            return;
        }
        VirtualFile sdm_xml_folder = external_dao_xml_file.getParent();
        if (sdm_xml_folder == null) {
            return;
        }
        VirtualFile root_file = find_root_file(sdm_xml_folder);
        if (root_file == null) {
            return;
        }
        try {
            Settings settings = SdmUtils.load_settings(sdm_xml_folder.getPath());
            StringBuilder output_dir = new StringBuilder();
            String sdm_folder_abs_path = root_file.getParent().getPath();
            String contextPath = DaoClass.class.getPackage().getName();
            XmlParser xml_parser = new XmlParser(contextPath, Helpers.concat_path(sdm_folder_abs_path, Const.DAO_XSD));
            String dao_xml_abs_path = external_dao_xml_file.getPath();
            Path path = Paths.get(dao_xml_abs_path);
            String dao_xml_file_name = path.getFileName().toString();
            Connection con = IdeaHelpers.get_connection(project, settings);
            try {
                IDaoCG gen = IdeaTargetLanguageHelpers.create_dao_cg(con, project, root_file, settings, output_dir);
                try {
                    DaoClass dao_class = xml_parser.unmarshal(dao_xml_abs_path);
                    String xml_file_path = external_dao_xml_file.getPath();
                    List<DaoClass> jaxb_dao_classes = IdeaHelpers.load_all_sdm_dao_classes(root_file);
                    String dao_class_name = JaxbUtils.get_dao_class_name_by_dao_xml_path(jaxb_dao_classes, xml_file_path);
                    dao_class.setName(dao_class_name);
                    String status = validate_single_sdm_dao(project, root_file, gen, dao_class, settings);
                    if (status.isEmpty()) {
                        IdeaMessageHelpers.add_info_to_ide_log(dao_xml_file_name, Const.VALIDATE_DAO_XML);
                    } else {
                        IdeaMessageHelpers.add_error_to_ide_log(dao_xml_file_name, " " + external_dao_xml_file.getNameWithoutExtension() + ". " + status);
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

    // IdeaCG, UITabDAO
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
            file_content = gen.translate(sdm_dao_class);
        } else {
            DaoClass external_dao_class = load_external_dao_class(root_file, sdm_dao_class);
            external_dao_class.setName(dao_class_name);
            file_content = gen.translate(external_dao_class);
        }
        return file_content;
    }

    // IdeaCG, UITabDAO
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
            file_content = gen.translate(sdm_dao_class);
        } else {
            DaoClass external_dao_class = load_external_dao_class(root_file, sdm_dao_class);
            external_dao_class.setName(dao_class_name);
            file_content = gen.translate(external_dao_class);
        }
        validate_single_dao_ignoring_eol(project, root_file, settings, dao_class_name, file_content, validation_buff);
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

    public static void prepare_generated_file_data(
            VirtualFile root_file,
            String class_name,
            String[] file_content,
            List<IdeaHelpers.GeneratedFileData> list) throws Exception {

        String file_name = TargetLangUtils.file_name_from_class_name(root_file.getName(), class_name);
        IdeaHelpers.GeneratedFileData gf = new IdeaHelpers.GeneratedFileData();
        gf.file_name = file_name;
        gf.file_content = file_content[0];
        list.add(gf);
    }

    // IdeaCG, UITabDTO
    public static boolean validate_single_dto_ignoring_eol(
            Project project,
            VirtualFile root_file,
            Settings settings,
            String dto_class_name,
            String[] file_content,
            StringBuilder err_buff) throws Exception {

        String target_file_abs_path = IdeaTargetLanguageHelpers.get_dto_file_abs_path(project, root_file, settings, dto_class_name);
        String old_text = Helpers.load_text_from_file(target_file_abs_path);
        return Helpers.equal_ignoring_eol(file_content[0], old_text, err_buff);
    }

    // only IdeaCG
    private static boolean validate_single_dao_ignoring_eol(
            Project project,
            VirtualFile root_file,
            Settings settings,
            String dao_class_name,
            String[] file_content,
            StringBuilder err_buff) throws Exception {

        String target_file_abs_path = IdeaTargetLanguageHelpers.get_dao_file_abs_path(project, root_file, settings, dao_class_name);
        String old_text = Helpers.load_text_from_file(target_file_abs_path);
        return Helpers.equal_ignoring_eol(file_content[0], old_text, err_buff);
    }
}
