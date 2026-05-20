package com.sqldalmaker.intellij.ui;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.components.ActionLink;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: sqldalmaker@gmail.com
 * Date: 2026-02-04
 */
public class SdmJbToolWindowFactory implements ToolWindowFactory {

    public static final String ID = "SDM";
    public static final String SEPARATOR_PREFIX = "---SDM_SEPARATOR---";

    @Override
    public void init(@NotNull ToolWindow toolWindow) {
        Icon icon = IconLoader.getIcon("/META-INF/pluginIcon.svg", SdmJbToolWindowFactory.class);
        toolWindow.setIcon(icon);
    }

    @Override
    public void createToolWindowContent(@NotNull final Project project, @NotNull final ToolWindow toolWindow) {

        final JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));
        buttonPanel.setBorder(JBUI.Borders.empty(10));
//        buttonPanel.setPreferredSize(new Dimension(400, 400));

        {
            JPanel topAnchor = new JPanel(new BorderLayout());
            topAnchor.add(buttonPanel, BorderLayout.NORTH);
            JBScrollPane scrollablePanel = new JBScrollPane(topAnchor, JBScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JBScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

            {
                final Content toolContent = ContentFactory.getInstance().createContent(scrollablePanel, "", false);
                toolWindow.getContentManager().addContent(toolContent);

                AnAction refreshAction = new AnAction("Refresh", "Refresh", AllIcons.Actions.Refresh) {
                    @Override
                    public void actionPerformed(@NotNull AnActionEvent e) {
                        updatePanel(project, buttonPanel);
                    }
                };
                AnAction aboutAction = new AnAction("About SDM", "About SDM", AllIcons.Actions.Help) {
                    @Override
                    public void actionPerformed(@NotNull AnActionEvent e) {
                        UIDialogAbout.show_modal();
                    }
                };
                List<AnAction> actions = new java.util.ArrayList<AnAction>();
                actions.add(refreshAction);
                actions.add(aboutAction);
                toolWindow.setTitleActions(actions);

                toolWindow.setAutoHide(false);
                toolWindow.setAvailable(true);
                toolWindow.activate(null);
            }
        }

        updatePanel(project, buttonPanel);
    }

    public void updatePanel(Project project, JPanel panel) {

        panel.removeAll();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        VirtualFile sdmFile = findFileByRelativePath(project, ".sdm");

        if (sdmFile != null) {
            List<String> newItems = buildItemsFromSdm(project);
            if (newItems != null) {
                for (String item : newItems) {
                    if (item.startsWith(SEPARATOR_PREFIX)) {
                        String separatorType = item.substring(SEPARATOR_PREFIX.length());
                        if (separatorType.equals("Simple")) {
                            JPanel separatorWrapper = new JPanel();
                            separatorWrapper.setLayout(new BorderLayout());
                            separatorWrapper.setBorder(JBUI.Borders.empty(2, 0));
                            separatorWrapper.add(new JSeparator(), BorderLayout.CENTER);
                            separatorWrapper.setAlignmentY(Component.TOP_ALIGNMENT); // Align to top
                            panel.add(separatorWrapper);
                        } else if (separatorType.startsWith("Title: ")) {
                            String title = separatorType.substring("Title: ".length());
                            JPanel titledSeparatorWrapper = new JPanel(new GridBagLayout());
                            titledSeparatorWrapper.setBorder(JBUI.Borders.empty(6, 0, 2, 0));

                            GridBagConstraints gbc = new GridBagConstraints();
                            gbc.gridy = 0;
                            gbc.fill = GridBagConstraints.HORIZONTAL;
                            gbc.anchor = GridBagConstraints.CENTER;

                            JSeparator leadSep = new JSeparator();
                            gbc.gridx = 0;
                            gbc.weightx = 0;
                            gbc.ipadx = JBUI.scale(12);
                            titledSeparatorWrapper.add(leadSep, gbc);

                            JLabel titleLabel = new JLabel(title);
                            titleLabel.setForeground(UIUtil.getContextHelpForeground());
                            titleLabel.setBorder(JBUI.Borders.empty(0, 6));
                            gbc.gridx = 1;
                            gbc.weightx = 0;
                            gbc.ipadx = 0;
                            gbc.fill = GridBagConstraints.NONE;
                            titledSeparatorWrapper.add(titleLabel, gbc);

                            JSeparator trailSep = new JSeparator();
                            gbc.gridx = 2;
                            gbc.weightx = 1;
                            gbc.fill = GridBagConstraints.HORIZONTAL;
                            titledSeparatorWrapper.add(trailSep, gbc);

                            titledSeparatorWrapper.setAlignmentY(Component.TOP_ALIGNMENT);
                            panel.add(titledSeparatorWrapper);
                        }
                    } else {
                        final String path = item;
                        JButton button = new JButton(path);
                        button.setFocusPainted(false);
                        // Set maximum size to preferred size to prevent stretching
                        button.setMaximumSize(button.getPreferredSize());
                        button.setPreferredSize(new Dimension(100, 36)); // Keep preferred size for initial layout
                        button.setAlignmentX(Component.CENTER_ALIGNMENT);
                        button.setAlignmentY(Component.TOP_ALIGNMENT); // Align to top to prevent vertical stretching

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

                        attachOpenBehavior(project, path, "", button);

                        JPanel buttonWrapper = new JPanel();
                        buttonWrapper.setLayout(new BorderLayout());
                        buttonWrapper.setBorder(JBUI.Borders.emptyBottom(3));
                        buttonWrapper.add(button, BorderLayout.CENTER);
                        buttonWrapper.setAlignmentY(Component.TOP_ALIGNMENT); // Align to top
                        panel.add(buttonWrapper);
                    }
                }
            }
        }

        addSdmLinkAndLabel(project, panel, sdmFile);

        // panel.add(Box.createVerticalGlue()); // This will push all content to the top
        panel.revalidate();
        panel.repaint();
    }

    private void addSdmLinkAndLabel(Project project, JPanel panel, VirtualFile sdmFile) {
        if (sdmFile == null) {
            panel.removeAll();
            panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
            // panel.add(Box.createVerticalGlue()); // push content to vertical center

            JLabel noLabel = new JLabel("No .sdm");
            noLabel.setFont(UIUtil.getLabelFont().deriveFont(Font.PLAIN, 11f));
            noLabel.setForeground(UIUtil.getContextHelpForeground());
            noLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            noLabel.setAlignmentY(Component.TOP_ALIGNMENT); // Align to top
            panel.add(noLabel);

            ActionLink sdmLink = new ActionLink("Create");
            attachSdmBehavior(project, panel, sdmLink);
            sdmLink.setAlignmentX(Component.CENTER_ALIGNMENT);
            sdmLink.setAlignmentY(Component.TOP_ALIGNMENT); // Align to top
            panel.add(sdmLink);

            // panel.add(Box.createVerticalGlue()); // vertical center
        } else {
            // sdm exists → just add the link at the end of buttons
            ActionLink sdmLink = new ActionLink(".sdm");
            attachOpenBehavior(project, ".sdm", "", sdmLink);

            JPanel linkWrapper = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
            linkWrapper.add(sdmLink);
            linkWrapper.setAlignmentY(Component.TOP_ALIGNMENT); // Align to top
            panel.add(linkWrapper);
        }
        panel.revalidate();
        panel.repaint();
    }

    private void attachSdmBehavior(Project project, JPanel panel, ActionLink link) {
        link.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                VirtualFile file = findFileByRelativePath(project, ".sdm");
                boolean created = false;
                if (file == null) {
                    try {
                        VirtualFile baseDir = IdeaHelpers.get_project_base_dir(project);
                        String defaultContent = "# " + project.getName() + "\n\n";
                        try {
                            String sdm = IdeaHelpers.read_from_jar("resources", ".sdm_example.txt");
                            defaultContent = defaultContent + sdm;
                        } catch (Exception ignore) {
                        }
                        file = createAndOpen(project, baseDir, ".sdm", defaultContent);

                        if (file != null) {
                            created = true;
                        }
                    } catch (Exception ex) {
                        IdeaMessageHelpers.show_error_in_ui_thread(ex);
                    }
                }
                if (file != null) {
                    openEditorQuietly(project, ".sdm");
                }
                if (created) {
                    updatePanel(project, panel);
                }
            }
        });
    }

    private static void attachOpenBehavior(Project project, final String path, final String defaultContent, AbstractButton button) {
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                VirtualFile file = findFileByRelativePath(project, path);
                if (file == null) {
                    try {
                        VirtualFile baseDir = IdeaHelpers.get_project_base_dir(project);
                        file = createAndOpen(project, baseDir, path, defaultContent);
                    } catch (Exception ex) {
                        IdeaMessageHelpers.show_error_in_ui_thread(ex);
                    }
                }

                if (file != null) {
                    openEditorQuietly(project, path);
                }
            }
        });
    }

    private static VirtualFile createAndOpen(Project project, VirtualFile baseDir, String path, String content) throws Exception {
        final VirtualFile[] result = new VirtualFile[1];
        ApplicationManager.getApplication().runWriteAction(new Runnable() {
            @Override
            public void run() {
                try {
                    int slash = path.lastIndexOf('/');
                    VirtualFile targetDir;
                    String fileName;
                    if (slash >= 0) {
                        String dirPart = path.substring(0, slash);
                        targetDir = IdeaHelpers.ensure_dir(baseDir, dirPart);
                        fileName = path.substring(slash + 1);
                    } else {
                        targetDir = baseDir;
                        fileName = path;
                    }
                    VirtualFile f = targetDir.createChildData(null, fileName);
                    f.setBinaryContent(content.getBytes(f.getCharset()));
                    IdeaEditorHelpers.open_local_file_in_editor_sync(project, f);
                    result[0] = f;
                } catch (Exception ex) {
                    IdeaMessageHelpers.show_error_in_ui_thread(ex);
                }
            }
        });
        if (result[0] != null) {
            baseDir.refresh(false, true);
        }
        return result[0];
    }

    private static void openEditorQuietly(Project project, String relativePath) {
        VirtualFile file = findFileByRelativePath(project, relativePath);
        if (file == null) return;

        FileEditorManager fem = FileEditorManager.getInstance(project);
        VirtualFile[] openFiles = fem.getOpenFiles();
        for (int i = 0; i < openFiles.length; i++) {
            if (openFiles[i].equals(file)) {
                fem.openFile(openFiles[i], false);
                return;
            }
        }

        fem.openFile(file, false);
    }

    private static VirtualFile findFileByRelativePath(Project project, String relativePath) {
        String basePath = project.getBasePath();
        if (basePath == null) return null;
        VirtualFile baseDir = LocalFileSystem.getInstance().findFileByPath(basePath);
        if (baseDir == null) return null;
        return baseDir.findFileByRelativePath(relativePath);
    }

    private static List<String> buildItemsFromSdm(Project project) {
        List<String> items = new ArrayList<String>();
        VirtualFile sdm = findFileByRelativePath(project, ".sdm");
        if (sdm == null) return null;

        String[] lines = ReadAction.compute(() -> readLines(sdm));

        if (lines.length == 0) return null;

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty()) {
                continue;
            }

            if (line.startsWith("---")) {
                String separatorContent = line.substring(3).trim();
                if (separatorContent.isEmpty()) {
                    items.add(SEPARATOR_PREFIX + "Simple");
                } else {
                    items.add(SEPARATOR_PREFIX + "Title: " + separatorContent);
                }
            } else {
                items.add(line);
            }
        }

        return items;
    }

    private static String[] readLines(VirtualFile file) {
        String text;
        com.intellij.openapi.editor.Document document = com.intellij.openapi.fileEditor.FileDocumentManager.getInstance().getDocument(file);

        if (document != null) {
            text = document.getText();
        } else {
            try {
                text = new String(file.contentsToByteArray(), file.getCharset());
            } catch (Exception e) {
                return new String[0];
            }
        }

        List<String> result = new ArrayList<String>();
        String[] lines = text.split("\n");
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
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