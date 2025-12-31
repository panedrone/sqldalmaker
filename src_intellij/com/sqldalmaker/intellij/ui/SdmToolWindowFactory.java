package com.sqldalmaker.intellij.ui;

import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.openapi.wm.ex.ToolWindowManagerListener;
import com.intellij.ui.components.ActionLink;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

public class SdmToolWindowFactory implements ToolWindowFactory {

    public static final String ID = "SDM";

    @Override
    public void init(@NotNull ToolWindow toolWindow) {
        Icon icon = IconLoader.getIcon("/META-INF/pluginIcon.svg", SdmToolWindowFactory.class);
        toolWindow.setIcon(icon);
    }

    @Override
    public void createToolWindowContent(@NotNull Project project,
                                        @NotNull ToolWindow toolWindow) {

        JPanel panel = new JPanel(new BorderLayout());
        JLabel label = new JLabel("SDM", SwingConstants.CENTER);
        panel.add(label, BorderLayout.CENTER);

        Content content = ContentFactory.getInstance()
                .createContent(panel, "", false);
        toolWindow.getContentManager().addContent(content);

        project.getMessageBus()
                .connect(toolWindow.getDisposable())
                .subscribe(ToolWindowManagerListener.TOPIC,
                        new ToolWindowManagerListener() {
                            @Override
                            public void stateChanged(@NotNull ToolWindowManager manager) {

                                ToolWindow tw = manager.getToolWindow(ID);

                                if (tw == null) return;
                                if (!tw.isAvailable()) return;
                                if (!tw.isVisible()) return;

                                // Hide immediately (acts as a trigger button)
                                tw.hide(null);

                                List<String> items = buildItemsFromSdm(project);
                                handleItems(project, items);
                            }
                        });
    }

    private static void handleItems(Project project, List<String> items) {
        if (items == null || items.isEmpty()) return;

        if (items.size() == 1) {
            openEditorQuietly(project, items.get(0));
        } else {
            showDialog(project, items);
        }
    }

    private static List<String> buildItemsFromSdm(Project project) {
        List<String> items = new ArrayList<>();
        VirtualFile sdm = findFileByRelativePath(project, ".sdm");

        if (sdm == null) {
            IdeaMessageHelpers.add_error_to_ide_log("ERROR", ".sdm not found in project root");
            return null;
        }

        String[] lines = ReadAction.compute(() -> readLines(sdm));

        if (lines.length == 0) {
            IdeaMessageHelpers.add_error_to_ide_log("ERROR", ".sdm is empty");
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

    private static void showDialog(Project project, List<String> items) {

        new DialogWrapper(project, true) {
            {
                setTitle("SDM");
                init();
            }

            @Override
            protected JComponent createCenterPanel() {

                JPanel panel = new JPanel();
                panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
                panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
                panel.setBackground(UIUtil.getPanelBackground());
                panel.setOpaque(true);

                for (String relativePath : items) {
                    JButton button = createButton(relativePath, project, this);
                    panel.add(button);
                    panel.add(Box.createRigidArea(new Dimension(0, 5)));
                }

                panel.add(Box.createVerticalGlue());

                ActionLink sdmLink = new ActionLink(".sdm", new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        openEditorQuietly(project, ".sdm"); // открыть файл
                        close(DialogWrapper.OK_EXIT_CODE);   // закрыть диалог
                    }
                });

                sdmLink.setAlignmentX(Component.CENTER_ALIGNMENT); // по центру
                panel.add(Box.createRigidArea(new Dimension(0, 10)));
                panel.add(sdmLink);

                return panel;
            }

            @Override
            protected Action @NonNull [] createActions() {
                return new Action[0]; // только кнопка закрытия (X)
            }

            private JButton createButton(String relativePath,
                                         Project project,
                                         DialogWrapper dialog) {

                JButton button = new JButton(relativePath);
                button.setAlignmentX(Component.CENTER_ALIGNMENT);
                button.setMinimumSize(JBUI.size(100, 30));
                button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
                button.setFocusPainted(false);
                button.setContentAreaFilled(true);
                button.setOpaque(true);

                Color defaultBg = UIUtil.getPanelBackground();
                Color hoverBg = UIUtil.getListSelectionBackground(true);
                button.setBackground(defaultBg);

                button.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseEntered(MouseEvent e) {
                        button.setBackground(hoverBg);
                    }

                    @Override
                    public void mouseExited(MouseEvent e) {
                        button.setBackground(defaultBg);
                    }
                });

                button.addActionListener(e -> {
                    openEditorQuietly(project, relativePath);
                    dialog.close(DialogWrapper.OK_EXIT_CODE);
                });

                return button;
            }

        }.show();
    }

    private static void openEditorQuietly(Project project, String relativePath) {

        VirtualFile file = findFileByRelativePath(project, relativePath);
        if (file == null) {
            IdeaMessageHelpers.add_error_to_ide_log("ERROR", relativePath + " not found");
            return;
        }

        FileEditorManager fem = FileEditorManager.getInstance(project);

        for (VirtualFile f : fem.getOpenFiles()) {
            if (f.equals(file)) {
                fem.openFile(f, false);
                return;
            }
        }

        fem.openFile(file, false);
    }

    private static VirtualFile findFileByRelativePath(Project project, String relativePath) {
        String basePath = project.getBasePath();
        VirtualFile baseDir = basePath == null ? null :
                LocalFileSystem.getInstance().findFileByPath(basePath);
        if (baseDir == null) return null;
        return baseDir.findFileByRelativePath(relativePath);
    }

    private static String[] readLines(VirtualFile file) {
        String text;

        com.intellij.openapi.editor.Document document =
                com.intellij.openapi.fileEditor.FileDocumentManager
                        .getInstance().getDocument(file);

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
            int hash = line.indexOf('#');
            if (hash >= 0) {
                line = line.substring(0, hash);
            }
            line = line.trim();
            if (!line.isEmpty()) {
                result.add(line);
            }
        }

        return result.toArray(new String[0]);
    }
}
