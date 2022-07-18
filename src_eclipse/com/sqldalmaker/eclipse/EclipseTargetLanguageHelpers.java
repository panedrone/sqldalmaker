/*
	Copyright 2011-2022 sqldalmaker@gmail.com
	Read LICENSE.txt in the root of this project/archive.
	Project web-site: http://sqldalmaker.sourceforge.net
*/
package com.sqldalmaker.eclipse;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;

import com.sqldalmaker.cg.FieldNamesMode;
import com.sqldalmaker.cg.Helpers;
import com.sqldalmaker.cg.IDaoCG;
import com.sqldalmaker.cg.IDtoCG;
import com.sqldalmaker.cg.Xml2Vm;
import com.sqldalmaker.cg.cpp.CppCG;
import com.sqldalmaker.cg.go.GoCG;
import com.sqldalmaker.cg.java.JavaCG;
import com.sqldalmaker.cg.php.PhpCG;
import com.sqldalmaker.cg.python.PythonCG;
import com.sqldalmaker.cg.ruby.RubyCG;
import com.sqldalmaker.common.RootFileName;
import com.sqldalmaker.common.SdmUtils;
import com.sqldalmaker.common.TargetLangUtils;
import com.sqldalmaker.common.XmlParser;
import com.sqldalmaker.jaxb.dto.DtoClasses;
import com.sqldalmaker.jaxb.settings.Macros;
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
		String[] rf_names = { RootFileName.PHP, RootFileName.JAVA, RootFileName.CPP, RootFileName.RUBY,
				RootFileName.GO };
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
				|| RootFileName.PYTHON.equals(fn) || RootFileName.RUBY.equals(fn) || RootFileName.GO.equals(fn)) {
			try {
				return file.getFullPath().toPortableString();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	private static String get_dto_template(Settings settings, String project_abs_path) throws Exception {

		String m_name = settings.getDto().getMacro();
		return get_template(m_name, settings, project_abs_path);
	}

	private static String get_dao_template(Settings settings, String project_abs_path) throws Exception {

		String m_name = settings.getDao().getMacro();
		return get_template(m_name, settings, project_abs_path);
	}

	private static String get_template(String m_name, Settings settings, String project_abs_path) throws Exception {
		String vm_template;
// read the file or find the macro
		if (m_name == null || m_name.trim().length() == 0) {
			if (settings.getExternalVmFile().getPath().trim().length() == 0) {
				return null;
			} else {
				String vm_file_system_path = Helpers.concat_path(project_abs_path,
						settings.getExternalVmFile().getPath());
// https://stackoverflow.com/questions/4716503/reading-a-plain-text-file-in-java
				vm_template = new String(Files.readAllBytes(Paths.get(vm_file_system_path)));
				return vm_template;
			}
		}
		Macros.Macro vm_macro = null;
		for (Macros.Macro m : settings.getMacros().getMacro()) {
			if (m.getName().equalsIgnoreCase(m_name)) {
				vm_macro = m;
				break;
			}
		}
		if (vm_macro == null) {
			throw new Exception("Macro not found: " + m_name);
		}
		if (vm_macro.getVm() != null) {
			vm_template = vm_macro.getVm().trim();
		} else if (vm_macro.getVmXml() != null) {
			vm_template = Xml2Vm.parse(vm_macro.getVmXml());
		} else {
			throw new Exception("Expected <vm> or <vm-xml> in " + m_name);
		}
		return vm_template;
	}

	public static IDtoCG create_dto_cg(Connection conn, IEditor2 editor2, Settings settings, StringBuilder output_dir)
			throws Exception {

		IProject project = editor2.get_project();
		String root_file_name = editor2.get_root_file_name();
		String dto_xml_abs_path = editor2.get_dto_xml_abs_path();
		String dto_xsd_abs_path = editor2.get_dto_xsd_abs_path();
		return create_dto_cg(conn, project, settings, root_file_name, dto_xml_abs_path, dto_xsd_abs_path, output_dir);
	}

	public static IDtoCG create_dto_cg(Connection conn, IProject project, Settings settings, String root_file_name,
			String dto_xml_abs_path, String dto_xsd_abs_path, StringBuilder output_dir) throws Exception {

		String sql_root_abs_path = EclipseHelpers.get_absolute_dir_path_str(project, settings.getFolders().getSql());
		String project_abs_path = project.getLocation().toPortableString();
		String vm_template = get_dto_template(settings, project_abs_path);
		String context_path = DtoClasses.class.getPackage().getName();
		XmlParser xml_parser = new XmlParser(context_path, dto_xsd_abs_path);
		DtoClasses dto_classes = xml_parser.unmarshal(dto_xml_abs_path);
		if (RootFileName.RUBY.equals(root_file_name)) {
			if (output_dir != null) {
				String rel_path = settings.getFolders().getTarget();
				String abs_path = EclipseHelpers.get_absolute_dir_path_str(project, rel_path);
				output_dir.append(abs_path);
			}
			RubyCG.DTO gen = new RubyCG.DTO(dto_classes, settings, conn, sql_root_abs_path, vm_template);
			return gen;
		} else if (RootFileName.PYTHON.equals(root_file_name)) {
			if (output_dir != null) {
				String rel_path = settings.getFolders().getTarget();
				String abs_path = EclipseHelpers.get_absolute_dir_path_str(project, rel_path);
				output_dir.append(abs_path);
			}
			PythonCG.DTO gen = new PythonCG.DTO(dto_classes, settings, conn, sql_root_abs_path, vm_template);
			return gen;
		} else if (RootFileName.PHP.equals(root_file_name)) {
			if (output_dir != null) {
				String dto_package = settings.getDto().getScope();
				String rel_path = SdmUtils.get_package_relative_path(settings, dto_package);
				String abs_path = EclipseHelpers.get_absolute_dir_path_str(project, rel_path);
				output_dir.append(abs_path);
			}
			FieldNamesMode field_names_mode = Helpers.get_field_names_mode(settings);
			PhpCG.DTO gen = new PhpCG.DTO(dto_classes, settings, conn, sql_root_abs_path, vm_template,
					field_names_mode);
			return gen;
		} else if (RootFileName.JAVA.equals(root_file_name)) {
			FieldNamesMode field_names_mode = Helpers.get_field_names_mode(settings);
			String dto_package = settings.getDto().getScope();
			if (output_dir != null) {
				String rel_path = SdmUtils.get_package_relative_path(settings, dto_package);
				String abs_path = EclipseHelpers.get_absolute_dir_path_str(project, rel_path);
				output_dir.append(abs_path);
			}
			String dto_inheritance = settings.getDto().getInheritance();
			JavaCG.DTO gen = new JavaCG.DTO(dto_classes, settings, conn, dto_package, sql_root_abs_path,
					dto_inheritance, field_names_mode, vm_template);
			return gen;
		} else if (RootFileName.CPP.equals(root_file_name)) {
			if (output_dir != null) {
				String rel_path = settings.getFolders().getTarget();
				String abs_path = EclipseHelpers.get_absolute_dir_path_str(project, rel_path);
				output_dir.append(abs_path);
			}
			CppCG.DTO gen = new CppCG.DTO(dto_classes, settings, conn, sql_root_abs_path,
					settings.getCpp().getClassPrefix(), vm_template);
			return gen;
		} else if (RootFileName.GO.equals(root_file_name)) {
			if (output_dir != null) {
				String rel_path = TargetLangUtils.get_golang_dto_folder_rel_path(settings);
				String abs_path = EclipseHelpers.get_absolute_dir_path_str(project, rel_path);
				output_dir.append(abs_path);
			}
			FieldNamesMode field_names_mode = Helpers.get_field_names_mode(settings);
			GoCG.DTO gen = new GoCG.DTO(dto_classes, settings, conn, sql_root_abs_path, field_names_mode,
					vm_template);
			return gen;
		} else {
			throw new Exception(TargetLangUtils.get_unknown_root_file_msg(root_file_name));
		}
	}

	public static IDaoCG create_dao_cg(Connection conn, IProject project, IEditor2 editor2, Settings settings,
			StringBuilder output_dir) throws Exception {

		String root_fn = editor2.get_root_file_name();
		String dto_xml_abs_path = editor2.get_dto_xml_abs_path();
		String dto_xsd_abs_path = editor2.get_dto_xsd_abs_path();
		return create_dao_cg(conn, project, root_fn, settings, dto_xml_abs_path, dto_xsd_abs_path, output_dir);
	}

	public static IDaoCG create_dao_cg(Connection conn, IProject project, String root_fn, Settings settings,
			String dto_xml_abs_path, String dto_xsd_abs_path, StringBuilder output_dir) throws Exception {

		String sql_root_abs_path = EclipseHelpers.get_absolute_dir_path_str(project, settings.getFolders().getSql());
		String project_abs_path = project.getLocation().toPortableString();
		String vm_template = get_dao_template(settings, project_abs_path);
		String context_path = DtoClasses.class.getPackage().getName();
		XmlParser xml_parser = new XmlParser(context_path, dto_xsd_abs_path);
		DtoClasses dto_classes = xml_parser.unmarshal(dto_xml_abs_path);
		if (RootFileName.RUBY.equals(root_fn)) {
			if (output_dir != null) {
				String rel_path = settings.getFolders().getTarget();
				String abs_path = EclipseHelpers.get_absolute_dir_path_str(project, rel_path);
				output_dir.append(abs_path);
			}
			RubyCG.DAO gen = new RubyCG.DAO(dto_classes, settings, conn, sql_root_abs_path, vm_template);
			return gen;
		} else if (RootFileName.PYTHON.equals(root_fn)) {
			String rel_path = settings.getFolders().getTarget();
			if (output_dir != null) {
				String abs_path = EclipseHelpers.get_absolute_dir_path_str(project, rel_path);
				output_dir.append(abs_path);
			}
			String dto_package = rel_path = rel_path.replace("/", ".").replace("\\", ".");
			PythonCG.DAO gen = new PythonCG.DAO(dto_package, dto_classes, settings, conn, sql_root_abs_path,
					vm_template);
			return gen;
		} else if (RootFileName.PHP.equals(root_fn)) {
			if (output_dir != null) {
				String dao_package = settings.getDao().getScope();
				String rel_path = SdmUtils.get_package_relative_path(settings, dao_package);
				String abs_path = EclipseHelpers.get_absolute_dir_path_str(project, rel_path);
				output_dir.append(abs_path);
			}
			FieldNamesMode field_names_mode = Helpers.get_field_names_mode(settings);
			PhpCG.DAO gen = new PhpCG.DAO(dto_classes, settings, conn, sql_root_abs_path, vm_template,
					field_names_mode);
			return gen;
		} else if (RootFileName.JAVA.equals(root_fn)) {
			String dao_package = settings.getDao().getScope();
			if (output_dir != null) {
				String rel_path = SdmUtils.get_package_relative_path(settings, dao_package);
				String abs_path = EclipseHelpers.get_absolute_dir_path_str(project, rel_path);
				output_dir.append(abs_path);
			}
			FieldNamesMode field_names_mode = Helpers.get_field_names_mode(settings);
			String dto_package = settings.getDto().getScope();
			JavaCG.DAO gen = new JavaCG.DAO(dto_classes, settings, conn, dto_package, dao_package, sql_root_abs_path,
					field_names_mode, vm_template);
			return gen;
		} else if (RootFileName.CPP.equals(root_fn)) {
			if (output_dir != null) {
				String rel_path = settings.getFolders().getTarget();
				String abs_path = EclipseHelpers.get_absolute_dir_path_str(project, rel_path);
				output_dir.append(abs_path);
			}
			String class_prefix = settings.getCpp().getClassPrefix();
			CppCG.DAO gen = new CppCG.DAO(dto_classes, settings, conn, sql_root_abs_path, class_prefix, vm_template);
			return gen;
		} else if (RootFileName.GO.equals(root_fn)) {
			if (output_dir != null) {
				String rel_path = TargetLangUtils.get_golang_dao_folder_rel_path(settings);
				String abs_path = EclipseHelpers.get_absolute_dir_path_str(project, rel_path);
				output_dir.append(abs_path);
			}
			FieldNamesMode field_names_mode = Helpers.get_field_names_mode(settings);
			GoCG.DAO gen = new GoCG.DAO(dto_classes, settings, conn, sql_root_abs_path, field_names_mode, vm_template);
			return gen;
		} else {
			throw new Exception(TargetLangUtils.get_unknown_root_file_msg(root_fn));
		}
	}
}