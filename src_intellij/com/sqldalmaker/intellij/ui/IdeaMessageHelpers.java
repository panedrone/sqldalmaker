/*
    Copyright 2011-2022 sqldalmaker@gmail.com
    SQL DAL Maker Website: https://sqldalmaker.sourceforge.net/
    Read LICENSE.txt in the root of this project/archive for details.
 */
package com.sqldalmaker.intellij.ui;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationGroup;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.sqldalmaker.common.InternalException;
import com.sqldalmaker.jaxb.settings.Ide;
import com.sqldalmaker.jaxb.settings.Settings;

/**
 * Created with IntelliJ IDEA.
 * User: sqldalmaker@gmail.com
 * Date: 21.06.12
 */
public class IdeaMessageHelpers {

    // https://stackoverflow.com/questions/32928914/how-do-i-show-a-notification-in-intellij
    // https://plugins.jetbrains.com/docs/intellij/notifications.html#editor-hints

    // @SuppressWarnings("CommentedOutCode")
    private static NotificationGroup getNotificationGroup() {
        // plugin.xml:
        // <notificationGroup id="SDM Errors" displayType="STICKY_BALLOON"/>
        NotificationGroup res = NotificationGroup.findRegisteredGroup("SDM"); // not available in 2017.3
        if (res != null) {
            return res;
        }
        // "Error Report" BALOON, not available in 2017.3    // BALOON is prefferable
        // "Compiler" NONE, available in 2017.3
        // "Run Anything" BALOON, not available in 2017.3
        res = findRegisteredGroup(new String[]{"Error Report", "Compiler", "Run Anything"});
        return res;
    }

    private static NotificationGroup findRegisteredGroup(String[] names) {
        for (int i = 0; i < names.length; i++) {
            NotificationGroup res = NotificationGroup.findRegisteredGroup(names[i]);
            if (res != null) {
                return res;
            }
        }
        return null;
    }

    private static final NotificationGroup GROUP_DISPLAY_ID = getNotificationGroup();
    // new NotificationGroup("sqldalmaker", NotificationDisplayType.NONE, true);  // Scheduled for removal constructor usage (1)

    public static void add_warning_to_ide_log(String msg) {
        if (GROUP_DISPLAY_ID == null) {
            return;
        }
        Notifications.Bus.notify(GROUP_DISPLAY_ID.createNotification(msg, MessageType.WARNING));
    }

    public static void add_info_to_ide_log(String title, String msg) {
        if (GROUP_DISPLAY_ID == null) {
            return;
        }
        add_to_ide_log(title, msg, NotificationType.INFORMATION);
    }

    public static void add_to_ide_log(String title, String msg, NotificationType nt) {
        if (msg == null || msg.trim().length() == 0) {
            return;
        }
        if (GROUP_DISPLAY_ID == null) {
            return;
        }
        msg = msg.replace("<", "&lt;").replace(">", "&gt;");
        //    public final fun createNotification(title: kotlin.String, content: kotlin.String, type: com.intellij.notification.NotificationType): com.intellij.notification.Notification { /* compiled code */ }
        Notification notification = GROUP_DISPLAY_ID.createNotification("<b>" + title + ":</b> " + msg, nt);
        Notifications.Bus.notify(notification);
    }

    public static void add_error_to_ide_log(String title, String msg) {
        add_to_ide_log(title, msg, NotificationType.ERROR);
    }

    public static void add_dto_error_message(Settings settings, final VirtualFile root_file, final String dto_class_name, String msg) {
        Ide ide = settings.getIde();
        if (ide != null) {
            if (!ide.isEventLog()) {
                return;
            }
        }
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                add_error_to_ide_log(dto_class_name, msg);
            }
        });
    }

    public static void add_dao_error_message(Settings settings, VirtualFile root_file, String dao_xml_rel_path, String msg) {
        Ide ide = settings.getIde();
        if (ide != null) {
            if (!ide.isEventLog()) {
                return;
            }
        }
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                add_error_to_ide_log(dao_xml_rel_path, msg);
            }
        });
    }

    public static void show_error_in_ui_thread(Throwable e) {
        String m;
        if (e instanceof InternalException) {
            m = e.getMessage();
        } else {
            m = e.getClass().getName() + ":\n" + e.getMessage();
        }
        // Messages.show... ---- if there is "<" in messages, the text is now shown
        final String msg = m.replace("<", "");
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            public void run() {
                Messages.showErrorDialog(msg, "Error");
            }
        });
    }

    public static void show_info_in_ui_thread(String m) {
        // Messages.show... ---- if there is "<" in messages, the text is now shown
        final String msg = m.replace("<", "");
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            public void run() {
                Messages.showInfoMessage(msg, "Info");
            }
        });
    }
}
