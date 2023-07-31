/*
    Copyright 2011-2023 sqldalmaker@gmail.com
    SQL DAL Maker Website: https://sqldalmaker.sourceforge.net/
    Read LICENSE.txt in the root of this project/archive for details.
 */
package com.sqldalmaker.intellij.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.sqldalmaker.cg.Helpers;
import com.sqldalmaker.common.Const;
import com.sqldalmaker.jaxb.settings.Settings;

import javax.swing.*;
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
    //private JButton editSettingsXmlButton;
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
    //private JButton dataStorePyMySQLButton;
    private JButton PDODataStorePhpButton;
    private JButton dataStoreCSTLButton;
    private JButton dataStoreCATLButton;
    private JButton cppVmButton;
    private JButton pythonVmButton;
    private JPanel rootPanel;
    private JTextPane text1;
    private JButton php_vm;
    private JButton java_vm;
    private JButton recentChangesButton;
    private JButton dataStoreQt5CButton;
    private JTextField vTextField;
    private JButton referenceSettingsXmlButton;
    private JScrollPane scroll_pane;
//    private JButton datastore_pyodbc;
    //private JButton dataStorePsycopg2Button;
    private JButton btn_php_pg;
    private JButton btn_mysql;
    private JButton dataStorePhpPDOSQLButton;
    private JButton btn_php_oracle;
//    private JButton btn_cx_Oracle;
    private JButton dataStorePhpOCI8Button;
    private JButton btn_golang;
    private JButton btn_golangVM;
    // private JButton djangoDbButton;
    private JButton sqlite3MysqlConnectorPsycopg2Button;
    private JButton SQLAlchemyButton;
    private JButton gormButton;
    private JButton doctrineORMButton;
    private JButton buttonSettings;

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
//        editSettingsXmlButton.addActionListener(new ActionListener() {
//            @Override
//            public void actionPerformed(ActionEvent e) {
//                IdeaEditorHelpers.open_settings_xml_sync(project, root_file);
//            }
//        });
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
//        datastore_pyodbc.addActionListener(new ActionListener() {
//            @Override
//            public void actionPerformed(ActionEvent e) {
//                IdeaEditorHelpers.open_or_activate_jar_resource_in_editor(project, "data_store_pyodbc.py", "data_store_pyodbc.py");
//            }
//        });
//        dataStorePsycopg2Button.addActionListener(new ActionListener() {
//            @Override
//            public void actionPerformed(ActionEvent e) {
//                IdeaEditorHelpers.open_or_activate_jar_resource_in_editor(project, "data_store_psycopg2.py", "data_store.py");
//            }
//        });
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
//        btn_cx_Oracle.addActionListener(new ActionListener() {
//            @Override
//            public void actionPerformed(ActionEvent e) {
//                IdeaEditorHelpers.open_or_activate_jar_resource_in_editor(project, "data_store_cx_oracle.py", "data_store_cx_oracle.py");
//            }
//        });
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
//        djangoDbButton.addActionListener(new ActionListener() {
//            @Override
//            public void actionPerformed(ActionEvent e) {
//                IdeaEditorHelpers.open_or_activate_jar_resource_in_editor(project, "data_store_django.py", "data_store_django.py");
//            }
//        });
        sqlite3MysqlConnectorPsycopg2Button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                IdeaEditorHelpers.open_or_activate_jar_resource_in_editor(project, "data_store.py", "data_store.py");
            }
        });
        SQLAlchemyButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                IdeaEditorHelpers.open_or_activate_jar_resource_in_editor(project, "data_store_sqlalchemy.py", "data_store_sqlalchemy.py");
            }
        });
        gormButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                IdeaEditorHelpers.open_or_activate_jar_resource_in_editor(project, "data_store_gorm.go", "data_store_gorm.go");
            }
        });
        doctrineORMButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                IdeaEditorHelpers.open_or_activate_jar_resource_in_editor(project, "DataStore_Doctrine_ORM.php", "DataStore_Doctrine_ORM.php");
            }
        });
        buttonSettings.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                openSettings();
            }
        });
    }

    public void openSettings() {
        IdeaEditorHelpers.open_settings_xml_sync(project, root_file);
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
        IdeaMessageHelpers.add_info_to_ide_log("INFO", msg + " -> OK");
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
        panel1.setLayout(new FlowLayout(FlowLayout.LEADING, 5, 5));
        panel1.setMaximumSize(new Dimension(2147483647, 1));
        scroll_pane.setViewportView(panel1);
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridBagLayout());
        panel1.add(panel2);
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
        GridBagConstraints gbc;
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        panel2.add(panel3, gbc);
        buttonSettings = new JButton();
        buttonSettings.setText("settings.xml");
        panel3.add(buttonSettings);
        referenceSettingsXmlButton = new JButton();
        referenceSettingsXmlButton.setText("Reference settings.xml");
        panel3.add(referenceSettingsXmlButton);
        btn_validate_all = new JButton();
        btn_validate_all.setText("Validate Configuration");
        panel3.add(btn_validate_all);
        recentChangesButton = new JButton();
        recentChangesButton.setPreferredSize(new Dimension(60, 30));
        recentChangesButton.setText("News");
        panel3.add(recentChangesButton);
        vTextField = new JTextField();
        vTextField.setEditable(false);
        vTextField.setHorizontalAlignment(0);
        vTextField.setText("v. ?");
        panel3.add(vTextField);
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        panel2.add(panel4, gbc);
        createOverwriteXSDFilesButton = new JButton();
        createOverwriteXSDFilesButton.setText("Create/Overwrite XSD files");
        panel4.add(createOverwriteXSDFilesButton);
        createOverwriteSettingsXmlButton = new JButton();
        createOverwriteSettingsXmlButton.setText("Create/Overwrite settings.xml");
        panel4.add(createOverwriteSettingsXmlButton);
        createOverwriteDtoXmlButton = new JButton();
        createOverwriteDtoXmlButton.setText("Create/Overwrite dto.xml");
        panel4.add(createOverwriteDtoXmlButton);
        final JPanel panel5 = new JPanel();
        panel5.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 5));
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.anchor = GridBagConstraints.WEST;
        panel2.add(panel5, gbc);
        final JLabel label1 = new JLabel();
        Font label1Font = this.$$$getFont$$$(null, -1, 14, label1.getFont());
        if (label1Font != null) label1.setFont(label1Font);
        label1.setHorizontalAlignment(4);
        label1.setPreferredSize(new Dimension(52, 20));
        label1.setText(" PHP ");
        panel5.add(label1);
        dataStorePhpButton = new JButton();
        dataStorePhpButton.setBorderPainted(true);
        dataStorePhpButton.setContentAreaFilled(true);
        dataStorePhpButton.setMinimumSize(new Dimension(40, 30));
        dataStorePhpButton.setPreferredSize(new Dimension(60, 30));
        dataStorePhpButton.setText("Base");
        panel5.add(dataStorePhpButton);
        PDODataStorePhpButton = new JButton();
        PDODataStorePhpButton.setPreferredSize(new Dimension(60, 30));
        PDODataStorePhpButton.setText("sqlite3");
        panel5.add(PDODataStorePhpButton);
        btn_mysql = new JButton();
        btn_mysql.setPreferredSize(new Dimension(60, 30));
        btn_mysql.setText("mysql");
        panel5.add(btn_mysql);
        btn_php_oracle = new JButton();
        btn_php_oracle.setPreferredSize(new Dimension(60, 30));
        btn_php_oracle.setText("oracle");
        panel5.add(btn_php_oracle);
        dataStorePhpOCI8Button = new JButton();
        dataStorePhpOCI8Button.setPreferredSize(new Dimension(60, 30));
        dataStorePhpOCI8Button.setText("oci8");
        panel5.add(dataStorePhpOCI8Button);
        dataStorePhpPDOSQLButton = new JButton();
        dataStorePhpPDOSQLButton.setPreferredSize(new Dimension(60, 30));
        dataStorePhpPDOSQLButton.setText("ms sql");
        panel5.add(dataStorePhpPDOSQLButton);
        btn_php_pg = new JButton();
        btn_php_pg.setText("postgesql");
        panel5.add(btn_php_pg);
        doctrineORMButton = new JButton();
        doctrineORMButton.setText("Doctrine");
        panel5.add(doctrineORMButton);
        final JPanel panel6 = new JPanel();
        panel6.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 5));
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.anchor = GridBagConstraints.WEST;
        panel2.add(panel6, gbc);
        final JLabel label2 = new JLabel();
        Font label2Font = this.$$$getFont$$$(null, -1, 14, label2.getFont());
        if (label2Font != null) label2.setFont(label2Font);
        label2.setHorizontalAlignment(4);
        label2.setPreferredSize(new Dimension(52, 20));
        label2.setText(" Java ");
        panel6.add(label2);
        dataStoreJavaButton = new JButton();
        dataStoreJavaButton.setMinimumSize(new Dimension(40, 30));
        dataStoreJavaButton.setPreferredSize(new Dimension(60, 30));
        dataStoreJavaButton.setText("Base");
        panel6.add(dataStoreJavaButton);
        dataStoreJavaDbUtilsButton = new JButton();
        dataStoreJavaDbUtilsButton.setPreferredSize(new Dimension(60, 30));
        dataStoreJavaDbUtilsButton.setText("JDBC");
        panel6.add(dataStoreJavaDbUtilsButton);
        dataStoreJavaAndroidButton = new JButton();
        dataStoreJavaAndroidButton.setText("Android");
        panel6.add(dataStoreJavaAndroidButton);
        final JLabel label3 = new JLabel();
        Font label3Font = this.$$$getFont$$$(null, -1, 14, label3.getFont());
        if (label3Font != null) label3.setFont(label3Font);
        label3.setHorizontalAlignment(4);
        label3.setPreferredSize(new Dimension(40, 20));
        label3.setText(" C++ ");
        panel6.add(label3);
        dataStoreQt5CButton = new JButton();
        dataStoreQt5CButton.setPreferredSize(new Dimension(60, 30));
        dataStoreQt5CButton.setText("Qt");
        panel6.add(dataStoreQt5CButton);
        dataStoreCSTLButton = new JButton();
        dataStoreCSTLButton.setText("STL, sqlite3");
        panel6.add(dataStoreCSTLButton);
        dataStoreCATLButton = new JButton();
        dataStoreCATLButton.setText("ATL, sqlite3");
        panel6.add(dataStoreCATLButton);
        final JPanel panel7 = new JPanel();
        panel7.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 5));
        panel7.setMinimumSize(new Dimension(40, 40));
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.anchor = GridBagConstraints.WEST;
        panel2.add(panel7, gbc);
        final JLabel label4 = new JLabel();
        Font label4Font = this.$$$getFont$$$(null, -1, 14, label4.getFont());
        if (label4Font != null) label4.setFont(label4Font);
        label4.setHorizontalAlignment(4);
        label4.setPreferredSize(new Dimension(52, 20));
        label4.setText(" Python ");
        panel7.add(label4);
        sqlite3MysqlConnectorPsycopg2Button = new JButton();
        sqlite3MysqlConnectorPsycopg2Button.setText("sqlite3, mysql, cx_oracle, psycopg2, django.db");
        panel7.add(sqlite3MysqlConnectorPsycopg2Button);
        SQLAlchemyButton = new JButton();
        SQLAlchemyButton.setText("sqlalchemy");
        panel7.add(SQLAlchemyButton);
        final JPanel panel8 = new JPanel();
        panel8.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 5));
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.fill = GridBagConstraints.BOTH;
        panel2.add(panel8, gbc);
        final JLabel label5 = new JLabel();
        Font label5Font = this.$$$getFont$$$(null, -1, 14, label5.getFont());
        if (label5Font != null) label5.setFont(label5Font);
        label5.setHorizontalAlignment(4);
        label5.setPreferredSize(new Dimension(52, 20));
        label5.setText(" Go ");
        panel8.add(label5);
        gormButton = new JButton();
        gormButton.setPreferredSize(new Dimension(60, 30));
        gormButton.setText("Gorm");
        panel8.add(gormButton);
        btn_golang = new JButton();
        btn_golang.setText("database/sql");
        panel8.add(btn_golang);
        final JPanel panel9 = new JPanel();
        panel9.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 6;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        panel2.add(panel9, gbc);
        php_vm = new JButton();
        php_vm.setText("php.vm");
        panel9.add(php_vm);
        java_vm = new JButton();
        java_vm.setText("java.vm");
        panel9.add(java_vm);
        cppVmButton = new JButton();
        cppVmButton.setText("cpp.vm");
        panel9.add(cppVmButton);
        pythonVmButton = new JButton();
        pythonVmButton.setText("python.vm");
        panel9.add(pythonVmButton);
        btn_golangVM = new JButton();
        btn_golangVM.setText("go.vm");
        panel9.add(btn_golangVM);
        final JPanel panel10 = new JPanel();
        panel10.setLayout(new BorderLayout(0, 0));
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 7;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.anchor = GridBagConstraints.NORTH;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(8, 8, 0, 8);
        panel2.add(panel10, gbc);
        text1 = new JTextPane();
        text1.setEditable(false);
        text1.setMargin(new Insets(0, 15, 15, 15));
        text1.setText("");
        panel10.add(text1, BorderLayout.CENTER);
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
