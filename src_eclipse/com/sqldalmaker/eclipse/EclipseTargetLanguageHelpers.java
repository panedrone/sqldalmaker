/*
	Copyright 2011-2024 sqldalmaker@gmail.com
	Read LICENSE.txt in the root of this project/archive.
	Project web-site: https://sqldalmaker.sourceforge.net/
*/
package com.sqldalmaker.eclipse;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;

import com.sqldalmaker.cg.IDaoCG;
import com.sqldalmaker.cg.IDtoCG;
import com.sqldalmaker.common.Const;
import com.sqldalmaker.common.SdmUtils;
import com.sqldalmaker.common.TargetLangUtils;
import com.sqldalmaker.jaxb.settings.Settings;

/**
 * @author sqldalmaker@gmail.com
 *
 */
public class EclipseTargetLanguageHelpers {

	public static boolean snake_case_needed(IEditor2 editor2) {
		String root_fn = editor2.get_root_file_name();
		return TargetLangUtils.snake_case_needed(root_fn);
	}

	public static boolean lower_camel_case_needed(IEditor2 editor2) {
		String root_fn = editor2.get_root_file_name();
		return TargetLangUtils.lower_camel_case_needed(root_fn);
	}

	public static List<IFile> get_root_files(IContainer xml_mp_folder) throws Exception {
		if (!(xml_mp_folder instanceof IFolder)) {
			throw new Exception("IFolder expected");
		}
		List<IFile> root_files = new ArrayList<IFile>();
		String[] rf_names = { Const.Root.PHP, Const.Root.JAVA, Const.Root.CPP, Const.Root.PYTHON, Const.Root.GO };
		for (String rf : rf_names) {
			IResource res = xml_mp_folder.findMember(rf);
			if (res instanceof IFile) {
				root_files.add((IFile) res);
			}
		}
		return root_files;
	}

	public static IFile find_root_file(IContainer xml_mp_folder) throws Exception {
		List<IFile> root_files = get_root_files(xml_mp_folder);
		if (root_files.size() == 0) {
			throw new Exception("Root files not found");
		}
		return root_files.get(0);
	}

	public static String get_rel_path(IEditor2 editor2, String output_dir, String class_name) throws Exception {
		String fn = editor2.get_root_file_name();
		return TargetLangUtils.get_target_file_path(fn, output_dir, class_name);
	}

	public static IFile find_source_file_in_project_tree(IProject project, Settings settings, String class_name,
			String class_scope, String root_fn) throws Exception {

		String output_dir;
		if (Const.Root.JAVA.equals(root_fn) || Const.Root.PHP.equals(root_fn)) {
			output_dir = SdmUtils.get_package_relative_path(settings, class_scope);
		} else {
			output_dir = settings.getFolders().getTarget();
		}
		String rel_path = TargetLangUtils.get_target_file_path(root_fn, output_dir, class_name);
		return project.getFile(rel_path);
	}

	public static String get_root_file_relative_path(final IFile file) {
		String fn = file.getName();
		if (Const.Root.JAVA.equals(fn) || Const.Root.CPP.equals(fn) || Const.Root.PHP.equals(fn)
				|| Const.Root.PYTHON.equals(fn) || Const.Root.GO.equals(fn)) {
			try {
				return file.getFullPath().toPortableString();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return null;
	}
}