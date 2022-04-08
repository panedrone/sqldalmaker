/*
	Copyright 2011-2022 sqldalmaker@gmail.com
	Read LICENSE.txt in the root of this project/archive.
	Project web-site: http://sqldalmaker.sourceforge.net
*/
package com.sqldalmaker.eclipse;

import java.io.StringReader;

import javax.xml.stream.Location;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.ResourceUtil;

import com.sqldalmaker.common.Const;

/**
 *
 * @author sqldalmaker@gmail.com
 *
 */
public class EclipseXmlAttrHelpers {

	public static boolean is_value_of(String attribute_name, int offset, String text) {
		boolean equal_sign_found = false;
		for (int i = offset; i > 0; i--) {
			char ch = text.charAt(i);
			if (Character.isWhitespace(ch)) {
				continue;
			}
			if (ch == '=') {
				if (equal_sign_found) {
					return false; // second '=' is failure
				} else {
					equal_sign_found = true;
					continue;
				}
			}
			int attr_name_len = attribute_name.length();
			if (i < attr_name_len + 1) {
				return false;
			}
			// Whitespace char is required before attribute name
			String attr = text.substring(i - attr_name_len, i + 1);
			ch = attr.charAt(0);
			if (Character.isWhitespace(ch) == false) {
				return false;
			}
			attr = attr.substring(1);
			if (attr.compareTo(attribute_name) == 0) {
				return true;
			}
		}
		return false;
	}

	// http://stackoverflow.com/questions/943030/eclipse-3-5-how-to-get-file-name-from-editor
	//
	public static IFile get_current_file() {
		IWorkbenchWindow win = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		IWorkbenchPage page = win.getActivePage();
		if (page != null) {
			IEditorPart editor = page.getActiveEditor();
			if (editor != null) {
				IEditorInput input = editor.getEditorInput();
				IFile input_file = ResourceUtil.getFile(input);
				return input_file;
			}
		}
		return null;
	}

	public static int get_start_offset(int offset, String text) {
		// if cursor is inside of empty attribute, then the quote is in current
		// position
		// this is why I start from offset - 1
		for (int i = offset - 1; i >= 0; i--) {
			if (text.charAt(i) == '\"') {
				return i;
			}
		}
		return -1;
	}

	public static int get_end_offset(int offset, String text) {
		for (int i = offset; i < text.length(); i++) {
			if (text.charAt(i) == '\"') {
				return i;
			}
		}
		return -1;
	}

	public static IRegion get_attribute_value_region(int offset, String text) {
		// returns position of left quote
		int reg_offset = get_start_offset(offset, text);
		if (reg_offset == -1) {
			return null;
		}
		// returns position of right quote
		//
		int end_offset = get_end_offset(offset, text);
		if (end_offset == -1) {
			return null;
		}
		if (end_offset <= reg_offset) {
			return null;
		}
		int reg_length = end_offset - reg_offset - 1;
		return new Region(reg_offset + 1, reg_length);
	}

	public static void goto_dto_class_declaration(Shell shell, IFile file, String dto_class_name) throws Exception {
		IEditorPart part = EclipseEditorHelpers.open_editor_sync(shell, file);
		// google: eclipse plugin api editor set caret position
		// https://stackoverflow.com/questions/35591397/eclipseget-and-set-caret-position-of-the-editor
		Control control = part.getAdapter(Control.class);
		// For a text editor the control will be StyledText
		if (control instanceof StyledText) {
			StyledText styledText = (StyledText) control;
			String xml_text = styledText.getText();
			Location location = get_offset(xml_text, dto_class_name);
			if (location == null) {
				styledText.setCaretOffset(0);
				styledText.setTopIndex(0);
			} else {
				// https://stackoverflow.com/questions/3176610/java-gathering-byte-offsets-of-xml-tags-using-an-xmlstreamreader
				// You could use getLocation() on the XMLStreamReader (or XMLEvent.getLocation()
				// if you use XMLEventReader),
				// but I remember reading somewhere that it is not reliable and precise.
				// And it looks like it gives the endpoint of the tag, not the starting
				// location.
				// int offset = location.getCharacterOffset();
				int line = location.getLineNumber();
				int offset = styledText.getOffsetAtLine(line - 1); // line index needed
				styledText.setCaretOffset(offset);
				// https://stackoverflow.com/questions/6321517/getting-swt-styledtext-widget-to-always-scroll-to-its-end
				line = line - 3;
				if (line < 0) {
					line = 0;
				}
				styledText.setTopIndex(line);
			}
		} else {
			throw new Exception("Cannot read " + Const.DTO_XML);
		}
	}

	private static boolean is_dto_class_element(XMLStreamReader streamReader, String dto_class_name) {
		int attr_count = streamReader.getAttributeCount(); // it works only with event == START_ELEMENT
		for (int i = 0; i < attr_count; i++) {
			String attr_name = streamReader.getAttributeLocalName(i); // getAttributeName(i).getLocalPart();
			if (attr_name.equals("name")) {
				String attr_value = streamReader.getAttributeValue(i);
				if (attr_value.equals(dto_class_name)) {
					return true;
				}
				return false;
			}
		}
		return false;
	}

	private static Location get_offset(String xml_text, String dto_class_name) throws Exception {
		XMLInputFactory factory = XMLInputFactory.newInstance();
		XMLStreamReader streamReader = factory.createXMLStreamReader(new StringReader(xml_text));
		while (streamReader.hasNext()) {
			streamReader.next();
			// https://stackoverflow.com/questions/3176610/java-gathering-byte-offsets-of-xml-tags-using-an-xmlstreamreader
			// You could use getLocation() on the XMLStreamReader (or XMLEvent.getLocation()
			// if you use XMLEventReader),
			// but I remember reading somewhere that it is not reliable and precise.
			// And it looks like it gives the endpoint of the tag, not the starting
			// location.

			if (streamReader.getEventType() == XMLStreamReader.START_ELEMENT) {
				String tag = streamReader.getLocalName();
				if (tag.compareTo("dto-class") == 0) {
					if (is_dto_class_element(streamReader, dto_class_name)) {
//						while (streamReader.hasNext()) {
//							streamReader.next();
//							if (streamReader.getEventType() == XMLStreamReader.END_ELEMENT) {
						Location location = streamReader.getLocation();
						return location;
//							}
//						}
//						return null;
					}
				}
			}
		}
		return null;
	}
}
