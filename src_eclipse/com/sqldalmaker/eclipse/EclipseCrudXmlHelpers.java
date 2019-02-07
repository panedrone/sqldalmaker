/*
 * Copyright 2011-2018 sqldalmaker@gmail.com
 * SQL DAL Maker Website: http://sqldalmaker.sourceforge.net
 * Read LICENSE.txt in the root of this project/archive for details.
 */
package com.sqldalmaker.eclipse;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.swt.widgets.Shell;

import com.sqldalmaker.cg.DbUtils;
import com.sqldalmaker.cg.Helpers;
import com.sqldalmaker.common.FileSearchHelpers;
import com.sqldalmaker.common.ISelectDbSchemaCallback;
import com.sqldalmaker.common.XmlParser;
import com.sqldalmaker.jaxb.dao.Crud;
import com.sqldalmaker.jaxb.dao.CrudAuto;
import com.sqldalmaker.jaxb.dao.DaoClass;
import com.sqldalmaker.jaxb.dao.ObjectFactory;
import com.sqldalmaker.jaxb.dao.QueryDtoList;
import com.sqldalmaker.jaxb.dao.TypeMethod;
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

					String project_abs_path = EclipseHelpers.get_absolute_dir_path_str(editor2.get_project());

					com.sqldalmaker.jaxb.dto.ObjectFactory object_factory = new com.sqldalmaker.jaxb.dto.ObjectFactory();

					DtoClasses dto_classes = object_factory.createDtoClasses();

					Connection con = EclipseHelpers.get_connection(editor2);

					try {

						// !!!! after 'try'
						Set<String> used;

						if (skip_used) {

							used = find_tables_used_in_dto_xml(editor2, project_abs_path);

						} else {

							used = new HashSet<String>();
						}

						List<DtoClass> items = dto_classes.getDtoClass();

						DatabaseMetaData db_info = con.getMetaData();

						ResultSet rs = DbUtils.get_tables(con, db_info, selected_schema, include_views);

						try {

							while (rs.next()) {

								// http://docs.oracle.com/javase/1.4.2/docs/api/java/sql/DatabaseMetaData.html

								try {

									String table_type = rs.getString("TABLE_TYPE");

									if (!("TABLE".equalsIgnoreCase(table_type)
											|| "VIEW".equalsIgnoreCase(table_type))) {

										continue;
									}

								} catch (Throwable e) {

								}

								String table_name = rs.getString("TABLE_NAME");

								if (used.contains(table_name) == false) {

									DtoClass cls = object_factory.createDtoClass();
									String dto_class_name = EclipseHelpers.table_name_to_dto_class_name(table_name,
											plural_to_singular);
									cls.setName(dto_class_name);
									cls.setRef(/* "table:" + */table_name);
									items.add(cls);
								}
							}

						} finally {

							rs.close();
						}

					} finally {

						con.close();
					}

					EclipseEditorHelpers.open_dto_xml_in_editor_sync(object_factory, dto_classes);

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

		List<DtoClass> list = EclipseHelpers.get_dto_classes(dto_xml_abs_path, dto_xsd_abs_path);

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

	private static Set<String> find_tables_in_use(XmlParser xml_parser, String xml_file_abs_path) throws Exception {

		Set<String> res = new HashSet<String>();

		DaoClass dao_class = xml_parser.unmarshal(xml_file_abs_path);

		if (dao_class.getCrudOrCrudAutoOrQuery() != null) {

			for (int i = 0; i < dao_class.getCrudOrCrudAutoOrQuery().size(); i++) {

				Object element = dao_class.getCrudOrCrudAutoOrQuery().get(i);

				if (element instanceof Crud) {

					Crud c = (Crud) element;
					
					res.add(c.getTable());

				} else if (element instanceof CrudAuto) {

					CrudAuto c = (CrudAuto) element;
					
					res.add(c.getTable());
				}
			}
		}

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

			Set<String> dao_tables = find_tables_in_use(xml_parser, xml_file_abs_path);

			for (String t : dao_tables) {

				res.add(t);
			}
		}

		return res;
	}

	private static void add_fk_access(final IEditor2 editor2, Connection conn, String selected_schema,
			DatabaseMetaData db_info, ResultSet rs_tables, List<Object> nodes, boolean plural_to_singular)
			throws SQLException {

		try {

			String table_type = rs_tables.getString("TABLE_TYPE");

			if (!("TABLE".equalsIgnoreCase(table_type) /*
														 * || "VIEW". equalsIgnoreCase( tableType)
														 */)) {
				return;
			}

		} catch (Throwable e) {

		}

		String fk_table_name = rs_tables.getString("TABLE_NAME");

		ResultSet rs = db_info.getImportedKeys(conn.getCatalog(), selected_schema, fk_table_name);

		// [pk_table_name - list of fk_column_name]

		HashMap<String, List<String>> hm = new HashMap<String, List<String>>();

		try {

			while (rs.next()) {

				String pk_table_name = rs.getString("PKTABLE_NAME");

				String fk_column_name = rs.getString("FKCOLUMN_NAME");

				if (!hm.containsKey(pk_table_name)) {

					hm.put(pk_table_name, new ArrayList<String>());
				}

				hm.get(pk_table_name).add(fk_column_name);

				// int fkSequence = rs.getInt("KEY_SEQ");
				//
				// System.out.print(fk_table_name);
				// System.out.print("\t" + fk_column_name);
				// System.out.println("\t" + fkSequence);
			}

		} finally {

			rs.close();
		}

		for (String pk_table_name : hm.keySet()) {

			List<String> fk_column_names = hm.get(pk_table_name);

			String params = "";
			
			String columns = "";

			boolean first = true;

			for (String fk_column_name : fk_column_names) {

				String c = fk_column_name;

				// if (ProfileUtils.underscores_needed(editor2)) {
				c = Helpers.camel_case_to_lower_under_scores(c);
				// }

				if (first) {

					params += c;
					
					columns += fk_column_name;

				} else {

					params += ", " + c;
					
					columns += ", " + fk_column_name;
				}

				first = false;
			}

			String dto_class_name = EclipseHelpers.table_name_to_dto_class_name(fk_table_name, plural_to_singular);

			String method_name = "get" + EclipseHelpers.table_name_to_dto_class_name(fk_table_name, false) + "By"
					+ EclipseHelpers.table_name_to_dto_class_name(pk_table_name, true);

			QueryDtoList node = new QueryDtoList();

			node.setDto(dto_class_name);

			if (EclipseTargetLanguageHelpers.underscores_needed(editor2)) {

				method_name = Helpers.camel_case_to_lower_under_scores(method_name);
			}

			String method = method_name + "(" + params.toLowerCase() + ")";

			node.setMethod(method);

			node.setRef(fk_table_name + "(" + columns + ")");

			nodes.add(node);
		}
	}

	public static void get_crud_dao_xml(Shell parent_shell, final IEditor2 editor2) throws Exception {

		ISelectDbSchemaCallback callback = new ISelectDbSchemaCallback() {

			@Override
			public void process_ok(String selected_schema, boolean skip_used, boolean include_views,
					boolean plural_to_singular, boolean use_crud_auto, boolean add_fk_access) {

				try {

					String project_root = EclipseHelpers.get_absolute_dir_path_str(editor2.get_project());

					ObjectFactory object_factory = new ObjectFactory();

					DaoClass root = object_factory.createDaoClass();

					Connection conn = EclipseHelpers.get_connection(editor2);

					try {

						// !!!! after 'try'
						Set<String> used;

						if (skip_used) {

							used = find_tables_used_in_dao_xml(editor2, project_root);

						} else {

							used = new HashSet<String>();
						}

						List<Object> nodes = root.getCrudOrCrudAutoOrQuery();

						DatabaseMetaData db_info = conn.getMetaData();

						ResultSet rs = DbUtils.get_tables(conn, db_info, selected_schema, include_views);

						try {

							while (rs.next()) {

								// http://docs.oracle.com/javase/1.4.2/docs/api/java/sql/DatabaseMetaData.html

								try {

									String table_type = rs.getString("TABLE_TYPE");

									if (!("TABLE".equalsIgnoreCase(table_type)
											|| "VIEW".equalsIgnoreCase(table_type))) {

										continue;
									}

								} catch (Throwable e) {

								}

								String table_name = rs.getString("TABLE_NAME");

								if (used.contains(table_name) == false) {

									String dto_class_name = EclipseHelpers.table_name_to_dto_class_name(table_name,
											plural_to_singular);

									if (use_crud_auto) {

										CrudAuto ca = new CrudAuto();
										ca.setTable(table_name);
										ca.setDto(dto_class_name);
										nodes.add(ca);

									} else {

										Crud crud = object_factory.createCrud();
										crud.setDto(dto_class_name);
										crud.setTable(table_name);

										{
											TypeMethod tm = new TypeMethod();
											String m = "create" + dto_class_name;
											if (EclipseTargetLanguageHelpers.underscores_needed(editor2)) {
												m = Helpers.camel_case_to_lower_under_scores(m);
											}
											tm.setMethod(m);
											crud.setCreate(tm);
										}
										{
											TypeMethod tm = new TypeMethod();
											String m = "read" + dto_class_name + "List";
											if (EclipseTargetLanguageHelpers.underscores_needed(editor2)) {
												m = Helpers.camel_case_to_lower_under_scores(m);
											}
											tm.setMethod(m);
											crud.setReadAll(tm);
										}
										{
											TypeMethod tm = new TypeMethod();
											String m = "read" + dto_class_name;
											if (EclipseTargetLanguageHelpers.underscores_needed(editor2)) {
												m = Helpers.camel_case_to_lower_under_scores(m);
											}
											tm.setMethod(m);
											crud.setRead(tm);
										}
										{
											TypeMethod tm = new TypeMethod();
											String m = "update" + dto_class_name;
											if (EclipseTargetLanguageHelpers.underscores_needed(editor2)) {
												m = Helpers.camel_case_to_lower_under_scores(m);
											}
											tm.setMethod(m);
											crud.setUpdate(tm);
										}
										{
											TypeMethod tm = new TypeMethod();
											String m = "delete" + dto_class_name;
											if (EclipseTargetLanguageHelpers.underscores_needed(editor2)) {
												m = Helpers.camel_case_to_lower_under_scores(m);
											}
											tm.setMethod(m);
											crud.setDelete(tm);
										}

										nodes.add(crud);
									}

									if (add_fk_access) {

										add_fk_access(editor2, conn, selected_schema, db_info, rs, nodes,
												plural_to_singular);
									}
								}
							}

						} finally {

							rs.close();
						}

					} finally {

						conn.close();
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

					ObjectFactory object_factory = new ObjectFactory();

					DaoClass root = object_factory.createDaoClass();

					List<Object> nodes = root.getCrudOrCrudAutoOrQuery();

					Connection conn = EclipseHelpers.get_connection(editor2);

					try {

						DatabaseMetaData db_info = conn.getMetaData();

						ResultSet rs = db_info.getTables(conn.getCatalog(), selected_schema, "%", null);

						try {

							while (rs.next()) {

								add_fk_access(editor2, conn, selected_schema, db_info, rs, nodes, plural_to_singular);
							}

						} finally {

							rs.close();
						}

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