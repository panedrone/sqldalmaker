/*
 * Copyright 2011-2020 sqldalmaker@gmail.com
 * SQL DAL Maker Website: http://sqldalmaker.sourceforge.net
 * Read LICENSE.txt in the root of this project/archive for details.
 */
package com.sqldalmaker.intellij.ui;

import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.JBColor;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.sqldalmaker.cg.Helpers;
import com.sqldalmaker.cg.IDaoCG;
import com.sqldalmaker.common.*;
import com.sqldalmaker.jaxb.dao.DaoClass;
import com.sqldalmaker.jaxb.settings.Settings;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.regex.PatternSyntaxException;

/**
 * Created with IntelliJ IDEA.
 * User: sqldalmaker@gmail.com
 * Date: 21.06.12
 */
public class UITabDAO {
    private JButton btn_NewXML;
    private JTable table;
    protected JPanel rootPanel;
    private JTextField textField1;
    private JButton btn_Refresh;
    private JButton btn_SelAll;
    private JButton btn_DeselAll;
    private JButton btn_Generate;
    private JButton btn_Validate;
    private JButton btn_OpenXML;
    private JButton btn_OpenJava;
    private JButton btn_CrudDao;
    private JPanel top_panel_1;
    private JButton button_fk_assistant;
    private JPanel tool_panel;

    private TableRowSorter<AbstractTableModel> sorter;
    private Project project;
    private VirtualFile root_file;
    private MyTableModel my_table_model;

    public JComponent get_tool_bar() {
        return tool_panel;
    }

    public UITabDAO() {
        $$$setupUI$$$();
        rootPanel.remove(top_panel_1);
        textField1.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent e) {
                set_filter();
            }

            public void removeUpdate(DocumentEvent e) {
                set_filter();
            }

            public void insertUpdate(DocumentEvent e) {
                set_filter();
            }
        });
        btn_Refresh.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                reload_table(true);
            }
        });
        btn_SelAll.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ListSelectionModel selectionModel = table.getSelectionModel();
                selectionModel.setSelectionInterval(0, table.getRowCount() - 1);
            }
        });
        btn_DeselAll.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                table.clearSelection();
            }
        });
        btn_Validate.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                validate();
            }
        });
        btn_Generate.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                generate_sync();
            }
        });
        btn_OpenXML.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                open_xml();
            }
        });
        btn_OpenJava.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                open_generated_source_file();
            }
        });
        btn_NewXML.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                new_dao_xml();
            }
        });
        btn_CrudDao.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                generate_crud_dao();
            }
        });
        Cursor wc = new Cursor(Cursor.HAND_CURSOR);
        tool_panel.setOpaque(false);
        for (Component c : tool_panel.getComponents()) {
            if (c instanceof JButton) {
                JButton b = (JButton) c;
                b.setCursor(wc);
                b.setFocusPainted(false);
                // https://stackoverflow.com/questions/4585867/transparent-jbutton
                b.setOpaque(false);
                b.setContentAreaFilled(false);
                b.setBorderPainted(false);
                // https://coderanch.com/t/336633/java/transparent-jbuttons
                b.setBorder(null);
            }
        }
        button_fk_assistant.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                IdeaCrudXmlHelpers.get_fk_access_xml(project, root_file);
            }
        });
    }

    private void createUIComponents() {
        final ColorRenderer colorRenderer = new ColorRenderer();
        table = new JTable() {
            public TableCellRenderer getCellRenderer(int row, int column) {
                if (column == 1) {
                    return colorRenderer;
                }
                return super.getCellRenderer(row, column);
            }
        };
        // createUIComponents() is called before constructors
        my_table_model = new MyTableModel();
        table.setModel(my_table_model);
        table.getTableHeader().setReorderingAllowed(false);

        sorter = new TableRowSorter<AbstractTableModel>(my_table_model);
        table.setRowSorter(sorter);
        table.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int col = table.columnAtPoint(new Point(e.getX(), e.getY()));
                    int row = table.rowAtPoint(new Point(e.getX(), e.getY()));
                    if (row >= 0) {
                        if (col == 0) {
                            open_xml();
                        } else if (col == 1) {
                            open_generated_source_file();
                        }
                    }
                }
            }
        });
        {
            TableColumn col = table.getColumnModel().getColumn(0);
            col.setPreferredWidth(416);
        }
        {
            TableColumn col = table.getColumnModel().getColumn(1);
            col.setPreferredWidth(300);
        }
    }

    private void generate_crud_dao() {
        IdeaCrudXmlHelpers.get_crud_dao_xml(project, root_file);
    }

    private void new_dao_xml() {
        final UIDialogNewDaoXml d = new UIDialogNewDaoXml(project, root_file);
        d.pack();
        d.setLocationRelativeTo(null);  // after pack!!!
        d.addWindowListener(new WindowAdapter() {
            public void windowClosed(WindowEvent e) {
                if (d.isSuccess()) {
                    reload_table(true);
                }
            }
        });
        d.setVisible(true);
    }

    protected void open_xml() {
        try {
            int[] selectedRows = get_selection();
            String relDirPath = (String) table.getValueAt(selectedRows[0], 0);
            IdeaEditorHelpers.open_local_file_in_editor_sync(project, root_file, relDirPath);
        } catch (Exception e) {
            e.printStackTrace();
            IdeaMessageHelpers.show_error_in_ui_thread(e);
        }
    }

    protected void open_generated_source_file() {
        try {
            int[] selectedRows = get_selection();
            final Settings settings = IdeaHelpers.load_settings(root_file);
            String v = (String) table.getValueAt(selectedRows[0], 0);
            String value = Helpers.get_dao_class_name(v);
            IdeaTargetLanguageHelpers.open_editor(project, root_file, value, settings, settings.getDao().getScope());
        } catch (Exception e) {
            e.printStackTrace();
            IdeaMessageHelpers.show_error_in_ui_thread(e);
        }
    }

    protected void generate_sync() {
        final java.util.List<IdeaHelpers.GeneratedFileData> list = new ArrayList<IdeaHelpers.GeneratedFileData>();
        final StringBuilder output_dir = new StringBuilder();
        class Error {
            public Throwable error = null;
        }
        Error error = new Error();
        final Runnable runnable = new Runnable() {
            @Override
            public void run() {
                try {
                    final Settings settings = IdeaHelpers.load_settings(root_file);
                    final int[] selectedRows = get_selection();
                    for (int row : selectedRows) {
                        table.setValueAt("", row, 1);
                    }
                    // to prevent:
                    // WARN - tellij.ide.HackyRepaintManager
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            table.updateUI();
                        }
                    });
                    // tableViewer.refresh();
                    Connection con = IdeaHelpers.get_connection(project, settings);
                    try {
                        // !!!! after 'try'
                        IDaoCG gen = IdeaTargetLanguageHelpers.create_dao_cg(con, project, root_file, settings, output_dir);
                        String local_abs_path = root_file.getParent().getPath();
                        String contextPath = DaoClass.class.getPackage().getName();
                        XmlParser xml_parser = new XmlParser(contextPath, Helpers.concat_path(local_abs_path, Const.DAO_XSD));
                        for (int row : selectedRows) {
                            String dao_xml_rel_path = (String) table.getValueAt(row, 0);
                            try {
                                ProgressManager.progress(dao_xml_rel_path);
                                String dao_class_name = Helpers.get_dao_class_name(dao_xml_rel_path);
                                String dao_xml_abs_path = Helpers.concat_path(local_abs_path, dao_xml_rel_path);
                                DaoClass dao_class = xml_parser.unmarshal(dao_xml_abs_path);
                                String[] file_content = gen.translate(dao_class_name, dao_class);
                                IdeaTargetLanguageHelpers.prepare_generated_file_data(root_file, dao_class_name, file_content, list);
                                table.setValueAt(Const.STATUS_GENERATED, row, 1);
                                try {
                                    Thread.sleep(50);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                // to prevent:
                                // WARN - intellij.ide.HackyRepaintManager
                                SwingUtilities.invokeLater(new Runnable() {
                                    public void run() {
                                        table.updateUI();
                                    }
                                });
                            } catch (Throwable e) {
                                String msg = e.getMessage();
                                table.setValueAt(msg, row, 1);
                                IdeaMessageHelpers.add_dao_error_message(settings, project, root_file, dao_xml_rel_path, msg);
                                // break; // exit the loop
                            }
                        }
                    } finally {
                        con.close();
                    }
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            table.updateUI();
                        }
                    });
                } catch (Throwable e) {
                    error.error = e;
                }
            }
        };
        ProgressManager.getInstance().runProcessWithProgressSynchronously(runnable, "Code generation", false, project);
        // write only the generated files
        // writeActions can show their own dialogs
        try {
            IdeaHelpers.run_write_action_to_generate_source_file(output_dir.toString(), list, project);
        } catch (Exception e) {
            e.printStackTrace();
            IdeaMessageHelpers.show_error_in_ui_thread(e);
        }
        if (error.error != null) {
            error.error.printStackTrace();
            IdeaMessageHelpers.show_error_in_ui_thread(error.error);
        }
        table.updateUI();
    }

    private int[] get_selection() throws Exception {
        int rc = table.getModel().getRowCount();
        if (rc == 1) {
            return new int[]{0};
        }
        int[] selected_rows = table.getSelectedRows();
        if (selected_rows.length == 0) {
            selected_rows = new int[rc];
            for (int i = 0; i < rc; i++) {
                selected_rows[i] = i;
            }
        }
        if (selected_rows.length == 0) {
            throw new InternalException("Selection is empty");
        }
        return selected_rows;
    }

    private void validate() {
        try {
            reload_table();
            Settings profile = IdeaHelpers.load_settings(root_file);
            Connection con = IdeaHelpers.get_connection(project, profile);
            try {
                // !!!! after 'try'
                IDaoCG gen = IdeaTargetLanguageHelpers.create_dao_cg(con, project, root_file, profile, null);
                validate_sync(gen, my_table_model, profile);
            } finally {
                con.close();
            }
        } catch (Throwable e) {
            e.printStackTrace();
            IdeaMessageHelpers.show_error_in_ui_thread(e);
        }
    }

    private void validate_sync(final IDaoCG gen, final TableModel model, final Settings settings) {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                try {
                    String local_abs_path = root_file.getParent().getPath();
                    String contextPath = DaoClass.class.getPackage().getName();
                    XmlParser xml_parser = new XmlParser(contextPath, Helpers.concat_path(local_abs_path, Const.DAO_XSD));
                    for (int i = 0; i < model.getRowCount(); i++) {
                        String dao_xml_rel_path = (String) model.getValueAt(i, 0);
                        try {
                            ProgressManager.progress(dao_xml_rel_path);
                            String dao_class_name = Helpers.get_dao_class_name(dao_xml_rel_path);
                            String dao_xml_abs_path = Helpers.concat_path(local_abs_path, dao_xml_rel_path);
                            DaoClass dao_class = xml_parser.unmarshal(dao_xml_abs_path);
                            String[] file_content = gen.translate(dao_class_name, dao_class);
                            StringBuilder validation_buff = new StringBuilder();
                            IdeaTargetLanguageHelpers.validate_dao(project, root_file, settings, dao_class_name, file_content, validation_buff);
                            String status = validation_buff.toString();
                            if (status.length() == 0) {
                                model.setValueAt(Const.STATUS_OK, i, 1);
                            } else {
                                model.setValueAt(status, i, 1);
                                IdeaMessageHelpers.add_dao_error_message(settings, project, root_file, dao_xml_rel_path, status);
                            }
                        } catch (Throwable ex) {
                            // ex.printStackTrace();
                            String msg = ex.getMessage();
                            model.setValueAt(msg, i, 1);
                            IdeaMessageHelpers.add_dao_error_message(settings, project, root_file, dao_xml_rel_path, msg);
                        }
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                table.updateUI();
                            }
                        });
                    }
                } catch (Throwable e) {
                    e.printStackTrace();
                    IdeaMessageHelpers.show_info_in_ui_thread(e.getMessage());
                }
            }
        };
        ProgressManager.getInstance().runProcessWithProgressSynchronously(runnable,
                "Validation", false, project);
    }

    public void set_project(Project project) {
        this.project = project;
    }

    public void set_file(VirtualFile propFile) {
        this.root_file = propFile;
    }

    public JComponent get_root_panel() {
        return rootPanel;
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        createUIComponents();
        rootPanel = new JPanel();
        rootPanel.setLayout(new GridLayoutManager(3, 1, new Insets(0, 0, 0, 0), -1, -1));
        textField1 = new JTextField();
        rootPanel.add(textField1, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JScrollPane scrollPane1 = new JScrollPane();
        rootPanel.add(scrollPane1, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        table.setAutoResizeMode(0);
        table.setFillsViewportHeight(true);
        scrollPane1.setViewportView(table);
        top_panel_1 = new JPanel();
        top_panel_1.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 5));
        top_panel_1.setOpaque(false);
        rootPanel.add(top_panel_1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        tool_panel = new JPanel();
        tool_panel.setLayout(new FlowLayout(FlowLayout.CENTER, 0, 0));
        tool_panel.setOpaque(false);
        top_panel_1.add(tool_panel);
        btn_NewXML = new JButton();
        btn_NewXML.setBorderPainted(false);
        btn_NewXML.setContentAreaFilled(false);
        btn_NewXML.setIcon(new ImageIcon(getClass().getResource("/img/new_xml.gif")));
        btn_NewXML.setMargin(new Insets(0, 0, 0, 0));
        btn_NewXML.setMaximumSize(new Dimension(32, 32));
        btn_NewXML.setMinimumSize(new Dimension(32, 32));
        btn_NewXML.setOpaque(false);
        btn_NewXML.setPreferredSize(new Dimension(32, 32));
        btn_NewXML.setText("");
        btn_NewXML.setToolTipText("New XML file");
        tool_panel.add(btn_NewXML);
        btn_OpenXML = new JButton();
        btn_OpenXML.setBorderPainted(false);
        btn_OpenXML.setContentAreaFilled(false);
        btn_OpenXML.setIcon(new ImageIcon(getClass().getResource("/img/xmldoc.gif")));
        btn_OpenXML.setMargin(new Insets(0, 0, 0, 0));
        btn_OpenXML.setMaximumSize(new Dimension(32, 32));
        btn_OpenXML.setMinimumSize(new Dimension(32, 32));
        btn_OpenXML.setOpaque(false);
        btn_OpenXML.setPreferredSize(new Dimension(32, 32));
        btn_OpenXML.setText("");
        btn_OpenXML.setToolTipText("Open XML file");
        tool_panel.add(btn_OpenXML);
        btn_OpenJava = new JButton();
        btn_OpenJava.setBorderPainted(false);
        btn_OpenJava.setContentAreaFilled(false);
        btn_OpenJava.setIcon(new ImageIcon(getClass().getResource("/img/GeneratedFile.gif")));
        btn_OpenJava.setMargin(new Insets(0, 0, 0, 0));
        btn_OpenJava.setMaximumSize(new Dimension(32, 32));
        btn_OpenJava.setMinimumSize(new Dimension(32, 32));
        btn_OpenJava.setOpaque(false);
        btn_OpenJava.setPreferredSize(new Dimension(32, 32));
        btn_OpenJava.setText("");
        btn_OpenJava.setToolTipText("Go to generated source");
        tool_panel.add(btn_OpenJava);
        btn_CrudDao = new JButton();
        btn_CrudDao.setBorderPainted(false);
        btn_CrudDao.setContentAreaFilled(false);
        btn_CrudDao.setIcon(new ImageIcon(getClass().getResource("/img/180.png")));
        btn_CrudDao.setMargin(new Insets(0, 0, 0, 0));
        btn_CrudDao.setMaximumSize(new Dimension(32, 32));
        btn_CrudDao.setMinimumSize(new Dimension(32, 32));
        btn_CrudDao.setOpaque(false);
        btn_CrudDao.setPreferredSize(new Dimension(32, 32));
        btn_CrudDao.setText("");
        btn_CrudDao.setToolTipText("DAO CRUD XML Assistant");
        tool_panel.add(btn_CrudDao);
        button_fk_assistant = new JButton();
        button_fk_assistant.setBorderPainted(false);
        button_fk_assistant.setContentAreaFilled(false);
        button_fk_assistant.setIcon(new ImageIcon(getClass().getResource("/img/FK.gif")));
        button_fk_assistant.setMargin(new Insets(0, 0, 0, 0));
        button_fk_assistant.setMaximumSize(new Dimension(32, 32));
        button_fk_assistant.setMinimumSize(new Dimension(32, 32));
        button_fk_assistant.setOpaque(false);
        button_fk_assistant.setPreferredSize(new Dimension(32, 32));
        button_fk_assistant.setText("");
        button_fk_assistant.setToolTipText("FK Access Assistant");
        tool_panel.add(button_fk_assistant);
        btn_Refresh = new JButton();
        btn_Refresh.setBorderPainted(false);
        btn_Refresh.setContentAreaFilled(false);
        btn_Refresh.setIcon(new ImageIcon(getClass().getResource("/img/arrow-circle-double-135.png")));
        btn_Refresh.setMargin(new Insets(0, 0, 0, 0));
        btn_Refresh.setMaximumSize(new Dimension(32, 32));
        btn_Refresh.setMinimumSize(new Dimension(32, 32));
        btn_Refresh.setOpaque(false);
        btn_Refresh.setPreferredSize(new Dimension(32, 32));
        btn_Refresh.setText("");
        btn_Refresh.setToolTipText("Refresh");
        tool_panel.add(btn_Refresh);
        btn_DeselAll = new JButton();
        btn_DeselAll.setBorderPainted(false);
        btn_DeselAll.setIcon(new ImageIcon(getClass().getResource("/img/none.gif")));
        btn_DeselAll.setMargin(new Insets(0, 0, 0, 0));
        btn_DeselAll.setMaximumSize(new Dimension(32, 32));
        btn_DeselAll.setMinimumSize(new Dimension(32, 32));
        btn_DeselAll.setOpaque(false);
        btn_DeselAll.setPreferredSize(new Dimension(32, 32));
        btn_DeselAll.setText("");
        btn_DeselAll.setToolTipText("Deselect all");
        tool_panel.add(btn_DeselAll);
        btn_SelAll = new JButton();
        btn_SelAll.setBorderPainted(false);
        btn_SelAll.setIcon(new ImageIcon(getClass().getResource("/img/text.gif")));
        btn_SelAll.setMargin(new Insets(0, 0, 0, 0));
        btn_SelAll.setMaximumSize(new Dimension(32, 32));
        btn_SelAll.setMinimumSize(new Dimension(32, 32));
        btn_SelAll.setOpaque(false);
        btn_SelAll.setPreferredSize(new Dimension(32, 32));
        btn_SelAll.setText("");
        btn_SelAll.setToolTipText("Select all");
        tool_panel.add(btn_SelAll);
        btn_Generate = new JButton();
        btn_Generate.setBorderPainted(false);
        btn_Generate.setContentAreaFilled(false);
        btn_Generate.setIcon(new ImageIcon(getClass().getResource("/img/compile-warning.png")));
        btn_Generate.setMargin(new Insets(0, 0, 0, 0));
        btn_Generate.setMaximumSize(new Dimension(32, 32));
        btn_Generate.setMinimumSize(new Dimension(32, 32));
        btn_Generate.setOpaque(false);
        btn_Generate.setPreferredSize(new Dimension(32, 32));
        btn_Generate.setText("");
        btn_Generate.setToolTipText("Generate for selection");
        tool_panel.add(btn_Generate);
        btn_Validate = new JButton();
        btn_Validate.setBorderPainted(false);
        btn_Validate.setContentAreaFilled(false);
        btn_Validate.setIcon(new ImageIcon(getClass().getResource("/img/validate.gif")));
        btn_Validate.setMargin(new Insets(0, 0, 0, 0));
        btn_Validate.setMaximumSize(new Dimension(32, 32));
        btn_Validate.setMinimumSize(new Dimension(32, 32));
        btn_Validate.setOpaque(false);
        btn_Validate.setPreferredSize(new Dimension(32, 32));
        btn_Validate.setText("");
        btn_Validate.setToolTipText("Validate all");
        tool_panel.add(btn_Validate);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return rootPanel;
    }

    private static class ColorRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean
                hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            String sValue = (String) value;
            setText(sValue);
            if (isSelected || Const.STATUS_OK.equals(sValue) || Const.STATUS_GENERATED.equals(sValue)) {
                TableCellRenderer r = table.getCellRenderer(row, column);
                Component c_0 = r.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, 0);
                c.setForeground(c_0.getForeground());
            } else {
                c.setForeground(JBColor.RED);
            }
            return c;
        }
    }

    private void set_filter() {
        // If current expression doesn't parse, don't update.
        try {
            RowFilter<AbstractTableModel, Object> rf = RowFilter.regexFilter(textField1.getText(), 0);
            sorter.setRowFilter(rf);
        } catch (PatternSyntaxException e) {
            sorter.setRowFilter(null);
        }
    }

    private static class MyTableModel extends AbstractTableModel {

        private final ArrayList<String[]> list = new ArrayList<String[]>();

        public ArrayList<String[]> getList() {
            return list;
        }

        public void refresh() {
            // http://stackoverflow.com/questions/3179136/jtable-how-to-refresh-table-model-after-insert-delete-or-update-the-data
            super.fireTableDataChanged();
        }

        public String getColumnName(int col) {
            return col == 0 ? "File" : "State";
        }

        @Override
        public int getColumnCount() {
            return 2;
        }

        @Override
        public int getRowCount() {
            return list.size();
        }

        @Override
        public Object getValueAt(int r, int c) {
            return list.get(r)[c];
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            list.get(rowIndex)[columnIndex] = (String) aValue;
        }
    }

    private void reload_table() {
        try {
            final ArrayList<String[]> list = my_table_model.getList();
            list.clear();
            FileSearchHelpers.IFile_List fileList = new FileSearchHelpers.IFile_List() {
                @Override
                public void add(String fileName) {
                    String[] item = new String[2];
                    try {
                        item[0] = fileName;
                    } catch (Exception e) {
                        e.printStackTrace();
                        item[0] = e.getMessage();
                    }
                    list.add(item);
                }
            };
            String xml_configs_folder_full_path = root_file.getParent().getPath();
            FileSearchHelpers.enum_dao_xml_file_names(xml_configs_folder_full_path, fileList);
        } finally {
            my_table_model.refresh(); // // table.updateUI();
        }
    }

    public void reload_table(boolean show_error_msg) {
        try {
            reload_table();
        } catch (Throwable e) {
            if (show_error_msg) {
                e.printStackTrace();
                IdeaMessageHelpers.show_error_in_ui_thread(e);
            }
        }
    }
}