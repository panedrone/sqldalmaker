/*
 * Copyright 2011-2018 sqldalmaker@gmail.com
 * SQL DAL Maker Website: http://sqldalmaker.sourceforge.net
 * Read LICENSE.txt in the root of this project/archive for details.
 */
package com.sqldalmaker.eclipse;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.ResourceUtil;

/**
 *
 * @author sqldalmaker@gmail.com
 *
 */
public class XmlAttributeHelpers {

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
		//
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
}
