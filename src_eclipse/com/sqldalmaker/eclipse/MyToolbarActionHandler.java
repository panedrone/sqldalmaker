/*
 * Copyright 2011-2020 sqldalmaker@gmail.com
 * SQL DAL Maker Website: http://sqldalmaker.sourceforge.net
 * Read LICENSE.txt in the root of this project/archive for details.
 */
package com.sqldalmaker.eclipse;

import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;

/**
 *
 * @author sqldalmaker@gmail.com
 *
 */
public class MyToolbarActionHandler extends AbstractHandler {

	public MyToolbarActionHandler() {
	}

	public Object execute(ExecutionEvent event) throws ExecutionException {
		final IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
		List<String> dal_file_titles = MyToolbarUtils.get_dal_file_titles(projects);
		if (dal_file_titles.size() >= 1) {
			try {
				MyToolbarUtils.open_dal_file(dal_file_titles.get(0), projects);
			} catch (Exception e) {
				e.printStackTrace();
				EclipseMessageHelpers.show_error(e);
			}
		}
		return null;
	}

//	@Override
//	public boolean isEnabled() {
//		return false;
//	}
}
