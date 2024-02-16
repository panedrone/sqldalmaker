/*
    Copyright 2011-2023 sqldalmaker@gmail.com
    SQL DAL Maker Website: https://sqldalmaker.sourceforge.net/
    Read LICENSE.txt in the root of this project/archive for details.
 */
package com.sqldalmaker.intellij.ui;

import com.intellij.openapi.fileTypes.ExactFileNameMatcher;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeConsumer;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.sqldalmaker.cg.Helpers;
import com.sqldalmaker.cg.IDaoCG;
import com.sqldalmaker.cg.IDtoCG;
import com.sqldalmaker.common.Const;
import com.sqldalmaker.common.TargetLangUtils;
import com.sqldalmaker.jaxb.settings.Settings;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: sqldalmaker@gmail.com
 * Date: 21.06.12
 */
public class IdeaTargetLanguageHelpers {

    public static List<VirtualFile> find_root_files(VirtualFile dir) {
        List<VirtualFile> root_files = new ArrayList<VirtualFile>();
        String[] rf_names = {Const.Root.PHP, Const.Root.JAVA, Const.Root.CPP, Const.Root.PYTHON, Const.Root.GO};
        for (String rf : rf_names) {
            VirtualFile root_file;
            root_file = dir.findFileByRelativePath(rf);
            if (root_file != null) {
                root_files.add(root_file);
            }
        }
        return root_files;
    }

    public static boolean snake_case_needed(VirtualFile root_file) {
        String fn = root_file.getName();
        return TargetLangUtils.snake_case_needed(fn);
    }

    public static boolean lower_camel_case_needed(VirtualFile root_file) {
        String fn = root_file.getName();
        return TargetLangUtils.lower_camel_case_needed(fn);
    }

    public static void register(@NotNull FileTypeConsumer consumer, FileType file_type) {
        consumer.consume(file_type, new ExactFileNameMatcher(Const.Root.JAVA));
        consumer.consume(file_type, new ExactFileNameMatcher(Const.Root.CPP));
        consumer.consume(file_type, new ExactFileNameMatcher(Const.Root.PHP));
        consumer.consume(file_type, new ExactFileNameMatcher(Const.Root.PYTHON));
        consumer.consume(file_type, new ExactFileNameMatcher(Const.Root.GO));
    }

    public static String get_target_folder_rel_path(
            Project project,
            VirtualFile root_file,
            Settings settings,
            String class_name,
            String class_scope) throws Exception {

        String target_folder_abs_path = get_target_folder_abs_path(project, root_file, settings, class_scope);
        String target_file_abs_path = TargetLangUtils.get_target_file_path(root_file.getName(), target_folder_abs_path, class_name);
        String rel_path = IdeaHelpers.get_relative_path(project, target_file_abs_path);
        return rel_path;
    }

    public static void open_dto_sync(
            Project project,
            VirtualFile root_file,
            Settings settings,
            String dto_class_name) throws Exception {

        String target_file_abs_path = get_dto_file_abs_path(project, root_file, settings, dto_class_name);
        String rel_path = IdeaHelpers.get_relative_path(project, target_file_abs_path);
        IdeaEditorHelpers.open_project_file_in_editor_sync(project, rel_path);
    }

    public static void open_dao_sync(
            Project project,
            VirtualFile root_file,
            Settings settings,
            String dao_class_name) throws Exception {

        String target_file_abs_path = get_dao_file_abs_path(project, root_file, settings, dao_class_name);
        String rel_path = IdeaHelpers.get_relative_path(project, target_file_abs_path);
        IdeaEditorHelpers.open_project_file_in_editor_sync(project, rel_path);
    }

    public static String get_target_folder_abs_path(
            Project project,
            VirtualFile root_file,
            Settings settings,
            String class_scope) throws Exception {

        String target_folder_rel_path = settings.getFolders().getTarget();
        String module_root = IdeaHelpers.get_project_base_dir(project).getPath();
        String root_file_fn = root_file.getName();
        return TargetLangUtils.get_target_folder_abs_path(class_scope, root_file_fn, target_folder_rel_path, module_root);
    }

    public static String get_dto_file_abs_path(
            Project project,
            VirtualFile root_file,
            Settings settings,
            String dto_class_name) throws Exception {

        String root_file_fn = root_file.getName();
        String target_folder_abs_path;
        if (Const.Root.GO.equals(root_file_fn)) {
            String dto_folder_rel_path = TargetLangUtils.get_golang_dto_folder_rel_path(settings);
            String module_root = IdeaHelpers.get_project_base_dir(project).getPath();
            target_folder_abs_path = Helpers.concat_path(module_root, dto_folder_rel_path);
        } else {
            target_folder_abs_path = get_target_folder_abs_path(project, root_file, settings, settings.getDto().getScope());
        }
        String target_file_abs_path = TargetLangUtils.get_target_file_path(root_file_fn, target_folder_abs_path, dto_class_name);
        return target_file_abs_path;
    }

    public static String get_dao_file_abs_path(
            Project project,
            VirtualFile root_file,
            Settings settings,
            String dao_class_name) throws Exception {

        String root_file_fn = root_file.getName();
        String target_folder_abs_path;
        if (Const.Root.GO.equals(root_file_fn)) {
            String dao_folder_rel_path = TargetLangUtils.get_golang_dao_folder_rel_path(settings);
            String module_root = IdeaHelpers.get_project_base_dir(project).getPath();
            target_folder_abs_path = Helpers.concat_path(module_root, dao_folder_rel_path);
        } else {
            target_folder_abs_path = get_target_folder_abs_path(project, root_file, settings, settings.getDao().getScope());
        }
        String target_file_abs_path = TargetLangUtils.get_target_file_path(root_file_fn, target_folder_abs_path, dao_class_name);
        return target_file_abs_path;
    }

    /*
     * returns null if the file is not root-file
     */
    public static String get_root_file_relative_path(Project project, VirtualFile file) {
        String fn = file.getName();
        if (Const.Root.JAVA.equals(fn)
                || Const.Root.CPP.equals(fn)
                || Const.Root.PHP.equals(fn)
                || Const.Root.PYTHON.equals(fn)
                || Const.Root.GO.equals(fn)) {
            try {
                return IdeaHelpers.get_relative_path(project, file);
            } catch (Exception e) {
                // e.printStackTrace();
            }
        }
        return null;
    }

    public static boolean accept(@NotNull VirtualFile file) {
        return TargetLangUtils.accept(file.getName());
    }

    public static IDtoCG create_dto_cg(
            Connection con,
            Project project,
            VirtualFile root_file,
            Settings settings,
            StringBuilder output_dir_rel_path) throws Exception {

        String project_abs_path = IdeaHelpers.get_project_base_dir(project).getPath();
        String xml_configs_folder_full_path = root_file.getParent().getPath();
        String root_fn = root_file.getName();
        return TargetLangUtils.create_dto_cg(root_fn, project_abs_path, xml_configs_folder_full_path,
                con, settings, output_dir_rel_path);
    }

    public static IDaoCG create_dao_cg(
            Connection con,
            Project project,
            VirtualFile root_file,
            Settings settings,
            StringBuilder output_dir_rel_path) throws Exception {

        String project_abs_path = IdeaHelpers.get_project_base_dir(project).getPath();
        String xml_configs_folder_full_path = root_file.getParent().getPath();
        String root_fn = root_file.getName();
        return TargetLangUtils.create_dao_cg(root_fn, project_abs_path, xml_configs_folder_full_path,
                con, settings, output_dir_rel_path);
    }
}