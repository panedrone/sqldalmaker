/*
    Copyright 2011-2024 sqldalmaker@gmail.com
    SQL DAL Maker Website: https://sqldalmaker.sourceforge.net/
    Read LICENSE.txt in the root of this project/archive for details.
 */
package com.sqldalmaker.common;

import com.sqldalmaker.cg.JdbcUtils;
import com.sqldalmaker.cg.FieldInfo;
import com.sqldalmaker.cg.FieldNamesMode;
import com.sqldalmaker.cg.Helpers;
import com.sqldalmaker.jaxb.sdm.*;
import com.sqldalmaker.jaxb.settings.Settings;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/*
 * @author sqldalmaker@gmail.com
 *
 * 17.12.2023 02:16 1.292 sdm.xml
 * 19.01.2023 20:57 1.276
 * 16.11.2022 08:02 1.269
 * 06.08.2022 08:37 1.261 no 'crud-auto' anymore, just empty 'crud' instead
 * 22.05.2022 20:02 1.244 PHP
 * 23.04.2022 12:00 1.225 Flask-SQLAlchemy
 * 17.05.2021 11:28
 * 01.05.2021 22:33
 *
 */
public class SdmUtils {

    /*
        Used in XML assistants
     */
    public static Set<String> find_dto_used_in_dao_xml_crud(
            String sdm_folder_abs_path,
            List<String> dao_xml_file_name_list) throws Exception {

        String context_path = DaoClass.class.getPackage().getName();
        XmlParser xml_parser = new XmlParser(context_path, Helpers.concat_path(sdm_folder_abs_path, Const.DAO_XSD));
        Set<String> res = new HashSet<String>();
        for (String dao_xml_file_name : dao_xml_file_name_list) {
            Set<String> dto_in_use = find_dto_used_in_dao_xml_crud(xml_parser,
                    Helpers.concat_path(sdm_folder_abs_path, dao_xml_file_name));
            res.addAll(dto_in_use);
        }
        return res;
    }

    private static Set<String> find_dto_used_in_dao_xml_crud(
            XmlParser xml_parser,
            String xml_file_abs_path) throws Exception {

        Set<String> res = new HashSet<String>();
        DaoClass dao_class = xml_parser.unmarshal(xml_file_abs_path);
        List<Object> tags = dao_class.getCrudOrQueryOrQueryList();
        if (tags != null) {
            for (int i = 0; i < tags.size(); i++) {
                Object element = tags.get(i);
                if (element instanceof Crud) {
                    Crud c = (Crud) element;
                    res.add(c.getDto());
                }
            }
        }
        return res;
    }

    /*
        Used in XML assistants
     */
    public static void add_fk_access(
            FieldNamesMode field_names_mode,
            Connection conn,
            boolean schema_in_xml,
            String selected_schema,
            ResultSet rs_tables,
            List<Object> nodes,
            boolean plural_to_singular) throws SQLException {

        String fk_table_name = rs_tables.getString("TABLE_NAME");
        DatabaseMetaData dbmd = conn.getMetaData();
        ResultSet rs = dbmd.getImportedKeys(conn.getCatalog(), selected_schema, fk_table_name);
        // [pk_table_name - list of fk_column_name]
        HashMap<String, List<String>> map = new HashMap<String, List<String>>();
        try {
            while (rs.next()) {
                String pk_table_name = rs.getString("PKTABLE_NAME");
                // String pk_column_name = rs.getString("PKCOLUMN_NAME");
                String fk_column_name = rs.getString("FKCOLUMN_NAME"); // column name in FK table
                if (!map.containsKey(pk_table_name)) {
                    map.put(pk_table_name, new ArrayList<String>());
                }
                map.get(pk_table_name).add(fk_column_name);
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
                if (field_names_mode == FieldNamesMode.SNAKE_CASE) {
                    c = Helpers.camel_case_to_lower_snake_case(c);
                } else if (field_names_mode == FieldNamesMode.LOWER_CAMEL_CASE) {
                    c = Helpers.to_lower_camel_or_title_case(c, false);
                }
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
            if (field_names_mode == FieldNamesMode.SNAKE_CASE) {
                method_name = Helpers.camel_case_to_lower_snake_case(method_name);
            }
            String method = method_name + "(" + params + ")";
            node.setMethod(method);
            if (schema_in_xml && selected_schema != null && !selected_schema.isEmpty()) {
                node.setRef(selected_schema + "." + fk_table_name + "(" + columns + ")");
            } else {
                node.setRef(fk_table_name + "(" + columns + ")");
            }
            nodes.add(node);
        }
    }

    /*
        just table name without schema needed
     */
    public static String table_name_to_dto_class_name(String table_name, boolean plural_to_singular) {
        String word = Helpers.to_lower_camel_or_title_case(table_name, true);
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

    public static DaoClass create_crud_xml_jaxb_dao_class(
            ObjectFactory object_factory,
            Connection conn,
            Set<String> in_use,
            boolean schema_in_xml,
            String selected_schema,
            boolean include_views,
            boolean crud_auto,
            boolean add_fk_access,
            boolean plural_to_singular,
            FieldNamesMode field_names_mode) throws SQLException {

        DaoClass root = object_factory.createDaoClass();
        List<Object> nodes = root.getCrudOrQueryOrQueryList();
        ResultSet rs = JdbcUtils.get_tables_rs(conn, selected_schema, include_views);
        try {
            while (rs.next()) {
                try {
                    String table_name = rs.getString("TABLE_NAME");
                    String dto_class_name = table_name_to_dto_class_name(table_name, plural_to_singular);
                    if (schema_in_xml && selected_schema != null && !selected_schema.isEmpty()) {
                        table_name = selected_schema + "." + table_name;
                    }
                    if (!in_use.contains(dto_class_name)) {
                        if (crud_auto) {
                            Crud ca = object_factory.createCrud();
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
                                if (field_names_mode == FieldNamesMode.SNAKE_CASE) {
                                    m = Helpers.camel_case_to_lower_snake_case(m);
                                }
                                tm.setMethod(m);
                                crud.setCreate(tm);
                            }
                            {
                                TypeMethod tm = new TypeMethod();
                                String m = "read" + dto_class_name + "List";
                                if (field_names_mode == FieldNamesMode.SNAKE_CASE) {
                                    m = Helpers.camel_case_to_lower_snake_case(m);
                                }
                                tm.setMethod(m);
                                crud.setReadAll(tm);
                            }
                            {
                                TypeMethod tm = new TypeMethod();
                                String m = "read" + dto_class_name;
                                if (field_names_mode == FieldNamesMode.SNAKE_CASE) {
                                    m = Helpers.camel_case_to_lower_snake_case(m);
                                }
                                tm.setMethod(m);
                                crud.setRead(tm);
                            }
                            {
                                TypeMethod tm = new TypeMethod();
                                String m = "update" + dto_class_name;
                                if (field_names_mode == FieldNamesMode.SNAKE_CASE) {
                                    m = Helpers.camel_case_to_lower_snake_case(m);
                                }
                                tm.setMethod(m);
                                crud.setUpdate(tm);
                            }
                            {
                                TypeMethod tm = new TypeMethod();
                                String m = "delete" + dto_class_name;
                                if (field_names_mode == FieldNamesMode.SNAKE_CASE) {
                                    m = Helpers.camel_case_to_lower_snake_case(m);
                                }
                                tm.setMethod(m);
                                crud.setDelete(tm);
                            }
                            nodes.add(crud);
                        }
                        if (add_fk_access) {
                            add_fk_access(field_names_mode, conn, schema_in_xml, selected_schema, rs,
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

    public static DaoClass get_fk_access_xml(
            Connection conn,
            ObjectFactory object_factory,
            boolean schema_in_xml,
            String selected_schema,
            boolean plural_to_singular,
            FieldNamesMode field_names_mode) throws SQLException {

        DaoClass root = object_factory.createDaoClass();
        List<Object> nodes = root.getCrudOrQueryOrQueryList();
        ResultSet rs = JdbcUtils.get_tables_rs(conn, selected_schema, /* include_views */ false); // no FK in views
        try {
            while (rs.next()) {
                add_fk_access(field_names_mode, conn, schema_in_xml, selected_schema, rs, nodes, plural_to_singular);
            }
        } finally {
            rs.close();
        }
        return root;
    }

    public static Sdm get_crud_sdm_xml(
            ObjectFactory object_factory,
            Connection conn,
            Set<String> in_use,
            boolean schema_in_xml,
            String selected_schema,
            boolean include_views,
            boolean plural_to_singular) throws SQLException {

        Sdm root = object_factory.createSdm();
        List<DtoClass> items = root.getDtoClass();
        ResultSet rs = JdbcUtils.get_tables_rs(conn, selected_schema, include_views);
        try {
            while (rs.next()) {
                String table_name = rs.getString("TABLE_NAME");
                String dto_class_name = table_name_to_dto_class_name(table_name, plural_to_singular);
                if (schema_in_xml && selected_schema != null && !selected_schema.isEmpty()) {
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

    // for Java and PHP
    public static String get_package_relative_path(Settings settings, String package_name) {
        String source_folder = settings.getFolders().getTarget();
        if (package_name.isEmpty()) {
            return source_folder;
        }
        return Helpers.concat_path(source_folder, package_name.replace(".", "/").replace('\\', '/'));
    }

    public static void gen_field_wizard_jaxb(
            Settings settings,
            Connection connection,
            ObjectFactory object_factory,
            DtoClass dto_class,
            String sql_root_abs_path) throws Exception {

        JdbcUtils db_utils = new JdbcUtils(connection, FieldNamesMode.AS_IS, FieldNamesMode.AS_IS, settings, sql_root_abs_path);
        List<FieldInfo> fields = db_utils.get_field_info_for_wizard(dto_class, sql_root_abs_path);
        for (FieldInfo f : fields) {
            DtoClass.Field df = object_factory.createDtoClassField();
            df.setColumn(f.getColumnName());
            df.setType(f.getType());
            dto_class.getField().add(df);
        }
    }

    public static List<DtoClass> get_dto_classes(String sdm_xml_abs_path, String sdm_xsd_abs_path) throws Exception {
        String context_path = Sdm.class.getPackage().getName();
        XmlParser xml_parser = new XmlParser(context_path, sdm_xsd_abs_path);
        Sdm elements = xml_parser.unmarshal(sdm_xml_abs_path);
        List<DtoClass> res = new ArrayList<DtoClass>(elements.getDtoClass());
        return res;
    }

    public static List<DaoClass> get_dao_classes(String sdm_xml_abs_path, String sdm_xsd_abs_path) throws Exception {
        String context_path = Sdm.class.getPackage().getName();
        XmlParser xml_parser = new XmlParser(context_path, sdm_xsd_abs_path);
        Sdm elements = xml_parser.unmarshal(sdm_xml_abs_path);
        List<DaoClass> res = new ArrayList<DaoClass>(elements.getDaoClass());
        return res;
    }

    public static Set<String> get_dto_class_names_used_in_sdm_xml(String sdm_xml_abs_path, String sdm_xsd_abs_path) throws Exception {
        Set<String> res = new HashSet<String>();
        List<DtoClass> list = get_dto_classes(sdm_xml_abs_path, sdm_xsd_abs_path);
        for (DtoClass cls : list) {
            String class_name = cls.getName();
            if (res.contains(class_name)) {
                continue;
            }
            res.add(class_name);
        }
        return res;
    }

    public static Settings load_settings(String sdm_folder_abs_path) throws Exception {
        String settings_xml_abs_path = Helpers.concat_path(sdm_folder_abs_path, Const.SETTINGS_XML);
        String settings_xsd_abs_path = Helpers.concat_path(sdm_folder_abs_path, Const.SETTINGS_XSD);
        String context_path = Settings.class.getPackage().getName();
        XmlParser xml_parser = new XmlParser(context_path, settings_xsd_abs_path);
        Settings res = xml_parser.unmarshal(settings_xml_abs_path);
        return res;
    }
}
