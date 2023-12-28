/*
    Copyright 2011-2023 sqldalmaker@gmail.com
    SQL DAL Maker Website: http://sqldalmaker.sourceforge.net
    Read LICENSE.txt in the root of this project/archive for details.
 */
package com.sqldalmaker.netbeans;

import com.sqldalmaker.common.InternalException;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

public class NbpIdeMessageHelpers {

    public static void show_error_in_ui_thread(final Throwable e) {
        // https://platform.netbeans.org/tutorials/nbm-hyperlink.html
        // RequestProcessor.getDefault().post(
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                String msg = "";
                if (e != null) {
                    if (!(e instanceof InternalException)) {
                        msg += e.getClass().getName() + ":\n";
                    }
                    msg += e.getMessage();
                } else {
                    msg = "Unknown Error";
                }
                JOptionPane.showMessageDialog(null, msg, "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    public static void show_error_in_ui_thread(final String msg) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                JOptionPane.showMessageDialog(null, msg, "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    public static void show_info_in_ui_thread(final String msg) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                JOptionPane.showMessageDialog(null, msg, "Info", JOptionPane.INFORMATION_MESSAGE);
            }
        });
    }
}
