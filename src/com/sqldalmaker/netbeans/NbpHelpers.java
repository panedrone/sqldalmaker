/*
 * Copyright 2011-2018 sqldalmaker@gmail.com
 * SQL DAL Maker Website: http://sqldalmaker.sourceforge.net
 * Read LICENSE.txt in the root of this project/archive for details.
 */
package com.sqldalmaker.netbeans;

import com.sqldalmaker.cg.Helpers;
import com.sqldalmaker.common.Const;
import com.sqldalmaker.common.EnglishNoun;
import com.sqldalmaker.common.InternalException;
import com.sqldalmaker.common.XmlParser;
import com.sqldalmaker.jaxb.dto.DtoClass;
import com.sqldalmaker.jaxb.dto.DtoClasses;
import com.sqldalmaker.jaxb.settings.Settings;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.Charset;
import java.sql.Connection;
import java.sql.Driver;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import org.openide.filesystems.FileLock;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;

/**
 *
 * @author sqldalmaker@gmail.com
 *
 */
public class NbpHelpers {

    private static Settings load_settings(String folder_abs_path) throws Exception {

        String config_xsd_abs_path = folder_abs_path + "/" + Const.SETTINGS_XSD;

        String context_path = Settings.class.getPackage().getName();

        XmlParser config_xml_parser = new XmlParser(context_path, config_xsd_abs_path);

        String config_xml_abs_path = folder_abs_path + "/" + Const.SETTINGS_XML;

        Settings res = config_xml_parser.unmarshal(config_xml_abs_path);

        return res;
    }

    public static Settings load_settings(SdmDataObject obj) throws Exception {

        return load_settings(obj.getPrimaryFile());
    }

    public static Settings load_settings(FileObject meta_program_file) throws Exception {

        FileObject folder = meta_program_file.getParent();

        String folder_abs_path = folder.getPath();

        return NbpHelpers.load_settings(folder_abs_path);
    }

    public static List<DtoClass> get_dto_classes(SdmDataObject obj) throws Exception {

        FileObject folder = obj.getPrimaryFile().getParent();

        return get_dto_classes(folder);
    }

    public static List<DtoClass> get_dto_classes(FileObject folder) throws Exception {

        List<DtoClass> res = new ArrayList<DtoClass>();

        String folder_abs_path = folder.getPath();

        String dto_xsd_abs_file_path = folder_abs_path + "/" + Const.DTO_XSD;

        String context_path = DtoClasses.class.getPackage().getName();

        XmlParser xml_parser = new XmlParser(context_path, dto_xsd_abs_file_path);

        String dto_xml_abs_file_path = folder_abs_path + "/" + Const.DTO_XML;

        DtoClasses elements = xml_parser.unmarshal(dto_xml_abs_file_path);

        for (DtoClass cls : elements.getDtoClass()) {

            res.add(cls);
        }

        return res;
    }

    public static Connection get_connection(SdmDataObject obj) throws Exception {

        Settings sett = NbpHelpers.load_settings(obj);

        String driver_jar = sett.getJdbc().getJar();

        String driver_class_name = sett.getJdbc().getClazz();

        String url = sett.getJdbc().getUrl();

        String project_abs_path = NbpPathHelpers.get_root_folder(obj.getPrimaryFile()).getPath();

        url = url.replace("$PROJECT_DIR$", project_abs_path);

        url = url.replace("${project_loc}", project_abs_path);

        String user_name = sett.getJdbc().getUser();

        String password = sett.getJdbc().getPwd();

        FileObject res = null;

        if (driver_jar != null && !"".equals(driver_jar)) {

            FileObject obj_file = obj.getPrimaryFile();

            FileObject root = NbpPathHelpers.get_root_folder(obj_file);

            // * @return the object representing this file or <CODE>null</CODE> if the file
            // *   or folder does not exist
            //
            res = root.getFileObject(driver_jar);

            if (res != null) {

                driver_jar = res.getPath();
            }
        }

        if (res == null) {

            throw new Exception("JDBC driver JAR file not found: " + driver_jar);
        }

        Class<?> cl;

        if (!"".equals(driver_jar)) {

            ClassLoader loader = new URLClassLoader(new URL[]{new File(
                driver_jar).toURI().toURL()});

            cl = Class.forName(driver_class_name, true, loader);

        } else {

            cl = Class.forName(driver_class_name);
        }

        // Driver driver = (Driver) cl.newInstance();
        Driver driver = (Driver) cl.getConstructor().newInstance(); // https://github.com/google/error-prone/issues/407

        Properties props = new Properties();

        if (user_name != null) {

            props.put("user", user_name);

            props.put("password", password);
        }

        Connection con = driver.connect(url, props);

        // connect javadocs:
        // The driver should return "null" if it realizes it is the
        // wrong kind of driver to connect to the given URL.
        //
        if (con == null) {

            throw new InternalException("JDBC Connection failed");
        }

        return con;
    }

    // http://www.devdaily.com/blog/post/java/read-text-file-from-jar-file
    //
    public static String read_from_jar_file(String res_name) throws Exception {

        return read_from_jar_file("resources", res_name);
    }

    public static String read_from_jar_file(String path, String res_name) throws Exception {

        InputStream is = get_resource_as_stream(path, res_name);

        try {

            InputStreamReader reader = new InputStreamReader(is);

            try {

                return Helpers.load_text(reader);

            } finally {

                reader.close();
            }

        } finally {

            is.close();
        }
    }

    private static InputStream get_resource_as_stream(String path, String res_name)
            throws InternalException {

        ClassLoader cl = NbpHelpers.class.getClassLoader();

        // no need in starting '/' for NetBeans:
        //
        InputStream is = cl.getResourceAsStream(path + "/" + res_name);

        if (is == null) {

            throw new InternalException("Resource not found: " + res_name);
        }

        return is;
    }

    public static void write_file_content(FileObject file, String file_content) throws IOException {

        InputStream is = new ByteArrayInputStream(file_content.getBytes(Charset.defaultCharset()));

        try {

            // https://platform.netbeans.org/tutorials/nbm-feedreader.html
            //
            FileLock lock = file.lock();

            try {

                // ObjectOutputStream os = new ObjectOutputStream(file.getOutputStream(lock));
                //
                OutputStream os = file.getOutputStream(lock);

                try {

                    FileUtil.copy(is, os);

                } finally {

                    os.close();
                }

            } finally {

                lock.releaseLock();
            }

        } finally {

            is.close();
        }
    }

    public static void save_text_to_file(SdmDataObject obj, String output_dir_rel_path, String file_name, String file_content) throws Exception {

        FileObject root_file = obj.getPrimaryFile();

        FileObject root = NbpPathHelpers.get_root_folder(root_file);

        FileObject file = root.getFileObject(output_dir_rel_path + "/" + file_name);

        if (file != null) {

            write_file_content(file, file_content);

            return;
        }

        FileObject folder = root.getFileObject(output_dir_rel_path);

        if (folder == null) {

            File dir = new File(root.getPath() + "/" + output_dir_rel_path);

            folder = FileUtil.createFolder(dir);

        } else if (folder.isFolder() == false) {

            throw new IOException(output_dir_rel_path + " is not folder");
        }

        file = folder.createData(file_name);

        write_file_content(file, file_content);
    }

    public static String table_name_to_dto_class_name(String table_name, boolean plural_to_singular) {

        String word = to_camel_case(table_name);

        if (plural_to_singular) {

            int last_word_index = -1;

            String last_word;

            for (int i = word.length() - 1; i >= 0; i--) {

                if (Character.isUpperCase(word.charAt(i))) {

                    last_word_index = i;

                    break;
                }
            }

            last_word = word.substring(last_word_index);

            last_word = EnglishNoun.singularOf(last_word); // makes lowercase

            StringBuilder sb = new StringBuilder();

            sb.append(Character.toUpperCase(last_word.charAt(0)));

            if (last_word.length() > 1) {

                sb.append(last_word.substring(1, last_word.length()).toLowerCase());
            }

            last_word = sb.toString();

            if (last_word_index == 0) {

                word = last_word;

            } else {

                word = word.substring(0, last_word_index);

                word = word + last_word;
            }
        }

        return word;
    }

    private static String to_camel_case(String str) {

        if (!str.contains("_")) {

            boolean all_is_upper_case = Helpers.is_upper_case(str);

            if (all_is_upper_case) {

                str = str.toLowerCase();
            }

            return Helpers.replace_char_at(str, 0, Character.toUpperCase(str.charAt(0)));
        }

        // http://stackoverflow.com/questions/1143951/what-is-the-simplest-way-to-convert-a-java-string-from-all-caps-words-separated
        //
        StringBuilder sb = new StringBuilder();

        String[] arr = str.split("_");

        for (String s : arr) {

            if (s.length() == 0) {

                continue; // E.g. _ALL_FILE_GROUPS
            }

            // if (i == 0) {
            //
            // sb.append(s.toLowerCase());
            //
            // } else {
            sb.append(Character.toUpperCase(s.charAt(0)));

            if (s.length() > 1) {

                sb.append(s.substring(1, s.length()).toLowerCase());
            }
        }
        // }

        return sb.toString();
    }
}
