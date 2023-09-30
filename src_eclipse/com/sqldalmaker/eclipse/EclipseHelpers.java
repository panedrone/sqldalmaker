/*
	Copyright 2011-2023 sqldalmaker@gmail.com
	Read LICENSE.txt in the root of this project/archive.
	Project web-site: https://sqldalmaker.sourceforge.net/
*/
package com.sqldalmaker.eclipse;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.Connection;
import java.sql.Driver;
import java.util.Properties;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.variables.IStringVariableManager;
import org.eclipse.core.variables.VariablesPlugin;
import org.osgi.framework.Bundle;
import org.osgi.framework.Version;

import com.sqldalmaker.cg.Helpers;
import com.sqldalmaker.common.InternalException;
import com.sqldalmaker.common.SdmUtils;
import com.sqldalmaker.jaxb.dto.DtoClass;
import com.sqldalmaker.jaxb.settings.Settings;

/**
 * 
 * @author sqldalmaker@gmail.com
 *
 */
public class EclipseHelpers {

	public static Settings load_settings(IEditor2 ed) throws Exception {
		String xml_mp_folder_abs_path = ed.get_metaprogram_folder_abs_path();
		return SdmUtils.load_settings(xml_mp_folder_abs_path);
	}

	public static String get_absolute_dir_path_str(IProject project, String rel_dir_path) {
		String res = project.getLocation().append(rel_dir_path).toPortableString();
		return res;
	}

	public static String get_absolute_dir_path_str(IProject project) throws InternalException {
		IPath loc = project.getLocation();
		if (loc == null) {
			throw new InternalException("Cannot detect the location of '" + project.getName()
					+ "'. Try to refresh the project explorer and reopen SQL DAL Maker editor.");
		}
		String res = loc.toPortableString();
		return res;
	}

	public static void refresh_project(IProject project) throws Exception {
		project.refreshLocal(IProject.DEPTH_INFINITE, null);
	}

	public static void refresh_metaprogram_folder(IEditor2 editor2) throws Exception {
		String rel_dir_path = editor2.get_metaprogram_folder_path_relative_to_project();
		EclipseHelpers.refresh_project_folder(editor2.get_project(), rel_dir_path);
	}

	public static void refresh_project_folder(IProject project, String rel_path) throws Exception {
		IResource f = project.findMember(rel_path);
		if (f != null) {
			f.refreshLocal(IProject.DEPTH_INFINITE, null);
		}
	}

	public static Connection get_connection(IEditor2 ed) throws Exception {
		Settings sett = load_settings(ed);
		Connection con = get_connection(ed.get_project(), sett);
		return con;
	}

	public static Connection get_connection(IProject project, Settings sett) throws Exception {
		String driver_jar = sett.getJdbc().getJar();
		String driver_class_name = sett.getJdbc().getClazz();
		String url = sett.getJdbc().getUrl();
		String user_name = sett.getJdbc().getUser();
		String password = sett.getJdbc().getPwd();
		String project_abs_path = project.getLocation().toPortableString();
		Connection con = get_connection(project_abs_path, driver_jar, driver_class_name, url, user_name, password);
		return con;
	}

	public static Connection get_connection(String project_abs_path, String driver_jar, String driver_class_name,
			String url, String user_name, String password) throws Exception {
		url = url.replace("$PROJECT_DIR$", "${project_loc}");
		VariablesPlugin p = VariablesPlugin.getDefault();
		IStringVariableManager m = p.getStringVariableManager();
		url = m.performStringSubstitution(url);
		driver_jar = project_abs_path + "/" + driver_jar;
		Class<?> cl = null;
		// System.setProperty("sqlite.purejava", "true");
		if (driver_jar != null && !"".equals(driver_jar)) {
			ClassLoader loader = new URLClassLoader(new URL[] { new File(driver_jar).toURI().toURL() });
			cl = Class.forName(driver_class_name, true, loader);
		} else {
			cl = Class.forName(driver_class_name);
		}
		// https://stackoverflow.com/questions/46393863/what-to-use-instead-of-class-newinstance
		Driver driver = (Driver) cl.getConstructor().newInstance();
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
			throw new InternalException("Invalid URL: " + url);
		}
		return con;
	}

	public static InputStream get_resource_as_stream_from_resource_folder(String res_name) throws Exception {
		return Helpers.get_resource_as_stream_2("resources/" + res_name);
	}

	//
	// http://www.devdaily.com/blog/post/java/read-text-file-from-jar-file
	//
	public static String read_from_resource_folder(String res_name) throws Exception {
		InputStream is = get_resource_as_stream_from_resource_folder(res_name);
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

	public static void save_text_to_file(String abs_file_path, String text) throws IOException {
		File file = new File(abs_file_path);
		file.delete();
		File parent_dir = file.getParentFile();
		if (parent_dir == null) {
			throw new IOException("Parent cannot be detected for '" + abs_file_path + "'");
		}
		parent_dir.mkdirs();
		file.createNewFile();
		FileWriter fw = new FileWriter(file);
		try {
			Writer writer = new BufferedWriter(fw);
			try {
				writer.write(text);
			} finally {
				writer.flush();
				writer.close();
			}
		} finally {
			fw.close();
		}
	}

	public static void gen_tmp_field_tags(Connection connection, com.sqldalmaker.jaxb.dto.ObjectFactory object_factory,
			DtoClass dto_class, String project_root, IEditor2 editor2) throws Exception {
		Settings settings = load_settings(editor2);
		String sql_root_abs_path = project_root + "/" + settings.getFolders().getSql();
		SdmUtils.gen_field_wizard_jaxb(settings, connection, object_factory, dto_class, sql_root_abs_path);
	}

	public static String get_sdm_info() {
		Bundle bundle = Platform.getBundle(Activator.PLUGIN_ID);
		Version version = bundle.getVersion();
		String v = String.format("%d.%d.%d.%s", version.getMajor(), version.getMinor(), version.getMicro(),
				version.getQualifier());
		String jv = System.getProperty("java.version");
		return v + " on Java " + jv;
	}
}