/*
    Copyright 2011-2026 sqldalmaker@gmail.com
    SQL DAL Maker Website: https://sqldalmaker.sourceforge.net/
    Read LICENSE.txt in the root of this project/archive for details.
 */
package com.sqldalmaker.intellij.ui;

import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.fileTypes.FileTypes;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.LightVirtualFile;
import com.sqldalmaker.cg.Helpers;
import com.sqldalmaker.common.Const;
import com.sqldalmaker.common.InternalException;
import com.sqldalmaker.common.SdmUtils;
import com.sqldalmaker.common.XmlHelpers;
import com.sqldalmaker.jaxb.sdm.DaoClass;
import com.sqldalmaker.jaxb.sdm.DtoClass;
import com.sqldalmaker.jaxb.sdm.ObjectFactory;
import com.sqldalmaker.jaxb.sdm.Sdm;
import com.sqldalmaker.jaxb.settings.Settings;

import java.io.File;
import java.sql.Connection;

/**
 * Created with IntelliJ IDEA.
 * User: sqldalmaker@gmail.com
 * Date: 21.06.12
 */
public class IdeaEditorHelpers {

    public static void open_in_editor_sync(Project project, VirtualFile file) {
        // .sdm opens quietly, without focus
        open_in_editor(project, file, !file.getName().endsWith(".sdm"));
    }

    // FileEditorManager.openFile(...) is deprecated since 263, and its replacement requestOpenFile is missing
    // in the older builds. navigateInEditor opens the file synchronously in all of them (an already open one
    // is activated, not opened twice) and says whether there is an editor for it.
    public static boolean open_in_editor(Project project, VirtualFile file, boolean focus) {
        return new OpenFileDescriptor(project, file).navigateInEditor(project, focus);
    }

    public static VirtualFile find_case_sensitive(VirtualFile dir, String rel_path) throws Exception {
        VirtualFile file = dir.findFileByRelativePath(rel_path);
        if (file == null) {
            throw new Exception("File not found: " + rel_path);
        }
        String file_name = new File(rel_path).getName();
        String vf_name = file.getName();
        if (!vf_name.equals(file_name)) {
            throw new Exception("File not found (case-sensitive): " + file_name);
        }
        return file;
    }

    public static void open_local_file_in_editor_sync(Project project, VirtualFile root_file, String rel_path) throws Exception {
        VirtualFile dir = root_file.getParent();
        VirtualFile file = find_case_sensitive(dir, rel_path);
        open_in_editor_sync(project, file);
    }

    public static void open_local_file_in_editor_sync(Project project, VirtualFile file) {
        open_in_editor_sync(project, file);
    }

    public static void open_project_file_in_editor_sync(Project project, String rel_path) throws Exception {
        VirtualFile project_dir = IdeaHelpers.get_project_base_dir(project);
        VirtualFile file = find_case_sensitive(project_dir, rel_path);
        open_in_editor_sync(project, file);
    }

    public static void open_text_in_new_editor(Project project, String file_name, String text) {
        String virtual_file_name = "_" + file_name; // '%' throws URI exception in NB
        // http://grepcode.com/file/repository.grepcode.com/java/ext/com.jetbrains/intellij-idea/9.0.4/com/intellij/openapi/vfs/VirtualFile.java
        // If an in-memory implementation of VirtualFile is required, LightVirtualFile from the com.intellij.testFramework package (Extended API) can be used.
        try {
            open_light_file(project, virtual_file_name, text);
        } catch (Throwable e1) {
            // e1.printStackTrace();
            IdeaMessageHelpers.show_error_in_ui_thread(e1);
        }
    }

    private static void open_light_file(Project project, String virtual_file_name, String text) throws Exception {
        // http://grepcode.com/file/repository.grepcode.com/java/ext/com.jetbrains/intellij-idea/9.0.4/com/intellij/openapi/vfs/VirtualFile.java
        // If an in-memory implementation of VirtualFile is required, LightVirtualFile from the com.intellij.testFramework package (Extended API) can be used.
        VirtualFile file;
        boolean opened;
        // Big problems with \r\n:
        // 1. copy-paste do not work
        // 2. VM files are not opened with exception like 'Wrong line separators...'
        //
        // Maybe, the hint is in here: 'Line breaks in a document are always normalized to \n.'
        // (http://confluence.jetbrains.com/display/IDEADEV/IntelliJ+IDEA+Architectural+Overview)
        //
        // '\r' for mac os?
        // Mac OS up to version 9 and OS-9 (http://en.wikipedia.org/wiki/Line_breaking_character)
        text = text.replace("\r\n", "\n");
        try {
            file = new LightVirtualFile(virtual_file_name, text);
            opened = open_in_editor(project, file, true); // for VM: it may throw something like 'Wrong line separators...'
        } catch (Throwable e) {
            // for VM: it will show error log message anyway, but il will open it and will not throw, at least
            file = new LightVirtualFile(virtual_file_name, FileTypes.PLAIN_TEXT, text);
            opened = open_in_editor(project, file, true);
        }
        if (!opened) {
            file = new LightVirtualFile(virtual_file_name, FileTypes.PLAIN_TEXT, text);
            if (!open_in_editor(project, file, true)) {
                throw new Exception("Cannot open the file in editor: " + virtual_file_name);
            }
        }
    }

    public static void open_or_activate_jar_resource_in_editor(Project project, String res_name, String title) {
        open_or_activate_jar_file_in_editor(project, "resources", res_name, title);
    }

    public static void open_or_activate_jar_file_in_editor(
            Project project,
            String res_path,
            String res_name,
            String title) {

        FileEditorManager fem = FileEditorManager.getInstance(project);
        String virtual_file_name = "%" + title; // to make unique
        VirtualFile file = null;
        VirtualFile[] open_files = fem.getOpenFiles();
        for (VirtualFile open_file : open_files) {
            if (virtual_file_name.equals(open_file.getName())) {
                file = open_file;
                break;
            }
        }
        try {
            if (file != null) {
                open_in_editor(project, file, true);
            } else {
                String text_from_jar = IdeaHelpers.read_from_jar(res_path, res_name);
                open_light_file(project, virtual_file_name, text_from_jar);
            }
        } catch (Throwable e1) {
            // e1.printStackTrace();
            IdeaMessageHelpers.show_error_in_ui_thread(e1);
        }
    }

    public static void open_sdm_xml_sync(Project project, VirtualFile root_file) {
        try {
            VirtualFile sdm_xml = root_file.getParent().findFileByRelativePath(Const.SDM_XML);
            if (sdm_xml == null) {
                throw new InternalException("File '" + Const.SDM_XML + "' not found");
            }
            open_in_editor_sync(project, sdm_xml);
        } catch (Exception e) {
            IdeaMessageHelpers.show_error_in_ui_thread(e);
            // // e.printStackTrace();
        }
    }

    public static void open_settings_xml_sync(Project project, VirtualFile root_file) {
        try {
            VirtualFile settings_xml = root_file.getParent().findFileByRelativePath(Const.SETTINGS_XML);
            if (settings_xml == null) {
                throw new InternalException("File '" + Const.SETTINGS_XML + "' not found");
            }
            open_in_editor_sync(project, settings_xml);
        } catch (Exception e) {
            IdeaMessageHelpers.show_error_in_ui_thread(e);
            // // e.printStackTrace();
        }
    }

    public static void gen_field_wizard_jaxb(
            String class_name,
            String ref,
            Project project,
            VirtualFile root_file) throws Exception {

        Settings settings = IdeaHelpers.load_settings(root_file);
        Connection con = IdeaHelpers.get_connection(project, settings);
        try {
            ObjectFactory object_factory = new ObjectFactory();
            Sdm sdm = object_factory.createSdm();
            DtoClass cls = object_factory.createDtoClass();
            cls.setName(class_name);
            cls.setRef(ref);
            sdm.getDtoClass().add(cls);
            VirtualFile project_dir = IdeaHelpers.get_project_base_dir(project);
            final String module_root = project_dir.getPath();
            String sql_root_folder_full_path = Helpers.concat_path(module_root, settings.getFolders().getSql());
            SdmUtils.gen_field_wizard_jaxb(settings, con, object_factory, cls, sql_root_folder_full_path);
            open_sdm_xml_in_editor(object_factory, project, sdm);
        } finally {
            con.close();
        }
    }

    public static void open_sdm_xml_in_editor(ObjectFactory object_factory, Project project, Sdm sdm) throws Exception {
        String text = XmlHelpers.get_sdm_xml_text(object_factory, sdm);
        String[] parts = text.split("\\?>");
        text = parts[0] + Const.COMMENT_GENERATED_SDM_XML + parts[1];
        open_text_in_new_editor(project, "dto.xml", text);
    }

    public static void open_dao_xml_in_editor(
            Project project,
            com.sqldalmaker.jaxb.sdm.ObjectFactory object_factory,
            String file_name,
            DaoClass root) throws Exception {

        String text = XmlHelpers.get_dao_xml_text(object_factory, root);
        String[] parts = text.split("\\?>");
        text = parts[0] + Const.COMMENT_GENERATED_DAO_XML + parts[1];
        open_text_in_new_editor(project, file_name, text);
    }
}
