/*
	Copyright 2011-2023 sqldalmaker@gmail.com
	Read LICENSE.txt in the root of this project/archive.
	Project web-site: https://sqldalmaker.sourceforge.net/
*/
package com.sqldalmaker.eclipse;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;

import com.sqldalmaker.common.Const;
import com.sqldalmaker.common.FileSearchHelpers;
import com.sqldalmaker.common.InternalException;

/**
 *
 * @author sqldalmaker@gmail.com
 *
 */
public class UIDialogNewDaoXmlFile extends WizardDialog {

	public UIDialogNewDaoXmlFile(Shell parentShell, IWizard newWizard) {
		super(parentShell, newWizard);
	}

	public static IFile open(Shell parentShell, IEditor2 editor2)
			throws InternalException {

		NewFileWizard wiz = new NewFileWizard(editor2);
		IStructuredSelection selection1 = new StructuredSelection(
				editor2.get_project());
		wiz.setWindowTitle("New File");
		wiz.init(null, selection1);
		wiz.initPage();

		// http://www.eclipsepluginsite.com/dialogs-wizards-2.html
		UIDialogNewDaoXmlFile dialog = new UIDialogNewDaoXmlFile(parentShell,
				wiz);
		dialog.create();
		dialog.setHelpAvailable(false);

		int res = dialog.open();

		if (res == WizardDialog.OK) {
			return wiz.getNewfile();
		}

		return null;
	}

	// http://blog.eclipse-tips.com/2008/07/how-to-create-new-file-wizard.html
	private static class NewFileWizard extends Wizard implements INewWizard {

		private final IEditor2 editor2;
		private Page page;

		private IFile newfile = null;

		private NewFileWizard(IEditor2 editor2) {
			this.editor2 = editor2;
		}

		public void initPage() throws InternalException {

			page.init(editor2);
		}

		// http://www.eclipsezone.com/eclipse/forums/t60396.html
		// org.eclipse.ui.ide plug-in required
		public class Page extends UIWizardPageNewDaoXmlFile {

			public Page(IStructuredSelection selection) {
				super(Page.class.getName());
			}

			@Override
			public void createControl(Composite parent) {
				super.createControl(parent);

				setTitle("Create DAO XML file");
				setDescription("The file must have extension '*.xml'");
				setResource("Dao.xml");
			}

			@Override
			protected InputStream getInitialContents() {
				try {

					String xml = EclipseHelpers
							.read_from_resource_folder(Const.EMPTY_DAO_XML);

					InputStream is = new ByteArrayInputStream(xml.getBytes());

					return is;

				} catch (Throwable e) {
					return null; // ignore and create empty comments
				}
			}
		}

		@Override
		public void addPages() {
			addPage(page);
		}

		@Override
		public boolean canFinish() {

			boolean res = super.canFinish();

			if (!res) {
				return false;
			}

			String name = page.getResource();

			res = FileSearchHelpers.is_dao_xml(name);

			return res;
		}

		@Override
		public boolean performFinish() {

			newfile = page.create_new_file();
			if (newfile != null) {
				return true;
			} else {
				return false;
			}
		}

		@Override
		public void init(IWorkbench workbench, IStructuredSelection selection) {

			this.page = new Page(selection);
		}

		public IFile getNewfile() {
			return newfile;
		}
	}
}