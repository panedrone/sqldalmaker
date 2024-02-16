/*
 * Copyright 2011-2024 sqldalmaker@gmail.com
 * Read LICENSE.txt in the root of this project/archive.
 * Project web-site: https://sqldalmaker.sourceforge.net/
 */
package com.sqldalmaker.eclipse;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.sqldalmaker.cg.Helpers;

public class UIDialogAbout extends Dialog {
	private Text text_1;

	static void show_modal(Shell parentShell) {
		UIDialogAbout dlg = new UIDialogAbout(parentShell);
		dlg.open();
	}

	private UIDialogAbout(Shell parentShell) {
		super(parentShell);
	}

	@Override
	public void create() {

		super.create();

		getContents().getShell().setText("About");

		try {
			String txt = Helpers.res_from_jar("ABOUT.txt");
			String get_sdm_info = EclipseHelpers.get_sdm_info();
			txt = String.format(txt, get_sdm_info);
			text_1.setText(txt);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Create contents of the dialog.
	 * 
	 * @param parent
	 */
	@Override
	protected Control createDialogArea(Composite parent) {
		Composite container = (Composite) super.createDialogArea(parent);
		
		text_1 = new Text(container, SWT.BORDER | SWT.MULTI);
		text_1.setEditable(false);
		text_1.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));

		return container;
	}

	/**
	 * Create contents of the button bar.
	 * 
	 * @param parent
	 */
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
	}

	/**
	 * Return the initial size of the dialog.
	 */
	@Override
	protected Point getInitialSize() {
		return new Point(772, 528);
	}

}
