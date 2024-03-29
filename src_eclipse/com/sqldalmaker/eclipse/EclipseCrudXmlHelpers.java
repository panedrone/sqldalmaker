/*
	Copyright 2011-2024 sqldalmaker@gmail.com
	Read LICENSE.txt in the root of this project/archive.
	Project web-site: https://sqldalmaker.sourceforge.net/
*/
package com.sqldalmaker.eclipse;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.swt.widgets.Shell;

import com.sqldalmaker.cg.FieldNamesMode;
import com.sqldalmaker.cg.Helpers;
import com.sqldalmaker.cg.Helpers.IFileList;
import com.sqldalmaker.common.ISelectDbSchemaCallback;
import com.sqldalmaker.common.SdmUtils;
import com.sqldalmaker.jaxb.sdm.*;
import com.sqldalmaker.jaxb.settings.Settings;

/**
 * The class to control 1) DTO XML assistant 2) DAO XML assistant 3) FK access
 * XML assistant
 *
 * @author sqldalmaker@gmail.com
 *
 */
public class EclipseCrudXmlHelpers {

	public static void get_crud_sdm_xml(final Shell parent_shell, final IEditor2 editor2) throws Exception {

		ISelectDbSchemaCallback callback = new ISelectDbSchemaCallback() {

			public void process_ok(boolean schema_in_xml, String selected_schema, boolean skip_used,
					boolean include_views, boolean plural_to_singular, boolean crud_auto, boolean add_fk_access) {

				try {
					com.sqldalmaker.jaxb.sdm.ObjectFactory object_factory = new com.sqldalmaker.jaxb.sdm.ObjectFactory();
					Sdm root;
					Connection connection = EclipseHelpers.get_connection(editor2);
					try {
						Set<String> in_use; // !!!! after 'try'
						if (skip_used) {
							String project_abs_path = EclipseHelpers.get_absolute_dir_path_str(editor2.get_project());
							in_use = find_dto_declared_in_sdm_xml(editor2, project_abs_path);
						} else {
							in_use = new HashSet<String>();
						}
						root = SdmUtils.get_crud_sdm_xml(object_factory, connection, in_use, schema_in_xml,
								selected_schema, include_views, plural_to_singular);
					} finally {
						connection.close();
					}
					EclipseEditorHelpers.open_sdm_xml_in_editor_sync(object_factory, root);
				} catch (Throwable e) {
					e.printStackTrace();
					EclipseMessageHelpers.show_error(e);
				}
			}
		};
		UIDialogSelectDbSchema.open(parent_shell, editor2, callback, UIDialogSelectDbSchema.Open_Mode.DTO);
	}

	private static Set<String> find_dto_declared_in_sdm_xml(IEditor2 editor2, String project_abs_path)
			throws Exception {

		String sdm_xml_abs_path = editor2.get_sdm_xml_abs_path();
		String sdm_xsd_abs_path = editor2.get_sdm_xsd_abs_path();
		Set<String> res = SdmUtils.get_dto_class_names_used_in_sdm_xml(sdm_xml_abs_path, sdm_xsd_abs_path);
		return res;
	}

	private static List<String> get_dao_xml_file_name_list(IProject project, String xml_configs_folder_full_path) {

		final List<String> res = new ArrayList<String>();
		IFileList file_list = new IFileList() {
			@Override
			public void add(String abs_file_path) {
				res.add(abs_file_path);
			}
		};
		Helpers.enum_dao_xml_file_names(xml_configs_folder_full_path, file_list);
		return res;
	}

	public static Set<String> find_dto_used_in_dao_xml_crud(IEditor2 editor2, String project_abs_path)
			throws Exception {

		String metaprogram_folder_abs_path = editor2.get_sdm_folder_abs_path();
		List<String> dao_xml_file_name_list = get_dao_xml_file_name_list(editor2.get_project(),
				editor2.get_sdm_folder_abs_path());
		Set<String> res = SdmUtils.find_dto_used_in_dao_xml_crud(metaprogram_folder_abs_path, dao_xml_file_name_list);
		return res;
	}

	private static FieldNamesMode get_field_names_mode(IEditor2 editor2, Settings settings) {

		boolean force_snake_case = EclipseTargetLanguageHelpers.snake_case_needed(editor2);
		FieldNamesMode field_names_mode;
		if (force_snake_case) {
			field_names_mode = FieldNamesMode.SNAKE_CASE;
		} else {
			boolean force_lower_camel_case = EclipseTargetLanguageHelpers.lower_camel_case_needed(editor2);
			if (force_lower_camel_case) {
				field_names_mode = FieldNamesMode.LOWER_CAMEL_CASE;
			} else {
				int fnm = settings.getDto().getFieldNamesMode();
				if (fnm == 1) {
					field_names_mode = FieldNamesMode.LOWER_CAMEL_CASE;
				} else {
					field_names_mode = FieldNamesMode.SNAKE_CASE;
				}
			}
		}
		return field_names_mode;
	}

	public static void get_crud_dao_xml(Shell parent_shell, final IEditor2 editor2) throws Exception {

		ISelectDbSchemaCallback callback = new ISelectDbSchemaCallback() {
			@Override
			public void process_ok(boolean schema_in_xml, String selected_schema, boolean skip_used,
					boolean include_views, boolean plural_to_singular, boolean use_crud_auto, boolean add_fk_access) {
				
				try {
					ObjectFactory object_factory = new ObjectFactory();
					DaoClass root;
					Settings settings = EclipseHelpers.load_settings(editor2);
					FieldNamesMode field_names_mode = get_field_names_mode(editor2, settings);
					Connection connection = EclipseHelpers.get_connection(editor2);
					try {
						// !!!! after 'try'
						Set<String> in_use;
						if (skip_used) {
							String project_root = EclipseHelpers.get_absolute_dir_path_str(editor2.get_project());
							in_use = find_dto_used_in_dao_xml_crud(editor2, project_root);
						} else {
							in_use = new HashSet<String>();
						}
						root = SdmUtils.create_crud_xml_jaxb_dao_class(object_factory, connection, in_use,
								schema_in_xml, selected_schema, include_views, use_crud_auto, add_fk_access,
								plural_to_singular, field_names_mode);
					} finally {
						connection.close();
					}

					EclipseEditorHelpers.open_dao_xml_in_editor_sync(object_factory.getClass().getPackage().getName(),
							"_crud-dao.xml", root); // '%' throws URI
													// exception in
													// NB
				} catch (Throwable e) {
					e.printStackTrace();
					EclipseMessageHelpers.show_error(e);
				}
			}
		};
		UIDialogSelectDbSchema.open(parent_shell, editor2, callback, UIDialogSelectDbSchema.Open_Mode.DAO);
	}

	public static void get_fk_access_xml(Shell parent_shell, final IEditor2 editor2) throws Exception {

		ISelectDbSchemaCallback callback = new ISelectDbSchemaCallback() {
			@Override
			public void process_ok(boolean schema_in_xml, String selected_schema, boolean skip_used,
					boolean include_views, boolean plural_to_singular, boolean use_crud_auto, boolean add_fk_access) {
				try {
					ObjectFactory object_factory = new ObjectFactory();
					DaoClass root;
					Settings settings = EclipseHelpers.load_settings(editor2);
					FieldNamesMode field_names_mode = get_field_names_mode(editor2, settings);
					Connection conn = EclipseHelpers.get_connection(editor2);
					try {
						root = SdmUtils.get_fk_access_xml(conn, object_factory, schema_in_xml, selected_schema,
								plural_to_singular, field_names_mode);
					} finally {
						conn.close();
					}
					EclipseEditorHelpers.open_dao_xml_in_editor_sync(object_factory.getClass().getPackage().getName(),
							"_fk-dao.xml", root); // '%' throws URI
													// exception in NB
				} catch (Throwable e) {
					e.printStackTrace();
					EclipseMessageHelpers.show_error(e);
				}
			}
		};
		UIDialogSelectDbSchema.open(parent_shell, editor2, callback, UIDialogSelectDbSchema.Open_Mode.FK);
	}
}