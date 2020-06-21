/*
 * Copyright 2011-2020 sqldalmaker@gmail.com
 * SQL DAL Maker Website: http://sqldalmaker.sourceforge.net
 * Read LICENSE.txt in the root of this project/archive for details.
 */
package com.sqldalmaker.eclipse;

import java.sql.Connection;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.action.Action;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.wb.swt.ResourceManager;
import org.osgi.framework.Bundle;
import org.osgi.framework.Version;

import com.sqldalmaker.cg.Helpers;
import com.sqldalmaker.common.Const;

/**
 *
 * @author sqldalmaker@gmail.com
 *
 */
public class UIEditorPageAdmin extends Composite {

	private final FormToolkit toolkit = new FormToolkit(Display.getCurrent());
	private Text text_1;

	private IEditor2 editor2;
	private Action action_settings_xml;
	private Action action_test_conn;
	private Text txtV;

	protected void testConnection() {

		try {

			Connection conn = EclipseHelpers.get_connection(editor2);

			conn.close();

			EclipseMessageHelpers.show_info("Test connection succeeded.");

		} catch (Throwable ex) {

			ex.printStackTrace();

			EclipseMessageHelpers.show_error(ex);
		}
	}

	/**
	 * Create the composite.
	 *
	 * @param parent
	 * @param style
	 */
	public UIEditorPageAdmin(Composite parent, int style) {
		super(parent, style);
		createActions();
		addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				toolkit.dispose();
			}
		});
		toolkit.adapt(this);
		toolkit.paintBordersFor(this);
		setLayout(new FillLayout(SWT.HORIZONTAL));

		Composite composite_top = new Composite(this, SWT.NONE);
		composite_top.setLayout(new GridLayout(1, false));
		toolkit.adapt(composite_top);
		toolkit.paintBordersFor(composite_top);

		Composite composite_0 = new Composite(composite_top, SWT.NONE);
		composite_0.setLayout(new GridLayout(5, false));
		toolkit.adapt(composite_0);
		toolkit.paintBordersFor(composite_0);

		Button btnNewButton = new Button(composite_0, SWT.NONE);
		btnNewButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				editSettings();
			}
		});
		toolkit.adapt(btnNewButton, true, true);
		btnNewButton.setText("Edit settings.xml");

		Button btnReferenceSettingsxml = new Button(composite_0, SWT.NONE);
		btnReferenceSettingsxml.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				EclipseResourceEditorHelpers.open_resource_file_in_editor("resources/" + Const.SETTINGS_XML,
						"reference_" + Const.SETTINGS_XML);
			}
		});
		btnReferenceSettingsxml.setText("Reference settings.xml");
		toolkit.adapt(btnReferenceSettingsxml, true, true);

		Button btnNewButton_1 = new Button(composite_0, SWT.NONE);
		btnNewButton_1.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				testConnection();
			}
		});
		toolkit.adapt(btnNewButton_1, true, true);
		btnNewButton_1.setText("Test connection");

		Composite composite_1 = new Composite(composite_top, SWT.NONE);
		composite_1.setLayout(new GridLayout(3, false));
		toolkit.adapt(composite_1);
		toolkit.paintBordersFor(composite_1);

		Button btnCreateoverwriteXsdFiles = new Button(composite_1, SWT.NONE);
		btnCreateoverwriteXsdFiles.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				EclipseMetaProgramHelpers.create_overwrite_xsd(editor2);
			}
		});
		btnCreateoverwriteXsdFiles.setText("Create/Overwrite XSD files");
		toolkit.adapt(btnCreateoverwriteXsdFiles, true, true);

		Button btnCreateoverwriteSettingsxml = new Button(composite_1, SWT.NONE);
		btnCreateoverwriteSettingsxml.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				EclipseMetaProgramHelpers.create_overwrite_settings_xml(editor2);
			}
		});
		btnCreateoverwriteSettingsxml.setText("Create/Overwrite settings.xml");
		toolkit.adapt(btnCreateoverwriteSettingsxml, true, true);

		Button btnNewButton_2 = new Button(composite_1, SWT.NONE);
		btnNewButton_2.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				EclipseMetaProgramHelpers.create_dto_xml(editor2);
			}
		});
		toolkit.adapt(btnNewButton_2, true, true);
		btnNewButton_2.setText("Create/Overwrite dto.xml");

		Composite composite_2 = new Composite(composite_top, SWT.NONE);
		composite_2.setLayout(new GridLayout(4, false));
		toolkit.adapt(composite_2);
		toolkit.paintBordersFor(composite_2);

		Button btnNewButton_5 = new Button(composite_2, SWT.NONE);
		btnNewButton_5.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				EclipseResourceEditorHelpers.open_resource_file_in_editor("resources/DataStore.php", "DataStore.php");
			}
		});
		btnNewButton_5.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
		toolkit.adapt(btnNewButton_5, true, true);
		btnNewButton_5.setText("DataStore.php");

		Button btnNewButton_12 = new Button(composite_2, SWT.NONE);
		btnNewButton_12.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				EclipseResourceEditorHelpers.open_resource_file_in_editor("resources/PDODataStore.php",
						"DataStore.php");
			}
		});
		btnNewButton_12.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
		toolkit.adapt(btnNewButton_12, true, true);
		btnNewButton_12.setText("DataStore.php (PDO)");

		Button btnDatastorephppdoPostgresql = new Button(composite_2, SWT.NONE);
		btnDatastorephppdoPostgresql.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
		btnDatastorephppdoPostgresql.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				EclipseResourceEditorHelpers.open_resource_file_in_editor("resources/DataStore_PDO_PostgreSQL.php",
						"DataStore_PDO_PostgreSQL.php");
			}
		});
		btnDatastorephppdoPostgresql.setText("DataStore.php (PDO, PostgreSQL)");
		toolkit.adapt(btnDatastorephppdoPostgresql, true, true);
		new Label(composite_2, SWT.NONE);

		Button btnDatastorejava = new Button(composite_2, SWT.NONE);
		btnDatastorejava.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				EclipseResourceEditorHelpers.open_resource_file_in_editor("resources/DataStore.java_",
						"DataStore.java");
			}
		});
		btnDatastorejava.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
		btnDatastorejava.setText("DataStore.java");
		toolkit.adapt(btnDatastorejava, true, true);

		Button btnDatastoreJavaDbutils = new Button(composite_2, SWT.NONE);
		btnDatastoreJavaDbutils.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				EclipseResourceEditorHelpers.open_resource_file_in_editor("resources/DataStoreManagerDbUtils.java_",
						"DataStoreManager.java");
			}
		});
		btnDatastoreJavaDbutils.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
		btnDatastoreJavaDbutils.setText("DataStoreManager.java (Apache DbUtils)");
		toolkit.adapt(btnDatastoreJavaDbutils, true, true);

		Button btnDatastoreJavaSpring = new Button(composite_2, SWT.NONE);
		btnDatastoreJavaSpring.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				EclipseResourceEditorHelpers.open_resource_file_in_editor("resources/DataStoreManagerSpring.java_",
						"DataStoreManager.java");
			}
		});
		btnDatastoreJavaSpring.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
		btnDatastoreJavaSpring.setText("DataStoreManager.java (Spring JDBC)");
		toolkit.adapt(btnDatastoreJavaSpring, true, true);

		Button btnNewButton_4 = new Button(composite_2, SWT.NONE);
		btnNewButton_4.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				EclipseResourceEditorHelpers.open_resource_file_in_editor("resources/DataStoreManagerAndroid.java_",
						"DataStoreManager.java");
			}
		});
		btnNewButton_4.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
		toolkit.adapt(btnNewButton_4, true, true);
		btnNewButton_4.setText("DataStoreManager.java (Android)");

		Button btnDatastoreCQt = new Button(composite_2, SWT.NONE);
		btnDatastoreCQt.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				EclipseResourceEditorHelpers.open_resource_file_in_editor("resources/DataStore_Qt5.cpp",
						"DataStore.cpp");
				EclipseResourceEditorHelpers.open_resource_file_in_editor("resources/DataStore_Qt5.h", "DataStore.h");
			}
		});
		btnDatastoreCQt.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
		toolkit.adapt(btnDatastoreCQt, true, true);
		btnDatastoreCQt.setText("DataStore, C++ (Qt5, Qt5Sql)");

		Button btnNewButton_11 = new Button(composite_2, SWT.NONE);
		btnNewButton_11.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				EclipseResourceEditorHelpers.open_resource_file_in_editor("resources/DataStore.cpp", "DataStore.cpp");
				EclipseResourceEditorHelpers.open_resource_file_in_editor("resources/DataStore.h", "DataStore.h");
			}
		});
		btnNewButton_11.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
		toolkit.adapt(btnNewButton_11, true, true);
		btnNewButton_11.setText("DataStore, C++ (ATL, , SQLite3)");

		Button btnDatastoreCStl = new Button(composite_2, SWT.NONE);
		btnDatastoreCStl.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				EclipseResourceEditorHelpers.open_resource_file_in_editor("resources/DataStore.STL.cpp",
						"DataStore.cpp");
				EclipseResourceEditorHelpers.open_resource_file_in_editor("resources/DataStore.STL.h", "DataStore.h");
			}
		});
		btnDatastoreCStl.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
		toolkit.adapt(btnDatastoreCStl, true, true);
		btnDatastoreCStl.setText("DataStore, C++ (STL, , SQLite3)");
		new Label(composite_2, SWT.NONE);

		Button btnNewButton_6 = new Button(composite_2, SWT.NONE);
		btnNewButton_6.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				EclipseResourceEditorHelpers.open_resource_file_in_editor("resources/DataStore1.py", "DataStore.py");
			}
		});
		btnNewButton_6.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
		toolkit.adapt(btnNewButton_6, true, true);
		btnNewButton_6.setText("DataStore.py (SQLite3)");

		Button btnNewButton_7 = new Button(composite_2, SWT.NONE);
		btnNewButton_7.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				EclipseResourceEditorHelpers.open_resource_file_in_editor("resources/DataStore2.py", "DataStore.py");
			}
		});
		btnNewButton_7.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
		toolkit.adapt(btnNewButton_7, true, true);
		btnNewButton_7.setText("DataStore.py (MySQL)");

		Button btnNewButton_7_1 = new Button(composite_2, SWT.NONE);
		btnNewButton_7_1.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				EclipseResourceEditorHelpers.open_resource_file_in_editor("resources/DataStore_pyodbc.py",
						"DataStore_pyodbc.py");
			}
		});
		btnNewButton_7_1.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
		btnNewButton_7_1.setText("DataStore.py (pyodbc)");
		toolkit.adapt(btnNewButton_7_1, true, true);

		Button btnNewButton_7_1_1 = new Button(composite_2, SWT.NONE);
		btnNewButton_7_1_1.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				EclipseResourceEditorHelpers.open_resource_file_in_editor("resources/DataStore_psycopg2.py",
						"DataStore_psycopg2.py");
			}
		});
		btnNewButton_7_1_1.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
		btnNewButton_7_1_1.setText("DataStore.py (psycopg2)");
		toolkit.adapt(btnNewButton_7_1_1, true, true);

		Button btnNewButton_8 = new Button(composite_2, SWT.NONE);
		btnNewButton_8.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				EclipseResourceEditorHelpers.open_resource_file_in_editor("resources/data_store.rb", "data_store.rb");
			}
		});
		btnNewButton_8.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
		toolkit.adapt(btnNewButton_8, true, true);
		btnNewButton_8.setText("data_store.rb (DBI)");
		new Label(composite_2, SWT.NONE);
		new Label(composite_2, SWT.NONE);
		new Label(composite_2, SWT.NONE);

		Composite composite_3 = new Composite(composite_top, SWT.NONE);
		composite_3.setLayout(new GridLayout(5, false));

		Button btnRecentChanges = new Button(composite_0, SWT.NONE);
		btnRecentChanges.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				EclipseResourceEditorHelpers.open_resource_file_in_editor("recent_changes.txt", "recent_changes.txt");
			}
		});
		toolkit.adapt(btnRecentChanges, true, true);
		btnRecentChanges.setText("Recent changes");

		txtV = new Text(composite_0, SWT.CENTER);
		txtV.setEditable(false);
		txtV.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false, 1, 1));
		toolkit.adapt(txtV, true, true);
		toolkit.adapt(composite_3);
		toolkit.paintBordersFor(composite_3);

		Button button = new Button(composite_3, SWT.NONE);
		button.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				EclipseResourceEditorHelpers.open_resource_file_in_text_editor("com/sqldalmaker/cg/php/php.vm",
						"php.vm");
			}
		});
		button.setText("php.vm");
		toolkit.adapt(button, true, true);

		Button button_1 = new Button(composite_3, SWT.NONE);
		button_1.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				EclipseResourceEditorHelpers.open_resource_file_in_text_editor("com/sqldalmaker/cg/java/java.vm",
						"java.vm");
			}
		});
		button_1.setText("java.vm");
		toolkit.adapt(button_1, true, true);

		Button button_2 = new Button(composite_3, SWT.NONE);
		button_2.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				EclipseResourceEditorHelpers.open_resource_file_in_text_editor("com/sqldalmaker/cg/cpp/cpp.vm",
						"cpp.vm");
			}
		});
		button_2.setText("cpp.vm");
		toolkit.adapt(button_2, true, true);

		Button button_3 = new Button(composite_3, SWT.NONE);
		button_3.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				EclipseResourceEditorHelpers.open_resource_file_in_text_editor("com/sqldalmaker/cg/python/python.vm",
						"python.vm");
			}
		});
		button_3.setText("python.vm");
		toolkit.adapt(button_3, true, true);

		Button button_4 = new Button(composite_3, SWT.NONE);
		button_4.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				EclipseResourceEditorHelpers.open_resource_file_in_text_editor("com/sqldalmaker/cg/ruby/ruby.vm",
						"ruby.vm");
			}
		});
		button_4.setText("ruby.vm");
		toolkit.adapt(button_4, true, true);

		Composite composite_text = new Composite(composite_top, SWT.NONE);
		composite_text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
		composite_text.setLayout(new GridLayout(1, false));
		toolkit.adapt(composite_text);
		toolkit.paintBordersFor(composite_text);

		text_1 = new Text(composite_text, SWT.BORDER | SWT.MULTI);
		text_1.setEditable(false);
		text_1.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		toolkit.adapt(text_1, true, true);
	}

	protected void editSettings() {
		try {

			IFile file = editor2.find_settings_xml();

			EclipseEditorHelpers.open_editor_sync(getShell(), file, true);

		} catch (Throwable ex) {

			ex.printStackTrace();

			EclipseMessageHelpers.show_error(ex);
		}
	}

	private void createActions() {
		{
			action_settings_xml = new Action("") {

				@Override
				public void run() {
					editSettings();
				}
			};
			action_settings_xml.setToolTipText("Edit settings.xml");
			action_settings_xml.setImageDescriptor(
					ResourceManager.getImageDescriptor(UIEditorPageAdmin.class, "/img/XMLFile.gif"));
		}
		{
			action_test_conn = new Action("") {

				@Override
				public void run() {
					testConnection();
				}
			};
			action_test_conn.setToolTipText("Test connection");
			action_test_conn.setImageDescriptor(
					ResourceManager.getImageDescriptor(UIEditorPageAdmin.class, "/img/connection.gif"));
		}
	}

	public void init_runtime() {
		try {

			Bundle bundle = Platform.getBundle(Activator.PLUGIN_ID);
			Version version = bundle.getVersion();
			String v = String.format("%d.%d.%d.%s", version.getMajor(), version.getMinor(), version.getMicro(),
					version.getQualifier());

			String jv = System.getProperty("java.version");

			txtV.setText(v + " on Java " + jv);

			String text = Helpers.read_from_jar_file_2("ABOUT.txt");
			// text += "\r\n" + v;
			text_1.setText(text);

		} catch (Exception e1) {
			e1.printStackTrace();
		}
	}

	public void setEditor2(Editor2 ed) {
		editor2 = ed;
	}
}
