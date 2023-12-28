/*
    Copyright 2011-2023 sqldalmaker@gmail.com
    SQL DAL Maker Website: http://sqldalmaker.sourceforge.net
    Read LICENSE.txt in the root of this project/archive for details.
 */
package com.sqldalmaker.netbeans;

import java.io.IOException;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.MIMEResolver;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectExistsException;
import org.openide.loaders.MultiDataObject;
import org.openide.loaders.MultiFileLoader;
import org.openide.util.NbBundle.Messages;

@Messages({
    "LBL_Sdm_LOADER=SDM"
})
@MIMEResolver.ExtensionRegistration(
        displayName = "#LBL_Sdm_LOADER",
        mimeType = "text/sqldalmaker+dal",
        extension = {"dal"}
)
@DataObject.Registration(
        mimeType = "text/sqldalmaker+dal",
        iconBase = "com/sqldalmaker/netbeans/sqldalmaker.gif",
        displayName = "#LBL_Sdm_LOADER",
        position = 300
)
@ActionReferences({
    @ActionReference(
            path = "Loaders/text/sqldalmaker+dal/Actions",
            id = @ActionID(category = "System", id = "org.openide.actions.OpenAction"),
            position = 100,
            separatorAfter = 200
    ),
    @ActionReference(
            path = "Loaders/text/sqldalmaker+dal/Actions",
            id = @ActionID(category = "Edit", id = "org.openide.actions.CutAction"),
            position = 300
    ),
    @ActionReference(
            path = "Loaders/text/sqldalmaker+dal/Actions",
            id = @ActionID(category = "Edit", id = "org.openide.actions.CopyAction"),
            position = 400,
            separatorAfter = 500
    ),
    @ActionReference(
            path = "Loaders/text/sqldalmaker+dal/Actions",
            id = @ActionID(category = "Edit", id = "org.openide.actions.DeleteAction"),
            position = 600
    ),
    @ActionReference(
            path = "Loaders/text/sqldalmaker+dal/Actions",
            id = @ActionID(category = "System", id = "org.openide.actions.RenameAction"),
            position = 700,
            separatorAfter = 800
    ),
    @ActionReference(
            path = "Loaders/text/sqldalmaker+dal/Actions",
            id = @ActionID(category = "System", id = "org.openide.actions.SaveAsTemplateAction"),
            position = 900,
            separatorAfter = 1000
    ),
    @ActionReference(
            path = "Loaders/text/sqldalmaker+dal/Actions",
            id = @ActionID(category = "System", id = "org.openide.actions.FileSystemAction"),
            position = 1100,
            separatorAfter = 1200
    ),
    @ActionReference(
            path = "Loaders/text/sqldalmaker+dal/Actions",
            id = @ActionID(category = "System", id = "org.openide.actions.ToolsAction"),
            position = 1300
    ),
    @ActionReference(
            path = "Loaders/text/sqldalmaker+dal/Actions",
            id = @ActionID(category = "System", id = "org.openide.actions.PropertiesAction"),
            position = 1400
    )
})
public class SdmDataObject extends MultiDataObject { // implements OpenCookie {

    public SdmDataObject(FileObject pf, MultiFileLoader loader) throws DataObjectExistsException, IOException {
        super(pf, loader);
        registerEditor("text/sqldalmaker+dal", true);
        // === panedrone: seems that SdmDataObject is a singleton, so constructor is called only once.
        // to process separate "open", use this 
        // https://netbeans.apache.org/wiki/main/wiki/DevFaqEditorTopComponent/
        // super.getCookieSet().add(new MySdmOpenCookie());
        // BUT it is much simpler to "implements OpenCookie"
        // because it works for both:
        // - double-click/enter on *.dal" file in file tree
        // - opening like in NbpIdeEditorHelpers.open_in_editor_async:
        //        DataObjectataObject obj = DataObject.find(file);
        //        obj.getLookup().lookup(OpenCookie.class).open()
    }

//    @Override
//    public <T extends Node.Cookie> T getCookie(Class<T> type) {
//        // just to check who calls
//        T res = super.getCookie(type);
//        if (type == OpenCookie.class) { //  repeats unpredictable amount of times
//            NbpIdeMessageHelpers.show_info_in_ui_thread("want OpenCookie.class");
//        }
//        return res;
//    }
//    @Override
//    public void open() {
//        NbpIdeMessageHelpers.show_info_in_ui_thread("SdmDataObject.OpenCookie.open");
//        
//        super.getLookup().lookup(OpenCookie.class).open();
//    }

    // === panedrone: copy-pasted from here:
    // https://netbeans.apache.org/wiki/main/wiki/DevFaqEditorTopComponent/
//    private class MySdmOpenCookie implements OpenCookie {
//
//        @Override
//        public void open() {
//            NbpIdeMessageHelpers.show_info_in_ui_thread("Opener.open");
//        }
//    }

//    @Override
//    public Lookup getLookup() {
//        // === panedrone: copy-pasted from here:
//        // https://netbeans.apache.org/wiki/main/wiki/DevFaqEditorTopComponent/
//        return getCookieSet().getLookup();
//    }
//    @Override
//    protected int associateLookup() {
//        int baseRes = super.associateLookup();
//        return 1;
//    }
    // === panedrone: no need to edit this file :)
//    @MultiViewElement.Registration(
//            displayName = "#LBL_Sdm_EDITOR",
//            iconBase = "com/sqldalmaker/netbeans/sqldalmaker.gif",
//            mimeType = "text/sqldalmaker+dal",
//            persistenceType = TopComponent.PERSISTENCE_ONLY_OPENED,
//            preferredID = "Sdm",
//            position = 1000
//    )
//    @Messages("LBL_Sdm_EDITOR=Source")
//    public static MultiViewEditorElement createEditor(Lookup lkp) {
//        return new MultiViewEditorElement(lkp);
//    }
}
