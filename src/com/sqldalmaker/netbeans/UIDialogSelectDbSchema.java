/*
 * Copyright 2011-2018 sqldalmaker@gmail.com
 * SQL DAL Maker Website: http://sqldalmaker.sourceforge.net
 * Read LICENSE.txt in the root of this project/archive for details.
 */
package com.sqldalmaker.netbeans;

import com.sqldalmaker.common.InternalException;
import com.sqldalmaker.jaxb.settings.Settings;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
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
    
    private UIDialogSelectDbSchema(SdmDataObject obj, ISelectDbSchemaCallback callback, boolean dto, boolean fk) throws Exception {

        initComponents();

        this.obj = obj;
        this.callback = callback;

        jTable1.setTableHeader(null);

        // setContentPane(jPanel1);
        setModal(true);
        getRootPane().setDefaultButton(jButton1);

        Settings sett = NbpHelpers.load_settings(obj);
        setTitle(sett.getJdbc().getUrl());

        jButton1.addActionListener(new ActionListener() {
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

            this.jRadioButton3.setVisible(false);
            this.jRadioButton4.setVisible(false);

            jCheckBox3.setVisible(false);
        }

        if (fk) {

            jCheckBox1.setVisible(false);
            chk_views.setVisible(false);
        }

        refresh();
    }

    public static void open(SdmDataObject obj, ISelectDbSchemaCallback callback, boolean dto, boolean fk) throws Exception {

        UIDialogSelectDbSchema dlg = new UIDialogSelectDbSchema(obj, callback, dto, fk);
        dlg.setPreferredSize(new Dimension(520, 480));
        dlg.pack(); // after setPreferredSize
        dlg.setLocationRelativeTo(null);  // after pack!!!
        dlg.setVisible(true);
    }

    private int[] getSelection() throws InternalException {

        int rc = jTable1.getModel().getRowCount();

        if (rc == 1) {

            return new int[]{0};
        }

        int[] selectedRows = jTable1.getSelectedRows();

        if (selectedRows.length == 0) {

            throw new InternalException("Selection is empty.");
        }

        return selectedRows;
    }

    private void onOK() {

        try {

            selected_schema = null;

            if (jTable1.getRowCount() == 1) {

                selected_schema = (String) jTable1.getValueAt(0, 0);

            } else {

                if (jTable1.getRowCount() != 0) {

                    int[] indexes = getSelection();

                    if (indexes.length == 1) {

                        selected_schema = (String) jTable1.getValueAt(indexes[0], 0);

                    } else {

                        throw new InternalException("Selection is empty.");
                    }
                }
            }

            callback.process_ok(selected_schema, jCheckBox1.isSelected(), chk_views.isSelected(), jCheckBox2.isSelected(), jRadioButton4.isSelected(), jCheckBox3.isSelected());

            dispose();

        } catch (InternalException e) {

            NbpIdeMessageHelpers.show_error_in_ui_thread(e);
        }
    }

    private void onCancel() {
// add your code here if necessary
        dispose();
    }

    private void refresh() {

        try {

            final ArrayList<String> items = new ArrayList<String>();

            Connection con = NbpHelpers.get_connection(obj);

            try {

                DatabaseMetaData db_info = con.getMetaData();

                ResultSet rs;

//                if (enum_catalogs) {
//
//                    rs = db_info.getCatalogs();
//
//                } else {
                rs = db_info.getSchemas();
//                }

                try {

                    while (rs.next()) {

//                       if (enum_catalogs) {
//
//                           items.add(rs.getString("TABLE_CAT"));
//
//                       } else {
                        items.add(rs.getString("TABLE_SCHEM"));
//                       }
                    }

                } finally {

                    rs.close();
                }

            } finally {

                con.close();
            }

            // buttonOK.setEnabled(items.size() <= 1);
            jTable1.setModel(new AbstractTableModel() {

                @Override
                public Object getValueAt(int rowIndex, int columnIndex) {

                    return items.get(rowIndex);
                }

                @Override
                public int getColumnCount() {

                    return 1;
                }

                @Override
                public String getColumnName(int column) {

                    return "Catalog/Schema";
                }

                @Override
                public int getRowCount() {

                    return items.size();
                }
            });

            jTable1.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {

                    if (e.getClickCount() == 2) {

                        onOK();
                    }
                }
            });

        } catch (Exception e) {

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

        buttonGroup1 = new javax.swing.ButtonGroup();
        jPanel1 = new javax.swing.JPanel();
        jPanel4 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();
        jPanel3 = new javax.swing.JPanel();
        jPanel7 = new javax.swing.JPanel();
        jPanel2 = new javax.swing.JPanel();
        jCheckBox1 = new javax.swing.JCheckBox();
        chk_views = new javax.swing.JCheckBox();
        jCheckBox2 = new javax.swing.JCheckBox();
        jCheckBox3 = new javax.swing.JCheckBox();
        jPanel6 = new javax.swing.JPanel();
        jRadioButton3 = new javax.swing.JRadioButton();
        jRadioButton4 = new javax.swing.JRadioButton();
        jPanel5 = new javax.swing.JPanel();
        jButton1 = new javax.swing.JButton();
        jButton2 = new javax.swing.JButton();

        getContentPane().setLayout(new java.awt.BorderLayout(5, 5));

        jPanel1.setLayout(new java.awt.BorderLayout(5, 5));

        jPanel4.setLayout(new javax.swing.BoxLayout(jPanel4, javax.swing.BoxLayout.Y_AXIS));

        jLabel1.setFont(new java.awt.Font("Tahoma", 0, 18)); // NOI18N
        org.openide.awt.Mnemonics.setLocalizedText(jLabel1, org.openide.util.NbBundle.getMessage(UIDialogSelectDbSchema.class, "UIDialogSelectDbSchema.jLabel1.text")); // NOI18N
        jLabel1.setToolTipText(org.openide.util.NbBundle.getMessage(UIDialogSelectDbSchema.class, "UIDialogSelectDbSchema.jLabel1.toolTipText")); // NOI18N
        jPanel4.add(jLabel1);
        jLabel1.getAccessibleContext().setAccessibleName(org.openide.util.NbBundle.getMessage(UIDialogSelectDbSchema.class, "UIDialogSelectDbSchema.jLabel1.AccessibleContext.accessibleName")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jLabel2, org.openide.util.NbBundle.getMessage(UIDialogSelectDbSchema.class, "UIDialogSelectDbSchema.jLabel2.text")); // NOI18N
        jPanel4.add(jLabel2);

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

        getContentPane().add(jPanel1, java.awt.BorderLayout.CENTER);

        jPanel3.setLayout(new java.awt.BorderLayout());

        jPanel7.setLayout(new javax.swing.BoxLayout(jPanel7, javax.swing.BoxLayout.LINE_AXIS));

        jPanel2.setLayout(new javax.swing.BoxLayout(jPanel2, javax.swing.BoxLayout.Y_AXIS));

        jCheckBox1.setSelected(true);
        org.openide.awt.Mnemonics.setLocalizedText(jCheckBox1, org.openide.util.NbBundle.getMessage(UIDialogSelectDbSchema.class, "UIDialogSelectDbSchema.jCheckBox1.text")); // NOI18N
        jPanel2.add(jCheckBox1);

        chk_views.setSelected(true);
        org.openide.awt.Mnemonics.setLocalizedText(chk_views, org.openide.util.NbBundle.getMessage(UIDialogSelectDbSchema.class, "UIDialogSelectDbSchema.chk_views.text")); // NOI18N
        jPanel2.add(chk_views);

        jCheckBox2.setSelected(true);
        org.openide.awt.Mnemonics.setLocalizedText(jCheckBox2, org.openide.util.NbBundle.getMessage(UIDialogSelectDbSchema.class, "UIDialogSelectDbSchema.jCheckBox2.text")); // NOI18N
        jPanel2.add(jCheckBox2);

        jCheckBox3.setSelected(true);
        org.openide.awt.Mnemonics.setLocalizedText(jCheckBox3, org.openide.util.NbBundle.getMessage(UIDialogSelectDbSchema.class, "UIDialogSelectDbSchema.jCheckBox3.text")); // NOI18N
        jPanel2.add(jCheckBox3);

        jPanel7.add(jPanel2);

        jPanel6.setMinimumSize(new java.awt.Dimension(0, 0));
        jPanel6.setPreferredSize(new java.awt.Dimension(0, 0));
        jPanel6.setLayout(new javax.swing.BoxLayout(jPanel6, javax.swing.BoxLayout.LINE_AXIS));

        buttonGroup1.add(jRadioButton3);
        org.openide.awt.Mnemonics.setLocalizedText(jRadioButton3, org.openide.util.NbBundle.getMessage(UIDialogSelectDbSchema.class, "UIDialogSelectDbSchema.jRadioButton3.text")); // NOI18N
        jPanel6.add(jRadioButton3);

        buttonGroup1.add(jRadioButton4);
        org.openide.awt.Mnemonics.setLocalizedText(jRadioButton4, org.openide.util.NbBundle.getMessage(UIDialogSelectDbSchema.class, "UIDialogSelectDbSchema.jRadioButton4.text")); // NOI18N
        jPanel6.add(jRadioButton4);

        jPanel7.add(jPanel6);

        jPanel3.add(jPanel7, java.awt.BorderLayout.PAGE_START);

        jPanel5.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));

        org.openide.awt.Mnemonics.setLocalizedText(jButton1, org.openide.util.NbBundle.getMessage(UIDialogSelectDbSchema.class, "UIDialogSelectDbSchema.jButton1.text")); // NOI18N
        jPanel5.add(jButton1);

        org.openide.awt.Mnemonics.setLocalizedText(jButton2, org.openide.util.NbBundle.getMessage(UIDialogSelectDbSchema.class, "UIDialogSelectDbSchema.jButton2.text")); // NOI18N
        jPanel5.add(jButton2);

        jPanel3.add(jPanel5, java.awt.BorderLayout.CENTER);

        getContentPane().add(jPanel3, java.awt.BorderLayout.PAGE_END);
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.ButtonGroup buttonGroup1;
    private javax.swing.JCheckBox chk_views;
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JCheckBox jCheckBox1;
    private javax.swing.JCheckBox jCheckBox2;
    private javax.swing.JCheckBox jCheckBox3;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JRadioButton jRadioButton3;
    private javax.swing.JRadioButton jRadioButton4;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTable jTable1;
    // End of variables declaration//GEN-END:variables
}
