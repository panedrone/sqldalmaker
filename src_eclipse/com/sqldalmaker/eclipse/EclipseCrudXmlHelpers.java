/*
 * Copyright 2011-2019 sqldalmaker@gmail.com
 * SQL DAL Maker Website: http://sqldalmaker.sourceforge.net
 * Read LICENSE.txt in the root of this project/archive for details.
 * 
 */
package com.sqldalmaker.eclipse;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.swt.widgets.Shell;

import com.sqldalmaker.common.FileSearchHelpers;
import com.sqldalmaker.common.ISelectDbSchemaCallback;
import com.sqldalmaker.common.SdmUtils;
import com.sqldalmaker.common.XmlParser;
import com.sqldalmaker.jaxb.dao.DaoClass;
import com.sqldalmaker.jaxb.dao.ObjectFactory;
import com.sqldalmaker.jaxb.dto.DtoClass;
import com.sqldalmaker.jaxb.dto.DtoClasses;

/**
 * The class to control DTO XML assistant, DAO XML assistant, FK access XML
 * assistant
 *
 * @author sqldalmaker@gmail.com
 *
 */
public class EclipseCrudXmlHelpers {

	public static void get_crud_dto_xml(final Shell parent_shell, final IEditor2 editor2) throws Exception {

		ISelectDbSchemaCallback callback = new ISelectDbSchemaCallback() {

			public void process_ok(String selected_schema, boolean skip_used, boolean include_views,
					boolean plural_to_singular, boolean crud_auto, boolean add_fk_access) {

				try {

					com.sqldalmaker.jaxb.dto.ObjectFactory object_factory = new com.sqldalmaker.jaxb.dto.ObjectFactory();

					DtoClasses root;

					Connection connection = EclipseHelpers.get_connection(editor2);

					try {

						Set<String> in_use; // !!!! after 'try'

						if (skip_used) {

							String project_abs_path = EclipseHelpers.get_absolute_dir_path_str(editor2.get_project());

							in_use = find_tables_used_in_dto_xml(editor2, project_abs_path);

						} else {

							in_use = new HashSet<String>();
						}

						root = SdmUtils.get_crud_dto_xml(object_factory, connection, in_use, selected_schema,
								include_views, plural_to_singular);

					} finally {

						connection.close();
					}

					EclipseEditorHelpers.open_dto_xml_in_editor_sync(object_factory, root);

				} catch (Throwable e) {

					e.printStackTrace();

					EclipseMessageHelpers.show_error(e);
				}
			}
		};

		UIDialogSelectDbSchema.open(parent_shell, editor2, callback, UIDialogSelectDbSchema.Open_Mode.DTO);
	}

	private static Set<String> find_tables_used_in_dto_xml(IEditor2 editor2, String project_abs_path) throws Exception {

		Set<String> res = new HashSet<String>();

		String dto_xml_abs_path = editor2.get_dto_xml_abs_path();

		String dto_xsd_abs_path = editor2.get_dto_xsd_abs_path();

		List<DtoClass> list = SdmUtils.get_dto_classes(dto_xml_abs_path, dto_xsd_abs_path);

		for (DtoClass cls : list) {

			String ref = cls.getRef();

			if (ref.toLowerCase().endsWith(".sql") == false) {

				res.add(ref);
			}
		}

		return res;
	}

	private static ArrayList<String> get_dao_file_names(IProject project, String xml_configs_folder_full_path) {

		final ArrayList<String> res = new ArrayList<String>();

		FileSearchHelpers.IFile_List file_list = new FileSearchHelpers.IFile_List() {

			@Override
			public void add(String abs_file_path) {

				res.add(abs_file_path);
			}
		};

		FileSearchHelpers.enum_dao_xml_file_names(xml_configs_folder_full_path, file_list);

		return res;
	}

	static Set<String> find_tables_used_in_dao_xml(IEditor2 editor2, String project_abs_path) throws Exception {

		Set<String> res = new HashSet<String>();

		ArrayList<String> dao_file_names = get_dao_file_names(editor2.get_project(),
				editor2.get_metaprogram_folder_abs_path());

		String context_path = DaoClass.class.getPackage().getName();

		String xsd_file_path = editor2.get_dao_xsd_abs_path();

		XmlParser xml_parser = new XmlParser(context_path, xsd_file_path);

		for (int i = 0; i < dao_file_names.size(); i++) {

			String file_name = dao_file_names.get(i);

			String xml_file_abs_path = editor2.get_metaprogram_file_abs_path(file_name);

			Set<String> dao_tables = SdmUtils.find_tables_in_use(xml_parser, xml_file_abs_path);

			for (String t : dao_tables) {

				res.add(t);
			}
		}

		return res;
	}

	public static void get_crud_dao_xml(Shell parent_shell, final IEditor2 editor2) throws Exception {

		ISelectDbSchemaCallback callback = new ISelectDbSchemaCallback() {

			@Override
			public void process_ok(String selected_schema, boolean skip_used, boolean include_views,
					boolean plural_to_singular, boolean use_crud_auto, boolean add_fk_access) {

				try {

					boolean underscores_needed = EclipseTargetLanguageHelpers.underscores_needed(editor2);

					ObjectFactory object_factory = new ObjectFactory();

					DaoClass root;

					Connection connection = EclipseHelpers.get_connection(editor2);

					try {

						// !!!! after 'try'
						Set<String> in_use;

						if (skip_used) {

							String project_root = EclipseHelpers.get_absolute_dir_path_str(editor2.get_project());

							in_use = find_tables_used_in_dao_xml(editor2, project_root);

						} else {

							in_use = new HashSet<String>();
						}

						root = SdmUtils.create_crud_xml_DaoClass(object_factory, connection, in_use, selected_schema,
								include_views, use_crud_auto, add_fk_access, plural_to_singular, underscores_needed);

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
			public void process_ok(String selected_schema, boolean skip_used, boolean include_views,
					boolean plural_to_singular, boolean use_crud_auto, boolean add_fk_access) {

				try {

					boolean underscores_needed = EclipseTargetLanguageHelpers.underscores_needed(editor2);

					ObjectFactory object_factory = new ObjectFactory();

					DaoClass root;

					Connection conn = EclipseHelpers.get_connection(editor2);

					try {

						root = SdmUtils.get_fk_access_xml(conn, object_factory, selected_schema, plural_to_singular,
								underscores_needed);

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