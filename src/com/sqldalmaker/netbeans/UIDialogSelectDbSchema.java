/*
 * Copyright 2011-2019 sqldalmaker@gmail.com
 * SQL DAL Maker Website: http://sqldalmaker.sourceforge.net
 * Read LICENSE.txt in the root of this project/archive for details.
 *
 */
package com.sqldalmaker.netbeans;

import com.sqldalmaker.jaxb.settings.Settings;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.ArrayList;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.KeyStroke;
import javax.swing.table.AbstractTableModel;
import com.sqldalmaker.common.ISelectDbSchemaCallback;
import java.util.List;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

/**
 *
 * @author sqldalmaker@gmail.com
 *
 */
public final class UIDialogSelectDbSchema extends JDialog {

    private static final long serialVersionUID = 1L;

    private final SdmDataObject obj;

    private final ISelectDbSchemaCallback callback;

    private String selected_schema;

    private final Settings settings;

    private final List<String> schemas = new ArrayList<String>();

    private UIDialogSelectDbSchema() {
        obj = null;
        callback = null;
        settings = null;
    }

    private UIDialogSelectDbSchema(SdmDataObject obj, ISelectDbSchemaCallback callback, boolean dto, boolean fk) throws Exception {

        initComponents();

        this.obj = obj;
        this.callback = callback;
        this.settings = NbpHelpers.load_settings(obj);

        jTable1.setTableHeader(null);

        // setContentPane(jPanel1);
        setModal(true);
        getRootPane().setDefaultButton(button_ok);

        setTitle("Select schema and provide options");

        lbl_jdbc_url.setText(settings.getJdbc().getUrl());

        button_ok.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onOK();
            }
        });

        jButton2.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        });

// call onCancel() when cross is clicked
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

// call onCancel() on ESCAPE
        jPanel1.registerKeyboardAction(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {

                onCancel();
            }

        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        if (dto || fk) {

            this.radio_crud.setVisible(false);
            this.radio_crud_auto.setVisible(false);

            chk_add_fk_access.setVisible(false);
        }

        if (fk) {

            chk_omit_used.setVisible(false);
            chk_including_views.setVisible(false);
        }

        jTable1.setModel(new AbstractTableModel() { // before refresh_schemas();

            @Override
            public Object getValueAt(int rowIndex, int columnIndex) {

                return schemas.get(rowIndex);
            }

            @Override
            public int getColumnCount() {

                return 1;
            }

            @Override
            public String getColumnName(int column) {

                return "Schema";
            }

            @Override
            public int getRowCount() {

                return schemas.size();
            }
        });

        refresh_schemas();

//        // sometime it works wrong...
//        //
//        jTable1.addMouseListener(new MouseAdapter() {
//            public void mouseClicked(MouseEvent e) {
//                if (e.getClickCount() == 1) {
//
//                    on_selection_changed();
//                }
//            }
//        });
//
        jTable1.getSelectionModel().addListSelectionListener(new ListSelectionListener() {

            // https://stackoverflow.com/questions/375265/jtable-selection-change-event-handling-find-the-source-table-dynamically
            //
            @Override
            public void valueChanged(ListSelectionEvent lse) {

                on_selection_changed();

//                if (!lse.getValueIsAdjusting()) {
//                    System.out.println("Selection Changed");
//                }
            }
        });
    }

    public static void open(SdmDataObject obj, ISelectDbSchemaCallback callback, boolean dto, boolean fk) throws Exception {

        UIDialogSelectDbSchema dlg = new UIDialogSelectDbSchema(obj, callback, dto, fk);
        dlg.setPreferredSize(new Dimension(520, 480));
        dlg.pack(); // after setPreferredSize
        dlg.setLocationRelativeTo(null);  // after pack!!!
        dlg.setVisible(true);
    }

    private int[] getSelection() {

        int rc = jTable1.getModel().getRowCount();

        if (rc == 1) {

            return new int[]{0};
        }

        int[] selected_rows = jTable1.getSelectedRows();

        return selected_rows;
    }

    private void onOK() {

        callback.process_ok(chk_schema_in_xml.isSelected(), selected_schema,
                chk_omit_used.isSelected(), chk_including_views.isSelected(), chk_singular.isSelected(),
                radio_crud_auto.isSelected(), chk_add_fk_access.isSelected());

        dispose();
    }

    private void onCancel() {

        dispose();
    }

    private void refresh_schemas() {

        try {

            schemas.clear();

            Connection con = NbpHelpers.get_connection(obj);

            try {

                DatabaseMetaData db_info = con.getMetaData();

                ResultSet rs;

                rs = db_info.getSchemas();

                try {

                    while (rs.next()) {

                        schemas.add(rs.getString("TABLE_SCHEM"));
                    }

                } finally {

                    rs.close();
                }

            } finally {

                con.close();
            }

            if (schemas.size() == 0) {

                lbl_hint.setText("This database doesn't have schemas. Just provide options.");
                radio_selected_schema.setText("Without schema");

            } else {

                lbl_hint.setText("Select schema or click 'DB user name as schema'. Provide options.");
                radio_selected_schema.setText("Use selected schema");
                chk_schema_in_xml.setSelected(true);
            }

            on_selection_changed();

        } catch (Exception e) {

            NbpIdeMessageHelpers.show_error_in_ui_thread(e);
        }
    }

    private void on_selection_changed() {

        boolean enabled = false;

        if (radio_user_as_schema.isSelected()) {

            enabled = true;

            chk_schema_in_xml.setEnabled(true);

            String user = settings.getJdbc().getUser();

            selected_schema = user;

        } else if (radio_selected_schema.isSelected()) {

            if (jTable1.getRowCount() == 0) {

                chk_schema_in_xml.setSelected(false);
                chk_schema_in_xml.setEnabled(false);

                enabled = true;

                selected_schema = null;

            } else {

                chk_schema_in_xml.setEnabled(true);

                if (jTable1.getRowCount() == 1) {

                    enabled = true;

                    selected_schema = null;

                } else {

                    int[] indexes = getSelection();

                    enabled = indexes.length == 1;

                    if (enabled) {

                        selected_schema = (String) jTable1.getModel().getValueAt(indexes[0], 0);
                    }
                }
            }
        }

        button_ok.setEnabled(enabled);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        buttonGroup1 = new javax.swing.ButtonGroup();
        buttonGroup2 = new javax.swing.ButtonGroup();
        jPanel1 = new javax.swing.JPanel();
        jPanel4 = new javax.swing.JPanel();
        lbl_jdbc_url = new javax.swing.JLabel();
        lbl_hint = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();
        jPanel2 = new javax.swing.JPanel();
        jPanel7 = new javax.swing.JPanel();
        radio_selected_schema = new javax.swing.JRadioButton();
        radio_user_as_schema = new javax.swing.JRadioButton();
        chk_schema_in_xml = new javax.swing.JCheckBox();
        chk_omit_used = new javax.swing.JCheckBox();
        chk_including_views = new javax.swing.JCheckBox();
        chk_singular = new javax.swing.JCheckBox();
        chk_add_fk_access = new javax.swing.JCheckBox();
        jPanel6 = new javax.swing.JPanel();
        radio_crud = new javax.swing.JRadioButton();
        radio_crud_auto = new javax.swing.JRadioButton();
        jPanel5 = new javax.swing.JPanel();
        button_ok = new javax.swing.JButton();
        jButton2 = new javax.swing.JButton();

        setMinimumSize(new java.awt.Dimension(600, 600));
        setPreferredSize(new java.awt.Dimension(600, 600));

        jPanel1.setLayout(new java.awt.BorderLayout());

        jPanel4.setLayout(new java.awt.GridLayout(2, 0));

        lbl_jdbc_url.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        lbl_jdbc_url.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        org.openide.awt.Mnemonics.setLocalizedText(lbl_jdbc_url, org.openide.util.NbBundle.getMessage(UIDialogSelectDbSchema.class, "UIDialogSelectDbSchema.lbl_jdbc_url.text")); // NOI18N
        lbl_jdbc_url.setBorder(javax.swing.BorderFactory.createEmptyBorder(5, 5, 5, 5));
        jPanel4.add(lbl_jdbc_url);

        lbl_hint.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        org.openide.awt.Mnemonics.setLocalizedText(lbl_hint, org.openide.util.NbBundle.getMessage(UIDialogSelectDbSchema.class, "UIDialogSelectDbSchema.lbl_hint.text")); // NOI18N
        lbl_hint.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 5, 5, 5));
        jPanel4.add(lbl_hint);

        jPanel1.add(jPanel4, java.awt.BorderLayout.PAGE_START);

        jScrollPane1.setPreferredSize(new java.awt.Dimension(0, 0));

        jTable1.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Title 1", "Title 2", "Title 3", "Title 4"
            }
        ));
        jTable1.setMinimumSize(new java.awt.Dimension(0, 0));
        jScrollPane1.setViewportView(jTable1);

        jPanel1.add(jScrollPane1, java.awt.BorderLayout.CENTER);

        jPanel2.setLayout(new java.awt.GridLayout(8, 1));

        jPanel7.setMinimumSize(new java.awt.Dimension(10, 100));
        jPanel7.setPreferredSize(new java.awt.Dimension(0, 0));
        jPanel7.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        buttonGroup2.add(radio_selected_schema);
        radio_selected_schema.setSelected(true);
        org.openide.awt.Mnemonics.setLocalizedText(radio_selected_schema, org.openide.util.NbBundle.getMessage(UIDialogSelectDbSchema.class, "UIDialogSelectDbSchema.radio_selected_schema.text")); // NOI18N
        radio_selected_schema.setMargin(new java.awt.Insets(5, 0, 0, 0));
        radio_selected_schema.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                radio_selected_schemaActionPerformed(evt);
            }
        });
        jPanel7.add(radio_selected_schema);

        buttonGroup2.add(radio_user_as_schema);
        org.openide.awt.Mnemonics.setLocalizedText(radio_user_as_schema, org.openide.util.NbBundle.getMessage(UIDialogSelectDbSchema.class, "UIDialogSelectDbSchema.radio_user_as_schema.text")); // NOI18N
        radio_user_as_schema.setMargin(new java.awt.Insets(5, 0, 0, 0));
        radio_user_as_schema.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                radio_user_as_schemaActionPerformed(evt);
            }
        });
        jPanel7.add(radio_user_as_schema);

        jPanel2.add(jPanel7);

        org.openide.awt.Mnemonics.setLocalizedText(chk_schema_in_xml, org.openide.util.NbBundle.getMessage(UIDialogSelectDbSchema.class, "UIDialogSelectDbSchema.chk_schema_in_xml.text")); // NOI18N
        chk_schema_in_xml.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        chk_schema_in_xml.setMargin(new java.awt.Insets(0, 5, 0, 0));
        jPanel2.add(chk_schema_in_xml);

        chk_omit_used.setSelected(true);
        org.openide.awt.Mnemonics.setLocalizedText(chk_omit_used, org.openide.util.NbBundle.getMessage(UIDialogSelectDbSchema.class, "UIDialogSelectDbSchema.chk_omit_used.text")); // NOI18N
        chk_omit_used.setActionCommand(org.openide.util.NbBundle.getMessage(UIDialogSelectDbSchema.class, "UIDialogSelectDbSchema.chk_omit_used.actionCommand")); // NOI18N
        chk_omit_used.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        chk_omit_used.setMargin(new java.awt.Insets(0, 5, 0, 0));
        jPanel2.add(chk_omit_used);

        chk_including_views.setSelected(true);
        org.openide.awt.Mnemonics.setLocalizedText(chk_including_views, org.openide.util.NbBundle.getMessage(UIDialogSelectDbSchema.class, "UIDialogSelectDbSchema.chk_including_views.text")); // NOI18N
        chk_including_views.setMargin(new java.awt.Insets(0, 5, 0, 0));
        jPanel2.add(chk_including_views);

        chk_singular.setSelected(true);
        org.openide.awt.Mnemonics.setLocalizedText(chk_singular, org.openide.util.NbBundle.getMessage(UIDialogSelectDbSchema.class, "UIDialogSelectDbSchema.chk_singular.text")); // NOI18N
        chk_singular.setMargin(new java.awt.Insets(0, 5, 0, 0));
        jPanel2.add(chk_singular);

        chk_add_fk_access.setSelected(true);
        org.openide.awt.Mnemonics.setLocalizedText(chk_add_fk_access, org.openide.util.NbBundle.getMessage(UIDialogSelectDbSchema.class, "UIDialogSelectDbSchema.chk_add_fk_access.text")); // NOI18N
        chk_add_fk_access.setMargin(new java.awt.Insets(0, 5, 0, 0));
        jPanel2.add(chk_add_fk_access);

        jPanel6.setMinimumSize(new java.awt.Dimension(10, 100));
        jPanel6.setPreferredSize(new java.awt.Dimension(0, 0));
        jPanel6.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        buttonGroup1.add(radio_crud);
        radio_crud.setSelected(true);
        org.openide.awt.Mnemonics.setLocalizedText(radio_crud, org.openide.util.NbBundle.getMessage(UIDialogSelectDbSchema.class, "UIDialogSelectDbSchema.radio_crud.text")); // NOI18N
        radio_crud.setMargin(new java.awt.Insets(5, 0, 0, 0));
        jPanel6.add(radio_crud);

        buttonGroup1.add(radio_crud_auto);
        org.openide.awt.Mnemonics.setLocalizedText(radio_crud_auto, org.openide.util.NbBundle.getMessage(UIDialogSelectDbSchema.class, "UIDialogSelectDbSchema.radio_crud_auto.text")); // NOI18N
        radio_crud_auto.setMargin(new java.awt.Insets(5, 0, 0, 0));
        jPanel6.add(radio_crud_auto);

        jPanel2.add(jPanel6);

        jPanel5.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));

        org.openide.awt.Mnemonics.setLocalizedText(button_ok, org.openide.util.NbBundle.getMessage(UIDialogSelectDbSchema.class, "UIDialogSelectDbSchema.button_ok.text")); // NOI18N
        jPanel5.add(button_ok);

        org.openide.awt.Mnemonics.setLocalizedText(jButton2, org.openide.util.NbBundle.getMessage(UIDialogSelectDbSchema.class, "UIDialogSelectDbSchema.jButton2.text")); // NOI18N
        jPanel5.add(jButton2);

        jPanel2.add(jPanel5);

        jPanel1.add(jPanel2, java.awt.BorderLayout.PAGE_END);

        getContentPane().add(jPanel1, java.awt.BorderLayout.CENTER);
    }// </editor-fold>//GEN-END:initComponents

    private void radio_selected_schemaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_radio_selected_schemaActionPerformed
        on_selection_changed();
    }//GEN-LAST:event_radio_selected_schemaActionPerformed

    private void radio_user_as_schemaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_radio_user_as_schemaActionPerformed
        on_selection_changed();
    }//GEN-LAST:event_radio_user_as_schemaActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.ButtonGroup buttonGroup1;
    private javax.swing.ButtonGroup buttonGroup2;
    private javax.swing.JButton button_ok;
    private javax.swing.JCheckBox chk_add_fk_access;
    private javax.swing.JCheckBox chk_including_views;
    private javax.swing.JCheckBox chk_omit_used;
    private javax.swing.JCheckBox chk_schema_in_xml;
    private javax.swing.JCheckBox chk_singular;
    private javax.swing.JButton jButton2;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTable jTable1;
    private javax.swing.JLabel lbl_hint;
    private javax.swing.JLabel lbl_jdbc_url;
    private javax.swing.JRadioButton radio_crud;
    private javax.swing.JRadioButton radio_crud_auto;
    private javax.swing.JRadioButton radio_selected_schema;
    private javax.swing.JRadioButton radio_user_as_schema;
    // End of variables declaration//GEN-END:variables
}
