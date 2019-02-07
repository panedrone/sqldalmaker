/*
 * Copyright 2011-2018 sqldalmaker@gmail.com
 * SQL DAL Maker Website: http://sqldalmaker.sourceforge.net
 * Read LICENSE.txt in the root of this project/archive for details.
 */
package com.sqldalmaker.eclipse;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.wb.swt.ResourceManager;

import com.sqldalmaker.cg.DbUtils;
import com.sqldalmaker.common.ISelectDbSchemaCallback;

import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;

/**
 *
 * @author sqldalmaker@gmail.com
 *
 */
public class UIDialogSelectDbSchema extends TitleAreaDialog {

	private String selected_schema;
	private ArrayList<String> items;

	private Table table;
	private TableViewer tableViewer;
	private Button buttonOK;
	private Button chk_Skip;
	private Button chk_delEndingS;

	private Composite compositeCrud;
	private Button radioCrudAuto;
	private Button radioCrud;

	public enum Open_Mode {
		DTO, DAO, FK
	}

	private Open_Mode open_mode = Open_Mode.DTO;

	private boolean skip;
	private boolean include_views;
	private boolean delS;
	private boolean use_crud_auto;
	private boolean add_fk_access;
	private Button chk_add_fk_access;
	private Button chk_views;

	/**
	 * Create the dialog.
	 *
	 * @param parentShell
	 * @param dto2
	 */
	private UIDialogSelectDbSchema(Shell parentShell, IProject project, ArrayList<String> items, Open_Mode open_mode) {

		super(parentShell);

		setHelpAvailable(false);

		setShellStyle(SWT.MAX | SWT.RESIZE | SWT.TITLE | SWT.APPLICATION_MODAL);

		this.items = items;

		this.open_mode = open_mode;

	}

	static void open(Shell parentShell, IEditor2 editor2, ISelectDbSchemaCallback callback, Open_Mode open_mode)
			throws Exception {

		ArrayList<String> schema_names = new ArrayList<String>();

		Connection con = EclipseHelpers.get_connection(editor2);

		try {

			DbUtils.get_schema_names(con, schema_names);

		} finally {

			con.close();
		}

		UIDialogSelectDbSchema dlg = new UIDialogSelectDbSchema(parentShell, editor2.get_project(), schema_names,
				open_mode);

		if (dlg.open() == IDialogConstants.OK_ID) {

			callback.process_ok(dlg.selected_schema, dlg.skip, dlg.include_views, dlg.delS, dlg.use_crud_auto,
					dlg.add_fk_access);
		}
	}

	@Override
	public void create() {

		super.create();

		getContents().getShell().setText("Select DB-Schema");

		// Select DB-Schema
		setTitle("Select DB-Schema");
		// Just click OK if DB-Schema list is empty
		setMessage("Just click OK if DB-Schema list is empty.");

		buttonOK.setEnabled(items.size() <= 1);

		selected_schema = null;

		if (open_mode == Open_Mode.FK) {

			chk_Skip.setVisible(false);
			chk_views.setVisible(false);
			compositeCrud.setVisible(false);
			chk_add_fk_access.setVisible(false);

		} else if (open_mode == Open_Mode.DTO) {

			compositeCrud.setVisible(false);
			chk_add_fk_access.setVisible(false);
		}

		tableViewer.setInput(items);
	}

	@Override
	public boolean close() {

		// http://alvinalexander.com/java/jwarehouse/eclipse/org.eclipse.jface/src/org/eclipse/jface/dialogs/TrayDialog.java.shtml

		skip = chk_Skip.getSelection();
		include_views = chk_views.getSelection();
		delS = chk_delEndingS.getSelection();
		use_crud_auto = radioCrudAuto.getSelection();
		add_fk_access = chk_add_fk_access.getSelection();

		return super.close();
	}

	private class IconLabelProviders extends LabelProvider {

		Image img = ResourceManager.getImageDescriptor(IconLabelProviders.class, "/img/schema.gif").createImage();

		@Override
		public Image getImage(Object element) {

			return img; // super.getImage(element);
		}

		@Override
		public void dispose() {

			super.dispose();
			img.dispose();
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
		container.setLayout(new GridLayout(1, false));

		tableViewer = new TableViewer(container, SWT.BORDER | SWT.FULL_SELECTION);
		table = tableViewer.getTable();
		table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));

		chk_Skip = new Button(container, SWT.CHECK);
		chk_Skip.setSelection(true);
		chk_Skip.setText("Skip the tables used in existing declarations");
		skip = chk_Skip.getSelection();

		chk_views = new Button(container, SWT.CHECK);
		chk_views.setSelection(true);
		chk_views.setText("Including Views");

		chk_delEndingS = new Button(container, SWT.CHECK);
		chk_delEndingS.setSelection(true);
		chk_delEndingS.setText("English plural to singular for DTO class names");

		chk_add_fk_access = new Button(container, SWT.CHECK);
		chk_add_fk_access.setText("Including FK access code");
		chk_add_fk_access.setSelection(true);

		compositeCrud = new Composite(container, SWT.NONE);
		compositeCrud.setLayout(new GridLayout(2, false));

		radioCrud = new Button(compositeCrud, SWT.RADIO);
		radioCrud.setSelection(true);
		radioCrud.setText("crud");

		radioCrudAuto = new Button(compositeCrud, SWT.RADIO);
		radioCrudAuto.setText("crud-auto");

		delS = chk_delEndingS.getSelection();

		table.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseDoubleClick(MouseEvent e) {

				onSchemaDoubleClick();
			}
		});

		tableViewer.setContentProvider(new ArrayContentProvider());
		tableViewer.setLabelProvider(new IconLabelProviders());

		tableViewer.addSelectionChangedListener(new ISelectionChangedListener() {

			public void selectionChanged(SelectionChangedEvent evt) {

				doOnSelectionChanged();
			}
		});

		return container;
	}

	protected void onSchemaDoubleClick() {

		doOnSelectionChanged();

		setReturnCode(IDialogConstants.OK_ID);

		close();
	}

	protected void doOnSelectionChanged() {

		@SuppressWarnings("unchecked")
		List<String> items = (List<String>) tableViewer.getInput();

		boolean enabled;

		if (items.size() == 1) {

			enabled = true;

			selected_schema = items.get(0);

		} else {

			int[] indexes = table.getSelectionIndices();

			enabled = indexes.length == 1;

			if (enabled) {

				selected_schema = items.get(indexes[0]);
			}
		}

		buttonOK.setEnabled(enabled);
	}

	/**
	 * Create contents of the button bar.
	 *
	 * @param parent
	 */
	@Override
	protected void createButtonsForButtonBar(Composite parent) {

		buttonOK = createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
		buttonOK.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
			}
		});
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
	}

	/**
	 * Return the initial size of the dialog.
	 */
	@Override
	protected Point getInitialSize() {

		return new Point(450, 384);
	}
}
