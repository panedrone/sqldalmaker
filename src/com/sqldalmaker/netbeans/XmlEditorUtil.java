/*
    Copyright 2011-2022 sqldalmaker@gmail.com
    SQL DAL Maker Website: http://sqldalmaker.sourceforge.net
    Read LICENSE.txt in the root of this project/archive for details.
 */
package com.sqldalmaker.netbeans;

import com.sqldalmaker.common.Const;
import javax.swing.JEditorPane;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import org.netbeans.api.editor.EditorRegistry;
import org.netbeans.api.lexer.Token;
import org.netbeans.api.lexer.TokenHierarchy;
import org.netbeans.api.lexer.TokenSequence;
import org.netbeans.api.xml.lexer.XMLTokenId;
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
 * Implementation of XmlEditorUtil is based on
 * https://platform.netbeans.org/tutorials/nbm-hyperlink.html
 *
 */
public class XmlEditorUtil {

    public class ATTRIBUTE {

        public static final String NAME = "name";
        public static final String REF = "ref";
        public static final String DTO = "dto";
        public static final String TABLE = "table";
    }

    private int start_offset;
    private int end_offset;

    private String attribute_value;

    public String verify_state(Document doc, int offset) {
        JTextComponent target = EditorRegistry.lastFocusedComponent();
        // Work only with the open editor 
        // and the editor has to be the active component: 
        if ((target == null) || (target.getDocument() != doc)) {
            return "";
        }
        TokenHierarchy<Document> hi = TokenHierarchy.get(doc);
        TokenSequence<XMLTokenId> ts = hi.tokenSequence(XMLTokenId.language());
        ts.move(offset);
        ts.moveNext();
        Token<XMLTokenId> tok = ts.token();
        if (tok != null) {
            int tok_offset = ts.offset();
            switch (tok.id()) {
                case VALUE:
                    while (ts.movePrevious()) {
                        Token<XMLTokenId> prev = ts.token();
                        XMLTokenId prev_id = prev.id();
                        switch (prev_id) {
                            case ARGUMENT:
                                String attribute_name = prev.text().toString();
                                if (ATTRIBUTE.NAME.equals(attribute_name) || ATTRIBUTE.REF.equals(attribute_name)
                                        || ATTRIBUTE.DTO.equals(attribute_name) || ATTRIBUTE.TABLE.equals(attribute_name)) {
                                    CharSequence cs = tok.text();
                                    if (cs == null) {
                                        attribute_value = "";
                                    } else {
                                        attribute_value = cs.toString();
                                        attribute_value = attribute_value.substring(1, attribute_value.length() - 1);
                                    }
                                    // identifier = tok.text().toString();
                                    // this.setLastDocument(doc);
                                    start_offset = tok_offset;
                                    end_offset = start_offset + attribute_value.length();
                                    return attribute_name;
                                }
                                return ""; // === panedrone
                            case OPERATOR:
                                continue;
                            case EOL:
                            case ERROR:
                            case WS:
                                continue;
                            default:
                                return "";
                        }
                    }
                    return "";
            }
            return "";
        }
        return "";
    }

    public int get_start_offset() {
        return start_offset;
    }

    public int get_end_offset() {
        return end_offset;
    }

    public String get_attribute_value() {
        return attribute_value == null ? "" : attribute_value;
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

    public static void goto_dto_class_declaration_async(final FileObject folder, final String dto_class_name) {
        String dto_xml_file_name = Const.DTO_XML;
        final FileObject dto_xml_file = folder.getFileObject(dto_xml_file_name);
        if (dto_xml_file == null) {
            NbpIdeMessageHelpers.show_error_in_ui_thread("File not found: " + dto_xml_file_name);
            return;
        }
        // https://platform.netbeans.org/tutorials/60/nbm-hyperlink.html
        RequestProcessor.getDefault().post(new Runnable() {
            @Override
            public void run() {
                try {
                    final DataObject dto_xml_data_object = DataObject.find(dto_xml_file);
                    // https://blogs.oracle.com/geertjan/entry/open_file_action
                    dto_xml_data_object.getLookup().lookup(OpenCookie.class).open();
                    // https://platform.netbeans.org/tutorials/60/nbm-hyperlink.html
                    org.netbeans.editor.Utilities.runInEventDispatchThread(new Runnable() {
                        // SwingUtilities.invokeAndWait(new Runnable() { // === panedrone: it does not work:
                        // SwingUtilities.invokeLater(new Runnable() { // === panedrone: it does not work:
                        @Override
                        public void run() {
                            try {
                                // copy-paste from // https://platform.netbeans.org/tutorials/60/nbm-hyperlink.html
                                // warning: [cast] redundant cast to Observable
                                // final EditorCookie.Observable ec = (EditorCookie.Observable) dObject.getCookie(EditorCookie.Observable.class);
                                final EditorCookie.Observable ec = dto_xml_data_object.getCookie(EditorCookie.Observable.class);
                                if (ec != null) {
                                    final JEditorPane[] panes = ec.getOpenedPanes(); // UI thread needed
                                    if ((panes != null) && (panes.length > 0)) {
                                        Document dto_xml_doc = panes[0].getDocument();
                                        if (dto_xml_doc == null) {
                                            return;
                                        }
                                        int tok_offset = get_gto_class_declaration_offset(dto_xml_doc, dto_class_name);
                                        // panes[0].setCaretPosition(tok_offset);
                                        panes[0].setSelectionStart(tok_offset + 1);
                                        panes[0].setSelectionEnd(tok_offset + 1 + dto_class_name.length());
                                    }
                                }
                                // === panedrone: it does not work:
                                //                                    
                                // final JTextComponent t = EditorRegistry.lastFocusedComponent(); 
                                // t.setCaretPosition(tok_offset);
                                //
                            } catch (Exception ex) {
                                // ex.printStackTrace();
                                //
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
