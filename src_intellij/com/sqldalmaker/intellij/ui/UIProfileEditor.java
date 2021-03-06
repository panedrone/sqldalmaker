/*
 * Copyright 2011-2020 sqldalmaker@gmail.com
 * SQL DAL Maker Website: http://sqldalmaker.sourceforge.net
 * Read LICENSE.txt in the root of this project/archive for details.
 */
package com.sqldalmaker.intellij.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Created with IntelliJ IDEA.
 * User: sqldalmaker@gmail.com
 * Date: 21.06.12
 */
public class UIProfileEditor {
    protected JPanel root;
    private JPanel cards1;
    private JPanel panel_1;

    private static final String DTO = "DTO";
    private static final String DAO = "DAO";
    private static final String Admin = "Admin";

    private final JButton buttonDto;
    private final JButton buttonDao;
    private final JButton buttonAdmin;

    private JComponent toolbar;

    private static final Cursor hand = new Cursor(Cursor.HAND_CURSOR);

    private static JButton createSimpleButton(String text) {
        JButton button = new JButton(text);
        button.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        button.setCursor(hand);
        button.setFocusPainted(false);
        button.setOpaque(false);
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        return button;
    }

    void makeUnderline(JButton b, String text, boolean underline) {
        if (underline) {
            b.setText("<html><u>" + text + "</u></html>");
        } else {
            b.setText(text);
        }
    }

    public void openDTO() {
        CardLayout cl = (CardLayout) (cards1.getLayout());
        cl.show(cards1, DTO);
        makeUnderline(buttonDto, DTO, true);
        makeUnderline(buttonDao, DAO, false);
        makeUnderline(buttonAdmin, Admin, false);
        if (toolbar != null) {
            panel_1.remove(toolbar);
        }
        toolbar = tabDTO1.get_tool_bar();
        panel_1.add(toolbar);
        panel_1.updateUI();
    }

    public void openDAO() {
        CardLayout cl = (CardLayout) (cards1.getLayout());
        cl.show(cards1, DAO);
        makeUnderline(buttonDto, DTO, false);
        makeUnderline(buttonDao, DAO, true);
        makeUnderline(buttonAdmin, Admin, false);
        if (toolbar != null) {
            panel_1.remove(toolbar);
        }
        toolbar = tabDAO1.get_tool_bar();
        panel_1.add(toolbar);
        panel_1.updateUI();
    }

    public void openAdmin() {
        CardLayout cl = (CardLayout) (cards1.getLayout());
        cl.show(cards1, Admin);
        makeUnderline(buttonDto, DTO, false);
        makeUnderline(buttonDao, DAO, false);
        makeUnderline(buttonAdmin, Admin, true);
        if (toolbar != null) {
            panel_1.remove(toolbar);
        }
        panel_1.updateUI();
    }

    public UIProfileEditor() {
        $$$setupUI$$$();
        buttonDto = createSimpleButton(DTO);
        buttonDao = createSimpleButton(DAO);
        buttonAdmin = createSimpleButton(Admin);
        buttonDto.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                openDTO();
            }
        });
        buttonDao.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                openDAO();
            }
        });
        buttonAdmin.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                openAdmin();
            }
        });
        panel_1.add(buttonDto);
        panel_1.add(buttonDao);
        panel_1.add(buttonAdmin);
        makeUnderline(buttonDto, DTO, true);
        makeUnderline(buttonDao, DAO, false);
        makeUnderline(buttonAdmin, Admin, false);
        CardLayout cl = (CardLayout) (cards1.getLayout());
        cl.show(cards1, DTO);
    }

    public JComponent getRootPanel() {
        return root;
    }

    private UITabDTO tabDTO1;
    private UITabDAO tabDAO1;
    protected UITabAdmin tabAdmin; // protected to supress warning

    public void init(Project project, VirtualFile file) {
        try {
            tabDTO1 = new UITabDTO();
            cards1.add(tabDTO1.get_root_panel(), DTO);
            tabDTO1.set_project(project);
            tabDTO1.set_file(file);
            tabDTO1.reload_table(false);
            tabDAO1 = new UITabDAO();
            cards1.add(tabDAO1.get_root_panel(), DAO);
            tabDAO1.set_project(project);
            tabDAO1.set_file(file);
            tabDAO1.reload_table(false);
            tabAdmin = new UITabAdmin();
            cards1.add(tabAdmin.getRootPanel(), Admin);
            tabAdmin.setProject(project);
            tabAdmin.setFile(file);
            tabAdmin.init_runtime();
            toolbar = tabDTO1.get_tool_bar();
            panel_1.add(toolbar);
        } catch (Exception e) {
            e.printStackTrace();
            IdeaMessageHelpers.show_error_in_ui_thread(e);
        }
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     */
    private void $$$setupUI$$$() {
        root = new JPanel();
        root.setLayout(new BorderLayout(0, 0));
        panel_1 = new JPanel();
        panel_1.setLayout(new FlowLayout(FlowLayout.LEFT, 2, 1));
        root.add(panel_1, BorderLayout.NORTH);
        cards1 = new JPanel();
        cards1.setLayout(new CardLayout(0, 0));
        root.add(cards1, BorderLayout.CENTER);
    }

    /**
     *
     */
    public JComponent $$$getRootComponent$$$() {
        return root;
    }
}
