/*
 * Copyright 2011-2018 sqldalmaker@gmail.com
 * SQL DAL Maker Website: http://sqldalmaker.sourceforge.net
 * Read LICENSE.txt in the root of this project/archive for details.
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
	IFile find_dto_xml();

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
	IContainer get_metaprogram_folder();

	String get_metaprogram_folder_path_relative_to_project() throws Exception;

	String get_metaprogram_folder_abs_path() throws Exception;
}
