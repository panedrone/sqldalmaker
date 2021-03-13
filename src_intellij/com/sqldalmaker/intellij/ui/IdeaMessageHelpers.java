/*
 * Copyright 2011-2021 sqldalmaker@gmail.com
 * Read LICENSE.txt in the root of this project/archive.
 * Project web-site: http://sqldalmaker.sourceforge.net
 */
package com.sqldalmaker.intellij.ui;

import com.intellij.notification.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.sqldalmaker.common.InternalException;
import com.sqldalmaker.jaxb.settings.Ide;
import com.sqldalmaker.jaxb.settings.Settings;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;

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

    public static void add_error_to_ide_log(final String title, String msg) {
        if (GROUP_DISPLAY_ID_INFO == null) {
            return;
        }
        NotificationListener listener = new NotificationListener.Adapter() {
            @SuppressWarnings("CommentedOutCode")
            @Override
            protected void hyperlinkActivated(@NotNull Notification notification, @NotNull HyperlinkEvent hyperlinkEvent) {
                // notification.expire();
                // notification.setTitle("");
                // notification.hideBalloon();
            }
        };
        Notifications.Bus.notify(GROUP_DISPLAY_ID_INFO.createNotification(title, msg, NotificationType.ERROR, listener));
    }

    public static void add_dto_error_message(Settings settings, final Project project,
                                             final VirtualFile root_file, final String clazz, String msg) {
        if (GROUP_DISPLAY_ID_INFO == null) {
            return;
        }
        Ide ide = settings.getIde();
        if (ide != null) {
            if (!ide.isEventLog()) {
                return;
            }
        }
        NotificationListener listener = new NotificationListener.Adapter() {
            @Override
            protected void hyperlinkActivated(@NotNull Notification notification, @NotNull HyperlinkEvent hyperlinkEvent) {
                notification.expire();
                // notification.setTitle("");
                notification.hideBalloon();
                try {
                    IdeaHelpers.navigate_to_dto_class_declaration(project, root_file, clazz);
                } catch (Exception e) {
                    e.printStackTrace();
                    Notifications.Bus.notify(
                            GROUP_DISPLAY_ID_INFO.createNotification("Cannot navigate to dto.xml",
                                    NotificationType.ERROR));
                }
            }
        };
        // https://www.jetbrains.org/intellij/sdk/docs/user_interface_components/notifications.html
        // The text of the notification can include HTML tags. You can allow the user to interact with
        // the notification by including hyperlink tags in the notification text and passing
        // a NotificationListener instance to the constructor of the Notification class.
        msg = msg.replace("<", "&lt;").replace(">", "&gt;");
        Notification notification = GROUP_DISPLAY_ID_INFO.createNotification(clazz, " <a href='Fix'>Fix</a> " + msg,
                NotificationType.ERROR, listener);
        Notifications.Bus.notify(notification);
    }

    public static void add_dao_error_message(Settings settings, final Project project,
                                             final VirtualFile root_file, final String dao_xml_rel_path, String msg) {
        if (GROUP_DISPLAY_ID_INFO == null) {
            return;
        }
        Ide ide = settings.getIde();
        if (ide != null) {
            if (!ide.isEventLog()) {
                return;
            }
        }
        NotificationListener listener = new NotificationListener.Adapter() {
            @Override
            protected void hyperlinkActivated(@NotNull Notification notification, @NotNull HyperlinkEvent hyperlinkEvent) {
                notification.expire();
                // notification.setTitle("");
                notification.hideBalloon();
                try {
                    IdeaEditorHelpers.open_local_file_in_editor_sync(project, root_file, dao_xml_rel_path);
                } catch (Exception e) {
                    e.printStackTrace();
                    Notifications.Bus.notify(GROUP_DISPLAY_ID_INFO.createNotification(
                            "Cannot navigate to " + dao_xml_rel_path,
                            NotificationType.ERROR));
                }
            }
        };
        // https://www.jetbrains.org/intellij/sdk/docs/user_interface_components/notifications.html
        // The text of the notification can include HTML tags. You can allow the user to interact with
        // the notification by including hyperlink tags in the notification text and passing
        // a NotificationListener instance to the constructor of the Notification class.
        // === <a> in the title is not clickable
        // === message may contain <>
        msg = msg.replace("<", "&lt;").replace(">", "&gt;");
        Notification notification = GROUP_DISPLAY_ID_INFO.createNotification(dao_xml_rel_path,
                " <a href='Fix'>Fix</a> " + msg,
                NotificationType.ERROR, listener);
        Notifications.Bus.notify(notification);
    }

    public static void show_error_in_ui_thread(final Throwable e) {
        SwingUtilities.invokeLater(new Runnable() {
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
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                Messages.showInfoMessage(msg, "Info");
            }
        });
    }
}
