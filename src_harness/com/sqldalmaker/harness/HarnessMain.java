/*
    Copyright 2011-2024 sqldalmaker@gmail.com
    SQL DAL Maker Website: https://sqldalmaker.sourceforge.net/
    Read LICENSE.txt in the root of this project/archive for details.
 */
package com.sqldalmaker.harness;

import com.sqldalmaker.cg.Helpers;
import com.sqldalmaker.cg.IDaoCG;
import com.sqldalmaker.cg.IDtoCG;
import com.sqldalmaker.common.Const;
import com.sqldalmaker.common.SdmUtils;
import com.sqldalmaker.common.TargetLangUtils;
import com.sqldalmaker.common.XmlParser;
import com.sqldalmaker.jaxb.sdm.DaoClass;
import com.sqldalmaker.jaxb.sdm.DtoClass;
import com.sqldalmaker.jaxb.settings.Settings;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.Driver;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/*
 * Design-time only. NOT a part of the plug-ins:
 * 'src_harness' is listed neither in 'build.properties' nor in '*.iml'.
 *
 * It runs the code generator headlessly (no IDE) and dumps everything it produces
 * to a folder, so that the outputs of two builds can be compared with 'diff -r'.
 * Errors are dumped as well - a changed error message is a changed behavior too.
 */
public class HarnessMain {

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.err.println("usage: HarnessMain <projects-list-file> <out-dir>");
            System.exit(2);
        }
        List<String[]> projects = _read_projects_list(args[0]);
        Path out_root = Paths.get(args[1]).toAbsolutePath();
        int failed = 0;
        for (String[] p : projects) {
            try {
                _run_project(p[0], p[1], p[2], out_root.resolve(p[0]));
            } catch (Throwable e) {
                failed++;
                System.out.println(p[0] + ": FAILED: " + _message(e));
            }
        }
        System.out.println("--- projects: " + projects.size() + ", failed: " + failed);
        if (failed > 0) {
            System.exit(1);
        }
    }

    // label | project dir | sdm folder relative to project dir
    private static List<String[]> _read_projects_list(String list_file) throws Exception {
        List<String[]> res = new ArrayList<String[]>();
        for (String line : Files.readAllLines(Paths.get(list_file), StandardCharsets.UTF_8)) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            String[] parts = line.split("\\|");
            if (parts.length != 3) {
                throw new Exception("Expected 'label | project dir | sdm folder': " + line);
            }
            for (int i = 0; i < parts.length; i++) {
                parts[i] = parts[i].trim();
            }
            res.add(parts);
        }
        return res;
    }

    private static void _run_project(
            String label,
            String project_dir,
            String sdm_folder_rel_path,
            Path out_dir) throws Exception {

        project_dir = new File(project_dir).getCanonicalPath().replace('\\', '/');
        // an absolute path means the XML configuration lives outside of the project
        // ('harness/fixtures/...'), while SQL files, the DB and the JDBC jar stay in it
        File sdm_folder = new File(sdm_folder_rel_path);
        String sdm_folder_abs_path = sdm_folder.isAbsolute()
                ? sdm_folder.getCanonicalPath().replace('\\', '/')
                : Helpers.concat_path(project_dir, sdm_folder_rel_path);
        String root_fn = _find_root_file(sdm_folder_abs_path);
        Settings settings = SdmUtils.load_settings(sdm_folder_abs_path);
        Connection con = _connect(project_dir, settings);
        int errors;
        try {
            errors = _gen_dto(root_fn, project_dir, sdm_folder_abs_path, con, settings, out_dir.resolve("dto"));
            errors += _gen_dao(root_fn, project_dir, sdm_folder_abs_path, con, settings, out_dir.resolve("dao"));
        } finally {
            con.close();
        }
        System.out.println(label + ": " + root_fn + ", errors: " + errors);
    }

    private static String _find_root_file(String sdm_folder_abs_path) throws Exception {
        String[] names = new File(sdm_folder_abs_path).list();
        if (names != null) {
            Arrays.sort(names); // stable choice if there are several
            for (String name : names) {
                if (TargetLangUtils.accept(name)) {
                    return name;
                }
            }
        }
        throw new Exception("Root file (java.dal, go.dal, ...) not found in " + sdm_folder_abs_path);
    }

    private static int _gen_dto(
            String root_fn,
            String project_dir,
            String sdm_folder_abs_path,
            Connection con,
            Settings settings,
            Path out_dir) throws Exception {

        StringBuilder output_dir_rel_path = new StringBuilder();
        IDtoCG gen = TargetLangUtils.create_dto_cg(
                root_fn, project_dir, sdm_folder_abs_path, con, settings, output_dir_rel_path);
        _write(out_dir, "_output_dir.txt", output_dir_rel_path.toString());
        String sdm_xml = Helpers.concat_path(sdm_folder_abs_path, Const.SDM_XML);
        String sdm_xsd = Helpers.concat_path(sdm_folder_abs_path, Const.SDM_XSD);
        int errors = 0;
        for (DtoClass cls : SdmUtils.get_dto_classes(sdm_xml, sdm_xsd)) {
            String class_name = cls.getName();
            try {
                String[] content = gen.translate(class_name);
                _write_generated(out_dir, root_fn, class_name, content);
            } catch (Throwable e) {
                errors++;
                _write(out_dir, class_name + ".ERROR.txt", _message(e));
            }
        }
        return errors;
    }

    private static int _gen_dao(
            String root_fn,
            String project_dir,
            String sdm_folder_abs_path,
            Connection con,
            Settings settings,
            Path out_dir) throws Exception {

        StringBuilder output_dir_rel_path = new StringBuilder();
        IDaoCG gen = TargetLangUtils.create_dao_cg(
                root_fn, project_dir, sdm_folder_abs_path, con, settings, output_dir_rel_path);
        _write(out_dir, "_output_dir.txt", output_dir_rel_path.toString());
        int errors = 0;
        for (DaoClass cls : SdmUtils.load_all_sdm_dao_classes(sdm_folder_abs_path)) {
            String class_name = cls.getName();
            try {
                DaoClass jaxb_dao_class = cls;
                String ref = cls.getRef();
                if (ref != null && !ref.trim().isEmpty()) {
                    jaxb_dao_class = _load_external_dao_class(sdm_folder_abs_path, ref);
                    jaxb_dao_class.setName(class_name);
                }
                String[] content = gen.translate(jaxb_dao_class);
                _write_generated(out_dir, root_fn, class_name, content);
            } catch (Throwable e) {
                errors++;
                _write(out_dir, class_name + ".ERROR.txt", _message(e));
            }
        }
        return errors;
    }

    private static DaoClass _load_external_dao_class(String sdm_folder_abs_path, String ref) throws Exception {
        String dao_xsd = Helpers.concat_path(sdm_folder_abs_path, Const.DAO_XSD);
        String context_path = DaoClass.class.getPackage().getName();
        XmlParser xml_parser = new XmlParser(context_path, dao_xsd);
        return xml_parser.unmarshal(Helpers.concat_path(sdm_folder_abs_path, ref));
    }

    private static void _write_generated(
            Path out_dir,
            String root_fn,
            String class_name,
            String[] content) throws Exception {

        String file_name = TargetLangUtils.file_name_from_class_name(root_fn, class_name);
        for (int i = 0; i < content.length; i++) {
            // only content[0] is used by the plug-ins,
            // the rest is dumped as well to notice it if it ever appears
            _write(out_dir, i == 0 ? file_name : file_name + "." + i, content[i]);
        }
    }

    private static void _write(Path out_dir, String file_name, String text) throws Exception {
        Files.createDirectories(out_dir);
        Files.write(out_dir.resolve(file_name), text.getBytes(StandardCharsets.UTF_8));
    }

    // no stack traces on purpose: line numbers would change on every refactoring
    // and produce diffs that mean nothing
    private static String _message(Throwable e) {
        return e.getClass().getName() + ": " + e.getMessage();
    }

    private static Connection _connect(String project_dir, Settings settings) throws Exception {
        String driver_jar = settings.getJdbc().getJar();
        String driver_class_name = settings.getJdbc().getClazz();
        String url = settings.getJdbc().getUrl();
        String user_name = settings.getJdbc().getUser();
        String password = settings.getJdbc().getPwd();
        url = url.replace("${project_loc}", project_dir).replace("$PROJECT_DIR$", project_dir);
        Class<?> cl;
        if (driver_jar != null && !driver_jar.trim().isEmpty()) {
            File jar = new File(project_dir, driver_jar);
            if (!jar.exists()) {
                throw new Exception("Cannot find " + jar);
            }
            ClassLoader loader = new URLClassLoader(new URL[]{jar.toURI().toURL()});
            cl = Class.forName(driver_class_name, true, loader);
        } else {
            cl = Class.forName(driver_class_name);
        }
        Driver driver = (Driver) cl.getDeclaredConstructor().newInstance();
        Properties props = new Properties();
        if (user_name != null) {
            props.put("user", user_name);
            props.put("password", password);
        }
        Connection con = driver.connect(url, props);
        if (con == null) {
            throw new Exception("Invalid URL: " + url);
        }
        return con;
    }
}
