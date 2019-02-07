/*
 * Copyright 2011-2018 sqldalmaker@gmail.com
 * SQL DAL Maker Website: http://sqldalmaker.sourceforge.net
 * Read LICENSE.txt in the root of this project/archive for details.
 */
package com.sqldalmaker.netbeans;

import com.sqldalmaker.cg.Helpers;
import com.sqldalmaker.cg.IDtoCG;
import com.sqldalmaker.common.Const;
import com.sqldalmaker.common.InternalException;
import com.sqldalmaker.jaxb.dto.DtoClass;
import com.sqldalmaker.jaxb.settings.Settings;
import java.awt.Color;
import java.awt.Component;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.PatternSyntaxException;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
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
import org.openide.util.Lookup;
import org.openide.util.NbBundle.Messages;
import org.openide.util.RequestProcessor;
import org.openide.windows.TopComponent;

/**
 *
 * @author sqldalmaker@gmail.com
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

    private static final String STATUS_GENERATED = "Generated successfully";
    private static final String STATUS_OK = "OK";

    private final MyTableModel myTableModel;

    private TableRowSorter<AbstractTableModel> sorter;

    public SdmTabDTO(Lookup lookup) {
        super(lookup);
        initComponents();

        jTextField1.getDocument().addDocumentListener(new DocumentListener() {
            private void updateFilter() {
                setFilter();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                updateFilter();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                updateFilter();
            }

            @Override
            public void insertUpdate(DocumentEvent e) {
                updateFilter();
            }
        });

        myTableModel = new MyTableModel();

        createUIComponents();

        reloadTable(false); // dto.xml is missing at beginning
    }

    @Override
    public JComponent getToolbarRepresentation() {
        return jToolBar1;
    }

    private void reloadTable() throws Exception {

        try {

            final ArrayList<String[]> list = myTableModel.getList();
            list.clear();

            List<DtoClass> res = NbpHelpers.get_dto_classes(obj);

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
                //e.printStackTrace();
                NbpIdeMessageHelpers.show_error_in_ui_thread(e);
            }
        }
    }

    private void setFilter() {
        try {

            RowFilter<AbstractTableModel, Object> rf = RowFilter.regexFilter(jTextField1.getText(), 0);
            sorter.setRowFilter(rf);

        } catch (PatternSyntaxException e) {

            sorter.setRowFilter(null); // don't filter
        }
    }

    private void selectAll() {
        ListSelectionModel selectionModel = table.getSelectionModel();
        selectionModel.setSelectionInterval(0, table.getRowCount() - 1);
    }

    private void deselectAll() {
        table.clearSelection();
    }

    private void generate() throws Exception {

        final Settings settings = NbpHelpers.load_settings(obj);

        final int[] selectedRows = getSelection();

        for (int row : selectedRows) {
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
                if (selectedRows.length == 0) {
                    return;
                }

                NbpIdeConsoleUtil err_log = new NbpIdeConsoleUtil(settings, obj);

                try {

                    //ph.progress("Connecting...");
                    Connection con = NbpHelpers.get_connection(obj); // !!! inside try/finally to ensure ph.finish()!!!

                    //ph.progress("Connected");
                    try {

                        StringBuilder output_dir = new StringBuilder();
                        // !!!! after 'try'
                        IDtoCG gen = NbpTargetLanguageHelpers.create_dto_cg(con, obj, settings, output_dir);

                        String output_dir_rel_path = output_dir.toString();

                        for (int row : selectedRows) {

                            String dtoClassName = (String) table.getValueAt(row, 0);

                            // ph.progress(dtoClassName);
                            try {

                                String fileContent[] = gen.translate(dtoClassName);

                                String fileName = NbpTargetLanguageHelpers.get_dao_xml_file_name(obj, dtoClassName);

                                NbpHelpers.save_text_to_file(obj, output_dir_rel_path, fileName, fileContent[0]);

                                table.setValueAt(STATUS_GENERATED, row, 2);

                                try {
                                    Thread.sleep(50);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }

                                // myTableModel.refresh(); cleares selection
                                // table.updateUI();       throws NullPointerException without SwingUtilities.invokeLater 
                                SwingUtilities.invokeLater(new Runnable() {
                                    @Override
                                    public void run() {
                                        table.updateUI();
                                    }
                                });

                            } catch (Throwable ex) {

                                String msg = ex.getMessage();

                                if (msg == null) {
                                    msg = "???";
                                }

                                table.setValueAt(msg, row, 2);

                                // throw ex; // outer 'catch' cannot read the
                                // message
                                // !!!! not Internal_Exception to show Exception
                                // class
                                // throw new Exception(ex);
                                // ex.printStackTrace();
//                                MyNbHelpers.showErrorInUIThread(ex);
//                                return;
                                err_log.add_error_message(dtoClassName, msg);
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

                } catch (Throwable ex) {

                    err_log.add_error_message(ex);

                    // ex.printStackTrace();
                    NbpIdeMessageHelpers.show_error_in_ui_thread(ex);

                } finally {

                    // ph.finish();
                    // errLog.close();
                }
            }
        });

        //////////////////////////////////////////////////////
        if (selectedRows.length > 0) {
            task.schedule(0);
        }
    }

    private void generate2() {
        try {

            generate();

        } catch (Throwable e) {

            // e.printStackTrace();
            NbpIdeMessageHelpers.show_error_in_ui_thread(e);
        }
    }

    private void validateAll2() {
        try {

            validateAll();

        } catch (Throwable e) {

            // e.printStackTrace();
            NbpIdeMessageHelpers.show_error_in_ui_thread(e);
        }
    }

    private void validateAll() throws Exception {

        final Settings settings = NbpHelpers.load_settings(obj);

        reloadTable();

        // 1. open connection
        // 2. create the list of generated java
        // 3. close connection
        // 4. update the files from the list
        // http://stackoverflow.com/questions/22326822/netbeans-platform-progress-bar
        RequestProcessor RP = new RequestProcessor("Validate DTO classes RP");
        // final ProgressHandle ph = ProgressHandle.createHandle("Validate DTO classes");
        RequestProcessor.Task task = RP.create(new Runnable() {
            @Override
            public void run() {

                // ph.start();
                NbpIdeConsoleUtil err_log = new NbpIdeConsoleUtil(settings, obj);

                try {

                    // ph.progress("Connecting...");
                    Connection con = NbpHelpers.get_connection(obj); // !!! inside try/finally to ensure ph.finish()!!!

                    // ph.progress("Connected");
                    try {

                        StringBuilder output_dir = new StringBuilder();
                        // !!!! after 'try'
                        IDtoCG gen = NbpTargetLanguageHelpers.create_dto_cg(con, obj, settings, output_dir);

                        for (int i = 0; i < myTableModel.getRowCount(); i++) {

                            String dtoClassName = (String) table.getValueAt(i, 0);

                            // ph.progress(dtoClassName);
                            try {

                                String fileContent[] = gen.translate(dtoClassName);

                                StringBuilder validationBuff = new StringBuilder();

                                NbpTargetLanguageHelpers.validate_dto(obj, settings, dtoClassName, fileContent, validationBuff);

                                String status = validationBuff.toString();

                                if (status.length() == 0) {
                                    table.setValueAt(STATUS_OK, i, 2);
                                } else {
                                    table.setValueAt(status, i, 2);
                                    err_log.add_error_message(dtoClassName, status);
                                }

                                try {
                                    Thread.sleep(50);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }

                                // myTableModel.refresh(); cleares selection
                                // table.updateUI();       throws NullPointerException without SwingUtilities.invokeLater 
                                SwingUtilities.invokeLater(new Runnable() {
                                    @Override
                                    public void run() {
                                        table.updateUI();
                                    }
                                });

                            } catch (Throwable ex) {

                                String msg = ex.getMessage();

                                if (msg == null) {
                                    msg = "???";
                                }

                                table.setValueAt(msg, i, 2);

                                // throw ex; // outer 'catch' cannot read the
                                // message
                                // !!!! not Internal_Exception to show Exception
                                // class
                                // throw new Exception(ex);
                                // ex.printStackTrace();
                                //MyNbHelpers.showErrorInUIThread(ex);
                                //return;
                                err_log.add_error_message(dtoClassName, msg);
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

                } catch (Throwable ex) {

                    err_log.add_error_message(ex);

                    // ex.printStackTrace();
                    NbpIdeMessageHelpers.show_error_in_ui_thread(ex);

                } finally {

                    // ph.finish();
                    // errLog.close();
                }
            }
        });
        task.schedule(0);
    }

    private void genTmpFieldTags() {
        try {

            int[] selectedRows = getSelection();

            String className = (String) table.getValueAt(selectedRows[0], 0);
            String ref = (String) table.getValueAt(selectedRows[0], 1);

            NbpIdeEditorHelpers.generate_tmp_field_tags_and_open_in_editor_async(obj, className, ref);

        } catch (Exception e) {
            e.printStackTrace();
            NbpIdeMessageHelpers.show_error_in_ui_thread(e);
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

        table.setModel(myTableModel);
        table.getTableHeader().setReorderingAllowed(false);

        sorter = new TableRowSorter<AbstractTableModel>(myTableModel);
        table.setRowSorter(sorter);
        // table.setRowHeight(24);
        table.addMouseListener(new MouseAdapter() {
            @Override
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

    private void navigateToDtoClassDeclaration() {
        // TODO implement navigation
        NbpIdeEditorHelpers.open_metaprogram_file_async(obj, Const.DTO_XML);
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

    private void openSQL() {

        try {
            int[] selectedRows = getSelection();

            String ref = (String) table.getValueAt(selectedRows[0], 1);

            if (Helpers.is_table_ref(ref)) {
                return;
            }

            Settings settings = NbpHelpers.load_settings(obj);

            String relPath = settings.getFolders().getSql() + "/" + ref;

            NbpIdeEditorHelpers.open_project_file_in_editor_async(obj, relPath);

        } catch (Throwable ex) {
            // ex.printStackTrace();
            NbpIdeMessageHelpers.show_error_in_ui_thread(ex);
        }
    }

    private void openDtoXML() {
        NbpIdeEditorHelpers.open_metaprogram_file_async(obj, Const.DTO_XML);
    }

    protected void openGeneratedSourceFile() {
        try {

            int[] selectedRows = getSelection();

            Settings settings = NbpHelpers.load_settings(obj);

            String value = (String) table.getValueAt(selectedRows[0], 0);

            NbpTargetLanguageHelpers.open_in_editor(obj, value, settings, settings.getDto().getScope());

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
        jButton7 = new javax.swing.JButton();
        jButton8 = new javax.swing.JButton();
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

        jButton7.setIcon(new javax.swing.ImageIcon(getClass().getResource("/img/none.gif"))); // NOI18N
        org.openide.awt.Mnemonics.setLocalizedText(jButton7, org.openide.util.NbBundle.getMessage(SdmTabDTO.class, "SdmTabDTO.jButton7.text")); // NOI18N
        jButton7.setToolTipText(org.openide.util.NbBundle.getMessage(SdmTabDTO.class, "SdmTabDTO.jButton7.toolTipText")); // NOI18N
        jButton7.setFocusable(false);
        jButton7.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jButton7.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jButton7.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton7ActionPerformed(evt);
            }
        });
        jToolBar1.add(jButton7);

        jButton8.setIcon(new javax.swing.ImageIcon(getClass().getResource("/img/text.gif"))); // NOI18N
        org.openide.awt.Mnemonics.setLocalizedText(jButton8, org.openide.util.NbBundle.getMessage(SdmTabDTO.class, "SdmTabDTO.jButton8.text")); // NOI18N
        jButton8.setToolTipText(org.openide.util.NbBundle.getMessage(SdmTabDTO.class, "SdmTabDTO.jButton8.toolTipText")); // NOI18N
        jButton8.setFocusable(false);
        jButton8.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jButton8.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jButton8.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton8ActionPerformed(evt);
            }
        });
        jToolBar1.add(jButton8);

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
        reloadTable(true);
    }//GEN-LAST:event_jButton6ActionPerformed

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        openDtoXML();
    }//GEN-LAST:event_jButton1ActionPerformed

    private void jButton7ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton7ActionPerformed
        deselectAll();
    }//GEN-LAST:event_jButton7ActionPerformed

    private void jButton8ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton8ActionPerformed
        selectAll();
    }//GEN-LAST:event_jButton8ActionPerformed

    private void jButton3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton3ActionPerformed
        openGeneratedSourceFile();
    }//GEN-LAST:event_jButton3ActionPerformed

    private void jButton9ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton9ActionPerformed
        generate2();
    }//GEN-LAST:event_jButton9ActionPerformed

    private void jButton10ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton10ActionPerformed
        validateAll2();
    }//GEN-LAST:event_jButton10ActionPerformed

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed
        openSQL();
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
        genTmpFieldTags();
    }//GEN-LAST:event_jButton4ActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton10;
    private javax.swing.JButton jButton2;
    private javax.swing.JButton jButton3;
    private javax.swing.JButton jButton4;
    private javax.swing.JButton jButton5;
    private javax.swing.JButton jButton6;
    private javax.swing.JButton jButton7;
    private javax.swing.JButton jButton8;
    private javax.swing.JButton jButton9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JToolBar.Separator jSeparator1;
    private javax.swing.JTextField jTextField1;
    private javax.swing.JToolBar jToolBar1;
    // End of variables declaration//GEN-END:variables
}
