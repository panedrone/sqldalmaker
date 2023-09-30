/*
    Copyright 2011-2023 sqldalmaker@gmail.com
    SQL DAL Maker Website: http://sqldalmaker.sourceforge.net
    Read LICENSE.txt in the root of this project/archive for details.
 */
package com.sqldalmaker.netbeans;

import com.sqldalmaker.common.Const;

/**
 *
 * @author sqldalmaker@gmail.com
 *
 */
public class NbpMetaProgramInitHelpers {

    public static void copy_xsd(SdmDataObject obj) {
        try {
            String output_dir_rel_path = NbpPathHelpers.get_folder_relative_path(obj);
            String file_content = NbpHelpers.read_from_jar_file(Const.DAO_XSD);
            NbpHelpers.save_text_to_file(obj, output_dir_rel_path, Const.DAO_XSD, file_content);
            file_content = NbpHelpers.read_from_jar_file(Const.DTO_XSD);
            NbpHelpers.save_text_to_file(obj, output_dir_rel_path, Const.DTO_XSD, file_content);
            file_content = NbpHelpers.read_from_jar_file(Const.SETTINGS_XSD);
            NbpHelpers.save_text_to_file(obj, output_dir_rel_path, Const.SETTINGS_XSD, file_content);
        } catch (Exception ex) {
            // ex.printStackTrace();
            NbpIdeMessageHelpers.show_error_in_ui_thread(ex);
        }
    }

    public static void copy_settings_xml(SdmDataObject obj) {
        try {
            String output_dir_rel_path = NbpPathHelpers.get_folder_relative_path(obj);
            String file_content = NbpHelpers.read_from_jar_file(Const.SETTINGS_XML);
            NbpHelpers.save_text_to_file(obj, output_dir_rel_path, Const.SETTINGS_XML, file_content);
        } catch (Exception ex) {
            // ex.printStackTrace();
            NbpIdeMessageHelpers.show_error_in_ui_thread(ex);
        }
    }

    public static void copy_dto_xml(SdmDataObject obj) {
        try {
            String output_dir_rel_path = NbpPathHelpers.get_folder_relative_path(obj);
            String file_content = NbpHelpers.read_from_jar_file(Const.DTO_XML);
            NbpHelpers.save_text_to_file(obj, output_dir_rel_path, Const.DTO_XML, file_content);
        } catch (Exception ex) {
            // ex.printStackTrace();
            NbpIdeMessageHelpers.show_error_in_ui_thread(ex);
        }
    }
}
