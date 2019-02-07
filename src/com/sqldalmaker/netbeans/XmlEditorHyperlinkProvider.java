/*
 * Copyright 2011-2018 sqldalmaker@gmail.com
 * SQL DAL Maker Website: http://sqldalmaker.sourceforge.net
 * Read LICENSE.txt in the root of this project/archive for details.
 */
package com.sqldalmaker.netbeans;

import com.sqldalmaker.common.Const;
import com.sqldalmaker.common.FileSearchHelpers;
import com.sqldalmaker.jaxb.settings.Settings;
import javax.swing.text.Document;
import org.netbeans.api.editor.mimelookup.MimeRegistration;
import org.netbeans.lib.editor.hyperlink.spi.HyperlinkProvider;
import org.netbeans.modules.editor.NbEditorUtilities;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataObject;

/**
 *
 * @author sqldalmaker@gmail.com
 *
 */
@MimeRegistration(mimeType = "text/xml", service = HyperlinkProvider.class)
public class XmlEditorHyperlinkProvider implements HyperlinkProvider {

    // Google: netbeans platform xml navigation api
    // basic link for NetBeans IDE version version 6.1 or version 6.0
    // https://platform.netbeans.org/tutorials/60/nbm-hyperlink.html
    // -->
    // Google: netbeans 8 register HyperlinkProvider
    // version for NetBeans IDE version 8.0 or above
    // https://platform.netbeans.org/tutorials/nbm-hyperlink.html
    // Docs about 
    // http://bits.netbeans.org/dev/javadoc/org-netbeans-modules-editor-lib/org/netbeans/lib/editor/hyperlink/spi/HyperlinkProvider.html
    // 
    // Note: there is no assurance on the order of calling of the methods 
    // in this class. The callers may call the methods in any order and 
    // even do not call some of these methods at all. 
    //
    // !!!!!!!!!!!!!
    // === panedrone: ^^ this is why verification is performed in all methods here
    // https://platform.netbeans.org/tutorials/nbm-hyperlink.html
    // unlike of https://platform.netbeans.org/tutorials/60/nbm-hyperlink.html
    // 
    private int start_offset;

    private int end_offset;

    private String identifier;

    private String verify_state(Document doc, int offset) {

        //////////////////////////////////////////////////////
        // Google: swing document get file name java -JFileChooser
        // -->
        // Java Code Examples for javax.swing.text.Document
        // http://www.programcreek.com/java-api-examples/index.php?api=javax.swing.text.Document
        //
        DataObject data_object = NbEditorUtilities.getDataObject(doc);

        if (data_object == null) {

            return null;
        }

        FileObject this_file_object = data_object.getPrimaryFile();

        if (this_file_object == null) {

            return null;
        }

        String doc_name = this_file_object.getNameExt();

        if (!FileSearchHelpers.is_dto_xml(doc_name) && !FileSearchHelpers.is_dao_xml(doc_name)) {

            return null;
        }

        XmlEditorUtil checker = new XmlEditorUtil();

        String attribute_name = checker.verify_state(doc, offset);

        if (XmlEditorUtil.ATTRIBUTE.REF.equals(attribute_name)) {

            String ref_value = checker.get_identifier();

            if (ref_value.endsWith(".sql")) {

                start_offset = checker.get_start_offset() + 1;

                end_offset = checker.get_end_offset() + 1;

                identifier = ref_value;

                return attribute_name;
            }

        } else if (XmlEditorUtil.ATTRIBUTE.DTO.equals(attribute_name)) {

            if (FileSearchHelpers.is_dao_xml(doc_name)) {

                start_offset = checker.get_start_offset() + 1;

                end_offset = checker.get_end_offset() + 1;

                identifier = checker.get_identifier();

                return attribute_name;
            }
        }

        return null;
    }

    @Override
    public boolean isHyperlinkPoint(Document doc, int offset) {

        return verify_state(doc, offset) != null;
    }

    @Override
    public int[] getHyperlinkSpan(Document doc, int offset) {

        if (verify_state(doc, offset) != null) {

            return new int[]{start_offset, end_offset};

        } else {

            return null;
        }
    }

    @Override
    public void performClickAction(Document doc, int offset) {

        String attribute_name = verify_state(doc, offset);

        if (identifier == null || identifier.length() == 0) {

            return;
        }

        // === panedrone: find document only when clicking the hyperlink
        //
        DataObject data_object = NbEditorUtilities.getDataObject(doc);

        if (data_object == null) {

            return;
        }

        FileObject this_doc_file = data_object.getPrimaryFile();

        if (this_doc_file == null) {

            return;
        }

        if (XmlEditorUtil.ATTRIBUTE.REF.equals(attribute_name)) {

            if (!identifier.endsWith(".sql")) {

                return;
            }

            try {

                Settings settings = NbpHelpers.load_settings(this_doc_file);

                String rel_path = settings.getFolders().getSql() + "/" + identifier;

                NbpIdeEditorHelpers.open_project_file_in_editor_async(this_doc_file, rel_path);

            } catch (Throwable ex) {
                // ex.printStackTrace();
                
                NbpIdeMessageHelpers.show_error_in_ui_thread(ex);
            }

        } else if (XmlEditorUtil.ATTRIBUTE.DTO.equals(attribute_name)) {

            FileObject folder = this_doc_file.getParent();

            NbpIdeEditorHelpers.open_metaprogram_file_in_editor_async(folder, Const.DTO_XML);
        }
    }
}
