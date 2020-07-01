/*
 * Copyright 2011-2020 sqldalmaker@gmail.com
 * SQL DAL Maker Website: http://sqldalmaker.sourceforge.net
 * Read LICENSE.txt in the root of this project/archive for details.
 */
package com.sqldalmaker.eclipse;

import java.sql.Connection;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;

import com.sqldalmaker.cg.Helpers;
import com.sqldalmaker.cg.IDaoCG;
import com.sqldalmaker.cg.IDtoCG;
import com.sqldalmaker.common.Const;
import com.sqldalmaker.common.SdmUtils;
import com.sqldalmaker.common.XmlParser;
import com.sqldalmaker.jaxb.dao.DaoClass;
import com.sqldalmaker.jaxb.dto.DtoClass;
import com.sqldalmaker.jaxb.settings.Settings;

/**
 *
 * @author sqldalmaker@gmail.com
 *
 */
public class EclipseCG {

	static class Error {
		public boolean happend = false;
	}

	public static void generate_all_dto(IFile root_file, IFile xml_file) {
		StringBuilder output_dir = new StringBuilder();
		Error error = new Error();
		try {
			String xml_mp_abs_path = root_file.getParent().getLocation().toPortableString();
			Settings settings = SdmUtils.load_settings(xml_mp_abs_path);
			IProject project = root_file.getProject();
			Connection con = EclipseHelpers.get_connection(project, settings);
			try {
				String dto_xml_abs_path = xml_file.getLocation().toPortableString();
				String dto_xsd_abs_path = Helpers.concat_path(xml_mp_abs_path, Const.DTO_XSD);
				IDtoCG gen = EclipseTargetLanguageHelpers.create_dto_cg(con, project, settings, root_file.getName(),
						dto_xml_abs_path, dto_xsd_abs_path, output_dir);
				List<DtoClass> dto_classes = SdmUtils.get_dto_classes(dto_xml_abs_path, dto_xsd_abs_path);
				for (DtoClass cls : dto_classes) {
					try {
						// ProgressManager.progress(cls.getName());
						String[] file_content = gen.translate(cls.getName());
						String file_name = EclipseTargetLanguageHelpers.get_rel_path(root_file.getName(), output_dir,
								cls.getName());
						EclipseHelpers.save_text_to_file(file_name, file_content[0]);
					} catch (Throwable e) {
						String msg = e.getMessage();
						EclipseConsoleHelpers.add_error_msg(cls.getName() + ": " + msg);
						error.happend = true;
					}
				}
			} finally {
				con.close();
			}
			if (!error.happend) {
				EclipseHelpers.refresh_project(xml_file.getProject());
				String current_xml_file_path = xml_file.getFullPath().toPortableString();
				if (current_xml_file_path.startsWith("/")) {
					current_xml_file_path = current_xml_file_path.substring(1);
				}
				EclipseConsoleHelpers.add_info_msg(current_xml_file_path + " -> Generated successfully");
			}
		} catch (Exception e) {
			EclipseConsoleHelpers.add_error_msg(e.getMessage());
			e.printStackTrace();
		}
	}

	public static void validate_all_dto(IFile root_file, IFile xml_file) {
		StringBuilder output_dir = new StringBuilder();
		Error error = new Error();
		try {
			String xml_mp_abs_path = root_file.getParent().getLocation().toPortableString();
			Settings settings = SdmUtils.load_settings(xml_mp_abs_path);
			IProject project = root_file.getProject();
			Connection con = EclipseHelpers.get_connection(project, settings);
			try {
				String dto_xml_abs_path = xml_file.getLocation().toPortableString();
				String dto_xsd_abs_path = Helpers.concat_path(xml_mp_abs_path, Const.DTO_XSD);
				IDtoCG gen = EclipseTargetLanguageHelpers.create_dto_cg(con, project, settings, root_file.getName(),
						dto_xml_abs_path, dto_xsd_abs_path, output_dir);
				List<DtoClass> dto_classes = SdmUtils.get_dto_classes(dto_xml_abs_path, dto_xsd_abs_path);
				for (DtoClass cls : dto_classes) {
					try {
						// ProgressManager.progress(cls.getName());
						String[] file_content = gen.translate(cls.getName());
						String file_name = EclipseTargetLanguageHelpers.get_rel_path(root_file.getName(), output_dir,
								cls.getName());
						String old_text = Helpers.load_text_from_file(file_name);
						StringBuilder validation_buff = new StringBuilder();
						if (old_text == null) {
							validation_buff.append("Generated file is missing");
						} else {
							String text = file_content[0];
							if (!old_text.equals(text)) {
								validation_buff.append("Generated file is out of date");
							}
						}
						String status = validation_buff.toString();
						if (status.length() > 0) {
							error.happend = true;
							EclipseConsoleHelpers.add_error_msg(cls.getName() + ": " + status);
						}
					} catch (Throwable e) {
						String msg = e.getMessage();
						EclipseConsoleHelpers.add_error_msg(cls.getName() + ": " + msg);
						error.happend = true;
					}
				}
			} finally {
				con.close();
			}
			if (!error.happend) {
				EclipseHelpers.refresh_project(xml_file.getProject());
				String current_xml_file_path = xml_file.getFullPath().toPortableString();
				if (current_xml_file_path.startsWith("/")) {
					current_xml_file_path = current_xml_file_path.substring(1);
				}
				EclipseConsoleHelpers.add_info_msg(current_xml_file_path + " -> OK");
			}
		} catch (Exception e) {
			EclipseConsoleHelpers.add_error_msg(e.getMessage());
			e.printStackTrace();
		}
	}

	public static void generate_dao(IFile root_file, IFile xml_file) {
		StringBuilder output_dir = new StringBuilder();
		Error error = new Error();
		try {
			String xml_mp_abs_path = root_file.getParent().getLocation().toPortableString();
			Settings settings = SdmUtils.load_settings(xml_mp_abs_path);
			IProject project = root_file.getProject();
			Connection conn = EclipseHelpers.get_connection(project, settings);
			try {
				String dto_xml_abs_path = Helpers.concat_path(xml_mp_abs_path, Const.DTO_XML);
				String dto_xsd_abs_path = Helpers.concat_path(xml_mp_abs_path, Const.DTO_XSD);
				IDaoCG gen = EclipseTargetLanguageHelpers.create_dao_cg(conn, project, root_file.getName(), settings,
						dto_xml_abs_path, dto_xsd_abs_path, output_dir);
				String dao_xsd_abs_path = Helpers.concat_path(xml_mp_abs_path, Const.DAO_XSD);
				String context_path = DaoClass.class.getPackage().getName();
				XmlParser dao_xml_parser = new XmlParser(context_path, dao_xsd_abs_path);
				String dao_xml_abs_path = xml_file.getLocation().toPortableString();
				String dao_class_name = Helpers.get_dao_class_name(xml_file.getName());
				DaoClass dao_class = dao_xml_parser.unmarshal(dao_xml_abs_path);
				try {
					String[] file_content = gen.translate(dao_class_name, dao_class);
					String file_name = EclipseTargetLanguageHelpers.get_rel_path(root_file.getName(), output_dir,
							dao_class_name);
					EclipseHelpers.save_text_to_file(file_name, file_content[0]);
				} catch (Throwable e) {
					String msg = e.getMessage();
					EclipseConsoleHelpers.add_error_msg(dao_class_name + ": " + msg);
					error.happend = true;
				}
			} finally {
				conn.close();
			}
			if (!error.happend) {
				EclipseHelpers.refresh_project(xml_file.getProject());
				String current_xml_file_path = xml_file.getFullPath().toPortableString();
				if (current_xml_file_path.startsWith("/")) {
					current_xml_file_path = current_xml_file_path.substring(1);
				}
				EclipseConsoleHelpers.add_info_msg(current_xml_file_path + " -> Generated successfully");
			}
		} catch (Exception e) {
			EclipseConsoleHelpers.add_error_msg(e.getMessage());
			e.printStackTrace();
		}
	}

	public static void validate_dao(IFile root_file, IFile xml_file) {
		String current_xml_file_path = xml_file.getFullPath().toPortableString();
		if (current_xml_file_path.startsWith("/")) {
			current_xml_file_path = current_xml_file_path.substring(1);
		}
		StringBuilder output_dir = new StringBuilder();
		Error error = new Error();
		try {
			String xml_mp_abs_path = root_file.getParent().getLocation().toPortableString();
			Settings settings = SdmUtils.load_settings(xml_mp_abs_path);
			IProject project = root_file.getProject();
			Connection conn = EclipseHelpers.get_connection(project, settings);
			try {
				String dto_xml_abs_path = Helpers.concat_path(xml_mp_abs_path, Const.DTO_XML);
				String dto_xsd_abs_path = Helpers.concat_path(xml_mp_abs_path, Const.DTO_XSD);
				IDaoCG gen = EclipseTargetLanguageHelpers.create_dao_cg(conn, project, root_file.getName(), settings,
						dto_xml_abs_path, dto_xsd_abs_path, output_dir);
				String dao_xsd_abs_path = Helpers.concat_path(xml_mp_abs_path, Const.DAO_XSD);
				String context_path = DaoClass.class.getPackage().getName();
				XmlParser dao_xml_parser = new XmlParser(context_path, dao_xsd_abs_path);
				String dao_xml_abs_path = xml_file.getLocation().toPortableString();
				String dao_class_name = Helpers.get_dao_class_name(xml_file.getName());
				DaoClass dao_class = dao_xml_parser.unmarshal(dao_xml_abs_path);
				try {
					String[] file_content = gen.translate(dao_class_name, dao_class);
					String file_name = EclipseTargetLanguageHelpers.get_rel_path(root_file.getName(), output_dir,
							dao_class_name);
					StringBuilder validation_buff = new StringBuilder();
					String oldText = Helpers.load_text_from_file(file_name);
					if (oldText == null) {
						validation_buff.append("Generated file is missing");
					} else {
						String text = file_content[0];
						if (!oldText.equals(text)) {
							validation_buff.append("Generated file is out of date");
						}
					}
					String status = validation_buff.toString();
					if (status.length() > 0) {
						EclipseConsoleHelpers.add_error_msg(current_xml_file_path + ": " + status);
						error.happend = true;
					}
				} catch (Throwable e) {
					String msg = e.getMessage();
					EclipseConsoleHelpers.add_error_msg(dao_class_name + ": " + msg);
					error.happend = true;
				}
			} finally {
				conn.close();
			}
			if (!error.happend) {
				EclipseHelpers.refresh_project(xml_file.getProject());
				EclipseConsoleHelpers.add_info_msg(current_xml_file_path + " -> OK");
			}
		} catch (Exception e) {
			EclipseConsoleHelpers.add_error_msg(e.getMessage());
			e.printStackTrace();
		}
	}
}
