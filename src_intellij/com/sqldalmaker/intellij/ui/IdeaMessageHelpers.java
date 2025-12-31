/*
    Copyright 2011-2026 sqldalmaker@gmail.com
    SQL DAL Maker Website: https://sqldalmaker.sourceforge.net/
    Read LICENSE.txt in the root of this project/archive for details.
 */
package com.sqldalmaker.intellij.ui;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationGroup;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.sqldalmaker.common.InternalException;
import com.sqldalmaker.jaxb.settings.Ide;
import com.sqldalmaker.jaxb.settings.Settings;

/*
  Created with IntelliJ IDEA.
  User: sqldalmaker@gmail.com
  Date: 21.06.12

  2025-12-31 - Updated for IntelliJ Platform 2022.2+:
  - Removed static initialization depending on services
  - Switched to NotificationGroupManager API
  - Removed duplicated code in add_dto_error_message/add_dao_error_message
*/
public class IdeaMessageHelpers {

    /*
     * Lazily obtain the notification group.
     * Group must be declared in plugin.xml:
     *
     * <notificationGroup id="SDM" displayType="BALLOON"/>
     */
    private static NotificationGroup getNotificationGroup() {
        // Lazy retrieval ensures no static initialization depending on services
        return NotificationGroupManager.getInstance().getNotificationGroup("SDM");
    }

    /** Global (project = null) notify wrapper */
    private static void notify(Notification notification) {
        notification.notify(null);
    }

    // -------------------------------------------------------------------------
    //  Logging to IDE Event Log
    // -------------------------------------------------------------------------

    public static void add_warning_to_ide_log(String msg) {
        if (msg == null || msg.trim().isEmpty()) return;
        Notification notification =
                getNotificationGroup().createNotification(msg, NotificationType.WARNING);
        notify(notification);
    }

    public static void add_info_to_ide_log(String title, String msg) {
        add_to_ide_log(title, msg, NotificationType.INFORMATION);
    }

    public static void add_to_ide_log(String title, String msg, NotificationType nt) {
        if (msg == null || msg.trim().isEmpty()) return;

        msg = msg.replace("<", "&lt;").replace(">", "&gt;");
        Notification notification =
                getNotificationGroup().createNotification("<b>" + title + ":</b> " + msg, nt);

        notify(notification);
    }

    public static void add_error_to_ide_log(String title, String msg) {
        add_to_ide_log(title, msg, NotificationType.ERROR);
    }

    // -------------------------------------------------------------------------
    //  Unified internal helper for DAO + DTO error messages
    // -------------------------------------------------------------------------

    private static void add_error_message_common(Settings settings, String id, String msg) {
        Ide ide = settings.getIde();
        // Early return: user disabled Event Log in settings
        if (ide != null && !ide.isEventLog()) return;

        // Safe invokeLater to run in EDT
        IdeaHelpers.invokeLater(() -> add_error_to_ide_log(id, msg));
    }

    public static void add_dto_error_message(Settings settings, VirtualFile root_file,
                                             String dto_class_name, String msg) {
        add_error_message_common(settings, dto_class_name, msg);
    }

    public static void add_dao_error_message(Settings settings, VirtualFile root_file,
                                             String dao_xml_rel_path, String msg) {
        add_error_message_common(settings, dao_xml_rel_path, msg);
    }

    // -------------------------------------------------------------------------
    //  UI dialogs
    // -------------------------------------------------------------------------

    public static void show_error_in_ui_thread(Throwable e) {
        String m;
        if (e instanceof InternalException) {
            m = e.getMessage();
        } else {
            m = e.getClass().getName() + ":\n" + e.getMessage();
        }

        String finalMsg = m.replace("<", "");

        IdeaHelpers.invokeLater(() ->
                Messages.showErrorDialog(finalMsg, "Error")
        );
    }

    public static void show_info_in_ui_thread(String title, String message) {
        IdeaHelpers.invokeLater(() -> {
            String msg = message.replace("<", "");
            Messages.showInfoMessage(msg, title);
        });
    }

    public static void show_info_in_ui_thread(String msg) {
        show_info_in_ui_thread("Info", msg);
    }
}
