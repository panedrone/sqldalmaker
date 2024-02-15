/*
	Copyright 2011-2024 sqldalmaker@gmail.com
	Read LICENSE.txt in the root of this project/archive.
	Project web-site: https://sqldalmaker.sourceforge.net/
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
import com.sqldalmaker.common.TargetLangUtils;
import com.sqldalmaker.common.XmlParser;
import com.sqldalmaker.jaxb.sdm.DaoClass;
import com.sqldalmaker.jaxb.sdm.DtoClass;
import com.sqldalmaker.jaxb.settings.Settings;

/**
 *
 * @author sqldalmaker@gmail.com
 *
 */
public class EclipseCG {

	private static class Error {
		public boolean happend = false;
	}

	public static void generate_all_sdm_dto(IFile root_file, IFile xml_file) {
		StringBuilder output_dir = new StringBuilder();
		Error error = new Error();
		try {
			String xml_mp_abs_path = root_file.getParent().getLocation().toPortableString();
			Settings settings = SdmUtils.load_settings(xml_mp_abs_path);
			IProject project = root_file.getProject();
			Connection con = EclipseHelpers.get_connection(project, settings);
			try {
				IDtoCG gen = EclipseTargetLanguageHelpers.create_dto_cg(con, project, settings, root_file.getName(),
						xml_mp_abs_path, output_dir);
				String sdm_xml_abs_path = xml_file.getLocation().toPortableString();
				String sdm_xsd_abs_path = Helpers.concat_path(xml_mp_abs_path, Const.SDM_XSD);
				List<DtoClass> dto_classes = SdmUtils.get_dto_classes(sdm_xml_abs_path, sdm_xsd_abs_path);
				for (DtoClass cls : dto_classes) {
					try {
						String[] file_content = gen.translate(cls.getName());
						String target_file_path = TargetLangUtils.get_target_file_path(root_file.getName(),
								output_dir.toString(), cls.getName());
						EclipseHelpers.save_text_to_file(target_file_path, file_content[0]);
					} catch (Throwable e) {
						String msg = e.getMessage();
						EclipseConsoleHelpers.add_error_msg(cls.getName() + ": " + msg);
						error.happend = true;
					}
				}
				if (!error.happend && !dto_classes.isEmpty()) {
					EclipseHelpers.refresh_project(xml_file.getProject());
					String current_xml_file_path = xml_file.getFullPath().toPortableString();
					if (current_xml_file_path.startsWith("/")) {
						current_xml_file_path = current_xml_file_path.substring(1);
					}
					EclipseConsoleHelpers.add_info_msg(current_xml_file_path + " -> " + Const.GENERATE_SDM_DTO_MODELS);
				}
			} finally {
				con.close();
			}
		} catch (Exception e) {
			EclipseConsoleHelpers.add_error_msg(e.getMessage());
			e.printStackTrace();
		}
	}

	public static void generate_all_sdm_dao(IFile root_file, IFile xml_file) {
		StringBuilder output_dir = new StringBuilder();
		Error error = new Error();
		try {
			String xml_mp_abs_path = root_file.getParent().getLocation().toPortableString();
			Settings settings = SdmUtils.load_settings(xml_mp_abs_path);
			IProject project = root_file.getProject();
			Connection con = EclipseHelpers.get_connection(project, settings);
			try {
				IDaoCG gen = EclipseTargetLanguageHelpers.create_dao_cg(con, project, root_file.getName(), settings,
						xml_mp_abs_path, output_dir);
				String sdm_xml_abs_path = xml_file.getLocation().toPortableString();
				String sdm_xsd_abs_path = Helpers.concat_path(xml_mp_abs_path, Const.SDM_XSD);
				List<DaoClass> dao_classes = SdmUtils.get_dao_classes(sdm_xml_abs_path, sdm_xsd_abs_path);
				String contextPath = DaoClass.class.getPackage().getName();
				XmlParser dao_xml_parser = new XmlParser(contextPath, sdm_xsd_abs_path);
				for (DaoClass cls : dao_classes) {
					try {
						String[] file_content = generate_single_sdm_dao(gen, dao_xml_parser, cls, sdm_xml_abs_path);
						String target_file_path = TargetLangUtils.get_target_file_path(root_file.getName(),
								output_dir.toString(), cls.getName());
						EclipseHelpers.save_text_to_file(target_file_path, file_content[0]);
					} catch (Throwable e) {
						String msg = e.getMessage();
						EclipseConsoleHelpers.add_error_msg(cls.getName() + ": " + msg);
						error.happend = true;
					}
				}
				if (!error.happend && !dao_classes.isEmpty()) {
					EclipseHelpers.refresh_project(xml_file.getProject());
					String current_xml_file_path = xml_file.getFullPath().toPortableString();
					if (current_xml_file_path.startsWith("/")) {
						current_xml_file_path = current_xml_file_path.substring(1);
					}
					EclipseConsoleHelpers.add_info_msg(current_xml_file_path + " -> " + Const.GENERATE_SDM_DAO);
				}
			} finally {
				con.close();
			}
		} catch (Exception e) {
			EclipseConsoleHelpers.add_error_msg(e.getMessage());
			e.printStackTrace();
		}
	}

	public static void validate_all_sdm_dto(IFile root_file, IFile xml_file) {
		StringBuilder output_dir = new StringBuilder();
		Error error = new Error();
		try {
			String xml_mp_abs_path = root_file.getParent().getLocation().toPortableString();
			Settings settings = SdmUtils.load_settings(xml_mp_abs_path);
			IProject project = root_file.getProject();
			Connection con = EclipseHelpers.get_connection(project, settings);
			String sdm_xml_abs_path = xml_file.getLocation().toPortableString();
			String sdm_xsd_abs_path = Helpers.concat_path(xml_mp_abs_path, Const.SDM_XSD);
			List<DtoClass> dto_classes = SdmUtils.get_dto_classes(sdm_xml_abs_path, sdm_xsd_abs_path);
			try {
				IDtoCG gen = EclipseTargetLanguageHelpers.create_dto_cg(con, project, settings, root_file.getName(),
						xml_mp_abs_path, output_dir);
				for (DtoClass cls : dto_classes) {
					try {
						String[] file_content = gen.translate(cls.getName());
						String target_file_path = TargetLangUtils.get_target_file_path(root_file.getName(),
								output_dir.toString(), cls.getName());
						String old_text = Helpers.load_text_from_file(target_file_path);
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
			if (!error.happend && !dto_classes.isEmpty()) {
				EclipseHelpers.refresh_project(xml_file.getProject());
				String current_xml_file_path = xml_file.getFullPath().toPortableString();
				if (current_xml_file_path.startsWith("/")) {
					current_xml_file_path = current_xml_file_path.substring(1);
				}
				EclipseConsoleHelpers.add_info_msg(current_xml_file_path + " -> " + Const.VALIDATE_SDM_DTO_MODELS);
			}
		} catch (Exception e) {
			EclipseConsoleHelpers.add_error_msg(e.getMessage());
			e.printStackTrace();
		}
	}

	public static void validate_all_sdm_dao(IFile root_file, IFile xml_file) {
		StringBuilder output_dir = new StringBuilder();
		Error error = new Error();
		try {
			String sdm_folder_abs_path = root_file.getParent().getLocation().toPortableString();
			Settings settings = SdmUtils.load_settings(sdm_folder_abs_path);
			IProject project = root_file.getProject();
			Connection con = EclipseHelpers.get_connection(project, settings);
			String sdm_xml_abs_path = xml_file.getLocation().toPortableString();
			String sdm_xsd_abs_path = Helpers.concat_path(sdm_folder_abs_path, Const.SDM_XSD);
			List<DaoClass> dao_classes = SdmUtils.get_dao_classes(sdm_xml_abs_path, sdm_xsd_abs_path);
			String contextPath = DaoClass.class.getPackage().getName();
			XmlParser dao_xml_parser = new XmlParser(contextPath, sdm_xsd_abs_path);
			try {
				IDaoCG gen = EclipseTargetLanguageHelpers.create_dao_cg(con, project, root_file.getName(), settings,
						sdm_folder_abs_path, output_dir);
				for (DaoClass cls : dao_classes) {
					try {
						String[] file_content = generate_single_sdm_dao(gen, dao_xml_parser, cls, sdm_xml_abs_path);
						String target_file_path = TargetLangUtils.get_target_file_path(root_file.getName(),
								output_dir.toString(), cls.getName());
						String old_text = Helpers.load_text_from_file(target_file_path);
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
			if (!error.happend && !dao_classes.isEmpty()) {
				EclipseHelpers.refresh_project(xml_file.getProject());
				String current_xml_file_path = xml_file.getFullPath().toPortableString();
				if (current_xml_file_path.startsWith("/")) {
					current_xml_file_path = current_xml_file_path.substring(1);
				}
				EclipseConsoleHelpers.add_info_msg(current_xml_file_path + " -> " + Const.VALIDATE_SDM_DAO);
			}
		} catch (Exception e) {
			EclipseConsoleHelpers.add_error_msg(e.getMessage());
			e.printStackTrace();
		}
	}

	public static void generate_dao(IFile root_file, IFile xml_file) {
		Error error = new Error();
		try {
			String xml_mp_abs_path = root_file.getParent().getLocation().toPortableString();
			Settings settings = SdmUtils.load_settings(xml_mp_abs_path);
			IProject project = root_file.getProject();
			Connection conn = EclipseHelpers.get_connection(project, settings);
			try {
				StringBuilder output_dir_abs_path = new StringBuilder();
				IDaoCG gen = EclipseTargetLanguageHelpers.create_dao_cg(conn, project, root_file.getName(), settings,
						xml_mp_abs_path, output_dir_abs_path);
				String dao_xsd_abs_path = Helpers.concat_path(xml_mp_abs_path, Const.DAO_XSD);
				String context_path = DaoClass.class.getPackage().getName();
				XmlParser dao_xml_parser = new XmlParser(context_path, dao_xsd_abs_path);
				String dao_xml_abs_path = xml_file.getLocation().toPortableString();
				String dao_class_name = Helpers.get_dao_class_name(xml_file.getName());
				DaoClass dao_class = dao_xml_parser.unmarshal(dao_xml_abs_path);
				dao_class.setName(dao_class_name);
				try {
					String[] file_content = generate_single_sdm_dao(gen, dao_xml_parser, dao_class, dao_xml_abs_path);
					String target_file_path = TargetLangUtils.get_target_file_path(root_file.getName(),
							output_dir_abs_path.toString(), dao_class_name);
					EclipseHelpers.save_text_to_file(target_file_path, file_content[0]);
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
				EclipseConsoleHelpers.add_info_msg(current_xml_file_path + " -> " + Const.GENERATE_DAO_XML);
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
		Error error = new Error();
		try {
			String xml_mp_abs_path = root_file.getParent().getLocation().toPortableString();
			Settings settings = SdmUtils.load_settings(xml_mp_abs_path);
			IProject project = root_file.getProject();
			Connection conn = EclipseHelpers.get_connection(project, settings);
			try {
				StringBuilder output_dir_abs_path = new StringBuilder();
				IDaoCG gen = EclipseTargetLanguageHelpers.create_dao_cg(conn, project, root_file.getName(), settings,
						xml_mp_abs_path, output_dir_abs_path);
				String dao_xsd_abs_path = Helpers.concat_path(xml_mp_abs_path, Const.DAO_XSD);
				String context_path = DaoClass.class.getPackage().getName();
				XmlParser dao_xml_parser = new XmlParser(context_path, dao_xsd_abs_path);
				String dao_xml_abs_path = xml_file.getLocation().toPortableString();
				String dao_class_name = Helpers.get_dao_class_name(xml_file.getName());
				DaoClass dao_class = dao_xml_parser.unmarshal(dao_xml_abs_path);
				dao_class.setName(dao_class_name);
				try {
					String[] file_content = generate_single_sdm_dao(gen, dao_xml_parser, dao_class, dao_xml_abs_path);
					String target_file_path = TargetLangUtils.get_target_file_path(root_file.getName(),
							output_dir_abs_path.toString(), dao_class_name);
					StringBuilder validation_buff = new StringBuilder();
					String oldText = Helpers.load_text_from_file(target_file_path);
					if (oldText == null) {
						validation_buff.append(Const.OUTPUT_FILE_IS_MISSING);
					} else {
						String text = file_content[0];
						if (!oldText.equals(text)) {
							validation_buff.append(Const.OUTPUT_FILE_IS_OUT_OF_DATE);
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
				EclipseConsoleHelpers.add_info_msg(current_xml_file_path + " -> " + Const.VALIDATE_DAO_XML);
			}
		} catch (Exception e) {
			EclipseConsoleHelpers.add_error_msg(e.getMessage());
			e.printStackTrace();
		}
	}

	private static DaoClass load_external_dao_class(XmlParser dao_xml_parser, String dao_xml_abs_path)
			throws Exception {
		DaoClass dao_class = dao_xml_parser.unmarshal(dao_xml_abs_path);
		return dao_class;
	}

	public static String[] generate_single_sdm_dao(IDaoCG gen, XmlParser dao_xml_parser, DaoClass sdm_dao_class,
			String dao_xml_abs_path) throws Exception {

		String dao_class_name = sdm_dao_class.getName();
		String[] file_content;
		String ref = sdm_dao_class.getRef();
		if (ref == null || ref.trim().isEmpty()) { // nullable
			file_content = gen.translate(dao_class_name, sdm_dao_class);
		} else {
			DaoClass external_dao_class = load_external_dao_class(dao_xml_parser, dao_xml_abs_path);
			file_content = gen.translate(dao_class_name, external_dao_class);
		}
		return file_content;
	}
}
