package com.sqldalmaker.cg;

public class SqlUtils {

	public static String sql_to_cpp_str(StringBuilder sql_buff) {

		return sql_to_cpp_str(sql_buff.toString());
	}

	public static String sql_to_cpp_str(String sql) {

		String[] parts = sql.split("(\\n|\\r)+");

		String new_line = System.getProperty("line.separator");

		String new_line_j = org.apache.commons.lang.StringEscapeUtils.escapeJava("\n");

		StringBuilder res = new StringBuilder();

		res.append("\"");

		for (int i = 0; i < parts.length; i++) {

			String j_str = parts[i].replace('\t', ' ');

			// packed into Velocity JAR:
			j_str = org.apache.commons.lang.StringEscapeUtils.escapeJava(j_str);

			// fix the bug in StringEscapeUtils:
			// case '/':
			// out.write('\\');
			// out.write('/');
			// break;
			j_str = j_str.replace("\\/", "/");

			res.append(j_str);

			if (i < parts.length - 1) {

				res.append(" ");
				res.append(new_line_j);
				res.append("\\");
				res.append(new_line);
				// res.append("\t\t");

			} else {

				res.append("\"");
			}
		}

		return res.toString();
	}

	public static String jdbc_sql_to_java_str(StringBuilder sql_buff) {

		return jdb_sql_to_java_str(sql_buff.toString());
	}

	public static String jdb_sql_to_java_str(String jdbc_sql) {

		String[] parts = jdbc_sql.split("(\\n|\\r)+");

		// "\n" it is OK for Eclipse debugger window:
		String new_line = "\n"; // System.getProperty("line.separator");

		StringBuilder res = new StringBuilder();

		for (int i = 0; i < parts.length; i++) {

			String j_str = parts[i].replace('\t', ' ');

			// packed into Velocity JAR:
			j_str = org.apache.commons.lang.StringEscapeUtils.escapeJava(j_str);

			// fix the bug in StringEscapeUtils:
			// case '/':
			// out.write('\\');
			// out.write('/');
			// break;
			j_str = j_str.replace("\\/", "/");

			res.append(j_str);

			if (i < parts.length - 1) {

				String s = " \" " + new_line + "\t\t\t\t + \"";

				res.append(s);
			}
		}

		return res.toString();
	}

	public static String sql_to_php_str(StringBuilder sql_buff) {

		return php_sql_to_php_str(sql_buff.toString());
	}

	public static String jdbc_sql_to_php_str(String jdbc_sql) throws Exception {

		boolean is_sp = DbUtils.is_jdbc_stored_proc_call(jdbc_sql);

		String php_sql;

		if (is_sp) {

			php_sql = DbUtils.jdbc_to_php_stored_proc_call(jdbc_sql);

		} else {

			php_sql = jdbc_sql;
		}

		return php_sql_to_php_str(php_sql);
	}

	private static String php_sql_to_php_str(String php_sql) {

		String[] parts = php_sql.split("(\\n|\\r)+");

		String new_line = "\n"; // System.getProperty("line.separator");

		StringBuilder res = new StringBuilder();

		for (int i = 0; i < parts.length; i++) {

			String j_str = parts[i].replace('\t', ' ');

			// packed into Velocity JAR:
			j_str = org.apache.commons.lang.StringEscapeUtils.escapeJava(j_str);

			// fix the bug in StringEscapeUtils:
			// case '/':
			// out.write('\\');
			// out.write('/');
			// break;
			j_str = j_str.replace("\\/", "/");

			res.append(j_str);

			if (i < parts.length - 1) {

				String s = " \" " + new_line + "\t\t\t\t . \"";

				res.append(s);
			}
		}

		return res.toString();
	}

	public static String python_sql_to_python_string(StringBuilder sql_buff) throws Exception {

		return jdbc_sql_to_python_string(sql_buff.toString());
	}

	public static String ruby_sql_to_ruby_string(StringBuilder sql_buff) throws Exception {

		return jdbc_sql_to_python_string(sql_buff.toString());
	}

	public static String jdbc_sql_to_ruby_string(String jdbc_sql) throws Exception {

		return jdbc_sql_to_python_string(jdbc_sql);
	}

	public static String jdbc_sql_to_python_string(String jdbc_sql) throws Exception {

		boolean is_sp = DbUtils.is_jdbc_stored_proc_call(jdbc_sql);

		String python_sql;

		if (is_sp) {

			python_sql = DbUtils.jdbc_to_python_stored_proc_call(jdbc_sql);

		} else {

			python_sql = jdbc_sql;
		}

		return python_sql_to_python_string(python_sql);
	}

	public static String python_sql_to_python_string(String python_sql) {

		String[] parts = python_sql.split("(\\n|\\r)+");

		String new_line = "\n"; // System.getProperty("line.separator");

		StringBuilder res = new StringBuilder();

		for (int i = 0; i < parts.length; i++) {

			String j_str = parts[i].replace('\t', ' ');

			// packed into Velocity JAR:
			j_str = org.apache.commons.lang.StringEscapeUtils.escapeJava(j_str);

			// fix the bug in StringEscapeUtils:
			// case '/':
			// out.write('\\');
			// out.write('/');
			// break;
			j_str = j_str.replace("\\/", "/");

			res.append(j_str);

			if (i < parts.length - 1) { // python wants 4 spaces instead of 1 tab

				String s = " " + new_line + "                ";

				res.append(s);
			}
		}

		return res.toString();
	}
}
