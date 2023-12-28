/*
    Copyright 2011-2023 sqldalmaker@gmail.com
    SQL DAL Maker Website: http://sqldalmaker.sourceforge.net
    Read LICENSE.txt in the root of this project/archive for details.
 */
package com.sqldalmaker.netbeans;

import com.sqldalmaker.cg.Helpers;
import com.sqldalmaker.cg.IDaoCG;
import com.sqldalmaker.cg.JaxbUtils;
import com.sqldalmaker.common.Const;
import com.sqldalmaker.common.FileSearchHelpers;
import com.sqldalmaker.common.InternalException;
import com.sqldalmaker.common.SdmUtils;
import com.sqldalmaker.common.XmlParser;
import com.sqldalmaker.jaxb.sdm.DaoClass;
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
import javax.swing.JOptionPane;
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
import org.openide.util.Lookup;
import org.openide.util.NbBundle.Messages;
import org.openide.util.RequestProcessor;
import org.openide.windows.TopComponent;

/*
 * 18.12.2023 03:01 1.292
 * 12.05.2023 23:01 1.283
 * 23.02.2023 15:42 1.279
 * 30.10.2022 08:03 1.266
 * 08.05.2021 22:29 1.200
 * 08.04.2021 22:08
 *
 */
@MultiViewElement.Registration(
        displayName = "#LBL_Sdm_DAO",
        iconBase = "com/sqldalmaker/netbeans/sqldalmaker.gif",
        mimeType = "text/sqldalmaker+dal",
        persistenceType = TopComponent.PERSISTENCE_NEVER,
        preferredID = "SdmTabDAO",
        position = 2000
)
@Messages("LBL_Sdm_DAO=DAO")
public final class SdmTabDAO extends SdmMultiViewCloneableEditor {

    private TableRowSorter<AbstractTableModel> sorter;
    private MyTableModel my_table_model;

    private JTable table;

    public SdmTabDAO(Lookup lookup) {
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
        createUIComponents();
        reload_table(true);
    }

    private void set_filter() {
        try {
            RowFilter<AbstractTableModel, Object> rf = RowFilter.regexFilter(jTextField1.getText(), 0);
            sorter.setRowFilter(rf);
        } catch (PatternSyntaxException e) {
            sorter.setRowFilter(null); // don't filter
        }
    }

    private void createUIComponents() {
        final ColorRenderer colorRenderer = new ColorRenderer();
        table = new JTable() {
            @Override
            public TableCellRenderer getCellRenderer(int row, int column) {
                if (column == 1) {
                    return colorRenderer;
                }
                return super.getCellRenderer(row, column);
            }
        };

        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        jScrollPane1.setViewportView(table);

        my_table_model = new MyTableModel();
        table.setModel(my_table_model);
        table.getTableHeader().setReorderingAllowed(false);

        sorter = new TableRowSorter<AbstractTableModel>(my_table_model);
        table.setRowSorter(sorter);

        table.addMouseListener(new MouseAdapter() {
            @Override
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

    private void open_xml() {
        try {
            final int[] selected_rows = get_selection();
            if (selected_rows.length == 0) {
                return;
            }
            int sel_row = selected_rows[0];
            List<DaoClass> jaxb_dao_classes = load_sdm_dao();
            if (!jaxb_dao_classes.isEmpty()) {
                FileObject this_doc_file = obj.getPrimaryFile();
                if (this_doc_file == null) {
                    return;
                }
                FileObject folder = this_doc_file.getParent();
                String dto_class_name = (String) table.getValueAt(sel_row, 0);
                XmlEditorUtil.goto_sdm_class_declaration_async(folder, dto_class_name);
            } else {
                String ref = (String) table.getValueAt(sel_row, 0) + ".xml";
                NbpIdeEditorHelpers.open_sdm_folder_file_async(obj, ref);
            }
        } catch (Exception ex) {
            // ex.printStackTrace();
            NbpIdeMessageHelpers.show_error_in_ui_thread(ex);
        }
    }

    private void open_generated_source_file() {
        try {
            int[] selected_rows = get_selection();
            Settings settings = NbpHelpers.load_settings(obj);
            String dao_class_name = (String) table.getValueAt(selected_rows[0], 0);
            NbpTargetLanguageHelpers.open_in_editor_async(obj, settings, dao_class_name, settings.getDao().getScope());
        } catch (Exception e) {
            // e.printStackTrace();
            NbpIdeMessageHelpers.show_error_in_ui_thread(e);
        }
    }

    private class ColorRenderer extends DefaultTableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            String sValue = (String) value;
            setText(sValue);
            // it seems like 'c' is always the same, 
            // so c.setForeground must be called in any case
            if (isSelected || Const.STATUS_OK.equals(sValue) || Const.STATUS_GENERATED.equals(sValue)) {
                TableCellRenderer r = table.getCellRenderer(row, column);
                Component c_0 = r.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, 0);
                c.setForeground(c_0.getForeground());
            } else {
                c.setForeground(Color.RED);
            }
            return c;
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
            return col == 0 ? "Class" : "State";
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

    private void reload_table() throws Exception {
        try {
            final ArrayList<String[]> list = my_table_model.getList();
            list.clear();
            List<DaoClass> jaxb_dao_classes = load_sdm_dao();
            if (!jaxb_dao_classes.isEmpty()) {
                for (DaoClass cls : jaxb_dao_classes) {
                    String[] item = new String[2];
                    item[0] = cls.getName();
                    list.add(item);
                }
            } else {
                FileSearchHelpers.IFileList file_list = new FileSearchHelpers.IFileList() {
                    @Override
                    public void add(String file_name) {
                        String[] item = new String[2];
                        try {
                            item[0] = file_name.replace(".xml", "");
                        } catch (Exception e) {
//                        e.printStackTrace();
                            item[0] = e.getMessage();
                        }
                        list.add(item);
                    }
                };
                FileObject folder = obj.getPrimaryFile().getParent();
                String xml_configs_folder_full_path = folder.getPath();
                FileSearchHelpers.enum_dao_xml_file_names(xml_configs_folder_full_path, file_list);
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
                // e.printStackTrace();
                NbpIdeMessageHelpers.show_error_in_ui_thread(e);
            }
        }
    }

    @Override
    public JComponent getToolbarRepresentation() {
        return jToolBar1;
    }

    private List<DaoClass> load_sdm_dao() throws Exception {
        String sdm_folder_abs_path = NbpPathHelpers.get_metaprogram_abs_path(obj);
        String sdm_xml_abs_path = Helpers.concat_path(sdm_folder_abs_path, Const.SDM_XML);
        String sdm_xsd_abs_path = Helpers.concat_path(sdm_folder_abs_path, Const.SDM_XSD);
        List<DaoClass> jaxb_dao_classes = SdmUtils.get_dao_classes(sdm_xml_abs_path, sdm_xsd_abs_path);
        return jaxb_dao_classes;
    }

    private void generate_async() {
        try {
            schedule_generate();
        } catch (Exception e) {
            // e.printStackTrace();
            NbpIdeMessageHelpers.show_error_in_ui_thread(e);
        }
    }

    private void schedule_generate() throws Exception {
        final Settings settings = NbpHelpers.load_settings(obj);
        final int[] selected_rows = get_selection();
        if (selected_rows.length == 0) {
            return;
        }
        for (int row : selected_rows) {
            my_table_model.setValueAt("", row, 1);
        }
        // myTableModel.refresh(); cleares selection
        update_table_async();
        // 1. open connection
        // 2. create the list of generated java
        // 3. close connection
        // 4. update the files from the list
        // http://stackoverflow.com/questions/22326822/netbeans-platform-progress-bar
        RequestProcessor RP = new RequestProcessor("Generate DAO classes RP");
        // final ProgressHandle ph = ProgressHandle.createHandle("Generate DAO class(es)");
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
                    // ph.progress("Connecting...");
                    Connection con = NbpHelpers.get_connection(obj); // !!! inside try/finally to ensure ph.finish()!!!
                    // ph.progress("Connected");
                    try {
                        StringBuilder output_dir = new StringBuilder();
                        IDaoCG gen = NbpTargetLanguageHelpers.create_dao_cg(con, obj, settings, output_dir);
                        List<DaoClass> jaxb_dao_classes = load_sdm_dao();
                        if (!jaxb_dao_classes.isEmpty()) {
                            generate_for_sdm_xml(ide_log, gen, output_dir, jaxb_dao_classes, selected_rows);
                        } else {
                            generate_for_dao_xml(ide_log, gen, output_dir, selected_rows);
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
//                    ide_log.add_debug_message("DONE");
                    // ph.finish();
                }
            }

            private void generate_for_dao_xml(
                    NbpIdeConsoleUtil ide_log,
                    IDaoCG gen,
                    StringBuilder output_dir,
                    int[] selected_rows) throws Exception {

                String sdm_folder_abs_path = NbpPathHelpers.get_metaprogram_abs_path(obj);
                String output_dir_rel_path = output_dir.toString();
                String context_path = DaoClass.class.getPackage().getName();
                XmlParser xml_parser = new XmlParser(context_path, Helpers.concat_path(sdm_folder_abs_path, Const.DAO_XSD));
                for (int row : selected_rows) {
                    String dao_class_name = (String) table.getValueAt(row, 0);
                    try {
                        // ph.progress(daoXmlRelPath);
                        String dao_xml_abs_path = Helpers.concat_path(sdm_folder_abs_path, dao_class_name) + ".xml";
                        DaoClass dao_class = xml_parser.unmarshal(dao_xml_abs_path);
                        String[] file_content = gen.translate(dao_class_name, dao_class);
                        String file_name = NbpTargetLanguageHelpers.get_target_file_name(obj, dao_class_name);
                        NbpHelpers.save_text_to_file(obj, output_dir_rel_path, file_name, file_content[0]);
                        ide_log.add_success_message(dao_class_name + " -> " + Const.STATUS_GENERATED);
                        my_table_model.setValueAt(Const.STATUS_GENERATED, row, 1);
                    } catch (Exception ex) {
                        String msg = ex.getMessage();
                        if (msg == null) {
                            msg = "???";
                        }
                        msg = ex.getClass().getName() + ": " + dao_class_name + " -> " + msg;
                        my_table_model.setValueAt(msg, row, 1);
                        ide_log.add_error_message(msg);
                    }
                    update_table_async();
                }
            }

            private void generate_for_sdm_xml(
                    NbpIdeConsoleUtil ide_log,
                    IDaoCG gen,
                    StringBuilder output_dir,
                    List<DaoClass> jaxb_dao_classes,
                    int[] selected_rows) {

                String output_dir_rel_path = output_dir.toString();
                for (int row : selected_rows) {
                    String dao_class_name = (String) table.getValueAt(row, 0);
                    try {
                        // ph.progress(daoXmlRelPath);
                        DaoClass dao_class = JaxbUtils.find_jaxb_dao_class(dao_class_name, jaxb_dao_classes);
                        String[] file_content = gen.translate(dao_class_name, dao_class);
                        String file_name = NbpTargetLanguageHelpers.get_target_file_name(obj, dao_class_name);
                        NbpHelpers.save_text_to_file(obj, output_dir_rel_path, file_name, file_content[0]);
                        ide_log.add_success_message(dao_class_name + " -> " + Const.STATUS_GENERATED);
                        my_table_model.setValueAt(Const.STATUS_GENERATED, row, 1);
                    } catch (Exception ex) {
                        String msg = ex.getMessage();
                        if (msg == null) {
                            msg = "???";
                        }
                        msg = ex.getClass().getName() + ": " + dao_class_name + " -> " + msg;
                        my_table_model.setValueAt(msg, row, 1);
                        ide_log.add_error_message(msg);
                    }
                    update_table_async();
                }
            }
        });
        ///////////////////////////////////////////
        task.schedule(0);
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
        RequestProcessor RP = new RequestProcessor("Validate all DAO classes RP");
        // final ProgressHandle ph = ProgressHandle.createHandle("Validate DAO classes");
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
                        final StringBuilder output_dir = new StringBuilder();
                        IDaoCG gen = NbpTargetLanguageHelpers.create_dao_cg(con, obj, settings, output_dir);
                        List<DaoClass> jaxb_dao_classes = load_sdm_dao();
                        if (!jaxb_dao_classes.isEmpty()) {
                            validate_for_sdm_xml(ide_log, gen, jaxb_dao_classes);
                        } else {
                            validate_for_dao_xml(ide_log, gen);
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
//                    ide_log.add_debug_message("DONE");
                    // ph.finish();
                }
            }

            private void validate_for_dao_xml(
                    NbpIdeConsoleUtil ide_log,
                    IDaoCG gen) throws Exception {

                String sdm_folder_abs_path = NbpPathHelpers.get_metaprogram_abs_path(obj);
                String context_path = DaoClass.class.getPackage().getName();
                XmlParser xml_Parser = new XmlParser(context_path, Helpers.concat_path(sdm_folder_abs_path, Const.DAO_XSD));
                // === panedrone: don't ask it in loop because of
                // "Cannot read the array length because "<local3>" is null"
                int rc = my_table_model.getRowCount();
                for (int i = 0; i < rc; i++) {
                    String dao_class_name = (String) table.getValueAt(i, 0);
                    try {
                        // ph.progress(daoXmlRelPath);
                        String dao_xml_abs_path = Helpers.concat_path(sdm_folder_abs_path, dao_class_name) + ".xml";
                        DaoClass dao_class = xml_Parser.unmarshal(dao_xml_abs_path);
                        String[] file_content = gen.translate(dao_class_name, dao_class);
                        StringBuilder validation_buff = new StringBuilder();
                        NbpTargetLanguageHelpers.validate_dao(obj, settings, dao_class_name, file_content, validation_buff);
                        String status = validation_buff.toString();
                        if (status.length() == 0) {
                            my_table_model.setValueAt(Const.STATUS_OK, i, 1);
                            ide_log.add_success_message(dao_class_name + " -> " + Const.STATUS_OK);
                        } else {
                            my_table_model.setValueAt(status, i, 1);
                            ide_log.add_error_message(dao_class_name + " -> " + status);
                        }
                    } catch (Exception ex) {
                        String msg = ex.getMessage();
                        if (msg == null) {
                            msg = "???";
                        }
                        msg = ex.getClass().getName() + ": " + dao_class_name + " -> " + msg;
                        my_table_model.setValueAt(msg, i, 1);
                        ide_log.add_error_message(msg);
                    }
                    update_table_async();
                }
            }

            private void validate_for_sdm_xml(
                    NbpIdeConsoleUtil ide_log,
                    IDaoCG gen,
                    List<DaoClass> jaxb_dao_classes) {

                int rc = my_table_model.getRowCount();
                for (int i = 0; i < rc; i++) {
                    String dao_class_name = (String) table.getValueAt(i, 0);
                    try {
                        // ph.progress(daoXmlRelPath);
                        DaoClass dao_class = JaxbUtils.find_jaxb_dao_class(dao_class_name, jaxb_dao_classes);
                        String[] file_content = gen.translate(dao_class_name, dao_class);
                        StringBuilder validation_buff = new StringBuilder();
                        NbpTargetLanguageHelpers.validate_dao(obj, settings, dao_class_name, file_content, validation_buff);
                        String status = validation_buff.toString();
                        if (status.length() == 0) {
                            my_table_model.setValueAt(Const.STATUS_OK, i, 1);
                            ide_log.add_success_message(dao_class_name + " -> " + Const.STATUS_OK);
                        } else {
                            my_table_model.setValueAt(status, i, 1);
                            ide_log.add_error_message(dao_class_name + " -> " + status);
                        }
                    } catch (Exception ex) {
                        String msg = ex.getMessage();
                        if (msg == null) {
                            msg = "???";
                        }
                        msg = ex.getClass().getName() + ": " + dao_class_name + " -> " + msg;
                        my_table_model.setValueAt(msg, i, 1);
                        ide_log.add_error_message(msg);
                    }
                    update_table_async();
                }
            }
        });
        ////////////////////////////////////////////////////
        task.schedule(0);
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

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jToolBar1 = new javax.swing.JToolBar();
        jSeparator1 = new javax.swing.JToolBar.Separator();
        jButton4 = new javax.swing.JButton();
        jButton1 = new javax.swing.JButton();
        jButton3 = new javax.swing.JButton();
        jButton5 = new javax.swing.JButton();
        jButton11 = new javax.swing.JButton();
        jButton6 = new javax.swing.JButton();
        jButton9 = new javax.swing.JButton();
        jButton10 = new javax.swing.JButton();
        jPanel1 = new javax.swing.JPanel();
        jTextField1 = new javax.swing.JTextField();
        jScrollPane1 = new javax.swing.JScrollPane();

        setLayout(new java.awt.BorderLayout());

        jToolBar1.setRollover(true);
        jToolBar1.add(jSeparator1);

        jButton4.setIcon(new javax.swing.ImageIcon(getClass().getResource("/img/new_xml.gif"))); // NOI18N
        org.openide.awt.Mnemonics.setLocalizedText(jButton4, org.openide.util.NbBundle.getMessage(SdmTabDAO.class, "SdmTabDAO.jButton4.text")); // NOI18N
        jButton4.setToolTipText(org.openide.util.NbBundle.getMessage(SdmTabDAO.class, "SdmTabDAO.jButton4.toolTipText")); // NOI18N
        jButton4.setFocusable(false);
        jButton4.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jButton4.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jButton4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton4ActionPerformed(evt);
            }
        });
        jToolBar1.add(jButton4);

        jButton1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/img/xmldoc.gif"))); // NOI18N
        org.openide.awt.Mnemonics.setLocalizedText(jButton1, org.openide.util.NbBundle.getMessage(SdmTabDAO.class, "SdmTabDAO.jButton1.text")); // NOI18N
        jButton1.setToolTipText(org.openide.util.NbBundle.getMessage(SdmTabDAO.class, "SdmTabDAO.jButton1.toolTipText")); // NOI18N
        jButton1.setFocusable(false);
        jButton1.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jButton1.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });
        jToolBar1.add(jButton1);

        jButton3.setIcon(new javax.swing.ImageIcon(getClass().getResource("/img/GeneratedFile.gif"))); // NOI18N
        org.openide.awt.Mnemonics.setLocalizedText(jButton3, org.openide.util.NbBundle.getMessage(SdmTabDAO.class, "SdmTabDAO.jButton3.text")); // NOI18N
        jButton3.setToolTipText(org.openide.util.NbBundle.getMessage(SdmTabDAO.class, "SdmTabDAO.jButton3.toolTipText")); // NOI18N
        jButton3.setFocusable(false);
        jButton3.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jButton3.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jButton3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton3ActionPerformed(evt);
            }
        });
        jToolBar1.add(jButton3);

        jButton5.setIcon(new javax.swing.ImageIcon(getClass().getResource("/img/180.png"))); // NOI18N
        org.openide.awt.Mnemonics.setLocalizedText(jButton5, org.openide.util.NbBundle.getMessage(SdmTabDAO.class, "SdmTabDAO.jButton5.text")); // NOI18N
        jButton5.setToolTipText(org.openide.util.NbBundle.getMessage(SdmTabDAO.class, "SdmTabDAO.jButton5.toolTipText")); // NOI18N
        jButton5.setFocusable(false);
        jButton5.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jButton5.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jButton5.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton5ActionPerformed(evt);
            }
        });
        jToolBar1.add(jButton5);

        jButton11.setIcon(new javax.swing.ImageIcon(getClass().getResource("/img/FK.gif"))); // NOI18N
        org.openide.awt.Mnemonics.setLocalizedText(jButton11, org.openide.util.NbBundle.getMessage(SdmTabDAO.class, "SdmTabDAO.jButton11.text")); // NOI18N
        jButton11.setToolTipText(org.openide.util.NbBundle.getMessage(SdmTabDAO.class, "SdmTabDAO.jButton11.toolTipText")); // NOI18N
        jButton11.setFocusable(false);
        jButton11.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jButton11.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jButton11.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton11ActionPerformed(evt);
            }
        });
        jToolBar1.add(jButton11);

        jButton6.setIcon(new javax.swing.ImageIcon(getClass().getResource("/img/arrow-circle-double-135.png"))); // NOI18N
        org.openide.awt.Mnemonics.setLocalizedText(jButton6, org.openide.util.NbBundle.getMessage(SdmTabDAO.class, "SdmTabDAO.jButton6.text")); // NOI18N
        jButton6.setToolTipText(org.openide.util.NbBundle.getMessage(SdmTabDAO.class, "SdmTabDAO.jButton6.toolTipText")); // NOI18N
        jButton6.setFocusable(false);
        jButton6.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jButton6.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jButton6.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton6ActionPerformed(evt);
            }
        });
        jToolBar1.add(jButton6);

        jButton9.setIcon(new javax.swing.ImageIcon(getClass().getResource("/img/compile-warning.png"))); // NOI18N
        org.openide.awt.Mnemonics.setLocalizedText(jButton9, org.openide.util.NbBundle.getMessage(SdmTabDAO.class, "SdmTabDAO.jButton9.text")); // NOI18N
        jButton9.setToolTipText(org.openide.util.NbBundle.getMessage(SdmTabDAO.class, "SdmTabDAO.jButton9.toolTipText")); // NOI18N
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
        org.openide.awt.Mnemonics.setLocalizedText(jButton10, org.openide.util.NbBundle.getMessage(SdmTabDAO.class, "SdmTabDAO.jButton10.text")); // NOI18N
        jButton10.setToolTipText(org.openide.util.NbBundle.getMessage(SdmTabDAO.class, "SdmTabDAO.jButton10.toolTipText")); // NOI18N
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

        jTextField1.setText(org.openide.util.NbBundle.getMessage(SdmTabDAO.class, "SdmTabDAO.jTextField1.text")); // NOI18N
        jPanel1.add(jTextField1, java.awt.BorderLayout.PAGE_START);
        jPanel1.add(jScrollPane1, java.awt.BorderLayout.CENTER);

        add(jPanel1, java.awt.BorderLayout.CENTER);
    }// </editor-fold>//GEN-END:initComponents

    private void jButton6ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton6ActionPerformed
        reload_table(true);
    }//GEN-LAST:event_jButton6ActionPerformed

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        open_xml();
    }//GEN-LAST:event_jButton1ActionPerformed

    private void jButton3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton3ActionPerformed
        open_generated_source_file();
    }//GEN-LAST:event_jButton3ActionPerformed

    private void jButton4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton4ActionPerformed
        String name = JOptionPane.showInputDialog(this, "DAO XML file name:");
        if (name != null) {
            if (name.endsWith(".xml") == false) {
                name += ".xml";
            }
            NbpIdeEditorHelpers.create_dao_xml(obj, name);
        }
    }//GEN-LAST:event_jButton4ActionPerformed

    private void jButton5ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton5ActionPerformed

        try {
            NbpCrudXmHelpers.get_crud_dao_xml(obj);
        } catch (Exception ex) {
            // ex.printStackTrace();
            NbpIdeMessageHelpers.show_error_in_ui_thread(ex);
        }
    }//GEN-LAST:event_jButton5ActionPerformed

    private void jButton10ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton10ActionPerformed
        validate_all_async();
    }//GEN-LAST:event_jButton10ActionPerformed

    private void jButton9ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton9ActionPerformed
        generate_async();
    }//GEN-LAST:event_jButton9ActionPerformed

    private void jButton11ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton11ActionPerformed
        try {
            NbpCrudXmHelpers.get_fk_access_xml(obj);
        } catch (Exception ex) {
            // ex.printStackTrace();
            NbpIdeMessageHelpers.show_error_in_ui_thread(ex);
        }
    }//GEN-LAST:event_jButton11ActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton10;
    private javax.swing.JButton jButton11;
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
