/*
 * Copyright 2011-2018 sqldalmaker@gmail.com
 * SQL DAL Maker Website: http://sqldalmaker.sourceforge.net
 * Read LICENSE.txt in the root of this project/archive for details.
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
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.variables.IStringVariableManager;
import org.eclipse.core.variables.VariablesPlugin;

import com.sqldalmaker.cg.DbUtils;
import com.sqldalmaker.cg.FieldInfo;
import com.sqldalmaker.cg.FieldNamesMode;
import com.sqldalmaker.cg.Helpers;
import com.sqldalmaker.common.Const;
import com.sqldalmaker.common.EnglishNoun;
import com.sqldalmaker.common.InternalException;
import com.sqldalmaker.common.XmlParser;
import com.sqldalmaker.jaxb.dto.DtoClass;
import com.sqldalmaker.jaxb.dto.DtoClasses;
import com.sqldalmaker.jaxb.settings.Settings;

/**
 * @author sqldalmaker@gmail.com
 *
 */
public class EclipseHelpers {

	public static Settings load_settings(IEditor2 ed) throws Exception {

		String xml_metaprogram_folder_full_path = ed.get_metaprogram_folder_abs_path();

		return load_settings(xml_metaprogram_folder_full_path);
	}

	public static Settings load_settings(String xml_metaprogram_folder_full_path) throws Exception {

		String xml_abs_path = xml_metaprogram_folder_full_path + "/" + Const.SETTINGS_XML;
		String xsd_abs_path = xml_metaprogram_folder_full_path + "/" + Const.SETTINGS_XSD;

		String context_path = Settings.class.getPackage().getName();

		XmlParser xml_parser = new XmlParser(context_path, xsd_abs_path);

		Settings res = xml_parser.unmarshal(xml_abs_path);

		return res;
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

	// //////////////////////////////////////////////////////////////////////////////

	public static String get_connection_string(String src) {

		return src.replace("$PROJECT_DIR$", "${project_loc}");
	}

	public static Connection get_connection(IEditor2 ed) throws Exception {

		Settings sett = load_settings(ed);

		String driver_jar = sett.getJdbc().getJar();

		String driver_class_name = sett.getJdbc().getClazz();

		String url = get_connection_string(sett.getJdbc().getUrl());

		String user_name = sett.getJdbc().getUser();

		String password = sett.getJdbc().getPwd();

		String project_abs_path = ed.get_project().getLocation().toPortableString();

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

			throw new InternalException("Invalid URL: " + url);
		}

		return con;
	}

	// //////////////////////////////////////////////////////////////////////////////

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

	private static String to_camel_case(String str) {

		if (!str.contains("_")) {

			boolean all_is_upper_case = Helpers.is_upper_case(str);

			if (all_is_upper_case) {

				str = str.toLowerCase();
			}

			return Helpers.replace_char_at(str, 0, Character.toUpperCase(str.charAt(0)));
		}

		// http://stackoverflow.com/questions/1143951/what-is-the-simplest-way-to-convert-a-java-string-from-all-caps-words-separated

		StringBuffer sb = new StringBuffer();

		String[] arr = str.split("_");

		for (int i = 0; i < arr.length; i++) {

			String s = arr[i];

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

	public static List<DtoClass> get_dto_classes(String dto_xml_abs_file_path, String dto_xsd_abs_file_path)
			throws Exception {

		List<DtoClass> res = new ArrayList<DtoClass>();

		String context_path = DtoClasses.class.getPackage().getName();

		XmlParser xml_parser = new XmlParser(context_path, dto_xsd_abs_file_path);

		DtoClasses elements = xml_parser.unmarshal(dto_xml_abs_file_path);

		for (DtoClass cls : elements.getDtoClass()) {

			res.add(cls);
		}

		return res;
	}

	public static void gen_tmp_field_tags(Connection connection, com.sqldalmaker.jaxb.dto.ObjectFactory object_factory,
			DtoClass cls_element, String project_root, IEditor2 editor2) throws Exception {

		Settings settings = load_settings(editor2);

		String sql_root_abs_path = project_root + "/" + settings.getFolders().getSql();

		DbUtils md = new DbUtils(connection, FieldNamesMode.AS_IS, null);

        String jdbc_sql = DbUtils.jdbc_sql_by_ref_query(cls_element.getRef(), sql_root_abs_path);

        ArrayList<FieldInfo> fields = new ArrayList<FieldInfo>();

		md.get_dto_field_info(jdbc_sql, cls_element, fields);

		for (FieldInfo f : fields) {

			DtoClass.Field df = object_factory.createDtoClassField();
			df.setColumn(f.getColumnName());
			df.setJavaType(f.getType().replace("java.lang.", ""));
			cls_element.getField().add(df);
		}
	}

	public static String get_package_relative_path(Settings settings, String package_name) {

		String source_folder = settings.getFolders().getTarget();

		return source_folder + "/" + package_name.replace(".", "/");
	}
}