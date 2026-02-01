package com.sqldalmaker.intellij.ui;

import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.JBColor;
import com.sqldalmaker.cg.IDaoCG;
import com.sqldalmaker.cg.JaxbUtils;
import com.sqldalmaker.common.Const;
import com.sqldalmaker.jaxb.sdm.DaoClass;
import com.sqldalmaker.jaxb.settings.Settings;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.*;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: sqldalmaker@gmail.com
 * Date: 21.06.12
 */
public class UITabDAO {
    private JButton btn_NewXML;
    private JTable table;
    private JPanel rootPanel;
    private JButton btn_Refresh;
    private JButton btn_Generate;
    private JButton btn_Validate;
    private JButton btn_goto_detailed_dao_xml;
    private JButton btn_OpenJava;
    private JButton btn_CrudDao;
    private JPanel top_panel_1;
    private JButton button_fk_assistant;
    private JPanel tool_panel;
    private JButton open_sdm_xml;

    private Project project;
    private VirtualFile root_file;
    private MyDaoTableModel dao_table_model;

    private final int COL_INDEX_NAME = 0;
    private final int COL_INDEX_REF = 1;
    private final int COL_INDEX_STATUS = 2;

    public UITabDAO() {
        $$$setupUI$$$();
        {
            btn_Refresh.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    reload_table(true);
                }
            });
            Icon icon = IconLoader.getIcon("/img/restartKernel.svg", UITabDAO.class);
            btn_Refresh.setIcon(icon);
        }
        {
            btn_Validate.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    validate_all_with_progress_sync();
                }
            });
            Icon icon = IconLoader.getIcon("/img/validator.svg", UITabDAO.class);
            btn_Validate.setIcon(icon);
        }
        {
            btn_Generate.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    generate_for_dao_selected_in_ui_with_progress_sync();
                }
            });
            Icon icon = IconLoader.getIcon("/img/buildOnFrameDeactivation.svg", UITabDAO.class);
            btn_Generate.setIcon(icon);
        }
        {
            btn_goto_detailed_dao_xml.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    int[] selected_rows = get_ui_table_sel_indexes();
                    if (selected_rows.length == 0) {
                        open_sdm_xml_async();
                        return;
                    }
                    open_detailed_dao_xml_async(selected_rows[0]);
                }
            });
            Icon icon = IconLoader.getIcon("/img/SQLDMLStatement.svg", UITabDAO.class);
            btn_goto_detailed_dao_xml.setIcon(icon);
        }
        {
            btn_OpenJava.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    int[] selected_rows = get_ui_table_sel_indexes();
                    if (selected_rows.length == 0) {
                        return;
                    }
                    open_target_file_async(selected_rows[0]);
                }
            });
            Icon icon = IconLoader.getIcon("/img/toolWindowJsonPath.svg", UITabDAO.class);
            btn_OpenJava.setIcon(icon);
        }
        {
            btn_NewXML.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    new_dao_xml();
                }
            });
            Icon icon = IconLoader.getIcon("/img/addFile.svg", UITabDAO.class);
            btn_NewXML.setIcon(icon);
        }
        {
            btn_CrudDao.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    crud_dao_wizard();
                }
            });
            Icon icon = IconLoader.getIcon("/img/importDataCell.svg", UITabDAO.class);
            btn_CrudDao.setIcon(icon);
        }
        {
            button_fk_assistant.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    IdeaCrudXmlHelpers.get_fk_access_xml(project, root_file);
                }
            });
            Icon icon = IconLoader.getIcon("/img/insightNavigate.svg", UITabDAO.class);
            button_fk_assistant.setIcon(icon);
        }
        {
            open_sdm_xml.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    int[] selected_rows = get_ui_table_sel_indexes();
                    if (selected_rows.length > 0) {
                        String dao_class_name = (String) table.getValueAt(selected_rows[0], COL_INDEX_NAME);
                        if (IdeaHelpers.navigate_to_sdm_xml_dao_class_by_name(project, root_file, dao_class_name)) {
                            return;
                        }
                    }
                    open_sdm_xml_async();
                }
            });
            Icon icon = IconLoader.getIcon("/img/dataStructure.svg", UITabDAO.class);
            open_sdm_xml.setIcon(icon);
        }
        Cursor wc = new Cursor(Cursor.HAND_CURSOR);
        tool_panel.setOpaque(false);
        for (Component c : tool_panel.getComponents()) {
            if (c instanceof JButton) {
                JButton b = (JButton) c;
                b.setCursor(wc);
                b.setFocusPainted(false);
                // https://stackoverflow.com/questions/4585867/transparent-jbutton
                b.setOpaque(false);
                b.setContentAreaFilled(false);
                b.setBorderPainted(false);
                // https://coderanch.com/t/336633/java/transparent-jbuttons
                b.setBorder(null);
            }
        }

        // === panedrone:
        // Set up columns after $$$setupUI$$$().
        // Do not do it in createUIComponents() which is called in beginning of $$$setupUI$$$();
        table.getColumnModel().getColumn(COL_INDEX_NAME).setPreferredWidth(320);
        table.getColumnModel().getColumn(COL_INDEX_NAME).setMaxWidth(2000); // === panedrone: AUTO_RESIZE_LAST_COLUMN not working without it
        table.getColumnModel().getColumn(COL_INDEX_REF).setPreferredWidth(320);
        table.getColumnModel().getColumn(COL_INDEX_REF).setMaxWidth(2000); // === panedrone: AUTO_RESIZE_LAST_COLUMN not working without it
        // table.getColumnModel().getColumn(2).setPreferredWidth(300);
        // https://stackoverflow.com/questions/953972/java-jtable-setting-column-width
        // JTable.AUTO_RESIZE_LAST_COLUMN is defined as "During all resize operations,
        // apply adjustments to the last column only" which means you have to set the auto resize mode
        // at the end of your code, otherwise setPreferredWidth() won't affect anything!
        table.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        table.doLayout();
    }

    private void open_sdm_xml_async() {
        IdeaHelpers.invokeLater(new Runnable() {
            @Override
            public void run() {
                int[] selected_rows = get_ui_table_sel_indexes();
                if (selected_rows.length > 0) {
                    String dao_class_name = (String) table.getValueAt(selected_rows[0], COL_INDEX_NAME);
                    if (IdeaHelpers.navigate_to_sdm_xml_dao_class_by_name(project, root_file, dao_class_name)) {
                        return;
                    }
                }
                IdeaEditorHelpers.open_sdm_xml_sync(project, root_file);
            }
        });
    }

    // it is called from constructor --> $$$setupUI$$$();
    private void createUIComponents() {
        table = new JTable() {
            public TableCellRenderer getCellRenderer(int row, int column) {
                if (column == COL_INDEX_STATUS) {
                    return new DaoTableColorRenderer();
                }
                return super.getCellRenderer(row, column);
            }
        };
        dao_table_model = new MyDaoTableModel();
        table.setModel(dao_table_model);
        table.getTableHeader().setReorderingAllowed(false);
        table.addMouseListener(new MouseAdapter() {
            // public void mouseClicked(MouseEvent e) {  // not working sometime
            public void mousePressed(MouseEvent e) {
                int click_count = e.getClickCount();
                if (click_count == 2 || (click_count == 1 && e.isAltDown())) {
                    int col = table.columnAtPoint(new Point(e.getX(), e.getY()));
                    int row = table.rowAtPoint(new Point(e.getX(), e.getY()));
                    if (row >= 0) {
                        if (col == COL_INDEX_NAME) {
                            navigate_to_sdm_dao_xml_async(row);
                        } else if (col == COL_INDEX_REF) {
                            open_detailed_dao_xml_async(row);
                        } else {
                            open_target_file_async(row);
                        }
                    }
                }
            }
        });
    }

    private void crud_dao_wizard() {
        IdeaCrudXmlHelpers.get_crud_dao_xml(project, root_file);
    }

    private void new_dao_xml() {
        UIDialogNewDaoXml d = new UIDialogNewDaoXml(project, root_file);
        boolean ok = d.showAndGet();   // модально, блокирует поток

        if (ok && d.isSuccess()) {
            reload_table(true);
        }
    }

    private void navigate_to_sdm_dao_xml_async(int row) {
        String dao_class_name = (String) table.getValueAt(row, COL_INDEX_NAME);
        if (dao_class_name == null || dao_class_name.trim().isEmpty()) {
            return;
        }
        IdeaHelpers.invokeLater(new Runnable() {
            public void run() {
                try {
                    IdeaHelpers.navigate_to_sdm_xml_dao_class_by_name(project, root_file, dao_class_name);
                } catch (Exception e) {
                    // e.printStackTrace();
                    IdeaMessageHelpers.show_error_in_ui_thread(e);
                }
            }
        });
    }

    private void open_detailed_dao_xml_async(int row) {
        String dao_class_ref = (String) table.getValueAt(row, COL_INDEX_REF);
        if (dao_class_ref == null || dao_class_ref.trim().isEmpty()) {
            String dao_class_name = (String) table.getValueAt(row, COL_INDEX_NAME);
            if (IdeaHelpers.navigate_to_sdm_xml_dao_class_by_name(project, root_file, dao_class_name)) {
                return;
            }
        }
        IdeaHelpers.invokeLater(new Runnable() {
            public void run() {
                try {
                    IdeaEditorHelpers.open_local_file_in_editor_sync(project, root_file, dao_class_ref);
                } catch (Exception e) {
                    // e.printStackTrace();
                    IdeaMessageHelpers.show_error_in_ui_thread(e);
                }
            }
        });
    }

    private void open_target_file_async(int row) {
        String dao_class_name = (String) table.getValueAt(row, COL_INDEX_NAME);
        IdeaHelpers.invokeLater(new Runnable() {
            public void run() {
                try {
                    Settings settings = IdeaHelpers.load_settings(root_file);
                    IdeaTargetLanguageHelpers.open_target_dao_sync(project, root_file, settings, dao_class_name);
                } catch (Exception e) {
                    // e.printStackTrace();
                    IdeaMessageHelpers.show_error_in_ui_thread(e);
                }
            }
        });
    }

    private void update_table_async() {
        // to prevent:
        // WARN - intellij.ide.HackyRepaintManager
        IdeaHelpers.invokeLater(new Runnable() {
            public void run() {
                table.updateUI();
            }
        });
    }

    private void generate_for_dao_selected_in_ui_with_progress_sync() {
        final int[] selected_rows = get_ui_table_sel_indexes();
        if (selected_rows.length == 0) {
            return;
        }
        List<IdeaHelpers.GeneratedFileData> list = new ArrayList<IdeaHelpers.GeneratedFileData>();
        StringBuilder output_dir = new StringBuilder();
        IdeaCG.ProgressError error = new IdeaCG.ProgressError();
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                try {
                    Settings settings = IdeaHelpers.load_settings(root_file);
                    for (int row : selected_rows) {
                        table.setValueAt("", row, COL_INDEX_STATUS);
                    }
                    update_table_async();
                    Connection con = IdeaHelpers.get_connection(project, settings);
                    try {
                        // !!!! after 'try'
                        IDaoCG gen = IdeaTargetLanguageHelpers.create_dao_cg(con, project, root_file, settings, output_dir);
                        List<DaoClass> jaxb_dao_classes = IdeaHelpers.load_all_sdm_dao_classes(root_file);
                        for (int row : selected_rows) {
                            String dao_class_name = (String) table.getValueAt(row, COL_INDEX_NAME);
                            try {
                                ProgressManager.progress(dao_class_name);
                                DaoClass dao_class = JaxbUtils.find_jaxb_dao_class(dao_class_name, jaxb_dao_classes);
                                String[] file_content = IdeaCG.generate_single_sdm_dao(project, root_file, gen, dao_class, settings);
                                IdeaCG.prepare_generated_file_data(root_file, dao_class_name, file_content, list);
                                table.setValueAt(Const.STATUS_GENERATED, row, COL_INDEX_STATUS);
                            } catch (Throwable e) {
                                String msg = e.getMessage();
                                table.setValueAt(msg, row, COL_INDEX_STATUS);
                                IdeaMessageHelpers.add_dao_error_message(settings, root_file, dao_class_name, msg);
                                // break; // exit the loop
                            }
                            update_table_async();
                        }
                    } finally {
                        con.close();
                    }
                    update_table_async();
                } catch (Throwable e) {
                    error.error = e;
                }
            }
        };
        ProgressManager.getInstance().runProcessWithProgressSynchronously(runnable, "Generating", false, project);
        // write only the generated files
        // writeActions can show their own dialogs
        try {
            IdeaHelpers.run_write_action_to_generate_source_file(output_dir.toString(), list, project);
        } catch (Exception e) {
            // e.printStackTrace();
            IdeaMessageHelpers.show_error_in_ui_thread(e);
        }
        update_table_async();
        if (error.error != null) {
            IdeaMessageHelpers.show_error_in_ui_thread(error.error);
        }
    }

    private int[] get_ui_table_sel_indexes() {
        int rc = table.getModel().getRowCount();
        if (rc == 1) {
            return new int[]{0};
        }
        int[] selected_rows = table.getSelectedRows();
        if (selected_rows.length == 0) {
            selected_rows = new int[rc]; // add all rows if none selected
            for (int i = 0; i < rc; i++) {
                selected_rows[i] = i;
            }
        }
        return selected_rows;
    }

    private void validate_all_with_progress_sync(IDaoCG gen, Settings settings) {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                try {
                    List<DaoClass> jaxb_dao_classes = IdeaHelpers.load_all_sdm_dao_classes(root_file);
                    int rc = dao_table_model.getRowCount();
                    for (int i = 0; i < rc; i++) {
                        String dao_class_name = (String) dao_table_model.getValueAt(i, COL_INDEX_NAME);
                        try {
                            ProgressManager.progress(dao_class_name);
                            DaoClass sdm_dao_class = JaxbUtils.find_jaxb_dao_class(dao_class_name, jaxb_dao_classes);
                            String status = IdeaCG.validate_single_sdm_dao(project, root_file, gen, sdm_dao_class, settings);
                            if (status.isEmpty()) {
                                dao_table_model.setValueAt(Const.STATUS_OK, i, COL_INDEX_STATUS);
                            } else {
                                dao_table_model.setValueAt(status, i, COL_INDEX_STATUS);
                                IdeaMessageHelpers.add_dao_error_message(settings, root_file, dao_class_name, status);
                            }
                        } catch (Throwable ex) {
                            // ex.printStackTrace();
                            String msg = ex.getMessage();
                            dao_table_model.setValueAt(msg, i, COL_INDEX_STATUS);
                            IdeaMessageHelpers.add_dao_error_message(settings, root_file, dao_class_name, msg);
                        }
                        update_table_async();
                    }
                } catch (Throwable e) {
                    // e.printStackTrace();
                    IdeaMessageHelpers.show_info_in_ui_thread(e.getMessage());
                }
            }
        };
        ProgressManager.getInstance().runProcessWithProgressSynchronously(runnable, "Validating", false, project);
    }

    private void validate_all_with_progress_sync() {
        try {
            reload_table();
            Settings profile = IdeaHelpers.load_settings(root_file);
            Connection con = IdeaHelpers.get_connection(project, profile);
            try {
                // !!!! after 'try'
                IDaoCG gen = IdeaTargetLanguageHelpers.create_dao_cg(con, project, root_file, profile, null);
                validate_all_with_progress_sync(gen, profile);
            } finally {
                con.close();
            }
        } catch (Throwable e) {
            // e.printStackTrace();
            IdeaMessageHelpers.show_error_in_ui_thread(e);
        }
    }

    public void set_project(Project project) {
        this.project = project;
    }

    public void set_root_file(VirtualFile propFile) {
        this.root_file = propFile;
    }

    public JComponent get_root_panel() {
        return rootPanel;
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        createUIComponents();
        rootPanel = new JPanel();
        rootPanel.setLayout(new BorderLayout(0, 0));
        final JScrollPane scrollPane1 = new JScrollPane();
        rootPanel.add(scrollPane1, BorderLayout.CENTER);
        table.setAutoResizeMode(0);
        table.setFillsViewportHeight(false);
        scrollPane1.setViewportView(table);
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new FlowLayout(FlowLayout.LEADING, 5, 5));
        panel1.setOpaque(false);
        rootPanel.add(panel1, BorderLayout.NORTH);
        tool_panel = new JPanel();
        tool_panel.setLayout(new FlowLayout(FlowLayout.CENTER, 0, 0));
        tool_panel.setOpaque(false);
        panel1.add(tool_panel);
        open_sdm_xml = new JButton();
        open_sdm_xml.setBorderPainted(false);
        open_sdm_xml.setContentAreaFilled(false);
        open_sdm_xml.setIcon(new ImageIcon(getClass().getResource("/img/xmldoc.gif")));
        open_sdm_xml.setMargin(new Insets(0, 0, 0, 0));
        open_sdm_xml.setMaximumSize(new Dimension(32, 32));
        open_sdm_xml.setMinimumSize(new Dimension(32, 32));
        open_sdm_xml.setOpaque(false);
        open_sdm_xml.setPreferredSize(new Dimension(32, 32));
        open_sdm_xml.setText("");
        open_sdm_xml.setToolTipText("Open 'sdm.xml'");
        tool_panel.add(open_sdm_xml);
        btn_goto_detailed_dao_xml = new JButton();
        btn_goto_detailed_dao_xml.setBorderPainted(false);
        btn_goto_detailed_dao_xml.setContentAreaFilled(false);
        btn_goto_detailed_dao_xml.setIcon(new ImageIcon(getClass().getResource("/img/xmldoc_12x12.gif")));
        btn_goto_detailed_dao_xml.setMargin(new Insets(0, 0, 0, 0));
        btn_goto_detailed_dao_xml.setMaximumSize(new Dimension(32, 32));
        btn_goto_detailed_dao_xml.setMinimumSize(new Dimension(32, 32));
        btn_goto_detailed_dao_xml.setOpaque(false);
        btn_goto_detailed_dao_xml.setPreferredSize(new Dimension(32, 32));
        btn_goto_detailed_dao_xml.setText("");
        btn_goto_detailed_dao_xml.setToolTipText("Navigate to XML definition (double-click one of cells in the middle column)");
        tool_panel.add(btn_goto_detailed_dao_xml);
        btn_OpenJava = new JButton();
        btn_OpenJava.setBorderPainted(false);
        btn_OpenJava.setContentAreaFilled(false);
        btn_OpenJava.setIcon(new ImageIcon(getClass().getResource("/img/GeneratedFile.gif")));
        btn_OpenJava.setMargin(new Insets(0, 0, 0, 0));
        btn_OpenJava.setMaximumSize(new Dimension(32, 32));
        btn_OpenJava.setMinimumSize(new Dimension(32, 32));
        btn_OpenJava.setOpaque(false);
        btn_OpenJava.setPreferredSize(new Dimension(32, 32));
        btn_OpenJava.setText("");
        btn_OpenJava.setToolTipText("Navigate to generated code (double-click one of the right-most cells)");
        tool_panel.add(btn_OpenJava);
        btn_NewXML = new JButton();
        btn_NewXML.setBorderPainted(false);
        btn_NewXML.setContentAreaFilled(false);
        btn_NewXML.setIcon(new ImageIcon(getClass().getResource("/img/new_xml.gif")));
        btn_NewXML.setMargin(new Insets(0, 0, 0, 0));
        btn_NewXML.setMaximumSize(new Dimension(32, 32));
        btn_NewXML.setMinimumSize(new Dimension(32, 32));
        btn_NewXML.setOpaque(false);
        btn_NewXML.setPreferredSize(new Dimension(32, 32));
        btn_NewXML.setText("");
        btn_NewXML.setToolTipText("New DAO XML file");
        tool_panel.add(btn_NewXML);
        btn_CrudDao = new JButton();
        btn_CrudDao.setBorderPainted(false);
        btn_CrudDao.setContentAreaFilled(false);
        btn_CrudDao.setIcon(new ImageIcon(getClass().getResource("/img/180.png")));
        btn_CrudDao.setMargin(new Insets(0, 0, 0, 0));
        btn_CrudDao.setMaximumSize(new Dimension(32, 32));
        btn_CrudDao.setMinimumSize(new Dimension(32, 32));
        btn_CrudDao.setOpaque(false);
        btn_CrudDao.setPreferredSize(new Dimension(32, 32));
        btn_CrudDao.setText("");
        btn_CrudDao.setToolTipText("DAO CRUD XML Assistant");
        tool_panel.add(btn_CrudDao);
        button_fk_assistant = new JButton();
        button_fk_assistant.setBorderPainted(false);
        button_fk_assistant.setContentAreaFilled(false);
        button_fk_assistant.setIcon(new ImageIcon(getClass().getResource("/img/FK.gif")));
        button_fk_assistant.setMargin(new Insets(0, 0, 0, 0));
        button_fk_assistant.setMaximumSize(new Dimension(32, 32));
        button_fk_assistant.setMinimumSize(new Dimension(32, 32));
        button_fk_assistant.setOpaque(false);
        button_fk_assistant.setPreferredSize(new Dimension(32, 32));
        button_fk_assistant.setText("");
        button_fk_assistant.setToolTipText("FK Access Assistant");
        tool_panel.add(button_fk_assistant);
        btn_Refresh = new JButton();
        btn_Refresh.setBorderPainted(false);
        btn_Refresh.setContentAreaFilled(false);
        btn_Refresh.setIcon(new ImageIcon(getClass().getResource("/img/arrow-circle-double-135.png")));
        btn_Refresh.setMargin(new Insets(0, 0, 0, 0));
        btn_Refresh.setMaximumSize(new Dimension(32, 32));
        btn_Refresh.setMinimumSize(new Dimension(32, 32));
        btn_Refresh.setOpaque(false);
        btn_Refresh.setPreferredSize(new Dimension(32, 32));
        btn_Refresh.setText("");
        btn_Refresh.setToolTipText("Refresh");
        tool_panel.add(btn_Refresh);
        btn_Generate = new JButton();
        btn_Generate.setBorderPainted(false);
        btn_Generate.setContentAreaFilled(false);
        btn_Generate.setIcon(new ImageIcon(getClass().getResource("/img/compile-warning.png")));
        btn_Generate.setMargin(new Insets(0, 0, 0, 0));
        btn_Generate.setMaximumSize(new Dimension(32, 32));
        btn_Generate.setMinimumSize(new Dimension(32, 32));
        btn_Generate.setOpaque(false);
        btn_Generate.setPreferredSize(new Dimension(32, 32));
        btn_Generate.setText("");
        btn_Generate.setToolTipText("Generate for selection");
        tool_panel.add(btn_Generate);
        btn_Validate = new JButton();
        btn_Validate.setBorderPainted(false);
        btn_Validate.setContentAreaFilled(false);
        btn_Validate.setIcon(new ImageIcon(getClass().getResource("/img/validate.gif")));
        btn_Validate.setMargin(new Insets(0, 0, 0, 0));
        btn_Validate.setMaximumSize(new Dimension(32, 32));
        btn_Validate.setMinimumSize(new Dimension(32, 32));
        btn_Validate.setOpaque(false);
        btn_Validate.setPreferredSize(new Dimension(32, 32));
        btn_Validate.setText("");
        btn_Validate.setToolTipText("Validate all");
        tool_panel.add(btn_Validate);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return rootPanel;
    }

    private static class DaoTableColorRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(
                JTable table,
                Object value,
                boolean isSelected,
                boolean hasFocus,
                int row,
                int column) {

            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (column != 2) {
                return c;
            }
            String sValue = (String) value;
            setText(sValue);
            if (isSelected || Const.STATUS_OK.equals(sValue) || Const.STATUS_GENERATED.equals(sValue)) {
                TableCellRenderer r = table.getCellRenderer(row, column);
                Component c_0 = r.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, 0);
                c.setForeground(c_0.getForeground());
            } else {
                c.setForeground(JBColor.RED);
            }
            return c;
        }
    }

    private class MyDaoTableModel extends AbstractTableModel {

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
            switch (col) {
                case COL_INDEX_NAME:
                    return "Class";
                case COL_INDEX_REF:
                    return "Ref.";
                case COL_INDEX_STATUS:
                    return "State";
            }
            return "";
        }

        @Override
        public int getColumnCount() {
            return 3;
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
            ArrayList<String[]> list = dao_table_model.getList();
            list.clear();
            List<DaoClass> jaxb_dao_classes = IdeaHelpers.load_all_sdm_dao_classes(root_file);
            for (DaoClass cls : jaxb_dao_classes) {
                String[] item = new String[3];
                item[COL_INDEX_NAME] = cls.getName();
                item[COL_INDEX_REF] = cls.getRef();
                item[COL_INDEX_STATUS] = "";
                list.add(item);
            }
        } finally {
            dao_table_model.refresh(); // // table.updateUI();
        }
    }

    public void reload_table(boolean show_error_msg) {
        try {
            reload_table();
        } catch (Throwable e) {
            if (show_error_msg) {
                // e.printStackTrace();
                IdeaMessageHelpers.show_error_in_ui_thread(e);
            }
        }
    }
}