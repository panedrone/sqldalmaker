/*
 * Copyright 2011-2018 sqldalmaker@gmail.com
 * SQL DAL Maker Website: http://sqldalmaker.sourceforge.net
 * Read LICENSE.txt in the root of this project/archive for details.
 */
package com.sqldalmaker.eclipse;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.events.IHyperlinkListener;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.eclipse.ui.ide.ResourceUtil;
import org.eclipse.ui.part.EditorPart;
import org.eclipse.wb.swt.SWTResourceManager;

import com.sqldalmaker.common.Const;

/**
 *
 * @author sqldalmaker@gmail.com
 *
 */
public class Editor2 extends EditorPart implements IEditor2 {

	private IFile config_file;
	private String config_file_name;

	private Composite buttons;
	private Composite composite_tabs;

	private UIEditorPageDTO editor_page_dto;
	private UIEditorPageDAO editor_page_dao;
	private UIEditorPageAdmin editor_page_admin;

	public Editor2() {

	}

	// //////////////////////////////////////////

	@Override
	public boolean isDirty() {
		return false;
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
	}

	@Override
	public void doSaveAs() {
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

	// //////////////////////////////////////////

	@Override
	public void init(IEditorSite site, IEditorInput editorInput) throws PartInitException {

		if (!(editorInput instanceof IFileEditorInput)) {

			throw new PartInitException("Invalid input: must be IFileEditorInput");
		}

		// IFileEditorInput fileEditorInput = (IFileEditorInput) editorInput;

		config_file = ResourceUtil.getFile(editorInput);

		IPath p = config_file.getFullPath();

		String file_name = p.lastSegment();

		config_file_name = file_name;

		// calls setSite and setInput:
		// super.init(site, editorInput);

		// from FormEditor
		super.setSite(site);
		super.setInput(editorInput);

		// @deprecated new code should use setPartName and setContentDescription
		// setTitle(p.toPortableString());

		IContainer parent = config_file.getParent();

		if (parent != null) {

			IPath parent_path = parent.getFullPath();

			p = p.makeRelativeTo(parent_path);

			String s = parent_path.lastSegment() + "/" + file_name;

			setPartName(s);

		} else {

			setPartName(file_name);
		}
	}

	// ////////////////////////////////////////////////

	@Override
	public String get_root_file_name() {
		return config_file_name;
	}

	@Override
	public IProject get_project() {
		return config_file.getProject();
	}

	@Override
	public IFile find_dto_xml() {
		return find_metaprogram_file(Const.DTO_XML);
	}

	@Override
	public String get_dto_xml_abs_path() throws Exception {
		return get_metaprogram_file_abs_path(Const.DTO_XML);
	}

	@Override
	public String get_dto_xsd_abs_path() throws Exception {
		return get_metaprogram_file_abs_path(Const.DTO_XSD);
	}

	@Override
	public String get_dao_xsd_abs_path() throws Exception {
		return get_metaprogram_file_abs_path(Const.DAO_XSD);
	}

	@Override
	public IFile find_metaprogram_file(String name) {
		Object member = get_metaprogram_folder().findMember(name);
		IFile res = (IFile) member;
		return res;
	}

	@Override
	public String get_metaprogram_file_abs_path(String name) throws Exception {

		IContainer res = get_metaprogram_folder();

		return get_abs_path(res) + "/" + name;
	}

	private static String get_abs_path(IResource res) throws Exception {

		if (res == null || res.getLocation() == null) {

			throw new Exception("Resource not found. Try to reopen SQL DAL Maker GUI.");
		}

		return res.getLocation().toPortableString();
	}

	@Override
	public IFile find_settings_xml() {
		return find_metaprogram_file(Const.SETTINGS_XML);
	}

	@Override
	public String get_metaprogram_folder_path_relative_to_project() throws Exception {

		IContainer res = get_metaprogram_folder();

		if (res == null) {

			throw new Exception("Internal error. Metaprogram folder not found for " + config_file.getFullPath());
		}

		// http://help.eclipse.org/luna/rtopic/org.eclipse.platform.doc.isv/reference/api/org/eclipse/core/resources/IResource.html
		// getFullPath()
		// Returns the full, absolute path of this resource relative to the
		// workspace.

		String s = res.getFullPath().makeRelativeTo(get_project().getFullPath()).toPortableString();

		return s;
	}

	@Override
	public IContainer get_metaprogram_folder() {

		IContainer parent = config_file.getParent();

		return parent;
	}

	@Override
	public String get_metaprogram_folder_abs_path() throws Exception {

		IContainer res = get_metaprogram_folder();

		if (res == null) {

			throw new Exception("Internal error. Metaprogram folder not found for " + config_file.getFullPath());
		}

		return get_abs_path(res);
	}

	//////////////////////////////////////////////////////////

	private Composite container;

	private final FormToolkit formToolkit = new FormToolkit(Display.getDefault());
	private Hyperlink hprlnkDao;
	private Hyperlink hprlnkAdmin;
	private Composite composite_toolbar;

	@Override
	public void createPartControl(Composite parent) {
		container = new Composite(parent, SWT.NONE); // additional control to
														// prevent changing BG
														// when focus is on/off
		container.setLayout(new GridLayout(1, false));

		buttons = new Composite(container, SWT.NONE);
		buttons.setLayout(new GridLayout(3, false));

		final Hyperlink hprlnkDto = formToolkit.createHyperlink(buttons, "DTO", SWT.NONE);
		hprlnkDto.setForeground(SWTResourceManager.getColor(51, 102, 153));
		hprlnkDto.addHyperlinkListener(new IHyperlinkListener() {
			public void linkActivated(HyperlinkEvent e) {
				StackLayout layout = (StackLayout) composite_tabs.getLayout();
				layout.topControl = editor_page_dto;
				composite_tabs.layout(true);

				hprlnkDto.setUnderlined(true);
				hprlnkDao.setUnderlined(false);
				hprlnkAdmin.setUnderlined(false);
			}

			public void linkEntered(HyperlinkEvent e) {
			}

			public void linkExited(HyperlinkEvent e) {
			}
		});
		formToolkit.paintBordersFor(hprlnkDto);

		hprlnkDao = formToolkit.createHyperlink(buttons, "DAO", SWT.NONE);
		hprlnkDao.setForeground(SWTResourceManager.getColor(51, 102, 153));
		hprlnkDao.addHyperlinkListener(new IHyperlinkListener() {
			public void linkActivated(HyperlinkEvent e) {

				StackLayout layout = (StackLayout) composite_tabs.getLayout();
				layout.topControl = editor_page_dao;
				composite_tabs.layout(true);

				hprlnkDto.setUnderlined(false);
				hprlnkDao.setUnderlined(true);
				hprlnkAdmin.setUnderlined(false);
			}

			public void linkEntered(HyperlinkEvent e) {
			}

			public void linkExited(HyperlinkEvent e) {
			}
		});
		hprlnkDao.setUnderlined(false);
		formToolkit.paintBordersFor(hprlnkDao);

		hprlnkAdmin = formToolkit.createHyperlink(buttons, "Admin", SWT.NONE);
		hprlnkAdmin.setForeground(SWTResourceManager.getColor(51, 102, 153));
		hprlnkAdmin.addHyperlinkListener(new IHyperlinkListener() {
			public void linkActivated(HyperlinkEvent e) {
				StackLayout layout = (StackLayout) composite_tabs.getLayout();
				layout.topControl = editor_page_admin;
				composite_tabs.layout(true);

				hprlnkDto.setUnderlined(false);
				hprlnkDao.setUnderlined(false);
				hprlnkAdmin.setUnderlined(true);
			}

			public void linkEntered(HyperlinkEvent e) {
			}

			public void linkExited(HyperlinkEvent e) {
			}
		});
		hprlnkAdmin.setUnderlined(false);
		formToolkit.paintBordersFor(hprlnkAdmin);

		composite_tabs = new Composite(container, SWT.NONE);
		composite_tabs.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		composite_tabs.setLayout(new StackLayout());

		editor_page_dto = new UIEditorPageDTO(composite_tabs, SWT.NONE);
		editor_page_dao = new UIEditorPageDAO(composite_tabs, SWT.NONE);
		editor_page_admin = new UIEditorPageAdmin(composite_tabs, SWT.NONE);

		// //////////////////////////////////////////

		editor_page_dto.setEditor2(this);
		editor_page_dao.setEditor2(this);
		editor_page_admin.setEditor2(this);

		// !!! not in constructor or in init(...
		// just after creation of pages

		Display.getDefault().asyncExec(new Runnable() {
			public void run() {
				editor_page_dto.reloadTable(false);
				editor_page_dao.reloadTable(false);
				// editorPageQueries.reloadTable(false);
				// editorPageConfiguration.loadDataInUIThreadSync();
			}
		});

		StackLayout sl_composite_tabs = (StackLayout) composite_tabs.getLayout();
		sl_composite_tabs.topControl = editor_page_dto;
		composite_tabs.layout(true);

		composite_toolbar = new Composite(buttons, SWT.NONE);
		formToolkit.adapt(composite_toolbar);
		formToolkit.paintBordersFor(composite_toolbar);
		composite_toolbar.setLayout(new FillLayout(SWT.HORIZONTAL));
	}

	@Override
	public void setFocus() {
		if (container != null) {
			// https://wiki.eclipse.org/FAQ_How_do_I_create_my_own_editor%3F
			container.setFocus(); // === panedrone: if not called, then opening
									// this editor stops opening other editors
									// with double-click
			container.setBackground(editor_page_dto.getBackground());
		}
	}
}
