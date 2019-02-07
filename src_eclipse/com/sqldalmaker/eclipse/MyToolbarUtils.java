/*
 * Copyright 2011-2018 sqldalmaker@gmail.com
 * SQL DAL Maker Website: http://sqldalmaker.sourceforge.net
 * Read LICENSE.txt in the root of this project/archive for details.
 */
package com.sqldalmaker.eclipse;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import com.sqldalmaker.common.InternalException;

/**
 *
 * @author sqldalmaker@gmail.com
 *
 */
public class MyToolbarUtils {

	private static void enum_dal_files(final IContainer dir, final List<String> res) {

		try {

			IResource[] members = dir.members(); // throws

			for (IResource r : members) {

				if (r instanceof IFolder) {

					if (!r.isHidden()) {

						enum_dal_files((IFolder) r, res);
					}

				} else if (r instanceof IFile) {

					String rel_path = ((IFile) r).getFullPath().toPortableString();

					if (rel_path.endsWith(".dal")) {

						res.add(rel_path);
					}
				}
			}

		} catch (Throwable e) {

			e.printStackTrace();
		}
	}

	public static List<String> get_dal_file_titles(final IProject[] projects) {

		List<String> res = new ArrayList<String>();

		for (IProject pr : projects) {

			if (!(pr instanceof IContainer)) {

				continue;
			}

			IContainer project_root = (IContainer) pr;

			List<String> rel_path_names = new ArrayList<String>();

			enum_dal_files(project_root, rel_path_names);

			for (String name : rel_path_names) {

				res.add(name.substring(1));
			}
		}

		return res;
	}

	public static void open_dal_file(String dal_title, IProject[] projects) throws InternalException, PartInitException {

		String dal_file_path = dal_title.replace("\\", "/");

		String[] segments = dal_file_path.split("/");

		if (segments.length < 1) {

			throw new InternalException("Invalid file path: " + dal_file_path);
		}

		String dal_file_project_name = segments[0];

		for (IProject pr : projects) {

			String curr_project_name = pr.getName();

			if (dal_file_project_name.equals(curr_project_name)) {

				String dal_rel_path = dal_title.substring(curr_project_name.length());

				Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();

				IFile file = pr.getFile(dal_rel_path);

				if (file == null) {

					throw new InternalException("File not found: " + dal_rel_path);
				}

				EclipseEditorHelpers.open_editor_sync(shell, file, false);

				// System.err.println(dal_rel_path);

				break;
			}
		}
	}
}
