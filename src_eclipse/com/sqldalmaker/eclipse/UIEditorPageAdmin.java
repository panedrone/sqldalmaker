/*
 * Copyright 2011-2021 sqldalmaker@gmail.com
 * Read LICENSE.txt in the root of this project/archive.
 * Project web-site: http://sqldalmaker.sourceforge.net
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
import com.sqldalmaker.jaxb.settings.Settings;

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
	private Action action_validate_all;
	private Text txtV;

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
		GridLayout gl_composite_top = new GridLayout(1, false);
		composite_top.setLayout(gl_composite_top);
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
				validate_all();
			}
		});
		toolkit.adapt(btnNewButton_1, true, true);
		btnNewButton_1.setText("Validate All");

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
		GridLayout gl_composite_2 = new GridLayout(1, false);
		composite_2.setLayout(gl_composite_2);
		toolkit.adapt(composite_2);
		toolkit.paintBordersFor(composite_2);

		Composite composite = new Composite(composite_2, SWT.NONE);
		toolkit.adapt(composite);
		toolkit.paintBordersFor(composite);
		composite.setLayout(new GridLayout(8, false));

		Label lblNewLabel = new Label(composite, SWT.NONE);
		toolkit.adapt(lblNewLabel, true, true);
		lblNewLabel.setText("PHP");

		Button btnNewButton_5 = new Button(composite, SWT.NONE);
		btnNewButton_5.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				EclipseResourceEditorHelpers.open_resource_file_in_editor("resources/DataStore.php", "DataStore.php");
			}
		});
		toolkit.adapt(btnNewButton_5, true, true);
		btnNewButton_5.setText("Base");

		Button btnDatastorephppdoMysql = new Button(composite, SWT.NONE);
		btnDatastorephppdoMysql.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				EclipseResourceEditorHelpers.open_resource_file_in_editor("resources/DataStore_PDO_MySQL.php",
						"DataStore_PDO_MySQL.php");
			}
		});
		btnDatastorephppdoMysql.setText("PDO, mysql");
		toolkit.adapt(btnDatastorephppdoMysql, true, true);

		Button btnDatastorephppdoOracle = new Button(composite, SWT.NONE);
		btnDatastorephppdoOracle.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				EclipseResourceEditorHelpers.open_resource_file_in_editor("resources/DataStore_PDO_Oracle.php",
						"DataStore_PDO_Oracle.php");
			}
		});
		btnDatastorephppdoOracle.setText("PDO, oracle");
		toolkit.adapt(btnDatastorephppdoOracle, true, true);

		Button btnDatastorephpociOracle = new Button(composite, SWT.NONE);
		btnDatastorephpociOracle.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				EclipseResourceEditorHelpers.open_resource_file_in_editor("resources/DataStore_oci8.php",
						"DataStore_oci8.php");
			}
		});
		btnDatastorephpociOracle.setText("oci8");
		toolkit.adapt(btnDatastorephpociOracle, true, true);

		Button btnDatastorephppdoPostgresql = new Button(composite, SWT.NONE);
		btnDatastorephppdoPostgresql.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				EclipseResourceEditorHelpers.open_resource_file_in_editor("resources/DataStore_PDO_PostgreSQL.php",
						"DataStore_PDO_PostgreSQL.php");
			}
		});
		btnDatastorephppdoPostgresql.setText("PDO, postgresql");
		toolkit.adapt(btnDatastorephppdoPostgresql, true, true);

		Button btnDatastorephppdoSql = new Button(composite, SWT.NONE);
		btnDatastorephppdoSql.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				EclipseResourceEditorHelpers.open_resource_file_in_editor("resources/DataStore_PDO_SQL_Server.php",
						"DataStore_PDO_SQL_Server.php");
			}
		});
		btnDatastorephppdoSql.setText("PDO, mssql");
		toolkit.adapt(btnDatastorephppdoSql, true, true);

		Button btnNewButton_12 = new Button(composite, SWT.NONE);
		btnNewButton_12.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				EclipseResourceEditorHelpers.open_resource_file_in_editor("resources/DataStore_PDO_SQLite3.php",
						"DataStore_PDO_SQLite3.php");
			}
		});
		toolkit.adapt(btnNewButton_12, true, true);
		btnNewButton_12.setText("PDO, sqlite3");

		Composite composite_4 = new Composite(composite_2, SWT.NONE);
		composite_4.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false, 1, 1));
		toolkit.adapt(composite_4);
		toolkit.paintBordersFor(composite_4);
		composite_4.setLayout(new GridLayout(8, false));

		Label lblJava = new Label(composite_4, SWT.NONE);
		toolkit.adapt(lblJava, true, true);
		lblJava.setText("Java");

		Button btnDatastorejava = new Button(composite_4, SWT.NONE);
		btnDatastorejava.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				EclipseResourceEditorHelpers.open_resource_file_in_editor("resources/DataStore.java_",
						"DataStore.java");
			}
		});
		btnDatastorejava.setText("Base");
		toolkit.adapt(btnDatastorejava, true, true);

		Button btnDatastoreJavaDbutils = new Button(composite_4, SWT.NONE);
		btnDatastoreJavaDbutils.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				EclipseResourceEditorHelpers.open_resource_file_in_editor("resources/DataStoreManagerJDBC.java_",
						"DataStoreManager.java");
			}
		});
		btnDatastoreJavaDbutils.setText("JDBC");
		toolkit.adapt(btnDatastoreJavaDbutils, true, true);

		Button btnNewButton_4 = new Button(composite_4, SWT.NONE);
		btnNewButton_4.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				EclipseResourceEditorHelpers.open_resource_file_in_editor("resources/DataStoreManagerAndroid.java_",
						"DataStoreManager.java");
			}
		});
		toolkit.adapt(btnNewButton_4, true, true);
		btnNewButton_4.setText("Android");

		Label lblC = new Label(composite_4, SWT.NONE);
		lblC.setAlignment(SWT.RIGHT);
		toolkit.adapt(lblC, true, true);
		lblC.setText("C++");

		Button btnDatastoreCQt = new Button(composite_4, SWT.NONE);
		btnDatastoreCQt.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				EclipseResourceEditorHelpers.open_resource_file_in_editor("resources/DataStore_Qt5.cpp",
						"DataStore.cpp");
				EclipseResourceEditorHelpers.open_resource_file_in_editor("resources/DataStore_Qt5.h", "DataStore.h");
			}
		});
		toolkit.adapt(btnDatastoreCQt, true, true);
		btnDatastoreCQt.setText("QtSql");

		Button btnDatastoreCStl = new Button(composite_4, SWT.NONE);
		btnDatastoreCStl.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				EclipseResourceEditorHelpers.open_resource_file_in_editor("resources/DataStore.STL.cpp",
						"DataStore.cpp");
				EclipseResourceEditorHelpers.open_resource_file_in_editor("resources/DataStore.STL.h", "DataStore.h");
			}
		});
		toolkit.adapt(btnDatastoreCStl, true, true);
		btnDatastoreCStl.setText("STL, sqlite3");

		Button btnNewButton_11 = new Button(composite_4, SWT.NONE);
		btnNewButton_11.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				EclipseResourceEditorHelpers.open_resource_file_in_editor("resources/DataStore.cpp", "DataStore.cpp");
				EclipseResourceEditorHelpers.open_resource_file_in_editor("resources/DataStore.h", "DataStore.h");
			}
		});
		toolkit.adapt(btnNewButton_11, true, true);
		btnNewButton_11.setText("ATL, sqlite3");

		Composite composite_5 = new Composite(composite_2, SWT.NONE);
		composite_5.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false, 1, 1));
		toolkit.adapt(composite_5);
		toolkit.paintBordersFor(composite_5);
		composite_5.setLayout(new GridLayout(10, false));

		Label lblPython = new Label(composite_5, SWT.NONE);
		toolkit.adapt(lblPython, true, true);
		lblPython.setText("Python");

		Button btnNewButton_7 = new Button(composite_5, SWT.NONE);
		btnNewButton_7.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				EclipseResourceEditorHelpers.open_resource_file_in_editor("resources/data_store_mysql.py",
						"data_store.py");
			}
		});
		toolkit.adapt(btnNewButton_7, true, true);
		btnNewButton_7.setText("mysql");

		Button btnDatastorepycxoracle = new Button(composite_5, SWT.NONE);
		btnDatastorepycxoracle.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				EclipseResourceEditorHelpers.open_resource_file_in_editor("resources/data_store_cx_oracle.py",
						"data_store_cx_oracle.py");
			}
		});
		btnDatastorepycxoracle.setText("cx_Oracle");
		toolkit.adapt(btnDatastorepycxoracle, true, true);

		Button btnNewButton_7_1_1 = new Button(composite_5, SWT.NONE);
		btnNewButton_7_1_1.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				EclipseResourceEditorHelpers.open_resource_file_in_editor("resources/data_store_psycopg2.py",
						"data_store_psycopg2.py");
			}
		});
		btnNewButton_7_1_1.setText("psycopg2");
		toolkit.adapt(btnNewButton_7_1_1, true, true);

		Button btnNewButton_7_1 = new Button(composite_5, SWT.NONE);
		btnNewButton_7_1.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				EclipseResourceEditorHelpers.open_resource_file_in_editor("resources/data_store_pyodbc.py",
						"data_store_pyodbc.py");
			}
		});
		btnNewButton_7_1.setText("pyodbc, mssql");
		toolkit.adapt(btnNewButton_7_1, true, true);

		Button btnNewButton_6 = new Button(composite_5, SWT.NONE);
		btnNewButton_6.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				EclipseResourceEditorHelpers.open_resource_file_in_editor("resources/data_store_sqlite3.py",
						"data_store.py");
			}
		});
		toolkit.adapt(btnNewButton_6, true, true);
		btnNewButton_6.setText("sqlite3");
		
				Label lblRuby = new Label(composite_5, SWT.NONE);
				toolkit.adapt(lblRuby, true, true);
				lblRuby.setText("Ruby");
		
				Button btnNewButton_8 = new Button(composite_5, SWT.NONE);
				btnNewButton_8.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						EclipseResourceEditorHelpers.open_resource_file_in_editor("resources/data_store.rb", "data_store.rb");
					}
				});
				toolkit.adapt(btnNewButton_8, true, true);
				btnNewButton_8.setText("DBI");

		Label lblGo = new Label(composite_5, SWT.NONE);
		toolkit.adapt(lblGo, true, true);
		lblGo.setText("Go");

		Button btnNewButton_8_1 = new Button(composite_5, SWT.NONE);
		btnNewButton_8_1.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				// ending '_' because of bugs
				EclipseResourceEditorHelpers.open_resource_file_in_editor("resources/data_store.go", "data_store.go_");
			}
		});
		btnNewButton_8_1.setText("database/sql");
		toolkit.adapt(btnNewButton_8_1, true, true);

		Composite composite_3 = new Composite(composite_top, SWT.NONE);
		composite_3.setLayout(new GridLayout(6, false));

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
						"go.vm");
			}
		});
		button_1.setText("go.vm");
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

		Button button_4_1 = new Button(composite_3, SWT.NONE);
		button_4_1.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				EclipseResourceEditorHelpers.open_resource_file_in_text_editor("com/sqldalmaker/cg/go/go.vm", "go.vm");
			}
		});
		button_4_1.setText("go.vm");
		toolkit.adapt(button_4_1, true, true);

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

	private static String get_err_msg(Throwable ex) {
		return ex.getClass().getName() + " -> " + ex.getMessage(); // printStackTrace();
	}

	private void validate_all() {
		EclipseConsoleHelpers.init_console();
		StringBuilder buff = new StringBuilder();
		Settings sett = null;
		if (check_xsd(buff, Const.SETTINGS_XSD)) {
			try {
				sett = EclipseHelpers.load_settings(editor2);
				add_ok_msg(buff, Const.SETTINGS_XML);
			} catch (Exception ex) {
				add_err_msg(buff, "Invalid " + Const.SETTINGS_XML + ": " + get_err_msg(ex));
			}
		} else {
			add_err_msg(buff, "Cannot load " + Const.SETTINGS_XML + " because of invalid " + Const.SETTINGS_XSD);
		}
		check_xsd(buff, Const.DTO_XSD);
		check_xsd(buff, Const.DAO_XSD);
		if (sett == null) {
			add_err_msg(buff, "Test connection -> failed because of invalid settings");
		} else {
			try {
				Connection con = EclipseHelpers.get_connection(editor2);
				con.close();
				add_ok_msg(buff, "Test connection");
			} catch (Exception ex) {
				add_err_msg(buff, "Test connection: " + get_err_msg(ex));
			}
		}
		EclipseMessageHelpers.show_info(buff.toString());
	}

	private boolean check_xsd(StringBuilder buff, String xsd_name) {
		IFile xsd = editor2.find_metaprogram_file(xsd_name);
		if (xsd == null) {
			add_err_msg(buff, "File not found: " + xsd_name);
			return false;
		} else {
			String cur_text;
			try {
				String xsd_abs_path = editor2.get_metaprogram_file_abs_path(xsd_name);
				cur_text = Helpers.load_text_from_file(xsd_abs_path);
			} catch (Exception ex) {
				add_err_msg(buff, get_err_msg(ex));
				return false;
			}
			String ref_text;
			try {
				ref_text = EclipseHelpers.read_from_resource_folder(xsd_name);
			} catch (Exception ex) {
				add_err_msg(buff, get_err_msg(ex));
				return false;
			}
			if (ref_text.equals(cur_text)) {
				add_ok_msg(buff, xsd_name);
			} else {
				add_err_msg(buff, xsd_name + " is out-of-date! Use 'Create/Overwrite XSD files'");
				return false;
			}
		}
		return true;
	}

	private void add_ok_msg(StringBuilder buff, String msg) {
		buff.append("\r\n");
		buff.append(msg);
		buff.append(" -> OK");
		EclipseConsoleHelpers.add_info_msg(msg + " -> OK");
	}

	private void add_err_msg(StringBuilder buff, String msg) {
		buff.append("\r\n[ERROR] ");
		buff.append(msg);
		EclipseConsoleHelpers.add_error_msg(msg);
	}

	protected void test_connection() {
		try {
			Connection conn = EclipseHelpers.get_connection(editor2);
			conn.close();
			EclipseMessageHelpers.show_info("Test connection succeeded.");
		} catch (Throwable ex) {
			ex.printStackTrace();
			EclipseMessageHelpers.show_error(ex);
		}
	}

	protected void editSettings() {
		try {
			IFile file = editor2.find_settings_xml();
			EclipseEditorHelpers.open_editor_sync(getShell(), file);
		} catch (Throwable ex) {
			ex.printStackTrace();
			EclipseMessageHelpers.show_error(ex);
		}
	}

	private void createActions() {
		action_settings_xml = new Action("") {
			@Override
			public void run() {
				editSettings();
			}
		};
		action_settings_xml.setToolTipText("Edit settings.xml");
		action_settings_xml
				.setImageDescriptor(ResourceManager.getImageDescriptor(UIEditorPageAdmin.class, "/img/XMLFile.gif"));
		action_test_conn = new Action("") {
			@Override
			public void run() {
				test_connection();
			}
		};
		action_test_conn.setToolTipText("Test connection");
		action_test_conn
				.setImageDescriptor(ResourceManager.getImageDescriptor(UIEditorPageAdmin.class, "/img/connection.gif"));
		action_validate_all = new Action("") {
			@Override
			public void run() {
				validate_all();
			}
		};
		action_validate_all.setToolTipText("Validate All");
		action_validate_all
				.setImageDescriptor(ResourceManager.getImageDescriptor(UIEditorPageAdmin.class, "/img/validate.gif"));
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
