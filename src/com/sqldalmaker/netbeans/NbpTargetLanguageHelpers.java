/*
 * Copyright 2011-2018 sqldalmaker@gmail.com
 * SQL DAL Maker Website: http://sqldalmaker.sourceforge.net
 * Read LICENSE.txt in the root of this project/archive for details.
 */
package com.sqldalmaker.netbeans;

import com.sqldalmaker.cg.FieldNamesMode;
import com.sqldalmaker.cg.Helpers;
import com.sqldalmaker.cg.IDaoCG;
import com.sqldalmaker.cg.IDtoCG;
import com.sqldalmaker.cg.cpp.CppCG;
import com.sqldalmaker.cg.java.JavaCG;
import com.sqldalmaker.cg.php.PhpCG;
import com.sqldalmaker.cg.python.PythonCG;
import com.sqldalmaker.cg.ruby.RubyCG;
import com.sqldalmaker.common.Const;
import com.sqldalmaker.common.RootFileName;
import com.sqldalmaker.common.XmlParser;
import com.sqldalmaker.jaxb.dto.DtoClasses;
import com.sqldalmaker.jaxb.settings.Settings;
import java.sql.Connection;
import org.openide.filesystems.FileObject;

/**
 *
 * @author sqldalmaker@gmail.com
 *
 */
public class NbpTargetLanguageHelpers {

    private static String get_unknown_root_file_msg(String fn) {

        return "Unknown root file: " + fn;
    }

    public static boolean underscores_needed(SdmDataObject obj) {

        String fn = obj.getPrimaryFile().getNameExt();

        if (RootFileName.RUBY.equals(fn) || RootFileName.PYTHON.equals(fn)) {

            return true;
        }

        return false;
    }

    public static String get_dao_xml_file_name(SdmDataObject obj, String class_name) throws Exception {

        String fn = obj.getPrimaryFile().getNameExt();

        if (RootFileName.RUBY.equals(fn)) {

            return Helpers.convert_to_ruby_file_name(class_name);

        } else if (RootFileName.PYTHON.equals(fn)) {

            return class_name + ".py";

        } else if (RootFileName.PHP.equals(fn)) {

            return class_name + ".php";

        } else if (RootFileName.JAVA.equals(fn)) {

            return class_name + ".java";

        } else if (RootFileName.CPP.equals(fn)) {

            return class_name + ".h";

        } else {

            throw new Exception(get_unknown_root_file_msg(fn));
        }
    }

    public static void validate_dto(SdmDataObject obj, Settings settings, String dto_class_name,
            String[] file_content, StringBuilder res_buf) throws Exception {

        String fn = obj.getPrimaryFile().getNameExt();

        String source_folder_rel_path = settings.getFolders().getTarget();

        String project_root_abs_path = NbpPathHelpers.get_root_folder_abs_path(obj.getPrimaryFile());

        String file_abs_path;

        if (RootFileName.JAVA.equals(fn)) {

            String destination = Helpers.concat_path(project_root_abs_path, source_folder_rel_path, settings.getDto().getScope().replace('.', '/'));

            file_abs_path = Helpers.concat_path(destination, dto_class_name + ".java");

        } else if (RootFileName.PHP.equals(fn)) {

            String dao_destination = Helpers.concat_path(project_root_abs_path, source_folder_rel_path);

            file_abs_path = Helpers.concat_path(dao_destination, dto_class_name + ".php");

        } else if (RootFileName.PYTHON.equals(fn)) {

            String dao_destination = Helpers.concat_path(project_root_abs_path, source_folder_rel_path);

            file_abs_path = Helpers.concat_path(dao_destination, dto_class_name + ".py");

        } else if (RootFileName.RUBY.equals(fn)) {

            String dao_destination = Helpers.concat_path(project_root_abs_path, source_folder_rel_path);

            file_abs_path = Helpers.concat_path(dao_destination, Helpers.convert_to_ruby_file_name(dto_class_name));

        } else if (RootFileName.CPP.equals(fn)) {

            String dao_destination = Helpers.concat_path(project_root_abs_path, source_folder_rel_path);

            file_abs_path = Helpers.concat_path(dao_destination, dto_class_name + ".h");

        } else {

            throw new Exception(get_unknown_root_file_msg(fn));
        }

        /////////////////////////////////////////////////////////
        //
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

    public static void validate_dao(SdmDataObject obj, Settings settings, String dao_class_name, String[] file_content, StringBuilder res_buf) throws Exception {

        String fn = obj.getPrimaryFile().getNameExt();

        String source_folder_rel_path = settings.getFolders().getTarget();

        String project_root_abs_path = NbpPathHelpers.get_root_folder_abs_path(obj.getPrimaryFile());

        String file_name;

        if (RootFileName.JAVA.equals(fn)) {

            String destination = Helpers.concat_path(project_root_abs_path, source_folder_rel_path, settings.getDao().getScope().replace('.', '/'));

            file_name = Helpers.concat_path(destination, dao_class_name + ".java");

        } else if (RootFileName.PHP.equals(fn)) {

            String destination = Helpers.concat_path(project_root_abs_path, source_folder_rel_path);

            file_name = Helpers.concat_path(destination, dao_class_name + ".php");

        } else if (RootFileName.PYTHON.equals(fn)) {

            String destination = Helpers.concat_path(project_root_abs_path, source_folder_rel_path);

            file_name = Helpers.concat_path(destination, dao_class_name + ".py");

        } else if (RootFileName.RUBY.equals(fn)) {

            String destination = Helpers.concat_path(project_root_abs_path, source_folder_rel_path);

            file_name = Helpers.concat_path(destination, Helpers.convert_to_ruby_file_name(dao_class_name));

        } else if (RootFileName.CPP.equals(fn)) {

            String destination = Helpers.concat_path(project_root_abs_path, source_folder_rel_path);

            file_name = Helpers.concat_path(destination, dao_class_name + ".h");

        } else {

            throw new Exception(get_unknown_root_file_msg(fn));
        }

        /////////////////////////////////////////////////////
        //
        String old_text = Helpers.load_text_from_file(file_name);

        if (old_text == null) {

            res_buf.append(Const.OUTPUT_FILE_IS_MISSING);

        } else {

            String text = file_content[0];

            if (old_text.compareTo(text) != 0) {

                res_buf.append(Const.OUTPUT_FILE_IS_OUT_OF_DATE);
            }
        }
    }

    public static void open_in_editor(SdmDataObject obj, String value, Settings settings, String scope) throws Exception {

        String fn = obj.getPrimaryFile().getNameExt();

        String rel_path;

        if (RootFileName.JAVA.equals(fn)) {

            rel_path = NbpPathHelpers.get_package_relative_path(settings, scope) + "/" + value + ".java";

        } else if (RootFileName.PHP.equals(fn)) {

            rel_path = settings.getFolders().getTarget() + "/" + value + ".php";

        } else if (RootFileName.PYTHON.equals(fn)) {

            rel_path = settings.getFolders().getTarget() + "/" + value + ".py";

        } else if (RootFileName.RUBY.equals(fn)) {

            rel_path = settings.getFolders().getTarget() + "/" + Helpers.convert_to_ruby_file_name(value);

        } else if (RootFileName.CPP.equals(fn)) {

            rel_path = settings.getFolders().getTarget() + "/" + value + ".h";

        } else {

            throw new Exception(get_unknown_root_file_msg(fn));
        }

        NbpIdeEditorHelpers.open_project_file_in_editor_async(obj, rel_path);
    }

    public static IDtoCG create_dto_cg(Connection connnection, SdmDataObject obj, Settings settings, StringBuilder output_dir_rel_path) throws Exception {

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

            RubyCG.DTO gen = new RubyCG.DTO(dto_classes, connnection, sql_root_abs_path, vm_file_system_path);

            return gen;

        } else if (RootFileName.PYTHON.equals(fn)) {

            if (output_dir_rel_path != null) {

                String package_rel_path = settings.getFolders().getTarget();

                output_dir_rel_path.append(package_rel_path);
            }

            PythonCG.DTO gen = new PythonCG.DTO(dto_classes, connnection, sql_root_abs_path, vm_file_system_path);

            return gen;

        } else if (RootFileName.PHP.equals(fn)) {

            if (output_dir_rel_path != null) {

                String package_rel_path = settings.getFolders().getTarget();

                output_dir_rel_path.append(package_rel_path);
            }

            String dto_package = settings.getDto().getScope();

            PhpCG.DTO gen = new PhpCG.DTO(dto_classes, connnection, sql_root_abs_path, vm_file_system_path, dto_package);

            return gen;

        } else if (RootFileName.JAVA.equals(fn)) {

            FieldNamesMode field_names_mode;

            int fnm = settings.getDto().getFieldNamesMode();

            switch (fnm) {

                case 1:

                    field_names_mode = FieldNamesMode.TO_LOWER_CAMEL_CASE;

                    break;

                case 2:

                    field_names_mode = FieldNamesMode.TO_LOWER_CASE;

                    break;

                default:

                    field_names_mode = FieldNamesMode.AS_IS;

                    break;
            }

            String dto_inheritance = settings.getDto().getInheritance();

            String dto_package = settings.getDto().getScope();

            if (output_dir_rel_path != null) {

                String package_rel_path = NbpPathHelpers.get_package_relative_path(settings, dto_package);

                output_dir_rel_path.append(package_rel_path);
            }

            JavaCG.DTO gen = new JavaCG.DTO(dto_classes, connnection, dto_package, sql_root_abs_path, dto_inheritance,
                    field_names_mode, vm_file_system_path);

            return gen;

        } else if (RootFileName.CPP.equals(fn)) {

            if (output_dir_rel_path != null) {

                String package_rel_path = settings.getFolders().getTarget();

                output_dir_rel_path.append(package_rel_path);
            }

            CppCG.DTO gen = new CppCG.DTO(dto_classes, settings.getTypeMap(), connnection, sql_root_abs_path,
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

        if (RootFileName.JAVA.equals(fn)) {

            FieldNamesMode field_names_mode;

            int fnm = settings.getDto().getFieldNamesMode();

            switch (fnm) {

                case 1:

                    field_names_mode = FieldNamesMode.TO_LOWER_CAMEL_CASE;

                    break;

                case 2:

                    field_names_mode = FieldNamesMode.TO_LOWER_CASE;

                    break;

                default:

                    field_names_mode = FieldNamesMode.AS_IS;

                    break;
            }

            String dto_package = settings.getDto().getScope();

            String dao_package = settings.getDao().getScope();

            if (output_dir != null) {

                String package_rel_path = NbpPathHelpers.get_package_relative_path(settings, dao_package);

                output_dir.append(package_rel_path);
            }

            return new JavaCG.DAO(dto_classes, con, dto_package, dao_package, sql_root_abs_path, field_names_mode, vm_file_system_path);

        } else if (RootFileName.CPP.equals(fn)) {

            if (output_dir != null) {

                String package_rel_path = settings.getFolders().getTarget();

                output_dir.append(package_rel_path);
            }

            String class_prefix = settings.getCpp().getClassPrefix();

            return new CppCG.DAO(dto_classes, settings.getTypeMap(), con, sql_root_abs_path, class_prefix, vm_file_system_path);

        } else if (RootFileName.PHP.equals(fn)) {

            if (output_dir != null) {

                String package_rel_path = settings.getFolders().getTarget();

                output_dir.append(package_rel_path);
            }

            String dto_package = settings.getDto().getScope();

            String dao_package = settings.getDao().getScope();

            return new PhpCG.DAO(dto_classes, con, sql_root_abs_path, vm_file_system_path, dto_package, dao_package);

        } else if (RootFileName.PYTHON.equals(fn)) {

            if (output_dir != null) {

                String package_rel_path = settings.getFolders().getTarget();

                output_dir.append(package_rel_path);
            }

            return new PythonCG.DAO(dto_classes, con, sql_root_abs_path, vm_file_system_path);

        } else if (RootFileName.RUBY.equals(fn)) {

            if (output_dir != null) {

                String package_rel_path = settings.getFolders().getTarget();

                output_dir.append(package_rel_path);
            }

            return new RubyCG.DAO(dto_classes, con, sql_root_abs_path, vm_file_system_path);

        } else {

            throw new Exception(get_unknown_root_file_msg(fn));
        }
    }

    /**
     *
     * @param root_folder
     * @param file
     * @return null if the file is not root-file
     */
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

                e.printStackTrace();
            }
        }

        return null;
    }
}
