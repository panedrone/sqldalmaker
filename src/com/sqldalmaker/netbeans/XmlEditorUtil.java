/*
 * Copyright 2011-2018 sqldalmaker@gmail.com
 * SQL DAL Maker Website: http://sqldalmaker.sourceforge.net
 * Read LICENSE.txt in the root of this project/archive for details.
 */
package com.sqldalmaker.netbeans;

import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import org.netbeans.api.editor.EditorRegistry;
import org.netbeans.api.lexer.Token;
import org.netbeans.api.lexer.TokenHierarchy;
import org.netbeans.api.lexer.TokenSequence;
import org.netbeans.api.xml.lexer.XMLTokenId;

/**
 *
 * @author sqldalmaker@gmail.com
 
 Implementation of XmlEditorUtil is based on
 https://platform.netbeans.org/tutorials/nbm-hyperlink.html
 *
 */
public class XmlEditorUtil {

    public class ATTRIBUTE {

        public static final String REF = "ref";
        public static final String DTO = "dto";
        public static final String TABLE = "table";
    }

    private int start_offset;

    private int end_offset;

    private String identifier;

    public String verify_state(Document doc, int offset) {

        JTextComponent target = EditorRegistry.lastFocusedComponent();

        // Work only with the open editor 
        // and the editor has to be the active component: 
        //
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

                                if (ATTRIBUTE.REF.equals(attribute_name)
                                        || ATTRIBUTE.DTO.equals(attribute_name)
                                        || ATTRIBUTE.TABLE.equals(attribute_name)) {

                                    CharSequence cs = tok.text();

                                    if (cs == null) {

                                        identifier = "";

                                    } else {

                                        identifier = cs.toString();

                                        identifier = identifier.substring(1, identifier.length() - 1);
                                    }

                                    // identifier = tok.text().toString();
                                    // this.setLastDocument(doc);
                                    //
                                    start_offset = tok_offset;

                                    end_offset = start_offset + identifier.length();

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

    public String get_identifier() {

        return identifier == null ? "" : identifier;
    }
}
