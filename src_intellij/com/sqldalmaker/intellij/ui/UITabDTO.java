/*
    Copyright 2011-2021 sqldalmaker@gmail.com
    SQL DAL Maker Website: http://sqldalmaker.sourceforge.net
    Read LICENSE.txt in the root of this project/archive for details.
 */
package com.sqldalmaker.intellij.ui;

import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.JBColor;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.sqldalmaker.cg.IDtoCG;
import com.sqldalmaker.cg.SqlUtils;
import com.sqldalmaker.common.Const;
import com.sqldalmaker.common.InternalException;
import com.sqldalmaker.common.SdmUtils;
import com.sqldalmaker.jaxb.dto.DtoClass;
import com.sqldalmaker.jaxb.settings.Settings;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.PatternSyntaxException;

/**
 * Created with IntelliJ IDEA.
 * User: sqldalmaker@gmail.com
 * Date: 21.06.2012
 * Time: 18:27
 */
public class UITabDTO {
    private JButton btn_OpenXML;
    private JTable table;
    protected JPanel rootPanel;
    private JTextField textField1;
    private JButton btn_Refresh;
    private JButton btn_SelAll;
    private JButton btn_DeselAll;
    private JButton btn_Validate;
    private JButton btn_Generate;
    private JButton btn_OpenSQL;
    private JButton btn_OpenJava;
    private JButton btn_genTmpFieldTags;
    private JButton btn_CrudXML;
    private JPanel top_panel_1;
    private JPanel tool_panel;
    private TableRowSorter<AbstractTableModel> sorter;
    private Project project;
    private VirtualFile root_file;
    private MyTableModel my_table_model;

    public JComponent get_root_panel() {
        return rootPanel;
    }

    public JComponent get_tool_bar() {
        return tool_panel;
    }

    public UITabDTO() {
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
                generate();
            }
        });
        btn_OpenSQL.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                open_sql();
            }
        });
        btn_OpenXML.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                open_dto_xml();
            }
        });
        btn_OpenJava.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                open_generated_source_file();
            }
        });
        btn_genTmpFieldTags.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                gen_tmp_field_tags();
            }
        });
        btn_CrudXML.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                create_crud_xml();
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
    }

    private void open_dto_xml() {
        IdeaEditorHelpers.open_dto_xml_sync(project, root_file);
    }

    private void navigate_to_dto_class_declaration() {
        try {
            int[] selected_rows = get_selection();
            String dto_class_name = (String) table.getValueAt(selected_rows[0], 0);
            IdeaHelpers.navigate_to_dto_class_declaration(project, root_file, dto_class_name);
        } catch (Exception e) {
            IdeaMessageHelpers.show_error_in_ui_thread(e);
            e.printStackTrace();
        }
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
        top_panel_1.setAutoscrolls(false);
        top_panel_1.setOpaque(true);
        rootPanel.add(top_panel_1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        tool_panel = new JPanel();
        tool_panel.setLayout(new FlowLayout(FlowLayout.CENTER, 0, 0));
        tool_panel.setOpaque(false);
        top_panel_1.add(tool_panel);
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
        btn_OpenSQL = new JButton();
        btn_OpenSQL.setBorderPainted(false);
        btn_OpenSQL.setContentAreaFilled(false);
        btn_OpenSQL.setIcon(new ImageIcon(getClass().getResource("/img/qrydoc.gif")));
        btn_OpenSQL.setMargin(new Insets(0, 0, 0, 0));
        btn_OpenSQL.setMaximumSize(new Dimension(32, 32));
        btn_OpenSQL.setMinimumSize(new Dimension(32, 32));
        btn_OpenSQL.setOpaque(false);
        btn_OpenSQL.setPreferredSize(new Dimension(32, 32));
        btn_OpenSQL.setText("");
        btn_OpenSQL.setToolTipText("Open SQL file");
        tool_panel.add(btn_OpenSQL);
        btn_OpenJava = new JButton();
        btn_OpenJava.setBorderPainted(false);
        btn_OpenJava.setIcon(new ImageIcon(getClass().getResource("/img/GeneratedFile.gif")));
        btn_OpenJava.setMargin(new Insets(0, 0, 0, 0));
        btn_OpenJava.setMaximumSize(new Dimension(32, 32));
        btn_OpenJava.setMinimumSize(new Dimension(32, 32));
        btn_OpenJava.setOpaque(false);
        btn_OpenJava.setPreferredSize(new Dimension(32, 32));
        btn_OpenJava.setText("");
        btn_OpenJava.setToolTipText("Go to generated source");
        tool_panel.add(btn_OpenJava);
        btn_genTmpFieldTags = new JButton();
        btn_genTmpFieldTags.setBorderPainted(false);
        btn_genTmpFieldTags.setIcon(new ImageIcon(getClass().getResource("/img/177.png")));
        btn_genTmpFieldTags.setMargin(new Insets(0, 0, 0, 0));
        btn_genTmpFieldTags.setMaximumSize(new Dimension(32, 32));
        btn_genTmpFieldTags.setMinimumSize(new Dimension(32, 32));
        btn_genTmpFieldTags.setOpaque(false);
        btn_genTmpFieldTags.setPreferredSize(new Dimension(32, 32));
        btn_genTmpFieldTags.setText("");
        btn_genTmpFieldTags.setToolTipText("Fields Definition Assistant");
        tool_panel.add(btn_genTmpFieldTags);
        btn_CrudXML = new JButton();
        btn_CrudXML.setBorderPainted(false);
        btn_CrudXML.setIcon(new ImageIcon(getClass().getResource("/img/180.png")));
        btn_CrudXML.setMargin(new Insets(0, 0, 0, 0));
        btn_CrudXML.setMaximumSize(new Dimension(32, 32));
        btn_CrudXML.setMinimumSize(new Dimension(32, 32));
        btn_CrudXML.setOpaque(false);
        btn_CrudXML.setPreferredSize(new Dimension(32, 32));
        btn_CrudXML.setText("");
        btn_CrudXML.setToolTipText("DTO CRUD XML Assistant");
        tool_panel.add(btn_CrudXML);
        btn_Refresh = new JButton();
        btn_Refresh.setBorderPainted(false);
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
        btn_Generate.setIcon(new ImageIcon(getClass().getResource("/img/compile.png")));
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

    private static class MyTableModel extends AbstractTableModel {

        private final ArrayList<String[]> list = new ArrayList<String[]>();

        public ArrayList<String[]> getList() {
            return list;
        }

        public void refresh() {
            // http://stackoverflow.com/questions/3179136/jtable-how-to-refresh-table-model-after-insert-delete-or-update-the-data
            super.fireTableDataChanged();
        }

        @Override
        public String getColumnName(int col) {
            switch (col) {
                case 0:
                    return "Class";
                case 1:
                    return "Ref.";
                case 2:
                    return "State";
            }
            return "";
        }

        @Override
        public int getColumnCount() {
            return 3;
        }

        @Override
        public int getRowCount() {
            return list.size();
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            return list.get(rowIndex)[columnIndex];
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            list.get(rowIndex)[columnIndex] = (String) aValue;
        }
    }

    private static class ColorTableCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
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

    private void createUIComponents() {
        final ColorTableCellRenderer colorRenderer = new ColorTableCellRenderer();
        table = new JTable() {
            public TableCellRenderer getCellRenderer(int row, int column) {
                if (column == 2) {
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
        // table.setRowHeight(24);
        table.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int col = table.columnAtPoint(new Point(e.getX(), e.getY()));
                    int row = table.rowAtPoint(new Point(e.getX(), e.getY()));
                    if (row >= 0) {
                        if (col == 0) {
                            navigate_to_dto_class_declaration();
                        } else if (col == 1) {
                            open_sql();
                        } else {
                            open_generated_source_file();
                        }
                    } else {
                        open_dto_xml();
                    }
                }
            }
        });

        {
            TableColumn col = table.getColumnModel().getColumn(0);
            col.setPreferredWidth(220);
        }
        {
            TableColumn col = table.getColumnModel().getColumn(1);
            col.setPreferredWidth(300);
        }
        {
            TableColumn col = table.getColumnModel().getColumn(2);
            col.setPreferredWidth(300);
        }
    }

    private void create_crud_xml() {
        IdeaCrudXmlHelpers.get_crud_dto_xml(project, root_file);
    }

    private void open_sql() {
        try {
            int[] selected_rows = get_selection();
            String ref = (String) table.getValueAt(selected_rows[0], 1);
            if (SqlUtils.is_sql_file_ref(ref) == false) {
                return;
            }
            Settings settings = IdeaHelpers.load_settings(root_file);
            String relPath = settings.getFolders().getSql() + "/" + ref;
            IdeaEditorHelpers.open_project_file_in_editor_sync(project, relPath);
        } catch (Exception e) {
            IdeaMessageHelpers.show_error_in_ui_thread(e);
            e.printStackTrace();
        }
    }

    protected void open_generated_source_file() {
        try {
            int[] selected_rows = get_selection();
            Settings settings = IdeaHelpers.load_settings(root_file);
            String value = (String) table.getValueAt(selected_rows[0], 0);
            IdeaTargetLanguageHelpers.open_editor(project, root_file, value, settings, settings.getDto().getScope());
        } catch (Exception e) {
            e.printStackTrace();
            IdeaMessageHelpers.show_error_in_ui_thread(e);
        }
    }

    protected void gen_tmp_field_tags() {
        try {
            int[] selected_rows = get_selection();
            String class_name = (String) table.getValueAt(selected_rows[0], 0);
            String ref = (String) table.getValueAt(selected_rows[0], 1);
            IdeaEditorHelpers.gen_tmp_field_tags(class_name, ref, project, root_file);
        } catch (Exception e) {
            e.printStackTrace();
            IdeaMessageHelpers.show_error_in_ui_thread(e);
        }
    }

    public void set_project(Project project) {
        this.project = project;
    }

    public void set_file(VirtualFile file) {
        this.root_file = file;
    }

    private void validate() {
        try {
            reload_table();
            Settings settings = IdeaHelpers.load_settings(root_file);
            Connection con = IdeaHelpers.get_connection(project, settings);
            try {
                // !!!! after 'try'
                IDtoCG gen = IdeaTargetLanguageHelpers.create_dto_cg(con, project, root_file, settings, null);
                validate_with_progress_sync(gen, my_table_model, settings);
            } finally {
                con.close();
            }
        } catch (Throwable e) {
            e.printStackTrace();
            IdeaMessageHelpers.show_error_in_ui_thread(e);
        }
    }

    private void validate_with_progress_sync(final IDtoCG gen, final AbstractTableModel model, final Settings settings) {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                // === panedrone: don't ask it in loop because of
                // "Cannot read the array length because "<local3>" is null"
                int rc = model.getRowCount();
                for (int i = 0; i < rc; i++) {
                    String dto_class_name = (String) model.getValueAt(i, 0);
                    ProgressManager.progress(dto_class_name);
                    try {
                        String[] fileContent = gen.translate(dto_class_name);
                        StringBuilder validationBuff = new StringBuilder();
                        IdeaTargetLanguageHelpers.validate_dto(project, root_file, settings, dto_class_name, fileContent, validationBuff);
                        String status = validationBuff.toString();
                        if (status.length() == 0) {
                            model.setValueAt(Const.STATUS_OK, i, 2);
                        } else {
                            model.setValueAt(status, i, 2);
                            IdeaMessageHelpers.add_dto_error_message(settings, project, root_file, dto_class_name, status);
                        }
                    } catch (Throwable e) {
                        // e.printStackTrace();
                        String msg = e.getMessage();
                        model.setValueAt(msg, i, 2);
                        IdeaMessageHelpers.add_dto_error_message(settings, project, root_file, dto_class_name, msg);
                    }
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            table.updateUI();
                        }
                    });
                }
            }
        };
        ProgressManager.getInstance().runProcessWithProgressSynchronously(runnable, "Validation", false, project);
    }

    protected void generate() {
        final StringBuilder output_dir = new StringBuilder();
        // 1. open connection
        // 2. create the list of generated text
        // 3. close connection
        // 4. update the files from the list
        final List<IdeaHelpers.GeneratedFileData> list = new ArrayList<IdeaHelpers.GeneratedFileData>();
        class Error {
            public Throwable error = null;
        }
        Error error = new Error();
        final Runnable runnable = new Runnable() {
            @Override
            public void run() {
                try {
                    final Settings settings = IdeaHelpers.load_settings(root_file);
                    final int[] selected_rows = get_selection();
                    for (int row : selected_rows) {
                        table.setValueAt("", row, 2);
                    }
                    // to prevent: WARN - tellij.ide.HackyRepaintManager
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            table.updateUI();
                        }
                    });
                    // tableViewer.refresh();
                    Connection con = IdeaHelpers.get_connection(project, settings);
                    try {
                        // !!!! after 'try'
                        IDtoCG gen = IdeaTargetLanguageHelpers.create_dto_cg(con, project, root_file, settings, output_dir);
                        for (int row : selected_rows) {
                            String dto_class_name = (String) table.getValueAt(row, 0);
                            try {
                                ProgressManager.progress(dto_class_name);
                                String[] fileContent = gen.translate(dto_class_name);
                                IdeaTargetLanguageHelpers.prepare_generated_file_data(root_file, dto_class_name, fileContent, list);
                                table.setValueAt(Const.STATUS_GENERATED, row, 2);
                                try {
                                    Thread.sleep(50);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                // to prevent:
                                // WARN - Intellij.ide.HackyRepaintManager
                                SwingUtilities.invokeLater(new Runnable() {
                                    public void run() {
                                        table.updateUI();
                                    }
                                });
                            } catch (Throwable e) {
                                String msg = e.getMessage();
                                table.setValueAt(msg, row, 2);
                                IdeaMessageHelpers.add_dto_error_message(settings, project, root_file, dto_class_name, msg);
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

    private int[] get_selection() throws InternalException {
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

    private void set_filter() {
        try {
            // don't update if current expression cannot br parsed.
            RowFilter<AbstractTableModel, Object> rf = RowFilter.regexFilter(textField1.getText(), 0);
            sorter.setRowFilter(rf);
        } catch (PatternSyntaxException e) {
            sorter.setRowFilter(null);
        }
    }

    private void reload_table() throws Exception {
        try {
            final ArrayList<String[]> list = my_table_model.getList();
            list.clear();
            String xml_configs_folder_full_path = root_file.getParent().getPath();
            String dto_xml_abs_path = xml_configs_folder_full_path + "/" + Const.DTO_XML;
            String dto_xsd_abs_path = xml_configs_folder_full_path + "/" + Const.DTO_XSD;
            List<DtoClass> res = SdmUtils.get_dto_classes(dto_xml_abs_path, dto_xsd_abs_path);
            for (DtoClass cls : res) {
                String[] item = new String[3];
                item[0] = cls.getName();
                item[1] = cls.getRef();
                list.add(item);
            }
        } finally {
            my_table_model.refresh(); // table.updateUI();
        }
    }

    public void reload_table(boolean showErrorMsg) {
        try {
            reload_table();
        } catch (Throwable e) {
            if (showErrorMsg) {
                e.printStackTrace();
                IdeaMessageHelpers.show_error_in_ui_thread(e);
            }
        }
    }
}