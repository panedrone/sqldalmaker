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

				int offset = location.getCharacterOffset();

				styledText.setCaretOffset(offset);

				// https://stackoverflow.com/questions/6321517/getting-swt-styledtext-widget-to-always-scroll-to-its-end

				int line = location.getLineNumber() - 3;
				
				if (line < 0) {
					line = 0;
				}
				
				styledText.setTopIndex(line);
			}

		} else {

			throw new Exception("Cannot read " + Const.DTO_XML);
		}

	}

	private static Location get_offset(String xml_text, String dto_class_name) throws Exception {

		XMLInputFactory factory = XMLInputFactory.newInstance();

		XMLStreamReader streamReader = factory.createXMLStreamReader(new StringReader(xml_text));

		while (streamReader.hasNext()) {

			streamReader.next();

			if (streamReader.getEventType() == XMLStreamReader.START_ELEMENT) {

				String tag = streamReader.getLocalName();

				if (tag.compareTo("dto-class") == 0) {

					int attr_count = streamReader.getAttributeCount();

					for (int i = 0; i < attr_count; i++) {

						String attr_name = streamReader.getAttributeName(i).getLocalPart();

						if (attr_name.equals("name")) {

							String attr_value = streamReader.getAttributeValue(i);

							if (attr_value.equals(dto_class_name)) {

								Location location = streamReader.getLocation();

								// return location.getCharacterOffset();

//								char[] cc = streamReader.getTextCharacters();
//								
//								String et = streamReader.getText(); 
//								
//								return location.getCharacterOffset() - et.length();

								return location;
							}
						}
					}
				}
			}
		}

		return null;
	}
}
