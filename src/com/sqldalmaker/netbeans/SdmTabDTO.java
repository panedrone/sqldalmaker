/*
    Copyright 2011-2023 sqldalmaker@gmail.com
    SQL DAL Maker Website: http://sqldalmaker.sourceforge.net
    Read LICENSE.txt in the root of this project/archive for details.
 */
package com.sqldalmaker.netbeans;

import com.sqldalmaker.cg.IDtoCG;
import com.sqldalmaker.cg.SqlUtils;
import com.sqldalmaker.common.Const;
import com.sqldalmaker.common.InternalException;
import com.sqldalmaker.jaxb.sdm.DtoClass;
import com.sqldalmaker.jaxb.settings.Settings;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.PatternSyntaxException;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.RowFilter;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;

import org.netbeans.core.spi.multiview.MultiViewElement;
import org.openide.filesystems.FileObject;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;
import org.openide.util.NbBundle.Messages;
import org.openide.util.RequestProcessor;
import org.openide.windows.TopComponent;

/*
 * @author sqldalmaker@gmail.com
 *
 * 18.12.2023 03:01 1.292
 *
 * 12.05.2023 23:01 1.283
 *
 * 23.02.2023 15:42 1.279
 *
 * 30.10.2022 08:03 1.266
 *
 * 08.05.2021 22:29 1.200
 *
 * 08.04.2021 22:08
 *
 */
@MultiViewElement.Registration(
        displayName = "#LBL_Sdm_DTO",
        iconBase = "com/sqldalmaker/netbeans/sqldalmaker.gif",
        mimeType = "text/sqldalmaker+dal",
        persistenceType = TopComponent.PERSISTENCE_NEVER,
        preferredID = "SdmTabDTO",
        position = 1000
)
@Messages("LBL_Sdm_DTO=DTO")
public final class SdmTabDTO extends SdmMultiViewCloneableEditor {

    private final MyTableModel my_table_model;
    private TableRowSorter<AbstractTableModel> sorter;

    public SdmTabDTO(Lookup lookup) {
        super(lookup);
        initComponents();
        Cursor wc = new Cursor(Cursor.HAND_CURSOR);
        for (Component c : jToolBar1.getComponents()) {
            if (c instanceof JButton) {
                JButton b = (JButton) c;
                b.setCursor(wc);
                b.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 4, 0, 4));
            }
        }
        jTextField1.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void changedUpdate(DocumentEvent e) {
                set_filter();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                set_filter();
            }

            @Override
            public void insertUpdate(DocumentEvent e) {
                set_filter();
            }
        });
        my_table_model = new MyTableModel();
        createUIComponents();
        reload_table(false); // dto.xml is missing at beginning

        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                check_need_sdm_migrate();
            }
        });
    }

    private void check_need_sdm_migrate() {
        FileObject root_file = obj.getPrimaryFile();
        FileObject mp_folder = root_file.getParent();
        FileObject xml_file = mp_folder.getFileObject(Const.SDM_XML);
        if (xml_file == null) {
            FileObject old_file = mp_folder.getFileObject("dto.xml");
            if (old_file != null) {
                NbpIdeMessageHelpers.show_info_in_ui_thread("Click 'Admin' for migration...");
            }
        }
    }

    @Override
    public JComponent getToolbarRepresentation() {
        return jToolBar1;
    }

    private void reload_table() throws Exception {
        try {
            final ArrayList<String[]> list = my_table_model.get_list();
            list.clear();
            List<DtoClass> res = NbpHelpers.get_dto_classes(obj);
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
        } catch (Exception e) {
            if (showErrorMsg) {
                Exceptions.printStackTrace(e);
                NbpIdeMessageHelpers.show_error_in_ui_thread(e);
            }
        }
    }

    private void set_filter() {
        try {
            RowFilter<AbstractTableModel, Object> rf = RowFilter.regexFilter(jTextField1.getText(), 0);
            sorter.setRowFilter(rf);
        } catch (PatternSyntaxException e) {
            sorter.setRowFilter(null); // don't filter
        }
    }

    private void update_table_async() {
        // table.updateUI();       throws NullPointerException without SwingUtilities.invokeLater 
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                table.updateUI();
            }
        });
    }

    private void schedule_generate() throws Exception {
        final Settings settings = NbpHelpers.load_settings(obj);
        final int[] selected_rows = get_selection();
        if (selected_rows.length == 0) {
            return;
        }
        for (int row : selected_rows) {
            table.setValueAt("", row, 2);
        }
        // myTableModel.refresh(); cleares selection
        table.updateUI();
        // 1. open connection
        // 2. create the list of generated java
        // 3. close connection
        // 4. update the files from the list
        // http://stackoverflow.com/questions/22326822/netbeans-platform-progress-bar
        RequestProcessor RP = new RequestProcessor("Generate DTO classes RP");
        // final ProgressHandle ph = ProgressHandle.createHandle("Generate DTO class(es)");
        RequestProcessor.Task task = RP.create(new Runnable() {
            @Override
            public void run() {
                // ph.start();
                if (selected_rows.length == 0) {
                    return;
                }
                NbpIdeConsoleUtil ide_log = new NbpIdeConsoleUtil(settings, obj);
//                ide_log.add_debug_message("STARTED...");
                try {
                    //ph.progress("Connecting...");
                    Connection con = NbpHelpers.get_connection(obj); // !!! inside try/finally to ensure ph.finish()!!!
                    //ph.progress("Connected");
                    try {
                        StringBuilder output_dir = new StringBuilder();
                        // !!!! after 'try'
                        IDtoCG gen = NbpTargetLanguageHelpers.create_dto_cg(con, obj, settings, output_dir);
                        String output_dir_rel_path = output_dir.toString();
                        for (int row : selected_rows) {
                            String dto_class_name = (String) table.getValueAt(row, 0);
                            // ph.progress(dtoClassName);
                            try {
                                String fileContent[] = gen.translate(dto_class_name);
                                String file_name = NbpTargetLanguageHelpers.get_target_file_name(obj, dto_class_name);
                                NbpHelpers.save_text_to_file(obj, output_dir_rel_path, file_name, fileContent[0]);
                                ide_log.add_success_message(dto_class_name + " -> " + Const.STATUS_GENERATED);
                                table.setValueAt(Const.STATUS_GENERATED, row, 2);
                                update_table_async();
                            } catch (Exception ex) {
                                String msg = ex.getMessage();
                                if (msg == null) {
                                    msg = "???";
                                }
                                msg = ex.getClass().getName() + ": " + dto_class_name + " -> " + msg;
                                table.setValueAt(msg, row, 2);
                                ide_log.add_error_message(msg);
                            }
                        }
                    } finally {
                        con.close();
                        update_table_async();
                    }
                } catch (Exception ex) {
                    ide_log.add_error_message(ex);
                    // ex.printStackTrace();
                    NbpIdeMessageHelpers.show_error_in_ui_thread(ex);
                } finally {
                    ide_log.add_debug_message("DONE");
                    // ph.finish();
                    // errLog.close();
                }
            }
        });
        //////////////////////////////////////////////////////
        task.schedule(0);
    }

    private void generate_async() {
        try {
            schedule_generate();
        } catch (Exception e) {
            // e.printStackTrace();
            NbpIdeMessageHelpers.show_error_in_ui_thread(e);
        }
    }

    private void validate_all_async() {
        try {
            schedule_validate_all();
        } catch (Exception e) {
            // e.printStackTrace();
            NbpIdeMessageHelpers.show_error_in_ui_thread(e);
        }
    }

    private void schedule_validate_all() throws Exception {
        final Settings settings = NbpHelpers.load_settings(obj);
        reload_table();
        // 1. open connection
        // 2. create the list of generated java
        // 3. close connection
        // 4. update the files from the list
        // http://stackoverflow.com/questions/22326822/netbeans-platform-progress-bar
        RequestProcessor RP = new RequestProcessor("Validate all DTO classes RP");
        // final ProgressHandle ph = ProgressHandle.createHandle("Validate DTO classes");
        RequestProcessor.Task task = RP.create(new Runnable() {
            @Override
            public void run() {
                // ph.start();
                NbpIdeConsoleUtil ide_log = new NbpIdeConsoleUtil(settings, obj);
//                ide_log.add_debug_message("STARTED...");
                try {
                    // ph.progress("Connecting...");
                    Connection con = NbpHelpers.get_connection(obj); // !!! inside try/finally to ensure ph.finish()!!!
                    // ph.progress("Connected");
                    try {
                        StringBuilder output_dir = new StringBuilder();
                        // !!!! after 'try'
                        IDtoCG gen = NbpTargetLanguageHelpers.create_dto_cg(con, obj, settings, output_dir);
                        // === panedrone: don't ask it in loop because of
                        // "Cannot read the array length because "<local3>" is null"
                        int rc = my_table_model.getRowCount();
                        for (int i = 0; i < rc; i++) {
                            String dto_class_name = (String) table.getValueAt(i, 0);
                            // ph.progress(dtoClassName);
                            try {
                                String[] file_content = gen.translate(dto_class_name);
                                StringBuilder validation_buff = new StringBuilder();
                                NbpTargetLanguageHelpers.validate_dto(obj, settings, dto_class_name, file_content, validation_buff);
                                String status = validation_buff.toString();
                                if (status.length() == 0) {
                                    table.setValueAt(Const.STATUS_OK, i, 2);
                                    ide_log.add_success_message(dto_class_name + " -> " + Const.STATUS_OK);
                                } else {
                                    table.setValueAt(status, i, 2);
                                    ide_log.add_error_message(dto_class_name + " -> " + status);
                                }
                                update_table_async();
                            } catch (Exception ex) {
                                String msg = ex.getMessage();
                                if (msg == null) {
                                    msg = "???";
                                }
                                msg = ex.getClass().getName() + ": " + dto_class_name + " -> " + msg;
                                table.setValueAt(msg, i, 2);
                                ide_log.add_error_message(msg);
                            }
                        }
                    } finally {
                        con.close();
                        // Exception can occur at 3rd line (for example):
                        // refresh first 3 lines
                        // error lines are not generated but update them too
                        SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                table.updateUI();
                            }
                        });
                    }
                } catch (Exception ex) {
                    ide_log.add_error_message(ex);
                    // ex.printStackTrace();
                    NbpIdeMessageHelpers.show_error_in_ui_thread(ex);
                } finally {
//                    ide_log.add_debug_message("DONE");
                    // ph.finish();
                    // errLog.close();
                }
            }
        });
        task.schedule(0);
    }

    private void gen_tmp_field_tags() {
        try {
            int[] selected_rows = get_selection();
            String class_name = (String) table.getValueAt(selected_rows[0], 0);
            String ref = (String) table.getValueAt(selected_rows[0], 1);
            NbpIdeEditorHelpers.generate_tmp_field_tags_and_open_in_editor_async(obj, class_name, ref);
        } catch (Exception e) {
            //e.printStackTrace();
            NbpIdeMessageHelpers.show_error_in_ui_thread(e);
        }
    }

    private static class MyTableModel extends AbstractTableModel {

        private final ArrayList<String[]> list = new ArrayList<String[]>();

        public ArrayList<String[]> get_list() {
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
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            String s_value = (String) value;
            setText(s_value);
            if (isSelected || Const.STATUS_OK.equals(s_value) || Const.STATUS_GENERATED.equals(s_value)) {
                TableCellRenderer r = table.getCellRenderer(row, column);
                Component c_0 = r.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, 0);
                c.setForeground(c_0.getForeground());
            } else {
                c.setForeground(Color.RED);
            }
            return c;
        }
    }

    private JTable table;

    private void createUIComponents() {
        final ColorTableCellRenderer colorRenderer = new ColorTableCellRenderer();
        table = new javax.swing.JTable();
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table = new JTable() {
            @Override
            public TableCellRenderer getCellRenderer(int row, int column) {
                if (column == 2) {
                    return colorRenderer;
                }
                return super.getCellRenderer(row, column);
            }
        };
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        jScrollPane1.setViewportView(table);
        table.setModel(my_table_model);
        table.getTableHeader().setReorderingAllowed(false);
        sorter = new TableRowSorter<AbstractTableModel>(my_table_model);
        table.setRowSorter(sorter);
        // table.setRowHeight(24);
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int col = table.columnAtPoint(new Point(e.getX(), e.getY()));
                    int row = table.rowAtPoint(new Point(e.getX(), e.getY()));
                    if (row >= 0) {
                        switch (col) {
                            case 0:
                                goto_dto_class_declaration_async();
                                break;
                            case 1:
                                open_sql();
                                break;
                            default:
                                open_generated_source_file();
                                break;
                        }
                    } else {
                        open_dto_xml();
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

    private void goto_dto_class_declaration_async() {
        try {
            FileObject this_doc_file = obj.getPrimaryFile();
            if (this_doc_file == null) {
                return;
            }
            final int[] selected_rows = get_selection();
            if (selected_rows.length == 1) {
                FileObject folder = this_doc_file.getParent();
                String dto_class_name = (String) table.getValueAt(selected_rows[0], 0);
                XmlEditorUtil.goto_sdm_class_declaration_async(folder, dto_class_name);
            }
        } catch (Exception ex) {
            // ex.printStackTrace();
            NbpIdeMessageHelpers.show_error_in_ui_thread(ex);
        }
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
            throw new InternalException("Selection is empty.");
        }
        return selected_rows;
    }

    private void open_sql() {
        try {
            int[] selected_rows = get_selection();
            String ref = (String) table.getValueAt(selected_rows[0], 1);
            if (SqlUtils.is_sql_file_ref(ref) == false) {
                return;
            }
            Settings settings = NbpHelpers.load_settings(obj);
            String relPath = settings.getFolders().getSql() + "/" + ref;
            NbpIdeEditorHelpers.open_project_file_in_editor_async(obj, relPath);
        } catch (Exception ex) {
            // ex.printStackTrace();
            NbpIdeMessageHelpers.show_error_in_ui_thread(ex);
        }
    }

    private void open_dto_xml() {
        NbpIdeEditorHelpers.open_sdm_folder_file_async(obj, Const.SDM_XML);
    }

    protected void open_generated_source_file() {
        try {
            int[] selected_rows = get_selection();
            Settings settings = NbpHelpers.load_settings(obj);
            String dto_class_name = (String) table.getValueAt(selected_rows[0], 0);
            NbpTargetLanguageHelpers.open_in_editor_async(obj, settings, dto_class_name, settings.getDto().getScope());
        } catch (Exception e) {
            // e.printStackTrace();
            NbpIdeMessageHelpers.show_error_in_ui_thread(e);
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jToolBar1 = new javax.swing.JToolBar();
        jSeparator1 = new javax.swing.JToolBar.Separator();
        jButton1 = new javax.swing.JButton();
        jButton2 = new javax.swing.JButton();
        jButton3 = new javax.swing.JButton();
        jButton4 = new javax.swing.JButton();
        jButton5 = new javax.swing.JButton();
        jButton6 = new javax.swing.JButton();
        jButton9 = new javax.swing.JButton();
        jButton10 = new javax.swing.JButton();
        jPanel1 = new javax.swing.JPanel();
        jTextField1 = new javax.swing.JTextField();
        jScrollPane1 = new javax.swing.JScrollPane();

        setLayout(new java.awt.BorderLayout());

        jToolBar1.setRollover(true);
        jToolBar1.add(jSeparator1);

        jButton1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/img/xmldoc.gif"))); // NOI18N
        org.openide.awt.Mnemonics.setLocalizedText(jButton1, org.openide.util.NbBundle.getMessage(SdmTabDTO.class, "SdmTabDTO.jButton1.text")); // NOI18N
        jButton1.setToolTipText(org.openide.util.NbBundle.getMessage(SdmTabDTO.class, "SdmTabDTO.jButton1.toolTipText")); // NOI18N
        jButton1.setFocusable(false);
        jButton1.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jButton1.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });
        jToolBar1.add(jButton1);

        jButton2.setIcon(new javax.swing.ImageIcon(getClass().getResource("/img/qrydoc.gif"))); // NOI18N
        org.openide.awt.Mnemonics.setLocalizedText(jButton2, org.openide.util.NbBundle.getMessage(SdmTabDTO.class, "SdmTabDTO.jButton2.text")); // NOI18N
        jButton2.setToolTipText(org.openide.util.NbBundle.getMessage(SdmTabDTO.class, "SdmTabDTO.jButton2.toolTipText")); // NOI18N
        jButton2.setFocusable(false);
        jButton2.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jButton2.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton2ActionPerformed(evt);
            }
        });
        jToolBar1.add(jButton2);

        jButton3.setIcon(new javax.swing.ImageIcon(getClass().getResource("/img/GeneratedFile.gif"))); // NOI18N
        org.openide.awt.Mnemonics.setLocalizedText(jButton3, org.openide.util.NbBundle.getMessage(SdmTabDTO.class, "SdmTabDTO.jButton3.text")); // NOI18N
        jButton3.setToolTipText(org.openide.util.NbBundle.getMessage(SdmTabDTO.class, "SdmTabDTO.jButton3.toolTipText")); // NOI18N
        jButton3.setFocusable(false);
        jButton3.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jButton3.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jButton3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton3ActionPerformed(evt);
            }
        });
        jToolBar1.add(jButton3);

        jButton4.setIcon(new javax.swing.ImageIcon(getClass().getResource("/img/177.png"))); // NOI18N
        org.openide.awt.Mnemonics.setLocalizedText(jButton4, org.openide.util.NbBundle.getMessage(SdmTabDTO.class, "SdmTabDTO.jButton4.text")); // NOI18N
        jButton4.setToolTipText(org.openide.util.NbBundle.getMessage(SdmTabDTO.class, "SdmTabDTO.jButton4.toolTipText")); // NOI18N
        jButton4.setFocusable(false);
        jButton4.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jButton4.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jButton4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton4ActionPerformed(evt);
            }
        });
        jToolBar1.add(jButton4);

        jButton5.setIcon(new javax.swing.ImageIcon(getClass().getResource("/img/180.png"))); // NOI18N
        org.openide.awt.Mnemonics.setLocalizedText(jButton5, org.openide.util.NbBundle.getMessage(SdmTabDTO.class, "SdmTabDTO.jButton5.text")); // NOI18N
        jButton5.setToolTipText(org.openide.util.NbBundle.getMessage(SdmTabDTO.class, "SdmTabDTO.jButton5.toolTipText")); // NOI18N
        jButton5.setFocusable(false);
        jButton5.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jButton5.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jButton5.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton5ActionPerformed(evt);
            }
        });
        jToolBar1.add(jButton5);

        jButton6.setIcon(new javax.swing.ImageIcon(getClass().getResource("/img/arrow-circle-double-135.png"))); // NOI18N
        org.openide.awt.Mnemonics.setLocalizedText(jButton6, org.openide.util.NbBundle.getMessage(SdmTabDTO.class, "SdmTabDTO.jButton6.text")); // NOI18N
        jButton6.setToolTipText(org.openide.util.NbBundle.getMessage(SdmTabDTO.class, "SdmTabDTO.jButton6.toolTipText")); // NOI18N
        jButton6.setFocusable(false);
        jButton6.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jButton6.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jButton6.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton6ActionPerformed(evt);
            }
        });
        jToolBar1.add(jButton6);

        jButton9.setIcon(new javax.swing.ImageIcon(getClass().getResource("/img/compile.png"))); // NOI18N
        org.openide.awt.Mnemonics.setLocalizedText(jButton9, org.openide.util.NbBundle.getMessage(SdmTabDTO.class, "SdmTabDTO.jButton9.text")); // NOI18N
        jButton9.setToolTipText(org.openide.util.NbBundle.getMessage(SdmTabDTO.class, "SdmTabDTO.jButton9.toolTipText")); // NOI18N
        jButton9.setFocusable(false);
        jButton9.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jButton9.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jButton9.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton9ActionPerformed(evt);
            }
        });
        jToolBar1.add(jButton9);

        jButton10.setIcon(new javax.swing.ImageIcon(getClass().getResource("/img/validate.gif"))); // NOI18N
        org.openide.awt.Mnemonics.setLocalizedText(jButton10, org.openide.util.NbBundle.getMessage(SdmTabDTO.class, "SdmTabDTO.jButton10.text")); // NOI18N
        jButton10.setToolTipText(org.openide.util.NbBundle.getMessage(SdmTabDTO.class, "SdmTabDTO.jButton10.toolTipText")); // NOI18N
        jButton10.setFocusable(false);
        jButton10.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jButton10.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jButton10.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton10ActionPerformed(evt);
            }
        });
        jToolBar1.add(jButton10);

        add(jToolBar1, java.awt.BorderLayout.PAGE_START);

        jPanel1.setLayout(new java.awt.BorderLayout());

        jTextField1.setText(org.openide.util.NbBundle.getMessage(SdmTabDTO.class, "SdmTabDTO.jTextField1.text")); // NOI18N
        jPanel1.add(jTextField1, java.awt.BorderLayout.PAGE_START);
        jPanel1.add(jScrollPane1, java.awt.BorderLayout.CENTER);

        add(jPanel1, java.awt.BorderLayout.CENTER);
    }// </editor-fold>//GEN-END:initComponents

    private void jButton6ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton6ActionPerformed
        reload_table(true);
    }//GEN-LAST:event_jButton6ActionPerformed

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        open_dto_xml();
    }//GEN-LAST:event_jButton1ActionPerformed

    private void jButton3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton3ActionPerformed
        open_generated_source_file();
    }//GEN-LAST:event_jButton3ActionPerformed

    private void jButton9ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton9ActionPerformed
        generate_async();
    }//GEN-LAST:event_jButton9ActionPerformed

    private void jButton10ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton10ActionPerformed
        validate_all_async();
    }//GEN-LAST:event_jButton10ActionPerformed

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed
        open_sql();
    }//GEN-LAST:event_jButton2ActionPerformed

    private void jButton5ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton5ActionPerformed
        try {
            NbpCrudXmHelpers.get_crud_dto_xml(obj);
        } catch (Exception ex) {
            // ex.printStackTrace();
            NbpIdeMessageHelpers.show_error_in_ui_thread(ex);
        }
    }//GEN-LAST:event_jButton5ActionPerformed

    private void jButton4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton4ActionPerformed
        gen_tmp_field_tags();
    }//GEN-LAST:event_jButton4ActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton10;
    private javax.swing.JButton jButton2;
    private javax.swing.JButton jButton3;
    private javax.swing.JButton jButton4;
    private javax.swing.JButton jButton5;
    private javax.swing.JButton jButton6;
    private javax.swing.JButton jButton9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JToolBar.Separator jSeparator1;
    private javax.swing.JTextField jTextField1;
    private javax.swing.JToolBar jToolBar1;
    // End of variables declaration//GEN-END:variables
}
