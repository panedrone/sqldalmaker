/*
	Copyright 2011-2024 sqldalmaker@gmail.com
	Read LICENSE.txt in the root of this project/archive.
	Project web-site: https://sqldalmaker.sourceforge.net/
*/
package com.sqldalmaker.eclipse;

import java.sql.Connection;

import org.eclipse.core.resources.IFile;
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
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.wb.swt.ResourceManager;
import org.eclipse.wb.swt.SWTResourceManager;

import com.sqldalmaker.cg.Helpers;
import com.sqldalmaker.common.Const;
import com.sqldalmaker.common.XmlHelpers;
import com.sqldalmaker.jaxb.settings.Settings;

/**
 *
 * @author sqldalmaker@gmail.com
 *
 */
public class UIEditorPageAdmin extends Composite {

	private final FormToolkit toolkit = new FormToolkit(Display.getCurrent());

	private IEditor2 editor2;
	private Action action_test_conn;
	private Action action_validate_all;
	private Text txtV;
	private Text txt_migrate;

	private Composite composite_8;

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

		Button btnSettingsxml = new Button(composite_0, SWT.NONE);
		btnSettingsxml.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				try {
					Shell active_shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
					IFile file = editor2.find_settings_xml();
					EclipseEditorHelpers.open_editor_sync(active_shell, file);
				} catch (Throwable ex) {
					ex.printStackTrace();
					EclipseMessageHelpers.show_error(ex);
				}
			}
		});
		toolkit.adapt(btnSettingsxml, true, true);
		btnSettingsxml.setText("settings.xml");

		Button btnNewButton_1 = new Button(composite_0, SWT.NONE);
		btnNewButton_1.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				validate_all();
			}
		});
		toolkit.adapt(btnNewButton_1, true, true);
		btnNewButton_1.setText("Validate Configuration");

		txtV = new Text(composite_0, SWT.CENTER);
		txtV.setEditable(false);
		txtV.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false, 1, 1));
		toolkit.adapt(txtV, true, true);

		Button btnRecentChanges = new Button(composite_0, SWT.NONE);
		btnRecentChanges.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				EclipseResourceEditorHelpers.open_resource_file_in_editor("recent_changes.txt", "recent_changes.txt");
			}
		});
		toolkit.adapt(btnRecentChanges, true, true);
		btnRecentChanges.setText("News");

		Button btnAbout = new Button(composite_0, SWT.NONE);
		btnAbout.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				UIDialogAbout.show_modal(getShell());
			}
		});
		toolkit.adapt(btnAbout, true, true);
		btnAbout.setText("About");

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

		Button btnNewButton_2 = new Button(composite_1, SWT.NONE);
		btnNewButton_2.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				EclipseMetaProgramHelpers.create_sdm_xml(editor2);
			}
		});
		toolkit.adapt(btnNewButton_2, true, true);
		btnNewButton_2.setText("Create sdm.xml");

		Button btnCreateoverwriteSettingsxml = new Button(composite_1, SWT.NONE);
		btnCreateoverwriteSettingsxml.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				EclipseMetaProgramHelpers.create_overwrite_settings_xml(editor2);
			}
		});
		btnCreateoverwriteSettingsxml.setText("Create settings.xml");
		toolkit.adapt(btnCreateoverwriteSettingsxml, true, true);

		Composite composite_2 = new Composite(composite_top, SWT.NONE);
		GridLayout gl_composite_2 = new GridLayout(1, false);
		composite_2.setLayout(gl_composite_2);
		toolkit.adapt(composite_2);
		toolkit.paintBordersFor(composite_2);

		Composite composite = new Composite(composite_2, SWT.NONE);
		toolkit.adapt(composite);
		toolkit.paintBordersFor(composite);
		composite.setLayout(new GridLayout(10, false));

		Label lblNewLabel = new Label(composite, SWT.NONE);
		toolkit.adapt(lblNewLabel, true, true);
		lblNewLabel.setText("PHP");

		Button btnDatastorephppdoMysql = new Button(composite, SWT.NONE);
		btnDatastorephppdoMysql.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				EclipseResourceEditorHelpers.open_resource_file_in_editor("resources/DataStore_PDO_MySQL.php",
						"DataStore_PDO_MySQL.php");
			}
		});
		btnDatastorephppdoMysql.setText("mysql");
		toolkit.adapt(btnDatastorephppdoMysql, true, true);

		Button btnDatastorephppdoOracle = new Button(composite, SWT.NONE);
		btnDatastorephppdoOracle.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				EclipseResourceEditorHelpers.open_resource_file_in_editor("resources/DataStore_PDO_Oracle.php",
						"DataStore_PDO_Oracle.php");
			}
		});
		btnDatastorephppdoOracle.setText("oracle, pdo");
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
		btnDatastorephppdoPostgresql.setText("postgresql");
		toolkit.adapt(btnDatastorephppdoPostgresql, true, true);

		Button btnDatastorephppdoSql = new Button(composite, SWT.NONE);
		btnDatastorephppdoSql.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				EclipseResourceEditorHelpers.open_resource_file_in_editor("resources/DataStore_PDO_SQL_Server.php",
						"DataStore_PDO_SQL_Server.php");
			}
		});
		btnDatastorephppdoSql.setText("ms sql");
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
		btnNewButton_12.setText("sqlite3");

		Button button_1_9 = new Button(composite, SWT.NONE);
		button_1_9.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				EclipseResourceEditorHelpers.open_resource_file_in_editor(
						"resources/DataStore_PDO_SQLite3.php.settings.xml", Const.SETTINGS_XML);
			}
		});
		button_1_9.setToolTipText("settings.xml");
		button_1_9.setImage(SWTResourceManager.getImage(UIEditorPageAdmin.class, "/img/xmldoc_12x12.gif"));
		toolkit.adapt(button_1_9, true, true);

		Button btnDoctrineOrm = new Button(composite, SWT.NONE);
		btnDoctrineOrm.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				EclipseResourceEditorHelpers.open_resource_file_in_editor("resources/DataStore_Doctrine_ORM.php",
						"DataStore_Doctrine_ORM.php");
			}
		});
		toolkit.adapt(btnDoctrineOrm, true, true);
		btnDoctrineOrm.setText("Doctrine");

		Button button_1_10 = new Button(composite, SWT.NONE);
		button_1_10.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				EclipseResourceEditorHelpers.open_resource_file_in_editor(
						"resources/DataStore_Doctrine_ORM.php.settings.xml", Const.SETTINGS_XML);
			}
		});
		button_1_10.setToolTipText("settings.xml");
		button_1_10.setImage(SWTResourceManager.getImage(UIEditorPageAdmin.class, "/img/xmldoc_12x12.gif"));
		toolkit.adapt(button_1_10, true, true);

		Composite composite_4 = new Composite(composite_2, SWT.NONE);
		toolkit.adapt(composite_4);
		toolkit.paintBordersFor(composite_4);
		composite_4.setLayout(new GridLayout(7, false));

		Label lblJava = new Label(composite_4, SWT.NONE);
		toolkit.adapt(lblJava, true, true);
		lblJava.setText("Java");

		Button btnDatastorejava = new Button(composite_4, SWT.NONE);
		btnDatastorejava.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				EclipseResourceEditorHelpers.open_resource_file_in_editor("resources/DataStore_JDBC.java_",
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

		Button button_1_4 = new Button(composite_4, SWT.NONE);
		button_1_4.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				EclipseResourceEditorHelpers.open_resource_file_in_editor("resources/DataStore.java.JDBC.settings.xml",
						Const.SETTINGS_XML);
			}
		});
		button_1_4.setToolTipText("settings.xml");
		button_1_4.setImage(SWTResourceManager.getImage(UIEditorPageAdmin.class, "/img/xmldoc_12x12.gif"));
		toolkit.adapt(button_1_4, true, true);

		Button btnNewButton_5 = new Button(composite_4, SWT.NONE);
		btnNewButton_5.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				EclipseResourceEditorHelpers.open_resource_file_in_editor("resources/DataStore_Android.java_",
						"DataStore.java");
			}
		});
		toolkit.adapt(btnNewButton_5, true, true);
		btnNewButton_5.setText("Base");

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

		Button button_1_5 = new Button(composite_4, SWT.NONE);
		button_1_5.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				EclipseResourceEditorHelpers.open_resource_file_in_editor(
						"resources/DataStore.java.android.settings.xml", Const.SETTINGS_XML);
			}
		});
		button_1_5.setToolTipText("settings.xml");
		button_1_5.setImage(SWTResourceManager.getImage(UIEditorPageAdmin.class, "/img/xmldoc_12x12.gif"));
		toolkit.adapt(button_1_5, true, true);

		Composite composite_7 = new Composite(composite_2, SWT.NONE);
		toolkit.adapt(composite_7);
		toolkit.paintBordersFor(composite_7);
		composite_7.setLayout(new GridLayout(5, false));

		Label lblC = new Label(composite_7, SWT.NONE);
		lblC.setAlignment(SWT.RIGHT);
		toolkit.adapt(lblC, true, true);
		lblC.setText("C++");

		Button btnDatastoreCQt = new Button(composite_7, SWT.NONE);
		btnDatastoreCQt.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				EclipseResourceEditorHelpers.open_resource_file_in_editor("resources/DataStore_Qt5.cpp",
						"DataStore.cpp");
				EclipseResourceEditorHelpers.open_resource_file_in_editor("resources/DataStore_Qt5.h", "DataStore.h");
			}
		});
		toolkit.adapt(btnDatastoreCQt, true, true);
		btnDatastoreCQt.setText("Qt");

		Button btnDatastoreCStl = new Button(composite_7, SWT.NONE);
		btnDatastoreCStl.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				EclipseResourceEditorHelpers.open_resource_file_in_editor("resources/DataStore.STL.cpp",
						"DataStore.cpp");
				EclipseResourceEditorHelpers.open_resource_file_in_editor("resources/DataStore.STL.h", "DataStore.h");
			}
		});
		toolkit.adapt(btnDatastoreCStl, true, true);
		btnDatastoreCStl.setText("stl, sqlite3");

		Button btnNewButton_11 = new Button(composite_7, SWT.NONE);
		btnNewButton_11.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				EclipseResourceEditorHelpers.open_resource_file_in_editor("resources/DataStore.cpp", "DataStore.cpp");
				EclipseResourceEditorHelpers.open_resource_file_in_editor("resources/DataStore.h", "DataStore.h");
			}
		});
		toolkit.adapt(btnNewButton_11, true, true);
		btnNewButton_11.setText("atl, sqlite3");

		Button button_1_6 = new Button(composite_7, SWT.NONE);
		button_1_6.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				EclipseResourceEditorHelpers.open_resource_file_in_editor("resources/DataStore.cpp.settings.xml",
						Const.SETTINGS_XML);
			}
		});
		button_1_6.setToolTipText("settings.xml");
		button_1_6.setImage(SWTResourceManager.getImage(UIEditorPageAdmin.class, "/img/xmldoc_12x12.gif"));
		toolkit.adapt(button_1_6, true, true);

		Composite composite_5 = new Composite(composite_2, SWT.NONE);
		toolkit.adapt(composite_5);
		toolkit.paintBordersFor(composite_5);
		composite_5.setLayout(new GridLayout(10, false));

		Label lblPython = new Label(composite_5, SWT.NONE);
		toolkit.adapt(lblPython, true, true);
		lblPython.setText("Python");

		Button btnSqliteMysqlconnectorPsycopg = new Button(composite_5, SWT.NONE);
		btnSqliteMysqlconnectorPsycopg.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				EclipseResourceEditorHelpers.open_resource_file_in_editor("resources/data_store_no_orm.py",
						"data_store.py");
			}
		});
		toolkit.adapt(btnSqliteMysqlconnectorPsycopg, true, true);
		btnSqliteMysqlconnectorPsycopg.setText("sqlite3, mysql, psycopg2");

		Button button_1 = new Button(composite_5, SWT.NONE);
		button_1.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				EclipseResourceEditorHelpers.open_resource_file_in_editor("resources/data_store_no_orm.py.settings.xml",
						Const.SETTINGS_XML);
			}
		});
		button_1.setToolTipText("settings.xml");
		button_1.setImage(SWTResourceManager.getImage(UIEditorPageAdmin.class, "/img/xmldoc_12x12.gif"));
		toolkit.adapt(button_1, true, true);

		Button btnCxoracle = new Button(composite_5, SWT.NONE);
		btnCxoracle.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				EclipseResourceEditorHelpers.open_resource_file_in_editor("resources/data_store_no_orm_cx_oracle.py",
						"data_store.py");
			}
		});
		toolkit.adapt(btnCxoracle, true, true);
		btnCxoracle.setText("cx_oracle");

		Button button_1_1 = new Button(composite_5, SWT.NONE);
		button_1_1.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				EclipseResourceEditorHelpers.open_resource_file_in_editor(
						"resources/data_store_no_orm_cx_oracle.py.settings.xml", Const.SETTINGS_XML);
			}
		});
		button_1_1.setToolTipText("settings.xml");
		button_1_1.setImage(SWTResourceManager.getImage(UIEditorPageAdmin.class, "/img/xmldoc_12x12.gif"));
		toolkit.adapt(button_1_1, true, true);

		Button btnDjangodb = new Button(composite_5, SWT.NONE);
		btnDjangodb.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				EclipseResourceEditorHelpers.open_resource_file_in_editor("resources/data_store_django.py",
						"data_store.py");
			}
		});
		btnDjangodb.setText("django.db");
		toolkit.adapt(btnDjangodb, true, true);

		Button button_1_2 = new Button(composite_5, SWT.NONE);
		button_1_2.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				EclipseResourceEditorHelpers.open_resource_file_in_editor("resources/data_store_django.py.settings.xml",
						Const.SETTINGS_XML);
			}
		});
		button_1_2.setToolTipText("settings.xml");
		button_1_2.setImage(SWTResourceManager.getImage(UIEditorPageAdmin.class, "/img/xmldoc_12x12.gif"));
		toolkit.adapt(button_1_2, true, true);

		Button btnSqlalchemy = new Button(composite_5, SWT.NONE);
		btnSqlalchemy.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				EclipseResourceEditorHelpers.open_resource_file_in_editor("resources/data_store_sqlalchemy.py",
						"data_store.py");
			}
		});
		toolkit.adapt(btnSqlalchemy, true, true);
		btnSqlalchemy.setText("SQLAlchemy");

		Button btnFlasksqlalchemy = new Button(composite_5, SWT.NONE);
		btnFlasksqlalchemy.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				EclipseResourceEditorHelpers.open_resource_file_in_editor("resources/data_store_flask_sqlalchemy.py",
						"data_store.py");
			}
		});
		btnFlasksqlalchemy.setText("Flask-SQLAlchemy");
		toolkit.adapt(btnFlasksqlalchemy, true, true);

		Button button_1_3 = new Button(composite_5, SWT.NONE);
		button_1_3.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				EclipseResourceEditorHelpers.open_resource_file_in_editor(
						"resources/data_store_sqlalchemy.py.settings.xml", Const.SETTINGS_XML);
			}
		});
		button_1_3.setToolTipText("settings.xml");
		button_1_3.setImage(SWTResourceManager.getImage(UIEditorPageAdmin.class, "/img/xmldoc_12x12.gif"));
		toolkit.adapt(button_1_3, true, true);

		Composite composite_6 = new Composite(composite_top, SWT.NONE);
		toolkit.adapt(composite_6);
		toolkit.paintBordersFor(composite_6);
		composite_6.setLayout(new GridLayout(7, false));

		Label lblGo = new Label(composite_6, SWT.NONE);
		toolkit.adapt(lblGo, true, true);
		lblGo.setText("Go");

		Button btnNewButton_8_1_1 = new Button(composite_6, SWT.NONE);
		btnNewButton_8_1_1.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				EclipseResourceEditorHelpers.open_resource_file_in_editor("resources/data_store_gorm.go",
						"data_store_gorm.go_");
			}
		});
		btnNewButton_8_1_1.setText("gorm");
		toolkit.adapt(btnNewButton_8_1_1, true, true);

		Button button_1_7 = new Button(composite_6, SWT.NONE);
		button_1_7.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				EclipseResourceEditorHelpers.open_resource_file_in_editor("resources/data_store_gorm.go.settings.xml",
						Const.SETTINGS_XML);
			}
		});
		button_1_7.setToolTipText("settings.xml");
		button_1_7.setImage(SWTResourceManager.getImage(UIEditorPageAdmin.class, "/img/xmldoc_12x12.gif"));
		toolkit.adapt(button_1_7, true, true);

		Button btnNewButton_8_1 = new Button(composite_6, SWT.NONE);
		btnNewButton_8_1.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				// ending '_' because of bugs
				EclipseResourceEditorHelpers.open_resource_file_in_editor("resources/data_store_no_orm.go",
						"data_store_no_orm.go_");
			}
		});
		btnNewButton_8_1.setText("database/sql");
		toolkit.adapt(btnNewButton_8_1, true, true);

		Button button_1_8 = new Button(composite_6, SWT.NONE);
		button_1_8.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				EclipseResourceEditorHelpers.open_resource_file_in_editor("resources/data_store_no_orm.go.settings.xml",
						Const.SETTINGS_XML);
			}
		});
		button_1_8.setToolTipText("settings.xml");
		button_1_8.setImage(SWTResourceManager.getImage(UIEditorPageAdmin.class, "/img/xmldoc_12x12.gif"));
		toolkit.adapt(button_1_8, true, true);

		Button btnSqlx = new Button(composite_6, SWT.NONE);
		btnSqlx.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				EclipseResourceEditorHelpers.open_resource_file_in_editor("resources/data_store_sqlx.go",
						"data_store_no_orm.go_");
			}
		});
		toolkit.adapt(btnSqlx, true, true);
		btnSqlx.setText("sqlx");

		Button button_1_8_1 = new Button(composite_6, SWT.NONE);
		button_1_8_1.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				EclipseResourceEditorHelpers.open_resource_file_in_editor("resources/data_store_sqlx.go.settings.xml",
						Const.SETTINGS_XML);
			}
		});
		button_1_8_1.setToolTipText("settings.xml");
		button_1_8_1.setImage(SWTResourceManager.getImage(UIEditorPageAdmin.class, "/img/xmldoc_12x12.gif"));
		toolkit.adapt(button_1_8_1, true, true);

		Composite composite_3 = new Composite(composite_top, SWT.NONE);
		composite_3.setLayout(new GridLayout(5, false));
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

		Button btnJavavm = new Button(composite_3, SWT.NONE);
		btnJavavm.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				EclipseResourceEditorHelpers.open_resource_file_in_text_editor("com/sqldalmaker/cg/java/java.vm",
						"java.vm");
			}
		});
		btnJavavm.setText("java.vm");
		toolkit.adapt(btnJavavm, true, true);

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

		Button button_4_1 = new Button(composite_3, SWT.NONE);
		button_4_1.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				EclipseResourceEditorHelpers.open_resource_file_in_text_editor("com/sqldalmaker/cg/go/go.vm", "go.vm");
			}
		});
		button_4_1.setText("go.vm");
		toolkit.adapt(button_4_1, true, true);

		composite_8 = new Composite(composite_top, SWT.NONE);
		toolkit.adapt(composite_8);
		toolkit.paintBordersFor(composite_8);
		composite_8.setLayout(new GridLayout(1, false));

		txt_migrate = new Text(composite_8, SWT.BORDER | SWT.MULTI);
		txt_migrate.setText(
				"=== sdm.xml migrate ===");
		txt_migrate.setEditable(false);
		txt_migrate.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		toolkit.adapt(txt_migrate, true, true);
	}

	private static String get_err_msg(Throwable ex) {
		return ex.getClass().getName() + " -> " + ex.getMessage(); // printStackTrace();
	}

	private void validate_all() {
		boolean need_migrate = false;
		IFile sdm_xml = editor2.find_metaprogram_file(Const.SDM_XML);
		if (sdm_xml == null) {
			need_migrate = true;
		}
		set_need_migrate_warning(need_migrate);

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
		check_xsd(buff, Const.SDM_XSD);
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
			String cur_xsd;
			try {
				String xsd_abs_path = editor2.get_metaprogram_file_abs_path(xsd_name);
				cur_xsd = Helpers.load_text_from_file(xsd_abs_path);
			} catch (Exception ex) {
				add_err_msg(buff, get_err_msg(ex));
				return false;
			}
			String ref_xsd;
			try {
				ref_xsd = EclipseHelpers.read_from_resource_folder(xsd_name);
			} catch (Exception ex) {
				add_err_msg(buff, get_err_msg(ex));
				return false;
			}
			if (XmlHelpers.compareXml(ref_xsd, cur_xsd) == 0) {
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

	private void createActions() {
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
		String get_sdm_info = EclipseHelpers.get_sdm_info();
		txtV.setText(get_sdm_info);
	}

	public void setEditor2(Editor2 ed) {
		editor2 = ed;
	}

	public void set_need_migrate_warning(boolean need_to_migrate) {
		if (need_to_migrate) {
			try {
				String txt = Helpers.res_from_jar("sdm.xml_how_to_migrate.txt");
				txt_migrate.setText(txt);
			} catch (Exception e) {
				txt_migrate.setText(e.getMessage());
			}	
		} else {
			txt_migrate.setText("");			
		}
		composite_8.setVisible(need_to_migrate);
	}
}
