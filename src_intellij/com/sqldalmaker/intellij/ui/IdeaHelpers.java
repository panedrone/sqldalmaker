/*
 * Copyright 2011-2018 sqldalmaker@gmail.com
 * SQL DAL Maker Website: http://sqldalmaker.sourceforge.net
 * Read LICENSE.txt in the root of this project/archive for details.
 */
package com.sqldalmaker.intellij.ui;

import com.intellij.application.options.PathMacrosImpl;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.impl.ProjectPathMacroManager;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.NavigatableFileEditor;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.pom.Navigatable;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.sqldalmaker.cg.Helpers;
import com.sqldalmaker.common.Const;
import com.sqldalmaker.common.EnglishNoun;
import com.sqldalmaker.common.InternalException;
import com.sqldalmaker.common.XmlParser;
import com.sqldalmaker.intellij.references.IdeaReferenceCompletion;
import com.sqldalmaker.jaxb.dto.DtoClass;
import com.sqldalmaker.jaxb.dto.DtoClasses;
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
import java.util.ArrayList;
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

        VirtualFile res = project.getBaseDir();

        if (res == null) {

            throw new Exception("Cannot detect the project base dir");
        }

        return res;
    }

    public static String get_relative_path(Project project, VirtualFile file) {

        String base_abs_path = project.getBasePath();

        String file_abs_path = file.getPath();

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

        String xml_meraprogram_folder_full_path = root_file.getParent().getPath();

        return load_settings(xml_meraprogram_folder_full_path);
    }

    public static Settings load_settings(String xml_meraprogram_folder_full_path) throws Exception {

        String xml_abs_path = xml_meraprogram_folder_full_path + "/" + Const.SETTINGS_XML;

        String xsd_abs_path = xml_meraprogram_folder_full_path + "/" + Const.SETTINGS_XSD;

        String context_path = Settings.class.getPackage().getName();

        XmlParser xml_parser = new XmlParser(context_path, xsd_abs_path);

        return xml_parser.unmarshal(xml_abs_path);
    }

    /**
     * Must be called only from DispatchThread
     */
    public static void start_write_action_from_ui_thread_and_refresh_folder_sync(VirtualFile folder) throws Exception {

        start_write_action_from_ui_thread_and_refresh_folder(folder, false);
    }

    /**
     * Must be called only from DispatchThread
     */
    private static void start_write_action_from_ui_thread_and_refresh_folder(final VirtualFile folder, final boolean async) throws Exception {

        // com.intellij.openapi.vfs.VirtualFile
        // public void refresh(boolean asynchronous, boolean recursive) {
        // This method should be only called within write-action.

        ApplicationManager.getApplication().runWriteAction(new Runnable() {

            @Override
            public void run() {

                try {

                    folder.refresh(async, true);

                } catch (Throwable e) {

                    e.printStackTrace();

                    throw new RuntimeException(e);
                }
            }

        });
    }

    /**
     * Must be called only from DispatchThread
     */
    public static void start_write_action_from_ui_thread_and_refresh_module_sync(final Project project) throws Exception {

        final VirtualFile project_dir = get_project_base_dir(project);

        // com.intellij.openapi.vfs.VirtualFile
        // public void refresh(boolean asynchronous, boolean recursive) {
        // This method should be only called within write-action.

        ApplicationManager.getApplication().runWriteAction(new Runnable() {

            @Override
            public void run() {

                project_dir.refresh(false, true);
            }
        });
    }

    // http://www.devdaily.com/blog/post/java/read-text-file-from-jar-file

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

        // swing app wants 'resources/' but plug-in wants '/resources/' WHY?
        ClassLoader cl = IdeaHelpers.class.getClassLoader();

        InputStream is = cl.getResourceAsStream(path + "/" + res_name);

        if (is == null) {

            is = cl.getResourceAsStream("/" + path + "/" + res_name);
        }

        if (is == null) {

            throw new InternalException("Resource not found: " + res_name);
        }

        return is;
    }

    public static Connection get_connection(Project project,
                                     Settings settings) throws Exception {

        String driver_jar = settings.getJdbc().getJar();
        String driver_class_name = settings.getJdbc().getClazz();
        String url = settings.getJdbc().getUrl();
        String user_name = settings.getJdbc().getUser();
        String password = settings.getJdbc().getPwd();

        return get_connection(project, driver_jar, driver_class_name,
                url, user_name, password);
    }

    public static Connection get_connection(Project project,
                                            String driver_jar,
                                            String driver_class_name, String url,
                                            String user_name, String password) throws Exception {

        PathMacrosImpl pm = PathMacrosImpl.getInstanceEx();
        ProjectPathMacroManager pPmm = new ProjectPathMacroManager(pm, project);
        url = url.replace("${project_loc}", "$PROJECT_DIR$"); // for compatibility with Eclipse
        url = pPmm.expandPath(url);

        VirtualFile project_dir = get_project_base_dir(project);

        VirtualFile driver_file = project_dir.findFileByRelativePath(driver_jar);

        if (driver_file == null) {

            throw new Exception("JDBC driver file not found");
        }

        driver_jar = driver_file.getPath();

        Class<?> cl;

        if (!"".equals(driver_jar)) {

            ClassLoader loader = new URLClassLoader(new URL[]{new File(
                    driver_jar).toURI().toURL()});

            cl = Class.forName(driver_class_name, true, loader);

        } else {

            cl = Class.forName(driver_class_name);
        }

        Driver driver = (Driver) cl.newInstance();

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

    public static List<DtoClass> get_dto_classes(String dto_xml_abs_file_path,
                                                 String dto_xsd_abs_file_path) throws Exception {

        List<DtoClass> res = new ArrayList<DtoClass>();

        String context_path = DtoClasses.class.getPackage().getName();

        XmlParser xml_parser = new XmlParser(context_path, dto_xsd_abs_file_path);

        DtoClasses elements = xml_parser.unmarshal(dto_xml_abs_file_path);

        for (DtoClass cls : elements.getDtoClass()) {

            res.add(cls);
        }

        return res;
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

    public static class GeneratedFileData {

        String file_name;
        String file_content;
    }

    private static class GenerateSourceFileWriteAction implements Runnable {

        private Throwable error;

        private List<GeneratedFileData> generated_file_data_list;

        private Project project;

        private VirtualFile root_file;

        private String output_dir_module_relative_path;

        private static void writeFile(VirtualFile dir, GeneratedFileData gf) throws Exception {

            VirtualFile file = dir.findFileByRelativePath(gf.file_name);

            if (file == null) {

                // createChildData may show dialog and throw
                // java.lang.IllegalStateException: The DialogWrapper can be used only on event dispatch thread.
                // if it is called outside of WriteAction

                file = dir.createChildData(null, gf.file_name);
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

    public static void run_write_action_to_generate_source_file(final String output_dir_module_relative_path, List<GeneratedFileData> generated_file_data_list,
                                                                final Project project, final VirtualFile root_file) throws Exception {

        GenerateSourceFileWriteAction write_action = new GenerateSourceFileWriteAction();

        write_action.generated_file_data_list = generated_file_data_list;
        write_action.project = project;
        write_action.root_file = root_file;
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
                // if it is called outside of WriteAction

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

    private static Throwable _save_text_file_error;

    public static void run_write_action_to_save_text_file(final VirtualFile root_file,
                                                   final String file_name, final String text) throws IOException {

        _save_text_file_error = null;

        Runnable writeAction = new Runnable() {

            @Override
            public void run() {

                try {

                    VirtualFile parent_dir = root_file.getParent();

                    VirtualFile file = parent_dir.findFileByRelativePath(file_name);

                    if (file == null) {

                        // createChildData may show dialog and throw
                        // java.lang.IllegalStateException: The DialogWrapper can be used only on event dispatch thread.
                        // if it is called outside of WriteAction

                        file = parent_dir.createChildData(null, file_name);
                    }

                    file.setBinaryContent(text.getBytes());

                } catch (Throwable e) {

                    _save_text_file_error = e;
                }
            }
        };

        ApplicationManager.getApplication().runWriteAction(writeAction);

        if (_save_text_file_error != null) {

            throw new IOException(_save_text_file_error);
        }
    }

    // for Java
    public static String get_package_relative_path(Settings settings,
                                                   String package_name) {

        String source_folder = settings.getFolders().getTarget();

        if (package_name.length() == 0) {

            return source_folder;
        }

        return source_folder + "/" + package_name.replace(".", "/");
    }

    // thanks to https://plugins.jetbrains.com/plugin/3202?pr=
    private static void navigate_to_source(@NotNull Project project, @NotNull PsiElement psi_element) {

        PsiFile containing_file = psi_element.getContainingFile();

        // VirtualFile virtual_file = containingFile.find_virtual_file();
        VirtualFile virtual_file = IdeaReferenceCompletion.find_virtual_file(containing_file);

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

    public static void navigate_to_dto_class_declaration(@NotNull Project project,
                                                         @NotNull VirtualFile root_file,
                                                         @NotNull String dto_class_name) throws Exception {

        VirtualFile xml_file_dir = root_file.getParent();

        if (xml_file_dir == null) {

            throw new Exception("Cannot get parent folder for " + root_file.getName());
        }

        VirtualFile dto_xml_file = xml_file_dir.findFileByRelativePath(Const.DTO_XML);

        if (dto_xml_file == null) {

            throw new Exception(Const.DTO_XML + " not found");
        }

        PsiElement psi_element = IdeaReferenceCompletion.find_dto_class_xml_tag(project, dto_xml_file, dto_class_name);

        if (psi_element == null) {

            throw new Exception(dto_class_name + ": declaration not found");
        }

        navigate_to_source(project, psi_element);
    }
}