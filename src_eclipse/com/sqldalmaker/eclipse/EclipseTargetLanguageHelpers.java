/*
 * Copyright 2011-2020 sqldalmaker@gmail.com
 * SQL DAL Maker Website: http://sqldalmaker.sourceforge.net
 * Read LICENSE.txt in the root of this project/archive for details.
 */
package com.sqldalmaker.eclipse;

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
import com.sqldalmaker.cg.cpp.CppCG;
import com.sqldalmaker.cg.go.GoCG;
import com.sqldalmaker.cg.java.JavaCG;
import com.sqldalmaker.cg.php.PhpCG;
import com.sqldalmaker.cg.python.PythonCG;
import com.sqldalmaker.cg.ruby.RubyCG;
import com.sqldalmaker.common.RootFileName;
import com.sqldalmaker.common.SdmUtils;
import com.sqldalmaker.common.XmlParser;
import com.sqldalmaker.jaxb.dto.DtoClasses;
import com.sqldalmaker.jaxb.settings.Settings;

/**
 *
 * @author sqldalmaker@gmail.com
 *
 */
public class EclipseTargetLanguageHelpers {

	public static boolean underscores_needed(IEditor2 editor2) {

		String root_fn = editor2.get_root_file_name();
		if (RootFileName.RUBY.equals(root_fn) || RootFileName.PYTHON.equals(root_fn)) {
			return true;
		}
		return false;
	}

	public static List<IFile> get_root_files(IContainer xml_mp_folder) throws Exception {

		if (!(xml_mp_folder instanceof IFolder)) {
			throw new Exception("IFolder expected");
		}
		List<IFile> root_files = new ArrayList<IFile>();
		IResource res;
		res = xml_mp_folder.findMember(RootFileName.PHP);
		if (res instanceof IFile) {
			root_files.add((IFile) res);
		}
		res = xml_mp_folder.findMember(RootFileName.JAVA);
		if (res instanceof IFile) {
			root_files.add((IFile) res);
		}
		res = xml_mp_folder.findMember(RootFileName.CPP);
		if (res instanceof IFile) {
			root_files.add((IFile) res);
		}
		res = xml_mp_folder.findMember(RootFileName.PYTHON);
		if (res instanceof IFile) {
			root_files.add((IFile) res);
		}
		res = xml_mp_folder.findMember(RootFileName.RUBY);
		if (res instanceof IFile) {
			root_files.add((IFile) res);
		}
		res = xml_mp_folder.findMember(RootFileName.GO);
		if (res instanceof IFile) {
			root_files.add((IFile) res);
		}
		return root_files;
	}

	private static String get_unknown_root_file_msg(String fn) {
		return "Unknown root file: " + fn;
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
		String vm_file_system_path;
		if (settings.getExternalVmFile().getPath().length() == 0) {
			vm_file_system_path = null;
		} else {
			String project_abs_path = project.getLocation().toPortableString();
			vm_file_system_path = Helpers.concat_path(project_abs_path, settings.getExternalVmFile().getPath());
		}
		String context_path = DtoClasses.class.getPackage().getName();
		XmlParser xml_parser = new XmlParser(context_path, dto_xsd_abs_path);
		DtoClasses dto_classes = xml_parser.unmarshal(dto_xml_abs_path);
		if (RootFileName.RUBY.equals(root_file_name)) {
			if (output_dir != null) {
				String rel_path = settings.getFolders().getTarget();
				String abs_path = EclipseHelpers.get_absolute_dir_path_str(project, rel_path);
				output_dir.append(abs_path);
			}
			RubyCG.DTO gen = new RubyCG.DTO(dto_classes, conn, sql_root_abs_path, vm_file_system_path);
			return gen;
		} else if (RootFileName.PYTHON.equals(root_file_name)) {
			if (output_dir != null) {
				String rel_path = settings.getFolders().getTarget();
				String abs_path = EclipseHelpers.get_absolute_dir_path_str(project, rel_path);
				output_dir.append(abs_path);
			}
			PythonCG.DTO gen = new PythonCG.DTO(dto_classes, conn, sql_root_abs_path, vm_file_system_path);
			return gen;
		} else if (RootFileName.PHP.equals(root_file_name)) {
			if (output_dir != null) {
				String rel_path = settings.getFolders().getTarget();
				String abs_path = EclipseHelpers.get_absolute_dir_path_str(project, rel_path);
				output_dir.append(abs_path);
			}
			String dto_package = settings.getDto().getScope();
			PhpCG.DTO gen = new PhpCG.DTO(dto_classes, conn, sql_root_abs_path, vm_file_system_path, dto_package);
			return gen;
		} else if (RootFileName.JAVA.equals(root_file_name)) {
			FieldNamesMode field_names_mode;
			int fnm = settings.getDto().getFieldNamesMode();
			if (fnm == 1) {
				field_names_mode = FieldNamesMode.LOWER_CAMEL_CASE;
			} else if (fnm == 2) {
				field_names_mode = FieldNamesMode.LOWER_CASE;
			} else {
				field_names_mode = FieldNamesMode.AS_IS;
			}
			String dto_package = settings.getDto().getScope();
			if (output_dir != null) {
				String rel_path = SdmUtils.get_package_relative_path(settings, dto_package);
				String abs_path = EclipseHelpers.get_absolute_dir_path_str(project, rel_path);
				output_dir.append(abs_path);
			}
			String dto_inheritance = settings.getDto().getInheritance();
			JavaCG.DTO gen = new JavaCG.DTO(dto_classes, conn, dto_package, sql_root_abs_path, dto_inheritance,
					field_names_mode, vm_file_system_path);
			return gen;
		} else if (RootFileName.CPP.equals(root_file_name)) {
			if (output_dir != null) {
				String rel_path = settings.getFolders().getTarget();
				String abs_path = EclipseHelpers.get_absolute_dir_path_str(project, rel_path);
				output_dir.append(abs_path);
			}
			CppCG.DTO gen = new CppCG.DTO(dto_classes, settings.getTypeMap(), conn, sql_root_abs_path,
					settings.getCpp().getClassPrefix(), vm_file_system_path);
			return gen;
		} else if (RootFileName.GO.equals(root_file_name)) {
			if (output_dir != null) {
				String rel_path = settings.getFolders().getTarget();
				String abs_path = EclipseHelpers.get_absolute_dir_path_str(project, rel_path);
				output_dir.append(abs_path);
			}
			String dto_package = settings.getDto().getScope();
			GoCG.DTO gen = new GoCG.DTO(dto_package, dto_classes, settings.getTypeMap(), conn, sql_root_abs_path,
					vm_file_system_path);
			return gen;
		} else {
			throw new Exception(get_unknown_root_file_msg(root_file_name));
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
		String vm_file_system_path;
		if (settings.getExternalVmFile().getPath().length() == 0) {
			vm_file_system_path = null;
		} else {
			String project_abs_path = project.getLocation().toPortableString();
			vm_file_system_path = Helpers.concat_path(project_abs_path, settings.getExternalVmFile().getPath());
		}
		String context_path = DtoClasses.class.getPackage().getName();
		XmlParser xml_parser = new XmlParser(context_path, dto_xsd_abs_path);
		DtoClasses dto_classes = xml_parser.unmarshal(dto_xml_abs_path);
		if (RootFileName.RUBY.equals(root_fn)) {
			if (output_dir != null) {
				String rel_path = settings.getFolders().getTarget();
				String abs_path = EclipseHelpers.get_absolute_dir_path_str(project, rel_path);
				output_dir.append(abs_path);
			}
			RubyCG.DAO gen = new RubyCG.DAO(dto_classes, conn, sql_root_abs_path, vm_file_system_path);
			return gen;
		} else if (RootFileName.PYTHON.equals(root_fn)) {
			String rel_path = settings.getFolders().getTarget();
			if (output_dir != null) {
				String abs_path = EclipseHelpers.get_absolute_dir_path_str(project, rel_path);
				output_dir.append(abs_path);
			}
			PythonCG.DAO gen = new PythonCG.DAO(dto_classes, conn, sql_root_abs_path, rel_path, vm_file_system_path);
			return gen;
		} else if (RootFileName.PHP.equals(root_fn)) {
			if (output_dir != null) {
				String rel_path = settings.getFolders().getTarget();
				String abs_path = EclipseHelpers.get_absolute_dir_path_str(project, rel_path);
				output_dir.append(abs_path);
			}
			String dto_package = settings.getDto().getScope();
			String dao_package = settings.getDao().getScope();
			PhpCG.DAO gen = new PhpCG.DAO(dto_classes, conn, sql_root_abs_path, vm_file_system_path, dto_package,
					dao_package);
			return gen;
		} else if (RootFileName.JAVA.equals(root_fn)) {
			FieldNamesMode field_names_mode;
			int fnm = settings.getDto().getFieldNamesMode();
			if (fnm == 1) {
				field_names_mode = FieldNamesMode.LOWER_CAMEL_CASE;
			} else if (fnm == 2) {
				field_names_mode = FieldNamesMode.LOWER_CASE;
			} else {
				field_names_mode = FieldNamesMode.AS_IS;
			}
			String dao_package = settings.getDao().getScope();
			if (output_dir != null) {
				String rel_path = SdmUtils.get_package_relative_path(settings, dao_package);
				String abs_path = EclipseHelpers.get_absolute_dir_path_str(project, rel_path);
				output_dir.append(abs_path);
			}
			String dto_package = settings.getDto().getScope();
			JavaCG.DAO gen = new JavaCG.DAO(dto_classes, conn, dto_package, dao_package, sql_root_abs_path,
					field_names_mode, vm_file_system_path);
			return gen;
		} else if (RootFileName.CPP.equals(root_fn)) {
			if (output_dir != null) {
				String rel_path = settings.getFolders().getTarget();
				String abs_path = EclipseHelpers.get_absolute_dir_path_str(project, rel_path);
				output_dir.append(abs_path);
			}
			String class_prefix = settings.getCpp().getClassPrefix();
			CppCG.DAO gen = new CppCG.DAO(dto_classes, settings.getTypeMap(), conn, sql_root_abs_path, class_prefix,
					vm_file_system_path);
			return gen;
		} else if (RootFileName.GO.equals(root_fn)) {
			if (output_dir != null) {
				String rel_path = settings.getFolders().getTarget();
				String abs_path = EclipseHelpers.get_absolute_dir_path_str(project, rel_path);
				output_dir.append(abs_path);
			}
			String dao_package = settings.getDao().getScope();
			GoCG.DAO gen = new GoCG.DAO(dao_package, dto_classes, settings.getTypeMap(), conn, sql_root_abs_path, vm_file_system_path);
			return gen;
		} else {
			throw new Exception(get_unknown_root_file_msg(root_fn));
		}
	}

	public static IFile find_source_file_in_project_tree(IProject project, Settings settings, String class_name,
			String java_package, String root_fn) throws Exception {

		if (RootFileName.JAVA.equals(root_fn)) {
			String path = SdmUtils.get_package_relative_path(settings, java_package) + "/" + class_name + ".java";
			return project.getFile(path);

		} else if (RootFileName.PHP.equals(root_fn)) {
			String path = settings.getFolders().getTarget() + "/" + class_name + ".php";
			return project.getFile(path);
		} else if (RootFileName.CPP.equals(root_fn)) {
			String path = settings.getFolders().getTarget() + "/" + class_name + ".h";
			return project.getFile(path);
		} else if (RootFileName.PYTHON.equals(root_fn)) {
			String path = settings.getFolders().getTarget() + "/" + class_name + ".py";
			return project.getFile(path);
		} else if (RootFileName.RUBY.equals(root_fn)) {
			String path = settings.getFolders().getTarget() + "/" + Helpers.convert_to_lower_underscores_file_name(class_name, "rb");
			return project.getFile(path);
		} else if (RootFileName.GO.equals(root_fn)) {
			String path = settings.getFolders().getTarget() + "/" + Helpers.convert_to_lower_underscores_file_name(class_name, "go");
			return project.getFile(path);
		}
		throw new Exception(get_unknown_root_file_msg(root_fn));
	}

	public static IResource find_root_file(IContainer meta_program_location) throws Exception {

		IResource res = meta_program_location.findMember(RootFileName.PHP);
		if (res instanceof IFile) {
			return res;
		}
		res = meta_program_location.findMember(RootFileName.JAVA);
		if (res instanceof IFile) {
			return res;
		}
		res = meta_program_location.findMember(RootFileName.CPP);
		if (res instanceof IFile) {
			return res;
		}
		res = meta_program_location.findMember(RootFileName.PYTHON);
		if (res instanceof IFile) {
			return res;
		}
		res = meta_program_location.findMember(RootFileName.RUBY);
		if (res instanceof IFile) {
			return res;
		}
		res = meta_program_location.findMember(RootFileName.GO);
		if (res instanceof IFile) {
			return res;
		}
		throw new Exception("Root file not found");
	}

	public static String get_rel_path(IEditor2 editor2, StringBuilder output_dir, String class_name) {

		String fn = editor2.get_root_file_name();
		return get_rel_path(fn, output_dir, class_name);
	}

	public static String get_rel_path(String fn, StringBuilder output_dir, String class_name) {

		if (RootFileName.RUBY.equals(fn)) {
			return output_dir + "/" + Helpers.convert_to_lower_underscores_file_name(class_name, "rb");
		} else if (RootFileName.PYTHON.equals(fn)) {
			return output_dir + "/" + class_name + ".py";
		} else if (RootFileName.PHP.equals(fn)) {
			return output_dir + "/" + class_name + ".php";
		} else if (RootFileName.JAVA.equals(fn)) {
			return output_dir + "/" + class_name + ".java";
		} else if (RootFileName.CPP.equals(fn)) {
			return output_dir + "/" + class_name + ".h";
		} else if (RootFileName.GO.equals(fn)) {
			return output_dir + "/" + Helpers.convert_to_lower_underscores_file_name(class_name, "go");
		}
		return null;
	}

	/**
	 *
	 * @param file
	 * @return null if the file is not root-file
	 */
	public static String get_root_file_relative_path(final IFile file) {

		String fn = file.getName();
		if (RootFileName.JAVA.equals(fn) || RootFileName.CPP.equals(fn) || 
				RootFileName.PHP.equals(fn) || RootFileName.PYTHON.equals(fn) || RootFileName.RUBY.equals(fn) ||
				RootFileName.GO.equals(fn)) {
			try {
				return file.getFullPath().toPortableString();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return null;
	}
}