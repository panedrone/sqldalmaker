/*
 * Copyright 2011-2018 sqldalmaker@gmail.com
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

    @NotNull
    @Override
    public AnAction[] getChildren(@Nullable AnActionEvent anActionEvent) {
        try {
            if (anActionEvent == null) {
                return new AnAction[0];
            }
            List<String> titles = get_root_file_titles(anActionEvent.getProject());
            List<AnAction> drop_down_actions_list = new ArrayList<AnAction>();
            Project project = anActionEvent.getProject(); // @Nullable
            if (project != null) {
                FileEditor editor = FileEditorManager.getInstance(project).getSelectedEditor(); // @Nullable
                /*if (editor instanceof RootFileEditor) {
                    //
                } else */
                if (editor instanceof TextEditor) {
                    VirtualFile xml_file = editor.getFile();
                    VirtualFile xml_file_dir = xml_file.getParent();
                    if (xml_file_dir != null) {
                        List<VirtualFile> root_files = IdeaTargetLanguageHelpers.find_root_files(xml_file_dir);
                        if (root_files.size() == 1) {
                            String xml_file_rel_path = IdeaHelpers.get_relative_path(project, xml_file);
                            String name = xml_file.getName();
                            if (FileSearchHelpers.is_dto_xml(name)) {
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
                            } else if (FileSearchHelpers.is_dao_xml(name)) {
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
                            //////////////////////////////////////////
                            if (FileSearchHelpers.is_dto_xml(name) || FileSearchHelpers.is_dao_xml(name)) {
                                String root_file_rel_path = IdeaHelpers.get_relative_path(project, root_files.get(0));
                                SdmAction action_dto = new SdmAction(root_file_rel_path + " -> DTO") {
                                    @Override
                                    public void actionPerformed(@NotNull AnActionEvent anActionEvent) {
                                        try {
                                            IdeaEditorHelpers.open_project_file_in_editor_sync(anActionEvent.getProject(), root_file_rel_path);
                                            FileEditor editor = FileEditorManager.getInstance(project).getSelectedEditor(); // @Nullable
                                            if (editor instanceof RootFileEditor) {
                                                RootFileEditor rfe = (RootFileEditor) editor;
                                                rfe.openDTO();
                                            }
                                        } catch (Exception e) {
                                            IdeaMessageHelpers.add_error_to_ide_log("ERROR", e.getMessage());
                                        }
                                    }
                                };
                                drop_down_actions_list.add(action_dto);
                                SdmAction action_dao = new SdmAction(root_file_rel_path + " -> DAO") {
                                    @Override
                                    public void actionPerformed(@NotNull AnActionEvent anActionEvent) {
                                        try {
                                            IdeaEditorHelpers.open_project_file_in_editor_sync(anActionEvent.getProject(), root_file_rel_path);
                                            FileEditor editor = FileEditorManager.getInstance(project).getSelectedEditor(); // @Nullable
                                            if (editor instanceof RootFileEditor) {
                                                RootFileEditor rfe = (RootFileEditor) editor;
                                                rfe.openDAO();
                                            }
                                        } catch (Exception e) {
                                            IdeaMessageHelpers.add_error_to_ide_log("ERROR", e.getMessage());
                                        }
                                    }
                                };
                                drop_down_actions_list.add(action_dao);
                                drop_down_actions_list.add(Separator.create());
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
        } catch (Exception e) {
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