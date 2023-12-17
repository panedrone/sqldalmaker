/*
    Copyright 2011-2023 sqldalmaker@gmail.com
    SQL DAL Maker Website: http://sqldalmaker.sourceforge.net
    Read LICENSE.txt in the root of this project/archive for details.
 */
package com.sqldalmaker.netbeans;

import com.sqldalmaker.common.FileSearchHelpers;
import com.sqldalmaker.jaxb.settings.Settings;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import javax.swing.text.Document;
import org.netbeans.api.editor.mimelookup.MimeRegistration;
import org.netbeans.api.project.Project;
import org.netbeans.lib.editor.hyperlink.spi.HyperlinkProviderExt;
import org.netbeans.lib.editor.hyperlink.spi.HyperlinkType;
import org.netbeans.modules.editor.NbEditorUtilities;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataObject;

/**
 *
 * @author sqldalmaker@gmail.com
 *
 */
@MimeRegistration(mimeType = "text/xml", service = HyperlinkProviderExt.class)
public class XmlEditorHyperlinkProvider implements HyperlinkProviderExt {

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
    private String attribute_value;
    private String attribute_name;

    private boolean update_state(Document doc, int offset) {
        start_offset = 0;
        end_offset = 0;
        attribute_value = "";
        attribute_name = "";
        //////////////////////////////////////////////////////
        // Google: swing document get file name java -JFileChooser
        // -->
        // Java Code Examples for javax.swing.text.Document
        // http://www.programcreek.com/java-api-examples/index.php?api=javax.swing.text.Document
        //
        DataObject data_object = NbEditorUtilities.getDataObject(doc);
        if (data_object == null) {
            return false;
        }
        FileObject this_file_object = data_object.getPrimaryFile();
        if (this_file_object == null) {
            return false;
        }
        String doc_name = this_file_object.getNameExt();
        if (!FileSearchHelpers.is_sdm_xml(doc_name) && !FileSearchHelpers.is_dao_xml(doc_name)) {
            return false;
        }
        XmlEditorUtil checker = new XmlEditorUtil();
        attribute_name = checker.verify_state(doc, offset);
        if (XmlEditorUtil.ATTRIBUTE.REF.equals(attribute_name)) {
            String ref_value = checker.get_attribute_value();
            if (ref_value.endsWith(".sql")) {
                start_offset = checker.get_start_offset() + 1;
                end_offset = checker.get_end_offset() + 1;
                attribute_value = ref_value;
                return true;
            }
        } else if (XmlEditorUtil.ATTRIBUTE.DTO.equals(attribute_name)) {
            if (FileSearchHelpers.is_dao_xml(doc_name)) {
                start_offset = checker.get_start_offset() + 1;
                end_offset = checker.get_end_offset() + 1;
                attribute_value = checker.get_attribute_value();
                return true;
            }
        } else if (XmlEditorUtil.ATTRIBUTE.NAME.equals(attribute_name)) {
            if (FileSearchHelpers.is_sdm_xml(doc_name)) {
                start_offset = checker.get_start_offset() + 1;
                end_offset = checker.get_end_offset() + 1;
                attribute_value = checker.get_attribute_value();
                return true;
            }
        }
        attribute_name = "";
        return false;
    }

    @Override
    public Set<HyperlinkType> getSupportedHyperlinkTypes() {
        return EnumSet.of(HyperlinkType.GO_TO_DECLARATION);
    }

    @Override
    public boolean isHyperlinkPoint(Document doc, int offset, HyperlinkType type) {
        boolean is_link = update_state(doc, offset);
        // === panedrone: if it returns false, performClickAction is never called
        return is_link;
    }

    @Override
    public int[] getHyperlinkSpan(Document doc, int offset, HyperlinkType type) {
        if (update_state(doc, offset)) {
            return new int[]{start_offset, end_offset};
        } else {
            return null;
        }
    }

    @Override
    public void performClickAction(Document doc, int offset, HyperlinkType type) {
        // === panedrone: update it anyway. attributes 'ref' and 'table' are processed 
        if (!update_state(doc, offset)) {
            return;
        }
        if (attribute_name == null || attribute_name.length() == 0) {
            return;
        }
        if (attribute_value == null || attribute_value.length() == 0) {
            return;
        }
        DataObject data_object = NbEditorUtilities.getDataObject(doc); // it is XMLDataObject
        if (data_object == null) {
            return;
        }
        FileObject this_doc_file = data_object.getPrimaryFile();
        if (this_doc_file == null) {
            return;
        }
        if (XmlEditorUtil.ATTRIBUTE.NAME.equals(attribute_name)) {
            try {
                Settings settings = NbpHelpers.load_settings(this_doc_file);
                String dto_class_name = attribute_value;
                final FileObject this_file_object = data_object.getPrimaryFile();
                if (this_file_object == null) {
                    return;
                }
                Project this_project = NbpPathHelpers.get_project(this_file_object);
                if (this_project == null) {
                    return;
                }
                FileObject xml_mp_dir = this_file_object.getParent();
                List<FileObject> root_files = NbpTargetLanguageHelpers.find_root_files(xml_mp_dir);
                if (root_files.size() != 1) {
                    return;
                }
                FileObject root_file = root_files.get(0);
                DataObject root_file_data_object = DataObject.find(root_file);
                if (!(root_file_data_object instanceof SdmDataObject)) {
                    return;
                }
                SdmDataObject root_data_object = (SdmDataObject) root_file_data_object;
                NbpTargetLanguageHelpers.open_in_editor_async(root_data_object, settings, dto_class_name, settings.getDto().getScope());
            } catch (Exception ex) {
                // ex.printStackTrace();
                NbpIdeMessageHelpers.show_error_in_ui_thread(ex);
            }
        } else if (XmlEditorUtil.ATTRIBUTE.REF.equals(attribute_name)) {
            if (!attribute_value.endsWith(".sql")) {
                return;
            }
            try {
                Settings settings = NbpHelpers.load_settings(this_doc_file);
                String rel_path = settings.getFolders().getSql() + "/" + attribute_value;
                NbpIdeEditorHelpers.open_project_file_in_editor_async(this_doc_file, rel_path);
            } catch (Exception ex) {
                // ex.printStackTrace();
                NbpIdeMessageHelpers.show_error_in_ui_thread(ex);
            }
        } else if (XmlEditorUtil.ATTRIBUTE.DTO.equals(attribute_name)) {
            FileObject folder = this_doc_file.getParent();
            XmlEditorUtil.goto_dto_class_declaration_async(folder, attribute_value);
        }
    }

    @Override
    public String getTooltipText(Document doc, int offset, HyperlinkType type) {
        return "Go to...";
    }
}
