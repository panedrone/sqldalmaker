/*
    Copyright 2011-2024 sqldalmaker@gmail.com
    SQL DAL Maker Website: https://sqldalmaker.sourceforge.net/
    Read LICENSE.txt in the root of this project/archive for details.
 */
package com.sqldalmaker.intellij.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.sqldalmaker.cg.JdbcUtils;
import com.sqldalmaker.common.ISelectDbSchemaCallback;
import com.sqldalmaker.jaxb.settings.Settings;

import javax.swing.*;
import javax.swing.plaf.FontUIResource;
import javax.swing.table.AbstractTableModel;
import javax.swing.text.StyleContext;
import java.awt.*;
import java.awt.event.*;
import java.sql.Connection;
import java.util.Locale;

/**
 * Created with IntelliJ IDEA.
 * User: sqldalmaker@gmail.com
 * Date: 21.06.12
 */
public class UIDialogSelectDbSchema extends JDialog {

    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JTable table;
    private JCheckBox chk_omit;
    private JCheckBox chk_singular;
    private JRadioButton crudDetailedRadioButton;
    private JRadioButton crudAutoRadioButton;
    private JPanel radioPanel;
    private JCheckBox chk_add_fk_access;
    private JCheckBox chk_including_views;
    private JCheckBox chk_schema_in_xml;
    private JRadioButton radio_selected_schema;
    private JRadioButton radio_user_as_schema;
    private JLabel lbl_jdbc_url;

    private final Project project;
    private final Settings settings;

    private String selected_schema = null;

    private UIDialogSelectDbSchema(Project project, VirtualFile profile, ISelectDbSchemaCallback callback, boolean dto, boolean fk) throws Exception {
        $$$setupUI$$$();
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);
        this.project = project;
        this.callback = callback;
        this.settings = IdeaHelpers.load_settings(profile);
        setTitle("Select a schema and provide options");
        lbl_jdbc_url.setText(settings.getJdbc().getUrl());
        buttonOK.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onOK();
            }
        });
        buttonCancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        });
        table.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    onOK();
                } else if (e.getClickCount() == 1) {
                    on_selection_changed();
                }
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
        if (dto || fk) {
            this.radioPanel.setVisible(false);
            chk_add_fk_access.setVisible(false);
        }
        if (fk) {
            this.chk_omit.setVisible(false);
            this.chk_including_views.setVisible(false);
        }
        radio_selected_schema.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                on_selection_changed();
            }
        });
        radio_user_as_schema.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                on_selection_changed();
            }
        });
        refresh();
    }

    private void on_selection_changed() {
        boolean enabled = false;
        if (radio_user_as_schema.isSelected()) {
            enabled = true;
            chk_schema_in_xml.setEnabled(true);
            String user = settings.getJdbc().getUser();
            selected_schema = user;
        } else if (radio_selected_schema.isSelected()) {
            if (table.getRowCount() == 0) {
                chk_schema_in_xml.setSelected(false);
                chk_schema_in_xml.setEnabled(false);
                enabled = true;
                selected_schema = null;
            } else {
                chk_schema_in_xml.setEnabled(true);
                if (table.getRowCount() == 1) {
                    enabled = true;
                    selected_schema = null;
                } else {
                    int[] indexes = getSelection();
                    enabled = indexes.length == 1;
                    if (enabled) {
                        selected_schema = (String) table.getModel().getValueAt(indexes[0], 0);
                    }
                }
            }
        }
        buttonOK.setEnabled(enabled);
    }

    private int[] getSelection() {
        int rc = table.getModel().getRowCount();
        if (rc == 1) {
            return new int[]{0};
        }
        int[] selected_rows = table.getSelectedRows();
        return selected_rows;
    }

    private void onOK() {
        callback.process_ok(chk_schema_in_xml.isSelected(), selected_schema,
                chk_omit.isSelected(), chk_including_views.isSelected(), chk_singular.isSelected(),
                crudAutoRadioButton.isSelected(), chk_add_fk_access.isSelected());
        dispose();
    }

    private void onCancel() {
// add your code here if necessary
        dispose();
    }

    private final ISelectDbSchemaCallback callback;

    static void open(Project project, VirtualFile profile, ISelectDbSchemaCallback callback, boolean dto, boolean fk) throws Exception {
        UIDialogSelectDbSchema dlg = new UIDialogSelectDbSchema(project, profile, callback, dto, fk);
        // dlg.setPreferredSize(new Dimension(520, 280));
        dlg.pack(); // after setPreferredSize
        dlg.setLocationRelativeTo(null);  // after pack!!!
        dlg.setVisible(true);
    }

    private void refresh() {
        try {
            Connection con = IdeaHelpers.get_connection(project, settings);
            try {
                table.setTableHeader(null);
                java.util.List<String> items = JdbcUtils.get_schema_names(con);
                table.setModel(new AbstractTableModel() {
                    public Object getValueAt(int rowIndex, int columnIndex) {
                        return items.get(rowIndex);
                    }

                    public int getColumnCount() {
                        return 1;
                    }

                    public String getColumnName(int column) {
                        return "Schema";
                    }

                    public int getRowCount() {
                        return items.size();
                    }
                });
                if (items.isEmpty()) {
                    radio_selected_schema.setText("Without schema");
                } else {
                    radio_selected_schema.setText("Use selected schema");
                    // chk_schema_in_xml.setSelected(true);
                }
            } finally {
                con.close();
            }
            on_selection_changed();
        } catch (Exception e) {
            // e.printStackTrace();
            IdeaMessageHelpers.show_error_in_ui_thread(e);
        }
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
        contentPane.setPreferredSize(new Dimension(500, 600));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new BorderLayout(0, 0));
        contentPane.add(panel1, BorderLayout.CENTER);
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridBagLayout());
        panel1.add(panel2, BorderLayout.NORTH);
        lbl_jdbc_url = new JLabel();
        Font lbl_jdbc_urlFont = this.$$$getFont$$$(null, Font.PLAIN, 14, lbl_jdbc_url.getFont());
        if (lbl_jdbc_urlFont != null) lbl_jdbc_url.setFont(lbl_jdbc_urlFont);
        lbl_jdbc_url.setText("Select a schema and specify the options...");
        GridBagConstraints gbc;
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.insets = new Insets(10, 0, 10, 0);
        panel2.add(lbl_jdbc_url, gbc);
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new BorderLayout(0, 0));
        panel1.add(panel3, BorderLayout.CENTER);
        final JScrollPane scrollPane1 = new JScrollPane();
        scrollPane1.setPreferredSize(new Dimension(453, 300));
        panel3.add(scrollPane1, BorderLayout.CENTER);
        table = new JTable();
        scrollPane1.setViewportView(table);
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new GridBagLayout());
        panel3.add(panel4, BorderLayout.SOUTH);
        final JPanel panel5 = new JPanel();
        panel5.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 5));
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        panel4.add(panel5, gbc);
        radio_selected_schema = new JRadioButton();
        radio_selected_schema.setSelected(true);
        radio_selected_schema.setText("Use selected schema");
        panel5.add(radio_selected_schema);
        radio_user_as_schema = new JRadioButton();
        radio_user_as_schema.setText("DB user name as schema");
        panel5.add(radio_user_as_schema);
        chk_omit = new JCheckBox();
        chk_omit.setSelected(true);
        chk_omit.setText("Omit DTO that are already declared");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 1.0;
        gbc.anchor = GridBagConstraints.WEST;
        panel4.add(chk_omit, gbc);
        chk_singular = new JCheckBox();
        chk_singular.setSelected(true);
        chk_singular.setText("English plural to English singular for DTO class names");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 1.0;
        gbc.anchor = GridBagConstraints.WEST;
        panel4.add(chk_singular, gbc);
        radioPanel = new JPanel();
        radioPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 5));
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        panel4.add(radioPanel, gbc);
        crudDetailedRadioButton = new JRadioButton();
        crudDetailedRadioButton.setSelected(true);
        crudDetailedRadioButton.setText("crud detailed");
        radioPanel.add(crudDetailedRadioButton);
        crudAutoRadioButton = new JRadioButton();
        crudAutoRadioButton.setText("crud empty");
        radioPanel.add(crudAutoRadioButton);
        chk_add_fk_access = new JCheckBox();
        chk_add_fk_access.setSelected(true);
        chk_add_fk_access.setText("Including FK access code");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.weightx = 1.0;
        gbc.anchor = GridBagConstraints.WEST;
        panel4.add(chk_add_fk_access, gbc);
        chk_including_views = new JCheckBox();
        chk_including_views.setSelected(true);
        chk_including_views.setText("Including views");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.weightx = 1.0;
        gbc.anchor = GridBagConstraints.WEST;
        panel4.add(chk_including_views, gbc);
        chk_schema_in_xml = new JCheckBox();
        chk_schema_in_xml.setSelected(false);
        chk_schema_in_xml.setText("Schema in generated XML declarations");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 6;
        gbc.weightx = 1.0;
        gbc.anchor = GridBagConstraints.WEST;
        panel4.add(chk_schema_in_xml, gbc);
        final JPanel panel6 = new JPanel();
        panel6.setLayout(new GridBagLayout());
        contentPane.add(panel6, BorderLayout.SOUTH);
        final JPanel panel7 = new JPanel();
        panel7.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        panel6.add(panel7, gbc);
        buttonOK = new JButton();
        buttonOK.setText("OK");
        panel7.add(buttonOK);
        buttonCancel = new JButton();
        buttonCancel.setText("Cancel");
        panel7.add(buttonCancel);
        ButtonGroup buttonGroup;
        buttonGroup = new ButtonGroup();
        buttonGroup.add(crudAutoRadioButton);
        buttonGroup.add(crudDetailedRadioButton);
        buttonGroup = new ButtonGroup();
        buttonGroup.add(radio_user_as_schema);
        buttonGroup.add(radio_selected_schema);
    }

    /**
     * @noinspection ALL
     */
    private Font $$$getFont$$$(String fontName, int style, int size, Font currentFont) {
        if (currentFont == null) return null;
        String resultName;
        if (fontName == null) {
            resultName = currentFont.getName();
        } else {
            Font testFont = new Font(fontName, Font.PLAIN, 10);
            if (testFont.canDisplay('a') && testFont.canDisplay('1')) {
                resultName = fontName;
            } else {
                resultName = currentFont.getName();
            }
        }
        Font font = new Font(resultName, style >= 0 ? style : currentFont.getStyle(), size >= 0 ? size : currentFont.getSize());
        boolean isMac = System.getProperty("os.name", "").toLowerCase(Locale.ENGLISH).startsWith("mac");
        Font fontWithFallback = isMac ? new Font(font.getFamily(), font.getStyle(), font.getSize()) : new StyleContext().getFont(font.getFamily(), font.getStyle(), font.getSize());
        return fontWithFallback instanceof FontUIResource ? fontWithFallback : new FontUIResource(fontWithFallback);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return contentPane;
    }

}