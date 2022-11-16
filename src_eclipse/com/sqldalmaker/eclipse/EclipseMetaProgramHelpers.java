/*
 * Copyright 2011-2020 sqldalmaker@gmail.com
 * SQL DAL Maker Website: https://sqldalmaker.sourceforge.net/
 * Read LICENSE.txt in the root of this project/archive for details.
 */
package com.sqldalmaker.eclipse;

import com.sqldalmaker.cg.Helpers;
import com.sqldalmaker.common.Const;

/**
 *
 * @author sqldalmaker@gmail.com
 *
 */
public class EclipseMetaProgramHelpers {

	public static void create_overwrite_xsd(IEditor2 editor2) {

		if (!EclipseMessageHelpers.show_confirmation(
				"This action creates/overwrites XSD files in the folder of XML meta-program. Continue?")) {
			return;
		}
		try {
			String mp_abs_path = editor2.get_metaprogram_folder_abs_path();
			{
				String text = EclipseHelpers.read_from_resource_folder(Const.DTO_XSD);
				String file_abs_path = Helpers.concat_path(mp_abs_path, Const.DTO_XSD);
				EclipseHelpers.save_text_to_file(file_abs_path, text);
			}
			EclipseHelpers.refresh_metaprogram_folder(editor2);
			{
				String text = EclipseHelpers.read_from_resource_folder(Const.DAO_XSD);
				String file_abs_path = Helpers.concat_path(mp_abs_path, Const.DAO_XSD);
				EclipseHelpers.save_text_to_file(file_abs_path, text);
			}
			EclipseHelpers.refresh_metaprogram_folder(editor2);
			{
				String text = EclipseHelpers.read_from_resource_folder(Const.SETTINGS_XSD);
				String file_abs_path = Helpers.concat_path(mp_abs_path, Const.SETTINGS_XSD);
				EclipseHelpers.save_text_to_file(file_abs_path, text);
			}
			EclipseHelpers.refresh_metaprogram_folder(editor2);
		} catch (final Exception e) {
			e.printStackTrace();
			EclipseMessageHelpers.show_error(e);
		}
	}

	public static void create_overwrite_settings_xml(IEditor2 editor2) {

		if (!EclipseMessageHelpers.show_confirmation(
				"This action creates/overwrites settings.xml in the folder of XML meta-program. Continue?")) {
			return;
		}
		try {
			String mp_abs_path = editor2.get_metaprogram_folder_abs_path();
			String text = EclipseHelpers.read_from_resource_folder(Const.SETTINGS_XML);
			String file_abs_path = Helpers.concat_path(mp_abs_path, Const.SETTINGS_XML);
			EclipseHelpers.save_text_to_file(file_abs_path, text);
			EclipseHelpers.refresh_metaprogram_folder(editor2);
		} catch (final Exception e) {
			e.printStackTrace();
			EclipseMessageHelpers.show_error(e);
		}
	}

	public static void create_dto_xml(IEditor2 editor2) {

		if (!EclipseMessageHelpers.show_confirmation(
				"This action creates/overwrites dto.xml in the folder of XML meta-program. Continue?")) {
			return;
		}
		try {
			String mp_abs_path = editor2.get_metaprogram_folder_abs_path();
			String text = EclipseHelpers.read_from_resource_folder(Const.DTO_XML);
			String file_abs_path = Helpers.concat_path(mp_abs_path, Const.DTO_XML);
			EclipseHelpers.save_text_to_file(file_abs_path, text);
			EclipseHelpers.refresh_metaprogram_folder(editor2);
		} catch (final Exception e) {
			e.printStackTrace();
			EclipseMessageHelpers.show_error(e);
		}
	}
}
