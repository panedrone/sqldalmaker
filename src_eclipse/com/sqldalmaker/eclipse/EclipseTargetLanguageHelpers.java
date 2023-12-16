/*
	Copyright 2011-2023 sqldalmaker@gmail.com
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
import com.sqldalmaker.common.RootFileName;
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
		String[] rf_names = { RootFileName.PHP, RootFileName.JAVA, RootFileName.CPP, RootFileName.GO };
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
		if (RootFileName.JAVA.equals(root_fn) || RootFileName.PHP.equals(root_fn)) {
			output_dir = SdmUtils.get_package_relative_path(settings, class_scope);
		} else {
			output_dir = settings.getFolders().getTarget();
		}
		String rel_path = TargetLangUtils.get_target_file_path(root_fn, output_dir, class_name);
		return project.getFile(rel_path);
	}

	public static String get_root_file_relative_path(final IFile file) {

		String fn = file.getName();
		if (RootFileName.JAVA.equals(fn) || RootFileName.CPP.equals(fn) || RootFileName.PHP.equals(fn)
				|| RootFileName.PYTHON.equals(fn) || RootFileName.GO.equals(fn)) {
			try {
				return file.getFullPath().toPortableString();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	public static IDtoCG create_dto_cg(Connection conn, IEditor2 editor2, Settings settings,
			StringBuilder output_dir_rel_path) throws Exception {

		IProject project = editor2.get_project();
		String root_fn = editor2.get_root_file_name();
		String xml_configs_folder_full_path = editor2.get_sdm_folder_abs_path();
		return create_dto_cg(conn, project, settings, root_fn, xml_configs_folder_full_path, output_dir_rel_path);
	}

	public static IDtoCG create_dto_cg(Connection conn, IProject project, Settings settings, String root_fn,
			String xml_configs_folder_full_path, StringBuilder output_dir_abs_path) throws Exception {

		String project_abs_path = project.getLocation().toPortableString();
		StringBuilder output_dir_rel_path = new StringBuilder();
		IDtoCG gen = TargetLangUtils.create_dto_cg(root_fn, project_abs_path, xml_configs_folder_full_path, conn,
				settings, output_dir_rel_path);
		String abs_path = EclipseHelpers.get_absolute_dir_path_str(project, output_dir_rel_path.toString());
		output_dir_abs_path.append(abs_path);
		return gen;
	}

	public static IDaoCG create_dao_cg(Connection conn, IProject project, IEditor2 editor2, Settings settings,
			StringBuilder output_dir_abs_path) throws Exception {

		String root_fn = editor2.get_root_file_name();
		String xml_configs_folder_full_path = editor2.get_sdm_folder_abs_path();
		return create_dao_cg(conn, project, root_fn, settings, xml_configs_folder_full_path, output_dir_abs_path);
	}

	public static IDaoCG create_dao_cg(Connection conn, IProject project, String root_fn, Settings settings,
			String xml_configs_folder_full_path, StringBuilder output_dir_abs_path) throws Exception {

		String project_abs_path = project.getLocation().toPortableString();
		StringBuilder output_dir_rel_path = new StringBuilder();
		IDaoCG gen = TargetLangUtils.create_dao_cg(root_fn, project_abs_path, xml_configs_folder_full_path, conn,
				settings, output_dir_rel_path);
		String abs_path = EclipseHelpers.get_absolute_dir_path_str(project, output_dir_rel_path.toString());
		output_dir_abs_path.append(abs_path);
		return gen;
	}
}