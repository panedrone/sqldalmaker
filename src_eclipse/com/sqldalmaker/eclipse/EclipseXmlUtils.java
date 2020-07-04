/*
 * Copyright 2011-2020 sqldalmaker@gmail.com
 * SQL DAL Maker Website: http://sqldalmaker.sourceforge.net
 * Read LICENSE.txt in the root of this project/archive for details.
 */
package com.sqldalmaker.eclipse;

import java.io.StringReader;

import javax.xml.stream.Location;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;

import org.eclipse.core.resources.IFile;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;

import com.sqldalmaker.common.Const;

/**
*
* @author sqldalmaker@gmail.com
*
*/
public class EclipseXmlUtils {

	public static void goto_dto_class_declaration(Shell shell, IFile file, String dto_class_name) throws Exception {

		IEditorPart part = EclipseEditorHelpers.open_editor_sync(shell, file, false);
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
//
//							streamReader.next();
//
//							if (streamReader.getEventType() == XMLStreamReader.END_ELEMENT) {
							
								Location location = streamReader.getLocation();

								return location;
//							}
//						}
//						
//						return null;
					}
				}
			}
		}

		return null;
	}
}
