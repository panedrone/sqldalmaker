/*
 * Copyright 2011-2020 sqldalmaker@gmail.com
 * SQL DAL Maker Website: http://sqldalmaker.sourceforge.net
 * Read LICENSE.txt in the root of this project/archive for details.
 * 
 */
package com.sqldalmaker.common;

import com.sqldalmaker.cg.JdbcUtils;
import com.sqldalmaker.cg.FieldInfo;
import com.sqldalmaker.cg.FieldNamesMode;
import com.sqldalmaker.cg.Helpers;
import com.sqldalmaker.jaxb.dao.*;
import com.sqldalmaker.jaxb.dto.DtoClass;
import com.sqldalmaker.jaxb.dto.DtoClasses;
import com.sqldalmaker.jaxb.settings.Settings;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 *
 * @author sqldalmaker@gmail.com
 *
 */
public class SdmUtils {

    /*
	 * Used in XML assistants
     */
    public static Set<String> find_dto_used_in_dao_xml_crud(String metaprogram_abs_path, List<String> dao_xml_file_name_list) throws Exception {

        String context_path = DaoClass.class.getPackage().getName();

        XmlParser xml_parser = new XmlParser(context_path,
                Helpers.concat_path(metaprogram_abs_path, Const.DAO_XSD));

        Set<String> res = new HashSet<String>();

        for (String dao_xml_file_name : dao_xml_file_name_list) {

            Set<String> dto_in_use = find_dto_used_in_dao_xml_crud(xml_parser,
                    Helpers.concat_path(metaprogram_abs_path, dao_xml_file_name));

            res.addAll(dto_in_use);
        }

        return res;
    }

    private static Set<String> find_dto_used_in_dao_xml_crud(XmlParser xml_parser, String xml_file_abs_path) throws Exception {

        Set<String> res = new HashSet<String>();

        DaoClass dao_class = xml_parser.unmarshal(xml_file_abs_path);

        if (dao_class.getCrudOrCrudAutoOrQuery() != null) {

            for (int i = 0; i < dao_class.getCrudOrCrudAutoOrQuery().size(); i++) {

                Object element = dao_class.getCrudOrCrudAutoOrQuery().get(i);

                if (element instanceof Crud) {

                    Crud c = (Crud) element;

                    res.add(c.getDto());

                } else if (element instanceof CrudAuto) {

                    CrudAuto c = (CrudAuto) element;

                    res.add(c.getDto());
                }
            }
        }

        return res;
    }

    /*
	 * Used in XML assistants
	 *
     */
    public static void add_fk_access(boolean underscores_needed, Connection conn, boolean schema_in_xml,
            String selected_schema, DatabaseMetaData db_info, ResultSet rs_tables, List<Object> nodes,
            boolean plural_to_singular) throws SQLException {

//		String table_type = rs_tables.getString("TABLE_TYPE");
//
//		if (!("TABLE".equalsIgnoreCase(table_type) /*
//													 * || "VIEW". equalsIgnoreCase( tableType)
//													 */)) {
//			return;
//		}
        String fk_table_name = rs_tables.getString("TABLE_NAME");

        ResultSet rs = db_info.getImportedKeys(conn.getCatalog(), selected_schema, fk_table_name);

        // [pk_table_name - list of fk_column_name]
        HashMap<String, List<String>> map = new HashMap<String, List<String>>();

        try {

            while (rs.next()) {

                String pk_table_name = rs.getString("PKTABLE_NAME");

                String fk_column_name = rs.getString("FKCOLUMN_NAME");

                if (!map.containsKey(pk_table_name)) {

                    map.put(pk_table_name, new ArrayList<String>());
                }

                map.get(pk_table_name).add(fk_column_name);

                // int fkSequence = rs.getInt("KEY_SEQ");
                //
                // System.out.print(fk_table_name);
                // System.out.print("\t" + fk_column_name);
                // System.out.println("\t" + fkSequence);
            }

        } finally {

            rs.close();
        }

        for (String pk_table_name : map.keySet()) {

            List<String> fk_column_names = map.get(pk_table_name);

            String params = "";

            String columns = "";

            boolean first = true;

            for (String fk_column_name : fk_column_names) {

                String c = fk_column_name;

                // if (IdeaTargetLanguageHelpers.underscores_needed(editor2)) {
                c = Helpers.camel_case_to_lower_under_scores(c);
                // }

                if (first) {

                    params += c;

                    columns += fk_column_name;

                } else {

                    params += ", " + c;

                    columns += ", " + fk_column_name;
                }

                first = false;
            }

            String dto_class_name = table_name_to_dto_class_name(fk_table_name, plural_to_singular);

            String method_name = "get" + table_name_to_dto_class_name(fk_table_name, false) + "By"
                    + table_name_to_dto_class_name(pk_table_name, true);

            QueryDtoList node = new QueryDtoList();

            node.setDto(dto_class_name);

            if (underscores_needed) {
                method_name = Helpers.camel_case_to_lower_under_scores(method_name);
            }

            String method = method_name + "(" + params.toLowerCase() + ")";

            node.setMethod(method);

            if (schema_in_xml && selected_schema != null && selected_schema.length() > 0) {

                node.setRef(selected_schema + "." + fk_table_name + "(" + columns + ")");

            } else {

                node.setRef(fk_table_name + "(" + columns + ")");
            }

            nodes.add(node);
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

                sb.append(s.substring(1).toLowerCase());
            }
        }
        // }

        return sb.toString();
    }

    /*
        just table name without schema needed
     */
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

                sb.append(last_word.substring(1).toLowerCase());
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

    public static DaoClass create_crud_xml_jaxb_dao_class(com.sqldalmaker.jaxb.dao.ObjectFactory object_factory,
                                                          Connection connection, Set<String> in_use, boolean schema_in_xml, String selected_schema,
                                                          boolean include_views, boolean crud_auto,
                                                          boolean add_fk_access, boolean plural_to_singular,
                                                          boolean underscores_needed) throws SQLException {

        DaoClass root = object_factory.createDaoClass();

        List<Object> nodes = root.getCrudOrCrudAutoOrQuery();

        DatabaseMetaData db_info = connection.getMetaData();

        ResultSet rs = JdbcUtils.get_tables_rs(connection, db_info, selected_schema, include_views);

        try {

            while (rs.next()) {

                try {

                    String table_name = rs.getString("TABLE_NAME");

                    String dto_class_name = table_name_to_dto_class_name(table_name, plural_to_singular);

                    if (schema_in_xml && selected_schema != null && selected_schema.length() > 0) {

                        table_name = selected_schema + "." + table_name;
                    }

                    if (!in_use.contains(dto_class_name)) {

                        if (crud_auto) {

                            CrudAuto ca = object_factory.createCrudAuto();
                            ca.setTable(table_name);
                            ca.setDto(dto_class_name);
                            nodes.add(ca);

                        } else {

                            Crud crud = object_factory.createCrud();
                            crud.setDto(dto_class_name);
                            crud.setTable(table_name);

                            {
                                TypeMethod tm = new TypeMethod();
                                String m = "create" + dto_class_name;
                                if (underscores_needed) {
                                    m = Helpers.camel_case_to_lower_under_scores(m);
                                }
                                tm.setMethod(m);
                                crud.setCreate(tm);
                            }
                            {
                                TypeMethod tm = new TypeMethod();
                                String m = "read" + dto_class_name + "List";
                                if (underscores_needed) {
                                    m = Helpers.camel_case_to_lower_under_scores(m);
                                }
                                tm.setMethod(m);
                                crud.setReadAll(tm);
                            }
                            {
                                TypeMethod tm = new TypeMethod();
                                String m = "read" + dto_class_name;
                                if (underscores_needed) {
                                    m = Helpers.camel_case_to_lower_under_scores(m);
                                }
                                tm.setMethod(m);
                                crud.setRead(tm);
                            }
                            {
                                TypeMethod tm = new TypeMethod();
                                String m = "update" + dto_class_name;
                                if (underscores_needed) {
                                    m = Helpers.camel_case_to_lower_under_scores(m);
                                }
                                tm.setMethod(m);
                                crud.setUpdate(tm);
                            }
                            {
                                TypeMethod tm = new TypeMethod();
                                String m = "delete" + dto_class_name;
                                if (underscores_needed) {
                                    m = Helpers.camel_case_to_lower_under_scores(m);
                                }
                                tm.setMethod(m);
                                crud.setDelete(tm);
                            }

                            nodes.add(crud);
                        }

                        if (add_fk_access) {

                            add_fk_access(underscores_needed, connection, schema_in_xml, selected_schema, db_info, rs,
                                    nodes, plural_to_singular);
                        }

                    }

                } catch (SQLException e) {

                    // just skip
                }
            }

        } finally {

            rs.close();
        }

        return root;
    }

    public static DaoClass get_fk_access_xml(Connection conn, ObjectFactory object_factory, boolean schema_in_xml,
            String selected_schema, boolean plural_to_singular, boolean underscores_needed) throws SQLException {

        DaoClass root = object_factory.createDaoClass();

        List<Object> nodes = root.getCrudOrCrudAutoOrQuery();

        DatabaseMetaData db_info = conn.getMetaData();

        ResultSet rs = JdbcUtils.get_tables_rs(conn, db_info, selected_schema, /* include_views */ false); // no FK in views

        try {

            while (rs.next()) {

                add_fk_access(underscores_needed, conn, schema_in_xml, selected_schema, db_info, rs, nodes,
                        plural_to_singular);
            }

        } finally {

            rs.close();
        }

        return root;
    }

    public static DtoClasses get_crud_dto_xml(com.sqldalmaker.jaxb.dto.ObjectFactory object_factory, Connection con,
            Set<String> in_use, boolean schema_in_xml, String selected_schema, boolean include_views,
            boolean plural_to_singular) throws SQLException {

        DtoClasses root = object_factory.createDtoClasses();

        List<DtoClass> items = root.getDtoClass();

        DatabaseMetaData db_info = con.getMetaData();

        ResultSet rs = JdbcUtils.get_tables_rs(con, db_info, selected_schema, include_views);

        try {

            while (rs.next()) {

                String table_name = rs.getString("TABLE_NAME");

                String dto_class_name = table_name_to_dto_class_name(table_name, plural_to_singular);

                if (schema_in_xml && selected_schema != null && selected_schema.length() > 0) {

                    table_name = selected_schema + "." + table_name;
                }

                if (!in_use.contains(dto_class_name)) {

                    DtoClass cls = object_factory.createDtoClass();
                    cls.setName(dto_class_name);
                    cls.setRef(table_name);
                    items.add(cls);
                }
            }

        } finally {

            rs.close();
        }

        return root;
    }

    // for Java
    public static String get_package_relative_path(Settings settings, String package_name) {

        String source_folder = settings.getFolders().getTarget();

        if (package_name.length() == 0) {

            return source_folder;
        }

        return Helpers.concat_path(source_folder, package_name.replace(".", "/"));
    }

    public static void gen_tmp_field_tags(Connection connection, com.sqldalmaker.jaxb.dto.ObjectFactory object_factory,
            DtoClass dto_class, String sql_root_abs_path) throws Exception {

        JdbcUtils db_utils = new JdbcUtils(connection, FieldNamesMode.AS_IS, null);

        ArrayList<FieldInfo> fields = new ArrayList<FieldInfo>();

        db_utils.get_dto_field_info(dto_class, sql_root_abs_path, fields);

        for (FieldInfo f : fields) {

            DtoClass.Field df = object_factory.createDtoClassField();
            df.setColumn(f.getColumnName());
            df.setJavaType(f.getType());
            dto_class.getField().add(df);
        }
    }

    public static List<DtoClass> get_dto_classes(String dto_xml_abs_file_path, String dto_xsd_abs_file_path)
            throws Exception {

        String context_path = DtoClasses.class.getPackage().getName();

        XmlParser xml_parser = new XmlParser(context_path, dto_xsd_abs_file_path);

        DtoClasses elements = xml_parser.unmarshal(dto_xml_abs_file_path);

        List<DtoClass> res = new ArrayList<DtoClass>(elements.getDtoClass());

        return res;
    }

    public static Set<String> get_dto_class_names_used_in_dto_xml(String dto_xml_abs_file_path,
            String dto_xsd_abs_file_path) throws Exception {

        Set<String> res = new HashSet<String>();

        List<DtoClass> list = SdmUtils.get_dto_classes(dto_xml_abs_file_path, dto_xsd_abs_file_path);

        for (DtoClass cls : list) {

            String class_name = cls.getName();

            if (res.contains(class_name)) {
                continue;
            }

            res.add(class_name);
        }

        return res;
    }

    public static Settings load_settings(String settings_folder_abs_path) throws Exception {

        String settings_xml_abs_path = Helpers.concat_path(settings_folder_abs_path, Const.SETTINGS_XML);
        String settings_xsd_abs_path = Helpers.concat_path(settings_folder_abs_path, Const.SETTINGS_XSD);

        String context_path = Settings.class.getPackage().getName();

        XmlParser xml_parser = new XmlParser(context_path, settings_xsd_abs_path);

        Settings res = xml_parser.unmarshal(settings_xml_abs_path);

        return res;
    }
}
