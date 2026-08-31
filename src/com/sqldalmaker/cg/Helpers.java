/*
    Copyright 2011-2026 sqldalmaker@gmail.com
    SQL DAL Maker Website: https://sqldalmaker.sourceforge.net/
    Read LICENSE.txt in the root of this project/archive for details.
 */
package com.sqldalmaker.cg;

import com.sqldalmaker.common.Const;
import com.sqldalmaker.jaxb.settings.Settings;

import java.io.*;

/*
 * 30.08.2026 14:00 1.331 Claude refactor
 * 30.05.2024 12.26 1.299
 * 12.05.2023 12:04
 * 12.05.2023 11:50
 * 07.05.2023 15:37
 * 16.11.2022 08:02 1.269
 * 25.10.2022 09:26
 * 21.04.2022 17:15 1.225
 * 16.04.2022 17:35
 * 17.05.2021 11:28
 * 08.05.2021 22:29 1.200
 * 17.04.2021 20:16
 * 22.03.2021 21:19
 * 05.03.2021 00:35
 * 15.05.2020 19:11
 * 02.01.2020 07:21
 * 03.09.2019 15:55
 * 07.02.2019 19:50 initial commit
 */
public class Helpers {

    // the same "internal template or the one from settings.xml" choice in every target language
    public static TemplateEngine create_template_engine(
            String vm_template,
            String vm_sub_folder,
            String vm_template_name) throws Exception {

        if (vm_template == null) {
            String vm_path = Helpers.class.getPackage().getName().replace('.', '/')
                    + "/" + vm_sub_folder + "/" + vm_sub_folder + ".vm";
            return new TemplateEngine(vm_path, false);
        }
        return new TemplateEngine(vm_template, vm_template_name);
    }

    public static String concat_path(String seg0, String seg1) {
        return seg0 + "/" + seg1;
    }

    public static String concat_path(String seg0, String seg1, String seg2) {
        String res = concat_path(seg0, seg1);
        return concat_path(res, seg2);
    }

    public static FieldNamesMode get_field_names_mode(Settings settings) {
        FieldNamesMode field_names_mode;
        int fnm = settings.getDto().getFieldNamesMode();
        if (fnm == 0) {
            field_names_mode = FieldNamesMode.TITLE_CASE;
        } else if (fnm == 1) {
            field_names_mode = FieldNamesMode.LOWER_CAMEL_CASE;
        } else {
            field_names_mode = FieldNamesMode.SNAKE_CASE;
        }
        return field_names_mode;
    }

    // Java File exists Case sensitive
    // https://stackoverflow.com/questions/34603505/java-file-exists-case-sensitive-jpg-and-jpg

    public static boolean exists(File dir, String filename) {
        if (!dir.isDirectory()) {
            return false; // target files were not generated yet
        }
        String[] files = dir.list();
        if (files == null) {
            return false; // it may be null in jdk 16
        }
        for (String file : files) {
            if (file.equals(filename)) {
                return true;
            }
        }
        return false;
    }

    // http://www.java2s.com/Tutorial/Java/0180__File/LoadatextfilecontentsasaString.htm

    public static String load_text_from_file(String file_path) throws Exception {
        File file = new File(file_path);
        File dir = new File(file.getParent());
        String file_name = file.getName();
        if (!exists(dir, file_name)) {
            throw new IOException("File not found (case-sensitive): " + file_path);
        }
        FileReader reader = new FileReader(file);
        try {
            return load_text(reader);
        } finally {
            reader.close();
        }
    }

    public static String load_text(InputStreamReader reader) throws IOException {
        int len;
        char[] chr = new char[4096];
        StringBuilder buffer = new StringBuilder();
        while ((len = reader.read(chr)) > 0) {
            buffer.append(chr, 0, len);
        }
        return buffer.toString();
    }

    // public for eclipse
    public static InputStream res_as_stream(String res_path) throws Exception {
        // swing app wants 'resources/' but plug-in wants '/resources/' WHY?
        ClassLoader cl = Helpers.class.getClassLoader();
        InputStream is = cl.getResourceAsStream(res_path);
        if (is == null) {
            is = cl.getResourceAsStream("/" + res_path);
        }
        if (is == null) {
            throw new Exception("Resource not found: " + res_path);
        }
        return is;
    }

    public static String res_from_jar(String res_name) throws Exception {
        // http://www.devdaily.com/blog/post/java/read-text-file-from-jar-file
        InputStream is = res_as_stream(res_name);
        try {
            InputStreamReader reader = new InputStreamReader(is);
            try {
                return load_text(reader);
            } finally {
                reader.close();
            }
        } finally {
            is.close();
        }
    }

    public static StringBuilder get_only_pk_warning(String method_name) {
        // if all values of the table are the parts of PK,
        // SQL will be invalid like "UPDATE term_groups SET WHERE g_id"
        // = ? AND t_id = ?'
        // (missing assignments between SET and WHERE)
        String msg = "    // INFO: " + method_name + " is not rendered because all columns are part of PK.";
        StringBuilder buffer = new StringBuilder();
        _build_warning_comment(buffer, msg);
        return buffer;
    }

    public static StringBuilder get_no_pk_warning(String method_name) {
        String msg = "    // INFO: " + method_name + " is not rendered because PK is not detected.";
        StringBuilder buffer = new StringBuilder();
        _build_warning_comment(buffer, msg);
        return buffer;
    }

    private static void _build_warning_comment(StringBuilder buffer, String msg) {
        String ls = System.lineSeparator();
        buffer.append(ls);
        buffer.append(msg);
        buffer.append(ls);
    }

    public static String get_error_message(String msg, Throwable e) {
        return msg + " " + e.getMessage();
    }


    public static boolean is_sdm_xml(String name) {
        return name != null && name.equals(Const.SDM_XML);
    }

    public static boolean is_dao_xml(String name) {
        if (name == null) {
            return false;
        }
        if (is_sdm_xml(name)) {
            return false;
        }
        if (is_setting_xml(name)) {
            return false;
        }
        return name.endsWith(".xml");
    }

    public static boolean is_setting_xml(String name) {
        return Const.SETTINGS_XML.equals(name);
    }

    public interface IFileList {
        void add(String fileName);
    }

    // still used in eclipse
    public static void enum_dao_xml_file_names(String sdm_xml_folder_abs_path, IFileList file_list) {
        File dir = new File(sdm_xml_folder_abs_path);
        String[] children = dir.list();
        if (children != null) {
            for (String fileName : children) {
                if (Helpers.is_dao_xml(fileName)) {
                    file_list.add(fileName);
                }
            }
        }
    }

    public static String remove_cr_and_lf(String text) {
        return text.replace("\r", "\n").replace("\n\n", "\n").trim();
    }

    public static boolean equal_ignoring_eol(String text1, String text2) {
        text1 = remove_cr_and_lf(text1);
        text2 = remove_cr_and_lf(text2);
        return text1.equals(text2);
    }

    public static boolean equal_ignoring_eol(String text, String old_text, StringBuilder err_buff) {
        if (old_text.isEmpty()) {
            err_buff.append(Const.OUTPUT_FILE_IS_MISSING);
            return false;
        }
        if (Helpers.equal_ignoring_eol(text, old_text)) {
            return true;
        }
        err_buff.append(Const.OUTPUT_FILE_IS_OUT_OF_DATE);
        return false;
    }
}
