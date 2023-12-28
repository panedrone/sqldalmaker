/*
    Copyright 2011-2023 sqldalmaker@gmail.com
    SQL DAL Maker Website: http://sqldalmaker.sourceforge.net
    Read LICENSE.txt in the root of this project/archive for details.
 */
package com.sqldalmaker.netbeans;

import com.sqldalmaker.cg.Helpers;
import com.sqldalmaker.common.Const;
import com.sqldalmaker.common.XmlHelpers;
import com.sqldalmaker.jaxb.settings.Settings;

import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.sql.Connection;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JToolBar;

import org.netbeans.core.spi.multiview.MultiViewElement;
import org.openide.filesystems.FileObject;
import org.openide.util.Lookup;
import org.openide.util.NbBundle.Messages;
import org.openide.windows.TopComponent;

/*
 * 18.12.2023 03:01 1.292
 * 12.05.2023 23:01 1.283
 * 23.02.2023 15:42 1.279
 * 30.10.2022 08:03 1.266
 * 08.05.2021 22:29 1.200
 * 29.06.2020 06:54
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
        String sdm_info = NbpHelpers.get_sdm_info(this.getClass());
        jTextField1.setText(sdm_info);
//        jTextField1.setBorder(javax.swing.BorderFactory.createEmptyBorder());

        jPanelMigrate.setVisible(false);

        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                check_need_sdm_migrate();
            }
        });
    }

    @Override
    public void componentActivated() {
        super.componentActivated();
    }

    private void check_need_sdm_migrate() {
        boolean need_migrate = false;
        FileObject root_file = obj.getPrimaryFile();
        FileObject mp_folder = root_file.getParent();
        FileObject xml_file = mp_folder.getFileObject(Const.SDM_XML);
        if (xml_file == null) {
            FileObject old_file = mp_folder.getFileObject("dto.xml");
            if (old_file != null) {
                need_migrate = true;
            }
            // ensureVisible(); === creates extra tab
        }
        set_need_migrate_warning(need_migrate);
    }

    private void ckeck_need_sdm() {
        FileObject root_file = obj.getPrimaryFile();
        FileObject mp_folder = root_file.getParent();
        boolean onlyExisting = true;
        FileObject xml_file = mp_folder.getFileObject(Const.SDM_XML, onlyExisting);
        if (xml_file != null) {
            set_need_migrate_warning(false);
        }
    }

    private void set_need_migrate_warning(boolean need_to_migrate) {
        if (need_to_migrate) {
            try {
                String text = NbpHelpers.read_from_jar_file("", "sdm.xml_how_to_migrate.txt");
                jTextPane1.setText(text);
            } catch (Exception ex) {
                jTextPane1.setText(ex.getMessage());
            }
        } else {
            jTextPane1.setText("");
        }
        jPanelMigrate.setVisible(need_to_migrate);
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
        NbpIdeEditorHelpers.open_sdm_folder_file_async(obj, Const.SETTINGS_XML);
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
        jButton6 = new javax.swing.JButton();
        jTextField1 = new javax.swing.JTextField();
        jButton13 = new javax.swing.JButton();
        jButton25 = new javax.swing.JButton();
        jPanel4 = new javax.swing.JPanel();
        jButton1 = new javax.swing.JButton();
        jButton3 = new javax.swing.JButton();
        jButton2 = new javax.swing.JButton();
        jPanel1 = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        jButton11 = new javax.swing.JButton();
        jButton28 = new javax.swing.JButton();
        jButton29 = new javax.swing.JButton();
        jButton33 = new javax.swing.JButton();
        jButton30 = new javax.swing.JButton();
        jButton26 = new javax.swing.JButton();
        jButton32 = new javax.swing.JButton();
        jButton27 = new javax.swing.JButton();
        jButton35 = new javax.swing.JButton();
        jPanel2 = new javax.swing.JPanel();
        jLabel8 = new javax.swing.JLabel();
        jButton4 = new javax.swing.JButton();
        jButton8 = new javax.swing.JButton();
        jButton36 = new javax.swing.JButton();
        jButton10 = new javax.swing.JButton();
        jButton17 = new javax.swing.JButton();
        jButton38 = new javax.swing.JButton();
        jLabel7 = new javax.swing.JLabel();
        jButton24 = new javax.swing.JButton();
        jButton14 = new javax.swing.JButton();
        jButton12 = new javax.swing.JButton();
        jButton37 = new javax.swing.JButton();
        jPanel9 = new javax.swing.JPanel();
        jLabel9 = new javax.swing.JLabel();
        jButton7 = new javax.swing.JButton();
        jButton42 = new javax.swing.JButton();
        jButton23 = new javax.swing.JButton();
        jButton41 = new javax.swing.JButton();
        jButton18 = new javax.swing.JButton();
        jButton40 = new javax.swing.JButton();
        jButton15 = new javax.swing.JButton();
        jButton9 = new javax.swing.JButton();
        jButton39 = new javax.swing.JButton();
        jPanel5 = new javax.swing.JPanel();
        jLabel11 = new javax.swing.JLabel();
        jButton16 = new javax.swing.JButton();
        jButton44 = new javax.swing.JButton();
        jButton34 = new javax.swing.JButton();
        jButton43 = new javax.swing.JButton();
        jButton45 = new javax.swing.JButton();
        jButton46 = new javax.swing.JButton();
        jPanel7 = new javax.swing.JPanel();
        jButton19 = new javax.swing.JButton();
        jButton20 = new javax.swing.JButton();
        jButton21 = new javax.swing.JButton();
        jButton22 = new javax.swing.JButton();
        jButton31 = new javax.swing.JButton();
        jPanelMigrate = new javax.swing.JPanel();
        jTextPane1 = new javax.swing.JTextPane();

        setName(""); // NOI18N
        setOpaque(false);
        setLayout(new java.awt.BorderLayout());

        jScrollPane1.setBorder(null);

        jPanel6.setToolTipText(org.openide.util.NbBundle.getMessage(SdmTabAdmin.class, "SdmTabAdmin.toolTipText")); // NOI18N
        jPanel6.setMinimumSize(new java.awt.Dimension(710, 560));
        jPanel6.setName(""); // NOI18N
        jPanel6.setPreferredSize(new java.awt.Dimension(710, 560));
        jPanel6.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                jPanel6MouseEntered(evt);
            }
        });
        jPanel6.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        jPanel8.setMinimumSize(new java.awt.Dimension(700, 560));
        jPanel8.setName(""); // NOI18N
        jPanel8.setPreferredSize(new java.awt.Dimension(700, 560));
        jPanel8.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        jPanel3.setMinimumSize(new java.awt.Dimension(710, 32));
        jPanel3.setPreferredSize(new java.awt.Dimension(710, 32));
        jPanel3.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        jButton5.setText("settings.xml");
        jButton5.setFocusPainted(false);
        jButton5.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton5ActionPerformed(evt);
            }
        });
        jPanel3.add(jButton5);

        jButton6.setText("Validate Configuration");
        jButton6.setFocusPainted(false);
        jButton6.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton6ActionPerformed(evt);
            }
        });
        jPanel3.add(jButton6);

        jTextField1.setEditable(false);
        jTextField1.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        jTextField1.setText("v. ?");
        jTextField1.setMinimumSize(new java.awt.Dimension(32, 22));
        jTextField1.setName(""); // NOI18N
        jPanel3.add(jTextField1);

        jButton13.setText("News");
        jButton13.setFocusPainted(false);
        jButton13.setPreferredSize(new java.awt.Dimension(64, 22));
        jButton13.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton13ActionPerformed(evt);
            }
        });
        jPanel3.add(jButton13);

        jButton25.setText("About");
        jButton25.setPreferredSize(new java.awt.Dimension(64, 22));
        jButton25.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton25ActionPerformed(evt);
            }
        });
        jPanel3.add(jButton25);

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

        jButton3.setText("Create 'sdm.xml'");
        jButton3.setActionCommand("Create sdm.xml");
        jButton3.setFocusPainted(false);
        jButton3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton3ActionPerformed(evt);
            }
        });
        jPanel4.add(jButton3);

        jButton2.setText("Create settings.xml");
        jButton2.setFocusPainted(false);
        jButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton2ActionPerformed(evt);
            }
        });
        jPanel4.add(jButton2);

        jPanel8.add(jPanel4);

        jPanel1.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        jLabel2.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel2.setText("PHP");
        jLabel2.setPreferredSize(new java.awt.Dimension(40, 14));
        jPanel1.add(jLabel2);

        jButton11.setText("sqlite3");
        jButton11.setFocusPainted(false);
        jButton11.setPreferredSize(new java.awt.Dimension(64, 22));
        jButton11.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton11ActionPerformed(evt);
            }
        });
        jPanel1.add(jButton11);

        jButton28.setText("mysql");
        jButton28.setPreferredSize(new java.awt.Dimension(64, 22));
        jButton28.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton28ActionPerformed(evt);
            }
        });
        jPanel1.add(jButton28);

        jButton29.setText("oracle");
        jButton29.setPreferredSize(new java.awt.Dimension(64, 22));
        jButton29.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton29ActionPerformed(evt);
            }
        });
        jPanel1.add(jButton29);

        jButton33.setText("oci8");
        jButton33.setPreferredSize(new java.awt.Dimension(64, 22));
        jButton33.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton33ActionPerformed(evt);
            }
        });
        jPanel1.add(jButton33);

        jButton30.setText("ms sql");
        jButton30.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton30ActionPerformed(evt);
            }
        });
        jPanel1.add(jButton30);

        jButton26.setText("postgres");
        jButton26.setFocusPainted(false);
        jButton26.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton26ActionPerformed(evt);
            }
        });
        jPanel1.add(jButton26);

        jButton32.setIcon(new javax.swing.ImageIcon(getClass().getResource("/img/xmldoc_12x12.gif"))); // NOI18N
        jButton32.setToolTipText("settings.xml");
        jButton32.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton32ActionPerformed(evt);
            }
        });
        jPanel1.add(jButton32);

        jButton27.setText("Doctrine");
        jButton27.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton27ActionPerformed(evt);
            }
        });
        jPanel1.add(jButton27);

        jButton35.setIcon(new javax.swing.ImageIcon(getClass().getResource("/img/xmldoc_12x12.gif"))); // NOI18N
        jButton35.setToolTipText("settings.xml");
        jButton35.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton35ActionPerformed(evt);
            }
        });
        jPanel1.add(jButton35);

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
        jButton4.setPreferredSize(new java.awt.Dimension(64, 22));
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
        jButton8.setPreferredSize(new java.awt.Dimension(64, 22));
        jButton8.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton8ActionPerformed(evt);
            }
        });
        jPanel2.add(jButton8);

        jButton36.setIcon(new javax.swing.ImageIcon(getClass().getResource("/img/xmldoc_12x12.gif"))); // NOI18N
        jButton36.setToolTipText("settings.xml");
        jButton36.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton36ActionPerformed(evt);
            }
        });
        jPanel2.add(jButton36);

        jButton10.setText("Base");
        jButton10.setFocusPainted(false);
        jButton10.setMaximumSize(new java.awt.Dimension(60, 22));
        jButton10.setMinimumSize(new java.awt.Dimension(60, 22));
        jButton10.setPreferredSize(new java.awt.Dimension(64, 22));
        jButton10.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton10ActionPerformed(evt);
            }
        });
        jPanel2.add(jButton10);

        jButton17.setText("Android");
        jButton17.setFocusPainted(false);
        jButton17.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton17ActionPerformed(evt);
            }
        });
        jPanel2.add(jButton17);

        jButton38.setIcon(new javax.swing.ImageIcon(getClass().getResource("/img/xmldoc_12x12.gif"))); // NOI18N
        jButton38.setToolTipText("settings.xml");
        jButton38.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton38ActionPerformed(evt);
            }
        });
        jPanel2.add(jButton38);

        jLabel7.setText("C++");
        jPanel2.add(jLabel7);

        jButton24.setText("Qt");
        jButton24.setFocusPainted(false);
        jButton24.setPreferredSize(new java.awt.Dimension(64, 22));
        jButton24.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton24ActionPerformed(evt);
            }
        });
        jPanel2.add(jButton24);

        jButton14.setText("stl, sqlite3");
        jButton14.setFocusPainted(false);
        jButton14.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton14ActionPerformed(evt);
            }
        });
        jPanel2.add(jButton14);

        jButton12.setText("atl, sqlite3");
        jButton12.setFocusPainted(false);
        jButton12.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton12ActionPerformed(evt);
            }
        });
        jPanel2.add(jButton12);

        jButton37.setIcon(new javax.swing.ImageIcon(getClass().getResource("/img/xmldoc_12x12.gif"))); // NOI18N
        jButton37.setToolTipText("settings.xml");
        jButton37.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton37ActionPerformed(evt);
            }
        });
        jPanel2.add(jButton37);

        jPanel8.add(jPanel2);

        jPanel9.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        jLabel9.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel9.setText("Python");
        jLabel9.setPreferredSize(new java.awt.Dimension(40, 14));
        jPanel9.add(jLabel9);

        jButton7.setText("sqlite3, mysql, psycopg2 ");
        jButton7.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton7ActionPerformed(evt);
            }
        });
        jPanel9.add(jButton7);

        jButton42.setIcon(new javax.swing.ImageIcon(getClass().getResource("/img/xmldoc_12x12.gif"))); // NOI18N
        jButton42.setToolTipText("settings.xml");
        jButton42.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton42ActionPerformed(evt);
            }
        });
        jPanel9.add(jButton42);

        jButton23.setText("cx_oracle");
        jButton23.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton23ActionPerformed(evt);
            }
        });
        jPanel9.add(jButton23);

        jButton41.setIcon(new javax.swing.ImageIcon(getClass().getResource("/img/xmldoc_12x12.gif"))); // NOI18N
        jButton41.setToolTipText("settings.xml");
        jButton41.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton41ActionPerformed(evt);
            }
        });
        jPanel9.add(jButton41);

        jButton18.setText("django.db");
        jButton18.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton18ActionPerformed(evt);
            }
        });
        jPanel9.add(jButton18);

        jButton40.setIcon(new javax.swing.ImageIcon(getClass().getResource("/img/xmldoc_12x12.gif"))); // NOI18N
        jButton40.setToolTipText("settings.xml");
        jButton40.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton40ActionPerformed(evt);
            }
        });
        jPanel9.add(jButton40);

        jButton15.setText("sqlalchemy");
        jButton15.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton15ActionPerformed(evt);
            }
        });
        jPanel9.add(jButton15);

        jButton9.setText("flask-sqlalchemy");
        jButton9.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton9ActionPerformed(evt);
            }
        });
        jPanel9.add(jButton9);

        jButton39.setIcon(new javax.swing.ImageIcon(getClass().getResource("/img/xmldoc_12x12.gif"))); // NOI18N
        jButton39.setToolTipText("settings.xml");
        jButton39.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton39ActionPerformed(evt);
            }
        });
        jPanel9.add(jButton39);

        jPanel8.add(jPanel9);

        jPanel5.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        jLabel11.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel11.setText("Go");
        jLabel11.setPreferredSize(new java.awt.Dimension(40, 14));
        jPanel5.add(jLabel11);

        jButton16.setText("gorm");
        jButton16.setPreferredSize(new java.awt.Dimension(64, 22));
        jButton16.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton16ActionPerformed(evt);
            }
        });
        jPanel5.add(jButton16);

        jButton44.setIcon(new javax.swing.ImageIcon(getClass().getResource("/img/xmldoc_12x12.gif"))); // NOI18N
        jButton44.setToolTipText("settings.xml");
        jButton44.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton44ActionPerformed(evt);
            }
        });
        jPanel5.add(jButton44);

        jButton34.setText("database/sql");
        jButton34.setToolTipText("");
        jButton34.setFocusPainted(false);
        jButton34.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton34ActionPerformed(evt);
            }
        });
        jPanel5.add(jButton34);

        jButton43.setIcon(new javax.swing.ImageIcon(getClass().getResource("/img/xmldoc_12x12.gif"))); // NOI18N
        jButton43.setToolTipText("settings.xml");
        jButton43.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton43ActionPerformed(evt);
            }
        });
        jPanel5.add(jButton43);

        jButton45.setText("sqlx");
        jButton45.setPreferredSize(new java.awt.Dimension(64, 22));
        jButton45.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton45ActionPerformed(evt);
            }
        });
        jPanel5.add(jButton45);

        jButton46.setIcon(new javax.swing.ImageIcon(getClass().getResource("/img/xmldoc_12x12.gif"))); // NOI18N
        jButton46.setToolTipText("settings.xml");
        jButton46.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton46ActionPerformed(evt);
            }
        });
        jPanel5.add(jButton46);

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

        jButton31.setText("go.vm");
        jButton31.setFocusPainted(false);
        jButton31.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton31ActionPerformed(evt);
            }
        });
        jPanel7.add(jButton31);

        jPanel8.add(jPanel7);

        jPanelMigrate.setLayout(new java.awt.BorderLayout());

        jTextPane1.setText("=== migrate ===");
        jTextPane1.setMargin(new java.awt.Insets(0, 20, 0, 20));
        jPanelMigrate.add(jTextPane1, java.awt.BorderLayout.CENTER);

        jPanel8.add(jPanelMigrate);

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
        NbpIdeEditorHelpers.open_resource_file_in_editor_async("data_store_no_orm.go", "data_store_no_orm.go");
    }//GEN-LAST:event_jButton34ActionPerformed

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
        NbpIdeEditorHelpers.open_resource_file_in_editor_async("DataStore.java", "DataStore_JDBC.java_");
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
        NbpIdeEditorHelpers.open_resource_file_in_editor_async("DataStore.java", "DataStore_Android.java_");
    }//GEN-LAST:event_jButton10ActionPerformed

    private void jButton31ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton31ActionPerformed
        NbpIdeEditorHelpers.open_resource_file_in_editor_async("go.vm", "com/sqldalmaker/cg/go", "go.vm");
    }//GEN-LAST:event_jButton31ActionPerformed

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

        int dialogResult = JOptionPane.showConfirmDialog(null, "Create/overwrite 'sdm.xml'?",
                "Warning", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (dialogResult == JOptionPane.YES_OPTION) {
            NbpMetaProgramInitHelpers.copy_dto_xml(obj);
        }
    }//GEN-LAST:event_jButton3ActionPerformed

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed

        int dialogResult = JOptionPane.showConfirmDialog(null, "Create/overwrite 'settings.xml'?",
                "Warning", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (dialogResult == JOptionPane.YES_OPTION) {
            NbpMetaProgramInitHelpers.copy_settings_xml(obj);
        }
    }//GEN-LAST:event_jButton2ActionPerformed

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed

        int dialogResult = JOptionPane.showConfirmDialog(null, "Create/overwrite xsd files?",
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

    private void jButton5ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton5ActionPerformed
        editSettingsXml();
    }//GEN-LAST:event_jButton5ActionPerformed

    private void jButton7ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton7ActionPerformed
        NbpIdeEditorHelpers.open_resource_file_in_editor_async("data_store.py", "data_store_no_orm.py");
    }//GEN-LAST:event_jButton7ActionPerformed

    private void jButton15ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton15ActionPerformed
        NbpIdeEditorHelpers.open_resource_file_in_editor_async("data_store.py", "data_store_sqlalchemy.py");
    }//GEN-LAST:event_jButton15ActionPerformed

    private void jButton16ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton16ActionPerformed
        NbpIdeEditorHelpers.open_resource_file_in_editor_async("data_store_gorm.go", "data_store_gorm.go");
    }//GEN-LAST:event_jButton16ActionPerformed

    private void jButton27ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton27ActionPerformed
        NbpIdeEditorHelpers.open_resource_file_in_editor_async("DataStore_Doctrine_ORM.php", "DataStore_Doctrine_ORM.php");
    }//GEN-LAST:event_jButton27ActionPerformed

    private void jButton23ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton23ActionPerformed
        NbpIdeEditorHelpers.open_resource_file_in_editor_async("data_store.py", "data_store_no_orm_cx_oracle.py");
    }//GEN-LAST:event_jButton23ActionPerformed

    private void jButton18ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton18ActionPerformed
        NbpIdeEditorHelpers.open_resource_file_in_editor_async("data_store.py", "data_store_django.py");
    }//GEN-LAST:event_jButton18ActionPerformed

    private void jButton9ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton9ActionPerformed
        NbpIdeEditorHelpers.open_resource_file_in_editor_async("data_store.py", "data_store_flask_sqlalchemy.py");
    }//GEN-LAST:event_jButton9ActionPerformed

    private void jButton42ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton42ActionPerformed
        NbpIdeEditorHelpers.open_resource_file_in_editor_async(Const.SETTINGS_XML, "data_store_no_orm.py.settings.xml");
    }//GEN-LAST:event_jButton42ActionPerformed

    private void jButton41ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton41ActionPerformed
        NbpIdeEditorHelpers.open_resource_file_in_editor_async(Const.SETTINGS_XML, "data_store_no_orm_cx_oracle.py.settings.xml");
    }//GEN-LAST:event_jButton41ActionPerformed

    private void jButton40ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton40ActionPerformed
        NbpIdeEditorHelpers.open_resource_file_in_editor_async(Const.SETTINGS_XML, "data_store_django.py.settings.xml");
    }//GEN-LAST:event_jButton40ActionPerformed

    private void jButton39ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton39ActionPerformed
        NbpIdeEditorHelpers.open_resource_file_in_editor_async(Const.SETTINGS_XML, "data_store_sqlalchemy.py.settings.xml");
    }//GEN-LAST:event_jButton39ActionPerformed

    private void jButton44ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton44ActionPerformed
        NbpIdeEditorHelpers.open_resource_file_in_editor_async(Const.SETTINGS_XML, "data_store_gorm.go.settings.xml");
    }//GEN-LAST:event_jButton44ActionPerformed

    private void jButton43ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton43ActionPerformed
        NbpIdeEditorHelpers.open_resource_file_in_editor_async(Const.SETTINGS_XML, "data_store_no_orm.go.settings.xml");
    }//GEN-LAST:event_jButton43ActionPerformed

    private void jButton35ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton35ActionPerformed
        NbpIdeEditorHelpers.open_resource_file_in_editor_async(Const.SETTINGS_XML, "DataStore_Doctrine_ORM.php.settings.xml");
    }//GEN-LAST:event_jButton35ActionPerformed

    private void jButton32ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton32ActionPerformed
        NbpIdeEditorHelpers.open_resource_file_in_editor_async(Const.SETTINGS_XML, "DataStore_PDO_SQLite3.php.settings.xml");
    }//GEN-LAST:event_jButton32ActionPerformed

    private void jButton36ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton36ActionPerformed
        NbpIdeEditorHelpers.open_resource_file_in_editor_async(Const.SETTINGS_XML, "DataStore.java.JDBC.settings.xml");
    }//GEN-LAST:event_jButton36ActionPerformed

    private void jButton38ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton38ActionPerformed
        NbpIdeEditorHelpers.open_resource_file_in_editor_async(Const.SETTINGS_XML, "DataStore.java.android.settings.xml");
    }//GEN-LAST:event_jButton38ActionPerformed

    private void jButton37ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton37ActionPerformed
        NbpIdeEditorHelpers.open_resource_file_in_editor_async(Const.SETTINGS_XML, "DataStore.cpp.settings.xml");
    }//GEN-LAST:event_jButton37ActionPerformed

    private void jButton25ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton25ActionPerformed
        UIDialogAbout.show_modal();
    }//GEN-LAST:event_jButton25ActionPerformed

    private void jButton46ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton46ActionPerformed
        NbpIdeEditorHelpers.open_resource_file_in_editor_async(Const.SETTINGS_XML, "data_store_sqlx.go.settings.xml");
    }//GEN-LAST:event_jButton46ActionPerformed

    private void jButton45ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton45ActionPerformed
        NbpIdeEditorHelpers.open_resource_file_in_editor_async("data_store_sqlx.go", "data_store_sqlx.go");
    }//GEN-LAST:event_jButton45ActionPerformed

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
    private javax.swing.JButton jButton26;
    private javax.swing.JButton jButton27;
    private javax.swing.JButton jButton28;
    private javax.swing.JButton jButton29;
    private javax.swing.JButton jButton3;
    private javax.swing.JButton jButton30;
    private javax.swing.JButton jButton31;
    private javax.swing.JButton jButton32;
    private javax.swing.JButton jButton33;
    private javax.swing.JButton jButton34;
    private javax.swing.JButton jButton35;
    private javax.swing.JButton jButton36;
    private javax.swing.JButton jButton37;
    private javax.swing.JButton jButton38;
    private javax.swing.JButton jButton39;
    private javax.swing.JButton jButton4;
    private javax.swing.JButton jButton40;
    private javax.swing.JButton jButton41;
    private javax.swing.JButton jButton42;
    private javax.swing.JButton jButton43;
    private javax.swing.JButton jButton44;
    private javax.swing.JButton jButton45;
    private javax.swing.JButton jButton46;
    private javax.swing.JButton jButton5;
    private javax.swing.JButton jButton6;
    private javax.swing.JButton jButton7;
    private javax.swing.JButton jButton8;
    private javax.swing.JButton jButton9;
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
    private javax.swing.JPanel jPanelMigrate;
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
        FileObject root_file = obj.getPrimaryFile();

        ckeck_need_sdm();

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
        check_xsd(ide_log, buff, Const.SDM_XSD);
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
            String cur_xsd;
            try {
                cur_xsd = Helpers.load_text_from_file(xml_file.getPath());
            } catch (Exception ex) {
                add_err_msg(ide_log, buff, get_err_msg(ex));
                return false;
            }
            String ref_xsd;
            try {
                ref_xsd = NbpHelpers.read_from_jar_file(xsd_name);
            } catch (Exception ex) {
                add_err_msg(ide_log, buff, get_err_msg(ex));
                return false;
            }
            if (XmlHelpers.compareXml(ref_xsd, cur_xsd) == 0) {
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
