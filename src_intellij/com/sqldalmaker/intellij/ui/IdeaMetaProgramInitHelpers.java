/*
    Copyright 2011-2023 sqldalmaker@gmail.com
    SQL DAL Maker Website: https://sqldalmaker.sourceforge.net/
    Read LICENSE.txt in the root of this project/archive for details.
 */
package com.sqldalmaker.intellij.ui;

import com.intellij.openapi.vfs.VirtualFile;
import com.sqldalmaker.common.Const;

/**
 * Created with IntelliJ IDEA.
 * User: sqldalmaker@gmail.com
 * Date: 21.06.12
 */
public class IdeaMetaProgramInitHelpers {

    public static void create_xsd(VirtualFile root_file) {
        try {
            {
                String text = IdeaHelpers.read_from_jar_resources(Const.DAO_XSD);
                IdeaHelpers.run_write_action_to_save_text_file(root_file, Const.DAO_XSD, text);
            }
            {
                String text = IdeaHelpers.read_from_jar_resources(Const.SDM_XSD);
                IdeaHelpers.run_write_action_to_save_text_file(root_file, Const.SDM_XSD, text);
            }
            {
                String text = IdeaHelpers.read_from_jar_resources(Const.SETTINGS_XSD);
                IdeaHelpers.run_write_action_to_save_text_file(root_file, Const.SETTINGS_XSD, text);
            }
        } catch (Throwable e) {
            // e.printStackTrace();
            IdeaMessageHelpers.show_error_in_ui_thread(e);
        }
    }

    public static void create_settings_xml(VirtualFile root_file) {
        try {
            String text = IdeaHelpers.read_from_jar_resources(Const.SETTINGS_XML);
            IdeaHelpers.run_write_action_to_save_text_file(root_file, Const.SETTINGS_XML, text);
        } catch (Throwable e) {
            // e.printStackTrace();
            IdeaMessageHelpers.show_error_in_ui_thread(e);
        }
    }

    public static void create_sdm_xml(VirtualFile root_file) {
        try {
            String text = IdeaHelpers.read_from_jar_resources(Const.SDM_XML);
            IdeaHelpers.run_write_action_to_save_text_file(root_file, Const.SDM_XML, text);
        } catch (Throwable e) {
            // e.printStackTrace();
            IdeaMessageHelpers.show_error_in_ui_thread(e);
        }
    }
}