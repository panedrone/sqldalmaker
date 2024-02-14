/*
    Copyright 2011-2024 sqldalmaker@gmail.com
    SQL DAL Maker Website: https://sqldalmaker.sourceforge.net/
    Read LICENSE.txt in the root of this project/archive for details.
 */
package com.sqldalmaker.intellij.ui;

import com.intellij.history.LocalHistory;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.NavigatableFileEditor;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.pom.Navigatable;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.sqldalmaker.cg.Helpers;
import com.sqldalmaker.common.Const;
import com.sqldalmaker.common.InternalException;
import com.sqldalmaker.common.SdmUtils;
import com.sqldalmaker.intellij.references.IdeaRefUtils;
import com.sqldalmaker.jaxb.sdm.DaoClass;
import com.sqldalmaker.jaxb.settings.Settings;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.Connection;
import java.sql.Driver;
import java.util.List;
import java.util.Properties;

/**
 * Created with IntelliJ IDEA.
 * User: sqldalmaker@gmail.com
 * Date: 21.06.12
 */
public class IdeaHelpers {

    @NotNull // or exception
    public static VirtualFile get_project_base_dir(final Project project) throws Exception {
        // VirtualFile res = project.getBaseDir(); // deprecated
        String project_path = project.getBasePath();
        if (project_path == null) {
            throw new Exception("Cannot detect the project base path");
        }
        /*
//        boolean found = false;
//        String content_root_list = "";
//        // https://jetbrains.org/intellij/sdk/docs/reference_guide/project_model/project.html
//        List<String> content_roots = ProjectRootManager.getInstance(project).getContentRootUrls();
//        for (String content_root_path : content_roots) {
//            if (content_root_path.endsWith(project_path)) {
//                found = true;
//            }
//            content_root_list += "[" + content_root_path + "]";
//        }
//        if (!found) {
//            throw new Exception("Something is wrong with project structure.\r\nProject base path '" + project_path + "' not found among content roots:\r\n" + content_root_list);
//        }
         */
        File file = new File(project_path);
        // https://intellij-support.jetbrains.com/hc/en-us/community/posts/360001957360-Get-file-from-path-to-have-a-virtual-file
        VirtualFile res = VfsUtil.findFileByIoFile(file, true);
        if (res == null) {
            throw new Exception("Cannot find project directory in virtual file system");
        }
        return res;
    }

    public static String get_relative_path(Project project, VirtualFile file) throws Exception {
        String base_abs_path = project.getBasePath();
        if (base_abs_path == null) {
            throw new Exception("Cannot detect the project base path");
        }
        String file_abs_path = file.getPath();
        return get_relative_path(base_abs_path, file_abs_path);
    }

    public static String get_relative_path(Project project, String file_abs_path) throws Exception {
        String base_abs_path = project.getBasePath();
        if (base_abs_path == null) {
            throw new Exception("Cannot detect the project base path");
        }
        return get_relative_path(base_abs_path, file_abs_path);
    }

    public static String get_relative_path(String base_abs_path, String file_abs_path) {
        // http://stackoverflow.com/questions/204784/how-to-construct-a-relative-path-in-java-from-two-absolute-paths-or-urls
        String base = base_abs_path.replace('\\', '/');
        String path = file_abs_path.replace('\\', '/');
        URI base_uri = new File(base).toURI();
        URI path_uri = new File(path).toURI();
        String relative = base_uri.relativize(path_uri).getPath();
        // bug: sometimes, relative is returned as /C:/idea/project/java.sqldalmaker
        if (relative.length() > base.length()) {
            relative = path.substring(base.length() + 1);
        }
        return relative;
    }

    public static Settings load_settings(VirtualFile root_file) throws Exception {
        String sdm_folder_full_path = root_file.getParent().getPath();
        return SdmUtils.load_settings(sdm_folder_full_path);
    }

    /**
     * Must be called only from DispatchThread
     */
    public static void start_write_action_from_ui_thread_and_refresh_folder_sync(VirtualFile folder) {
        start_write_action_from_ui_thread_and_refresh_folder(folder);
    }

    /**
     * Must be called only from DispatchThread
     */
    private static void start_write_action_from_ui_thread_and_refresh_folder(final VirtualFile folder) {
        // com.intellij.openapi.vfs.VirtualFile
        // public void refresh(boolean asynchronous, boolean recursive) ...
        // This method should be only called within write-action.
        ApplicationManager.getApplication().runWriteAction(new Runnable() {
            @Override
            public void run() {
                try {
                    folder.refresh(/*asynchronous*/ false, /*recursive*/ true);
                } catch (Throwable e) {
                    // e.printStackTrace();
                    throw new RuntimeException(e);
                }
            }
        });
    }

    // http://www.devdaily.com/blog/post/java/read-text-file-from-jar-file
    public static String read_from_jar_resources(String res_name) throws Exception {
        return read_from_jar("resources", res_name);
    }

    public static String read_from_jar(String path, String res_name) throws Exception {
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

    private static InputStream get_resource_as_stream(String path, String res_name) throws InternalException {
        // swing app wants 'resources/' but plug-in wants '/resources/' WHY?
        ClassLoader cl = IdeaHelpers.class.getClassLoader();
        InputStream is;
        if (path == null || path.trim().isEmpty()) {
            // to avoid Warning
            // Do not request resource from classloader using path with leading slash
            is = cl.getResourceAsStream(res_name);
        } else {
            is = cl.getResourceAsStream(path + "/" + res_name);
        }
        if (is == null) {
            is = cl.getResourceAsStream("/" + path + "/" + res_name);
        }
        if (is == null) {
            throw new InternalException("Resource not found: " + res_name);
        }
        return is;
    }

    public static Connection get_connection(Project project, Settings settings) throws Exception {
        String driver_jar = settings.getJdbc().getJar();
        String driver_class_name = settings.getJdbc().getClazz();
        String url = settings.getJdbc().getUrl();
        String user_name = settings.getJdbc().getUser();
        String password = settings.getJdbc().getPwd();
        return get_connection(project, driver_jar, driver_class_name, url, user_name, password);
    }

    public static Connection get_connection(
            Project project,
            String driver_jar,
            String driver_class_name, String url,
            String user_name, String password) throws Exception {

        VirtualFile project_dir = get_project_base_dir(project);
        String project_abs_path = project_dir.getPath();
        if (url.contains("${project_loc}")) {
            url = url.replace("${project_loc}", project_abs_path); // for compatibility with Eclipse
        } else if (url.contains("$PROJECT_DIR$")) {
            url = url.replace("$PROJECT_DIR$", project_abs_path); // for compatibility with Eclipse
        }
        VirtualFile driver_file = project_dir.findFileByRelativePath(driver_jar);
        if (driver_file == null) {
            File base_directory = new File(project_dir.getPath());
            File jar_path = new File(base_directory, driver_jar);
            throw new Exception("Cannot find '" + jar_path + "'");
        }
        driver_jar = driver_file.getPath();
        Class<?> cl;
        if (!driver_jar.isEmpty()) {
            ClassLoader loader = new URLClassLoader(new URL[]{new File(
                    driver_jar).toURI().toURL()});
            cl = Class.forName(driver_class_name, true, loader);
        } else {
            cl = Class.forName(driver_class_name);
        }
        // https://stackoverflow.com/questions/46393863/what-to-use-instead-of-class-newinstance
        Driver driver = (Driver) cl.getDeclaredConstructor().newInstance();
        Connection con;
        Properties props = new Properties();
        if (user_name != null) {
            props.put("user", user_name);
            props.put("password", password);
        }
        con = driver.connect(url, props);
        // connect javadocs:
        // The driver should return "null" if it realizes it is the
        // wrong kind of driver to connect to the given URL.
        if (con == null) {
            throw new InternalException("Invalid URL");
        }
        return con;
    }

    public static String get_sdm_info() {
        String plugin_version = "1.289+";
        try {
            String plugin_xml = IdeaHelpers.read_from_jar("", "plugin.xml");
            String[] parts = plugin_xml.split("<version>");
            plugin_version = parts[1].split("</version>")[0];
        } catch (Throwable e) {
            //
        }
        String jv = System.getProperty("java.version");
        return plugin_version + " on Java " + jv;
    }

    public static class GeneratedFileData {
        String file_name;
        String file_content;
    }

    private static class GenerateSourceFileWriteAction implements Runnable {

        private Throwable error;
        private List<GeneratedFileData> generated_file_data_list;
        private Project project;
        private String output_dir_module_relative_path;

        private static void writeFile(VirtualFile dir, GeneratedFileData gf) throws Exception {
            // findFileByRelativePath is case-insensitive
            VirtualFile file = dir.findFileByRelativePath(gf.file_name);
            if (file == null) {
                // createChildData may show dialog and throw
                // java.lang.IllegalStateException: The DialogWrapper can be used only on event dispatch thread.
                // if it is called outside of WriteAction
                file = dir.createChildData(null, gf.file_name);
            } else {
                // rename if the case differs
                // file.getName() is @NotNull
                if (!file.getName().equals(gf.file_name)) {
                    // https://www.programcreek.com/java-api-examples/?class=com.intellij.openapi.vfs.VirtualFile&method=delete
                    file.rename(LocalHistory.VFS_EVENT_REQUESTOR, gf.file_name);
                }
            }
            file.setBinaryContent(gf.file_content.getBytes());
        }

        @Override
        public void run() {
            try {
                VirtualFile project_dir = IdeaHelpers.get_project_base_dir(project);
                VirtualFile output_dir = project_dir.findFileByRelativePath(output_dir_module_relative_path);
                if (output_dir != null && !output_dir.isDirectory()) {
                    throw new Exception("This is not a dir: " + output_dir_module_relative_path);
                }
                output_dir = ensure_dir(project_dir, output_dir_module_relative_path);
                for (GeneratedFileData gf : generated_file_data_list) {
                    writeFile(output_dir, gf);
                }
                error = null;
            } catch (Throwable e) {
                error = e;
            }
        }
    }

    public static void run_write_action_to_generate_source_file(
            String output_dir_module_relative_path,
            List<GeneratedFileData> generated_file_data_list,
            Project project) throws Exception {

        GenerateSourceFileWriteAction write_action = new GenerateSourceFileWriteAction();
        write_action.generated_file_data_list = generated_file_data_list;
        write_action.project = project;
        write_action.output_dir_module_relative_path = output_dir_module_relative_path;
        ApplicationManager.getApplication().runWriteAction(write_action);
        // writeAction.run();
        if (write_action.error != null) {
            throw new Exception(write_action.error);
        }
    }

    private static VirtualFile ensure_dir(VirtualFile root_dir, String rel_path) throws IOException {
        rel_path = rel_path.replace('\\', '/');
        VirtualFile virtual_dir = root_dir.findFileByRelativePath(rel_path);
        if (virtual_dir == null) {
            String[] parts = rel_path.split("/");
            VirtualFile tmp = root_dir;
            for (String p : parts) {
                // createChildDirectory may show dialog and throw
                // java.lang.IllegalStateException: The DialogWrapper can be used only on event dispatch thread.
                // if it is called outside WriteAction
                VirtualFile dir = tmp.findFileByRelativePath(p);
                if (dir == null) {
                    tmp = tmp.createChildDirectory(null, p);
                } else {
                    tmp = dir;
                }
            }
            virtual_dir = tmp;
        }
        return virtual_dir;
    }

    public static void run_write_action_to_save_text_file(VirtualFile root_file, String file_name, String text) throws IOException {
        class Error {
            public Throwable exception = null;
        }
        Error error = new Error();
        Runnable writeAction = new Runnable() {
            @Override
            public void run() {
                try {
                    VirtualFile parent_dir = root_file.getParent();
                    VirtualFile file = parent_dir.findFileByRelativePath(file_name);
                    if (file == null) {
                        // createChildData may show dialog and throw
                        // java.lang.IllegalStateException: The DialogWrapper can be used only on event dispatch thread.
                        // if it is called outside WriteAction
                        file = parent_dir.createChildData(null, file_name);
                    }
                    file.setBinaryContent(text.getBytes());
                    parent_dir.refresh(/*asynchronous*/ false, /*recursive*/ true);
                } catch (Throwable e) {
                    error.exception = e;
                }
            }
        };
        ApplicationManager.getApplication().runWriteAction(writeAction);
        if (error.exception != null) {
            throw new IOException(error.exception);
        }
    }

    // thanks to https://plugins.jetbrains.com/plugin/3202?pr=
    private static void navigate_to_source(Project project, PsiElement psi_element) {
        PsiFile containing_file = psi_element.getContainingFile();
        // VirtualFile virtual_file = containingFile.find_virtual_file();
        VirtualFile virtual_file = IdeaRefUtils.find_virtual_file(containing_file);
        if (virtual_file != null) {
            FileEditorManager manager = FileEditorManager.getInstance(project);
            FileEditor[] file_editors = manager.openFile(virtual_file, true);
            if (file_editors.length > 0) {
                FileEditor file_editor = file_editors[0];
                if (file_editor instanceof NavigatableFileEditor) {
                    NavigatableFileEditor navigatableFileEditor = (NavigatableFileEditor) file_editor;
                    Navigatable descriptor = new OpenFileDescriptor(project, virtual_file, psi_element.getTextOffset());
                    navigatableFileEditor.navigateTo(descriptor);
                }
            }
        }
    }

    public static void navigate_to_dto_class_declaration(
            Project project,
            VirtualFile root_file,
            String dto_class_name) throws Exception {

        VirtualFile xml_file_dir = root_file.getParent();
        if (xml_file_dir == null) {
            throw new Exception("Cannot get parent folder for " + root_file.getName());
        }
        VirtualFile sdm_xml_file = xml_file_dir.findFileByRelativePath(Const.SDM_XML);
        if (sdm_xml_file == null) {
            throw new Exception(Const.SDM_XML + " not found");
        }
        PsiElement psi_element = IdeaRefUtils.find_dto_class_xml_tag(project, sdm_xml_file, dto_class_name);
        if (psi_element == null) {
            throw new Exception(dto_class_name + ": declaration not found");
        }
        navigate_to_source(project, psi_element);
    }

    public static boolean navigate_to_dao_class_declaration(
            Project project,
            VirtualFile root_file,
            String dao_class_name) {

        VirtualFile xml_file_dir = root_file.getParent();
        if (xml_file_dir == null) {
            return false;
        }
        VirtualFile sdm_xml_file = xml_file_dir.findFileByRelativePath(Const.SDM_XML);
        if (sdm_xml_file == null) {
            return false;
        }
        PsiElement psi_element = IdeaRefUtils.find_dao_class_xml_tag(project, sdm_xml_file, dao_class_name);
        if (psi_element == null) {
            return false;
        }
        navigate_to_source(project, psi_element);
        return true;
    }

    public static List<DaoClass> load_all_sdm_dao_classes(VirtualFile root_file) throws Exception {
        String sdm_folder_abs_path = root_file.getParent().getPath();
        String sdm_xml_abs_path = Helpers.concat_path(sdm_folder_abs_path, Const.SDM_XML);
        String sdm_xsd_abs_path = Helpers.concat_path(sdm_folder_abs_path, Const.SDM_XSD);
        List<DaoClass> jaxb_dao_classes = SdmUtils.get_dao_classes(sdm_xml_abs_path, sdm_xsd_abs_path);
        return jaxb_dao_classes;
    }

    public static void enum_root_files(Project project, VirtualFile current_folder, List<VirtualFile> root_files) {
        @SuppressWarnings("UnsafeVfsRecursion") VirtualFile[] children = current_folder.getChildren();
        for (VirtualFile c : children) {
            if (c.isDirectory()) {
                if (!c.getName().equals("bin")) {
                    enum_root_files(project, c, root_files);
                }
            } else {
                String path = IdeaTargetLanguageHelpers.get_root_file_relative_path(project, c);
                if (path != null) {
                    root_files.add(c);
                }
            }
        }
    }
}