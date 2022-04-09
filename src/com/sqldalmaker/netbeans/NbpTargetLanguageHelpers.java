/*
    Copyright 2011-2022 sqldalmaker@gmail.com
    SQL DAL Maker Website: http://sqldalmaker.sourceforge.net
    Read LICENSE.txt in the root of this project/archive for details.
 */
package com.sqldalmaker.netbeans;

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
import com.sqldalmaker.common.Const;
import com.sqldalmaker.common.RootFileName;
import com.sqldalmaker.common.SdmUtils;
import com.sqldalmaker.common.XmlParser;
import com.sqldalmaker.jaxb.dto.DtoClasses;
import com.sqldalmaker.jaxb.settings.Settings;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import org.openide.filesystems.FileObject;

/**
 *
 * @author sqldalmaker@gmail.com
 *
 */
public class NbpTargetLanguageHelpers {

    public static List<FileObject> find_root_files(FileObject xml_mp_dir) {
        List<FileObject> root_files = new ArrayList<FileObject>();
        if (xml_mp_dir == null) {
            return root_files;
        }
        String[] rfn = {RootFileName.PHP, RootFileName.JAVA, RootFileName.CPP,
            RootFileName.PYTHON, RootFileName.RUBY, RootFileName.GO};
        for (String fn : rfn) {
            FileObject root_file = xml_mp_dir.getFileObject(fn);
            if (root_file != null) {
                root_files.add(root_file);
            }
        }
        return root_files;
    }

    private static String get_unknown_root_file_msg(String fn) {
        return "Unknown root file: " + fn;
    }

    public static boolean snake_case_needed(SdmDataObject obj) {
        String fn = obj.getPrimaryFile().getNameExt();
        if (RootFileName.RUBY.equals(fn) || RootFileName.PYTHON.equals(fn)) {
            return true;
        }
        return false;
    }

    public static boolean lower_camel_case_needed(SdmDataObject obj) {
        String fn = obj.getPrimaryFile().getNameExt();
        if (RootFileName.GO.equals(fn)) {
            return true;
        }
        return false;
    }

    public static void validate_dto(SdmDataObject obj, Settings settings, String dto_class_name,
            String[] file_content, StringBuilder res_buf) throws Exception {
        String root_fn = obj.getPrimaryFile().getNameExt();
        String source_folder_rel_path = settings.getFolders().getTarget();
        String project_root_abs_path = NbpPathHelpers.get_root_folder_abs_path(obj.getPrimaryFile());
        String target_folder = get_target_folder(root_fn, project_root_abs_path, source_folder_rel_path, settings.getDto().getScope());
        String file_abs_path = Helpers.concat_path(target_folder, get_target_file_name(root_fn, dto_class_name));
        /////////////////////////////////////////////////////////
        String old_text = Helpers.load_text_from_file(file_abs_path);
        if (old_text == null) {
            res_buf.append(Const.OUTPUT_FILE_IS_MISSING);
        } else {
            String text = file_content[0];
            if (!old_text.equals(text)) {
                res_buf.append(Const.OUTPUT_FILE_IS_OUT_OF_DATE);
            }
        }
    }

    public static String get_target_folder(String root_fn, String project_root_abs_path, String source_folder_rel_path, String scope) {
        if (RootFileName.JAVA.equals(root_fn)) {
            return Helpers.concat_path(project_root_abs_path, source_folder_rel_path, scope.replace('.', '/'));
        } else {
            return Helpers.concat_path(project_root_abs_path, source_folder_rel_path);
        }
    }

    public static void validate_dao(SdmDataObject obj, Settings settings, String dao_class_name, String[] file_content, StringBuilder res_buf) throws Exception {
        String root_fn = obj.getPrimaryFile().getNameExt();
        String source_folder_rel_path = settings.getFolders().getTarget();
        String project_root_abs_path = NbpPathHelpers.get_root_folder_abs_path(obj.getPrimaryFile());
        String target_folder = get_target_folder(root_fn, project_root_abs_path, source_folder_rel_path, settings.getDao().getScope());
        String file_abs_path = Helpers.concat_path(target_folder, get_target_file_name(root_fn, dao_class_name));
        /////////////////////////////////////////////////////
        String old_text = Helpers.load_text_from_file(file_abs_path);
        if (old_text == null) {
            res_buf.append(Const.OUTPUT_FILE_IS_MISSING);
        } else {
            String text = file_content[0];
            if (old_text.compareTo(text) != 0) {
                res_buf.append(Const.OUTPUT_FILE_IS_OUT_OF_DATE);
            }
        }
    }

    public static String get_target_file_name(SdmDataObject obj, String class_name) throws Exception {
        String root_fn = obj.getPrimaryFile().getNameExt();
        return get_target_file_name(root_fn, class_name);
    }

    public static String get_target_file_name(String root_fn, String class_name) throws Exception {
        if (RootFileName.JAVA.equals(root_fn)) {
            return class_name + ".java";
        } else if (RootFileName.PHP.equals(root_fn)) {
            return class_name + ".php";
        } else if (RootFileName.CPP.equals(root_fn)) {
            return class_name + ".h";
        } else if (RootFileName.PYTHON.equals(root_fn)) {
            return Helpers.convert_file_name_to_snake_case(class_name, "py");
        } else if (RootFileName.RUBY.equals(root_fn)) {
            return Helpers.convert_file_name_to_snake_case(class_name, "rb");
        } else if (RootFileName.GO.equals(root_fn)) {
            return Helpers.convert_file_name_to_snake_case(class_name, "go");
        }
        throw new Exception(get_unknown_root_file_msg(root_fn));
    }

    public static String get_rel_path(Settings settings, String root_fn, String file_name, String scope) {
        if (RootFileName.JAVA.equals(root_fn)) {
            return Helpers.concat_path(SdmUtils.get_package_relative_path(settings, scope), file_name);
        } else {
            return Helpers.concat_path(settings.getFolders().getTarget(), file_name);
        }
    }

    public static String get_rel_path(SdmDataObject obj, Settings settings, String class_name, String scope) throws Exception {
        String root_fn = obj.getPrimaryFile().getNameExt();
        String target_file_name = get_target_file_name(root_fn, class_name);
        String rel_path = get_rel_path(settings, root_fn, target_file_name, scope);
        return rel_path;
    }

    public static void open_in_editor_async(SdmDataObject obj, Settings settings, String class_name, String scope) throws Exception {
        String rel_path = get_rel_path(obj, settings, class_name, scope);
        NbpIdeEditorHelpers.open_project_file_in_editor_async(obj, rel_path);
    }

    public static IDtoCG create_dto_cg(Connection connection, SdmDataObject obj,
            Settings settings, StringBuilder output_dir_rel_path) throws Exception {
        String fn = obj.getPrimaryFile().getNameExt();
        String sql_root_abs_path = NbpPathHelpers.get_absolute_dir_path_str(obj, settings.getFolders().getSql());
        String dto_xml_abs_path = NbpPathHelpers.get_dto_xml_abs_path(obj);
        String dto_xsd_abs_path = NbpPathHelpers.get_dto_xsd_abs_path(obj);
        String vm_file_system_path;
        if (settings.getExternalVmFile().getPath().length() == 0) {
            vm_file_system_path = null;
        } else {
            vm_file_system_path = NbpPathHelpers.get_absolute_dir_path_str(obj, settings.getExternalVmFile().getPath());
        }
        String context_path = DtoClasses.class.getPackage().getName();
        XmlParser xml_parser = new XmlParser(context_path, dto_xsd_abs_path);
        DtoClasses dto_classes = xml_parser.unmarshal(dto_xml_abs_path);
        if (RootFileName.RUBY.equals(fn)) {
            if (output_dir_rel_path != null) {
                String package_rel_path = settings.getFolders().getTarget();
                output_dir_rel_path.append(package_rel_path);
            }
            RubyCG.DTO gen = new RubyCG.DTO(dto_classes, settings.getTypeMap(),
                    connection, sql_root_abs_path, vm_file_system_path);
            return gen;
        } else if (RootFileName.PYTHON.equals(fn)) {
            if (output_dir_rel_path != null) {
                String package_rel_path = settings.getFolders().getTarget();
                output_dir_rel_path.append(package_rel_path);
            }
            PythonCG.DTO gen = new PythonCG.DTO(dto_classes, settings.getTypeMap(),
                    connection, sql_root_abs_path, vm_file_system_path);
            return gen;
        } else if (RootFileName.GO.equals(fn)) {
            if (output_dir_rel_path != null) {
                String package_rel_path = settings.getFolders().getTarget();
                output_dir_rel_path.append(package_rel_path);
            }
            String dto_package = settings.getDto().getScope();
            FieldNamesMode field_names_mode = Helpers.get_field_names_mode(settings);
            return new GoCG.DTO(dto_package, dto_classes, settings.getTypeMap(),
                    connection, sql_root_abs_path, field_names_mode, vm_file_system_path);
        } else if (RootFileName.PHP.equals(fn)) {
            if (output_dir_rel_path != null) {
                String package_rel_path = settings.getFolders().getTarget();
                output_dir_rel_path.append(package_rel_path);
            }
            FieldNamesMode field_names_mode = Helpers.get_field_names_mode(settings);
            String dto_package = settings.getDto().getScope();
            PhpCG.DTO gen = new PhpCG.DTO(dto_classes, settings.getTypeMap(),
                    connection, sql_root_abs_path, vm_file_system_path, dto_package, field_names_mode);
            return gen;
        } else if (RootFileName.JAVA.equals(fn)) {
            FieldNamesMode field_names_mode = Helpers.get_field_names_mode(settings);
            String dto_inheritance = settings.getDto().getInheritance();
            String dto_package = settings.getDto().getScope();
            if (output_dir_rel_path != null) {
                String package_rel_path = SdmUtils.get_package_relative_path(settings, dto_package);
                output_dir_rel_path.append(package_rel_path);
            }
            JavaCG.DTO gen = new JavaCG.DTO(dto_classes, settings.getTypeMap(), connection, dto_package,
                    sql_root_abs_path, dto_inheritance, field_names_mode, vm_file_system_path);
            return gen;
        } else if (RootFileName.CPP.equals(fn)) {
            if (output_dir_rel_path != null) {
                String package_rel_path = settings.getFolders().getTarget();
                output_dir_rel_path.append(package_rel_path);
            }
            CppCG.DTO gen = new CppCG.DTO(dto_classes, settings.getTypeMap(), connection, sql_root_abs_path,
                    settings.getCpp().getClassPrefix(), vm_file_system_path);
            return gen;
        } else {
            throw new Exception(get_unknown_root_file_msg(fn));
        }
    }

    public static IDaoCG create_dao_cg(Connection con, SdmDataObject obj, Settings settings, StringBuilder output_dir) throws Exception {
        String fn = obj.getPrimaryFile().getNameExt();
        String sql_root_abs_path = NbpPathHelpers.get_absolute_dir_path_str(obj, settings.getFolders().getSql());
        String dto_xml_abs_path = NbpPathHelpers.get_dto_xml_abs_path(obj);
        String dto_xsd_abs_path = NbpPathHelpers.get_dto_xsd_abs_path(obj);
        String vm_file_system_path;
        if (settings.getExternalVmFile().getPath().length() == 0) {
            vm_file_system_path = null;
        } else {
            vm_file_system_path = NbpPathHelpers.get_absolute_dir_path_str(obj, settings.getExternalVmFile().getPath());
        }
        String context_path = DtoClasses.class.getPackage().getName();
        XmlParser xml_Parser = new XmlParser(context_path, dto_xsd_abs_path);
        DtoClasses dto_classes = xml_Parser.unmarshal(dto_xml_abs_path);
        if (RootFileName.PHP.equals(fn)) {
            if (output_dir != null) {
                String package_rel_path = settings.getFolders().getTarget();
                output_dir.append(package_rel_path);
            }
            FieldNamesMode field_names_mode = Helpers.get_field_names_mode(settings);
            String dto_package = settings.getDto().getScope();
            String dao_package = settings.getDao().getScope();
            return new PhpCG.DAO(dto_classes, settings.getTypeMap(), con,
                    sql_root_abs_path, vm_file_system_path, dto_package, dao_package, field_names_mode);
        } else if (RootFileName.JAVA.equals(fn)) {
            FieldNamesMode field_names_mode = Helpers.get_field_names_mode(settings);
            String dto_package = settings.getDto().getScope();
            String dao_package = settings.getDao().getScope();
            if (output_dir != null) {
                String package_rel_path = SdmUtils.get_package_relative_path(settings, dao_package);
                output_dir.append(package_rel_path);
            }
            return new JavaCG.DAO(dto_classes, settings.getTypeMap(), con, dto_package,
                    dao_package, sql_root_abs_path, field_names_mode, vm_file_system_path);
        } else if (RootFileName.CPP.equals(fn)) {
            if (output_dir != null) {
                String package_rel_path = settings.getFolders().getTarget();
                output_dir.append(package_rel_path);
            }
            String class_prefix = settings.getCpp().getClassPrefix();
            return new CppCG.DAO(dto_classes, settings.getTypeMap(), con,
                    sql_root_abs_path, class_prefix, vm_file_system_path);
        } else if (RootFileName.PYTHON.equals(fn)) {
            String package_rel_path = settings.getFolders().getTarget();
            if (output_dir != null) {
                output_dir.append(package_rel_path);
            }
            String dto_package = package_rel_path.replace("/", ".").replace("\\", ".");
            return new PythonCG.DAO(dto_package, dto_classes, settings.getTypeMap(),
                    con, sql_root_abs_path, vm_file_system_path);
        } else if (RootFileName.RUBY.equals(fn)) {
            if (output_dir != null) {
                String package_rel_path = settings.getFolders().getTarget();
                output_dir.append(package_rel_path);
            }
            return new RubyCG.DAO(dto_classes, settings.getTypeMap(), con,
                    sql_root_abs_path, vm_file_system_path);
        } else if (RootFileName.GO.equals(fn)) {
            if (output_dir != null) {
                String package_rel_path = settings.getFolders().getTarget();
                output_dir.append(package_rel_path);
            }
            String dao_package = settings.getDao().getScope();
            FieldNamesMode field_names_mode = Helpers.get_field_names_mode(settings);
            return new GoCG.DAO(dao_package, dto_classes, settings.getTypeMap(),
                    con, sql_root_abs_path, field_names_mode, vm_file_system_path);
        } else {
            throw new Exception(get_unknown_root_file_msg(fn));
        }
    }

    public static String get_root_file_relative_path(final FileObject root_folder, FileObject file) {
        String fn = file.getNameExt();
        if (RootFileName.JAVA.equals(fn)
                || RootFileName.CPP.equals(fn)
                || //ProfileNames.OBJC.equals(fn) ||
                RootFileName.PHP.equals(fn)
                || RootFileName.PYTHON.equals(fn)
                || RootFileName.RUBY.equals(fn)) {
            try {
                return NbpPathHelpers.get_relative_path(root_folder, file);
            } catch (Exception e) {
                // e.printStackTrace();
            }
        }
        return null;
    }
}
