package com.sqldalmaker.common;

import com.sqldalmaker.cg.Helpers;

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
}
