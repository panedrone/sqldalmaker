/*
 * Copyright 2011-2018 sqldalmaker@gmail.com
 * SQL DAL Maker Website: http://sqldalmaker.sourceforge.net
 * Read LICENSE.txt in the root of this project/archive for details.
 */
package com.sqldalmaker.intellij.ui;

import com.intellij.openapi.fileTypes.ExactFileNameMatcher;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeConsumer;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
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
import com.sqldalmaker.common.SdmUtils;
import com.sqldalmaker.common.XmlParser;
import com.sqldalmaker.jaxb.dto.DtoClasses;
import com.sqldalmaker.jaxb.settings.Settings;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: sqldalmaker@gmail.com
 * Date: 21.06.12
 */
public class IdeaTargetLanguageHelpers {

    public static boolean underscores_needed(VirtualFile root_file) {

        String fn = root_file.getName();

        if (RootFileName.RUBY.equals(fn) || RootFileName.PYTHON.equals(fn)) {

            return true;
        }

        return false;
    }

    public static void register(@NotNull FileTypeConsumer consumer, FileType file_type) {

        consumer.consume(file_type, new ExactFileNameMatcher(RootFileName.JAVA));
        consumer.consume(file_type, new ExactFileNameMatcher(RootFileName.CPP));
        // consumer.consume(file_type, new ExactFileNameMatcher(ProfileNames.OBJC));
        consumer.consume(file_type, new ExactFileNameMatcher(RootFileName.PHP));
        consumer.consume(file_type, new ExactFileNameMatcher(RootFileName.PYTHON));
        consumer.consume(file_type, new ExactFileNameMatcher(RootFileName.RUBY));
    }

    private static String get_unknown_root_file_msg(String fn) {

        return "Unknown root file: " + fn;
    }

    public static void open_editor(Project project, VirtualFile root_file,
                                   String value, Settings settings, String java_package) throws Exception {

        String rel_path;

        String fn = root_file.getName();

        if (RootFileName.JAVA.equals(fn)) {

            rel_path = SdmUtils.get_package_relative_path(settings, java_package) + "/" + value + ".java";

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

        IdeaEditorHelpers.open_module_file_in_editor(project, rel_path);
    }

    public static void prepare_generated_file_data(VirtualFile root_file, String class_name, String[] file_content,
                                                   List<IdeaHelpers.GeneratedFileData> list) throws Exception {

        String fn = root_file.getName();

        String file_name;

        if (RootFileName.JAVA.equals(fn)) {

            file_name = class_name + ".java";

        } else if (RootFileName.PHP.equals(fn)) {

            file_name = class_name + ".php";

        } else if (RootFileName.PYTHON.equals(fn)) {

            file_name = class_name + ".py";

        } else if (RootFileName.RUBY.equals(fn)) {

            file_name = Helpers.convert_to_ruby_file_name(class_name);

        } else if (RootFileName.CPP.equals(fn)) {

            file_name = class_name + ".h";

        } else {

            throw new Exception(get_unknown_root_file_msg(fn));
        }

        IdeaHelpers.GeneratedFileData gf = new IdeaHelpers.GeneratedFileData();
        gf.file_name = file_name;
        gf.file_content = file_content[0];
        list.add(gf);
    }

    public static void validate_dto(Project project, VirtualFile root_file, Settings settings,
                                    String class_name, String[] file_content,
                                    StringBuilder validation_buff) throws Exception {

        String source_folder = settings.getFolders().getTarget();

        String module_root = IdeaHelpers.get_project_base_dir(project).getPath();

        String fn = root_file.getName();

        String file_name;

        if (RootFileName.JAVA.equals(fn)) {

            String dao_destination = Helpers.concat_path(
                    module_root, source_folder, settings.getDto().getScope().replace('.', '/'));

            file_name = Helpers.concat_path(dao_destination, class_name + ".java");

        } else if (RootFileName.PHP.equals(fn)) {

            String dao_destination = Helpers.concat_path(module_root, source_folder);

            file_name = Helpers.concat_path(dao_destination, class_name + ".php");

        } else if (RootFileName.PYTHON.equals(fn)) {

            String dao_destination = Helpers.concat_path(module_root, source_folder);

            file_name = Helpers.concat_path(dao_destination, class_name + ".py");

        } else if (RootFileName.RUBY.equals(fn)) {

            String dao_destination = Helpers.concat_path(module_root, source_folder);

            file_name = Helpers.concat_path(dao_destination, Helpers.convert_to_ruby_file_name(class_name));

        } else if (RootFileName.CPP.equals(fn)) {

            String dao_destination = Helpers.concat_path(module_root, source_folder);

            file_name = Helpers.concat_path(dao_destination, class_name + ".h");

        } else {

            throw new Exception(get_unknown_root_file_msg(fn));
        }

        ////////////////////////////////

        String old_text = Helpers.load_text_from_file(file_name);

        if (old_text == null) {

            validation_buff.append(Const.OUTPUT_FILE_IS_MISSING);

        } else {

            String text = file_content[0];

            if (!old_text.equals(text)) {

                validation_buff.append(Const.OUTPUT_FILE_IS_OUT_OF_DATE);
            }
        }
    }

    public static void validate_dao(Project project, VirtualFile root_file,
                                    Settings settings, String dao_class_name,
                                    String[] file_content,
                                    StringBuilder validation_buff) throws Exception {

        String source_folder = settings.getFolders().getTarget();

        String module_root = IdeaHelpers.get_project_base_dir(project).getPath();

        String fn = root_file.getName();

        String file_name;

        if (RootFileName.JAVA.equals(fn)) {

            String dao_destination = Helpers.concat_path(
                    module_root, source_folder, settings.getDao().getScope().replace('.', '/'));

            file_name = Helpers.concat_path(dao_destination, dao_class_name + ".java");

        } else if (RootFileName.PHP.equals(fn)) {

            String dao_destination = Helpers.concat_path(module_root, source_folder);

            file_name = Helpers.concat_path(dao_destination, dao_class_name + ".php");

        } else if (RootFileName.PYTHON.equals(fn)) {

            String dao_destination = Helpers.concat_path(module_root, source_folder);

            file_name = Helpers.concat_path(dao_destination, dao_class_name + ".py");

        } else if (RootFileName.RUBY.equals(fn)) {

            String dao_destination = Helpers.concat_path(module_root, source_folder);

            file_name = Helpers.concat_path(dao_destination, Helpers.convert_to_ruby_file_name(dao_class_name));

        } else if (RootFileName.CPP.equals(fn)) {

            String dao_destination = Helpers.concat_path(module_root, source_folder);

            file_name = Helpers.concat_path(dao_destination, dao_class_name + ".h");

        } else {

            throw new Exception(get_unknown_root_file_msg(fn));
        }

        String old_text = Helpers.load_text_from_file(file_name);

        if (old_text == null) {

            validation_buff.append(Const.OUTPUT_FILE_IS_MISSING);

        } else {

            String text = file_content[0];

            if (!old_text.equals(text)) {

                validation_buff.append(Const.OUTPUT_FILE_IS_OUT_OF_DATE);
            }
        }
    }

    /*
     * returns null if the file is not root-file
     */
    public static String get_root_file_relative_path(Project project, VirtualFile file) {

        String fn = file.getName();

        if (RootFileName.JAVA.equals(fn)
                || RootFileName.CPP.equals(fn)
                || //ProfileNames.OBJC.equals(fn) ||
                RootFileName.PHP.equals(fn)
                || RootFileName.PYTHON.equals(fn)
                || RootFileName.RUBY.equals(fn)) {

            try {

                return IdeaHelpers.get_relative_path(project, file);

            } catch (Exception e) {

                e.printStackTrace();
            }
        }

        return null;
    }

    public static boolean accept(@NotNull VirtualFile file) {

        return RootFileName.JAVA.equals(file.getName()) ||
                RootFileName.CPP.equals(file.getName()) ||
                // ProfileNames.OBJC.equals(file.getName()) ||
                RootFileName.PHP.equals(file.getName()) ||
                RootFileName.PYTHON.equals(file.getName()) ||
                RootFileName.RUBY.equals(file.getName());
    }

    public static IDtoCG create_dto_cg(
            Connection connection, Project project, VirtualFile root_file,
            Settings settings, StringBuilder output_dir_rel_path) throws Exception {

        String project_abs_path = IdeaHelpers.get_project_base_dir(project).getPath();

        String sql_root_abs_path = Helpers.concat_path(project_abs_path, settings.getFolders().getSql());

        String xml_configs_folder_full_path = root_file.getParent().getPath();

        String dto_xml_abs_path = xml_configs_folder_full_path + "/" + Const.DTO_XML;

        String dto_xsd_abs_path = xml_configs_folder_full_path + "/" + Const.DTO_XSD;

        String vm_file_system_path;

        if (settings.getExternalVmFile().getPath().length() == 0) {

            vm_file_system_path = null;

        } else {

            vm_file_system_path = Helpers.concat_path(project_abs_path, settings.getExternalVmFile().getPath());
        }

        String context_path = DtoClasses.class.getPackage().getName();

        XmlParser xml_parser = new XmlParser(context_path, dto_xsd_abs_path);

        DtoClasses dto_classes = xml_parser.unmarshal(dto_xml_abs_path);

        String fn = root_file.getName();

        if (RootFileName.JAVA.equals(fn)) {

            FieldNamesMode field_names_mode;

            int fnm = settings.getDto().getFieldNamesMode();

            if (fnm == 1) {

                field_names_mode = FieldNamesMode.TO_LOWER_CAMEL_CASE;

            } else if (fnm == 2) {

                field_names_mode = FieldNamesMode.TO_LOWER_CASE;

            } else {

                field_names_mode = FieldNamesMode.AS_IS;
            }

            String dto_inheritance = settings.getDto().getInheritance();

            String dto_package = settings.getDto().getScope();

            if (output_dir_rel_path != null) {

                String package_rel_path = SdmUtils.get_package_relative_path(settings, dto_package);

                output_dir_rel_path.append(package_rel_path);
            }

            return new JavaCG.DTO(dto_classes, connection,
                    dto_package, sql_root_abs_path, dto_inheritance,
                    field_names_mode, vm_file_system_path);

        } else if (RootFileName.CPP.equals(fn)) {

            if (output_dir_rel_path != null) {

                String package_rel_path = settings.getFolders().getTarget();

                output_dir_rel_path.append(package_rel_path);
            }

            Settings sett = IdeaHelpers.load_settings(root_file);

            return new CppCG.DTO(dto_classes, sett.getTypeMap(), connection,
                    sql_root_abs_path, settings.getCpp().getClassPrefix(), vm_file_system_path);

        } else if (RootFileName.PHP.equals(fn)) {

            if (output_dir_rel_path != null) {

                String package_rel_path = settings.getFolders().getTarget();

                output_dir_rel_path.append(package_rel_path);
            }

            String dto_package = settings.getDto().getScope();

            return new PhpCG.DTO(dto_classes, connection,
                    sql_root_abs_path, vm_file_system_path, dto_package);

        } else if (RootFileName.PYTHON.equals(fn)) {

            if (output_dir_rel_path != null) {

                String package_rel_path = settings.getFolders().getTarget();

                output_dir_rel_path.append(package_rel_path);
            }

            return new PythonCG.DTO(dto_classes, connection,
                    sql_root_abs_path, vm_file_system_path);

        } else if (RootFileName.RUBY.equals(fn)) {

            if (output_dir_rel_path != null) {

                String package_rel_path = settings.getFolders().getTarget();

                output_dir_rel_path.append(package_rel_path);
            }

            return new RubyCG.DTO(dto_classes, connection,
                    sql_root_abs_path, vm_file_system_path);

        } else {

            throw new Exception(get_unknown_root_file_msg(fn));
        }
    }

    protected static IDaoCG create_dao_cg(Connection con, Project project, VirtualFile root_file,
                                          Settings settings, StringBuilder output_dir_rel_path) throws Exception {

        String project_abs_path = IdeaHelpers.get_project_base_dir(project).getPath();

        String sql_root_abs_path = Helpers.concat_path(project_abs_path, settings.getFolders().getSql());

        String xml_configs_folder_full_path = root_file.getParent().getPath();

        String dto_xml_abs_path = xml_configs_folder_full_path + "/" + Const.DTO_XML;

        String dto_xsd_abs_path = xml_configs_folder_full_path + "/" + Const.DTO_XSD;

        String vm_file_system_path;

        if (settings.getExternalVmFile().getPath().length() == 0) {

            vm_file_system_path = null;

        } else {

            vm_file_system_path = Helpers.concat_path(project_abs_path, settings.getExternalVmFile().getPath());
        }

        String context_path = DtoClasses.class.getPackage().getName();

        XmlParser xml_parser = new XmlParser(context_path, dto_xsd_abs_path);

        DtoClasses dto_classes = xml_parser.unmarshal(dto_xml_abs_path);

        String fn = root_file.getName();

        if (RootFileName.JAVA.equals(fn)) {

            FieldNamesMode field_names_mode;

            int fnm = settings.getDto().getFieldNamesMode();

            if (fnm == 1) {

                field_names_mode = FieldNamesMode.TO_LOWER_CAMEL_CASE;

            } else if (fnm == 2) {

                field_names_mode = FieldNamesMode.TO_LOWER_CASE;

            } else {

                field_names_mode = FieldNamesMode.AS_IS;
            }

            String dto_package = settings.getDto().getScope();

            String dao_package = settings.getDao().getScope();

            if (output_dir_rel_path != null) {

                String package_rel_path = SdmUtils.get_package_relative_path(settings, dao_package);

                output_dir_rel_path.append(package_rel_path);
            }

            return new JavaCG.DAO(dto_classes, con,
                    dto_package,
                    dao_package, sql_root_abs_path, field_names_mode, vm_file_system_path);

        } else if (RootFileName.CPP.equals(fn)) {

            if (output_dir_rel_path != null) {

                String package_rel_path = settings.getFolders().getTarget();

                output_dir_rel_path.append(package_rel_path);
            }

            String class_prefix = settings.getCpp().getClassPrefix();

            return new CppCG.DAO(dto_classes, settings.getTypeMap(), con,
                    sql_root_abs_path, class_prefix, vm_file_system_path);

        } else if (RootFileName.PHP.equals(fn)) {

            if (output_dir_rel_path != null) {

                String package_rel_path = settings.getFolders().getTarget();

                output_dir_rel_path.append(package_rel_path);
            }

            String dto_package = settings.getDto().getScope();

            String dao_package = settings.getDao().getScope();

            return new PhpCG.DAO(dto_classes, con,
                    sql_root_abs_path, vm_file_system_path, dto_package, dao_package);

        } else if (RootFileName.PYTHON.equals(fn)) {

            if (output_dir_rel_path != null) {

                String package_rel_path = settings.getFolders().getTarget();

                output_dir_rel_path.append(package_rel_path);
            }

            return new PythonCG.DAO(dto_classes, con,
                    sql_root_abs_path, vm_file_system_path);

        } else if (RootFileName.RUBY.equals(fn)) {

            if (output_dir_rel_path != null) {

                String package_rel_path = settings.getFolders().getTarget();

                output_dir_rel_path.append(package_rel_path);
            }

            return new RubyCG.DAO(dto_classes, con, sql_root_abs_path, vm_file_system_path);

        } else {

            throw new Exception(get_unknown_root_file_msg(fn));
        }
    }
}