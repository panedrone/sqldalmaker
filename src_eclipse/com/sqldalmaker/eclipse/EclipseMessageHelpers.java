/*
	Copyright 2011-2024 sqldalmaker@gmail.com
	Read LICENSE.txt in the root of this project/archive.
	Project web-site: https://sqldalmaker.sourceforge.net/
*/
package com.sqldalmaker.eclipse;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

import com.sqldalmaker.common.InternalException;

/**
 *
 * @author sqldalmaker@gmail.com
 *
 */
public class EclipseMessageHelpers {

	public static void show_error(Throwable e) {
		Shell active_shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
		MessageBox box = new MessageBox(active_shell, SWT.ICON_ERROR);
		box.setText("Error");
		String msg = "";
		if ((e instanceof InternalException) == false) {
			msg += e.getClass().getName() + ":\n";
		}
		msg += e.getMessage();
		box.setMessage(msg);
		box.open();
	}

	public static void show_info(String msg) {
		Shell active_shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
		MessageBox box = new MessageBox(active_shell, SWT.ICON_INFORMATION);
		box.setText("Info");
		box.setMessage(msg);
		box.open();
	}

	public static boolean show_confirmation(String msg) {
		Shell active_shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
		MessageBox box = new MessageBox(active_shell, SWT.ICON_QUESTION | SWT.YES | SWT.NO);
		box.setText("");
		box.setMessage(msg);
		int res = box.open();
		return res == SWT.YES;
	}
}
