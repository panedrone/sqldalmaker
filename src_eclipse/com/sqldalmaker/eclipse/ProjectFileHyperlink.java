/*
 * Copyright 2011-2018 sqldalmaker@gmail.com
 * SQL DAL Maker Website: http://sqldalmaker.sourceforge.net
 * Read LICENSE.txt in the root of this project/archive for details.
 */
package com.sqldalmaker.eclipse;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

/**
 *
 * @author sqldalmaker@gmail.com
 *
 */
public class ProjectFileHyperlink implements IHyperlink {

	private final IRegion hyperlink_region;

	private final IFile file;

	private final boolean crate_missing;

	public ProjectFileHyperlink(IRegion hyperlink_region, IFile file, boolean crate_missing) {

		this.hyperlink_region = hyperlink_region;

		this.file = file;

		this.crate_missing = crate_missing;
	}

	@Override
	public IRegion getHyperlinkRegion() {

		return hyperlink_region;
	}

	@Override
	public String getTypeLabel() {

		return "Go to file";
	}

	@Override
	public String getHyperlinkText() {

		return "Go to " + file.getLocation().toOSString();
	}

	@Override
	public void open() {

		try {

			Shell active_shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();

			EclipseEditorHelpers.open_editor_sync(active_shell, file, crate_missing);

		} catch (Throwable e) {

			e.printStackTrace();

			EclipseMessageHelpers.show_error(e);
		}
	}
}
