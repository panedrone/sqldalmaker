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
import com.sqldalmaker.common.XmlHelpers;
import com.sqldalmaker.jaxb.settings.Settings;

import javax.swing.*;
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
    private JButton php_vm;
    private JButton java_vm;
    private JButton recentChangesButton;
    private JButton dataStoreQt5CButton;
    private JTextField vTextField;
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
    private JButton djangoDbButton;
    private JButton flaskSqlalchemyButton;
    private JButton cx_oracleButton;
    private JPanel pnl_php;
    private JPanel pnl_java;
    private JPanel pnl_py;
    private JPanel pnl_go;
    private JPanel pnl_cpp;
    private JButton btn_gorn;
    private JButton btn_go_no_orm;
    private JButton btn_sqla;
    private JButton btn_django;
    private JButton btn_cx_oracle;
    private JButton btn_py_no_orm;
    private JButton btn_cpp;
    private JButton btn_php_no_orm;
    private JButton btn_doctrine;
    private JButton btn_jdbc;
    private JButton btn_android;
    private JPanel pnl_settings;
    private JPanel pnl_vm;
    private JPanel pnl_xsd;
    private JButton aboutButton;
    private JButton btn_sqlx_xml;
    private JButton btn_sqlx;

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

                String sdm_info = IdeaHelpers.get_sdm_info();
                vTextField.setText(sdm_info);
                vTextField.setBorder(BorderFactory.createEmptyBorder());
                // https://stackoverflow.com/questions/291115/java-swing-using-jscrollpane-and-having-it-scroll-back-to-top
                scroll_pane.getVerticalScrollBar().setValue(0);
                // https://stackoverflow.com/questions/5583495/how-do-i-speed-up-the-scroll-speed-in-a-jscrollpane-when-using-the-mouse-wheel
                scroll_pane.getVerticalScrollBar().setUnitIncrement(20);
            }
        });
    }

    private static void set_cursor(Cursor wc, JPanel panel) {
        panel.setOpaque(false);
        for (Component c : panel.getComponents()) {
            if (c instanceof JButton) {
                JButton b = (JButton) c;
                b.setCursor(wc);
//                b.setFocusPainted(false);
//                // https://stackoverflow.com/questions/4585867/transparent-jbutton
//                b.setOpaque(false);
//                b.setContentAreaFilled(false);
//                b.setBorderPainted(false);
//                // https://coderanch.com/t/336633/java/transparent-jbuttons
//                b.setBorder(null);
            }
        }
    }

    public UITabAdmin() {

        Cursor wc = new Cursor(Cursor.HAND_CURSOR);
        set_cursor(wc, pnl_cpp);
        set_cursor(wc, pnl_go);
        set_cursor(wc, pnl_java);
        set_cursor(wc, pnl_php);
        set_cursor(wc, pnl_py);

        set_cursor(wc, pnl_settings);
        set_cursor(wc, pnl_xsd);
        set_cursor(wc, pnl_vm);

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
                IdeaEditorHelpers.open_or_activate_jar_resource_in_editor(project, "DataStore_JDBC.java_", "DataStore.java");
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
                IdeaEditorHelpers.open_or_activate_jar_resource_in_editor(project, "DataStore_Android.java_", "DataStore.java");
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
//        datastore_pyodbc.addActionListener(new ActionListener() {
//            @Override
//            public void actionPerformed(ActionEvent e) {
//                IdeaEditorHelpers.open_or_activate_jar_resource_in_editor(project, "data_store_pyodbc.py", "data_store_pyodbc.py");
//            }
//        });
//        dataStorePsycopg2Button.addActionListener(new ActionListener() {
//            @Override
//            public void actionPerformed(ActionEvent e) {
//                IdeaEditorHelpers.open_or_activate_jar_resource_in_editor(project, "data_store_psycopg2.py", "data_store_no_orm.py");
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
        dataStorePhpOCI8Button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                IdeaEditorHelpers.open_or_activate_jar_resource_in_editor(project, "DataStore_oci8.php", "DataStore_oci8.php");
            }
        });
        btn_golang.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                IdeaEditorHelpers.open_or_activate_jar_resource_in_editor(project, "data_store_no_orm.go", "data_store_no_orm.go");
            }
        });
        btn_golangVM.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                IdeaEditorHelpers.open_or_activate_jar_file_in_editor(project, "com/sqldalmaker/cg/go", "go.vm", "go.vm");
            }
        });
        sqlite3MysqlConnectorPsycopg2Button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                IdeaEditorHelpers.open_or_activate_jar_resource_in_editor(project, "data_store_no_orm.py", "data_store.py");
            }
        });
        SQLAlchemyButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                IdeaEditorHelpers.open_or_activate_jar_resource_in_editor(project, "data_store_sqlalchemy.py", "data_store.py");
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
        djangoDbButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                IdeaEditorHelpers.open_or_activate_jar_resource_in_editor(project, "data_store_django.py", "data_store.py");
            }
        });
        flaskSqlalchemyButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                IdeaEditorHelpers.open_or_activate_jar_resource_in_editor(project, "data_store_flask_sqlalchemy.py", "data_store.py");
            }
        });
        cx_oracleButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                IdeaEditorHelpers.open_or_activate_jar_resource_in_editor(project, "data_store_no_orm_cx_oracle.py", "data_store.py");
            }
        });
        btn_gorn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                IdeaEditorHelpers.open_or_activate_jar_resource_in_editor(project, "data_store_gorm.go.settings.xml", "settings.xml");
            }
        });
        btn_go_no_orm.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                IdeaEditorHelpers.open_or_activate_jar_resource_in_editor(project, "data_store_no_orm.go.settings.xml", "settings.xml");
            }
        });
        btn_sqla.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                IdeaEditorHelpers.open_or_activate_jar_resource_in_editor(project, "data_store_sqlalchemy.py.settings.xml", "settings.xml");
            }
        });
        btn_django.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                IdeaEditorHelpers.open_or_activate_jar_resource_in_editor(project, "data_store_django.py.settings.xml", "settings.xml");
            }
        });
        btn_cx_oracle.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                IdeaEditorHelpers.open_or_activate_jar_resource_in_editor(project, "data_store_no_orm_cx_oracle.py.settings.xml", "settings.xml");
            }
        });
        btn_py_no_orm.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                IdeaEditorHelpers.open_or_activate_jar_resource_in_editor(project, "data_store_no_orm.py.settings.xml", "settings.xml");
            }
        });
        btn_cpp.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                IdeaEditorHelpers.open_or_activate_jar_resource_in_editor(project, "DataStore.cpp.settings.xml", "settings.xml");
            }
        });
        btn_php_no_orm.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                IdeaEditorHelpers.open_or_activate_jar_resource_in_editor(project, "DataStore_PDO_SQLite3.php.settings.xml", "settings.xml");
            }
        });
        btn_doctrine.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                IdeaEditorHelpers.open_or_activate_jar_resource_in_editor(project, "DataStore_Doctrine_ORM.php.settings.xml", "settings.xml");
            }
        });
        btn_jdbc.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                IdeaEditorHelpers.open_or_activate_jar_resource_in_editor(project, "DataStore.java.JDBC.settings.xml", "settings.xml");
            }
        });
        btn_android.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                IdeaEditorHelpers.open_or_activate_jar_resource_in_editor(project, "DataStore.java.android.settings.xml", "settings.xml");
            }
        });
        aboutButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                UIDialogAbout.show_modal();
            }
        });
        btn_sqlx_xml.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                IdeaEditorHelpers.open_or_activate_jar_resource_in_editor(project, "data_store_sqlx.go.settings.xml", "settings.xml");
            }
        });
        btn_sqlx.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                IdeaEditorHelpers.open_or_activate_jar_resource_in_editor(project, "data_store_sqlx.go", "data_store_sqlx.go");
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
            String cur_xsd;
            try {
                cur_xsd = Helpers.load_text_from_file(xsd_file.getPath());
            } catch (Exception ex) {
                add_err_msg(buff, get_err_msg(ex));
                return false;
            }
            String ref_xsd;
            try {
                ref_xsd = IdeaHelpers.read_from_jar_file(xsd_name);
            } catch (Exception ex) {
                add_err_msg(buff, get_err_msg(ex));
                return false;
            }
            if (XmlHelpers.compareXml(ref_xsd, cur_xsd) == 0) {
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
        pnl_settings = new JPanel();
        pnl_settings.setLayout(new GridBagLayout());
        GridBagConstraints gbc;
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        panel2.add(pnl_settings, gbc);
        buttonSettings = new JButton();
        buttonSettings.setText("settings.xml");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        pnl_settings.add(buttonSettings, gbc);
        btn_validate_all = new JButton();
        btn_validate_all.setText("Validate Configuration");
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 0;
        pnl_settings.add(btn_validate_all, gbc);
        vTextField = new JTextField();
        vTextField.setEditable(false);
        vTextField.setHorizontalAlignment(0);
        vTextField.setText("v. ?");
        gbc = new GridBagConstraints();
        gbc.gridx = 2;
        gbc.gridy = 0;
        pnl_settings.add(vTextField, gbc);
        recentChangesButton = new JButton();
        recentChangesButton.setPreferredSize(new Dimension(60, 30));
        recentChangesButton.setText("News");
        gbc = new GridBagConstraints();
        gbc.gridx = 3;
        gbc.gridy = 0;
        pnl_settings.add(recentChangesButton, gbc);
        aboutButton = new JButton();
        aboutButton.setPreferredSize(new Dimension(60, 30));
        aboutButton.setText("About");
        gbc = new GridBagConstraints();
        gbc.gridx = 4;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        pnl_settings.add(aboutButton, gbc);
        pnl_php = new JPanel();
        pnl_php.setLayout(new GridBagLayout());
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.WEST;
        panel2.add(pnl_php, gbc);
        final JLabel label1 = new JLabel();
        Font label1Font = this.$$$getFont$$$(null, -1, 14, label1.getFont());
        if (label1Font != null) label1.setFont(label1Font);
        label1.setHorizontalAlignment(4);
        label1.setPreferredSize(new Dimension(58, 20));
        label1.setText(" PHP ");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        pnl_php.add(label1, gbc);
        PDODataStorePhpButton = new JButton();
        PDODataStorePhpButton.setPreferredSize(new Dimension(60, 30));
        PDODataStorePhpButton.setText("sqlite3");
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 0;
        pnl_php.add(PDODataStorePhpButton, gbc);
        btn_mysql = new JButton();
        btn_mysql.setPreferredSize(new Dimension(60, 30));
        btn_mysql.setText("mysql");
        gbc = new GridBagConstraints();
        gbc.gridx = 3;
        gbc.gridy = 0;
        pnl_php.add(btn_mysql, gbc);
        btn_php_oracle = new JButton();
        btn_php_oracle.setPreferredSize(new Dimension(60, 30));
        btn_php_oracle.setText("oracle");
        gbc = new GridBagConstraints();
        gbc.gridx = 4;
        gbc.gridy = 0;
        pnl_php.add(btn_php_oracle, gbc);
        dataStorePhpOCI8Button = new JButton();
        dataStorePhpOCI8Button.setPreferredSize(new Dimension(60, 30));
        dataStorePhpOCI8Button.setText("oci8");
        gbc = new GridBagConstraints();
        gbc.gridx = 5;
        gbc.gridy = 0;
        pnl_php.add(dataStorePhpOCI8Button, gbc);
        dataStorePhpPDOSQLButton = new JButton();
        dataStorePhpPDOSQLButton.setPreferredSize(new Dimension(60, 30));
        dataStorePhpPDOSQLButton.setText("ms sql");
        gbc = new GridBagConstraints();
        gbc.gridx = 6;
        gbc.gridy = 0;
        pnl_php.add(dataStorePhpPDOSQLButton, gbc);
        doctrineORMButton = new JButton();
        doctrineORMButton.setText("Doctrine");
        gbc = new GridBagConstraints();
        gbc.gridx = 8;
        gbc.gridy = 0;
        pnl_php.add(doctrineORMButton, gbc);
        btn_php_pg = new JButton();
        btn_php_pg.setText("postgesql");
        gbc = new GridBagConstraints();
        gbc.gridx = 2;
        gbc.gridy = 0;
        pnl_php.add(btn_php_pg, gbc);
        btn_doctrine = new JButton();
        btn_doctrine.setBorderPainted(true);
        btn_doctrine.setContentAreaFilled(true);
        btn_doctrine.setIcon(new ImageIcon(getClass().getResource("/img/xmldoc_12x12.gif")));
        btn_doctrine.setMargin(new Insets(0, 0, 0, 0));
        btn_doctrine.setMaximumSize(new Dimension(22, 24));
        btn_doctrine.setMinimumSize(new Dimension(22, 24));
        btn_doctrine.setOpaque(true);
        btn_doctrine.setPreferredSize(new Dimension(22, 24));
        btn_doctrine.setText("");
        btn_doctrine.setToolTipText("settings.xml");
        gbc = new GridBagConstraints();
        gbc.gridx = 9;
        gbc.gridy = 0;
        pnl_php.add(btn_doctrine, gbc);
        btn_php_no_orm = new JButton();
        btn_php_no_orm.setBorderPainted(true);
        btn_php_no_orm.setContentAreaFilled(true);
        btn_php_no_orm.setIcon(new ImageIcon(getClass().getResource("/img/xmldoc_12x12.gif")));
        btn_php_no_orm.setMargin(new Insets(0, 0, 0, 0));
        btn_php_no_orm.setMaximumSize(new Dimension(22, 24));
        btn_php_no_orm.setMinimumSize(new Dimension(22, 24));
        btn_php_no_orm.setOpaque(true);
        btn_php_no_orm.setPreferredSize(new Dimension(22, 24));
        btn_php_no_orm.setText("");
        btn_php_no_orm.setToolTipText("settings.xml");
        gbc = new GridBagConstraints();
        gbc.gridx = 7;
        gbc.gridy = 0;
        pnl_php.add(btn_php_no_orm, gbc);
        pnl_java = new JPanel();
        pnl_java.setLayout(new GridBagLayout());
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.WEST;
        panel2.add(pnl_java, gbc);
        final JLabel label2 = new JLabel();
        Font label2Font = this.$$$getFont$$$(null, -1, 14, label2.getFont());
        if (label2Font != null) label2.setFont(label2Font);
        label2.setHorizontalAlignment(4);
        label2.setPreferredSize(new Dimension(58, 20));
        label2.setText(" Java ");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        pnl_java.add(label2, gbc);
        dataStoreJavaAndroidButton = new JButton();
        dataStoreJavaAndroidButton.setText("Android");
        gbc = new GridBagConstraints();
        gbc.gridx = 5;
        gbc.gridy = 0;
        pnl_java.add(dataStoreJavaAndroidButton, gbc);
        btn_android = new JButton();
        btn_android.setBorderPainted(true);
        btn_android.setContentAreaFilled(true);
        btn_android.setIcon(new ImageIcon(getClass().getResource("/img/xmldoc_12x12.gif")));
        btn_android.setMargin(new Insets(0, 0, 0, 0));
        btn_android.setMaximumSize(new Dimension(22, 24));
        btn_android.setMinimumSize(new Dimension(22, 24));
        btn_android.setOpaque(true);
        btn_android.setPreferredSize(new Dimension(22, 24));
        btn_android.setText("");
        btn_android.setToolTipText("settings.xml");
        gbc = new GridBagConstraints();
        gbc.gridx = 6;
        gbc.gridy = 0;
        pnl_java.add(btn_android, gbc);
        btn_jdbc = new JButton();
        btn_jdbc.setBorderPainted(true);
        btn_jdbc.setContentAreaFilled(true);
        btn_jdbc.setIcon(new ImageIcon(getClass().getResource("/img/xmldoc_12x12.gif")));
        btn_jdbc.setMargin(new Insets(0, 0, 0, 0));
        btn_jdbc.setMaximumSize(new Dimension(22, 24));
        btn_jdbc.setMinimumSize(new Dimension(22, 24));
        btn_jdbc.setOpaque(true);
        btn_jdbc.setPreferredSize(new Dimension(22, 24));
        btn_jdbc.setText("");
        btn_jdbc.setToolTipText("settings.xml");
        gbc = new GridBagConstraints();
        gbc.gridx = 3;
        gbc.gridy = 0;
        pnl_java.add(btn_jdbc, gbc);
        dataStoreJavaButton = new JButton();
        dataStoreJavaButton.setMinimumSize(new Dimension(40, 30));
        dataStoreJavaButton.setPreferredSize(new Dimension(60, 30));
        dataStoreJavaButton.setText("Base");
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 0;
        pnl_java.add(dataStoreJavaButton, gbc);
        dataStoreJavaDbUtilsButton = new JButton();
        dataStoreJavaDbUtilsButton.setPreferredSize(new Dimension(60, 30));
        dataStoreJavaDbUtilsButton.setText("JDBC");
        gbc = new GridBagConstraints();
        gbc.gridx = 2;
        gbc.gridy = 0;
        pnl_java.add(dataStoreJavaDbUtilsButton, gbc);
        dataStorePhpButton = new JButton();
        dataStorePhpButton.setBorderPainted(true);
        dataStorePhpButton.setContentAreaFilled(true);
        dataStorePhpButton.setMinimumSize(new Dimension(40, 30));
        dataStorePhpButton.setPreferredSize(new Dimension(60, 30));
        dataStorePhpButton.setText("Base");
        gbc = new GridBagConstraints();
        gbc.gridx = 4;
        gbc.gridy = 0;
        pnl_java.add(dataStorePhpButton, gbc);
        pnl_py = new JPanel();
        pnl_py.setLayout(new GridBagLayout());
        pnl_py.setMinimumSize(new Dimension(40, 40));
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.WEST;
        panel2.add(pnl_py, gbc);
        final JLabel label3 = new JLabel();
        Font label3Font = this.$$$getFont$$$(null, -1, 14, label3.getFont());
        if (label3Font != null) label3.setFont(label3Font);
        label3.setHorizontalAlignment(4);
        label3.setPreferredSize(new Dimension(58, 20));
        label3.setText("  Python ");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        pnl_py.add(label3, gbc);
        sqlite3MysqlConnectorPsycopg2Button = new JButton();
        sqlite3MysqlConnectorPsycopg2Button.setText("sqlite3, mysql, psycopg2");
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 0;
        pnl_py.add(sqlite3MysqlConnectorPsycopg2Button, gbc);
        cx_oracleButton = new JButton();
        cx_oracleButton.setText("cx_oracle");
        gbc = new GridBagConstraints();
        gbc.gridx = 3;
        gbc.gridy = 0;
        pnl_py.add(cx_oracleButton, gbc);
        djangoDbButton = new JButton();
        djangoDbButton.setText("django.db");
        gbc = new GridBagConstraints();
        gbc.gridx = 5;
        gbc.gridy = 0;
        pnl_py.add(djangoDbButton, gbc);
        SQLAlchemyButton = new JButton();
        SQLAlchemyButton.setText("SQLAlchemy");
        gbc = new GridBagConstraints();
        gbc.gridx = 7;
        gbc.gridy = 0;
        pnl_py.add(SQLAlchemyButton, gbc);
        flaskSqlalchemyButton = new JButton();
        flaskSqlalchemyButton.setText("Flask-SQLAlchemy");
        gbc = new GridBagConstraints();
        gbc.gridx = 8;
        gbc.gridy = 0;
        pnl_py.add(flaskSqlalchemyButton, gbc);
        btn_py_no_orm = new JButton();
        btn_py_no_orm.setBorderPainted(true);
        btn_py_no_orm.setContentAreaFilled(true);
        btn_py_no_orm.setIcon(new ImageIcon(getClass().getResource("/img/xmldoc_12x12.gif")));
        btn_py_no_orm.setMargin(new Insets(0, 0, 0, 0));
        btn_py_no_orm.setMaximumSize(new Dimension(22, 24));
        btn_py_no_orm.setMinimumSize(new Dimension(22, 24));
        btn_py_no_orm.setOpaque(true);
        btn_py_no_orm.setPreferredSize(new Dimension(22, 24));
        btn_py_no_orm.setText("");
        btn_py_no_orm.setToolTipText("settings.xml");
        gbc = new GridBagConstraints();
        gbc.gridx = 2;
        gbc.gridy = 0;
        pnl_py.add(btn_py_no_orm, gbc);
        btn_sqla = new JButton();
        btn_sqla.setBorderPainted(true);
        btn_sqla.setContentAreaFilled(true);
        btn_sqla.setIcon(new ImageIcon(getClass().getResource("/img/xmldoc_12x12.gif")));
        btn_sqla.setMargin(new Insets(0, 0, 0, 0));
        btn_sqla.setMaximumSize(new Dimension(22, 24));
        btn_sqla.setMinimumSize(new Dimension(22, 24));
        btn_sqla.setOpaque(true);
        btn_sqla.setPreferredSize(new Dimension(22, 24));
        btn_sqla.setText("");
        btn_sqla.setToolTipText("settings.xml");
        gbc = new GridBagConstraints();
        gbc.gridx = 9;
        gbc.gridy = 0;
        pnl_py.add(btn_sqla, gbc);
        btn_django = new JButton();
        btn_django.setBorderPainted(true);
        btn_django.setContentAreaFilled(true);
        btn_django.setIcon(new ImageIcon(getClass().getResource("/img/xmldoc_12x12.gif")));
        btn_django.setMargin(new Insets(0, 0, 0, 0));
        btn_django.setMaximumSize(new Dimension(22, 24));
        btn_django.setMinimumSize(new Dimension(22, 24));
        btn_django.setOpaque(true);
        btn_django.setPreferredSize(new Dimension(22, 24));
        btn_django.setText("");
        btn_django.setToolTipText("settings.xml");
        gbc = new GridBagConstraints();
        gbc.gridx = 6;
        gbc.gridy = 0;
        pnl_py.add(btn_django, gbc);
        btn_cx_oracle = new JButton();
        btn_cx_oracle.setBorderPainted(true);
        btn_cx_oracle.setContentAreaFilled(true);
        btn_cx_oracle.setIcon(new ImageIcon(getClass().getResource("/img/xmldoc_12x12.gif")));
        btn_cx_oracle.setMargin(new Insets(0, 0, 0, 0));
        btn_cx_oracle.setMaximumSize(new Dimension(22, 24));
        btn_cx_oracle.setMinimumSize(new Dimension(22, 24));
        btn_cx_oracle.setOpaque(true);
        btn_cx_oracle.setPreferredSize(new Dimension(22, 24));
        btn_cx_oracle.setText("");
        btn_cx_oracle.setToolTipText("settings.xml");
        gbc = new GridBagConstraints();
        gbc.gridx = 4;
        gbc.gridy = 0;
        pnl_py.add(btn_cx_oracle, gbc);
        pnl_go = new JPanel();
        pnl_go.setLayout(new GridBagLayout());
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 6;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.VERTICAL;
        panel2.add(pnl_go, gbc);
        final JLabel label4 = new JLabel();
        Font label4Font = this.$$$getFont$$$(null, -1, 14, label4.getFont());
        if (label4Font != null) label4.setFont(label4Font);
        label4.setHorizontalAlignment(4);
        label4.setPreferredSize(new Dimension(58, 20));
        label4.setText(" Go ");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        pnl_go.add(label4, gbc);
        gormButton = new JButton();
        gormButton.setPreferredSize(new Dimension(60, 30));
        gormButton.setText("Gorm");
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 0;
        pnl_go.add(gormButton, gbc);
        btn_golang = new JButton();
        btn_golang.setText("database/sql");
        gbc = new GridBagConstraints();
        gbc.gridx = 3;
        gbc.gridy = 0;
        pnl_go.add(btn_golang, gbc);
        btn_go_no_orm = new JButton();
        btn_go_no_orm.setBorderPainted(true);
        btn_go_no_orm.setContentAreaFilled(true);
        btn_go_no_orm.setIcon(new ImageIcon(getClass().getResource("/img/xmldoc_12x12.gif")));
        btn_go_no_orm.setMargin(new Insets(0, 0, 0, 0));
        btn_go_no_orm.setMaximumSize(new Dimension(22, 24));
        btn_go_no_orm.setMinimumSize(new Dimension(22, 24));
        btn_go_no_orm.setOpaque(true);
        btn_go_no_orm.setPreferredSize(new Dimension(22, 24));
        btn_go_no_orm.setText("");
        btn_go_no_orm.setToolTipText("settings.xml");
        gbc = new GridBagConstraints();
        gbc.gridx = 4;
        gbc.gridy = 0;
        pnl_go.add(btn_go_no_orm, gbc);
        btn_gorn = new JButton();
        btn_gorn.setBorderPainted(true);
        btn_gorn.setContentAreaFilled(true);
        btn_gorn.setIcon(new ImageIcon(getClass().getResource("/img/xmldoc_12x12.gif")));
        btn_gorn.setMargin(new Insets(0, 0, 0, 0));
        btn_gorn.setMaximumSize(new Dimension(22, 24));
        btn_gorn.setMinimumSize(new Dimension(22, 24));
        btn_gorn.setOpaque(true);
        btn_gorn.setPreferredSize(new Dimension(22, 24));
        btn_gorn.setText("");
        btn_gorn.setToolTipText("settings.xml");
        gbc = new GridBagConstraints();
        gbc.gridx = 2;
        gbc.gridy = 0;
        pnl_go.add(btn_gorn, gbc);
        btn_sqlx = new JButton();
        btn_sqlx.setPreferredSize(new Dimension(60, 30));
        btn_sqlx.setText("sqlx");
        gbc = new GridBagConstraints();
        gbc.gridx = 5;
        gbc.gridy = 0;
        pnl_go.add(btn_sqlx, gbc);
        btn_sqlx_xml = new JButton();
        btn_sqlx_xml.setBorderPainted(true);
        btn_sqlx_xml.setContentAreaFilled(true);
        btn_sqlx_xml.setIcon(new ImageIcon(getClass().getResource("/img/xmldoc_12x12.gif")));
        btn_sqlx_xml.setMargin(new Insets(0, 0, 0, 0));
        btn_sqlx_xml.setMaximumSize(new Dimension(22, 24));
        btn_sqlx_xml.setMinimumSize(new Dimension(22, 24));
        btn_sqlx_xml.setOpaque(true);
        btn_sqlx_xml.setPreferredSize(new Dimension(22, 24));
        btn_sqlx_xml.setText("");
        btn_sqlx_xml.setToolTipText("settings.xml");
        gbc = new GridBagConstraints();
        gbc.gridx = 6;
        gbc.gridy = 0;
        pnl_go.add(btn_sqlx_xml, gbc);
        pnl_vm = new JPanel();
        pnl_vm.setLayout(new GridBagLayout());
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 7;
        gbc.gridwidth = 4;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        panel2.add(pnl_vm, gbc);
        php_vm = new JButton();
        php_vm.setText("php.vm");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        pnl_vm.add(php_vm, gbc);
        java_vm = new JButton();
        java_vm.setText("java.vm");
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 0;
        pnl_vm.add(java_vm, gbc);
        cppVmButton = new JButton();
        cppVmButton.setText("cpp.vm");
        gbc = new GridBagConstraints();
        gbc.gridx = 2;
        gbc.gridy = 0;
        pnl_vm.add(cppVmButton, gbc);
        pythonVmButton = new JButton();
        pythonVmButton.setText("python.vm");
        gbc = new GridBagConstraints();
        gbc.gridx = 3;
        gbc.gridy = 0;
        pnl_vm.add(pythonVmButton, gbc);
        btn_golangVM = new JButton();
        btn_golangVM.setText("go.vm");
        gbc = new GridBagConstraints();
        gbc.gridx = 4;
        gbc.gridy = 0;
        pnl_vm.add(btn_golangVM, gbc);
        pnl_xsd = new JPanel();
        pnl_xsd.setLayout(new GridBagLayout());
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.BOTH;
        panel2.add(pnl_xsd, gbc);
        createOverwriteXSDFilesButton = new JButton();
        createOverwriteXSDFilesButton.setText("Create/Overwrite XSD files");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        pnl_xsd.add(createOverwriteXSDFilesButton, gbc);
        createOverwriteSettingsXmlButton = new JButton();
        createOverwriteSettingsXmlButton.setText("Create settings.xml");
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 0;
        pnl_xsd.add(createOverwriteSettingsXmlButton, gbc);
        createOverwriteDtoXmlButton = new JButton();
        createOverwriteDtoXmlButton.setText("Create dto.xml");
        gbc = new GridBagConstraints();
        gbc.gridx = 2;
        gbc.gridy = 0;
        pnl_xsd.add(createOverwriteDtoXmlButton, gbc);
        pnl_cpp = new JPanel();
        pnl_cpp.setLayout(new GridBagLayout());
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.VERTICAL;
        panel2.add(pnl_cpp, gbc);
        final JLabel label5 = new JLabel();
        Font label5Font = this.$$$getFont$$$(null, -1, 14, label5.getFont());
        if (label5Font != null) label5.setFont(label5Font);
        label5.setHorizontalAlignment(4);
        label5.setPreferredSize(new Dimension(58, 20));
        label5.setText(" C++ ");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.anchor = GridBagConstraints.WEST;
        pnl_cpp.add(label5, gbc);
        dataStoreQt5CButton = new JButton();
        dataStoreQt5CButton.setPreferredSize(new Dimension(60, 30));
        dataStoreQt5CButton.setText("Qt");
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 0;
        pnl_cpp.add(dataStoreQt5CButton, gbc);
        dataStoreCSTLButton = new JButton();
        dataStoreCSTLButton.setText("STL, sqlite3");
        gbc = new GridBagConstraints();
        gbc.gridx = 2;
        gbc.gridy = 0;
        pnl_cpp.add(dataStoreCSTLButton, gbc);
        dataStoreCATLButton = new JButton();
        dataStoreCATLButton.setText("ATL, sqlite3");
        gbc = new GridBagConstraints();
        gbc.gridx = 3;
        gbc.gridy = 0;
        pnl_cpp.add(dataStoreCATLButton, gbc);
        btn_cpp = new JButton();
        btn_cpp.setBorderPainted(true);
        btn_cpp.setContentAreaFilled(true);
        btn_cpp.setIcon(new ImageIcon(getClass().getResource("/img/xmldoc_12x12.gif")));
        btn_cpp.setMargin(new Insets(0, 0, 0, 0));
        btn_cpp.setMaximumSize(new Dimension(22, 24));
        btn_cpp.setMinimumSize(new Dimension(22, 24));
        btn_cpp.setOpaque(true);
        btn_cpp.setPreferredSize(new Dimension(22, 24));
        btn_cpp.setText("");
        btn_cpp.setToolTipText("settings.xml");
        gbc = new GridBagConstraints();
        gbc.gridx = 4;
        gbc.gridy = 0;
        pnl_cpp.add(btn_cpp, gbc);
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
