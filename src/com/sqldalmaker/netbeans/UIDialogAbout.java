/*
    Copyright 2011-2023 sqldalmaker@gmail.com
    SQL DAL Maker Website: http://sqldalmaker.sourceforge.net
    Read LICENSE.txt in the root of this project/archive for details.
 */
package com.sqldalmaker.netbeans;

import java.awt.Desktop;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.net.URISyntaxException;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import static javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

/**
 * @author panedrone
 */
public class UIDialogAbout extends javax.swing.JDialog {

    public static void show_modal() {

        java.awt.EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                UIDialogAbout dialog = new UIDialogAbout(new javax.swing.JFrame(), true);

                // dialog.setPreferredSize(new Dimension(720, 300)); // don't !!!
                // How to completely remove an icon from JDialog?
                // https://stackoverflow.com/questions/8504731/how-to-completely-remove-an-icon-from-jdialog
                dialog.setResizable(false);
                dialog.pack(); // after setPreferredSize

                dialog.setLocationRelativeTo(null);  // after pack!!!                
                dialog.setVisible(true);
            }
        });
    }

    private UIDialogAbout(java.awt.Frame parent, boolean modal) {
        super(parent, modal);
        initComponents();

        setModal(true);
        getRootPane().setDefaultButton(button_ok);

        setTitle("About");

        button_ok.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        });

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

        jTextPane1.setContentType("text/html");
        jTextPane1.setEditable(false);

        try {
            String text = NbpHelpers.read_from_jar_file("", "ABOUT.html");
            String sdm_info = NbpHelpers.get_sdm_info(this.getClass());
            text = String.format(text, sdm_info);
            jTextPane1.setText(text);
        } catch (Exception ex) {
            NbpIdeMessageHelpers.show_error_in_ui_thread(ex);
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
                        } catch (IOException | URISyntaxException ex) {
                            NbpIdeMessageHelpers.show_error_in_ui_thread(ex);
                        }
                    }
                }
            }
        });
    }

    private void onCancel() {
        dispose();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel5 = new javax.swing.JPanel();
        button_ok = new javax.swing.JButton();
        jPanel1 = new javax.swing.JPanel();
        jPanel2 = new javax.swing.JPanel();
        jTextPane1 = new javax.swing.JTextPane();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

        jPanel5.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));

        org.openide.awt.Mnemonics.setLocalizedText(button_ok, org.openide.util.NbBundle.getMessage(UIDialogAbout.class, "UIDialogAbout.button_ok.text")); // NOI18N
        jPanel5.add(button_ok);

        getContentPane().add(jPanel5, java.awt.BorderLayout.SOUTH);

        jPanel1.setLayout(new java.awt.BorderLayout());

        jPanel2.setLayout(new java.awt.BorderLayout());

        jTextPane1.setMargin(new java.awt.Insets(0, 20, 0, 20));
        jPanel2.add(jTextPane1, java.awt.BorderLayout.CENTER);

        jPanel1.add(jPanel2, java.awt.BorderLayout.CENTER);

        getContentPane().add(jPanel1, java.awt.BorderLayout.CENTER);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton button_ok;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JTextPane jTextPane1;
    // End of variables declaration//GEN-END:variables
}
