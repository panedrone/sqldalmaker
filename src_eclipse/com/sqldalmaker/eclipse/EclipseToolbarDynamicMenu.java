/*
	Copyright 2011-2024 sqldalmaker@gmail.com
	Read LICENSE.txt in the root of this project/archive.
	Project web-site: https://sqldalmaker.sourceforge.net/
*/
package com.sqldalmaker.eclipse;

import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.action.ContributionItem;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.ResourceUtil;

import com.sqldalmaker.cg.Helpers;

/**
 *
 * @author sqldalmaker@gmail.com
 *
 */
public class EclipseToolbarDynamicMenu extends ContributionItem {

	// http://insights.sigasi.com/tech/dynamic-menu-items-eclipse.html

	public EclipseToolbarDynamicMenu() {
	}

	public EclipseToolbarDynamicMenu(String id) {
		super(id);
	}

	private static String add_xml_file_actions(Menu menu, int index, IProject[] projects) {
		IWorkbenchWindow win = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		IWorkbenchPage page = win.getActivePage();
		if (page == null) {
			return null;
		}
		IEditorPart editor_part = page.getActiveEditor();
		if (editor_part == null) {
			return null;
		}
		IEditorInput input = editor_part.getEditorInput();
		if (input == null) {
			return null;
		}
		IFile input_file = ResourceUtil.getFile(input);
		IContainer xml_mp_folder = input_file.getParent();
		if (!(xml_mp_folder instanceof IFolder)) {
			return null;
		}
		List<IFile> root_files;
		try {
			root_files = EclipseTargetLanguageHelpers.get_root_files(xml_mp_folder);
		} catch (Exception e1) {
			e1.printStackTrace();
			return null;
		}
		if (root_files.size() != 1) {
			return null;
		}
		IFile root_file = root_files.get(0);
		boolean is_sdm_xml = Helpers.is_sdm_xml(input_file.getName());
		boolean is_dao_xml = Helpers.is_dao_xml(input_file.getName());
		String path = input_file.getFullPath().toPortableString();
		if (path.startsWith("/")) {
			path = path.substring(1);
		}
		String current_xml_file_rel_path = path;
		if (is_sdm_xml) {
			{
				MenuItem menuItem = new MenuItem(menu, SWT.PUSH, index++);
				menuItem.setText(current_xml_file_rel_path + " -> Generate All");
				menuItem.addSelectionListener(new SelectionAdapter() {
					public void widgetSelected(SelectionEvent e) {
						editor_part.setFocus(); // to make working ${project_loc}
						EclipseCG.generate_all_sdm_dto(root_file, input_file);
						EclipseCG.generate_all_sdm_dao(root_file, input_file);
					}
				});
			}
			{
				MenuItem menuItem = new MenuItem(menu, SWT.PUSH, index++);
				menuItem.setText(current_xml_file_rel_path + " -> Validate All");
				menuItem.addSelectionListener(new SelectionAdapter() {
					public void widgetSelected(SelectionEvent e) {
						editor_part.setFocus(); // to make working ${project_loc}
						EclipseCG.validate_all_sdm_dto(root_file, input_file);
						EclipseCG.validate_all_sdm_dao(root_file, input_file);
					}
				});
			}
		} else if (is_dao_xml) {
			{
				MenuItem menuItem = new MenuItem(menu, SWT.PUSH, index++);
				menuItem.setText(current_xml_file_rel_path + " -> Generate");
				menuItem.addSelectionListener(new SelectionAdapter() {
					public void widgetSelected(SelectionEvent e) {
						editor_part.setFocus(); // to make working ${project_loc}
						EclipseCG.toolbar_action_generate_external_dao(root_file, input_file);
					}
				});
			}
			{
				MenuItem menuItem = new MenuItem(menu, SWT.PUSH, index++);
				menuItem.setText(current_xml_file_rel_path + " -> Validate");
				menuItem.addSelectionListener(new SelectionAdapter() {
					public void widgetSelected(SelectionEvent e) {
						editor_part.setFocus(); // to make working ${project_loc}
						EclipseCG.toolbar_action_validate_external_dao(root_file, input_file);
					}
				});
			}
		}
		if (is_sdm_xml || is_dao_xml) {
			String root_file_rel_path = add_goto_root_file(menu, projects, index++, current_xml_file_rel_path,
					root_file);
			return root_file_rel_path;
		}
//		if (is_dao_xml) {
//			add_goto_target_menu(menu, index++, current_xml_file_rel_path, root_file);
//		}
		return null;
	}

	private static String add_goto_root_file(Menu menu, IProject[] projects, final int index,
			String current_xml_file_rel_path, IFile root_file) {

		MenuItem menuItem = new MenuItem(menu, SWT.PUSH, index);
		String path = root_file.getFullPath().toPortableString();
		if (path.startsWith("/")) {
			path = path.substring(1);
		}
		String root_file_rel_path = path;
		menuItem.setText(root_file_rel_path);
		menuItem.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				try {
					EclipseToolbarUtils.open_root_file(root_file_rel_path, projects);
				} catch (Exception e1) {
					e1.printStackTrace();
					EclipseMessageHelpers.show_error(e1);
				}
			}
		});
		return root_file_rel_path;
	}

//	private static void add_goto_target_menu(Menu menu, final int index, String current_xml_file_rel_path,
//			IFile root_file) {
//
//		MenuItem menuItem = new MenuItem(menu, SWT.PUSH, index);
//		menuItem.setText("Open Target");
//		menuItem.addSelectionListener(new SelectionAdapter() {
//			public void widgetSelected(SelectionEvent e) {
//				try {
//					List<DaoClass> jaxb_dao_classes = EclipseHelpers.load_all_sdm_dao_classes(root_file);
//					String dao_class_name = JaxbUtils.get_dao_class_name_by_dao_xml_path(jaxb_dao_classes,
//							current_xml_file_rel_path);
//					Settings settings = EclipseHelpers.load_settings(root_file);
//					String root_file_nm = root_file.getName();
//					IProject project = root_file.getProject();
//					IFile target_file = EclipseTargetLanguageHelpers.find_source_file_in_project_tree(project, settings,
//							dao_class_name, settings.getDao().getScope(), root_file_nm);
//					IWorkbenchWindow win = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
//					Shell active_shell = win.getShell();
//					EclipseEditorHelpers.open_editor_sync(active_shell, target_file);
//				} catch (Exception e1) {
//					e1.printStackTrace();
//					EclipseMessageHelpers.show_error(e1);
//				}
//			}
//		});
//	}

	@Override
	public void fill(Menu menu, int index) {
		IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
		String root_file_rel_path = add_xml_file_actions(menu, index, projects);
		index = menu.getItems().length;
		List<String> root_file_titles = EclipseToolbarUtils.get_root_file_titles(projects);
		if (index > 0 && root_file_titles.size() > 1) {
			new MenuItem(menu, SWT.SEPARATOR, index++);
		}
		for (String root_file_title : root_file_titles) {
			if (root_file_title.equals(root_file_rel_path)) {
				continue;
			}
			MenuItem menuItem = new MenuItem(menu, SWT.PUSH, index++);
			menuItem.setText(root_file_title);
			menuItem.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					try {
						EclipseToolbarUtils.open_root_file(root_file_title, projects);
					} catch (Exception e1) {
						e1.printStackTrace();
						EclipseMessageHelpers.show_error(e1);
					}
				}
			});
		}
		index = menu.getItems().length;
		if (index > 1) {
			new MenuItem(menu, SWT.SEPARATOR, index++);
		}
		MenuItem menuItem = new MenuItem(menu, SWT.PUSH, index++);
		menuItem.setText("About");
		menuItem.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				Shell active_shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
				UIDialogAbout.show_modal(active_shell);
			}
		});
	}

	@Override
	public boolean isDynamic() {
		return true; // !!! REQUIRED
	}
}