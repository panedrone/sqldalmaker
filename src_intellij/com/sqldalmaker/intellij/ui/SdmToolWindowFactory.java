/*
    Copyright 2011-2026 sqldalmaker@gmail.com
    SQL DAL Maker Website: https://sqldalmaker.sourceforge.net/
    Read LICENSE.txt in the root of this project/archive for details.
 */
package com.sqldalmaker.intellij.ui;

import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.openapi.wm.ex.ToolWindowManagerListener;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/*
 * @author sqldalmaker@gmail.com
 *
 * 30.12.2024 20:00 1.314
 */
public class SdmToolWindowFactory implements ToolWindowFactory {

    private static final String ID = "SDM";
    private List<String> currentItems;

    @Override
    public void createToolWindowContent(@NotNull Project project,
                                        @NotNull ToolWindow toolWindow) {
        // ToolWindow UI is not used
    }

    @Override
    public void init(@NotNull ToolWindow toolWindow) {
        toolWindow.setToHideOnEmptyContent(true);

        Project project = toolWindow.getProject();

        project.getMessageBus()
                .connect(toolWindow.getDisposable())
                .subscribe(ToolWindowManagerListener.TOPIC,
                        new ToolWindowManagerListener() {
                            @Override
                            public void toolWindowShown(@NotNull ToolWindow tw) {
                                if (!ID.equals(tw.getId())) return;

                                List<String> items = buildItemsFromSdm(project);
                                if (items == null || items.isEmpty()) return;

                                tw.hide(null);

                                currentItems = items;
                                handleItems(project, currentItems);
                            }
                        });
    }

    /** Centralized processing of SDM items */
    private void handleItems(Project project, List<String> items) {
        if (items.size() == 1) {
            openEditorQuietly(project, items.get(0));
        } else {
            showDialog(project, items);
        }
        currentItems = null; // end of lifecycle
    }

    private List<String> buildItemsFromSdm(Project project) {
        List<String> items = new ArrayList<>();
        VirtualFile sdm = findFileByRelativePath(project, ".sdm");
        if (sdm == null) {
            IdeaMessageHelpers.add_error_to_ide_log("ERROR", ".sdm not found in project root");
            return null;
        }

        String[] lines = ReadAction.compute(() -> readLines(sdm));

        if (lines.length == 0 || lines[0].trim().isEmpty()) {
            IdeaMessageHelpers.add_error_to_ide_log("ERROR",
                    ".sdm is empty, you need to specify at least one file path");
            return null;
        }

        for (String line : lines) {
            String path = line.trim();
            if (path.isEmpty()) continue;
            VirtualFile file = findFileByRelativePath(project, path);
            if (file == null) {
                IdeaMessageHelpers.add_error_to_ide_log("ERROR", path + " not found");
                continue;
            }
            if (file.isDirectory()) {
                IdeaMessageHelpers.add_error_to_ide_log("ERROR", path + " is a directory");
                continue;
            }
            items.add(path);
        }

        return items;
    }

    private void showDialog(Project project, List<String> items) {
        currentItems = null; // lifecycle end

        new DialogWrapper(project, true) {
            {
                setTitle("SDM");
                init();
            }

            @Override
            protected @NonNull JComponent createCenterPanel() {
                JPanel panel = new JPanel();
                panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
                panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
                panel.setBackground(UIUtil.getPanelBackground());
                panel.setOpaque(true);

                for (String relativePath : items) {
                    JButton button = createJetBrainsButton(relativePath, project, this);
                    panel.add(button);
                    panel.add(Box.createRigidArea(new Dimension(0, 5)));
                }

                panel.setMinimumSize(new Dimension(250, panel.getPreferredSize().height));
                return panel;
            }

            @Override
            protected Action @NonNull [] createActions() {
                return new Action[0]; // only close button (X)
            }

            private JButton createJetBrainsButton(String relativePath, Project project, DialogWrapper dialog) {
                JButton button = new JButton(relativePath);
                button.setAlignmentX(Component.CENTER_ALIGNMENT);
                button.setMinimumSize(JBUI.size(100, 30));
                button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
                button.setFocusPainted(false);
                button.setContentAreaFilled(true);
                button.setOpaque(true);
                button.setBorderPainted(true);

                Color defaultBg = UIUtil.getPanelBackground();
                Color hoverBg = UIUtil.getListSelectionBackground(true);
                button.setBackground(defaultBg);

                button.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseEntered(MouseEvent e) { button.setBackground(hoverBg); }
                    @Override
                    public void mouseExited(MouseEvent e) { button.setBackground(defaultBg); }
                });

                button.addActionListener(e -> {
                    openEditorQuietly(project, relativePath); // opens the file in editor
                    dialog.close(DialogWrapper.OK_EXIT_CODE);   // closes the dialog
                });

                return button;
            }
        }.show();
    }

    /**
     * Quiet editor opening: do not force focus for .sdm files
     * fem.openFile(file, false):
     *   false = do not request focus immediately, let IDE handle it automatically
     */
    private void openEditorQuietly(Project project, String relativePath) {
        VirtualFile file = findFileByRelativePath(project, relativePath);
        if (file == null) {
            IdeaMessageHelpers.add_error_to_ide_log("ERROR", relativePath + " not found");
            return;
        }

        FileEditorManager fem = FileEditorManager.getInstance(project);

        // If the file is already opened, activate the tab without forcing focus
        for (VirtualFile f : fem.getOpenFiles()) {
            if (f.equals(file)) {
                fem.openFile(f, false); // do not force focus
                return;
            }
        }

        // New file → open without forcing focus
        fem.openFile(file, false); // do not force focus
    }

    // ---------------- Helper functions ----------------

    public static VirtualFile findFileByRelativePath(Project project, String relativePath) {
        String basePath = project.getBasePath();
        VirtualFile baseDir = basePath == null ? null : LocalFileSystem.getInstance().findFileByPath(basePath);
        if (baseDir == null) return null;
        return baseDir.findFileByRelativePath(relativePath);
    }

    public static String[] readLines(VirtualFile file) {
        String text;

        com.intellij.openapi.editor.Document document =
                com.intellij.openapi.fileEditor.FileDocumentManager.getInstance().getDocument(file);

        if (document != null) {
            text = document.getText();
        } else {
            try {
                text = new String(file.contentsToByteArray(), file.getCharset());
            } catch (Exception e) {
                return new String[0];
            }
        }

        List<String> result = new ArrayList<>();

        for (String line : text.split("\n")) {
            int hashIndex = line.indexOf('#');
            if (hashIndex >= 0) {
                line = line.substring(0, hashIndex);
            }

            line = line.trim();
            if (!line.isEmpty()) {
                result.add(line);
            }
        }

        return result.toArray(new String[0]);
    }
}
