/*
    Copyright 2011-2022 sqldalmaker@gmail.com
    SQL DAL Maker Website: http://sqldalmaker.sourceforge.net
    Read LICENSE.txt in the root of this project/archive for details.
 */
package com.sqldalmaker.netbeans;

import com.sqldalmaker.cg.Helpers;
import com.sqldalmaker.common.Const;
import com.sqldalmaker.jaxb.settings.Settings;
import java.awt.Desktop;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.sql.Connection;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JToolBar;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import org.netbeans.core.spi.multiview.MultiViewElement;
import org.openide.filesystems.FileObject;
import org.openide.modules.ModuleInfo;
import org.openide.modules.Modules;
import org.openide.util.Exceptions;
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
        jTextField1.setText("v" + v + " on Java " + jv);
        jTextField1.setBorder(javax.swing.BorderFactory.createEmptyBorder());
        ////////////////////////////////////////
        jTextPane1.setEditable(false);
        jTextPane1.setContentType("text/html");
        try {
            String text = NbpHelpers.read_from_jar_file("", "ABOUT.html");
            jTextPane1.setText(text);
        } catch (Exception ex) {
            Exceptions.printStackTrace(ex);
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
                        } catch (Exception ex) {
                            Exceptions.printStackTrace(ex);
                        }
                    }
                }
            }
        });
    }

    private boolean is_opened = false;

    // unlike componentActivated(), componentOpened() 
    // is called after construct only once
    @Override
    public void componentOpened() { // componentActivated() {
        super.componentOpened();
        if (!is_opened) {
            // https://stackoverflow.com/questions/291115/java-swing-using-jscrollpane-and-having-it-scroll-back-to-top
            javax.swing.SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    jScrollPane1.getVerticalScrollBar().setValue(0);
                    // https://stackoverflow.com/questions/5583495/how-do-i-speed-up-the-scroll-speed-in-a-jscrollpane-when-using-the-mouse-wheel
                    jScrollPane1.getVerticalScrollBar().setUnitIncrement(20);
                }
            });

            jPanel6.setFocusable(true);
            
            jPanel6.addFocusListener(new FocusListener() {

                @Override
                public void focusGained(FocusEvent arg0) {
                    // System.out.println("panel focus");
                    // jScrollPane1.requestFocus();
                    jScrollPane1.grabFocus();
                }

                @Override
                public void focusLost(FocusEvent arg0) {
                    // TODO Auto-generated method stub

                }

            });

            is_opened = true;
        }
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
        jPanel6 = new javax.swing.JPanel();
        jPanel8 = new javax.swing.JPanel();
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
        jPanel1 = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        jButton10 = new javax.swing.JButton();
        jButton28 = new javax.swing.JButton();
        jButton29 = new javax.swing.JButton();
        jButton33 = new javax.swing.JButton();
        jButton30 = new javax.swing.JButton();
        jButton26 = new javax.swing.JButton();
        jButton11 = new javax.swing.JButton();
        jPanel2 = new javax.swing.JPanel();
        jLabel8 = new javax.swing.JLabel();
        jButton4 = new javax.swing.JButton();
        jButton8 = new javax.swing.JButton();
        jButton17 = new javax.swing.JButton();
        jLabel7 = new javax.swing.JLabel();
        jButton24 = new javax.swing.JButton();
        jButton14 = new javax.swing.JButton();
        jButton12 = new javax.swing.JButton();
        jPanel9 = new javax.swing.JPanel();
        jLabel9 = new javax.swing.JLabel();
        jButton7 = new javax.swing.JButton();
        jButton15 = new javax.swing.JButton();
        jButton32 = new javax.swing.JButton();
        jButton9 = new javax.swing.JButton();
        jPanel5 = new javax.swing.JPanel();
        jLabel10 = new javax.swing.JLabel();
        jButton18 = new javax.swing.JButton();
        jLabel11 = new javax.swing.JLabel();
        jButton34 = new javax.swing.JButton();
        jPanel7 = new javax.swing.JPanel();
        jButton19 = new javax.swing.JButton();
        jButton20 = new javax.swing.JButton();
        jButton21 = new javax.swing.JButton();
        jButton22 = new javax.swing.JButton();
        jButton23 = new javax.swing.JButton();
        jButton31 = new javax.swing.JButton();
        jTextPane1 = new javax.swing.JTextPane();

        setName(""); // NOI18N
        setOpaque(false);
        setLayout(new java.awt.BorderLayout());

        jScrollPane1.setBorder(null);

        jPanel6.setToolTipText(org.openide.util.NbBundle.getMessage(SdmTabAdmin.class, "SdmTabAdmin.toolTipText")); // NOI18N
        jPanel6.setMinimumSize(new java.awt.Dimension(710, 436));
        jPanel6.setName(""); // NOI18N
        jPanel6.setPreferredSize(new java.awt.Dimension(710, 436));
        jPanel6.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                jPanel6MouseEntered(evt);
            }
        });
        jPanel6.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        jPanel8.setMinimumSize(new java.awt.Dimension(680, 436));
        jPanel8.setName(""); // NOI18N
        jPanel8.setPreferredSize(new java.awt.Dimension(680, 436));
        jPanel8.setLayout(new javax.swing.BoxLayout(jPanel8, javax.swing.BoxLayout.Y_AXIS));

        jPanel3.setMinimumSize(new java.awt.Dimension(710, 32));
        jPanel3.setPreferredSize(new java.awt.Dimension(710, 32));
        jPanel3.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        jButton5.setText("Edit settings.xml");
        jButton5.setFocusPainted(false);
        jButton5.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton5ActionPerformed(evt);
            }
        });
        jPanel3.add(jButton5);

        jButton25.setText("Reference settings.xml");
        jButton25.setFocusPainted(false);
        jButton25.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton25ActionPerformed(evt);
            }
        });
        jPanel3.add(jButton25);

        jButton6.setText("Validate All");
        jButton6.setFocusPainted(false);
        jButton6.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton6ActionPerformed(evt);
            }
        });
        jPanel3.add(jButton6);

        jButton13.setText("News");
        jButton13.setActionCommand("News");
        jButton13.setFocusPainted(false);
        jButton13.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton13ActionPerformed(evt);
            }
        });
        jPanel3.add(jButton13);

        jTextField1.setEditable(false);
        jTextField1.setHorizontalAlignment(javax.swing.JTextField.LEFT);
        jTextField1.setText("v. ?");
        jTextField1.setMargin(new java.awt.Insets(2, 10, 2, 2));
        jTextField1.setMinimumSize(new java.awt.Dimension(63, 22));
        jTextField1.setPreferredSize(new java.awt.Dimension(180, 22));
        jPanel3.add(jTextField1);

        jPanel8.add(jPanel3);

        jPanel4.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        jButton1.setText("Create/Overwrite XSD files");
        jButton1.setFocusPainted(false);
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });
        jPanel4.add(jButton1);

        jButton2.setText("Create/Overwrite settings.xml");
        jButton2.setFocusPainted(false);
        jButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton2ActionPerformed(evt);
            }
        });
        jPanel4.add(jButton2);

        jButton3.setText("Create/Overwrite dto.xml");
        jButton3.setFocusPainted(false);
        jButton3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton3ActionPerformed(evt);
            }
        });
        jPanel4.add(jButton3);

        jPanel8.add(jPanel4);

        jPanel1.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        jLabel2.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel2.setText("PHP");
        jLabel2.setPreferredSize(new java.awt.Dimension(40, 14));
        jPanel1.add(jLabel2);

        jButton10.setText("Base");
        jButton10.setFocusPainted(false);
        jButton10.setMaximumSize(new java.awt.Dimension(60, 22));
        jButton10.setMinimumSize(new java.awt.Dimension(60, 22));
        jButton10.setPreferredSize(new java.awt.Dimension(60, 22));
        jButton10.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton10ActionPerformed(evt);
            }
        });
        jPanel1.add(jButton10);

        jButton28.setText("MySQL");
        jButton28.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton28ActionPerformed(evt);
            }
        });
        jPanel1.add(jButton28);

        jButton29.setText("Oracle");
        jButton29.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton29ActionPerformed(evt);
            }
        });
        jPanel1.add(jButton29);

        jButton33.setText("oci8");
        jButton33.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton33ActionPerformed(evt);
            }
        });
        jPanel1.add(jButton33);

        jButton30.setText("MS SQL");
        jButton30.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton30ActionPerformed(evt);
            }
        });
        jPanel1.add(jButton30);

        jButton26.setText("PostgreSQL");
        jButton26.setFocusPainted(false);
        jButton26.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton26ActionPerformed(evt);
            }
        });
        jPanel1.add(jButton26);

        jButton11.setText("SQLite3");
        jButton11.setFocusPainted(false);
        jButton11.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton11ActionPerformed(evt);
            }
        });
        jPanel1.add(jButton11);

        jPanel8.add(jPanel1);

        jPanel2.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        jLabel8.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel8.setText("Java");
        jLabel8.setPreferredSize(new java.awt.Dimension(40, 14));
        jPanel2.add(jLabel8);

        jButton4.setText("Base");
        jButton4.setFocusPainted(false);
        jButton4.setMaximumSize(new java.awt.Dimension(60, 22));
        jButton4.setMinimumSize(new java.awt.Dimension(60, 22));
        jButton4.setPreferredSize(new java.awt.Dimension(60, 22));
        jButton4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton4ActionPerformed(evt);
            }
        });
        jPanel2.add(jButton4);

        jButton8.setText("JDBC");
        jButton8.setFocusPainted(false);
        jButton8.setMaximumSize(new java.awt.Dimension(60, 22));
        jButton8.setMinimumSize(new java.awt.Dimension(60, 22));
        jButton8.setPreferredSize(new java.awt.Dimension(60, 22));
        jButton8.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton8ActionPerformed(evt);
            }
        });
        jPanel2.add(jButton8);

        jButton17.setText("Android");
        jButton17.setFocusPainted(false);
        jButton17.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton17ActionPerformed(evt);
            }
        });
        jPanel2.add(jButton17);

        jLabel7.setText("C++");
        jPanel2.add(jLabel7);

        jButton24.setText("QtSql");
        jButton24.setFocusPainted(false);
        jButton24.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton24ActionPerformed(evt);
            }
        });
        jPanel2.add(jButton24);

        jButton14.setText("STL, sqlite3");
        jButton14.setFocusPainted(false);
        jButton14.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton14ActionPerformed(evt);
            }
        });
        jPanel2.add(jButton14);

        jButton12.setText("ATL, sqlite3");
        jButton12.setFocusPainted(false);
        jButton12.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton12ActionPerformed(evt);
            }
        });
        jPanel2.add(jButton12);

        jPanel8.add(jPanel2);

        jPanel9.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        jLabel9.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel9.setText("Python");
        jLabel9.setPreferredSize(new java.awt.Dimension(40, 14));
        jPanel9.add(jLabel9);

        jButton7.setText("sqlite3, mysql.connector, psycopg2");
        jButton7.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton7ActionPerformed(evt);
            }
        });
        jPanel9.add(jButton7);

        jButton15.setText("SQLAlchemy");
        jButton15.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton15ActionPerformed(evt);
            }
        });
        jPanel9.add(jButton15);

        jButton32.setText("cx_Oracle");
        jButton32.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton32ActionPerformed(evt);
            }
        });
        jPanel9.add(jButton32);

        jButton9.setText("pyodbc, mssql");
        jButton9.setActionCommand("DataStore (pyodbc, SQL Server)");
        jButton9.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton9ActionPerformed(evt);
            }
        });
        jPanel9.add(jButton9);

        jPanel8.add(jPanel9);

        jPanel5.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        jLabel10.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel10.setText("Ruby");
        jLabel10.setPreferredSize(new java.awt.Dimension(40, 14));
        jPanel5.add(jLabel10);

        jButton18.setText("DBI");
        jButton18.setToolTipText("");
        jButton18.setFocusPainted(false);
        jButton18.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton18ActionPerformed(evt);
            }
        });
        jPanel5.add(jButton18);

        jLabel11.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel11.setText("Go");
        jLabel11.setPreferredSize(new java.awt.Dimension(18, 14));
        jPanel5.add(jLabel11);

        jButton34.setText("database/sql");
        jButton34.setToolTipText("");
        jButton34.setFocusPainted(false);
        jButton34.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton34ActionPerformed(evt);
            }
        });
        jPanel5.add(jButton34);

        jPanel8.add(jPanel5);

        jPanel7.setPreferredSize(new java.awt.Dimension(478, 32));
        jPanel7.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        jButton19.setText("php.vm");
        jButton19.setFocusPainted(false);
        jButton19.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton19ActionPerformed(evt);
            }
        });
        jPanel7.add(jButton19);

        jButton20.setText("java.vm");
        jButton20.setFocusPainted(false);
        jButton20.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton20ActionPerformed(evt);
            }
        });
        jPanel7.add(jButton20);

        jButton21.setText("cpp.vm");
        jButton21.setFocusPainted(false);
        jButton21.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton21ActionPerformed(evt);
            }
        });
        jPanel7.add(jButton21);

        jButton22.setText("python.vm");
        jButton22.setFocusPainted(false);
        jButton22.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton22ActionPerformed(evt);
            }
        });
        jPanel7.add(jButton22);

        jButton23.setText("ruby.vm");
        jButton23.setFocusPainted(false);
        jButton23.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton23ActionPerformed(evt);
            }
        });
        jPanel7.add(jButton23);

        jButton31.setText("go.vm");
        jButton31.setFocusPainted(false);
        jButton31.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton31ActionPerformed(evt);
            }
        });
        jPanel7.add(jButton31);

        jPanel8.add(jPanel7);

        jTextPane1.setEditable(false);
        jTextPane1.setBackground(new java.awt.Color(255, 255, 255));
        jTextPane1.setFont(new java.awt.Font("Tahoma", 0, 10)); // NOI18N
        jTextPane1.setAutoscrolls(false);
        jTextPane1.setMargin(new java.awt.Insets(0, 12, 12, 12));
        jTextPane1.setName(""); // NOI18N
        jPanel8.add(jTextPane1);

        jPanel6.add(jPanel8);

        jScrollPane1.setViewportView(jPanel6);

        add(jScrollPane1, java.awt.BorderLayout.CENTER);
        jScrollPane1.getAccessibleContext().setAccessibleDescription("");
    }// </editor-fold>//GEN-END:initComponents

    private void jPanel6MouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jPanel6MouseEntered
        // System.out.println("MouseEntered");
        // jPanel6.requestFocus();
        jPanel6.grabFocus();
        //        jPanel6.addFocusListener(new FocusListener(){
            //
            //            @Override
            //            public void focusGained(FocusEvent arg0) {
                //                System.out.println("panel focus");
                //            }
            //
            //            @Override
            //            public void focusLost(FocusEvent arg0) {
                //                // TODO Auto-generated method stub
                //
                //            }
            //
            //        });
    }//GEN-LAST:event_jPanel6MouseEntered

    private void jButton34ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton34ActionPerformed
        NbpIdeEditorHelpers.open_resource_file_in_editor_async("data_store.go", "data_store.go");
    }//GEN-LAST:event_jButton34ActionPerformed

    private void jButton18ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton18ActionPerformed
        NbpIdeEditorHelpers.open_resource_file_in_editor_async("data_store.rb", "data_store.rb");
    }//GEN-LAST:event_jButton18ActionPerformed

    private void jButton9ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton9ActionPerformed
        NbpIdeEditorHelpers.open_resource_file_in_editor_async("data_store_pyodbc.py", "data_store_pyodbc.py");
    }//GEN-LAST:event_jButton9ActionPerformed

    private void jButton32ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton32ActionPerformed
        NbpIdeEditorHelpers.open_resource_file_in_editor_async("data_store_cx_oracle.py", "data_store_cx_oracle.py");
    }//GEN-LAST:event_jButton32ActionPerformed

    private void jButton12ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton12ActionPerformed
        NbpIdeEditorHelpers.open_resource_file_in_editor_async("DataStore.cpp", "DataStore.cpp");
        NbpIdeEditorHelpers.open_resource_file_in_editor_async("DataStore.h", "DataStore.h");
    }//GEN-LAST:event_jButton12ActionPerformed

    private void jButton14ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton14ActionPerformed
        NbpIdeEditorHelpers.open_resource_file_in_editor_async("DataStore.STL.cpp", "DataStore.STL.cpp");
        NbpIdeEditorHelpers.open_resource_file_in_editor_async("DataStore.STL.h", "DataStore.STL.h");
    }//GEN-LAST:event_jButton14ActionPerformed

    private void jButton24ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton24ActionPerformed
        NbpIdeEditorHelpers.open_resource_file_in_editor_async("DataStore_Qt5.cpp", "DataStore_Qt5.cpp");
        NbpIdeEditorHelpers.open_resource_file_in_editor_async("DataStore_Qt5.h", "DataStore_Qt5.h");
    }//GEN-LAST:event_jButton24ActionPerformed

    private void jButton17ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton17ActionPerformed
        NbpIdeEditorHelpers.open_resource_file_in_editor_async("DataStoreManagerAndroid.java", "DataStoreManagerAndroid.java_");
    }//GEN-LAST:event_jButton17ActionPerformed

    private void jButton8ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton8ActionPerformed
        NbpIdeEditorHelpers.open_resource_file_in_editor_async("DataStoreManager.java", "DataStoreManagerJDBC.java_");
    }//GEN-LAST:event_jButton8ActionPerformed

    private void jButton4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton4ActionPerformed
        NbpIdeEditorHelpers.open_resource_file_in_editor_async("DataStore.java", "DataStore.java_");
    }//GEN-LAST:event_jButton4ActionPerformed

    private void jButton11ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton11ActionPerformed
        NbpIdeEditorHelpers.open_resource_file_in_editor_async("DataStore_PDO_SQLite3.php", "DataStore_PDO_SQLite3.php");
    }//GEN-LAST:event_jButton11ActionPerformed

    private void jButton26ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton26ActionPerformed
        NbpIdeEditorHelpers.open_resource_file_in_editor_async("DataStore_PDO_PostgreSQL.php", "DataStore_PDO_PostgreSQL.php");
    }//GEN-LAST:event_jButton26ActionPerformed

    private void jButton30ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton30ActionPerformed
        NbpIdeEditorHelpers.open_resource_file_in_editor_async("DataStore_PDO_SQL_Server.php", "DataStore_PDO_SQL_Server.php");
    }//GEN-LAST:event_jButton30ActionPerformed

    private void jButton33ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton33ActionPerformed
        NbpIdeEditorHelpers.open_resource_file_in_editor_async("DataStore_oci8.php", "DataStore_oci8.php");
    }//GEN-LAST:event_jButton33ActionPerformed

    private void jButton29ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton29ActionPerformed
        NbpIdeEditorHelpers.open_resource_file_in_editor_async("DataStore_PDO_Oracle.php", "DataStore_PDO_Oracle.php");
    }//GEN-LAST:event_jButton29ActionPerformed

    private void jButton28ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton28ActionPerformed
        NbpIdeEditorHelpers.open_resource_file_in_editor_async("DataStore_PDO_MySQL.php", "DataStore_PDO_MySQL.php");
    }//GEN-LAST:event_jButton28ActionPerformed

    private void jButton10ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton10ActionPerformed
        NbpIdeEditorHelpers.open_resource_file_in_editor_async("DataStore.php", "DataStore.php");
    }//GEN-LAST:event_jButton10ActionPerformed

    private void jButton31ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton31ActionPerformed
        NbpIdeEditorHelpers.open_resource_file_in_editor_async("go.vm", "com/sqldalmaker/cg/go", "go.vm");
    }//GEN-LAST:event_jButton31ActionPerformed

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

    private void jButton3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton3ActionPerformed

        int dialogResult = JOptionPane.showConfirmDialog(null,
            "This action creates/overwrites 'dto.xml' in the folder of XML meta-program. Continue?",
            "Warning", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (dialogResult == JOptionPane.YES_OPTION) {

            NbpMetaProgramInitHelpers.copy_dto_xml(obj);
        }
    }//GEN-LAST:event_jButton3ActionPerformed

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed

        int dialogResult = JOptionPane.showConfirmDialog(null, "This action creates/overwrites 'settings.xml' in the folder of XML meta-program. Continue?",
            "Warning", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (dialogResult == JOptionPane.YES_OPTION) {

            NbpMetaProgramInitHelpers.copy_settings_xml(obj);
        }
    }//GEN-LAST:event_jButton2ActionPerformed

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed

        int dialogResult = JOptionPane.showConfirmDialog(null, "This action creates/overwrites XSD files in the folder of XML meta-program. Continue?",
            "Warning", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (dialogResult == JOptionPane.YES_OPTION) {

            NbpMetaProgramInitHelpers.copy_xsd(obj);
        }
    }//GEN-LAST:event_jButton1ActionPerformed

    private void jButton13ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton13ActionPerformed
        NbpIdeEditorHelpers.open_resource_file_in_editor_async("recent_changes.txt", "", "recent_changes.txt");
    }//GEN-LAST:event_jButton13ActionPerformed

    private void jButton6ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton6ActionPerformed
        validate_all();
    }//GEN-LAST:event_jButton6ActionPerformed

    private void jButton25ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton25ActionPerformed
        NbpIdeEditorHelpers.open_resource_file_in_editor_async("reference-settings.xml", "settings.xml");
    }//GEN-LAST:event_jButton25ActionPerformed

    private void jButton5ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton5ActionPerformed
        editSettingsXml();
    }//GEN-LAST:event_jButton5ActionPerformed

    private void jButton7ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton7ActionPerformed
        NbpIdeEditorHelpers.open_resource_file_in_editor_async("data_store.py", "data_store.py");
    }//GEN-LAST:event_jButton7ActionPerformed

    private void jButton15ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton15ActionPerformed
        NbpIdeEditorHelpers.open_resource_file_in_editor_async("data_store_sqlalchemy.py", "data_store_sqlalchemy.py");
    }//GEN-LAST:event_jButton15ActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton10;
    private javax.swing.JButton jButton11;
    private javax.swing.JButton jButton12;
    private javax.swing.JButton jButton13;
    private javax.swing.JButton jButton14;
    private javax.swing.JButton jButton15;
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
    private javax.swing.JButton jButton26;
    private javax.swing.JButton jButton28;
    private javax.swing.JButton jButton29;
    private javax.swing.JButton jButton3;
    private javax.swing.JButton jButton30;
    private javax.swing.JButton jButton31;
    private javax.swing.JButton jButton32;
    private javax.swing.JButton jButton33;
    private javax.swing.JButton jButton34;
    private javax.swing.JButton jButton4;
    private javax.swing.JButton jButton5;
    private javax.swing.JButton jButton6;
    private javax.swing.JButton jButton7;
    private javax.swing.JButton jButton8;
    private javax.swing.JButton jButton9;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JPanel jPanel8;
    private javax.swing.JPanel jPanel9;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTextField jTextField1;
    private javax.swing.JTextPane jTextPane1;
    // End of variables declaration//GEN-END:variables

//    private void testConnection() {
//        try {
//            Connection conn = NbpHelpers.get_connection(obj);
//            conn.close();
//            NbpIdeMessageHelpers.show_info_in_ui_thread("Test connection succeeded.");
//        } catch (Exception ex) {
//            // ex.printStackTrace();
//            NbpIdeMessageHelpers.show_error_in_ui_thread(ex);
//        }
//    }
    private static String get_err_msg(Throwable ex) {
        return ex.getClass().getName() + " -> " + ex.getMessage(); // printStackTrace();
    }

    private void validate_all() {
        NbpIdeConsoleUtil ide_log = new NbpIdeConsoleUtil(obj);
        StringBuilder buff = new StringBuilder();
        Settings sett = null;
        if (check_xsd(ide_log, buff, Const.SETTINGS_XSD)) {
            try {
                sett = NbpHelpers.load_settings(obj);
                add_ok_msg(ide_log, buff, Const.SETTINGS_XML);
            } catch (Exception ex) {
                add_err_msg(ide_log, buff, "Invalid " + Const.SETTINGS_XML + ": " + get_err_msg(ex));
            }
        } else {
            add_err_msg(ide_log, buff, "Cannot load " + Const.SETTINGS_XML + " because of invalid " + Const.SETTINGS_XSD);
        }
        check_xsd(ide_log, buff, Const.DTO_XSD);
        check_xsd(ide_log, buff, Const.DAO_XSD);
        if (sett == null) {
            add_err_msg(ide_log, buff, "Test connection -> failed because of invalid settings");
        } else {
            try {
                Connection con = NbpHelpers.get_connection(obj);
                con.close();
                add_ok_msg(ide_log, buff, "Test connection");
            } catch (Exception ex) {
                add_err_msg(ide_log, buff, "Test connection: " + get_err_msg(ex));
            }
        }
        NbpIdeMessageHelpers.show_info_in_ui_thread(buff.toString());
    }

    private boolean check_xsd(NbpIdeConsoleUtil ide_log, StringBuilder buff, String xsd_name) {
        FileObject root_file = obj.getPrimaryFile();
        FileObject mp_folder = root_file.getParent();
        FileObject xml_file = mp_folder.getFileObject(xsd_name);
        if (xml_file == null) {
            add_err_msg(ide_log, buff, "File not found: " + xsd_name);
            return false;
        } else {
            String cur_text;
            try {
                cur_text = Helpers.load_text_from_file(xml_file.getPath());
            } catch (Exception ex) {
                add_err_msg(ide_log, buff, get_err_msg(ex));
                return false;
            }
            String ref_text;
            try {
                ref_text = NbpHelpers.read_from_jar_file(xsd_name);
            } catch (Exception ex) {
                add_err_msg(ide_log, buff, get_err_msg(ex));
                return false;
            }
            if (ref_text.equals(cur_text)) {
                add_ok_msg(ide_log, buff, xsd_name);
            } else {
                add_err_msg(ide_log, buff, xsd_name + " is out-of-date! Use 'Create/Overwrite XSD files'");
                return false;
            }
        }
        return true;
    }

    private void add_ok_msg(NbpIdeConsoleUtil ide_log, StringBuilder buff, String msg) {
        buff.append("\r\n");
        buff.append(msg);
        buff.append(" -> OK");
        ide_log.add_success_message(msg + " -> OK");
    }

    private void add_err_msg(NbpIdeConsoleUtil ide_log, StringBuilder buff, String msg) {
        buff.append("\r\n[ERROR] ");
        buff.append(msg);
        ide_log.add_error_message(msg);
    }
}
