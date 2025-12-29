package com.sqldalmaker.intellij.ui;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.openapi.wm.ex.ToolWindowManagerListener;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.ListPopup;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.Collection;
import java.util.Collections;

public class SdmToolWindowFactory implements ToolWindowFactory {

    private static final String TOOLWINDOW_ID = "SDM";

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        // Контент намеренно не добавляем
    }

    @Override
    public void init(@NotNull ToolWindow toolWindow) {
        // Окно не отображается, кнопка висит спокойно
        toolWindow.setToHideOnEmptyContent(true);

        Project project = toolWindow.getProject();

        project.getMessageBus()
                .connect(toolWindow.getDisposable()) // ✅ ВАЖНО
                .subscribe(ToolWindowManagerListener.TOPIC,
                        new ToolWindowManagerListener() {
                            @Override
                            public void toolWindowShown(@NotNull ToolWindow shownToolWindow) {
                                if (!TOOLWINDOW_ID.equals(shownToolWindow.getId())) return;

                                runMyAction(project);

                                shownToolWindow.hide(null);
                            }
                        });
    }

    public static VirtualFile findFileByRelativePath(Project project, String relativePath) {
        String basePath = project.getBasePath(); // вместо getBaseDir()
        VirtualFile baseDir = basePath == null ? null : LocalFileSystem.getInstance().findFileByPath(basePath);
        if (baseDir == null) {
            return null;
        }
        // ищем файл по относительному пути
        return baseDir.findFileByRelativePath(relativePath);
    }

    public static String[] readLines(VirtualFile file) {
        Document document = FileDocumentManager.getInstance().getDocument(file);
        if (document != null) {
            return document.getText().split("\n");
        }
        // fallback если файл не открыт как текст
        try {
            return new String(file.contentsToByteArray(), file.getCharset())
                    .split("\n");
        } catch (Exception e) {
            return new String[0];
        }
    }

    private void runMyAction(Project project) {
        new Task.Backgroundable(project, "Processing .sdm file", false) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                final VirtualFile sdm = findFileByRelativePath(project, ".sdm");
                if (sdm == null) {
                    IdeaMessageHelpers.add_error_to_ide_log("ERROR", ".sdm not found in project root");
                    return;
                }
                final String[] lines = ReadAction.compute(() -> readLines(sdm));
                if (lines.length == 0 || lines[0].trim().isEmpty()) {
                    IdeaMessageHelpers.add_error_to_ide_log("ERROR", ".sdm is empty, you need to specify at least one file path, i.e. sdm/php.dal, sdm/java.dal, sdm/go.dal, etc.");
                    return;
                }
                final VirtualFile root_file = findFileByRelativePath(project, lines[0]);
                if (root_file == null) {
                    IdeaMessageHelpers.add_error_to_ide_log("ERROR", lines[0] + " not found");
                    return;
                }
                if (root_file.isDirectory()) {
                    IdeaMessageHelpers.add_error_to_ide_log("ERROR", lines[0] + " is a directory");
                    return;
                }
                // Открытие файла в редакторе — на EDT
                ApplicationManager.getApplication().invokeLater(() -> {
                    try {
                        String root_file_rel_path = IdeaHelpers.get_relative_path(project, root_file);
                        IdeaEditorHelpers.open_project_file_in_editor_sync(project, root_file_rel_path);
                    } catch (Exception e) {
                        IdeaMessageHelpers.add_error_to_ide_log("ERROR", e.getMessage());
                    }
                });
            }
        }.queue();
    }
}
