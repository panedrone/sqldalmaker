/*
 * Copyright 2011-2020 sqldalmaker@gmail.com
 * SQL DAL Maker Website: http://sqldalmaker.sourceforge.net
 * Read LICENSE.txt in the root of this project/archive for details.
 */
package com.sqldalmaker.intellij;

import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Separator;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.TextEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.sqldalmaker.common.FileSearchHelpers;
import com.sqldalmaker.intellij.ui.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: sqldalmaker@gmail.com
 * Date: 21.06.12
 */
public class SdmActionGroup extends ActionGroup {

    abstract static class SdmAction extends AnAction {

        SdmAction(String text) {
            this.getTemplatePresentation().setText(text, false); // to prevent parsing and replacement of '_'
        }
    }

    private static void enum_root_files(Project project, VirtualFile current_folder, List<String> rel_path_names) {
        @SuppressWarnings("UnsafeVfsRecursion")
        VirtualFile[] children = current_folder.getChildren();
        for (VirtualFile c : children) {
            if (c.isDirectory()) {
                if (!c.getName().equals("bin")) {
                    enum_root_files(project, c, rel_path_names);
                }
            } else {
                String path = IdeaTargetLanguageHelpers.get_root_file_relative_path(project, c);
                if (path != null) {
                    rel_path_names.add(path);
                }
            }
        }
    }

    private List<String> get_root_file_titles(Project project) throws Exception {
        VirtualFile project_root_folder = IdeaHelpers.get_project_base_dir(project);
        List<String> rel_path_names = new ArrayList<String>();
        enum_root_files(project, project_root_folder, rel_path_names);
        return rel_path_names;
    }

    private void add_dto_actions(Project project, List<AnAction> drop_down_actions_list, VirtualFile xml_file) throws Exception {
        String xml_file_rel_path = IdeaHelpers.get_relative_path(project, xml_file);
        SdmAction action_generate = new SdmAction(xml_file_rel_path + " -> Generate All") {
            @Override
            public void actionPerformed(@NotNull AnActionEvent anActionEvent) {
                IdeaCG.generate_all_dto(project, xml_file);
            }
        };
        drop_down_actions_list.add(action_generate);
        SdmAction action_validate = new SdmAction(xml_file_rel_path + " -> Validate All") {
            @Override
            public void actionPerformed(@NotNull AnActionEvent anActionEvent) {
                IdeaCG.validate_all_dto(project, xml_file);
            }
        };
        drop_down_actions_list.add(action_validate);
        drop_down_actions_list.add(Separator.create());
    }

    private void add_dao_actions(Project project, List<AnAction> drop_down_actions_list, VirtualFile xml_file) throws Exception {
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
        drop_down_actions_list.add(Separator.create());
    }

    private void add_common_actions(Project project, List<AnAction> drop_down_actions_list, List<VirtualFile> root_files) throws Exception {
        String root_file_rel_path = IdeaHelpers.get_relative_path(project, root_files.get(0));
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
        drop_down_actions_list.add(Separator.create());
    }

    @NotNull
    @Override
    public AnAction[] getChildren(@Nullable AnActionEvent anActionEvent) {
        try {
            if (anActionEvent == null) {
                return new AnAction[0];
            }
            Project project = anActionEvent.getProject(); // @Nullable
            if (project == null) {
                return new AnAction[0];
            }
            List<String> titles = get_root_file_titles(project);
            if (titles.isEmpty()) {
                return new AnAction[0];
            }
            List<AnAction> drop_down_actions_list = new ArrayList<AnAction>();
            FileEditor editor = FileEditorManager.getInstance(project).getSelectedEditor(); // @Nullable
            if (editor instanceof TextEditor) {
                VirtualFile xml_file = editor.getFile(); // @Nullable
                if (xml_file != null) {
                    VirtualFile xml_file_dir = xml_file.getParent();
                    if (xml_file_dir != null) {
                        List<VirtualFile> root_files = IdeaTargetLanguageHelpers.find_root_files(xml_file_dir);
                        if (root_files.size() == 1) {
                            String name = xml_file.getName();
                            if (FileSearchHelpers.is_dto_xml(name)) {
                                add_dto_actions(project, drop_down_actions_list, xml_file);
                            } else if (FileSearchHelpers.is_dao_xml(name)) {
                                add_dao_actions(project, drop_down_actions_list, xml_file);
                            }
                            if (FileSearchHelpers.is_dto_xml(name) || FileSearchHelpers.is_dao_xml(name)) {
                                if (titles.size() > 1) {
                                    add_common_actions(project, drop_down_actions_list, root_files);
                                }
                            }
                        }
                    }
                }
            }

            for (String title : titles) {
                SdmAction action = new SdmAction(title) {
                    @Override
                    public void actionPerformed(@NotNull AnActionEvent anActionEvent) {
                        try {
                            String rel_path = this.getTemplatePresentation().getText();
                            IdeaEditorHelpers.open_project_file_in_editor_sync(anActionEvent.getProject(), rel_path);
                        } catch (Throwable e) {
                            e.printStackTrace();
                            IdeaMessageHelpers.show_error_in_ui_thread(e);
                        }
                    }
                };

                drop_down_actions_list.add(action);
            }
            AnAction[] arr = new AnAction[drop_down_actions_list.size()];
            return drop_down_actions_list.toArray(arr);
        } catch (/*Exception*/ Throwable e) {
            IdeaMessageHelpers.add_error_to_ide_log("ERROR", e.getMessage());
            e.printStackTrace();
            return new AnAction[0];
        }
    }

    public boolean disableIfNoVisibleChildren() {
        // in other case, it calls getChildren each 3 sec.
        //
        return false;
    }
}