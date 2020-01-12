/*
 * Copyright 2011-2020 sqldalmaker@gmail.com
 * SQL DAL Maker Website: http://sqldalmaker.sourceforge.net
 * Read LICENSE.txt in the root of this project/archive for details.
 *
 */
package com.sqldalmaker.netbeans;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.KeyEvent;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import org.netbeans.api.editor.completion.Completion;
import org.netbeans.spi.editor.completion.CompletionItem;
import org.netbeans.spi.editor.completion.CompletionTask;
import org.netbeans.spi.editor.completion.support.CompletionUtilities;

/**
 *
 * @author sqldalmaker@gmail.com
 *
 */
public class XmlEditorCompletionItem implements CompletionItem {

    /*
    
     Implementation is based on
     https://platform.netbeans.org/tutorials/nbm-code-completion.html
    
     */
    private final String text;
    
    // private static ImageIcon fieldIcon = new ImageIcon(ImageUtilities.loadImage("org/netbeans/modules/countries/icon.png"));
    
    private static final Color FIELD_COLOR = Color.decode("0x0000B2");
    
    private final int start_offset;
    
    private final int end_offset;

    XmlEditorCompletionItem(String text, int start_offset, int end_offset) {
        
        this.text = text;
        
        this.start_offset = start_offset;
        
        this.end_offset = end_offset;
    }

    @Override
    public void defaultAction(JTextComponent jtc) {

        try {

            Document doc = jtc.getDocument();
            doc.remove(start_offset, end_offset - start_offset);
            doc.insertString(start_offset, text, null);
            //This statement will close the code completion box: 
            Completion.get().hideAll();

        } catch (BadLocationException ex) {
            // ex.printStackTrace();
        }
    }

    @Override
    public void processKeyEvent(KeyEvent ke) {
    }

    @Override
    public int getPreferredWidth(Graphics graphics, Font font) {

        return CompletionUtilities.getPreferredWidth(text, null, graphics, font);
    }

    @Override
    public void render(Graphics g, Font defaultFont, Color defaultColor, Color backgroundColor, int width, int height, boolean selected) {

        CompletionUtilities.renderHtml(/*fieldIcon*/null, text, null, g, defaultFont, (selected ? Color.white : FIELD_COLOR), width, height, selected);
    }

    @Override
    public CompletionTask createDocumentationTask() {

        return null;
    }

    @Override
    public CompletionTask createToolTipTask() {

        return null;
    }

    @Override
    public boolean instantSubstitution(JTextComponent jtc) {

        return false;
    }

    @Override
    public int getSortPriority() {

        return 0;
    }

    @Override
    public CharSequence getSortText() {
        
        return text;
    }

    @Override
    public CharSequence getInsertPrefix() {

        return text;
    }
}
