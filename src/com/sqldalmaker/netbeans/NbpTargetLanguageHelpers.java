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
import com.sqldalmaker.common.RootFileName;
import com.sqldalmaker.common.SdmUtils;
import com.sqldalmaker.common.TargetLangUtils;
import com.sqldalmaker.jaxb.settings.Settings;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import org.openide.filesystems.FileObject;

/**
 *
 * @author sqldalmaker@gmail.com
 *
 */
public class NbpTargetLanguageHelpers {

    public static List<FileObject> find_root_files(FileObject xml_mp_dir) {
        List<FileObject> root_files = new ArrayList<FileObject>();
        if (xml_mp_dir == null) {
            return root_files;
        }
        String[] rfn = {RootFileName.PHP, RootFileName.JAVA, RootFileName.CPP,
            RootFileName.PYTHON, RootFileName.GO};
        for (String fn : rfn) {
            FileObject root_file = xml_mp_dir.getFileObject(fn);
            if (root_file != null) {
                root_files.add(root_file);
            }
        }
        return root_files;
    }

    public static boolean snake_case_needed(SdmDataObject obj) {
        String fn = obj.getPrimaryFile().getNameExt();
        return TargetLangUtils.snake_case_needed(fn);
    }

    public static boolean lower_camel_case_needed(SdmDataObject obj) {
        String fn = obj.getPrimaryFile().getNameExt();
        return TargetLangUtils.lower_camel_case_needed(fn);
    }

    public static void validate_dto(SdmDataObject obj,
            Settings settings,
            String dto_class_name,
            String[] file_content,
            StringBuilder res_buf) throws Exception {

        String root_fn = obj.getPrimaryFile().getNameExt();
        String source_folder_rel_path = settings.getFolders().getTarget();
        String project_root_abs_path = NbpPathHelpers.get_root_folder_abs_path(obj.getPrimaryFile());
        String target_folder_abs_path = get_target_folder_abs_path(root_fn, project_root_abs_path, source_folder_rel_path, settings.getDto().getScope());
        String file_abs_path = Helpers.concat_path(target_folder_abs_path, TargetLangUtils.file_name_from_class_name(root_fn, dto_class_name));
        /////////////////////////////////////////////////////////
        String old_text = Helpers.load_text_from_file(file_abs_path);
        if (old_text == null) {
            res_buf.append(Const.OUTPUT_FILE_IS_MISSING);
        } else {
            String text = file_content[0];
            if (!Helpers.equal_ignoring_eol(text, old_text)) {
                res_buf.append(Const.OUTPUT_FILE_IS_OUT_OF_DATE);
            }
        }
    }

    public static String get_target_folder_abs_path(String root_fn,
            String project_root_abs_path,
            String target_folder_rel_path,
            String class_scope) {

        return TargetLangUtils.get_target_folder_abs_path(class_scope, root_fn, target_folder_rel_path, project_root_abs_path);
    }

    public static void validate_dao(SdmDataObject obj,
            Settings settings,
            String dao_class_name,
            String[] file_content,
            StringBuilder res_buf) throws Exception {

        String root_fn = obj.getPrimaryFile().getNameExt();
        String source_folder_rel_path = settings.getFolders().getTarget();
        String project_root_abs_path = NbpPathHelpers.get_root_folder_abs_path(obj.getPrimaryFile());
        String target_folder_abs_path = get_target_folder_abs_path(root_fn, project_root_abs_path, source_folder_rel_path, settings.getDao().getScope());
        String file_abs_path = Helpers.concat_path(target_folder_abs_path, TargetLangUtils.file_name_from_class_name(root_fn, dao_class_name));
        /////////////////////////////////////////////////////
        String old_text = Helpers.load_text_from_file(file_abs_path);
        if (old_text == null) {
            res_buf.append(Const.OUTPUT_FILE_IS_MISSING);
        } else {
            String text = file_content[0];
            if (!Helpers.equal_ignoring_eol(text, old_text)) {
                res_buf.append(Const.OUTPUT_FILE_IS_OUT_OF_DATE);
            }
        }
    }

    public static String get_target_file_name(SdmDataObject obj,
            String class_name) throws Exception {

        String root_fn = obj.getPrimaryFile().getNameExt();
        return TargetLangUtils.file_name_from_class_name(root_fn, class_name);
    }

    public static String get_rel_path(Settings settings,
            String root_fn,
            String file_name,
            String scope) {

        if (RootFileName.JAVA.equals(root_fn) || RootFileName.PHP.equals(root_fn)) {
            return Helpers.concat_path(SdmUtils.get_package_relative_path(settings, scope), file_name);
        } else {
            return Helpers.concat_path(settings.getFolders().getTarget(), file_name);
        }
    }

    public static String get_rel_path(SdmDataObject obj,
            Settings settings,
            String class_name,
            String scope) throws Exception {

        String root_fn = obj.getPrimaryFile().getNameExt();
        String target_file_name = TargetLangUtils.file_name_from_class_name(root_fn, class_name);
        String rel_path = get_rel_path(settings, root_fn, target_file_name, scope);
        return rel_path;
    }

    public static void open_in_editor_async(SdmDataObject obj,
            Settings settings,
            String class_name,
            String scope) throws Exception {

        String rel_path = get_rel_path(obj, settings, class_name, scope);
        NbpIdeEditorHelpers.open_project_file_in_editor_async(obj, rel_path);
    }

    public static IDtoCG create_dto_cg(
            Connection conn,
            SdmDataObject obj,
            Settings settings,
            StringBuilder output_dir_rel_path) throws Exception {

        String root_fn = obj.getPrimaryFile().getNameExt();
        String xml_configs_folder_full_path = obj.getPrimaryFile().getParent().getPath();
        String project_abs_path = NbpPathHelpers.get_root_folder(obj.getPrimaryFile()).getPath();
        return TargetLangUtils.create_dto_cg(root_fn, project_abs_path, xml_configs_folder_full_path, 
                conn, settings, output_dir_rel_path);
    }

    public static IDaoCG create_dao_cg(
            Connection conn,
            SdmDataObject obj,
            Settings settings,
            StringBuilder output_dir_rel_path) throws Exception {

        String root_fn = obj.getPrimaryFile().getNameExt();
        String xml_configs_folder_full_path = obj.getPrimaryFile().getParent().getPath();
        String project_abs_path = NbpPathHelpers.get_root_folder(obj.getPrimaryFile()).getPath();
        return TargetLangUtils.create_dao_cg(root_fn, project_abs_path, xml_configs_folder_full_path, 
                conn, settings, output_dir_rel_path);
    }

    public static String get_root_file_relative_path(FileObject root_folder,
            FileObject file) {

        String fn = file.getNameExt();
        if (RootFileName.JAVA.equals(fn)
                || RootFileName.CPP.equals(fn)
                || //ProfileNames.OBJC.equals(fn) ||
                RootFileName.PHP.equals(fn)
                || RootFileName.PYTHON.equals(fn)) {
            try {
                return NbpPathHelpers.get_relative_path(root_folder, file);
            } catch (Exception e) {
                // e.printStackTrace();
            }
        }
        return null;
    }
}
