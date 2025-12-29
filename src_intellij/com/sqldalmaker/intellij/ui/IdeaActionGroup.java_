/*
    Copyright 2011-2024 sqldalmaker@gmail.com
    SQL DAL Maker Website: https://sqldalmaker.sourceforge.net/
    Read LICENSE.txt in the root of this project/archive for details.
 */
package com.sqldalmaker.intellij.ui;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.vfs.VirtualFile;
import com.sqldalmaker.cg.Helpers;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
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
 *
 * 05.03.2025 16:30 [+] getActionUpdateThread
 * 25.02.2025 11:00 no menu icon fixed
 * 30.05.2024 20:00 1.299
 * 14.02.2024 18:50 1.294 <dao-class ref="...
 * 28.12.2023 13:12 1.292
 * 01.09.2023 12:21 1.287
 * 18.03.2023 08:46 adapted for python3.11
 * 16.11.2022 08:02 1.269
 * 16.07.2022 13:49 enabled separate modules for dto and dao
 * 09.04.2022 19:15 Revert "Revert "[fix] intellij xml navigation""
 * 30.03.2022 20:33 intellij event log + balloons
 * 17.05.2021 11:28 intellij -> optimized SDM toolbar drop-down
 * 02.08.2020 01:29 fix of xml ==> sql navigation in intellij
 * 10.05.2020 04:51 New approach of field mappings
 * 07.02.2019 19:50 initial commit
 *
 */

// === panedrone: not DefaultActionGroup!!!
// DefaultActionGroup --> This class is used if a set of actions belonging to the group does not change at runtime, which is the majority of cases.

public class IdeaActionGroup extends ActionGroup implements DynamicActionGroup
        // === panedrone: EAP 243 AlwaysVisibleActionGroup (1) (scheduled for removal in a future release)
        //        implements AlwaysVisibleActionGroup
        // --> return 'about' only if empty
{

    //     deprecated with no class --> Deprecated method IconLoader.findIcon
    //     https://plugins.jetbrains.com/docs/intellij/work-with-icons-and-images.html#how-to-organize-and-how-to-use-icons
    private static final Icon SDM_ICON = IconLoader.findIcon("/img/sqldalmaker.png", IdeaActionGroup.class);

    public Icon getIcon() {
        return SDM_ICON;
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        // === panedrone: no constructors in this example:
        // https://github.com/JetBrains/intellij-sdk-docs/blob/main/code_samples/action_basics/src/main/java/org/intellij/sdk/action/CustomDefaultActionGroup.java
        return ActionUpdateThread.BGT;
    }

    @Override
    public void update(AnActionEvent event) {

//    // https://intellij-sdk-docs-cn.github.io/intellij/sdk/docs/tutorials/action_system/grouping_action.html

        // Enable/disable depending on whether user is editing
//        Editor editor = event.getData(CommonDataKeys.EDITOR);
//        event.getPresentation().setEnabled(editor != null);
        // Take this opportunity to set an icon for the menu entry.

        if (!event.getPresentation().isEnabled()) {
            event.getPresentation().setEnabled(true); // always
        }

        if (event.getPresentation().getIcon() != SDM_ICON) { // IDE freeze if no check
            event.getPresentation().setIcon(SDM_ICON);
        }
    }

//    public IdeaActionGroup() { // === panedrone: better to remove constructors at all

    // /        super("SDM===", "SDM", SDM_ICON); // === panedrone: the same signature as in plugin.xml
//        super();
//    }

    // IntelliJ IDEA Ultimate    // 2021.3.3

    // IdeaActionGroup.<init>(String, String, Icon) contains an invoke special instruction referencing an
    // unresolved constructor DefaultActionGroup.<init>(String, String, Icon)

//    public IdeaActionGroup(@Nullable String text, @Nullable String description
//            , @Nullable Icon icon
//    ) {
//        super(text, description, icon); // === panedrone: not called
//    }

    // ActionGroup is disabled. Why?
    // It's working after I make children actions implement DumbAware. What's it? No documentation for it.
    abstract static class SdmAction extends AnAction implements DumbAware {
        SdmAction(String text) {
            this.getTemplatePresentation().setText(text, false); // to prevent parsing and replacement of '_'
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

    private void add_sdm_actions(
            Project project,
            List<AnAction> drop_down_actions_list,
            VirtualFile xml_file) throws Exception {

        String xml_file_rel_path = IdeaHelpers.get_relative_path(project, xml_file);
        SdmAction action_generate = new SdmAction(xml_file_rel_path + " -> Generate All") {
            @Override
            public void actionPerformed(@NotNull AnActionEvent anActionEvent) {
                IdeaCG.action_generate_all_sdm_dto(project, xml_file);
                IdeaCG.action_generate_all_sdm_dao(project, xml_file);
            }
        };
        drop_down_actions_list.add(action_generate);
        SdmAction action_validate = new SdmAction(xml_file_rel_path + " -> Validate All") {
            @Override
            public void actionPerformed(@NotNull AnActionEvent anActionEvent) {
                IdeaCG.action_validate_all_sdm_dto(project, xml_file);
                IdeaCG.action_validate_all_sdm_dao(project, xml_file);
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
                IdeaCG.toolbar_action_generate_external_dao_xml(project, xml_file);
            }
        };
        drop_down_actions_list.add(action_generate);
        SdmAction action_validate = new SdmAction(xml_file_rel_path + " -> Validate") {
            @Override
            public void actionPerformed(@NotNull AnActionEvent anActionEvent) {
                IdeaCG.toolbar_action_action_validate_external_dao_xml(project, xml_file);
            }
        };
        drop_down_actions_list.add(action_validate);
    }

    private String add_xml_file_actions(Project project, List<AnAction> drop_down_actions_list) throws Exception {
        FileEditorManager fm = FileEditorManager.getInstance(project);
        // FileEditor editor = fm.getSelectedEditor(); // since 182.711
        VirtualFile[] files = fm.getSelectedFiles();
        if (files.length == 0) {
            return null;
        }
        VirtualFile xml_file = files[0];
        String ext = xml_file.getExtension();
        if (!"xml".equals(ext)) {
            return null;
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
            return null;
        }
        List<VirtualFile> root_files = IdeaTargetLanguageHelpers.find_root_files(xml_file_dir);
        if (root_files.isEmpty()) {
            return null;
        }
        VirtualFile root_file = root_files.get(0);
        String name = xml_file.getName();
        if (Helpers.is_sdm_xml(name)) {
            add_sdm_actions(project, drop_down_actions_list, xml_file);
        } else if (Helpers.is_dao_xml(name)) {
            add_dao_actions(project, drop_down_actions_list, xml_file);
        }
        if (Helpers.is_sdm_xml(name) || Helpers.is_dao_xml(name)) {
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
            return root_file_rel_path;
        }
//        if (Helpers.is_dao_xml(name)) {
//            add_open_dao_target_action(project, root_file, xml_file, drop_down_actions_list);
//        }
        return null;
    }

    @Override
    public AnAction @NotNull [] getChildren(@Nullable AnActionEvent anActionEvent) {
        try {
            if (anActionEvent == null) {
                return AnAction.EMPTY_ARRAY;
                // return about_only(drop_down_actions_list);
            }
            List<AnAction> drop_down_actions_list = new ArrayList<AnAction>();
            if (anActionEvent.isFromActionToolbar()) {
                // === panedrone: to prevent asking for children if SDM toolbar drop-down is hidden
//                return AnAction.EMPTY_ARRAY;
                return about_only(drop_down_actions_list);
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
                return about_only(drop_down_actions_list);
            }
            String curr_root_file_rel_path = add_xml_file_actions(project, drop_down_actions_list);
            VirtualFile project_base_dir = IdeaHelpers.get_project_base_dir(project);
            List<VirtualFile> root_files = new ArrayList<VirtualFile>();
            IdeaHelpers.enum_root_files(project, project_base_dir, root_files);
            if (curr_root_file_rel_path != null) {
                for (VirtualFile root_file : root_files) {
                    String root_file_rel_path = IdeaHelpers.get_relative_path(project, root_file);
                    if (curr_root_file_rel_path.equals(root_file_rel_path)) {
                        root_files.remove(root_file);
                        break;
                    }
                }
                drop_down_actions_list.add(Separator.create());
            }
            add_common_actions(project, drop_down_actions_list, root_files);
            if (!drop_down_actions_list.isEmpty()) {
                drop_down_actions_list.add(Separator.create());
            }
            add_about_action(drop_down_actions_list);
            AnAction[] arr = new AnAction[drop_down_actions_list.size()];
            return drop_down_actions_list.toArray(arr);
        } catch (/*Exception*/ Throwable e) {
            IdeaMessageHelpers.add_error_to_ide_log("ERROR", e.getMessage());
            // e.printStackTrace();
//            return about_only(drop_down_actions_list);
            return AnAction.EMPTY_ARRAY;
        }
    }

    private void add_about_action(List<AnAction> drop_down_actions_list) {
        SdmAction action = new SdmAction("About") {
            @Override
            public void actionPerformed(@NotNull AnActionEvent anActionEvent) {
                UIDialogAbout.show_modal();
            }
        };
        drop_down_actions_list.add(action);
    }

    private AnAction[] about_only(List<AnAction> drop_down_actions_list) {
        add_about_action(drop_down_actions_list);
        AnAction[] arr = new AnAction[drop_down_actions_list.size()];
        return drop_down_actions_list.toArray(arr);
    }

    // it cannot be overridden because of warnings of intellij checker
//    public boolean disableIfNoVisibleChildren() {
//        // in other case, it calls getChildren each 3 sec.
//        return false;
//    }
}