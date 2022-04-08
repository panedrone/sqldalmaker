/*
 * Copyright 2011-2021 sqldalmaker@gmail.com
 * Read LICENSE.txt in the root of this project/archive.
 * Project web-site: http://sqldalmaker.sourceforge.net
 */
package com.sqldalmaker.eclipse;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

/**
 * @author sqldalmaker@gmail.com
 */
public class ProjectFileHyperlink implements IHyperlink {

	private final IRegion hyperlink_region;
	private final IFile file;
	private final String dto_class_name;

	public ProjectFileHyperlink(IRegion hyperlink_region, IFile file, String dto_class_name) {
		this.hyperlink_region = hyperlink_region;
		this.file = file;
		this.dto_class_name = dto_class_name;
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
			if (dto_class_name == null) {
				EclipseEditorHelpers.open_editor_sync(active_shell, file);
			} else {
				EclipseXmlUtils.goto_dto_class_declaration(active_shell, file, dto_class_name);
			}
		} catch (Throwable e) {
			e.printStackTrace();
			EclipseMessageHelpers.show_error(e);
		}
	}
}
