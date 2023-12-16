/*
	Copyright 2011-2023 sqldalmaker@gmail.com
	Read LICENSE.txt in the root of this project/archive.
	Project web-site: https://sqldalmaker.sourceforge.net/
*/
package com.sqldalmaker.eclipse;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;

/**
 *
 * @author sqldalmaker@gmail.com
 *
 */
public interface IEditor2 {

	String get_root_file_name();

	IProject get_project();

	/**
	 * @return null if not found
	 */
	IFile find_metaprogram_file(String name);

	String get_metaprogram_file_abs_path(String name) throws Exception;

	/**
	 * @return null if not found
	 */
	IFile find_sdm_xml();

	String get_dto_xml_abs_path() throws Exception;

	String get_dto_xsd_abs_path() throws Exception;

	String get_dao_xsd_abs_path() throws Exception;

	/**
	 * @return null if not found
	 */
	IFile find_settings_xml();

	/**
	 * @return 1) IProject if meta-program is located directly in project root 2)
	 *         IFolder for project sub-folder
	 */
	IContainer get_sdm_folder();

	String get_sdm_folder_path_relative_to_project() throws Exception;

	String get_sdm_folder_abs_path() throws Exception;
}
