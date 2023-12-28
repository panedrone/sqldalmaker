/*
    Copyright 2011-2023 sqldalmaker@gmail.com
    SQL DAL Maker Website: http://sqldalmaker.sourceforge.net
    Read LICENSE.txt in the root of this project/archive for details.
 */
package com.sqldalmaker.netbeans;

import com.sqldalmaker.jaxb.settings.Ide;
import com.sqldalmaker.jaxb.settings.Settings;
import org.netbeans.api.io.IOProvider;
import org.netbeans.api.io.InputOutput;
import org.netbeans.api.io.OutputColor;
import org.netbeans.api.io.OutputWriter;
import org.netbeans.api.project.ProjectInformation;
import org.netbeans.api.project.ProjectUtils;
import org.openide.util.Exceptions;

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
            display_name = "SDM";
        }
        // https://bits.netbeans.org/dev/javadoc/org-netbeans-api-io/org/netbeans/api/io/OutputColor.html
        // returns (org.netbeans.core.output2.NbIOProvider) org.netbeans.core.output2.NbIOProvider
        IOProvider prov = IOProvider.getDefault();

        // org.netbeans.api.io
        // OutputColor.
        //
        // http://wiki.netbeans.org/BookNBPlatformCookbookCH0209
        // Set the second parameter to true to enable reuse previously used object if any
        //
        // === panedrone:
        // prov.getIO(...) creates the tab.
        // the tab doesn't appear if it was closed by user.
        // 
        io = prov.getIO(display_name, false); // === ^^ false adds new tab everytime!!!
        // === panedrone: https://netbeans.org/download/5_5/org-openide-io/index.html?org/openide/windows/OutputWriter.html
        // Clear the output pane. Expect this method to be deprecated in a future release and an equivalent created in InputOutput. 
        // It is ambiguous what it means to reset stdout but not stderr, etc. For the current implementation, reset should be called on the stdout. 
        // ... but it is missing in org.netbeans.api.io
        // io.getErr().reset(); 
        
        // === panedrone: they print then reset to clear:
        // http://bits.netbeans.org/dev/javadoc/org-netbeans-api-io/architecture-summary.html
//        io.getOut().print("...");
//        io.getErr().println("...");
        io.reset(); // === panedrone:  clear all messages not working on linux + NB11

//        io.getOut().flush();
//        io.getOut().close();
//        io.getErr().flush();
//        io.getErr().close();

        io.show();
        
        // 
        // https://netbeans.org/download/5_5/javadoc/org-openide-io/org/openide/windows/InputOutput.html
        // Set whether the error output should be mixed into the regular output or not.
        //
        // io.setErrSeparated(false);
        // 
        // https://netbeans.org/download/5_5/javadoc/org-openide-io/org/openide/windows/InputOutput.html
        // Show or hide the error pane, if separated.
        //
        // io.setErrVisible(false);
//        try {
            // io.getOut().reset(); // === panedrone: it must be called too!
            //
            // https://netbeans.org/download/5_5/javadoc/org-openide-io/org/openide/windows/OutputWriter.html
            // Clear the output pane.
            // 
            // io.getErr().reset();
//        } catch (IOException ex) {
//            Exceptions.printStackTrace(ex);
//        }
        // 
        // https://netbeans.org/download/5_5/javadoc/org-openide-io/org/openide/windows/InputOutput.html
        // Ensure this pane is visible.
        //
        // io.select();
    }

    public void add_debug_message(String msg) {
        if (io != null) {
            // How to use I/O APIs - Swing ? 
            // https://bits.netbeans.org/dev/javadoc/usecases.html
            // io.select();
            // io.setOutputVisible(true);
            OutputWriter w = io.getOut();
            // io.getErr().print("ERROR");
            // io.getOut().println(" [" + clazz + "]: " + msg);

            // === panedrone: it makes output as Hyperlink
//            OutputListener listener;
//            listener = new OutputListener() {
//                @Override
//                public void outputLineAction(OutputEvent ev) {
//                    //StatusDisplayer.getDefault().setStatusText("Hyperlink clicked!");
//                }
//
//                public void outputLineSelected(OutputEvent ev) {
//                    // Let's not do anything special.
//                }
//
//                public void outputLineCleared(OutputEvent ev) {
//                    // Leave it blank, no state to remove.
//                }
//            };
//            try {
//                io.getOut().println(msg, listener, true);
//            } catch (IOException ex) {
//                io.getOut().println(msg);
//            }
            w.println(msg, OutputColor.debug());

            // io.getOut().close();
            w.flush();
            // io.getOut().close();
            // io.select();
        }
    }

    public void add_success_message(String msg) {
        if (io != null) {
            // How to use I/O APIs - Swing ? 
            // https://bits.netbeans.org/dev/javadoc/usecases.html
            // io.select();
            // io.setOutputVisible(true);
            OutputWriter w = io.getOut();
            // io.getErr().print("ERROR");
            // io.getOut().println(" [" + clazz + "]: " + msg);

            // === panedrone: it makes output as Hyperlink
//            OutputListener listener;
//            listener = new OutputListener() {
//                @Override
//                public void outputLineAction(OutputEvent ev) {
//                    //StatusDisplayer.getDefault().setStatusText("Hyperlink clicked!");
//                }
//
//                public void outputLineSelected(OutputEvent ev) {
//                    // Let's not do anything special.
//                }
//
//                public void outputLineCleared(OutputEvent ev) {
//                    // Leave it blank, no state to remove.
//                }
//            };
//            try {
//                io.getOut().println(msg, listener, true);
//            } catch (IOException ex) {
//                io.getOut().println(msg);
//            }
            w.println(msg, OutputColor.success());

            // io.getOut().close();
            w.flush();
            // io.getOut().close();
            // io.select();
        }
    }
    
    public void add_error_message(String msg) {
        if (io != null) {
            // How to use I/O APIs - Swing ? 
            // https://bits.netbeans.org/dev/javadoc/usecases.html
            // io.select();
            // io.setErrVisible(true);
            OutputWriter w = io.getErr();
            // io.getErr().print("ERROR");
            // io.getOut().println(" [" + clazz + "]: " + msg);
            w.println(msg);
            // io.getOut().close();
            w.flush();
            // io.getErr().close();
        }
    }

    public void add_error_message(Throwable ex) {
        if (io != null) {
            OutputWriter w = io.getErr();
            // io.getErr().print("EXCEPTION");
            // io.getOut().println(" [" + ex.getClass().getName() + "]:" + ex.getMessage());
            w.println("[" + ex.getClass().getName() + "]:" + ex.getMessage());
            // io.getOut().close();
//            io.getErr().close();
//            io.setErrVisible(true);
//            io.select();
            w.flush();
        }
    }
}
