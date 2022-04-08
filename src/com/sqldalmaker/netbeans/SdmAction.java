/*
    Copyright 2011-2022 sqldalmaker@gmail.com
    SQL DAL Maker Website: http://sqldalmaker.sourceforge.net
    Read LICENSE.txt in the root of this project/archive for details.
 */
package com.sqldalmaker.netbeans;

import com.sqldalmaker.cg.Helpers;
import com.sqldalmaker.common.FileSearchHelpers;
import com.sqldalmaker.jaxb.settings.Settings;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import org.netbeans.api.editor.EditorRegistry;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectInformation;
import org.netbeans.api.project.ProjectUtils;
import org.netbeans.api.project.ui.OpenProjects;
import org.netbeans.modules.editor.NbEditorUtilities;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.awt.DropDownButtonFactory;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectNotFoundException;
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
        category = "Tools",
        id = "sqldalmaker.netbeans.SdmAction"
)
@ActionRegistration(
        lazy = false, // must be false to ensure call of getToolbarPresenter
        // iconBase = "com/sqldalmaker/netbeans/sqldalmaker_24.png", // forbidden for AbstractAction implements Presenter.Toolbar
        displayName = "#CTL_SdmAction" // works for toolbar, not for button
//         displayName = "NOT-USED"
)
@ActionReference(path = "Toolbars/Database", position = 1000)
@Messages("CTL_SdmAction=SDM")
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

    private static void add_list_is_empty_item(JPopupMenu popup) {
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

    private static String get_prefix(Project project) {
        ProjectInformation info = ProjectUtils.getInformation(project);
        String prefix = info.getDisplayName() + "/";
        return prefix;
    }

    private static void open_root_file(String title, Project[] projects) {
        for (Project project : projects) {
            String prefix = get_prefix(project);
            if (title.startsWith(prefix)) {
                try {
                    String rel_path = title.substring(prefix.length());
                    NbpIdeEditorHelpers.open_project_file_in_editor_async(project.getProjectDirectory(), rel_path);
                } catch (Exception ex) {
                    // Exceptions.printStackTrace(e); // // === panedrone: it shows banner!!!
                }
                break;
            }
        }
    }

    private static String get_title(Project project, String rel_path) {
        String prefix = get_prefix(project);
        String res = prefix + rel_path;
        return res;
    }

    private static List<String> get_root_file_titles(final Project[] projects) throws Exception {
        List<String> res = new ArrayList<String>();
        for (Project project : projects) {
            FileObject project_dir = project.getProjectDirectory();
            FileObject project_root_folder = NbpPathHelpers.get_root_folder(project_dir);
            List<String> rel_path_names = new ArrayList<String>();
            enum_root_files(project_root_folder, project_root_folder, rel_path_names);
            for (String rel_path : rel_path_names) {
                String title = get_title(project, rel_path);
                res.add(title);
            }
        }
        return res;
    }

    private void build_popup_dto(final JPopupMenu popup, final SdmDataObject root_data_object, final FileObject xml_file, final String xml_file_title) {
        final String name_gen = xml_file_title + " -> Generate All";
        JMenuItem item_gen = new JMenuItem(new AbstractAction(name_gen) {
            @Override
            public void actionPerformed(ActionEvent e) {
                NbpCG.generate_all_dto(root_data_object, xml_file, xml_file_title);
            }
        });
        item_gen.setText(name_gen);
        popup.add(item_gen);
        final String name_val = xml_file_title + " -> Validate All";
        JMenuItem item_val = new JMenuItem(new AbstractAction(name_val) {
            @Override
            public void actionPerformed(ActionEvent e) {
                NbpCG.validate_all_dto(root_data_object, xml_file, xml_file_title);
            }
        });
        item_val.setText(name_val);
        popup.add(item_val);
    }

    private void build_popup_dao(final JPopupMenu popup, final SdmDataObject root_data_object, final FileObject xml_file, final String xml_file_title) {
        final String name_gen = xml_file_title + " -> Generate";
        JMenuItem item_gen = new JMenuItem(new AbstractAction(name_gen) {
            @Override
            public void actionPerformed(ActionEvent e) {
                NbpCG.generate_dao(root_data_object, xml_file, xml_file_title);
            }
        });
        item_gen.setText(name_gen);
        popup.add(item_gen);
        final String name_val = xml_file_title + " -> Validate";
        JMenuItem item_val = new JMenuItem(new AbstractAction(name_val) {
            @Override
            public void actionPerformed(ActionEvent e) {
                NbpCG.validate_dao(root_data_object, xml_file, xml_file_title);
            }
        });
        item_val.setText(name_val);
        popup.add(item_val);
    }

    private void add_xml_file_items(JPopupMenu popup, Project[] projects) throws DataObjectNotFoundException {
        JTextComponent text_component = EditorRegistry.focusedComponent();
        if (text_component == null) {
            return;
        }
        Document doc = text_component.getDocument();
        DataObject data_object = NbEditorUtilities.getDataObject(doc);
        if (data_object == null) {
            return;
        }
        final FileObject this_file_object = data_object.getPrimaryFile();
        if (this_file_object == null) {
            return;
        }
        Project this_project = NbpPathHelpers.get_project(this_file_object);
        if (this_project == null) {
            return;
        }
        FileObject xml_mp_dir = this_file_object.getParent();
        List<FileObject> root_files = NbpTargetLanguageHelpers.find_root_files(xml_mp_dir);
        if (root_files.size() != 1) {
            return;
        }
        FileObject root_file = root_files.get(0);
        DataObject root_file_data_object = DataObject.find(root_file);
        if (!(root_file_data_object instanceof SdmDataObject)) {
            return;
        }
        SdmDataObject root_data_object = (SdmDataObject) root_file_data_object;
        FileObject project_dir = this_project.getProjectDirectory();
        String this_file_rel_path = NbpPathHelpers.get_relative_path(project_dir, this_file_object);
        String this_file_title = get_title(this_project, this_file_rel_path);
        String doc_name = this_file_object.getNameExt();
        if (FileSearchHelpers.is_dto_xml(doc_name)) {
            build_popup_dto(popup, root_data_object, this_file_object, this_file_title);
        } else if (FileSearchHelpers.is_dao_xml(doc_name)) {
            build_popup_dao(popup, root_data_object, this_file_object, this_file_title);
        }
        ///////////////////////////////////////
        if (FileSearchHelpers.is_dto_xml(doc_name) || FileSearchHelpers.is_dao_xml(doc_name)) {
            String root_file_rel_path = NbpPathHelpers.get_relative_path(project_dir, root_file);
            final String root_file_title = get_title(this_project, root_file_rel_path);
            JMenuItem item = new JMenuItem(new AbstractAction(root_file_title) {
                @Override
                public void actionPerformed(ActionEvent e) {
                    open_root_file(root_file_title, projects);
                }
            });
            item.setText(root_file_title);
            popup.add(item);
        }
        if (FileSearchHelpers.is_dao_xml(doc_name)) {
            try {
                Settings settings = NbpHelpers.load_settings(root_data_object);
                String dao_class_name = Helpers.get_dao_class_name(this_file_object.getNameExt());
                String file_title = NbpTargetLanguageHelpers.get_rel_path(root_data_object, settings, dao_class_name, settings.getDao().getScope());
                JMenuItem item = new JMenuItem(new AbstractAction(file_title) {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        try {
                            NbpTargetLanguageHelpers.open_in_editor_async(root_data_object, settings, dao_class_name, settings.getDao().getScope());
                        } catch (Exception ex) {
                            // ex.printStackTrace();
                            NbpIdeMessageHelpers.show_error_in_ui_thread(ex);
                        }
                    }
                });
                item.setText(file_title);
                popup.add(item);
            } catch (Exception ex) {
                Exceptions.printStackTrace(ex);
            }
        }
    }

    private void build_popup(JPopupMenu popup) {
        // TODO: async?
        popup.removeAll();
        try {
            // Google: netbeans enumerate open projects
            // https://netbeans.org/download/5_5/org-netbeans-modules-projectuiapi/org/netbeans/api/project/ui/OpenProjects.html
            // org.netbeans.api.project.ui.OpenProjects
            OpenProjects open_projects = OpenProjects.getDefault();
            Project[] projects = open_projects.getOpenProjects();
            // there may be projects without SDM:
            List<String> root_file_titles = get_root_file_titles(projects);
            if (root_file_titles.isEmpty()) {
                return;
            }
            add_xml_file_items(popup, projects);
            add_root_file_items(popup, projects, root_file_titles);
        } catch (Exception ex) {
            // Exceptions.printStackTrace(e); // // === panedrone: it shows banner!!!
        }
    }

    private void add_root_file_items(JPopupMenu popup, Project[] projects, List<String> root_file_titles) {
        if (root_file_titles.size() < 2) {
            return;
        }
        popup.add(new JPopupMenu.Separator());
        for (final String root_file_title : root_file_titles) {
            JMenuItem item = new JMenuItem(new AbstractAction(root_file_title) {
                @Override
                public void actionPerformed(ActionEvent e) {
                    open_root_file(root_file_title, projects);
                }
            });
            item.setText(root_file_title);
            popup.add(item);
        }
    }

    @Override
    public Component getToolbarPresenter() {
        final JPopupMenu popup = new JPopupMenu();

        // === panedrone: it will make triggering popupMenuWillBecomeVisible with even empty dropdown
        add_list_is_empty_item(popup);

        // add_empty_item(popup);
        popup.addPopupMenuListener(new PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                build_popup(popup);
            }

            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
            }

            @Override
            public void popupMenuCanceled(PopupMenuEvent e) {
            }
        });
        /////////////////////////////////////////////////////////////
        final String ICON_PATH = "com/sqldalmaker/netbeans/sqldalmaker_24.png";
        final JButton drop_down_button = DropDownButtonFactory.createDropDownButton(ImageUtilities.loadImageIcon(ICON_PATH, true), popup);
        drop_down_button.setToolTipText(NbBundle.getMessage(SdmAction.class, "CTL_SdmAction"));
        drop_down_button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                // === panedrone: tool-bar button clicked itself (not an arrow aside)
                try {
                    // OpenProjects open_projects = OpenProjects.getDefault();
                    // final Project[] projects = open_projects.getOpenProjects();
                    // List<String> titles = get_root_file_titles(projects);
                    //if (titles.size() > 1) {
                    popup.show(drop_down_button, 0, drop_down_button.getHeight());
//                    } else if (titles.size() == 1) {
//                        open_root_file(titles.get(0), projects);
//                    }
                } catch (Exception ex) {
                    // Exceptions.printStackTrace(e); // // === panedrone: it shows banner!!!
                }
            }
        });
        // DROPDOWN WORKS WITHOUT THIS CODE:
        drop_down_button.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                int state = e.getStateChange();
                if (state == ItemEvent.SELECTED) {
                    //\*\* show popup menu on dropdown button at position: (0, height) \*/
                    popup.show(drop_down_button, 0, drop_down_button.getHeight());
                }
            }
        });
        return drop_down_button;
    }
}
