/*
	Copyright 2011-2022 sqldalmaker@gmail.com
	Read LICENSE.txt in the root of this project/archive.
	Project web-site: http://sqldalmaker.sourceforge.net
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
import org.eclipse.ui.PlatformUI;

import com.sqldalmaker.common.InternalException;

/**
 * @author sqldalmaker@gmail.com
 *
 */
public class EclipseToolbarUtils {

	private static void enum_root_files(final IContainer dir, final List<String> res) {
		try {
			IResource[] members = dir.members(); // throws
			for (IResource r : members) {
				if (r instanceof IFolder) {
					if (!r.isHidden()) {
						if (!r.getName().equals("bin")) {
							enum_root_files((IFolder) r, res);
						}
					}
				} else if (r instanceof IFile) {
					String rel_path = EclipseTargetLanguageHelpers.get_root_file_relative_path((IFile) r);
					if (rel_path != null) {
						res.add(rel_path);
					}
				}
			}
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	public static List<String> get_root_file_titles(final IProject[] projects) {
		List<String> res = new ArrayList<String>();
		for (IProject pr : projects) {
			if (!(pr instanceof IContainer)) {
				continue;
			}
			IContainer project_root = (IContainer) pr;
			List<String> rel_path_names = new ArrayList<String>();
			enum_root_files(project_root, rel_path_names);
			for (String name : rel_path_names) {
				res.add(name.substring(1));
			}
		}
		return res;
	}

	public static void open_root_file(String dal_title, IProject[] projects)
			throws Exception {
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
				EclipseEditorHelpers.open_editor_sync(shell, file);
				// System.err.println(dal_rel_path);
				break;
			}
		}
	}
}
