package com.sqldalmaker.common;

import com.sqldalmaker.cg.Helpers;
import com.sqldalmaker.cg.Xml2Vm;
import com.sqldalmaker.jaxb.settings.Macros;
import com.sqldalmaker.jaxb.settings.Settings;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class TargetLangUtils {

    public static String file_name_from_class_name(String root_fn, String class_name) throws Exception {
        int model_name_end_index = class_name.indexOf('-');
        if (model_name_end_index != -1) {
            class_name = class_name.substring(model_name_end_index + 1);
        }
        if (RootFileName.PHP.equals(root_fn)) {
            return class_name + ".php";
        } else if (RootFileName.JAVA.equals(root_fn)) {
            return class_name + ".java";
        } else if (RootFileName.CPP.equals(root_fn)) {
            return class_name + ".h";
        } else if (RootFileName.RUBY.equals(root_fn)) {
            return Helpers.convert_file_name_to_snake_case(class_name, "rb");
        } else if (RootFileName.PYTHON.equals(root_fn)) {
            return Helpers.convert_file_name_to_snake_case(class_name, "py");
        } else if (RootFileName.GO.equals(root_fn)) {
            return Helpers.convert_file_name_to_snake_case(class_name, "go");
        }
        throw new Exception(get_unknown_root_file_msg(root_fn));
    }

    public static String get_unknown_root_file_msg(String fn) {
        return "Unknown root file: " + fn;
    }

    public static String get_target_file_path(String root_fn, String output_dir, String class_name) throws Exception {
        String file_name = file_name_from_class_name(root_fn, class_name);
        return Helpers.concat_path(output_dir, file_name);
    }

    public static boolean snake_case_needed(String fn) {
        if (RootFileName.RUBY.equals(fn) || RootFileName.PYTHON.equals(fn)) {
            return true;
        }
        return false;
    }

    public static boolean lower_camel_case_needed(String fn) {
        if (RootFileName.GO.equals(fn)) {
            return true;
        }
        return false;
    }

    public static String get_target_folder_abs_path(String class_scope,
                                                    String root_file_fn,
                                                    String target_folder_rel_path,
                                                    String module_root) {
        String res;
        if (RootFileName.JAVA.equals(root_file_fn) || RootFileName.PHP.equals(root_file_fn)) {
            class_scope = class_scope.replace('.', '/').replace('\\', '/');
            res = Helpers.concat_path(module_root, target_folder_rel_path, class_scope);
        } else {
            res = Helpers.concat_path(module_root, target_folder_rel_path);
        }
        return res;
    }

    private static final String SCOPES_ERR = "Error in 'settings.xml': \n" +
            "the scopes of DTO and DAO must both be empty or both be fully qualified like in Go 'import' section";

    public static String get_golang_dto_folder_rel_path(Settings settings) throws Exception {
        String dto_scope = settings.getDto().getScope().replace("\\", "/");
        String dao_scope = settings.getDao().getScope().replace("\\", "/");
        String package_rel_path;
        if (dto_scope.length() == 0) {
            if (dao_scope.length() != 0) {
                throw new Exception(SCOPES_ERR);
            }
            package_rel_path = settings.getFolders().getTarget().replace("\\", "/");
        } else {
            Path p = Paths.get(dto_scope);
            String dto_scope_last_segment = p.getFileName().toString();
            if (dao_scope.length() == 0 || dto_scope_last_segment.equals(dto_scope)) { // just package name
                throw new Exception(SCOPES_ERR);
            } else {
                String[] dto_scope_parts = dto_scope.split("/");
                String path_after_root_module = dto_scope.substring(dto_scope_parts[0].length() + 1);
                package_rel_path = path_after_root_module; // just ignore target folder
            }
        }
        return package_rel_path;
    }

    public static String get_golang_dao_folder_rel_path(Settings settings) throws Exception {
        String dao_scope = settings.getDao().getScope().replace("\\", "/");
        String dto_scope = settings.getDto().getScope().replace("\\", "/");
        String package_rel_path;
        if (dao_scope.length() == 0) {
            if (dto_scope.length() != 0) {
                throw new Exception(SCOPES_ERR);
            }
            package_rel_path = settings.getFolders().getTarget().replace("\\", "/");
        } else {
            Path p = Paths.get(dao_scope);
            String dao_scope_last_segment = p.getFileName().toString();
            if (dto_scope.length() == 0 || dao_scope_last_segment.equals(dao_scope)) { // just package name
                throw new Exception(SCOPES_ERR);
            } else {
                String[] dao_scope_parts = dao_scope.split("/");
                String path_after_root_module = dao_scope.substring(dao_scope_parts[0].length() + 1);
                package_rel_path = path_after_root_module; // just ignore target folder
            }
        }
        return package_rel_path;
    }

    public static String get_dto_vm_template(Settings settings,
                                              String project_abs_path) throws Exception {

        String macro_name = settings.getDto().getMacro();
        return get_vm_template(macro_name, settings, project_abs_path);
    }

    public static String get_dao_vm_template(Settings settings,
                                              String project_abs_path) throws Exception {

        String macro_name = settings.getDao().getMacro();
        return get_vm_template(macro_name, settings, project_abs_path);
    }

    private static String get_vm_template(String macro_name,
                                          Settings settings,
                                          String project_abs_path) throws Exception {
        // read the file or find the macro
        if (macro_name == null || macro_name.trim().length() == 0) {
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
}
