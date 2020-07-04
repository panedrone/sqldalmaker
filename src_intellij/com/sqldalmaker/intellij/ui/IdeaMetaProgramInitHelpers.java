/*
 * Copyright 2011-2020 sqldalmaker@gmail.com
 * SQL DAL Maker Website: http://sqldalmaker.sourceforge.net
 * Read LICENSE.txt in the root of this project/archive for details.
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
                String text = IdeaHelpers.read_from_jar_file(Const.DAO_XSD);
                IdeaHelpers.run_write_action_to_save_text_file(root_file, Const.DAO_XSD, text);
            }
            {
                String text = IdeaHelpers.read_from_jar_file(Const.DTO_XSD);
                IdeaHelpers.run_write_action_to_save_text_file(root_file, Const.DTO_XSD, text);
            }
            {
                String text = IdeaHelpers.read_from_jar_file(Const.SETTINGS_XSD);
                IdeaHelpers.run_write_action_to_save_text_file(root_file, Const.SETTINGS_XSD, text);
            }
        } catch (Throwable e) {
            e.printStackTrace();
            IdeaMessageHelpers.show_error_in_ui_thread(e);
        }
    }

    public static void create_settings_xml(VirtualFile root_file) {
        try {
            String text = IdeaHelpers.read_from_jar_file(Const.SETTINGS_XML);
            IdeaHelpers.run_write_action_to_save_text_file(root_file, Const.SETTINGS_XML, text);
        } catch (Throwable e) {
            e.printStackTrace();
            IdeaMessageHelpers.show_error_in_ui_thread(e);
        }
    }

    public static void create_dto_xml(VirtualFile root_file) {
        try {
            String text = IdeaHelpers.read_from_jar_file(Const.DTO_XML);
            IdeaHelpers.run_write_action_to_save_text_file(root_file, Const.DTO_XML, text);
        } catch (Throwable e) {
            e.printStackTrace();
            IdeaMessageHelpers.show_error_in_ui_thread(e);
        }
    }
}