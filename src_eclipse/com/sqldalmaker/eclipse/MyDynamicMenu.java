/*
 * Copyright 2011-2020 sqldalmaker@gmail.com
 * SQL DAL Maker Website: http://sqldalmaker.sourceforge.net
 * Read LICENSE.txt in the root of this project/archive for details.
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
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.ResourceUtil;

import com.sqldalmaker.common.FileSearchHelpers;
import com.sqldalmaker.common.InternalException;

/**
 *
 * @author sqldalmaker@gmail.com
 *
 */
public class MyDynamicMenu extends ContributionItem {

	// http://insights.sigasi.com/tech/dynamic-menu-items-eclipse.html

	public MyDynamicMenu() {
	}

	public MyDynamicMenu(String id) {
		super(id);
	}

	private static void add_item_no_items(Menu menu) {
		MenuItem menuItem = new MenuItem(menu, SWT.PUSH, 0);
		menuItem.setText("(empty)");
		menuItem.setEnabled(false);
	}

	@Override
	public void fill(Menu menu, int index) {
		final IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
		List<String> dal_file_titles = MyToolbarUtils.get_dal_file_titles(projects);
		if (dal_file_titles.isEmpty()) {
			add_item_no_items(menu);
		} else {
			final IWorkbenchWindow win = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
			final IWorkbenchPage page = win.getActivePage();
			if (page != null) {
				final IEditorPart editor_part = page.getActiveEditor();
				if (editor_part != null) {
					final IEditorInput input = editor_part.getEditorInput();
					if (input != null) {
						final IFile input_file = ResourceUtil.getFile(input);
						final IContainer xml_mp_folder = input_file.getParent();
						if (!(xml_mp_folder instanceof IFolder)) {
							return;
						}
						List<IFile> root_files;
						try {
							root_files = EclipseTargetLanguageHelpers.get_root_files(xml_mp_folder);
						} catch (Exception e1) {
							e1.printStackTrace();
							return;
						}
						if (root_files.size() != 1) {
							return;
						}
						final IFile root_file = root_files.get(0);
						boolean is_dto_xml = FileSearchHelpers.is_dto_xml(input_file.getName());
						boolean is_dao_xml = FileSearchHelpers.is_dao_xml(input_file.getName());
						String path = input_file.getFullPath().toPortableString();
						if (path.startsWith("/")) {
							path = path.substring(1);
						}
						final String current_xml_file_rel_path = path;
						if (is_dto_xml) {
							MenuItem menuItem = new MenuItem(menu, SWT.PUSH, index++);
							menuItem.setText(current_xml_file_rel_path + " -> Generate All");
							menuItem.addSelectionListener(new SelectionAdapter() {
								public void widgetSelected(SelectionEvent e) {
									editor_part.setFocus(); // to make working ${project_loc}
									EclipseCG.generate_all_dto(root_file, input_file);
								}
							});
							menuItem = new MenuItem(menu, SWT.PUSH, index++);
							menuItem.setText(current_xml_file_rel_path + " -> Validate All");
							menuItem.addSelectionListener(new SelectionAdapter() {
								public void widgetSelected(SelectionEvent e) {
									editor_part.setFocus(); // to make working ${project_loc}
									EclipseCG.validate_all_dto(root_file, input_file);
								}
							});
						} else if (is_dao_xml) {
							MenuItem menuItem = new MenuItem(menu, SWT.PUSH, index++);
							menuItem.setText(current_xml_file_rel_path + " -> Generate");
							menuItem.addSelectionListener(new SelectionAdapter() {
								public void widgetSelected(SelectionEvent e) {
									editor_part.setFocus(); // to make working ${project_loc}
									EclipseCG.generate_dao(root_file, input_file);
								}
							});
							menuItem = new MenuItem(menu, SWT.PUSH, index++);
							menuItem.setText(current_xml_file_rel_path + " -> Validate");
							menuItem.addSelectionListener(new SelectionAdapter() {
								public void widgetSelected(SelectionEvent e) {
									editor_part.setFocus(); // to make working ${project_loc}
									EclipseCG.validate_dao(root_file, input_file);
								}
							});
						}
						if (is_dto_xml || is_dao_xml) {
							if (dal_file_titles.size() > 1) {
								path = root_file.getFullPath().toPortableString();
								if (path.startsWith("/")) {
									path = path.substring(1);
								}
								final String root_file_rel_path = path;
								MenuItem menuItem = new MenuItem(menu, SWT.PUSH, index++);
								menuItem.setText(root_file_rel_path);
								menuItem.addSelectionListener(new SelectionAdapter() {
									public void widgetSelected(SelectionEvent e) {
										try {
											MyToolbarUtils.open_dal_file(root_file_rel_path, projects);
										} catch (PartInitException | InternalException e1) {
											e1.printStackTrace();
											EclipseMessageHelpers.show_error(e1);
										}
									}
								});
							}
							new MenuItem(menu, SWT.SEPARATOR, index++);
						}
					}
				}
			}
			////////////////////////////////////////////////////////////
			for (final String name : dal_file_titles) {
				MenuItem menuItem = new MenuItem(menu, SWT.PUSH, index++);
				menuItem.setText(name);
				menuItem.addSelectionListener(new SelectionAdapter() {
					public void widgetSelected(SelectionEvent e) {
						try {
							MyToolbarUtils.open_dal_file(name, projects);
						} catch (Exception e1) {
							e1.printStackTrace();
							EclipseMessageHelpers.show_error(e1);
						}
					}
				});
			}
		}
	}

	@Override
	public boolean isDynamic() {
		return true; // !!! REQUIRED
	}
}