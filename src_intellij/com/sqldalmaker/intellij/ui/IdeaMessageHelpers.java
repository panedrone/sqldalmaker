/*
    Copyright 2011-2021 sqldalmaker@gmail.com
    SQL DAL Maker Website: http://sqldalmaker.sourceforge.net
    Read LICENSE.txt in the root of this project/archive for details.
 */
package com.sqldalmaker.intellij.ui;

import com.intellij.notification.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
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

    @SuppressWarnings("CommentedOutCode")
    private static NotificationGroup getNotificationGroup() {
//        for (NotificationGroup gr : NotificationGroup.getAllRegisteredGroups()) {
//            String id = gr.getDisplayId();
//            System.out.println(id);
//        }
        NotificationGroup res = NotificationGroup.findRegisteredGroup("SQL DAL Maker"); // not available in 2017.3
//        if (res == null) {
//            // https://www.plugin-dev.com/intellij-notifications.pdf
//            // https://plugins.jetbrains.com/docs/intellij/notifications.html
//            // inbound NONE, working in in 2017.3, deprecated in 2021.3
//            // @NotNull
//            res = NotificationGroup.logOnlyGroup("SQL DAL Maker");
//        }
        //noinspection ConstantConditions
        if (res == null) {
            // NONE is prefferable
            res = NotificationGroup.findRegisteredGroup("Compiler");  // inbound, NONE, available in 2017.3
            if (res == null) {
                res = NotificationGroup.findRegisteredGroup("Run Anything"); // inbound, BALOON, not available in 2017.3
                if (res == null) {
                    res = NotificationGroup.findRegisteredGroup("Error Report");  // inbound, BALOON, not available in 2017.3
                }
            }
        }
        return res;
    }

    private static final NotificationGroup GROUP_DISPLAY_ID_INFO = getNotificationGroup();
    // new NotificationGroup("sqldalmaker", NotificationDisplayType.NONE, true);  // Scheduled for removal constructor usage (1)

    public static void add_warning_to_ide_log(String msg) {
        if (GROUP_DISPLAY_ID_INFO == null) {
            return;
        }
        Notifications.Bus.notify(GROUP_DISPLAY_ID_INFO.createNotification(msg, MessageType.WARNING));
    }

    public static void add_info_to_ide_log(String msg) {
        if (GROUP_DISPLAY_ID_INFO == null) {
            return;
        }
        Notifications.Bus.notify(GROUP_DISPLAY_ID_INFO.createNotification(msg, MessageType.INFO));
    }

    public static void add_error_to_ide_log(String title, String msg) {
        if (GROUP_DISPLAY_ID_INFO == null) {
            return;
        }
        Notification notification = GROUP_DISPLAY_ID_INFO.createNotification(title + ": " + msg, NotificationType.ERROR);
        Notifications.Bus.notify(notification);
    }

    public static void add_dto_error_message(Settings settings, Project project,
                                             final VirtualFile root_file,
                                             final String dto_class_name,
                                             String msg) {

        if (GROUP_DISPLAY_ID_INFO == null) {
            return;
        }
        Ide ide = settings.getIde();
        if (ide != null) {
            if (!ide.isEventLog()) {
                return;
            }
        }
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                Notification notification = GROUP_DISPLAY_ID_INFO.createNotification(dto_class_name + ": " + msg, NotificationType.ERROR);
                Notifications.Bus.notify(notification);
            }
        });
    }

    public static void add_dao_error_message(Settings settings,
                                             Project project,
                                             VirtualFile root_file,
                                             String dao_xml_rel_path,
                                             String msg) {

        if (GROUP_DISPLAY_ID_INFO == null) {
            return;
        }
        Ide ide = settings.getIde();
        if (ide != null) {
            if (!ide.isEventLog()) {
                return;
            }
        }
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                Notification notification = GROUP_DISPLAY_ID_INFO.createNotification(dao_xml_rel_path + ": " + msg, NotificationType.ERROR);
                Notifications.Bus.notify(notification);
            }
        });
    }

    public static void show_error_in_ui_thread(final Throwable e) {
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            public void run() {
                String msg = "";
                if (!(e instanceof InternalException)) {
                    msg += e.getClass().getName() + ":\n";
                }
                msg += e.getMessage();
                Messages.showErrorDialog(msg, "Error");
            }
        });
    }

    public static void show_info_in_ui_thread(final String msg) {
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            public void run() {
                Messages.showInfoMessage(msg, "Info");
            }
        });
    }
}
