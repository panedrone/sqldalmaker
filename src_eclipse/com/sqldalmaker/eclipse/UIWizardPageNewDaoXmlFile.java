/*
 * Copyright 2011-2018 sqldalmaker@gmail.com
 * SQL DAL Maker Website: https://sqldalmaker.sourceforge.net/
 * Read LICENSE.txt in the root of this project/archive for details.
 */
package com.sqldalmaker.eclipse;

import org.eclipse.core.resources.IContainer;

import com.sqldalmaker.common.InternalException;

/**
 *
 * @author sqldalmaker@gmail.com
 *
 */
public class UIWizardPageNewDaoXmlFile extends UIWizardPageNewFile {

	public UIWizardPageNewDaoXmlFile(String pageName) {
		super(pageName);
	}

	public void init(final IEditor2 editor2) throws InternalException {

		final IContainer res = editor2.get_metaprogram_folder();

		if (res == null) {
			throw new InternalException(
					"Invalid meta-program folder");
		}

		setInput(res);

		bindControls(null);
	}
}
