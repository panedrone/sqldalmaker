/*
 * Copyright 2011-2020 sqldalmaker@gmail.com
 * SQL DAL Maker Website: http://sqldalmaker.sourceforge.net
 * Read LICENSE.txt in the root of this project/archive for details.
 *
 */
package com.sqldalmaker.netbeans;

import com.sqldalmaker.jaxb.settings.Ide;
import com.sqldalmaker.jaxb.settings.Settings;
import java.io.IOException;
import org.netbeans.api.project.ProjectInformation;
import org.netbeans.api.project.ProjectUtils;
import org.openide.util.Exceptions;
import org.openide.windows.IOProvider;
import org.openide.windows.InputOutput;

/**
 *
 * @author sqldalmaker@gmail.com
 *
 */
public class NbpIdeConsoleUtil {

    private InputOutput io = null;

    public NbpIdeConsoleUtil(Settings settings, SdmDataObject obj) {

        Ide ide = settings.getIde();

        if (ide != null) {

            if (!ide.isEventLog()) {

                return;
            }
        }

        construct(obj);
    }

    public NbpIdeConsoleUtil(SdmDataObject obj) {

        construct(obj);
    }

    private void construct(SdmDataObject obj) {

        String display_name;

        try {

            ProjectInformation info = ProjectUtils.getInformation(NbpPathHelpers.get_project(obj.getPrimaryFile()));

            display_name = info.getDisplayName() + " (" + NbpPathHelpers.get_path_relative_to_root_folder(obj) + ")";

        } catch (Exception ex) {

            Exceptions.printStackTrace(ex);

            display_name = "sqldalmaker";
        }

        IOProvider prov = IOProvider.getDefault();
        
        //
        // http://wiki.netbeans.org/BookNBPlatformCookbookCH0209
        // Set the second parameter to true to enable reuse previously used object if any
        //
        // === panedrone:
        // prov.getIO(...) creates the tab.
        // the tab doesn't appear if it was closed by user.
        // 
        io = prov.getIO(display_name, false); // === ^^ false adds new tab everytime!!!

        // 
        // https://netbeans.org/download/5_5/javadoc/org-openide-io/org/openide/windows/InputOutput.html
        // Set whether the error output should be mixed into the regular output or not.
        //
        io.setErrSeparated(false);

        // 
        // https://netbeans.org/download/5_5/javadoc/org-openide-io/org/openide/windows/InputOutput.html
        // Show or hide the error pane, if separated.
        //
        io.setErrVisible(false);

        try {

            io.getOut().reset(); // === panedrone: it must be called too!
            //
            // https://netbeans.org/download/5_5/javadoc/org-openide-io/org/openide/windows/OutputWriter.html
            // Clear the output pane.
            // 
            io.getErr().reset();

        } catch (IOException ex) {

            Exceptions.printStackTrace(ex);
        }

        // 
        // https://netbeans.org/download/5_5/javadoc/org-openide-io/org/openide/windows/InputOutput.html
        // Ensure this pane is visible.
        //
        io.select();
    }

    public void add_error_message(String clazz, String msg) {

        if (io != null) {

            // io.getErr().print("ERROR");
            // io.getOut().println(" [" + clazz + "]: " + msg);

            io.getErr().println("[" + clazz + "]: " + msg);
            
            // io.getOut().close();
            io.getErr().close();

            io.setErrVisible(true);
            io.select();
        }
    }

    public void add_error_message(Throwable ex) {

        if (io != null) {

            // io.getErr().print("EXCEPTION");
            // io.getOut().println(" [" + ex.getClass().getName() + "]:" + ex.getMessage());

            io.getErr().println("[" + ex.getClass().getName() + "]:" + ex.getMessage());
            
            // io.getOut().close();
            io.getErr().close();

            io.setErrVisible(true);
            io.select();
        }
    }
}
