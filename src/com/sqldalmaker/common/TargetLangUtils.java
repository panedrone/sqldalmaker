/*
    Copyright 2011-2023 sqldalmaker@gmail.com
    SQL DAL Maker Website: https://sqldalmaker.sourceforge.net/
    Read LICENSE.txt in the root of this project/archive for details.
 */
package com.sqldalmaker.common;

import com.sqldalmaker.cg.*;
import com.sqldalmaker.cg.cpp.CppCG;
import com.sqldalmaker.cg.go.GoCG;
import com.sqldalmaker.cg.java.JavaCG;
import com.sqldalmaker.cg.php.PhpCG;
import com.sqldalmaker.cg.python.PythonCG;
import com.sqldalmaker.jaxb.sdm.*;
import com.sqldalmaker.jaxb.settings.Macros;
import com.sqldalmaker.jaxb.settings.Settings;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.util.List;

public class TargetLangUtils {

    public static String file_name_from_class_name(String root_fn, String class_name) throws Exception {
        int model_name_end_index = class_name.indexOf('-');
        if (model_name_end_index != -1) {
            class_name = class_name.substring(model_name_end_index + 1);
        }
        if (Const.Root.PHP.equals(root_fn)) {
            return class_name + ".php";
        } else if (Const.Root.JAVA.equals(root_fn)) {
            return class_name + ".java";
        } else if (Const.Root.CPP.equals(root_fn)) {
            return class_name + ".h";
        } else if (Const.Root.PYTHON.equals(root_fn)) {
            return Helpers.convert_file_name_to_snake_case(class_name, "py");
        } else if (Const.Root.GO.equals(root_fn)) {
            return Helpers.convert_file_name_to_snake_case(class_name, "go");
        }
        throw new Exception(get_unknown_root_file_msg(root_fn));
    }

    public static boolean accept(String root_fn) {
        return Const.Root.JAVA.equals(root_fn) ||
                Const.Root.CPP.equals(root_fn) ||
                Const.Root.PHP.equals(root_fn) ||
                Const.Root.PYTHON.equals(root_fn) ||
                Const.Root.GO.equals(root_fn);
    }

    public static String get_unknown_root_file_msg(String fn) {
        return "Unknown root file: " + fn;
    }

    public static String get_target_file_path(String root_fn, String output_dir, String class_name) throws Exception {
        String file_name = file_name_from_class_name(root_fn, class_name);
        return Helpers.concat_path(output_dir, file_name);
    }

    public static boolean snake_case_needed(String fn) {
        return Const.Root.PYTHON.equals(fn);
    }

    public static boolean lower_camel_case_needed(String fn) {
        return Const.Root.GO.equals(fn);
    }

    public static String get_target_folder_abs_path(
            String class_scope,
            String root_file_fn,
            String target_folder_rel_path,
            String module_root) {

        String res;
        if (Const.Root.JAVA.equals(root_file_fn) || Const.Root.PHP.equals(root_file_fn)) {
            class_scope = class_scope.replace('.', '/').replace('\\', '/');
            res = Helpers.concat_path(module_root, target_folder_rel_path, class_scope);
        } else {
            res = Helpers.concat_path(module_root, target_folder_rel_path);
        }
        return res;
    }

    public static String get_golang_dto_folder_rel_path(Settings settings) throws Exception {
        String dto_scope = settings.getDto().getScope().replace("\\", "/").trim();
        if (dto_scope.isEmpty()) {
            throw new Exception(Const.GOLANG_SCOPES_ERR);
        }
        return dto_scope;
    }

    public static String get_golang_dao_folder_rel_path(Settings settings) throws Exception {
        String dao_scope = settings.getDao().getScope().replace("\\", "/").trim();
        if (dao_scope.isEmpty()) {
            throw new Exception(Const.GOLANG_SCOPES_ERR);
        }
        return dao_scope;
    }

    public static String get_dto_vm_template(Settings settings, String project_abs_path) throws Exception {
        String macro_name = settings.getDto().getMacro();
        return _get_vm_template(macro_name, settings, project_abs_path);
    }

    public static String get_dao_vm_template(Settings settings, String project_abs_path) throws Exception {
        String macro_name = settings.getDao().getMacro();
        return _get_vm_template(macro_name, settings, project_abs_path);
    }

    private static String _get_vm_template(String macro_name, Settings settings, String project_abs_path) throws Exception {
        // read the file or find the macro
        if (macro_name == null || macro_name.trim().isEmpty()) {
            return null;
        }
        String vm_template;
        if (macro_name.endsWith(".vm")) {
            String vm_file_system_path = Helpers.concat_path(project_abs_path, macro_name);
            vm_template = new String(Files.readAllBytes(Paths.get(vm_file_system_path)));
            return vm_template;
        }
        Macros.Macro vm_macro = null;
        for (Macros.Macro m : settings.getMacros().getMacro()) {
            if (m.getName().equalsIgnoreCase(macro_name)) {
                vm_macro = m;
                break;
            }
        }
        if (vm_macro == null) {
            throw new Exception("Macro not found: " + macro_name);
        }
        if (vm_macro.getVm() != null) {
            vm_template = vm_macro.getVm().trim();
        } else if (vm_macro.getVmXml() != null) {
            vm_template = Xml2Vm.parse(vm_macro.getVmXml());
        } else {
            throw new Exception("Expected <vm> or <vm-xml> in " + macro_name);
        }
        return vm_template;
    }

    public static IDtoCG create_dto_cg(
            String root_fn,
            String project_abs_path,
            String xml_configs_folder_full_path,
            Connection connection,
            Settings settings,
            StringBuilder output_dir_rel_path) throws Exception {

        String sql_root_abs_path = Helpers.concat_path(project_abs_path, settings.getFolders().getSql());
        String vm_template = TargetLangUtils.get_dto_vm_template(settings, project_abs_path);
        String sdm_xml_abs_path = Helpers.concat_path(xml_configs_folder_full_path, Const.SDM_XML);
        String sdm_xsd_abs_path = Helpers.concat_path(xml_configs_folder_full_path, Const.SDM_XSD);
        String context_path = Sdm.class.getPackage().getName();
        XmlParser xml_parser = new XmlParser(context_path, sdm_xsd_abs_path);
        Sdm sdm = xml_parser.unmarshal(sdm_xml_abs_path);
        ////////////////////////////////////////////////////
        if (Const.Root.PHP.equals(root_fn)) {
            if (output_dir_rel_path != null) {
                String dto_package = settings.getDto().getScope();
                String package_rel_path = SdmUtils.get_package_relative_path(settings, dto_package);
                output_dir_rel_path.append(package_rel_path);
            }
            FieldNamesMode field_names_mode = Helpers.get_field_names_mode(settings);
            return new PhpCG.DTO(sdm, settings, connection, sql_root_abs_path, vm_template, field_names_mode);
        } else if (Const.Root.JAVA.equals(root_fn)) {
            FieldNamesMode field_names_mode = Helpers.get_field_names_mode(settings);
            String dto_inheritance = settings.getDto().getInheritance();
            String dto_package = settings.getDto().getScope();
            if (output_dir_rel_path != null) {
                String package_rel_path = SdmUtils.get_package_relative_path(settings, dto_package);
                output_dir_rel_path.append(package_rel_path);
            }
            return new JavaCG.DTO(sdm, settings, connection, dto_package,
                    sql_root_abs_path, dto_inheritance, field_names_mode, vm_template);
        } else if (Const.Root.CPP.equals(root_fn)) {
            if (output_dir_rel_path != null) {
                String package_rel_path = settings.getFolders().getTarget();
                output_dir_rel_path.append(package_rel_path);
            }
            return new CppCG.DTO(sdm, settings, connection, sql_root_abs_path, "", vm_template);
        } else if (Const.Root.PYTHON.equals(root_fn)) {
            if (output_dir_rel_path != null) {
                String package_rel_path = settings.getFolders().getTarget();
                output_dir_rel_path.append(package_rel_path);
            }
            return new PythonCG.DTO(sdm, settings, connection, sql_root_abs_path, vm_template);
        } else if (Const.Root.GO.equals(root_fn)) {
            if (output_dir_rel_path != null) {
                String package_rel_path = TargetLangUtils.get_golang_dto_folder_rel_path(settings);
                output_dir_rel_path.append(package_rel_path);
            }
            FieldNamesMode field_names_mode = Helpers.get_field_names_mode(settings);
            return new GoCG.DTO(sdm, settings, connection, sql_root_abs_path, field_names_mode, vm_template);
        } else {
            throw new Exception(TargetLangUtils.get_unknown_root_file_msg(root_fn));
        }
    }

    public static IDaoCG create_dao_cg(
            String root_fn,
            String project_abs_path,
            String xml_configs_folder_full_path,
            Connection con,
            Settings settings,
            StringBuilder output_dir_rel_path) throws Exception {

        String sql_root_abs_path = Helpers.concat_path(project_abs_path, settings.getFolders().getSql());
        String sdm_xml_abs_path = Helpers.concat_path(xml_configs_folder_full_path, Const.SDM_XML);
        String sdm_xsd_abs_path = Helpers.concat_path(xml_configs_folder_full_path, Const.SDM_XSD);
        String vm_template = TargetLangUtils.get_dao_vm_template(settings, project_abs_path);
        String context_path = Sdm.class.getPackage().getName();
        XmlParser xml_parser = new XmlParser(context_path, sdm_xsd_abs_path);
        Sdm sdm = xml_parser.unmarshal(sdm_xml_abs_path);
        List<DtoClass> dto_classes = sdm.getDtoClass();
        ////////////////////////////////////////////////////
        if (Const.Root.PHP.equals(root_fn)) {
            if (output_dir_rel_path != null) {
                String dao_package = settings.getDao().getScope();
                String package_rel_path = SdmUtils.get_package_relative_path(settings, dao_package);
                output_dir_rel_path.append(package_rel_path);
            }
            FieldNamesMode field_names_mode = Helpers.get_field_names_mode(settings);
            return new PhpCG.DAO(dto_classes, settings, con, sql_root_abs_path, vm_template, field_names_mode);
        } else if (Const.Root.JAVA.equals(root_fn)) {
            FieldNamesMode field_names_mode = Helpers.get_field_names_mode(settings);
            String dto_package = settings.getDto().getScope();
            String dao_package = settings.getDao().getScope();
            if (output_dir_rel_path != null) {
                String package_rel_path = SdmUtils.get_package_relative_path(settings, dao_package);
                output_dir_rel_path.append(package_rel_path);
            }
            return new JavaCG.DAO(dto_classes, settings, con, dto_package, dao_package,
                    sql_root_abs_path, field_names_mode, vm_template);
        } else if (Const.Root.CPP.equals(root_fn)) {
            if (output_dir_rel_path != null) {
                String package_rel_path = settings.getFolders().getTarget();
                output_dir_rel_path.append(package_rel_path);
            }
            return new CppCG.DAO(dto_classes, settings, con, sql_root_abs_path, "", vm_template);
        } else if (Const.Root.PYTHON.equals(root_fn)) {
            String package_rel_path = settings.getFolders().getTarget();
            if (output_dir_rel_path != null) {
                output_dir_rel_path.append(package_rel_path);
            }
            String dto_package = package_rel_path.replace("/", ".").replace("\\", ".");
            return new PythonCG.DAO(dto_package, dto_classes, settings, con, sql_root_abs_path, vm_template);
        } else if (Const.Root.GO.equals(root_fn)) {
            if (output_dir_rel_path != null) {
                String package_rel_path = TargetLangUtils.get_golang_dao_folder_rel_path(settings);
                output_dir_rel_path.append(package_rel_path);
            }
            FieldNamesMode field_names_mode = Helpers.get_field_names_mode(settings);
            return new GoCG.DAO(dto_classes, settings, con, sql_root_abs_path, field_names_mode, vm_template);
        } else {
            throw new Exception(TargetLangUtils.get_unknown_root_file_msg(root_fn));
        }
    }
}