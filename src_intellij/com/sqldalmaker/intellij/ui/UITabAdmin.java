/*
 * Copyright 2011-2018 sqldalmaker@gmail.com
 * SQL DAL Maker Website: http://sqldalmaker.sourceforge.net
 * Read LICENSE.txt in the root of this project/archive for details.
 */
package com.sqldalmaker.intellij.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.sqldalmaker.jaxb.settings.Settings;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.Connection;

/**
 * Created with IntelliJ IDEA.
 * User: sqldalmaker@gmail.com
 * Date: 21.06.12
 */
public class UITabAdmin {
    private JButton editSettingsXmlButton;
    private JButton testConnectionButton;
    private JButton createOverwriteXSDFilesButton;
    private JButton createOverwriteSettingsXmlButton;
    private JButton createOverwriteDtoXmlButton;
    private JButton dataStoreJavaButton;
    private JButton dataStorePySQLite3Button;
    private JButton dataStoreJavaSpringButton;
    private JButton dataStoreJavaDbUtilsButton;
    private JButton dataStoreGroovyButton;
    private JButton dataStoreJavaAndroidButton;
    private JButton dataStorePhpButton;
    private JButton dataStorePyMySQLButton;
    private JButton PDODataStorePhpButton;
    private JButton dataStoreCSTLButton;
    private JButton dataStoreCATLButton;
    private JButton dataStoreRUBYDBIButton;
    private JButton cppVmButton;
    private JButton pythonVmButton;
    private JButton rubyVmButton;
    private JPanel rootPanel;
    private JTextPane text1;
    private JButton php_vm;
    private JButton java_vm;
    private JPanel toolbar_1;
    private JToolBar toolbar1;
    private JButton btn_OpenXML;
    private JButton btn_OpenSQL;
    private JButton recentChangesButton;
    private JButton dataStoreQt5CButton;

    private Project project;
    private VirtualFile propFile;

    public JComponent getToolBar() {
        return toolbar_1;
    }

    public UITabAdmin() {

        rootPanel.remove(toolbar_1);
        Cursor wc = new Cursor(Cursor.HAND_CURSOR);

        for (Component c : toolbar1.getComponents()) {
            JButton b = (JButton) c;
            b.setCursor(wc);
        }

        try {
            text1.setContentType("text/html");
            text1.setEditable(false);

            String text = IdeaHelpers.read_from_jar_file("", "ABOUT.html");
            text1.setText(text);

            text1.addHyperlinkListener(new HyperlinkListener() {
                @Override
                public void hyperlinkUpdate(HyperlinkEvent hle) {
                    if (HyperlinkEvent.EventType.ACTIVATED.equals(hle.getEventType())) {
                        // System.out.println(hle.getURL());
                        if (Desktop.isDesktopSupported()) {
                            try {
                                Desktop desktop = Desktop.getDesktop();
                                desktop.browse(hle.getURL().toURI());
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }
                        }
                    }
                }
            });

//            HTMLDocument doc = (HTMLDocument) text1.getDocument();
//            HTMLEditorKit editorKit = (HTMLEditorKit) text1.getEditorKit();
//
//            editorKit.insertHTML(doc, doc.getLength(), text, 0, 0, null);

        } catch (Exception e) {
            e.printStackTrace();
        }
        createOverwriteXSDFilesButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                int dialogResult = JOptionPane.showConfirmDialog(null,
                        "This action creates/overwrites XSD files in the folder of XML meta-program. Continue?",
                        "Warning", JOptionPane.YES_NO_OPTION);

                if (dialogResult == JOptionPane.YES_OPTION) {
                    IdeaMetaProgramInitHelpers.create_xsd(propFile);
                }
            }
        });
        createOverwriteSettingsXmlButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int dialogResult = JOptionPane.showConfirmDialog(null, "This action creates/overwrites settings.xml in the folder of XML meta-program. Continue?",
                        "Warning", JOptionPane.YES_NO_OPTION);

                if (dialogResult == JOptionPane.YES_OPTION) {

                    IdeaMetaProgramInitHelpers.create_settings_xml(propFile);
                }
            }
        });
        createOverwriteDtoXmlButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int dialogResult = JOptionPane.showConfirmDialog(null, "This action creates/overwrites dto.xml in the folder of XML meta-program. Continue?",
                        "Warning", JOptionPane.YES_NO_OPTION);

                if (dialogResult == JOptionPane.YES_OPTION) {

                    IdeaMetaProgramInitHelpers.create_dto_xml(propFile);
                }
            }
        });
        editSettingsXmlButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                IdeaEditorHelpers.open_settings_xml(project, propFile);
            }
        });
        testConnectionButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                testConnection();
            }
        });
        php_vm.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                IdeaEditorHelpers.open_or_activate_jar_file_in_editor(project, "com/sqldalmaker/cg/php", "php.vm", "php.vm");
            }
        });
        java_vm.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                IdeaEditorHelpers.open_or_activate_jar_file_in_editor(project, "com/sqldalmaker/cg/java", "java.vm", "java.vm");
            }
        });
        cppVmButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                IdeaEditorHelpers.open_or_activate_jar_file_in_editor(project, "com/sqldalmaker/cg/cpp", "cpp.vm", "cpp.vm");
            }
        });
        pythonVmButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                IdeaEditorHelpers.open_or_activate_jar_file_in_editor(project, "com/sqldalmaker/cg/python", "python.vm", "python.vm");
            }
        });
        rubyVmButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                IdeaEditorHelpers.open_or_activate_jar_file_in_editor(project, "com/sqldalmaker/cg/ruby", "ruby.vm", "ruby.vm");
            }
        });
        dataStoreJavaButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                IdeaEditorHelpers.open_or_activate_jar_resource_in_editor(project, "DataStore.java_", "DataStore.java");
            }
        });
        dataStoreJavaSpringButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                IdeaEditorHelpers.open_or_activate_jar_resource_in_editor(project, "DataStoreManagerSpring.java_", "DataStoreManagerSpring.java");
            }
        });
        dataStoreJavaDbUtilsButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                IdeaEditorHelpers.open_or_activate_jar_resource_in_editor(project, "DataStoreManagerDbUtils.java_", "DataStoreManagerDbUtils.java");
            }
        });
        dataStoreGroovyButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                IdeaEditorHelpers.open_or_activate_jar_resource_in_editor(project, "DataStoreManager.groovy_", "DataStoreManager.groovy");
            }
        });
        dataStoreJavaAndroidButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                IdeaEditorHelpers.open_or_activate_jar_resource_in_editor(project, "DataStoreManagerAndroid.java_", "DataStoreManagerAndroid.java");
            }
        });
        dataStorePhpButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                IdeaEditorHelpers.open_or_activate_jar_resource_in_editor(project, "DataStore.php", "DataStore.php");
            }
        });
        PDODataStorePhpButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                IdeaEditorHelpers.open_or_activate_jar_resource_in_editor(project, "PDODataStore.php", "PDODataStore.php");
            }
        });
        dataStoreCSTLButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                IdeaEditorHelpers.open_or_activate_jar_resource_in_editor(project, "DataStore.STL.cpp", "DataStore.STL.cpp");
                IdeaEditorHelpers.open_or_activate_jar_resource_in_editor(project, "DataStore.STL.h", "DataStore.STL.h");
            }
        });
        dataStoreCATLButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                IdeaEditorHelpers.open_or_activate_jar_resource_in_editor(project, "DataStore.cpp", "DataStore.cpp");
                IdeaEditorHelpers.open_or_activate_jar_resource_in_editor(project, "DataStore.h", "DataStore.h");
            }
        });
        dataStorePySQLite3Button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                IdeaEditorHelpers.open_or_activate_jar_resource_in_editor(project, "DataStore1.py", "DataStore1.py");
            }
        });
        dataStorePyMySQLButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                IdeaEditorHelpers.open_or_activate_jar_resource_in_editor(project, "DataStore2.py", "DataStore2.py");
            }
        });
        dataStoreRUBYDBIButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                IdeaEditorHelpers.open_or_activate_jar_resource_in_editor(project, "data_store.rb", "data_store.rb");
            }
        });
        btn_OpenXML.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                IdeaEditorHelpers.open_settings_xml(project, propFile);
            }
        });
        btn_OpenSQL.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                testConnection();
            }
        });
        recentChangesButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                IdeaEditorHelpers.open_or_activate_jar_resource_in_editor(project, "recent_changes.txt", "recent_changes.txt");
            }
        });
        dataStoreQt5CButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                IdeaEditorHelpers.open_or_activate_jar_resource_in_editor(project, "DataStore_Qt5.cpp", "DataStore_Qt5.cpp");
                IdeaEditorHelpers.open_or_activate_jar_resource_in_editor(project, "DataStore_Qt5.h", "DataStore_Qt5.h");
            }
        });
    }

    private void testConnection() {
        try {
            Settings prof = IdeaHelpers.load_settings(propFile);
            Connection con = IdeaHelpers.get_connection(project, prof);
            con.close();

            IdeaMessageHelpers.show_info_in_ui_thread("Test connection succeeded.");

        } catch (Exception ex) {
            IdeaMessageHelpers.show_error_in_ui_thread(ex);
            ex.printStackTrace();
        }
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public void setFile(VirtualFile file) {
        this.propFile = file;
    }

    public JComponent getRootPanel() {
        return rootPanel;
    }

    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        rootPanel = new JPanel();
        rootPanel.setLayout(new BorderLayout(0, 0));
        rootPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(new Color(-11513776)), null));
        final JScrollPane scrollPane1 = new JScrollPane();
        rootPanel.add(scrollPane1, BorderLayout.CENTER);
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(6, 2, new Insets(10, 10, 10, 10), 10, 20));
        scrollPane1.setViewportView(panel1);
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
        panel1.add(panel2, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        editSettingsXmlButton = new JButton();
        editSettingsXmlButton.setText("Edit settings.xml");
        panel2.add(editSettingsXmlButton);
        testConnectionButton = new JButton();
        testConnectionButton.setText("Test connection");
        panel2.add(testConnectionButton);
        recentChangesButton = new JButton();
        recentChangesButton.setText("Recent changes");
        panel2.add(recentChangesButton);
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridBagLayout());
        panel1.add(panel3, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_VERTICAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        createOverwriteXSDFilesButton = new JButton();
        createOverwriteXSDFilesButton.setText("Create/Overwrite XSD files");
        GridBagConstraints gbc;
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel3.add(createOverwriteXSDFilesButton, gbc);
        createOverwriteSettingsXmlButton = new JButton();
        createOverwriteSettingsXmlButton.setText("Create/Overwrite settings.xml");
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 0;
        panel3.add(createOverwriteSettingsXmlButton, gbc);
        createOverwriteDtoXmlButton = new JButton();
        createOverwriteDtoXmlButton.setText("Create/Overwrite dto.xml");
        gbc = new GridBagConstraints();
        gbc.gridx = 2;
        gbc.gridy = 0;
        panel3.add(createOverwriteDtoXmlButton, gbc);
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new GridBagLayout());
        panel1.add(panel4, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_VERTICAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        dataStoreJavaButton = new JButton();
        dataStoreJavaButton.setText("DataStore.java");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel4.add(dataStoreJavaButton, gbc);
        dataStoreJavaSpringButton = new JButton();
        dataStoreJavaSpringButton.setText("DataStore Java Spring");
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel4.add(dataStoreJavaSpringButton, gbc);
        dataStoreJavaDbUtilsButton = new JButton();
        dataStoreJavaDbUtilsButton.setText("DataStore Java DbUtils");
        gbc = new GridBagConstraints();
        gbc.gridx = 2;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel4.add(dataStoreJavaDbUtilsButton, gbc);
        dataStoreGroovyButton = new JButton();
        dataStoreGroovyButton.setText("DataStore Groovy");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel4.add(dataStoreGroovyButton, gbc);
        dataStoreJavaAndroidButton = new JButton();
        dataStoreJavaAndroidButton.setText("DataStore Java Android");
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel4.add(dataStoreJavaAndroidButton, gbc);
        dataStorePyMySQLButton = new JButton();
        dataStorePyMySQLButton.setText("DataStore.py MySQL");
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 4;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel4.add(dataStorePyMySQLButton, gbc);
        dataStorePySQLite3Button = new JButton();
        dataStorePySQLite3Button.setText("DataStore.py SQLite3");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel4.add(dataStorePySQLite3Button, gbc);
        dataStoreCSTLButton = new JButton();
        dataStoreCSTLButton.setText("DataStore C++ STL");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel4.add(dataStoreCSTLButton, gbc);
        dataStoreCATLButton = new JButton();
        dataStoreCATLButton.setText("DataStore C++ ATL");
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 3;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel4.add(dataStoreCATLButton, gbc);
        dataStorePhpButton = new JButton();
        dataStorePhpButton.setText("DataStore.php");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel4.add(dataStorePhpButton, gbc);
        PDODataStorePhpButton = new JButton();
        PDODataStorePhpButton.setText("PDODataStore.php");
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 2;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel4.add(PDODataStorePhpButton, gbc);
        dataStoreQt5CButton = new JButton();
        dataStoreQt5CButton.setText("DataStore Qt5 C++");
        gbc = new GridBagConstraints();
        gbc.gridx = 2;
        gbc.gridy = 3;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel4.add(dataStoreQt5CButton, gbc);
        dataStoreRUBYDBIButton = new JButton();
        dataStoreRUBYDBIButton.setText("DataStore RUBY DBI");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel4.add(dataStoreRUBYDBIButton, gbc);
        final JPanel panel5 = new JPanel();
        panel5.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
        panel1.add(panel5, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        php_vm = new JButton();
        php_vm.setText("php.vm");
        panel5.add(php_vm);
        java_vm = new JButton();
        java_vm.setText("java.vm");
        panel5.add(java_vm);
        cppVmButton = new JButton();
        cppVmButton.setText("cpp.vm");
        panel5.add(cppVmButton);
        pythonVmButton = new JButton();
        pythonVmButton.setText("python.vm");
        panel5.add(pythonVmButton);
        rubyVmButton = new JButton();
        rubyVmButton.setText("ruby.vm");
        panel5.add(rubyVmButton);
        text1 = new JTextPane();
        text1.setEditable(false);
        text1.setMargin(new Insets(0, 15, 15, 5));
        text1.setText("");
        panel1.add(text1, new GridConstraints(4, 0, 2, 1, GridConstraints.ANCHOR_NORTH, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_WANT_GROW, null, new Dimension(150, 50), null, 0, false));
        toolbar_1 = new JPanel();
        toolbar_1.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 5));
        rootPanel.add(toolbar_1, BorderLayout.NORTH);
        toolbar1 = new JToolBar();
        toolbar1.setBorderPainted(false);
        toolbar1.setFloatable(false);
        toolbar_1.add(toolbar1);
        btn_OpenXML = new JButton();
        btn_OpenXML.setBorderPainted(false);
        btn_OpenXML.setIcon(new ImageIcon(getClass().getResource("/img/xmldoc.gif")));
        btn_OpenXML.setMargin(new Insets(5, 5, 5, 5));
        btn_OpenXML.setMaximumSize(new Dimension(32, 32));
        btn_OpenXML.setMinimumSize(new Dimension(32, 32));
        btn_OpenXML.setOpaque(false);
        btn_OpenXML.setPreferredSize(new Dimension(32, 32));
        btn_OpenXML.setText("");
        btn_OpenXML.setToolTipText("Edit settings.xml");
        toolbar1.add(btn_OpenXML);
        btn_OpenSQL = new JButton();
        btn_OpenSQL.setBorderPainted(false);
        btn_OpenSQL.setIcon(new ImageIcon(getClass().getResource("/img/connection.gif")));
        btn_OpenSQL.setMargin(new Insets(5, 5, 5, 5));
        btn_OpenSQL.setMaximumSize(new Dimension(32, 32));
        btn_OpenSQL.setMinimumSize(new Dimension(32, 32));
        btn_OpenSQL.setOpaque(false);
        btn_OpenSQL.setPreferredSize(new Dimension(32, 32));
        btn_OpenSQL.setText("");
        btn_OpenSQL.setToolTipText("Test connection");
        toolbar1.add(btn_OpenSQL);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return rootPanel;
    }
}
