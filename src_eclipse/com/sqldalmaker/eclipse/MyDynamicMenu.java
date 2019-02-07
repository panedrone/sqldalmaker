/*
 * Copyright 2011-2018 sqldalmaker@gmail.com
 * SQL DAL Maker Website: http://sqldalmaker.sourceforge.net
 * Read LICENSE.txt in the root of this project/archive for details.
 */
package com.sqldalmaker.eclipse;

import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.action.ContributionItem;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

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

	private static void add_empty_item(Menu menu) {

		MenuItem menuItem = new MenuItem(menu, SWT.PUSH, 0);
		menuItem.setText("(empty)");
		menuItem.setEnabled(false);
	}

	@Override
	public void fill(Menu menu, int index) {

		final IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();

		List<String> dal_file_titles = MyToolbarUtils.get_dal_file_titles(projects);

		if (dal_file_titles.isEmpty()) {

			add_empty_item(menu);

		} else {

			for (final String name : dal_file_titles) {

				MenuItem menuItem = new MenuItem(menu, SWT.PUSH, index);
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