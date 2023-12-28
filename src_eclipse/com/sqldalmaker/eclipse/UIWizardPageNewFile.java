/*
 * Copyright 2011-2018 sqldalmaker@gmail.com
 * SQL DAL Maker Website: https://sqldalmaker.sourceforge.net/
 * Read LICENSE.txt in the root of this project/archive for details.
 */
package com.sqldalmaker.eclipse;

import java.io.InputStream;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.ide.undo.CreateFileOperation;
import org.eclipse.ui.ide.undo.WorkspaceUndoUtil;

/**
 *
 * @author sqldalmaker@gmail.com
 *
 */
public class UIWizardPageNewFile extends WizardPage implements Listener {
	private Text resourceNameField;

	private ViewerComparator fComparator;
	private IContainer fInput;

	// initial value stores
	private String initialFileName;

	// problem indicator
	private String problemMessage = "";//$NON-NLS-1$

	/**
	 * Constant for no problem.
	 */
	public static final int PROBLEM_NONE = 0;
	/**
	 * Constant for empty resource.
	 */
	public static final int PROBLEM_RESOURCE_EMPTY = 1;

	/**
	 * Constant for resource already exists.
	 */
	public static final int PROBLEM_RESOURCE_EXIST = 2;

	/**
	 * Constant for invalid path.
	 */
	public static final int PROBLEM_PATH_INVALID = 4;

	/**
	 * Constant for empty container.
	 */
	// public static final int PROBLEM_CONTAINER_EMPTY = 5;

	/**
	 * Constant for project does not exist.
	 */
	// public static final int PROBLEM_PROJECT_DOES_NOT_EXIST = 6;

	/**
	 * Constant for invalid name.
	 */
	public static final int PROBLEM_NAME_INVALID = 7;

	/**
	 * Constant for path already occupied.
	 */
	// public static final int PROBLEM_PATH_OCCUPIED = 8;

	@SuppressWarnings("unused")
	private int problemType = PROBLEM_NONE;

	protected UIWizardPageNewFile(String pageName) {
		super(pageName);
		setPageComplete(false);
	}

	@Override
	public void createControl(Composite parent) {
		// extends WizardPage
		Composite topLevel = new Composite(parent, SWT.NONE);

		setControl(topLevel);

		GridLayout layout = new GridLayout(2, false);
		layout.marginWidth = 8;
		layout.marginHeight = 8;
		topLevel.setLayout(layout);
		topLevel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		Label lblFileName = new Label(topLevel, SWT.NONE);
		lblFileName.setText("File name:");

		resourceNameField = new Text(topLevel, SWT.BORDER);
		resourceNameField.setLayoutData(new GridData(SWT.FILL, SWT.CENTER,
				true, false, 1, 1));
		resourceNameField.setFocus();
		resourceNameField.setSelection(0);

		resourceNameField.addListener(SWT.Modify, this);

		if (initialFileName != null) {
			setResource(initialFileName);
		}
	}

	protected void bindControls(IContainer container) {

		problemType = PROBLEM_NONE;
	}

//	protected void containerSelectionChanged(IContainer firstElement) {
//
//		Event changeEvent = new Event();
//		changeEvent.type = SWT.Selection;
//		changeEvent.widget = getControl();
//		handleEvent(changeEvent);
//	}

	public ViewerComparator getComparator() {
		return fComparator;
	}

	public void setComparator(ViewerComparator fComparator) {
		this.fComparator = fComparator;
	}

	public IContainer getInput() {
		return fInput;
	}

	public void setInput(IContainer fInput) {
		this.fInput = fInput;
	}

	public String getResource() {
		return resourceNameField.getText();
	}

	public void setResource(String value) {
		if (resourceNameField == null) {
			initialFileName = value;
		} else {
			resourceNameField.setText(value);
			validatePage();
		}

	}

	@Override
	public void handleEvent(Event event) {
		boolean res = validatePage();
		setPageComplete(res);
	}

//	public boolean areAllValuesValid() {
//		return problemType == PROBLEM_NONE;
//	}


	private boolean validatePage() {
		problemType = PROBLEM_NONE;
		problemMessage = "";//$NON-NLS-1$
		if (!validateResourceName()) {
			setErrorMessage(problemMessage);
			return false;
		}
		IPath p = this.fInput.getFullPath().append(resourceNameField.getText());
		String s =  p.toPortableString();
		IPath path = Path.fromPortableString(s);
		if (!validateFullResourcePath(path)) {
			setErrorMessage(problemMessage);
			return false;
		}

//		String resourceName = getResource();
//		IWorkspace workspace = ResourcesPlugin.getWorkspace();
//		IStatus result = workspace.validateName(resourceName, IResource.FILE);
//		if (!result.isOK()) {
//			setErrorMessage(result.getMessage());
//			return false;
//		}

		setErrorMessage(null);
		return true;
	}

	protected boolean validateFullResourcePath(IPath resourcePath) {
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IStatus result = workspace.validatePath(resourcePath.toString(),
				IResource.FOLDER);
		if (!result.isOK()) {
			problemType = PROBLEM_PATH_INVALID;
			problemMessage = result.getMessage();
			return false;
		}
		boolean allowExistingResources = false;
		if (!allowExistingResources
				&& (workspace.getRoot().getFolder(resourcePath).exists() || workspace
						.getRoot().getFile(resourcePath).exists())) {
			problemType = PROBLEM_RESOURCE_EXIST;
			problemMessage = "File exists: " + getResource();
			return false;
		}
		return true;
	}

	protected boolean validateResourceName() {
		String resourceName = getResource();
		if (resourceName.length() == 0) {
			problemType = PROBLEM_RESOURCE_EMPTY;
			problemMessage = "Name cannot be empty";
			return false;
		}
		if (!Path.ROOT.isValidPath(resourceName)) {
			problemType = PROBLEM_NAME_INVALID;
			problemMessage = "Invalid file name";
			return false;
		}
		return true;
	}

	protected InputStream getInitialContents() {
		return null;
	}

	protected IFile createFileHandle(IPath filePath) {

		IPath p = Path.fromPortableString(resourceNameField.getText());
		return this.fInput.getFile(p);
	}

	public IFile create_new_file() {
		final IFile newFileHandle = createFileHandle(new Path(getResource()));
		final InputStream initialContents = getInitialContents();
		IRunnableWithProgress op = new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) {
				CreateFileOperation op = new CreateFileOperation(newFileHandle,
						null, initialContents, getTitle());
				try {
					// see bug
					// https://bugs.eclipse.org/bugs/show_bug.cgi?id=219901
					// directly execute the operation so that the undo state is
					// not preserved. Making this undoable resulted in too many
					// accidental file deletions.
					op.execute(monitor,
							WorkspaceUndoUtil.getUIInfoAdapter(getShell()));

				} catch (final ExecutionException e) {

					getContainer().getShell().getDisplay()
							.syncExec(new Runnable() {
								public void run() {

									EclipseMessageHelpers.show_error(e);
								}
							});
				}
			}
		};
		try {
			getContainer().run(true, true, op);

		} catch (Throwable e) {
			EclipseMessageHelpers.show_error(e);
		}
		return newFileHandle;
	}

}
