/*
    Copyright 2011-2024 sqldalmaker@gmail.com
    SQL DAL Maker Website: https://sqldalmaker.sourceforge.net/
    Read LICENSE.txt in the root of this project/archive for details.
 */
package com.sqldalmaker.intellij.ui;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import java.awt.*;
import java.awt.event.*;

/**
 * @author panedrone
 *
 * 01.09.2023 12:21 1.287
 *
 */
public class UIDialogAbout extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JTextPane text1;

    public static void show_modal() {
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                UIDialogAbout dialog = new UIDialogAbout(new JFrame(), true);
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

    private UIDialogAbout(Frame parent, boolean modal) {
        super(parent, modal);
        setContentPane(contentPane);
        // setModal(true);
        setTitle("About");
        getRootPane().setDefaultButton(buttonOK);

        buttonOK.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        });
// call onCancel() when cross is clicked
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

// call onCancel() on ESCAPE
        contentPane.registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        try {
            text1.setContentType("text/html");
            text1.setEditable(false);

            String text = IdeaHelpers.read_from_jar("", "ABOUT.html");
            String sdm_info = IdeaHelpers.get_sdm_info();
            text = String.format(text, sdm_info);
            text1.setText(text);
            text1.addHyperlinkListener(new HyperlinkListener() {
                @Override
                public void hyperlinkUpdate(HyperlinkEvent hle) {
                    if (HyperlinkEvent.EventType.ACTIVATED.equals(hle.getEventType())) {
                        // System.out.println(hle.getURL());
                        if (Desktop.isDesktopSupported()) {
                            try {
                                Desktop desktop = Desktop.getDesktop();
                                desktop.browse(hle.getURL().toURI());
                            } catch (Exception ex) {
                                // ex.printStackTrace();
                            }
                        }
                    }
                }
            });
        } catch (Exception e) {
            // e.printStackTrace();
        }
    }

    private void onCancel() {
        // add your code here
        dispose();
    }

    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        contentPane = new JPanel();
        contentPane.setLayout(new BorderLayout(0, 0));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new BorderLayout(0, 0));
        contentPane.add(panel1, BorderLayout.CENTER);
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new BorderLayout(0, 0));
        panel1.add(panel2, BorderLayout.CENTER);
        text1 = new JTextPane();
        text1.setEditable(false);
        text1.setMargin(new Insets(0, 15, 15, 15));
        text1.setText("");
        panel2.add(text1, BorderLayout.CENTER);
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new FlowLayout(FlowLayout.RIGHT, 5, 5));
        contentPane.add(panel3, BorderLayout.SOUTH);
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new GridBagLayout());
        panel3.add(panel4);
        buttonOK = new JButton();
        buttonOK.setText("OK");
        GridBagConstraints gbc;
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel4.add(buttonOK, gbc);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return contentPane;
    }

}
