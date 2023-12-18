/*
    Copyright 2011-2023 sqldalmaker@gmail.com
    SQL DAL Maker Website: https://sqldalmaker.sourceforge.net/
    Read LICENSE.txt in the root of this project/archive for details.
 */
package com.sqldalmaker.intellij.ui;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.sqldalmaker.cg.Helpers;
import com.sqldalmaker.common.FileSearchHelpers;
import com.sqldalmaker.jaxb.settings.Settings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/*
 * Created with IntelliJ IDEA.
 * User: sqldalmaker@gmail.com
 * Date: 21.06.12
 * Google --> intellij ActionGroup always enabled
 * -->
 * ActionGroup is disabled. Why?
 * <a href="https://intellij-support.jetbrains.com/hc/en-us/community/posts/360008627020-ActionGroup-is-disabled-Why-">...</a>
 */
public class IdeaActionGroup extends ActionGroup implements AlwaysVisibleActionGroup {

    abstract static class SdmAction extends AnAction {
        SdmAction(String text) {
            this.getTemplatePresentation().setText(text, false); // to prevent parsing and replacement of '_'
        }
    }

    private static void enum_root_files(
            Project project,
            VirtualFile current_folder,
            List<VirtualFile> root_files) {

        @SuppressWarnings("UnsafeVfsRecursion") VirtualFile[] children = current_folder.getChildren();
        for (VirtualFile c : children) {
            if (c.isDirectory()) {
                if (!c.getName().equals("bin")) {
                    enum_root_files(project, c, root_files);
                }
            } else {
                String path = IdeaTargetLanguageHelpers.get_root_file_relative_path(project, c);
                if (path != null) {
                    root_files.add(c);
                }
            }
        }
    }

    private void add_common_actions(
            Project project,
            List<AnAction> drop_down_actions_list,
            List<VirtualFile> root_files) throws Exception {

        for (VirtualFile root_file : root_files) {
            String root_file_rel_path = IdeaHelpers.get_relative_path(project, root_file);
            SdmAction action = new SdmAction(root_file_rel_path) {
                @Override
                public void actionPerformed(@NotNull AnActionEvent anActionEvent) {
                    try {
                        IdeaEditorHelpers.open_project_file_in_editor_sync(anActionEvent.getProject(), root_file_rel_path);
                    } catch (Exception e) {
                        IdeaMessageHelpers.add_error_to_ide_log("ERROR", e.getMessage());
                    }
                }
            };
            drop_down_actions_list.add(action);
        }
    }

    private void add_about_actions(List<AnAction> drop_down_actions_list) {
        SdmAction action = new SdmAction("About") {
            @Override
            public void actionPerformed(@NotNull AnActionEvent anActionEvent) {
                UIDialogAbout.show_modal();
            }
        };
        drop_down_actions_list.add(action);
    }

    private void add_sdm_actions(
            Project project,
            List<AnAction> drop_down_actions_list,
            VirtualFile xml_file) throws Exception {

        String xml_file_rel_path = IdeaHelpers.get_relative_path(project, xml_file);
        SdmAction action_generate = new SdmAction(xml_file_rel_path + " -> Generate All") {
            @Override
            public void actionPerformed(@NotNull AnActionEvent anActionEvent) {
                IdeaCG.generate_all_dto(project, xml_file);
                IdeaCG.generate_all_sdm_dao(project, xml_file);
            }
        };
        drop_down_actions_list.add(action_generate);
        SdmAction action_validate = new SdmAction(xml_file_rel_path + " -> Validate All") {
            @Override
            public void actionPerformed(@NotNull AnActionEvent anActionEvent) {
                IdeaCG.validate_all_dto(project, xml_file);
                IdeaCG.validate_all_sdm_dao(project, xml_file);
            }
        };
        drop_down_actions_list.add(action_validate);
    }

    private void add_dao_actions(
            Project project,
            List<AnAction> drop_down_actions_list,
            VirtualFile xml_file) throws Exception {

        String xml_file_rel_path = IdeaHelpers.get_relative_path(project, xml_file);
        SdmAction action_generate = new SdmAction(xml_file_rel_path + " -> Generate") {
            @Override
            public void actionPerformed(@NotNull AnActionEvent anActionEvent) {
                IdeaCG.generate_dao(project, xml_file);
            }
        };
        drop_down_actions_list.add(action_generate);
        SdmAction action_validate = new SdmAction(xml_file_rel_path + " -> Validate") {
            @Override
            public void actionPerformed(@NotNull AnActionEvent anActionEvent) {
                IdeaCG.validate_dao(project, xml_file);
            }
        };
        drop_down_actions_list.add(action_validate);
    }

    private void add_open_dao_target_action(
            Project project,
            VirtualFile root_file,
            VirtualFile xml_file,
            List<AnAction> drop_down_actions_list) throws Exception {

        Settings settings = IdeaHelpers.load_settings(root_file);
        String xml_file_path = xml_file.getPath();
        String dao_class_name = Helpers.get_dao_class_name(xml_file_path);
        String rel_path = IdeaTargetLanguageHelpers.get_target_folder_rel_path(project, root_file, settings, dao_class_name, settings.getDao().getScope());
        SdmAction action_goto_target = new SdmAction(rel_path) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent anActionEvent) {
                try {
                    IdeaTargetLanguageHelpers.open_dao_sync(project, root_file, settings, dao_class_name);
                } catch (Exception e) {
                    // e.printStackTrace();
                    IdeaMessageHelpers.show_error_in_ui_thread(e);
                }
            }
        };
        drop_down_actions_list.add(action_goto_target);
    }

    private boolean add_xml_file_actions(
            Project project,
            List<AnAction> drop_down_actions_list) throws Exception {

        FileEditorManager fm = FileEditorManager.getInstance(project);
        // FileEditor editor = fm.getSelectedEditor(); // since 182.711
        VirtualFile[] files = fm.getSelectedFiles();
        if (files.length == 0) {
            return false;
        }
        VirtualFile xml_file = files[0];
        String ext = xml_file.getExtension();
        if (!"xml".equals(ext)) {
            return false;
        }
        // === panedrone: getSelectedEditor throws ProcessCanceledException on Goland 2022
//        FileEditor editor = files.length == 0 ? null : fm.getSelectedEditor(files[0]);
//        if (!(editor instanceof TextEditor)) {
//            return false;
//        }
//        VirtualFile xml_file = editor.getFile(); // @Nullable
//        if (xml_file == null) {
//            return false;
//        }
        VirtualFile xml_file_dir = xml_file.getParent();
        if (xml_file_dir == null) {
            return false;
        }
        List<VirtualFile> root_files = IdeaTargetLanguageHelpers.find_root_files(xml_file_dir);
        if (root_files.isEmpty()) {
            return false;
        }
        VirtualFile root_file = root_files.get(0);
        String name = xml_file.getName();
        if (FileSearchHelpers.is_sdm_xml(name)) {
            add_sdm_actions(project, drop_down_actions_list, xml_file);
        } else if (FileSearchHelpers.is_dao_xml(name)) {
            add_dao_actions(project, drop_down_actions_list, xml_file);
        }
        if (FileSearchHelpers.is_sdm_xml(name) || FileSearchHelpers.is_dao_xml(name)) {
            String root_file_rel_path = IdeaHelpers.get_relative_path(project, root_file);
            SdmAction action = new SdmAction(root_file_rel_path) {
                @Override
                public void actionPerformed(@NotNull AnActionEvent anActionEvent) {
                    try {
                        IdeaEditorHelpers.open_project_file_in_editor_sync(anActionEvent.getProject(), root_file_rel_path);
                    } catch (Exception e) {
                        IdeaMessageHelpers.add_error_to_ide_log("ERROR", e.getMessage());
                    }
                }
            };
            drop_down_actions_list.add(action);
        }
        if (FileSearchHelpers.is_dao_xml(name)) {
            add_open_dao_target_action(project, root_file, xml_file, drop_down_actions_list);
        }
        return true;
    }

    @Override
    public AnAction @NotNull [] getChildren(@Nullable AnActionEvent anActionEvent) {
        try {
            if (anActionEvent == null) {
                return AnAction.EMPTY_ARRAY;
            }
            if (anActionEvent.isFromActionToolbar()) {
                // === panedrone: to prevent asking for children if SDM toolbar drop-down is hidden
                return AnAction.EMPTY_ARRAY;
            }
            Project project = null;
            try {
                // === panedrone: anActionEvent.getProject() throws ProcessCanceledException on Goland 2022
                project = anActionEvent.getProject(); // @Nullable
            } catch (Throwable t) {
                Project[] pp = ProjectManager.getInstance().getOpenProjects();
                if (pp.length > 0) {
                    project = pp[0];
                }
            }
            if (project == null) {
                return AnAction.EMPTY_ARRAY;
            }
            List<AnAction> drop_down_actions_list = new ArrayList<AnAction>();
            add_xml_file_actions(project, drop_down_actions_list);
            // there may be several MP in one project. start searching from project_base_dir:
            VirtualFile project_base_dir = IdeaHelpers.get_project_base_dir(project);
            List<VirtualFile> root_files = new ArrayList<VirtualFile>();
            enum_root_files(project, project_base_dir, root_files);
            if (drop_down_actions_list.isEmpty()) {
                add_common_actions(project, drop_down_actions_list, root_files);
            } else {
                if (root_files.size() > 1) {
                    drop_down_actions_list.add(Separator.create());
                    add_common_actions(project, drop_down_actions_list, root_files);
                }
            }
            add_about_actions(drop_down_actions_list);
            AnAction[] arr = new AnAction[drop_down_actions_list.size()];
            return drop_down_actions_list.toArray(arr);
        } catch (/*Exception*/ Throwable e) {
            IdeaMessageHelpers.add_error_to_ide_log("ERROR", e.getMessage());
            // e.printStackTrace();
            return AnAction.EMPTY_ARRAY;
        }
    }

    // it cannot be overridden because of warnings of intellij checker
//    public boolean disableIfNoVisibleChildren() {
//        // in other case, it calls getChildren each 3 sec.
//        return false;
//    }
}