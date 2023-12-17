/*
    Copyright 2011-2023 sqldalmaker@gmail.com
    SQL DAL Maker Website: http://sqldalmaker.sourceforge.net
    Read LICENSE.txt in the root of this project/archive for details.
 */
package com.sqldalmaker.netbeans;

import com.sqldalmaker.cg.Helpers;
import com.sqldalmaker.common.Const;
import com.sqldalmaker.common.InternalException;
import com.sqldalmaker.common.SdmUtils;
import com.sqldalmaker.jaxb.sdm.DtoClass;
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
import org.openide.modules.ModuleInfo;
import org.openide.modules.Modules;

/**
 *
 * @author sqldalmaker@gmail.com
 *
 */
public class NbpHelpers {

    public static Settings load_settings(SdmDataObject obj) throws Exception {
        return load_settings(obj.getPrimaryFile());
    }

    public static Settings load_settings(FileObject meta_program_file) throws Exception {
        FileObject folder = meta_program_file.getParent();
        String folder_abs_path = folder.getPath();
        return SdmUtils.load_settings(folder_abs_path);
    }

    public static List<DtoClass> get_dto_classes(SdmDataObject obj) throws Exception {
        FileObject folder = obj.getPrimaryFile().getParent();
        return get_dto_classes(folder);
    }

    public static List<DtoClass> get_dto_classes(FileObject folder) throws Exception {
        List<DtoClass> res = new ArrayList<DtoClass>();
        String folder_abs_path = folder.getPath();
        String sdm_xsd_abs_path = folder_abs_path + "/" + Const.SDM_XSD;
        String sdm_xml_abs_path = folder_abs_path + "/" + Const.SDM_XML;
        List<DtoClass> list = SdmUtils.get_dto_classes(sdm_xml_abs_path, sdm_xsd_abs_path);
        res.addAll(list);
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
        // https://stackoverflow.com/questions/46393863/what-to-use-instead-of-class-newinstance
        Driver driver = (Driver) cl.getConstructor().newInstance();
        Properties props = new Properties();
        if (user_name != null) {
            props.put("user", user_name);
            props.put("password", password);
        }
        Connection con = driver.connect(url, props);
        // connect javadocs:
        // The driver should return "null" if it realizes it is the
        // wrong kind of driver to connect to the given URL.
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
            FileLock lock = file.lock();
            try {
                // ObjectOutputStream os = new ObjectOutputStream(file.getOutputStream(lock));
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

    static String get_sdm_info(Class<?> clazz) {
        ModuleInfo m = Modules.getDefault().ownerOf(clazz);
        String v = m.getSpecificationVersion().toString();
        String jv = System.getProperty("java.version");
        return v + " on Java " + jv;
    }
}
