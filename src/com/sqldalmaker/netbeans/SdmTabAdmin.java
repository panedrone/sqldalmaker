/*
 * Copyright 2011-2018 sqldalmaker@gmail.com
 * SQL DAL Maker Website: http://sqldalmaker.sourceforge.net
 * Read LICENSE.txt in the root of this project/archive for details.
 */
package com.sqldalmaker.netbeans;

import com.sqldalmaker.common.Const;
import java.awt.Desktop;
import java.sql.Connection;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JToolBar;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import org.netbeans.core.spi.multiview.MultiViewElement;
import org.openide.modules.ModuleInfo;
import org.openide.modules.Modules;
import org.openide.util.Lookup;
import org.openide.util.NbBundle.Messages;
import org.openide.windows.TopComponent;

/**
 *
 * @author sqldalmaker@gmail.com
 *
 */
@MultiViewElement.Registration(
        displayName = "#LBL_Sdm_Admin",
        iconBase = "com/sqldalmaker/netbeans/sqldalmaker.gif",
        mimeType = "text/sqldalmaker+dal",
        persistenceType = TopComponent.PERSISTENCE_NEVER,
        preferredID = "SdmTabAdmin",
        position = 3000
)
@Messages("LBL_Sdm_Admin=Admin")
public final class SdmTabAdmin extends SdmMultiViewCloneableEditor {

    private final JToolBar toolBar = new JToolBar();

    public SdmTabAdmin(Lookup lookup) {

        super(lookup);
        initComponents();

        ModuleInfo m = Modules.getDefault().ownerOf(this.getClass());

        String v = m.getSpecificationVersion().toString();

        String jv = System.getProperty("java.version");

        jTextField1.setText("v. " + v + " on Java " + jv);

        jTextField1.setBorder(javax.swing.BorderFactory.createEmptyBorder());

        ////////////////////////////////////////
        
        jTextPane1.setEditable(false);
        jTextPane1.setContentType("text/html");

        try {

            String text = NbpHelpers.read_from_jar_file("", "ABOUT.html");

            jTextPane1.setText(text);

        } catch (Exception ex) {
            ex.printStackTrace();
        }

        jTextPane1.addHyperlinkListener(new HyperlinkListener() {
            
            @Override
            public void hyperlinkUpdate(HyperlinkEvent hle) {
                if (HyperlinkEvent.EventType.ACTIVATED.equals(hle.getEventType())) {
                    // System.out.println(hle.getURL());
                    if (Desktop.isDesktopSupported()) {
                        try {
                            Desktop desktop = Desktop.getDesktop();
                            desktop.browse(hle.getURL().toURI());
                        } catch (Throwable ex) {
                            ex.printStackTrace();
                        }
                    }
                }
            }
        });

        ///////////////////////////////////////////////////////////////////
        //
        // https://stackoverflow.com/questions/291115/java-swing-using-jscrollpane-and-having-it-scroll-back-to-top
        //
        jScrollPane1.getVerticalScrollBar().setValue(0);

        ///////////////////////////////////////////////////////////////////
        //
        // https://stackoverflow.com/questions/5583495/how-do-i-speed-up-the-scroll-speed-in-a-jscrollpane-when-using-the-mouse-wheel
        //
        jScrollPane1.getVerticalScrollBar().setUnitIncrement(20);
    }

    @Override
    public JComponent getToolbarRepresentation() {
        return toolBar;
    }

    private void editSettingsXml() {
        NbpIdeEditorHelpers.open_metaprogram_file_async(obj, Const.SETTINGS_XML);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jScrollPane1 = new javax.swing.JScrollPane();
        jPanel1 = new javax.swing.JPanel();
        jPanel6 = new javax.swing.JPanel();
        jPanel3 = new javax.swing.JPanel();
        jButton5 = new javax.swing.JButton();
        jButton25 = new javax.swing.JButton();
        jButton6 = new javax.swing.JButton();
        jButton13 = new javax.swing.JButton();
        jTextField1 = new javax.swing.JTextField();
        jPanel4 = new javax.swing.JPanel();
        jButton1 = new javax.swing.JButton();
        jButton2 = new javax.swing.JButton();
        jButton3 = new javax.swing.JButton();
        jPanel5 = new javax.swing.JPanel();
        jButton10 = new javax.swing.JButton();
        jButton11 = new javax.swing.JButton();
        jLabel9 = new javax.swing.JLabel();
        jLabel11 = new javax.swing.JLabel();
        jLabel10 = new javax.swing.JLabel();
        jButton4 = new javax.swing.JButton();
        jButton8 = new javax.swing.JButton();
        jButton7 = new javax.swing.JButton();
        jButton17 = new javax.swing.JButton();
        jLabel7 = new javax.swing.JLabel();
        jButton24 = new javax.swing.JButton();
        jButton12 = new javax.swing.JButton();
        jButton14 = new javax.swing.JButton();
        jLabel4 = new javax.swing.JLabel();
        jLabel1 = new javax.swing.JLabel();
        jButton15 = new javax.swing.JButton();
        jButton16 = new javax.swing.JButton();
        jLabel5 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jButton18 = new javax.swing.JButton();
        jLabel3 = new javax.swing.JLabel();
        jPanel7 = new javax.swing.JPanel();
        jButton19 = new javax.swing.JButton();
        jButton20 = new javax.swing.JButton();
        jButton21 = new javax.swing.JButton();
        jButton22 = new javax.swing.JButton();
        jButton23 = new javax.swing.JButton();
        jTextPane1 = new javax.swing.JTextPane();

        setLayout(new java.awt.BorderLayout());

        jPanel6.setPreferredSize(new java.awt.Dimension(540, 300));

        jPanel3.setLayout(new java.awt.GridLayout(1, 0));

        org.openide.awt.Mnemonics.setLocalizedText(jButton5, org.openide.util.NbBundle.getMessage(SdmTabAdmin.class, "SdmTabAdmin.jButton5.text")); // NOI18N
        jButton5.setFocusPainted(false);
        jButton5.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton5ActionPerformed(evt);
            }
        });
        jPanel3.add(jButton5);

        org.openide.awt.Mnemonics.setLocalizedText(jButton25, org.openide.util.NbBundle.getMessage(SdmTabAdmin.class, "SdmTabAdmin.jButton25.text")); // NOI18N
        jButton25.setFocusPainted(false);
        jButton25.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton25ActionPerformed(evt);
            }
        });
        jPanel3.add(jButton25);

        org.openide.awt.Mnemonics.setLocalizedText(jButton6, org.openide.util.NbBundle.getMessage(SdmTabAdmin.class, "SdmTabAdmin.jButton6.text")); // NOI18N
        jButton6.setFocusPainted(false);
        jButton6.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton6ActionPerformed(evt);
            }
        });
        jPanel3.add(jButton6);

        org.openide.awt.Mnemonics.setLocalizedText(jButton13, org.openide.util.NbBundle.getMessage(SdmTabAdmin.class, "SdmTabAdmin.jButton13.text")); // NOI18N
        jButton13.setFocusPainted(false);
        jButton13.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton13ActionPerformed(evt);
            }
        });
        jPanel3.add(jButton13);

        jTextField1.setEditable(false);
        jTextField1.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        jTextField1.setText(org.openide.util.NbBundle.getMessage(SdmTabAdmin.class, "SdmTabAdmin.jTextField1.text")); // NOI18N
        jTextField1.setBorder(null);
        jPanel3.add(jTextField1);

        jPanel4.setLayout(new java.awt.GridLayout(1, 0));

        org.openide.awt.Mnemonics.setLocalizedText(jButton1, org.openide.util.NbBundle.getMessage(SdmTabAdmin.class, "SdmTabAdmin.jButton1.text")); // NOI18N
        jButton1.setFocusPainted(false);
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });
        jPanel4.add(jButton1);

        org.openide.awt.Mnemonics.setLocalizedText(jButton2, org.openide.util.NbBundle.getMessage(SdmTabAdmin.class, "SdmTabAdmin.jButton2.text")); // NOI18N
        jButton2.setFocusPainted(false);
        jButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton2ActionPerformed(evt);
            }
        });
        jPanel4.add(jButton2);

        org.openide.awt.Mnemonics.setLocalizedText(jButton3, org.openide.util.NbBundle.getMessage(SdmTabAdmin.class, "SdmTabAdmin.jButton3.text")); // NOI18N
        jButton3.setFocusPainted(false);
        jButton3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton3ActionPerformed(evt);
            }
        });
        jPanel4.add(jButton3);

        jPanel5.setLayout(new java.awt.GridLayout(5, 4));

        org.openide.awt.Mnemonics.setLocalizedText(jButton10, org.openide.util.NbBundle.getMessage(SdmTabAdmin.class, "SdmTabAdmin.jButton10.text")); // NOI18N
        jButton10.setFocusPainted(false);
        jButton10.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton10ActionPerformed(evt);
            }
        });
        jPanel5.add(jButton10);

        org.openide.awt.Mnemonics.setLocalizedText(jButton11, org.openide.util.NbBundle.getMessage(SdmTabAdmin.class, "SdmTabAdmin.jButton11.text")); // NOI18N
        jButton11.setFocusPainted(false);
        jButton11.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton11ActionPerformed(evt);
            }
        });
        jPanel5.add(jButton11);

        org.openide.awt.Mnemonics.setLocalizedText(jLabel9, org.openide.util.NbBundle.getMessage(SdmTabAdmin.class, "SdmTabAdmin.jLabel9.text")); // NOI18N
        jPanel5.add(jLabel9);

        org.openide.awt.Mnemonics.setLocalizedText(jLabel11, org.openide.util.NbBundle.getMessage(SdmTabAdmin.class, "SdmTabAdmin.jLabel11.text")); // NOI18N
        jPanel5.add(jLabel11);

        org.openide.awt.Mnemonics.setLocalizedText(jLabel10, org.openide.util.NbBundle.getMessage(SdmTabAdmin.class, "SdmTabAdmin.jLabel10.text")); // NOI18N
        jPanel5.add(jLabel10);

        org.openide.awt.Mnemonics.setLocalizedText(jButton4, org.openide.util.NbBundle.getMessage(SdmTabAdmin.class, "SdmTabAdmin.jButton4.text")); // NOI18N
        jButton4.setFocusPainted(false);
        jButton4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton4ActionPerformed(evt);
            }
        });
        jPanel5.add(jButton4);

        org.openide.awt.Mnemonics.setLocalizedText(jButton8, org.openide.util.NbBundle.getMessage(SdmTabAdmin.class, "SdmTabAdmin.jButton8.text")); // NOI18N
        jButton8.setFocusPainted(false);
        jButton8.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton8ActionPerformed(evt);
            }
        });
        jPanel5.add(jButton8);

        org.openide.awt.Mnemonics.setLocalizedText(jButton7, org.openide.util.NbBundle.getMessage(SdmTabAdmin.class, "SdmTabAdmin.jButton7.text")); // NOI18N
        jButton7.setFocusPainted(false);
        jButton7.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton7ActionPerformed(evt);
            }
        });
        jPanel5.add(jButton7);

        org.openide.awt.Mnemonics.setLocalizedText(jButton17, org.openide.util.NbBundle.getMessage(SdmTabAdmin.class, "SdmTabAdmin.jButton17.text")); // NOI18N
        jButton17.setFocusPainted(false);
        jButton17.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton17ActionPerformed(evt);
            }
        });
        jPanel5.add(jButton17);

        org.openide.awt.Mnemonics.setLocalizedText(jLabel7, org.openide.util.NbBundle.getMessage(SdmTabAdmin.class, "SdmTabAdmin.jLabel7.text")); // NOI18N
        jPanel5.add(jLabel7);

        org.openide.awt.Mnemonics.setLocalizedText(jButton24, org.openide.util.NbBundle.getMessage(SdmTabAdmin.class, "SdmTabAdmin.jButton24.text")); // NOI18N
        jButton24.setFocusPainted(false);
        jButton24.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton24ActionPerformed(evt);
            }
        });
        jPanel5.add(jButton24);

        org.openide.awt.Mnemonics.setLocalizedText(jButton12, org.openide.util.NbBundle.getMessage(SdmTabAdmin.class, "SdmTabAdmin.jButton12.text")); // NOI18N
        jButton12.setFocusPainted(false);
        jButton12.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton12ActionPerformed(evt);
            }
        });
        jPanel5.add(jButton12);

        org.openide.awt.Mnemonics.setLocalizedText(jButton14, org.openide.util.NbBundle.getMessage(SdmTabAdmin.class, "SdmTabAdmin.jButton14.text")); // NOI18N
        jButton14.setFocusPainted(false);
        jButton14.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton14ActionPerformed(evt);
            }
        });
        jPanel5.add(jButton14);

        org.openide.awt.Mnemonics.setLocalizedText(jLabel4, org.openide.util.NbBundle.getMessage(SdmTabAdmin.class, "SdmTabAdmin.jLabel4.text")); // NOI18N
        jPanel5.add(jLabel4);

        org.openide.awt.Mnemonics.setLocalizedText(jLabel1, org.openide.util.NbBundle.getMessage(SdmTabAdmin.class, "SdmTabAdmin.jLabel1.text")); // NOI18N
        jPanel5.add(jLabel1);

        org.openide.awt.Mnemonics.setLocalizedText(jButton15, org.openide.util.NbBundle.getMessage(SdmTabAdmin.class, "SdmTabAdmin.jButton15.text")); // NOI18N
        jButton15.setFocusPainted(false);
        jButton15.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton15ActionPerformed(evt);
            }
        });
        jPanel5.add(jButton15);

        org.openide.awt.Mnemonics.setLocalizedText(jButton16, org.openide.util.NbBundle.getMessage(SdmTabAdmin.class, "SdmTabAdmin.jButton16.text")); // NOI18N
        jButton16.setToolTipText(org.openide.util.NbBundle.getMessage(SdmTabAdmin.class, "SdmTabAdmin.jButton16.toolTipText")); // NOI18N
        jButton16.setFocusPainted(false);
        jButton16.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton16ActionPerformed(evt);
            }
        });
        jPanel5.add(jButton16);

        org.openide.awt.Mnemonics.setLocalizedText(jLabel5, org.openide.util.NbBundle.getMessage(SdmTabAdmin.class, "SdmTabAdmin.jLabel5.text")); // NOI18N
        jPanel5.add(jLabel5);

        org.openide.awt.Mnemonics.setLocalizedText(jLabel6, org.openide.util.NbBundle.getMessage(SdmTabAdmin.class, "SdmTabAdmin.jLabel6.text")); // NOI18N
        jPanel5.add(jLabel6);

        org.openide.awt.Mnemonics.setLocalizedText(jLabel2, org.openide.util.NbBundle.getMessage(SdmTabAdmin.class, "SdmTabAdmin.jLabel2.text")); // NOI18N
        jPanel5.add(jLabel2);

        org.openide.awt.Mnemonics.setLocalizedText(jButton18, org.openide.util.NbBundle.getMessage(SdmTabAdmin.class, "SdmTabAdmin.jButton18.text")); // NOI18N
        jButton18.setToolTipText(org.openide.util.NbBundle.getMessage(SdmTabAdmin.class, "SdmTabAdmin.jButton18.toolTipText")); // NOI18N
        jButton18.setFocusPainted(false);
        jButton18.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton18ActionPerformed(evt);
            }
        });
        jPanel5.add(jButton18);

        org.openide.awt.Mnemonics.setLocalizedText(jLabel3, org.openide.util.NbBundle.getMessage(SdmTabAdmin.class, "SdmTabAdmin.jLabel3.text")); // NOI18N
        jPanel5.add(jLabel3);

        jPanel7.setLayout(new java.awt.GridLayout(1, 0));

        org.openide.awt.Mnemonics.setLocalizedText(jButton19, org.openide.util.NbBundle.getMessage(SdmTabAdmin.class, "SdmTabAdmin.jButton19.text")); // NOI18N
        jButton19.setFocusPainted(false);
        jButton19.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton19ActionPerformed(evt);
            }
        });
        jPanel7.add(jButton19);

        org.openide.awt.Mnemonics.setLocalizedText(jButton20, org.openide.util.NbBundle.getMessage(SdmTabAdmin.class, "SdmTabAdmin.jButton20.text")); // NOI18N
        jButton20.setFocusPainted(false);
        jButton20.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton20ActionPerformed(evt);
            }
        });
        jPanel7.add(jButton20);

        org.openide.awt.Mnemonics.setLocalizedText(jButton21, org.openide.util.NbBundle.getMessage(SdmTabAdmin.class, "SdmTabAdmin.jButton21.text")); // NOI18N
        jButton21.setFocusPainted(false);
        jButton21.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton21ActionPerformed(evt);
            }
        });
        jPanel7.add(jButton21);

        org.openide.awt.Mnemonics.setLocalizedText(jButton22, org.openide.util.NbBundle.getMessage(SdmTabAdmin.class, "SdmTabAdmin.jButton22.text")); // NOI18N
        jButton22.setFocusPainted(false);
        jButton22.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton22ActionPerformed(evt);
            }
        });
        jPanel7.add(jButton22);

        org.openide.awt.Mnemonics.setLocalizedText(jButton23, org.openide.util.NbBundle.getMessage(SdmTabAdmin.class, "SdmTabAdmin.jButton23.text")); // NOI18N
        jButton23.setFocusPainted(false);
        jButton23.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton23ActionPerformed(evt);
            }
        });
        jPanel7.add(jButton23);

        jTextPane1.setMargin(new java.awt.Insets(3, 10, 3, 3));

        javax.swing.GroupLayout jPanel6Layout = new javax.swing.GroupLayout(jPanel6);
        jPanel6.setLayout(jPanel6Layout);
        jPanel6Layout.setHorizontalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jTextPane1)
                    .addComponent(jPanel7, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel5, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 861, Short.MAX_VALUE)))
        );
        jPanel6Layout.setVerticalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jPanel5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jPanel7, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jTextPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 267, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addComponent(jPanel6, javax.swing.GroupLayout.PREFERRED_SIZE, 867, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 16, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addComponent(jPanel6, javax.swing.GroupLayout.PREFERRED_SIZE, 547, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 18, Short.MAX_VALUE))
        );

        jScrollPane1.setViewportView(jPanel1);

        add(jScrollPane1, java.awt.BorderLayout.CENTER);
    }// </editor-fold>//GEN-END:initComponents

    private void jButton23ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton23ActionPerformed
        NbpIdeEditorHelpers.open_resource_file_in_editor_async("ruby.vm", "com/sqldalmaker/cg/ruby", "ruby.vm");
    }//GEN-LAST:event_jButton23ActionPerformed

    private void jButton22ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton22ActionPerformed
        NbpIdeEditorHelpers.open_resource_file_in_editor_async("python.vm", "com/sqldalmaker/cg/python", "python.vm");
    }//GEN-LAST:event_jButton22ActionPerformed

    private void jButton21ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton21ActionPerformed
        NbpIdeEditorHelpers.open_resource_file_in_editor_async("cpp.vm", "com/sqldalmaker/cg/cpp", "cpp.vm");
    }//GEN-LAST:event_jButton21ActionPerformed

    private void jButton20ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton20ActionPerformed
        NbpIdeEditorHelpers.open_resource_file_in_editor_async("java.vm", "com/sqldalmaker/cg/java", "java.vm");
    }//GEN-LAST:event_jButton20ActionPerformed

    private void jButton19ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton19ActionPerformed
        NbpIdeEditorHelpers.open_resource_file_in_editor_async("php.vm", "com/sqldalmaker/cg/php", "php.vm");
    }//GEN-LAST:event_jButton19ActionPerformed

    private void jButton18ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton18ActionPerformed
        NbpIdeEditorHelpers.open_resource_file_in_editor_async("data_store.rb", "data_store.rb");
    }//GEN-LAST:event_jButton18ActionPerformed

    private void jButton15ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton15ActionPerformed
        NbpIdeEditorHelpers.open_resource_file_in_editor_async("DataStore1.py", "DataStore1.py");
    }//GEN-LAST:event_jButton15ActionPerformed

    private void jButton16ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton16ActionPerformed
        NbpIdeEditorHelpers.open_resource_file_in_editor_async("DataStore2.py", "DataStore2.py");
    }//GEN-LAST:event_jButton16ActionPerformed

    private void jButton14ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton14ActionPerformed
        NbpIdeEditorHelpers.open_resource_file_in_editor_async("DataStore.STL.cpp", "DataStore.STL.cpp");
        NbpIdeEditorHelpers.open_resource_file_in_editor_async("DataStore.STL.h", "DataStore.STL.h");
    }//GEN-LAST:event_jButton14ActionPerformed

    private void jButton12ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton12ActionPerformed
        NbpIdeEditorHelpers.open_resource_file_in_editor_async("DataStore.cpp", "DataStore.cpp");
        NbpIdeEditorHelpers.open_resource_file_in_editor_async("DataStore.h", "DataStore.h");
    }//GEN-LAST:event_jButton12ActionPerformed

    private void jButton24ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton24ActionPerformed
        NbpIdeEditorHelpers.open_resource_file_in_editor_async("DataStore_Qt5.cpp", "DataStore_Qt5.cpp");
        NbpIdeEditorHelpers.open_resource_file_in_editor_async("DataStore_Qt5.h", "DataStore_Qt5.h");
    }//GEN-LAST:event_jButton24ActionPerformed

    private void jButton17ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton17ActionPerformed
        NbpIdeEditorHelpers.open_resource_file_in_editor_async("DataStoreManagerAndroid.java", "DataStoreManagerAndroid.java_");
    }//GEN-LAST:event_jButton17ActionPerformed

    private void jButton7ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton7ActionPerformed
        NbpIdeEditorHelpers.open_resource_file_in_editor_async("DataStoreManagerSpring.java", "DataStoreManagerSpring.java_");
    }//GEN-LAST:event_jButton7ActionPerformed

    private void jButton8ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton8ActionPerformed
        NbpIdeEditorHelpers.open_resource_file_in_editor_async("DataStoreManagerDbUtils.java", "DataStoreManagerDbUtils.java_");
    }//GEN-LAST:event_jButton8ActionPerformed

    private void jButton4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton4ActionPerformed
        NbpIdeEditorHelpers.open_resource_file_in_editor_async("DataStore.java", "DataStore.java_");
    }//GEN-LAST:event_jButton4ActionPerformed

    private void jButton11ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton11ActionPerformed
        NbpIdeEditorHelpers.open_resource_file_in_editor_async("PDODataStore.php", "PDODataStore.php");
    }//GEN-LAST:event_jButton11ActionPerformed

    private void jButton10ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton10ActionPerformed
        NbpIdeEditorHelpers.open_resource_file_in_editor_async("DataStore.php", "DataStore.php");
    }//GEN-LAST:event_jButton10ActionPerformed

    private void jButton3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton3ActionPerformed
        int dialogResult = JOptionPane.showConfirmDialog(null, "This action creates/overwrites dto.xml in the folder of XML meta-program. Continue?",
            "Warning", JOptionPane.YES_NO_OPTION);

        if (dialogResult == JOptionPane.YES_OPTION) {

            NbpMetaProgramInitHelpers.copy_dto_xml(obj);
        }
    }//GEN-LAST:event_jButton3ActionPerformed

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed

        int dialogResult = JOptionPane.showConfirmDialog(null, "This action creates/overwrites settings.xml in the folder of XML meta-program. Continue?",
            "Warning", JOptionPane.YES_NO_OPTION);

        if (dialogResult == JOptionPane.YES_OPTION) {

            NbpMetaProgramInitHelpers.copy_settings_xml(obj);
        }
    }//GEN-LAST:event_jButton2ActionPerformed

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed

        int dialogResult = JOptionPane.showConfirmDialog(null, "This action creates/overwrites XSD files in the folder of XML meta-program. Continue?",
            "Warning", JOptionPane.YES_NO_OPTION);

        if (dialogResult == JOptionPane.YES_OPTION) {

            NbpMetaProgramInitHelpers.copy_xsd(obj);
        }
    }//GEN-LAST:event_jButton1ActionPerformed

    private void jButton13ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton13ActionPerformed
        NbpIdeEditorHelpers.open_resource_file_in_editor_async("recent_changes.txt", "recent_changes.txt");
    }//GEN-LAST:event_jButton13ActionPerformed

    private void jButton6ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton6ActionPerformed
        testConnection();
    }//GEN-LAST:event_jButton6ActionPerformed

    private void jButton25ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton25ActionPerformed
        NbpIdeEditorHelpers.open_resource_file_in_editor_async("reference-settings.xml", "settings.xml");
    }//GEN-LAST:event_jButton25ActionPerformed

    private void jButton5ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton5ActionPerformed
        editSettingsXml();
    }//GEN-LAST:event_jButton5ActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton10;
    private javax.swing.JButton jButton11;
    private javax.swing.JButton jButton12;
    private javax.swing.JButton jButton13;
    private javax.swing.JButton jButton14;
    private javax.swing.JButton jButton15;
    private javax.swing.JButton jButton16;
    private javax.swing.JButton jButton17;
    private javax.swing.JButton jButton18;
    private javax.swing.JButton jButton19;
    private javax.swing.JButton jButton2;
    private javax.swing.JButton jButton20;
    private javax.swing.JButton jButton21;
    private javax.swing.JButton jButton22;
    private javax.swing.JButton jButton23;
    private javax.swing.JButton jButton24;
    private javax.swing.JButton jButton25;
    private javax.swing.JButton jButton3;
    private javax.swing.JButton jButton4;
    private javax.swing.JButton jButton5;
    private javax.swing.JButton jButton6;
    private javax.swing.JButton jButton7;
    private javax.swing.JButton jButton8;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTextField jTextField1;
    private javax.swing.JTextPane jTextPane1;
    // End of variables declaration//GEN-END:variables

    private void testConnection() {
        try {

            Connection conn = NbpHelpers.get_connection(obj);

            conn.close();

            NbpIdeMessageHelpers.show_info_in_ui_thread("Test connection succeeded.");

        } catch (Exception ex) {
            // ex.printStackTrace();
            NbpIdeMessageHelpers.show_error_in_ui_thread(ex);
        }
    }
}
