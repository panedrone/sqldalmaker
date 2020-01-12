/*
 * Copyright 2011-2018 sqldalmaker@gmail.com
 * SQL DAL Maker Website: http://sqldalmaker.sourceforge.net
 * Read LICENSE.txt in the root of this project/archive for details.
 */
package com.sqldalmaker.netbeans;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectInformation;
import org.netbeans.api.project.ProjectUtils;
import org.netbeans.api.project.ui.OpenProjects;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.awt.DropDownButtonFactory;
import org.openide.filesystems.FileObject;
import org.openide.util.Exceptions;
import org.openide.util.ImageUtilities;
import org.openide.util.NbBundle;
import org.openide.util.NbBundle.Messages;
import org.openide.util.actions.Presenter;

/**
 *
 * @author sqldalmaker@gmail.com
 *
 */
@ActionID(
        category = "File",
        id = "com.sqldalmaker.netbeans.SdmAction"
)
@ActionRegistration(
        lazy = false, // must be false to ensure call of getToolbarPresenter
        // iconBase = "com/sqldalmaker/netbeans/sqldalmaker_24.png", // forbidden for AbstractAction implements Presenter.Toolbar
        displayName = "#CTL_SdmAction" // works for toolbar, not for button
//         displayName = "NOT-USED"
)
@ActionReference(path = "Toolbars/File", position = 1000)
@Messages("CTL_SdmAction=SQL DAL Maker Root-Files")
public final class SdmAction extends AbstractAction implements Presenter.Toolbar {

    // extends AbstractAction
    // https://blogs.oracle.com/geertjan/entry/generic_node_popup_registration_solution
    // MyToolbarPresenter
    // https://blogs.oracle.com/geertjan/entry/presenter_toolbar_presenter_menu
    // Google: "org.openide.awt.DropDownButton"
    // https://blogs.oracle.com/geertjan/entry/org_openide_awt_dropdownbuttonfactory1
    // DropDownButtonFactory
    // http://www.codejava.net/java-se/swing/how-to-create-drop-down-button-in-swing
    @Override
    public void actionPerformed(ActionEvent e) {
        // NEVER CALLED
    }

    private static void add_empty_item(JPopupMenu popup) {

        JMenuItem item = new JMenuItem("(empty)");
        item.setEnabled(false); // add dummy to fire popupMenuWillBecomeVisible in the future
        popup.add(item);
    }

    private static void enum_root_files(final FileObject root_folder, final FileObject current_folder, final List<String> rel_path_names) {

        FileObject[] children = current_folder.getChildren();

        for (FileObject c : children) {

            if (c.isFolder()) {

                enum_root_files(root_folder, c, rel_path_names);

            } else {

                String path = NbpTargetLanguageHelpers.get_root_file_relative_path(root_folder, c);

                if (path != null) {

                    rel_path_names.add(path);
                }
            }
        }
    }

    private List<String> get_root_file_titles(final Project[] projects) throws Exception {

        List<String> res = new ArrayList<String>();

        for (Project pr : projects) {

            FileObject project_dir = pr.getProjectDirectory();

            FileObject project_root_folder = NbpPathHelpers.get_root_folder(project_dir);

            List<String> rel_path_names = new ArrayList<String>();

            enum_root_files(project_root_folder, project_root_folder, rel_path_names);

            for (String name : rel_path_names) {

                ProjectInformation info = ProjectUtils.getInformation(pr);

                name = info.getDisplayName() + " - " + name;

                res.add(name);
            }
        }

        return res;
    }

    @Override
    public Component getToolbarPresenter() {

        final JPopupMenu popup = new JPopupMenu();

        add_empty_item(popup);

        popup.addPopupMenuListener(new PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {

                // TODO: async?
                popup.removeAll();

                try {

                    // Google: netbeans enumerate open projects
                    // https://netbeans.org/download/5_5/org-netbeans-modules-projectuiapi/org/netbeans/api/project/ui/OpenProjects.html
                    // org.netbeans.api.project.ui.OpenProjects
                    OpenProjects open_projects = OpenProjects.getDefault();

                    final Project[] projects = open_projects.getOpenProjects();

                    List<String> root_file_titles = get_root_file_titles(projects);

                    if (root_file_titles.isEmpty()) {

                        add_empty_item(popup);

                    } else {

                        for (String name : root_file_titles) {

                            JMenuItem item = new JMenuItem(new AbstractAction(name) {
                                @Override
                                public void actionPerformed(ActionEvent e) {

                                    String title = e.getActionCommand();

                                    open_root_file(title, projects);

                                }
                            });

                            item.setText(name);

                            popup.add(item);
                        }
                    }

                } catch (Exception ex) {

                    Exceptions.printStackTrace(ex);
                }
            }

            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
            }

            @Override
            public void popupMenuCanceled(PopupMenuEvent e) {
            }
        }
        );

        /////////////////////////////////////////////////////////////
        //
        final String ICON_PATH = "com/sqldalmaker/netbeans/sqldalmaker_24.png";

        final JButton drop_down_button = DropDownButtonFactory.createDropDownButton(ImageUtilities.loadImageIcon(ICON_PATH, true), popup);

        drop_down_button.setToolTipText(NbBundle.getMessage(SdmAction.class, "CTL_SdmAction"));

        drop_down_button.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent arg0) {

                try {

                    OpenProjects open_projects = OpenProjects.getDefault();

                    final Project[] projects = open_projects.getOpenProjects();

                    List<String> titles = get_root_file_titles(projects);

                    if (titles.size() > 1) {

                        popup.show(drop_down_button, 0, drop_down_button.getHeight());

                    } else if (titles.size() == 1) {

                        open_root_file(titles.get(0), projects);
                    }

                } catch (Exception ex) {

                    Exceptions.printStackTrace(ex);
                }
            }
        });

        // DROPDOWN WORKS WITHOUT THIS CODE:
//        drop_down_button.addItemListener(new ItemListener() {
//            
//            @Override
//            public void itemStateChanged(ItemEvent e) {
//                int state = e.getStateChange();
//                if (state == ItemEvent.SELECTED) {
//                    //\*\* show popup menu on dropdown button at position: (0, height) \*/
//                    popup.show(drop_down_button, 0, drop_down_button.getHeight());
//                }
//            }
//        });
        return drop_down_button;
    }

    private void open_root_file(String title, Project[] projects) {

        for (Project pr : projects) {

            ProjectInformation info = ProjectUtils.getInformation(pr);

            String prefix = info.getDisplayName() + " - ";

            if (title.startsWith(prefix)) {

                try {

                    String rel_path = title.substring(prefix.length());

                    NbpIdeEditorHelpers.open_project_file_in_editor_async(pr.getProjectDirectory(), rel_path);

                } catch (Exception ex) {

                    Exceptions.printStackTrace(ex);
                }

                break;
            }
        }
    }
}
