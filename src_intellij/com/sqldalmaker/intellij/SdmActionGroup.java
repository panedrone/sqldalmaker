/*
 * Copyright 2011-2018 sqldalmaker@gmail.com
 * SQL DAL Maker Website: http://sqldalmaker.sourceforge.net
 * Read LICENSE.txt in the root of this project/archive for details.
 */
package com.sqldalmaker.intellij;

import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.sqldalmaker.intellij.ui.IdeaEditorHelpers;
import com.sqldalmaker.intellij.ui.IdeaHelpers;
import com.sqldalmaker.intellij.ui.IdeaMessageHelpers;
import com.sqldalmaker.intellij.ui.IdeaTargetLanguageHelpers;
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

    abstract class SdmAction extends AnAction {

        SdmAction(String text) {

            this.getTemplatePresentation().setText(text, false); // to prevent parsing and replacement of '_'
        }
    }

    private static void enum_root_files(Project project,
                                        VirtualFile current_folder, List<String> rel_path_names) throws Exception {

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

            List<String> titles = get_root_file_titles(anActionEvent.getProject());

            List<SdmAction> res = new ArrayList<SdmAction>();

            for (String title : titles) {

                SdmAction action = new SdmAction(title) {

                    @Override
                    public void actionPerformed(AnActionEvent anActionEvent) {

                        try {

                            String rel_path = this.getTemplatePresentation().getText();

                            IdeaEditorHelpers.open_module_file_in_editor(anActionEvent.getProject(), rel_path);

                        } catch (Throwable e) {

                            e.printStackTrace();

                            IdeaMessageHelpers.show_error_in_ui_thread(e);
                        }
                    }
                };

                res.add(action);
            }

            return res.toArray(new AnAction[res.size()]);

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