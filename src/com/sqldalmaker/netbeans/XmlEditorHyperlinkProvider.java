/*
 * Copyright 2011-2018 sqldalmaker@gmail.com
 * SQL DAL Maker Website: http://sqldalmaker.sourceforge.net
 * Read LICENSE.txt in the root of this project/archive for details.
 */
package com.sqldalmaker.netbeans;

import com.sqldalmaker.common.Const;
import com.sqldalmaker.common.FileSearchHelpers;
import com.sqldalmaker.jaxb.settings.Settings;
import javax.swing.JEditorPane;
import javax.swing.text.Document;
import org.netbeans.api.editor.mimelookup.MimeRegistration;
import org.netbeans.api.lexer.Token;
import org.netbeans.api.lexer.TokenHierarchy;
import org.netbeans.api.lexer.TokenSequence;
import org.netbeans.api.xml.lexer.XMLTokenId;
import org.netbeans.lib.editor.hyperlink.spi.HyperlinkProvider;
import org.netbeans.modules.editor.NbEditorUtilities;
import org.openide.cookies.EditorCookie;
import org.openide.cookies.OpenCookie;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectNotFoundException;
import org.openide.util.RequestProcessor;

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

        start_offset = 0;
        end_offset = 0;

        identifier = "";

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

    private static int process_tag(TokenSequence<XMLTokenId> ts, Token<XMLTokenId> tag_token, String search_pattern) {

        String tag_text = tag_token.text().toString();

        if (!(tag_token.id() == XMLTokenId.TAG && tag_text.equals("<dto-class"))) {

            return 0;
        }

        while (ts.moveNext()) { // search for argument 'name'

            if (tag_text.equals(">")) {

                break;
            }

            Token<XMLTokenId> arg_token = ts.token();

            if (arg_token == null) {

                break;
            }

            String arg_text = arg_token.text().toString();

            if (!(arg_token.id() == XMLTokenId.ARGUMENT && arg_text.equals("name"))) {

                continue;
            }

            while (ts.moveNext()) { // search for operator '='

                Token<XMLTokenId> equal_operator_token = ts.token();

                if (equal_operator_token == null) {

                    break;
                }

                String equal_operator_text = equal_operator_token.text().toString();

                if (!(equal_operator_token.id() == XMLTokenId.OPERATOR && equal_operator_text.equals("="))) {

                    continue;
                }

                while (ts.moveNext()) { // skip spaces

                    Token<XMLTokenId> value_token = ts.token();

                    if (value_token == null) {

                        break;
                    }

                    if (value_token.id() == XMLTokenId.VALUE) {

                        CharSequence cs = value_token.text();

                        if (cs != null) {

                            String dto_class_name = cs.toString();

                            dto_class_name = dto_class_name.substring(1, dto_class_name.length() - 1);

                            // System.out.println(dto_class_name);
                            if (dto_class_name.equals(search_pattern)) {

                                int tok_offset = ts.offset();

                                // System.out.println(tok_offset);
                                return tok_offset;
                            }
                        }

                        break; // break if XMLTokenId.VALUE found after XMLTokenId.OPERATOR
                    }

                } // while

                break; // break if XMLTokenId.OPERATOR '=' found

            } // while

        } // while

        return 0;
    }

    private static int get_gto_class_declaration_offset(Document dto_xml_doc, String search_pattern) throws Exception {

        TokenHierarchy<Document> hi = TokenHierarchy.get(dto_xml_doc);

        TokenSequence<XMLTokenId> ts = hi.tokenSequence(XMLTokenId.language());

        ts.moveStart();

        while (ts.moveNext()) {

            Token<XMLTokenId> tag_token = ts.token();

            if (tag_token == null) {

                break;
            }

            int offset = process_tag(ts, tag_token, search_pattern);

            if (offset > 0) {

                return offset;
            }

        } // while

        return 0;
    }

    @Override
    public void performClickAction(Document doc, int offset) {

        String attribute_name = verify_state(doc, offset); // attribute_name is 'ref' or 'table'

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

            } catch (Exception ex) {
                // ex.printStackTrace();

                NbpIdeMessageHelpers.show_error_in_ui_thread(ex);
            }

        } else if (XmlEditorUtil.ATTRIBUTE.DTO.equals(attribute_name)) {

            FileObject folder = this_doc_file.getParent();

            String file_name = Const.DTO_XML;

            final FileObject dto_xml_file = folder.getFileObject(file_name);

            if (dto_xml_file == null) {

                NbpIdeMessageHelpers.show_error_in_ui_thread("File not found: " + file_name);

                return;
            }

            // https://platform.netbeans.org/tutorials/60/nbm-hyperlink.html
            RequestProcessor.getDefault().post(new Runnable() {

                @Override
                public void run() {

                    try {

                        final DataObject data_object = DataObject.find(dto_xml_file);

                        data_object.getLookup().lookup(OpenCookie.class).open(); // https://blogs.oracle.com/geertjan/entry/open_file_action

                        org.netbeans.editor.Utilities.runInEventDispatchThread(new Runnable() { // https://platform.netbeans.org/tutorials/60/nbm-hyperlink.html
//                                // SwingUtilities.invokeAndWait(new Runnable() { // === panedrone: it does not work:
//                                // SwingUtilities.invokeLater(new Runnable() { // === panedrone: it does not work:
                            @Override
                            public void run() {

                                try {

                                    // copy-paste from // https://platform.netbeans.org/tutorials/60/nbm-hyperlink.html
                                    // warning: [cast] redundant cast to Observable
                                    // final EditorCookie.Observable ec = (EditorCookie.Observable) dObject.getCookie(EditorCookie.Observable.class);
                                    final EditorCookie.Observable ec = data_object.getCookie(EditorCookie.Observable.class);

                                    if (ec != null) {

                                        final JEditorPane[] panes = ec.getOpenedPanes(); // UI thread needed

                                        if ((panes != null) && (panes.length > 0)) {

                                            Document dto_xml_doc = panes[0].getDocument();

                                            if (dto_xml_doc == null) {

                                                return;
                                            }

                                            int tok_offset = get_gto_class_declaration_offset(dto_xml_doc, identifier);

                                            // panes[0].setCaretPosition(tok_offset);
                                            panes[0].setSelectionStart(tok_offset + 1);
                                            panes[0].setSelectionEnd(tok_offset + 1 + identifier.length());
                                        }
                                    }

                                    // // === panedrone: it does not work:
                                    //                                    
                                    // final JTextComponent t = EditorRegistry.lastFocusedComponent(); 
                                    // t.setCaretPosition(tok_offset);
                                    //
                                } catch (Exception ex) {
                                    // ex.printStackTrace();
                                    NbpIdeMessageHelpers.show_error_in_ui_thread(ex);
                                }

                            } // run
                        });

                    } catch (DataObjectNotFoundException ex) {
                        // ex.printStackTrace();
                        NbpIdeMessageHelpers.show_error_in_ui_thread(ex);
                    }

                } // run

            });

        }
    }
}
