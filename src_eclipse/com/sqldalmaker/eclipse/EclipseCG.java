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
import com.sqldalmaker.cg.JaxbUtils;
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

	/// DTO ////////////////////////////////////////

	public static void generate_all_sdm_dto(IFile root_file, IFile xml_file) {
		try {
			boolean err_happened = false;
			String sdm_folder_abs_path = root_file.getParent().getLocation().toPortableString();
			Settings settings = SdmUtils.load_settings(sdm_folder_abs_path);
			IProject project = root_file.getProject();
			Connection con = EclipseHelpers.get_connection(project, settings);
			try {
				StringBuilder output_dir = new StringBuilder();
				IDtoCG gen = create_dto_cg(con, project, settings, root_file.getName(), sdm_folder_abs_path,
						output_dir);
				String sdm_xml_abs_path = xml_file.getLocation().toPortableString();
				String sdm_xsd_abs_path = Helpers.concat_path(sdm_folder_abs_path, Const.SDM_XSD);
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
						err_happened = true;
					}
				}
				if (!err_happened && !dto_classes.isEmpty()) {
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

	public static void validate_all_sdm_dto(IFile root_file, IFile xml_file) {
		try {
			String sdm_folder_abs_path = root_file.getParent().getLocation().toPortableString();
			Settings settings = SdmUtils.load_settings(sdm_folder_abs_path);
			IProject project = root_file.getProject();
			Connection con = EclipseHelpers.get_connection(project, settings);
			String sdm_xml_abs_path = xml_file.getLocation().toPortableString();
			String sdm_xsd_abs_path = Helpers.concat_path(sdm_folder_abs_path, Const.SDM_XSD);
			List<DtoClass> dto_classes = SdmUtils.get_dto_classes(sdm_xml_abs_path, sdm_xsd_abs_path);
			boolean err_happened = false;
			try {
				StringBuilder output_dir = new StringBuilder();
				IDtoCG gen = create_dto_cg(con, project, settings, root_file.getName(), sdm_folder_abs_path,
						output_dir);
				for (DtoClass cls : dto_classes) {
					try {
						String[] file_content = gen.translate(cls.getName());
						String target_file_path = TargetLangUtils.get_target_file_path(root_file.getName(),
								output_dir.toString(), cls.getName());
						String old_text = Helpers.load_text_from_file(target_file_path);
						StringBuilder validation_buff = new StringBuilder();
						if (old_text == null) {
							validation_buff.append(Const.OUTPUT_FILE_IS_MISSING);
						} else {
							String text = file_content[0];
							if (!old_text.equals(text)) {
								validation_buff.append(Const.OUTPUT_FILE_IS_OUT_OF_DATE);
							}
						}
						String status = validation_buff.toString();
						if (status.length() > 0) {
							err_happened = true;
							EclipseConsoleHelpers.add_error_msg(cls.getName() + ": " + status);
						}
					} catch (Throwable e) {
						String msg = e.getMessage();
						EclipseConsoleHelpers.add_error_msg(cls.getName() + ": " + msg);
						err_happened = true;
					}
				}
			} finally {
				con.close();
			}
			if (!err_happened && !dto_classes.isEmpty()) {
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

	/// DAO ////////////////////////////////////////

	private static XmlParser get_external_dao_xml_parser(String sdm_folder_abs_path) throws Exception {
		String sdm_xsd_abs_path = Helpers.concat_path(sdm_folder_abs_path, Const.DAO_XSD);
		String contextPath = DaoClass.class.getPackage().getName();
		return new XmlParser(contextPath, sdm_xsd_abs_path);
	}

	public static void generate_all_sdm_dao(IFile root_file, IFile xml_file) {
		try {
			String sdm_folder_abs_path = root_file.getParent().getLocation().toPortableString();
			Settings settings = SdmUtils.load_settings(sdm_folder_abs_path);
			IProject project = root_file.getProject();
			Connection con = EclipseHelpers.get_connection(project, settings);
			try {
				StringBuilder output_dir = new StringBuilder();
				IDaoCG gen = create_dao_cg(con, project, root_file.getName(), settings, sdm_folder_abs_path,
						output_dir);
				List<DaoClass> jaxb_dao_classes = EclipseHelpers.load_all_sdm_dao_classes(root_file);
				XmlParser external_dao_xml_parser = get_external_dao_xml_parser(sdm_folder_abs_path);
				boolean err_happened = false;
				for (DaoClass sdm_dao_class : jaxb_dao_classes) {
					try {
						String[] file_content = generate_single_sdm_dao(gen, external_dao_xml_parser, sdm_dao_class,
								sdm_folder_abs_path);
						String target_file_path = TargetLangUtils.get_target_file_path(root_file.getName(),
								output_dir.toString(), sdm_dao_class.getName());
						EclipseHelpers.save_text_to_file(target_file_path, file_content[0]);
					} catch (Throwable e) {
						String msg = e.getMessage();
						EclipseConsoleHelpers.add_error_msg(sdm_dao_class.getName() + ": " + msg);
						err_happened = true;
					}
				}
				if (!err_happened && !jaxb_dao_classes.isEmpty()) {
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

	public static void validate_all_sdm_dao(IFile root_file, IFile xml_file) {
		try {
			String sdm_folder_abs_path = root_file.getParent().getLocation().toPortableString();
			Settings settings = SdmUtils.load_settings(sdm_folder_abs_path);
			IProject project = root_file.getProject();
			Connection con = EclipseHelpers.get_connection(project, settings);
			List<DaoClass> jaxb_dao_classes = EclipseHelpers.load_all_sdm_dao_classes(root_file);
			XmlParser external_dao_xml_parser = get_external_dao_xml_parser(sdm_folder_abs_path);
			boolean err_happened = false;
			try {
				StringBuilder output_dir = new StringBuilder();
				IDaoCG gen = create_dao_cg(con, project, root_file.getName(), settings, sdm_folder_abs_path,
						output_dir);
				for (DaoClass sdm_dao_class : jaxb_dao_classes) {
					try {
						String[] file_content = generate_single_sdm_dao(gen, external_dao_xml_parser, sdm_dao_class,
								sdm_folder_abs_path);
						String target_file_path = TargetLangUtils.get_target_file_path(root_file.getName(),
								output_dir.toString(), sdm_dao_class.getName());
						String old_text = Helpers.load_text_from_file(target_file_path);
						StringBuilder validation_buff = new StringBuilder();
						if (old_text == null) {
							validation_buff.append(Const.OUTPUT_FILE_IS_MISSING);
						} else {
							String text = file_content[0];
							if (!old_text.equals(text)) {
								validation_buff.append(Const.OUTPUT_FILE_IS_OUT_OF_DATE);
							}
						}
						String status = validation_buff.toString();
						if (status.length() > 0) {
							err_happened = true;
							EclipseConsoleHelpers.add_error_msg(sdm_dao_class.getName() + ": " + status);
						}
					} catch (Throwable e) {
						String msg = e.getMessage();
						EclipseConsoleHelpers.add_error_msg(sdm_dao_class.getName() + ": " + msg);
						err_happened = true;
					}
				}
			} finally {
				con.close();
			}
			if (!err_happened && !jaxb_dao_classes.isEmpty()) {
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

	public static void toolbar_action_generate_external_dao(IFile root_file, IFile external_dao_xml_file) {
		String current_xml_file_path = external_dao_xml_file.getFullPath().toPortableString();
		if (current_xml_file_path.startsWith("/")) {
			current_xml_file_path = current_xml_file_path.substring(1);
		}
		try {
			String sdm_folder_abs_path = root_file.getParent().getLocation().toPortableString();
			Settings settings = SdmUtils.load_settings(sdm_folder_abs_path);
			IProject project = root_file.getProject();
			Connection conn = EclipseHelpers.get_connection(project, settings);
			boolean err_happened = false;
			try {
				StringBuilder output_dir_abs_path = new StringBuilder();
				IDaoCG gen = create_dao_cg(conn, project, root_file.getName(), settings, sdm_folder_abs_path,
						output_dir_abs_path);
				XmlParser external_dao_xml_parser = get_external_dao_xml_parser(sdm_folder_abs_path);
				String external_dao_xml_abs_path = external_dao_xml_file.getLocation().toPortableString();
				List<DaoClass> jaxb_dao_classes = EclipseHelpers.load_all_sdm_dao_classes(root_file);
				String sdm_dao_class_name = JaxbUtils.get_dao_class_name_by_dao_xml_path(jaxb_dao_classes,
						sdm_folder_abs_path);
				try {
					String[] file_content = generate_single_external_dao(gen, external_dao_xml_parser,
							sdm_dao_class_name, external_dao_xml_abs_path);
					String target_file_path = TargetLangUtils.get_target_file_path(root_file.getName(),
							output_dir_abs_path.toString(), sdm_dao_class_name);
					EclipseHelpers.save_text_to_file(target_file_path, file_content[0]);
				} catch (Throwable e) {
					String msg = e.getMessage();
					EclipseConsoleHelpers.add_error_msg(sdm_dao_class_name + ": " + msg);
					err_happened = true;
				}
			} finally {
				conn.close();
			}
			if (!err_happened) {
				EclipseHelpers.refresh_project(external_dao_xml_file.getProject());
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

	public static void toolbar_action_validate_external_dao(IFile root_file, IFile external_dao_xml_file) {
		String current_xml_file_path = external_dao_xml_file.getFullPath().toPortableString();
		if (current_xml_file_path.startsWith("/")) {
			current_xml_file_path = current_xml_file_path.substring(1);
		}
		try {
			String sdm_folder_abs_path = root_file.getParent().getLocation().toPortableString();
			Settings settings = SdmUtils.load_settings(sdm_folder_abs_path);
			IProject project = root_file.getProject();
			Connection conn = EclipseHelpers.get_connection(project, settings);
			boolean err_happened = false;
			try {
				StringBuilder output_dir_abs_path = new StringBuilder();
				IDaoCG gen = create_dao_cg(conn, project, root_file.getName(), settings, sdm_folder_abs_path,
						output_dir_abs_path);
				XmlParser external_dao_xml_parser = get_external_dao_xml_parser(sdm_folder_abs_path);
				List<DaoClass> jaxb_dao_classes = EclipseHelpers.load_all_sdm_dao_classes(root_file);
				String external_dao_xml_abs_path = external_dao_xml_file.getLocation().toPortableString();
				String sdm_dao_class_name = JaxbUtils.get_dao_class_name_by_dao_xml_path(jaxb_dao_classes,
						external_dao_xml_abs_path);
				try {
					String[] file_content = generate_single_external_dao(gen, external_dao_xml_parser,
							sdm_dao_class_name, external_dao_xml_abs_path);
					String target_file_path = TargetLangUtils.get_target_file_path(root_file.getName(),
							output_dir_abs_path.toString(), sdm_dao_class_name);
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
						err_happened = true;
					}
				} catch (Throwable e) {
					String msg = e.getMessage();
					EclipseConsoleHelpers.add_error_msg(sdm_dao_class_name + ": " + msg);
					err_happened = true;
				}
			} finally {
				conn.close();
			}
			if (!err_happened) {
				EclipseHelpers.refresh_project(external_dao_xml_file.getProject());
				EclipseConsoleHelpers.add_info_msg(current_xml_file_path + " -> " + Const.VALIDATE_DAO_XML);
			}
		} catch (Exception e) {
			EclipseConsoleHelpers.add_error_msg(e.getMessage());
			e.printStackTrace();
		}
	}

	public static String[] generate_single_sdm_dao(IDaoCG gen, XmlParser external_dao_xml_parser,
			DaoClass sdm_dao_class, String sdm_folder_abs_path) throws Exception {

		String dao_class_name = sdm_dao_class.getName();
		String ref = sdm_dao_class.getRef();
		if (ref == null || ref.trim().isEmpty()) { // nullable
			return gen.translate(sdm_dao_class);
		}
		String external_dao_xml_abs_path = Helpers.concat_path(sdm_folder_abs_path, ref.trim());
		return generate_single_external_dao(gen, external_dao_xml_parser, dao_class_name, external_dao_xml_abs_path);
	}

	public static String[] generate_single_external_dao(IDaoCG gen, XmlParser external_dao_xml_parser,
			String sdm_dao_class_name, String external_dao_xml_abs_path) throws Exception {

		DaoClass external_dao_class = external_dao_xml_parser.unmarshal(external_dao_xml_abs_path);
		external_dao_class.setName(sdm_dao_class_name);
		return gen.translate(external_dao_class);
	}

	////////////////////////////////////////////////

	public static IDtoCG create_dto_cg(Connection conn, IEditor2 editor2, Settings settings,
			StringBuilder output_dir_rel_path) throws Exception {

		IProject project = editor2.get_project();
		String root_fn = editor2.get_root_file_name();
		String sdm_folder_abs_path = editor2.get_sdm_folder_abs_path();
		return create_dto_cg(conn, project, settings, root_fn, sdm_folder_abs_path, output_dir_rel_path);
	}

	public static IDtoCG create_dto_cg(Connection conn, IProject project, Settings settings, String root_fn,
			String sdm_folder_abs_path, StringBuilder output_dir_abs_path) throws Exception {

		String project_abs_path = project.getLocation().toPortableString();
		StringBuilder output_dir_rel_path = new StringBuilder();
		IDtoCG gen = TargetLangUtils.create_dto_cg(root_fn, project_abs_path, sdm_folder_abs_path, conn, settings,
				output_dir_rel_path);
		String abs_path = EclipseHelpers.get_absolute_dir_path_str(project, output_dir_rel_path.toString());
		output_dir_abs_path.append(abs_path);
		return gen;
	}

	public static IDaoCG create_dao_cg(Connection conn, IProject project, IEditor2 editor2, Settings settings,
			StringBuilder output_dir_abs_path) throws Exception {

		String root_fn = editor2.get_root_file_name();
		String sdm_folder_abs_path = editor2.get_sdm_folder_abs_path();
		return create_dao_cg(conn, project, root_fn, settings, sdm_folder_abs_path, output_dir_abs_path);
	}

	public static IDaoCG create_dao_cg(Connection conn, IProject project, String root_fn, Settings settings,
			String sdm_folder_abs_path, StringBuilder output_dir_abs_path) throws Exception {

		String project_abs_path = project.getLocation().toPortableString();
		StringBuilder output_dir_rel_path = new StringBuilder();
		IDaoCG gen = TargetLangUtils.create_dao_cg(root_fn, project_abs_path, sdm_folder_abs_path, conn, settings,
				output_dir_rel_path);
		String abs_path = EclipseHelpers.get_absolute_dir_path_str(project, output_dir_rel_path.toString());
		output_dir_abs_path.append(abs_path);
		return gen;
	}
}
