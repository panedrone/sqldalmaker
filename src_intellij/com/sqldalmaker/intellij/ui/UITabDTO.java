/*
    Copyright 2011-2024 sqldalmaker@gmail.com
    SQL DAL Maker Website: https://sqldalmaker.sourceforge.net/
    Read LICENSE.txt in the root of this project/archive for details.
 */
package com.sqldalmaker.intellij.ui;

import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.JBColor;
import com.sqldalmaker.cg.Helpers;
import com.sqldalmaker.cg.IDtoCG;
import com.sqldalmaker.cg.SqlUtils;
import com.sqldalmaker.common.Const;
import com.sqldalmaker.common.SdmUtils;
import com.sqldalmaker.jaxb.sdm.DtoClass;
import com.sqldalmaker.jaxb.settings.Settings;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

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
    private JButton btn_Refresh;
    private JButton btn_Validate;
    private JButton btn_Generate;
    private JButton btn_OpenSQL;
    private JButton btn_OpenJava;
    private JButton btn_genTmpFieldTags;
    private JButton btn_CrudXML;
    private JPanel tool_panel;
    private JButton btn_goto_dto_class_in_xml;

    private Project project;
    private VirtualFile root_file;
    private MyDtoTableModel dto_table_model;

    private final int COL_INDEX_NAME = 0;
    private final int COL_INDEX_REF = 1;
    private final int COL_INDEX_STATUS = 2;

    public JComponent get_root_panel() {
        return rootPanel;
    }

    public UITabDTO() {
        $$$setupUI$$$();
        {
            btn_Refresh.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    reload_table(true);
                }
            });
            Icon icon = IconLoader.getIcon("/img/restartKernel.svg", UITabDTO.class);
            btn_Refresh.setIcon(icon);
        }
        {
            btn_Validate.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    validate();
                }
            });
            Icon icon = IconLoader.getIcon("/img/validator.svg", UITabDTO.class);
            btn_Validate.setIcon(icon);
        }
        {
            btn_Generate.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    generate_selected_with_progress_sync();
                }
            });
            Icon icon = IconLoader.getIcon("/img/buildOnFrameDeactivation.svg", UITabDTO.class);
            btn_Generate.setIcon(icon);
        }
        {
            btn_OpenSQL.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    int[] selected_rows = get_ui_table_sel_indexes();
                    if (selected_rows.length == 0) {
                        return;
                    }
                    open_sql_async(selected_rows[0]);
                }
            });
            Icon icon = IconLoader.getIcon("/img/dbmsOutput.svg", UITabDTO.class);
            btn_OpenSQL.setIcon(icon);
        }
        {
            btn_OpenXML.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    open_sdm_xml_async();
                }
            });
            Icon icon = IconLoader.getIcon("/img/dataStructure.svg", UITabDTO.class);
            btn_OpenXML.setIcon(icon);
        }
        {
            btn_goto_dto_class_in_xml.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    int[] selected_rows = get_ui_table_sel_indexes();
                    if (selected_rows.length > 0) {
                        navigate_to_sdm_dto_class_async(selected_rows[0]);
                        return;
                    }
                    open_sdm_xml_async();
                }
            });
            Icon icon = IconLoader.getIcon("/img/SQLDMLStatement.svg", UITabDTO.class);
            btn_goto_dto_class_in_xml.setIcon(icon);
        }
        {
            btn_OpenJava.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    int[] selected_rows = get_ui_table_sel_indexes();
                    if (selected_rows.length == 0) {
                        return;
                    }
                    open_target_file_async(selected_rows[0]);
                }
            });
            Icon icon = IconLoader.getIcon("/img/toolWindowJsonPath.svg", UITabDTO.class);
            btn_OpenJava.setIcon(icon);
        }
        {
            btn_genTmpFieldTags.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    jaxb_fields_wizard();
                }
            });
            Icon icon = IconLoader.getIcon("/img/schema.svg", UITabDTO.class);
            btn_genTmpFieldTags.setIcon(icon);
        }
        {
            btn_CrudXML.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    create_crud_xml();
                }
            });
            Icon icon = IconLoader.getIcon("/img/importDataCell.svg", UITabDTO.class);
            btn_CrudXML.setIcon(icon);
        }
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

        // === panedrone:
        // Set up columns after $$$setupUI$$$().
        // Do not do it in createUIComponents() which is called in beginning of $$$setupUI$$$();
        table.getColumnModel().getColumn(COL_INDEX_NAME).setPreferredWidth(220);
        table.getColumnModel().getColumn(COL_INDEX_NAME).setMaxWidth(2000); // === panedrone: AUTO_RESIZE_LAST_COLUMN not working without it
        table.getColumnModel().getColumn(COL_INDEX_REF).setPreferredWidth(300);
        table.getColumnModel().getColumn(COL_INDEX_REF).setMaxWidth(2000); // === panedrone: AUTO_RESIZE_LAST_COLUMN not working without it
        // table.getColumnModel().getColumn(2).setPreferredWidth(300);
        // https://stackoverflow.com/questions/953972/java-jtable-setting-column-width
        // JTable.AUTO_RESIZE_LAST_COLUMN is defined as "During all resize operations,
        // apply adjustments to the last column only" which means you have to set the auto resize mode
        // at the end of your code, otherwise setPreferredWidth() won't affect anything!
        table.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        table.doLayout();
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
        rootPanel.setLayout(new BorderLayout(0, 0));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 5));
        panel1.setAutoscrolls(false);
        panel1.setOpaque(true);
        rootPanel.add(panel1, BorderLayout.NORTH);
        tool_panel = new JPanel();
        tool_panel.setLayout(new FlowLayout(FlowLayout.CENTER, 0, 0));
        tool_panel.setOpaque(false);
        panel1.add(tool_panel);
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
        btn_OpenXML.setToolTipText("Open 'sdm.xml'");
        tool_panel.add(btn_OpenXML);
        btn_goto_dto_class_in_xml = new JButton();
        btn_goto_dto_class_in_xml.setBorderPainted(false);
        btn_goto_dto_class_in_xml.setContentAreaFilled(false);
        btn_goto_dto_class_in_xml.setIcon(new ImageIcon(getClass().getResource("/img/xmldoc_12x12.gif")));
        btn_goto_dto_class_in_xml.setMargin(new Insets(0, 0, 0, 0));
        btn_goto_dto_class_in_xml.setMaximumSize(new Dimension(32, 32));
        btn_goto_dto_class_in_xml.setMinimumSize(new Dimension(32, 32));
        btn_goto_dto_class_in_xml.setOpaque(false);
        btn_goto_dto_class_in_xml.setPreferredSize(new Dimension(32, 32));
        btn_goto_dto_class_in_xml.setText("");
        btn_goto_dto_class_in_xml.setToolTipText("Navigate to XML definition (double-click one of cells in the leftmost column)");
        tool_panel.add(btn_goto_dto_class_in_xml);
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
        btn_OpenSQL.setToolTipText("Navigate to SQL file (double-click one the middle cells)");
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
        btn_OpenJava.setToolTipText("Navigate to generated code (double-click one the right-most cells)");
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
        final JScrollPane scrollPane1 = new JScrollPane();
        rootPanel.add(scrollPane1, BorderLayout.CENTER);
        table.setAutoResizeMode(0);
        table.setFillsViewportHeight(false);
        scrollPane1.setViewportView(table);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return rootPanel;
    }

    private class MyDtoTableModel extends AbstractTableModel {

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
                case COL_INDEX_NAME:
                    return "Class";
                case COL_INDEX_REF:
                    return "Ref.";
                case COL_INDEX_STATUS:
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

    private static class DtoTableCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(
                JTable table,
                Object value,
                boolean isSelected,
                boolean hasFocus,
                int row,
                int column) {

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
        table = new JTable() {
            public TableCellRenderer getCellRenderer(int row, int column) {
                if (column == COL_INDEX_STATUS) {
                    return new DtoTableCellRenderer();
                }
                return super.getCellRenderer(row, column);
            }
        };
        // createUIComponents() is called from constructor --> $$$setupUI$$$();
        dto_table_model = new MyDtoTableModel();
        table.setModel(dto_table_model);
        table.getTableHeader().setReorderingAllowed(false);
        // table.setRowHeight(24);
        table.addMouseListener(new MouseAdapter() {
            // public void mouseClicked(MouseEvent e) {  // not working sometime
            public void mousePressed(MouseEvent e) {
                int click_count = e.getClickCount();
                if (click_count == 2 || (click_count == 1 && e.isAltDown())) {
                    int col = table.columnAtPoint(new Point(e.getX(), e.getY()));
                    int row = table.rowAtPoint(new Point(e.getX(), e.getY()));
                    if (row >= 0) {
                        if (col == COL_INDEX_NAME) {
                            navigate_to_sdm_dto_class_async(row);
                        } else if (col == COL_INDEX_REF) {
                            open_sql_async(row);
                        } else {
                            open_target_file_async(row);
                        }
                    } else {
                        open_sdm_xml_async();
                    }
                }
            }
        });
    }

    private void create_crud_xml() {
        IdeaCrudXmlHelpers.get_crud_sdm_xml(project, root_file);
    }

    private void navigate_to_sdm_dto_class_async(int row) {
        final String dto_class_name = (String) table.getValueAt(row, COL_INDEX_NAME);
        IdeaHelpers.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    IdeaHelpers.navigate_to_dto_class_declaration(project, root_file, dto_class_name);
                } catch (Exception e) {
                    IdeaMessageHelpers.show_error_in_ui_thread(e);
                    // e.printStackTrace();
                }
            }
        });
    }

    private void open_sdm_xml_async() {
        IdeaHelpers.invokeLater(new Runnable() {
            @Override
            public void run() {
                IdeaEditorHelpers.open_sdm_xml_sync(project, root_file);
            }
        });
    }

    private void open_sql_async(int row) {
        String ref = (String) table.getValueAt(row, COL_INDEX_REF);
        if (SqlUtils.is_sql_file_ref(ref) == false) {
            navigate_to_sdm_dto_class_async(row);
            return;
        }
        IdeaHelpers.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    String sql_rel_path = IdeaHelpers.load_settings(root_file).getFolders().getSql();
                    String relPath = Helpers.concat_path(sql_rel_path, ref);
                    IdeaEditorHelpers.open_project_file_in_editor_sync(project, relPath);
                } catch (Exception e) {
                    IdeaMessageHelpers.show_error_in_ui_thread(e);
                    // e.printStackTrace();
                }
            }
        });
    }

    protected void open_target_file_async(int row) {
        String dto_class_name = (String) table.getValueAt(row, COL_INDEX_NAME);
        IdeaHelpers.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    Settings settings = IdeaHelpers.load_settings(root_file);
                    IdeaTargetLanguageHelpers.open_target_dto_sync(project, root_file, settings, dto_class_name);
                } catch (Exception e) {
                    // e.printStackTrace();
                    IdeaMessageHelpers.show_error_in_ui_thread(e);
                }
            }
        });
    }

    private int[] get_ui_table_sel_indexes() {
        int rc = table.getModel().getRowCount();
        if (rc == 1) {
            return new int[]{0};
        }
        int[] selected_rows = table.getSelectedRows();
        if (selected_rows.length == 0) {
            selected_rows = new int[rc];
            for (int i = 0; i < rc; i++) {  // add all rows if none selected
                selected_rows[i] = i;
            }
        }
        return selected_rows;
    }

    protected void jaxb_fields_wizard() {
        int[] selected_rows = get_ui_table_sel_indexes();
        if (selected_rows.length == 0) {
            return;
        }
        String dto_class_name = (String) table.getValueAt(selected_rows[0], COL_INDEX_NAME);
        String ref = (String) table.getValueAt(selected_rows[0], COL_INDEX_REF);
        try {
            IdeaEditorHelpers.gen_field_wizard_jaxb(dto_class_name, ref, project, root_file);
        } catch (Exception e) {
            // e.printStackTrace();
            IdeaMessageHelpers.show_error_in_ui_thread(e);
        }
    }

    public void set_project(Project project) {
        this.project = project;
    }

    public void set_root_file(VirtualFile file) {
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
                validate_all_with_progress_sync(gen, settings);
            } finally {
                con.close();
            }
        } catch (Throwable e) {
            // e.printStackTrace();
            IdeaMessageHelpers.show_error_in_ui_thread(e);
        }
    }

    protected void generate_selected_with_progress_sync() {
        final int[] selected_rows = get_ui_table_sel_indexes();
        if (selected_rows.length == 0) {
            return;
        }
        StringBuilder output_dir = new StringBuilder();
        // 1. open connection
        // 2. create the list of generated text
        // 3. close connection
        // 4. update the files from the list
        List<IdeaHelpers.GeneratedFileData> list = new ArrayList<IdeaHelpers.GeneratedFileData>();
        IdeaCG.ProgressError error = new IdeaCG.ProgressError();
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                try {
                    Settings settings = IdeaHelpers.load_settings(root_file);
                    for (int row : selected_rows) {
                        dto_table_model.setValueAt("", row, COL_INDEX_STATUS);
                    }
                    update_table_async();
                    // tableViewer.refresh();
                    Connection con = IdeaHelpers.get_connection(project, settings);
                    try {
                        // !!!! after 'try'
                        IDtoCG gen = IdeaTargetLanguageHelpers.create_dto_cg(con, project, root_file, settings, output_dir);
                        for (int row : selected_rows) {
                            String dto_class_name = (String) dto_table_model.getValueAt(row, COL_INDEX_NAME);
                            try {
                                ProgressManager.progress(dto_class_name);
                                String[] fileContent = gen.translate(dto_class_name);
                                IdeaCG.prepare_generated_file_data(root_file, dto_class_name, fileContent, list);
                                dto_table_model.setValueAt(Const.STATUS_GENERATED, row, COL_INDEX_STATUS);
                                update_table_async();
                            } catch (Throwable e) {
                                String msg = e.getMessage();
                                dto_table_model.setValueAt(msg, row, COL_INDEX_STATUS);
                                IdeaMessageHelpers.add_dto_error_message(settings, root_file, dto_class_name, msg);
                            }
                        }
                    } finally {
                        con.close();
                    }
                    update_table_async();
                } catch (Exception e) {
                    error.error = e;
                }
            }
        };
        ProgressManager.getInstance().runProcessWithProgressSynchronously(runnable, "Generating", false, project);
        // write only the generated files
        // writeActions can show their own dialogs
        try {
            IdeaHelpers.run_write_action_to_generate_source_file(output_dir.toString(), list, project);
        } catch (Exception e) {
            // e.printStackTrace();
            IdeaMessageHelpers.show_error_in_ui_thread(e);
        }
        if (error.error != null) {
            IdeaMessageHelpers.show_error_in_ui_thread(error.error);
        }
        update_table_async();
    }

    private void validate_all_with_progress_sync(IDtoCG gen, Settings settings) {
        ProgressManager progressManager = ProgressManager.getInstance();
        // ProgressIndicator indicator = progressManager.getProgressIndicator();
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                // === panedrone: don't ask it in loop because of
                // "Cannot read the array length because "<local3>" is null"
                int rc = dto_table_model.getRowCount();
                for (int i = 0; i < rc; i++) {
                    String dto_class_name = (String) dto_table_model.getValueAt(i, COL_INDEX_NAME);
                    ProgressManager.progress(dto_class_name);
                    progressManager.getProgressIndicator().setText(dto_class_name);
                    try {
                        String[] fileContent = gen.translate(dto_class_name);
                        StringBuilder validationBuff = new StringBuilder();
                        IdeaCG.validate_single_dto_ignoring_eol(project, root_file, settings, dto_class_name, fileContent, validationBuff);
                        String status = validationBuff.toString();
                        if (status.isEmpty()) {
                            dto_table_model.setValueAt(Const.STATUS_OK, i, COL_INDEX_STATUS);
                        } else {
                            dto_table_model.setValueAt(status, i, COL_INDEX_STATUS);
                            IdeaMessageHelpers.add_dto_error_message(settings, root_file, dto_class_name, status);
                        }
                    } catch (Throwable e) {
                        // // e.printStackTrace();
                        String msg = e.getMessage();
                        dto_table_model.setValueAt(msg, i, COL_INDEX_STATUS);
                        IdeaMessageHelpers.add_dto_error_message(settings, root_file, dto_class_name, msg);
                    }
                    update_table_async();
                }
            }
        };
        progressManager.runProcessWithProgressSynchronously(runnable, "Validating", false, project);
    }

    private void update_table_async() {
        // to prevent:
        // WARN - intellij.ide.HackyRepaintManager
        IdeaHelpers.invokeLater(new Runnable() {
            public void run() {
                table.updateUI();
            }
        });
    }

    private void reload_table() throws Exception {
        try {
            ArrayList<String[]> list = dto_table_model.getList();
            list.clear();
            String sdm_xml_folder_full_path = root_file.getParent().getPath();
            String sdm_xml_abs_path = sdm_xml_folder_full_path + "/" + Const.SDM_XML;
            String sdm_xsd_abs_path = sdm_xml_folder_full_path + "/" + Const.SDM_XSD;
            List<DtoClass> res = SdmUtils.get_dto_classes(sdm_xml_abs_path, sdm_xsd_abs_path);
            for (DtoClass cls : res) {
                String[] item = new String[3];
                item[COL_INDEX_NAME] = cls.getName();
                item[COL_INDEX_REF] = cls.getRef();
                item[COL_INDEX_STATUS] = "";
                list.add(item);
            }
        } finally {
            dto_table_model.refresh(); // table.updateUI();
        }
    }

    public void reload_table(boolean showErrorMsg) {
        try {
            reload_table();
        } catch (Throwable e) {
            if (showErrorMsg) {
                // e.printStackTrace();
                IdeaMessageHelpers.show_error_in_ui_thread(e);
            }
        }
    }
}