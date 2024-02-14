/*
    Copyright 2011-2023 sqldalmaker@gmail.com
    SQL DAL Maker Website: https://sqldalmaker.sourceforge.net/
    Read LICENSE.txt in the root of this project/archive for details.
 */
package com.sqldalmaker.intellij.ui;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.sqldalmaker.cg.Helpers;
import com.sqldalmaker.common.Const;
import com.sqldalmaker.common.InternalException;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.*;

/**
 * Created with IntelliJ IDEA.
 * User: sqldalmaker@gmail.com
 * Date: 21.06.12
 */
public class UIDialogNewDaoXml extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JTextField textField1;
    private final Project project;
    private final VirtualFile propFile;

    public UIDialogNewDaoXml(Project project, VirtualFile propFile) {
        $$$setupUI$$$();

        this.project = project;
        this.propFile = propFile;

        setTitle("Create new DAO XML file");
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);

        buttonOK.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onOK();
            }
        });
        textField1.setText("Dao.xml");

        buttonCancel.addActionListener(new ActionListener() {
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

        textField1.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent e) {
                validateFileName();
            }

            public void removeUpdate(DocumentEvent e) {
                validateFileName();
            }

            public void insertUpdate(DocumentEvent e) {
                validateFileName();
            }
        });

        textField1.setCaretPosition(0);
    }

    private void validateFileName() {
        String name = textField1.getText();
        boolean res = name.endsWith(".xml");
        buttonOK.setEnabled(res);
    }

    private boolean success = false;

    private void onOK() {
        ApplicationManager.getApplication().runWriteAction(new Runnable() {
            @Override
            public void run() {
                try {
                    VirtualFile dir = propFile.getParent();
                    String dao_xml_file_name = textField1.getText();
                    VirtualFile f = dir.findChild(dao_xml_file_name);
                    if (f != null) {
                        throw new InternalException("Already exists: " + dao_xml_file_name);
                    }
                    f = dir.createChildData(null, dao_xml_file_name);
                    String xml = IdeaHelpers.read_from_jar_resources(Const.EMPTY_DAO_XML);
                    f.setBinaryContent(xml.getBytes());
                    IdeaHelpers.start_write_action_from_ui_thread_and_refresh_folder_sync(dir);
                    IdeaEditorHelpers.open_local_file_in_editor_sync(project, f);
                    success = true;
                    dispose();

                    String dao_class_name = Helpers.get_dao_class_name(dao_xml_file_name);
                    IdeaMessageHelpers.show_info_in_ui_thread(String.format("Now you need to register it in \"sdm.xml\":\r\n" +
                            "≤dao-class name=\"%s\" ref=\"%s\"/>", dao_class_name, dao_xml_file_name));

                } catch (Exception e) {
                    // e.printStackTrace();
                    IdeaMessageHelpers.show_error_in_ui_thread(e);
                }
            }
        });
    }

    private void onCancel() {
// add your code here if necessary
        dispose();
    }

    public boolean isSuccess() {
        return success;
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
        contentPane.setPreferredSize(new Dimension(300, 200));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new BorderLayout(0, 0));
        contentPane.add(panel1, BorderLayout.CENTER);
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new BorderLayout(0, 0));
        panel1.add(panel2, BorderLayout.NORTH);
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridBagLayout());
        panel1.add(panel3, BorderLayout.CENTER);
        final JLabel label1 = new JLabel();
        label1.setText("File name");
        GridBagConstraints gbc;
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weighty = 1.0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(20, 20, 20, 20);
        panel3.add(label1, gbc);
        textField1 = new JTextField();
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 0, 20);
        panel3.add(textField1, gbc);
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
        contentPane.add(panel4, BorderLayout.SOUTH);
        final JPanel panel5 = new JPanel();
        panel5.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
        panel4.add(panel5);
        buttonOK = new JButton();
        buttonOK.setText("OK");
        panel5.add(buttonOK);
        buttonCancel = new JButton();
        buttonCancel.setText("Cancel");
        panel5.add(buttonCancel);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return contentPane;
    }

}