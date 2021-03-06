/*
    Copyright 2011-2021 sqldalmaker@gmail.com
    SQL DAL Maker Website: http://sqldalmaker.sourceforge.net
    Read LICENSE.txt in the root of this project/archive for details.
 */
package com.sqldalmaker.netbeans;

import javax.swing.Action;
import javax.swing.JComponent;
import org.netbeans.core.spi.multiview.CloseOperationState;
import org.netbeans.core.spi.multiview.MultiViewElement;
import org.netbeans.core.spi.multiview.MultiViewElementCallback;
import org.openide.awt.UndoRedo;
import org.openide.text.CloneableEditor;
import org.openide.text.CloneableEditorSupport;
import org.openide.util.Lookup;
import org.openide.util.Mutex;
import org.openide.windows.TopComponent;

/**
 *
 * @author sqldalmaker@gmail.com
 *
 */
public class SdmMultiViewCloneableEditor extends CloneableEditor implements MultiViewElement {

    protected final SdmDataObject obj;
    // private final JToolBar toolbar = new JToolBar();
    protected transient MultiViewElementCallback callback;

    public SdmMultiViewCloneableEditor(Lookup lookup) {
        super(lookup.lookup(CloneableEditorSupport.class));
        super.initializeBySupport();

        obj = lookup.lookup(SdmDataObject.class);
        assert obj != null;
        initComponents();
    }

    // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
    // === panedrone: never override it for CloneableEditor successor 
    //
    //    @Override
    //    public String getName() {
    //        return "SdmVisualElement";
    //    }
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 400, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 300, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    // End of variables declaration//GEN-END:variables
    @Override
    public JComponent getVisualRepresentation() {
        return this;
    }

    @Override
    public JComponent getToolbarRepresentation() {
        return null; // toolbar;
    }

    @Override
    public Lookup getLookup() {
        return obj.getLookup();
    }

    // === panedrone: in MultiViewCloneableEditor, all of them call super
    @Override
    public Action[] getActions() {
        return super.getActions(); // new Action[0];
    }

    @Override
    public void componentActivated() {
        super.componentActivated();
    }

    @Override
    public void componentClosed() {
        super.componentClosed();
    }

    @Override
    public void componentDeactivated() {
        super.componentDeactivated();
    }

    @Override
    public void componentHidden() {
        super.componentHidden();
    }

    @Override
    public void componentOpened() {
        super.componentOpened();
    }

    @Override
    public void componentShowing() {
        // super.componentShowing(); === panedrone: it shows default text editor

        updateName(); // instead of call from base constructor
        
        // updateDisplayText(); // copy-paste from MultiViewCloneableEditor: does not work
    }

    @Override
    public UndoRedo getUndoRedo() {
        return UndoRedo.NONE; // TODO: warning if not saved
    }

    @Override
    public void setMultiViewCallback(MultiViewElementCallback callback) {
        this.callback = callback;

        // updateName(); // calling it here leads to empty titles
    }

    @Override
    public CloseOperationState canCloseElement() {
        return CloseOperationState.STATE_OK;
    }

    @Override
    public void updateName() {
        if (obj == null) { // updateName() is called by base constructor when obj is null
            // super.updateName();
            return; // it will be assigned in this.componentShowing()
        }
        // this is copy-paste from CloneableEditor.updateName()
        // custom values are assigned instead of defaults:
        Mutex.EVENT.writeAccess(
                new Runnable() {
                    @Override
                    public void run() {

                        String display_name;
                        try {
                            display_name = NbpPathHelpers.get_path_relative_to_root_folder(obj);
                        } catch (Exception ex) {
                            // ex.printStackTrace();
                            display_name = obj.getPrimaryFile().getNameExt();
                        }

                        // === panedrone: callback.updateTitle causes StackOverflow while drag-drop throuh project tree:
                        // Mutex.EVENT.writeAccess does not solve problem
                        // callback.updateTitle(n);
                        setHtmlDisplayName("<HTML>" + display_name); // TODO
                        setDisplayName(display_name); // causes stack error if called it super.updateName() 2 and more times
                        String name = obj.getPrimaryFile().getNameExt();
                        setName(name); // XXX compatibility

                        setToolTipText(display_name);
                    }
                }
        );
        updateDisplayText();
    }

    /**
     * this copy-paste from MultiViewCloneableEditor solves problem related to unexpected empty title
     */
    private void updateDisplayText() {
        if (this.callback != null) {
            TopComponent tc = this.callback.getTopComponent();
            tc.setHtmlDisplayName(getHtmlDisplayName());
            tc.setDisplayName(getDisplayName());
            tc.setName(getName());
            tc.setToolTipText(getToolTipText());
            tc.setIcon(getIcon());
        }
    }
}
