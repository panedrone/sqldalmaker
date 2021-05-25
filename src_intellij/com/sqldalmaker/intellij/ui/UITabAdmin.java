/*
    Copyright 2011-2021 sqldalmaker@gmail.com
    SQL DAL Maker Website: http://sqldalmaker.sourceforge.net
    Read LICENSE.txt in the root of this project/archive for details.
 */
package com.sqldalmaker.intellij.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import com.sqldalmaker.cg.Helpers;
import com.sqldalmaker.common.Const;
import com.sqldalmaker.jaxb.settings.Settings;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.plaf.FontUIResource;
import javax.swing.text.StyleContext;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.Connection;
import java.util.Locale;

/**
 * Created with IntelliJ IDEA.
 * User: sqldalmaker@gmail.com
 * Date: 21.06.12
 */
public class UITabAdmin {
    private JButton editSettingsXmlButton;
    private JButton btn_validate_all;
    private JButton createOverwriteXSDFilesButton;
    private JButton createOverwriteSettingsXmlButton;
    private JButton createOverwriteDtoXmlButton;
    private JButton dataStoreJavaButton;
    private JButton dataStorePySQLite3Button;
    private JButton dataStoreJavaDbUtilsButton;
    // private JButton dataStoreGroovyButton;
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
    private JButton recentChangesButton;
    private JButton dataStoreQt5CButton;
    private JTextField vTextField;
    private JButton referenceSettingsXmlButton;
    private JScrollPane scroll_pane;
    private JButton datastore_pyodbc;
    private JButton dataStorePsycopg2Button;
    private JButton btn_php_pg;
    private JButton btn_mysql;
    private JButton dataStorePhpPDOSQLButton;
    private JButton btn_php_oracle;
    private JButton btn_cx_Oracle;
    private JButton dataStorePhpOCI8Button;
    private JButton btn_golang;
    private JButton btn_golangVM;

    private Project project;
    private VirtualFile root_file;

    public void init_runtime() {

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {

                ///////////////////////////////////////////////////////////////////
                //
                // google: intellij platform plugin sdk detect plugin version programmatically
                // Programmatically get the version of an IntelliJ IDEA plugin
                // https://stackoverflow.com/questions/28080707/programmatically-get-the-version-of-an-intellij-idea-plugin

                String plugin_version = "1.160+";
                try {
                    String plugin_xml = IdeaHelpers.read_from_jar_file("", "plugin.xml");
                    String[] parts = plugin_xml.split("<version>");
                    plugin_version = parts[1].split("</version>")[0];
                } catch (Throwable e) {
                    //
                }
                String jv = System.getProperty("java.version");
                vTextField.setText(plugin_version + " on Java " + jv);
                vTextField.setBorder(BorderFactory.createEmptyBorder());
                // https://stackoverflow.com/questions/291115/java-swing-using-jscrollpane-and-having-it-scroll-back-to-top
                scroll_pane.getVerticalScrollBar().setValue(0);
                // https://stackoverflow.com/questions/5583495/how-do-i-speed-up-the-scroll-speed-in-a-jscrollpane-when-using-the-mouse-wheel
                scroll_pane.getVerticalScrollBar().setUnitIncrement(20);
            }
        });
    }

    public UITabAdmin() {
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
                    IdeaMetaProgramInitHelpers.create_xsd(root_file);
                }
            }
        });
        createOverwriteSettingsXmlButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int dialogResult = JOptionPane.showConfirmDialog(null, "This action creates/overwrites settings.xml in the folder of XML meta-program. Continue?",
                        "Warning", JOptionPane.YES_NO_OPTION);
                if (dialogResult == JOptionPane.YES_OPTION) {
                    IdeaMetaProgramInitHelpers.create_settings_xml(root_file);
                }
            }
        });
        createOverwriteDtoXmlButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int dialogResult = JOptionPane.showConfirmDialog(null, "This action creates/overwrites dto.xml in the folder of XML meta-program. Continue?",
                        "Warning", JOptionPane.YES_NO_OPTION);
                if (dialogResult == JOptionPane.YES_OPTION) {
                    IdeaMetaProgramInitHelpers.create_dto_xml(root_file);
                }
            }
        });
        editSettingsXmlButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                IdeaEditorHelpers.open_settings_xml_sync(project, root_file);
            }
        });
        btn_validate_all.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                validate_all();
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
                IdeaEditorHelpers.open_or_activate_jar_file_in_editor(project, "com/sqldalmaker/cg/java", "go.vm", "go.vm");
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
        dataStoreJavaDbUtilsButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                IdeaEditorHelpers.open_or_activate_jar_resource_in_editor(project, "DataStoreManagerJDBC.java_", "DataStoreManager.java");
            }
        });
        dataStoreJavaAndroidButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                IdeaEditorHelpers.open_or_activate_jar_resource_in_editor(project, "DataStoreManagerAndroid.java_", "DataStoreManager.java");
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
                IdeaEditorHelpers.open_or_activate_jar_resource_in_editor(project, "DataStore_PDO_SQLite3.php", "DataStore_PDO_SQLite3.php");
            }
        });
        dataStoreCSTLButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                IdeaEditorHelpers.open_or_activate_jar_resource_in_editor(project, "DataStore.STL.cpp", "DataStore.cpp");
                IdeaEditorHelpers.open_or_activate_jar_resource_in_editor(project, "DataStore.STL.h", "DataStore.h");
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
                IdeaEditorHelpers.open_or_activate_jar_resource_in_editor(project, "data_store_sqlite3.py", "data_store.py");
            }
        });
        dataStorePyMySQLButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                IdeaEditorHelpers.open_or_activate_jar_resource_in_editor(project, "data_store_mysql.py", "data_store.py");
            }
        });
        dataStoreRUBYDBIButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                IdeaEditorHelpers.open_or_activate_jar_resource_in_editor(project, "data_store.rb", "data_store.rb");
            }
        });
        recentChangesButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                IdeaEditorHelpers.open_or_activate_jar_file_in_editor(project, "", "recent_changes.txt", "recent_changes.txt");
            }
        });
        dataStoreQt5CButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                IdeaEditorHelpers.open_or_activate_jar_resource_in_editor(project, "DataStore_Qt5.cpp", "DataStore.cpp");
                IdeaEditorHelpers.open_or_activate_jar_resource_in_editor(project, "DataStore_Qt5.h", "DataStore.h");
            }
        });
        referenceSettingsXmlButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                IdeaEditorHelpers.open_or_activate_jar_resource_in_editor(project, Const.SETTINGS_XML, Const.SETTINGS_XML);
            }
        });
        datastore_pyodbc.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                IdeaEditorHelpers.open_or_activate_jar_resource_in_editor(project, "data_store_pyodbc.py", "data_store.py");
            }
        });
        dataStorePsycopg2Button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                IdeaEditorHelpers.open_or_activate_jar_resource_in_editor(project, "data_store_psycopg2.py", "data_store.py");
            }
        });
        btn_php_pg.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                IdeaEditorHelpers.open_or_activate_jar_resource_in_editor(project, "DataStore_PDO_PostgreSQL.php", "DataStore_PDO_PostgreSQL.php");
            }
        });
        btn_mysql.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                IdeaEditorHelpers.open_or_activate_jar_resource_in_editor(project, "DataStore_PDO_MySQL.php", "DataStore_PDO_MySQL.php");
            }
        });
        dataStorePhpPDOSQLButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                IdeaEditorHelpers.open_or_activate_jar_resource_in_editor(project, "DataStore_PDO_SQL_Server.php", "DataStore_PDO_SQL_Server.php");
            }
        });
        btn_php_oracle.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                IdeaEditorHelpers.open_or_activate_jar_resource_in_editor(project, "DataStore_PDO_Oracle.php", "DataStore_PDO_Oracle.php");
            }
        });
        btn_cx_Oracle.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                IdeaEditorHelpers.open_or_activate_jar_resource_in_editor(project, "data_store_cx_oracle.py", "data_store.py");
            }
        });
        dataStorePhpOCI8Button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                IdeaEditorHelpers.open_or_activate_jar_resource_in_editor(project, "DataStore_oci8.php", "DataStore_oci8.php");
            }
        });
        btn_golang.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                IdeaEditorHelpers.open_or_activate_jar_resource_in_editor(project, "data_store.go", "data_store.go");
            }
        });
        btn_golangVM.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                IdeaEditorHelpers.open_or_activate_jar_file_in_editor(project, "com/sqldalmaker/cg/go", "go.vm", "go.vm");
            }
        });
    }

    private static String get_err_msg(Throwable ex) {
        return ex.getClass().getName() + " -> " + ex.getMessage(); // printStackTrace();
    }

    private void validate_all() {
        StringBuilder buff = new StringBuilder();
        Settings sett = null;
        if (check_xsd(buff, Const.SETTINGS_XSD)) {
            try {
                sett = IdeaHelpers.load_settings(root_file);
                add_ok_msg(buff, Const.SETTINGS_XML);
            } catch (Exception ex) {
                add_err_msg(buff, "Invalid " + Const.SETTINGS_XML + ": " + get_err_msg(ex));
            }
        } else {
            add_err_msg(buff, "Cannot load " + Const.SETTINGS_XML + " because of invalid " + Const.SETTINGS_XSD);
        }
        check_xsd(buff, Const.DTO_XSD);
        check_xsd(buff, Const.DAO_XSD);
        if (sett == null) {
            add_err_msg(buff, "Test connection -> failed because of invalid settings");
        } else {
            try {
                Connection con = IdeaHelpers.get_connection(project, sett);
                con.close();
                add_ok_msg(buff, "Test connection");
            } catch (Exception ex) {
                add_err_msg(buff, "Test connection: " + get_err_msg(ex));
            }
        }
        IdeaMessageHelpers.show_info_in_ui_thread(buff.toString());
    }

    private boolean check_xsd(StringBuilder buff, String xsd_name) {
        VirtualFile xsd_file = root_file.getParent().findChild(xsd_name);
        if (xsd_file == null) {
            add_err_msg(buff, "File not found: " + xsd_name);
            return false;
        } else {
            String cur_text;
            try {
                cur_text = Helpers.load_text_from_file(xsd_file.getPath());
            } catch (Exception ex) {
                add_err_msg(buff, get_err_msg(ex));
                return false;
            }
            String ref_text;
            try {
                ref_text = IdeaHelpers.read_from_jar_file(xsd_name);
            } catch (Exception ex) {
                add_err_msg(buff, get_err_msg(ex));
                return false;
            }
            if (ref_text.equals(cur_text)) {
                add_ok_msg(buff, xsd_name);
            } else {
                add_err_msg(buff, "File '" + xsd_name + "' is invalid or out-of-date! Use 'Create/Overwrite XSD files'");
                return false;
            }
        }
        return true;
    }

    private void add_ok_msg(StringBuilder buff, String msg) {
        buff.append("\r\n");
        buff.append(msg);
        buff.append(" -> OK");
        IdeaMessageHelpers.add_info_to_ide_log(msg + " -> OK");
    }

    private void add_err_msg(StringBuilder buff, String msg) {
        buff.append("\r\n[ERROR] ");
        buff.append(msg);
        IdeaMessageHelpers.add_error_to_ide_log("ERROR", msg);
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public void setFile(VirtualFile file) {
        this.root_file = file;
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
        scroll_pane = new JScrollPane();
        rootPanel.add(scroll_pane, BorderLayout.CENTER);
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(7, 2, new Insets(0, 10, 0, 0), -1, -1));
        panel1.setMaximumSize(new Dimension(2147483647, 2147483647));
        scroll_pane.setViewportView(panel1);
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
        panel1.add(panel2, new GridConstraints(0, 0, 1, 2, GridConstraints.ANCHOR_NORTHWEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        referenceSettingsXmlButton = new JButton();
        referenceSettingsXmlButton.setText("Reference settings.xml");
        panel2.add(referenceSettingsXmlButton);
        btn_validate_all = new JButton();
        btn_validate_all.setText("Validate All");
        panel2.add(btn_validate_all);
        recentChangesButton = new JButton();
        recentChangesButton.setText("Recent changes");
        panel2.add(recentChangesButton);
        editSettingsXmlButton = new JButton();
        editSettingsXmlButton.setText("Edit settings.xml");
        panel2.add(editSettingsXmlButton);
        vTextField = new JTextField();
        vTextField.setBackground(new Color(-855310));
        vTextField.setEditable(false);
        vTextField.setHorizontalAlignment(4);
        vTextField.setText("v. ?");
        panel2.add(vTextField);
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
        panel1.add(panel3, new GridConstraints(1, 0, 1, 2, GridConstraints.ANCHOR_NORTHWEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        createOverwriteXSDFilesButton = new JButton();
        createOverwriteXSDFilesButton.setText("Create/Overwrite XSD files");
        panel3.add(createOverwriteXSDFilesButton);
        createOverwriteSettingsXmlButton = new JButton();
        createOverwriteSettingsXmlButton.setText("Create/Overwrite settings.xml");
        panel3.add(createOverwriteSettingsXmlButton);
        createOverwriteDtoXmlButton = new JButton();
        createOverwriteDtoXmlButton.setText("Create/Overwrite dto.xml");
        panel3.add(createOverwriteDtoXmlButton);
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
        panel1.add(panel4, new GridConstraints(3, 0, 1, 2, GridConstraints.ANCHOR_NORTHWEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        php_vm = new JButton();
        php_vm.setText("php.vm");
        panel4.add(php_vm);
        java_vm = new JButton();
        java_vm.setText("java.vm");
        panel4.add(java_vm);
        cppVmButton = new JButton();
        cppVmButton.setText("cpp.vm");
        panel4.add(cppVmButton);
        pythonVmButton = new JButton();
        pythonVmButton.setText("python.vm");
        panel4.add(pythonVmButton);
        btn_golangVM = new JButton();
        btn_golangVM.setText("go.vm");
        panel4.add(btn_golangVM);
        rubyVmButton = new JButton();
        rubyVmButton.setText("ruby.vm");
        panel4.add(rubyVmButton);
        final JPanel panel5 = new JPanel();
        panel5.setLayout(new BorderLayout(0, 0));
        panel1.add(panel5, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_VERTICAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JPanel panel6 = new JPanel();
        panel6.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
        panel5.add(panel6, BorderLayout.NORTH);
        final JLabel label1 = new JLabel();
        Font label1Font = this.$$$getFont$$$(null, -1, 14, label1.getFont());
        if (label1Font != null) label1.setFont(label1Font);
        label1.setText(" PHP ");
        panel6.add(label1);
        dataStorePhpButton = new JButton();
        dataStorePhpButton.setBorderPainted(true);
        dataStorePhpButton.setContentAreaFilled(true);
        dataStorePhpButton.setMinimumSize(new Dimension(40, 30));
        dataStorePhpButton.setPreferredSize(new Dimension(42, 30));
        dataStorePhpButton.setText("Base");
        panel6.add(dataStorePhpButton);
        btn_mysql = new JButton();
        btn_mysql.setText("PDO, mysql");
        panel6.add(btn_mysql);
        btn_php_oracle = new JButton();
        btn_php_oracle.setText("PDO, oracle");
        panel6.add(btn_php_oracle);
        dataStorePhpOCI8Button = new JButton();
        dataStorePhpOCI8Button.setPreferredSize(new Dimension(50, 30));
        dataStorePhpOCI8Button.setText("oci8");
        panel6.add(dataStorePhpOCI8Button);
        dataStorePhpPDOSQLButton = new JButton();
        dataStorePhpPDOSQLButton.setText("PDO, mssql");
        panel6.add(dataStorePhpPDOSQLButton);
        btn_php_pg = new JButton();
        btn_php_pg.setText("PDO, postgesql");
        panel6.add(btn_php_pg);
        PDODataStorePhpButton = new JButton();
        PDODataStorePhpButton.setText("PDO, sqlite3");
        panel6.add(PDODataStorePhpButton);
        final JPanel panel7 = new JPanel();
        panel7.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
        panel5.add(panel7, BorderLayout.CENTER);
        final JLabel label2 = new JLabel();
        Font label2Font = this.$$$getFont$$$(null, -1, 14, label2.getFont());
        if (label2Font != null) label2.setFont(label2Font);
        label2.setText(" Java ");
        panel7.add(label2);
        dataStoreJavaButton = new JButton();
        dataStoreJavaButton.setMinimumSize(new Dimension(40, 30));
        dataStoreJavaButton.setPreferredSize(new Dimension(42, 30));
        dataStoreJavaButton.setText("Base");
        panel7.add(dataStoreJavaButton);
        dataStoreJavaDbUtilsButton = new JButton();
        dataStoreJavaDbUtilsButton.setPreferredSize(new Dimension(56, 30));
        dataStoreJavaDbUtilsButton.setText("JDBC");
        panel7.add(dataStoreJavaDbUtilsButton);
        dataStoreJavaAndroidButton = new JButton();
        dataStoreJavaAndroidButton.setText("Android");
        panel7.add(dataStoreJavaAndroidButton);
        final JLabel label3 = new JLabel();
        Font label3Font = this.$$$getFont$$$(null, -1, 14, label3.getFont());
        if (label3Font != null) label3.setFont(label3Font);
        label3.setText(" C++ ");
        panel7.add(label3);
        dataStoreQt5CButton = new JButton();
        dataStoreQt5CButton.setPreferredSize(new Dimension(56, 30));
        dataStoreQt5CButton.setText("QtSql");
        panel7.add(dataStoreQt5CButton);
        dataStoreCSTLButton = new JButton();
        dataStoreCSTLButton.setText("STL, sqlite3");
        panel7.add(dataStoreCSTLButton);
        dataStoreCATLButton = new JButton();
        dataStoreCATLButton.setText("ATL, sqlite3");
        panel7.add(dataStoreCATLButton);
        final JPanel panel8 = new JPanel();
        panel8.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
        panel8.setMinimumSize(new Dimension(40, 40));
        panel5.add(panel8, BorderLayout.SOUTH);
        final JLabel label4 = new JLabel();
        Font label4Font = this.$$$getFont$$$(null, -1, 14, label4.getFont());
        if (label4Font != null) label4.setFont(label4Font);
        label4.setText(" Python ");
        panel8.add(label4);
        dataStorePyMySQLButton = new JButton();
        dataStorePyMySQLButton.setPreferredSize(new Dimension(56, 30));
        dataStorePyMySQLButton.setText("mysql");
        panel8.add(dataStorePyMySQLButton);
        btn_cx_Oracle = new JButton();
        btn_cx_Oracle.setText("cx_Oracle");
        panel8.add(btn_cx_Oracle);
        dataStorePsycopg2Button = new JButton();
        dataStorePsycopg2Button.setText("psycopg2");
        panel8.add(dataStorePsycopg2Button);
        datastore_pyodbc = new JButton();
        datastore_pyodbc.setText("pyodbc, mssql");
        panel8.add(datastore_pyodbc);
        dataStorePySQLite3Button = new JButton();
        dataStorePySQLite3Button.setPreferredSize(new Dimension(56, 30));
        dataStorePySQLite3Button.setText("sqlite3");
        panel8.add(dataStorePySQLite3Button);
        final JLabel label5 = new JLabel();
        Font label5Font = this.$$$getFont$$$(null, -1, 14, label5.getFont());
        if (label5Font != null) label5.setFont(label5Font);
        label5.setText(" Ruby ");
        panel8.add(label5);
        dataStoreRUBYDBIButton = new JButton();
        dataStoreRUBYDBIButton.setPreferredSize(new Dimension(42, 30));
        dataStoreRUBYDBIButton.setText("DBI");
        panel8.add(dataStoreRUBYDBIButton);
        final JLabel label6 = new JLabel();
        Font label6Font = this.$$$getFont$$$(null, -1, 14, label6.getFont());
        if (label6Font != null) label6.setFont(label6Font);
        label6.setText(" Go ");
        panel8.add(label6);
        btn_golang = new JButton();
        btn_golang.setText("database/sql");
        panel8.add(btn_golang);
        final JPanel panel9 = new JPanel();
        panel9.setLayout(new GridLayoutManager(1, 1, new Insets(10, 0, 0, 0), 1, -1));
        panel1.add(panel9, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_NORTH, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        text1 = new JTextPane();
        text1.setBackground(new Color(-1));
        text1.setEditable(false);
        text1.setMargin(new Insets(15, 15, 15, 15));
        text1.setText("");
        panel9.add(text1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_NORTHWEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        panel1.add(spacer1, new GridConstraints(5, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    private Font $$$getFont$$$(String fontName, int style, int size, Font currentFont) {
        if (currentFont == null) return null;
        String resultName;
        if (fontName == null) {
            resultName = currentFont.getName();
        } else {
            Font testFont = new Font(fontName, Font.PLAIN, 10);
            if (testFont.canDisplay('a') && testFont.canDisplay('1')) {
                resultName = fontName;
            } else {
                resultName = currentFont.getName();
            }
        }
        Font font = new Font(resultName, style >= 0 ? style : currentFont.getStyle(), size >= 0 ? size : currentFont.getSize());
        boolean isMac = System.getProperty("os.name", "").toLowerCase(Locale.ENGLISH).startsWith("mac");
        Font fontWithFallback = isMac ? new Font(font.getFamily(), font.getStyle(), font.getSize()) : new StyleContext().getFont(font.getFamily(), font.getStyle(), font.getSize());
        return fontWithFallback instanceof FontUIResource ? fontWithFallback : new FontUIResource(fontWithFallback);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return rootPanel;
    }

}
