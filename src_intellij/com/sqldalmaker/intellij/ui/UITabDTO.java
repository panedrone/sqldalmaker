/*
 * Copyright 2011-2018 sqldalmaker@gmail.com
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
import com.sqldalmaker.cg.IDtoCG;
import com.sqldalmaker.common.Const;
import com.sqldalmaker.common.InternalException;
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
 * Date: 21.06.12
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
    private JPanel toolbar_panel;
    private TableRowSorter<AbstractTableModel> sorter;
    private Project project;
    private VirtualFile propFile;

    private MyTableModel myTableModel;

    private static final String STATUS_GENERATED = "Generated successfully";
    private static final String STATUS_OK = "OK";

    public JComponent getRootPanel() {
        return rootPanel;
    }

    public JComponent getToolBar() {
        return toolbar_panel;
    }

    public UITabDTO() {
        $$$setupUI$$$();

        rootPanel.remove(top_panel_1);

        textField1.getDocument().addDocumentListener(new DocumentListener() {
            private void updateFilter() {
                setFilter();
            }

            public void changedUpdate(DocumentEvent e) {
                updateFilter();
            }

            public void removeUpdate(DocumentEvent e) {
                updateFilter();
            }

            public void insertUpdate(DocumentEvent e) {
                updateFilter();
            }
        });

        btn_Refresh.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                reloadTable(true);
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
                openSQL();
            }
        });
        btn_OpenXML.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                openDtoXML();
            }
        });
        btn_OpenJava.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                openGeneratedSourceFile();
            }
        });
        btn_genTmpFieldTags.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                genTmpFieldTags();
            }
        });
        btn_CrudXML.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                createCrudXml();
            }
        });

        Cursor wc = new Cursor(Cursor.HAND_CURSOR);

        for (Component c : toolbar_panel.getComponents()) {
            JButton b = (JButton) c;
            b.setCursor(wc);
        }
    }

    private void openDtoXML() {

        IdeaEditorHelpers.open_dto_xml(project, propFile);
    }

    private void navigateToDtoClassDeclaration() {

        try {

            int[] selectedRows = getSelection();

            String dto_class_name = (String) table.getValueAt(selectedRows[0], 0);

            IdeaHelpers.navigate_to_dto_class_declaration(project, propFile, dto_class_name);

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
        rootPanel.add(top_panel_1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        toolbar_panel = new JPanel();
        toolbar_panel.setLayout(new FlowLayout(FlowLayout.CENTER, 0, 0));
        top_panel_1.add(toolbar_panel);
        btn_OpenXML = new JButton();
        btn_OpenXML.setBorderPainted(false);
        btn_OpenXML.setIcon(new ImageIcon(getClass().getResource("/img/xmldoc.gif")));
        btn_OpenXML.setMargin(new Insets(0, 0, 0, 0));
        btn_OpenXML.setMaximumSize(new Dimension(32, 32));
        btn_OpenXML.setMinimumSize(new Dimension(32, 32));
        btn_OpenXML.setOpaque(false);
        btn_OpenXML.setPreferredSize(new Dimension(32, 32));
        btn_OpenXML.setText("");
        btn_OpenXML.setToolTipText("Open XML file");
        toolbar_panel.add(btn_OpenXML);
        btn_OpenSQL = new JButton();
        btn_OpenSQL.setBorderPainted(false);
        btn_OpenSQL.setIcon(new ImageIcon(getClass().getResource("/img/qrydoc.gif")));
        btn_OpenSQL.setMargin(new Insets(0, 0, 0, 0));
        btn_OpenSQL.setMaximumSize(new Dimension(32, 32));
        btn_OpenSQL.setMinimumSize(new Dimension(32, 32));
        btn_OpenSQL.setOpaque(false);
        btn_OpenSQL.setPreferredSize(new Dimension(32, 32));
        btn_OpenSQL.setText("");
        btn_OpenSQL.setToolTipText("Open SQL file");
        toolbar_panel.add(btn_OpenSQL);
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
        toolbar_panel.add(btn_OpenJava);
        btn_genTmpFieldTags = new JButton();
        btn_genTmpFieldTags.setBorderPainted(false);
        btn_genTmpFieldTags.setIcon(new ImageIcon(getClass().getResource("/img/177.png")));
        btn_genTmpFieldTags.setMargin(new Insets(0, 0, 0, 0));
        btn_genTmpFieldTags.setMaximumSize(new Dimension(32, 32));
        btn_genTmpFieldTags.setMinimumSize(new Dimension(32, 32));
        btn_genTmpFieldTags.setOpaque(false);
        btn_genTmpFieldTags.setPreferredSize(new Dimension(32, 32));
        btn_genTmpFieldTags.setText("");
        btn_genTmpFieldTags.setToolTipText("Fields definition assistant");
        toolbar_panel.add(btn_genTmpFieldTags);
        btn_CrudXML = new JButton();
        btn_CrudXML.setBorderPainted(false);
        btn_CrudXML.setIcon(new ImageIcon(getClass().getResource("/img/180.png")));
        btn_CrudXML.setMargin(new Insets(0, 0, 0, 0));
        btn_CrudXML.setMaximumSize(new Dimension(32, 32));
        btn_CrudXML.setMinimumSize(new Dimension(32, 32));
        btn_CrudXML.setOpaque(false);
        btn_CrudXML.setPreferredSize(new Dimension(32, 32));
        btn_CrudXML.setText("");
        btn_CrudXML.setToolTipText("DTO CRUD assistant");
        toolbar_panel.add(btn_CrudXML);
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
        toolbar_panel.add(btn_Refresh);
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
        toolbar_panel.add(btn_DeselAll);
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
        toolbar_panel.add(btn_SelAll);
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
        toolbar_panel.add(btn_Generate);
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
        toolbar_panel.add(btn_Validate);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return rootPanel;
    }

    private static class MyTableModel extends AbstractTableModel {

        private ArrayList<String[]> list = new ArrayList<String[]>();

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

    private class ColorTableCellRenderer extends DefaultTableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(JTable table,
                                                       Object value, boolean isSelected, boolean hasFocus, int row,
                                                       int column) {

            Component c = super.getTableCellRendererComponent(table, value,
                    isSelected, hasFocus, row, column);

            String sValue = (String) value;

            setText(sValue);

            if (isSelected || STATUS_OK.equals(sValue) || STATUS_GENERATED.equals(sValue)) {

                TableCellRenderer r = table.getCellRenderer(row, column);

                Component c_0 = r.getTableCellRendererComponent(table, value,
                        isSelected, hasFocus, row, 0);

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
        myTableModel = new MyTableModel();
        table.setModel(myTableModel);
        table.getTableHeader().setReorderingAllowed(false);

        sorter = new TableRowSorter<AbstractTableModel>(myTableModel);
        table.setRowSorter(sorter);
        // table.setRowHeight(24);
        table.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int col = table.columnAtPoint(new Point(e.getX(), e.getY()));
                    int row = table.rowAtPoint(new Point(e.getX(), e.getY()));
                    if (row >= 0) {
                        if (col == 0) {
                            navigateToDtoClassDeclaration();
                        } else if (col == 1) {
                            openSQL();
                        } else {
                            openGeneratedSourceFile();
                        }
                    } else {
                        openDtoXML();
                    }
                }
            }
        });

        {
            TableColumn col = table.getColumnModel().getColumn(0);
            col.setPreferredWidth(260);
        }
        {
            TableColumn col = table.getColumnModel().getColumn(1);
            col.setPreferredWidth(340);
        }
        {
            TableColumn col = table.getColumnModel().getColumn(2);
            col.setPreferredWidth(300);
        }
    }

    private void createCrudXml() {
        IdeaCrudXmlHelpers.get_crud_dto_xml(project, propFile);
    }

    private void openSQL() {

        try {

            int[] selectedRows = getSelection();

            String ref = (String) table.getValueAt(selectedRows[0], 1);

            if (Helpers.is_table_ref(ref)) {
                return;
            }

            Settings settings = IdeaHelpers.load_settings(propFile);

            String relPath = settings.getFolders().getSql() + "/" + ref;

            IdeaEditorHelpers.open_module_file_in_editor(project, relPath);

        } catch (Exception e) {
            IdeaMessageHelpers.show_error_in_ui_thread(e);
            e.printStackTrace();
        }
    }

    protected void openGeneratedSourceFile() {
        try {

            int[] selectedRows = getSelection();

            Settings settings = IdeaHelpers.load_settings(propFile);

            String value = (String) table.getValueAt(selectedRows[0], 0);

            IdeaTargetLanguageHelpers.open_editor(project, propFile, value, settings, settings.getDto().getScope());

        } catch (Exception e) {
            e.printStackTrace();
            IdeaMessageHelpers.show_error_in_ui_thread(e);
        }
    }

    protected void genTmpFieldTags() {
        try {

            int[] selectedRows = getSelection();

            String class_name = (String) table.getValueAt(selectedRows[0], 0);
            String ref = (String) table.getValueAt(selectedRows[0], 1);

            IdeaEditorHelpers.gen_tmp_field_tags(class_name, ref, project, propFile);

        } catch (Exception e) {
            e.printStackTrace();
            IdeaMessageHelpers.show_error_in_ui_thread(e);
        }
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public void setFile(VirtualFile file) {
        this.propFile = file;
    }

    private void validate() {

        try {

            reloadTable();

            Settings settings = IdeaHelpers.load_settings(propFile);

            Connection con = IdeaHelpers.get_connection(project, settings);

            try {

                // !!!! after 'try'
                IDtoCG gen = IdeaTargetLanguageHelpers.create_dto_cg(con, project, propFile, settings, null);

                validateWithProgressSynchronously(gen, myTableModel, settings);

            } finally {
                con.close();
            }

        } catch (Throwable e) {
            e.printStackTrace();
            IdeaMessageHelpers.show_error_in_ui_thread(e);
        }
    }

    private void validateWithProgressSynchronously(final IDtoCG gen, final AbstractTableModel model, final Settings settings) {

        Runnable runnable = new Runnable() {
            @Override
            public void run() {

                for (int i = 0; i < model.getRowCount(); i++) {

                    String dto_class_name = (String) model.getValueAt(i, 0);

                    ProgressManager.progress(dto_class_name);

                    try {

                        String[] fileContent = gen.translate(dto_class_name);

                        StringBuilder validationBuff = new StringBuilder();

                        IdeaTargetLanguageHelpers.validate_dto(project, propFile, settings, dto_class_name, fileContent, validationBuff);

                        String status = validationBuff.toString();

                        if (status.length() == 0) {
                            model.setValueAt(STATUS_OK, i, 2);
                        } else {
                            model.setValueAt(status, i, 2);
                            IdeaMessageHelpers.add_dto_error_message(settings, project, propFile, dto_class_name, status);
                        }

                    } catch (Throwable e) {

                        // e.printStackTrace();

                        String msg = e.getMessage();
                        model.setValueAt(msg, i, 2);
                        IdeaMessageHelpers.add_dto_error_message(settings, project, propFile, dto_class_name, msg);
                    }

                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            table.updateUI();
                        }
                    });
                }
            }
        };

        ProgressManager.getInstance().runProcessWithProgressSynchronously(runnable,
                "Validation", false, project);
    }

    private Throwable _error;

    protected void generate() {

        final StringBuilder output_dir = new StringBuilder();

        // 1. open connection
        // 2. create the list of generated java
        // 3. close connection
        // 4. update the files from the list

        final List<IdeaHelpers.GeneratedFileData> list = new ArrayList<IdeaHelpers.GeneratedFileData>();

        final Runnable runnable = new Runnable() {
            @Override
            public void run() {

                _error = null;

                try {

                    final Settings settings = IdeaHelpers.load_settings(propFile);

                    final int[] selectedRows = getSelection();

                    for (int row : selectedRows) {
                        table.setValueAt("", row, 2);
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
                        IDtoCG gen = IdeaTargetLanguageHelpers.create_dto_cg(con, project, propFile, settings, output_dir);

                        /////////////////////////////////

                        for (int row : selectedRows) {

                            String dto_class_name = (String) table.getValueAt(row, 0);

                            try {

                                ProgressManager.progress(dto_class_name);

                                String[] fileContent = gen.translate(dto_class_name);

                                IdeaTargetLanguageHelpers.prepare_generated_file_data(propFile, dto_class_name, fileContent, list);

                                table.setValueAt(STATUS_GENERATED, row, 2);

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

                                IdeaMessageHelpers.add_dto_error_message(settings, project, propFile, dto_class_name, msg);

                                // _error = e;

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

                    _error = e;
                }
            }
        };

        ProgressManager.getInstance().runProcessWithProgressSynchronously(runnable,
                "Code generation", false, project);

        // write only the generated files
        // writeActions can show their own dialogs

        try {

            IdeaHelpers.run_write_action_to_generate_source_file(output_dir.toString(), list, project, propFile);

        } catch (Exception e) {
            e.printStackTrace();
            IdeaMessageHelpers.show_error_in_ui_thread(e);
        }

        if (_error != null) {

            _error.printStackTrace();
            IdeaMessageHelpers.show_error_in_ui_thread(_error);
        }

        table.updateUI();
    }

    private int[] getSelection() throws Exception {

        int rc = table.getModel().getRowCount();

        if (rc == 1) {
            return new int[]{0};
        }

        int[] selectedRows = table.getSelectedRows();

        if (selectedRows.length == 0) {

            throw new InternalException("Selection is empty.");
        }

        return selectedRows;
    }

    private void setFilter() {

        try {

            // don't update if current expression cannot br parsed.

            RowFilter<AbstractTableModel, Object> rf = RowFilter.regexFilter(textField1.getText(), 0);
            sorter.setRowFilter(rf);

        } catch (PatternSyntaxException e) {
            sorter.setRowFilter(null);
        }
    }

    private void reloadTable() throws Exception {

        try {

            final ArrayList<String[]> list = myTableModel.getList();
            list.clear();

            String xml_configs_folder_full_path = propFile.getParent().getPath();

            String dtoXmlAbsPath = xml_configs_folder_full_path + "/" + Const.DTO_XML;
            String dtoXsdAbsPath = xml_configs_folder_full_path + "/" + Const.DTO_XSD;

            List<DtoClass> res = IdeaHelpers.get_dto_classes(
                    dtoXmlAbsPath, dtoXsdAbsPath);

            for (DtoClass cls : res) {

                String[] item = new String[3];
                item[0] = cls.getName();
                item[1] = cls.getRef();
                list.add(item);
            }

        } finally {

            myTableModel.refresh(); // table.updateUI();
        }
    }

    public void reloadTable(boolean showErrorMsg) {

        try {

            reloadTable();

        } catch (Throwable e) {

            if (showErrorMsg) {
                e.printStackTrace();
                IdeaMessageHelpers.show_error_in_ui_thread(e);
            }
        }
    }
}