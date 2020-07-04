/*
 * Copyright 2011-2020 sqldalmaker@gmail.com
 * SQL DAL Maker Website: http://sqldalmaker.sourceforge.net
 * Read LICENSE.txt in the root of this project/archive for details.
 */
package com.sqldalmaker.common;

import java.io.File;

/**
 * @author sqldalmaker@gmail.com
 */
public class FileSearchHelpers {

    public interface IFile_List {
        void add(String fileName);
    }

    public static boolean is_dto_xml(String name) {
        return name != null && name.equals(Const.DTO_XML);
    }

    public static boolean is_dao_xml(String name) {
        if (name == null) {
            return false;
        }
        if (is_dto_xml(name)) {
            return false;
        }
        if (is_setting_xml(name)) {
            return false;
        }
        return name.endsWith(".xml");
    }

    public static boolean is_setting_xml(String name) {
        return /*name != null &&*/ Const.SETTINGS_XML.equals(name);
    }

    public static void enum_dao_xml_file_names(String xml_configs_folder_full_path,
                                               IFile_List file_list) {

        File dir = new File(xml_configs_folder_full_path);
        String[] children = dir.list();
        if (children != null) {
            for (String fileName : children) {
                if (is_dao_xml(fileName)) {
                    file_list.add(fileName);
                }
            }
        }
    }
}
