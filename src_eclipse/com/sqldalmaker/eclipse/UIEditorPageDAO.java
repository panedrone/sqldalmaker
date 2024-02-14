/*
	Copyright 2011-2023 sqldalmaker@gmail.com
	Read LICENSE.txt in the root of this project/archive.
	Project web-site: https://sqldalmaker.sourceforge.net/
*/
package com.sqldalmaker.eclipse;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.wb.swt.ResourceManager;

import com.sqldalmaker.cg.Helpers;
import com.sqldalmaker.cg.IDaoCG;
import com.sqldalmaker.cg.JaxbUtils;
import com.sqldalmaker.common.Const;
import com.sqldalmaker.common.InternalException;
import com.sqldalmaker.common.SdmUtils;
import com.sqldalmaker.common.XmlParser;
import com.sqldalmaker.jaxb.sdm.DaoClass;
import com.sqldalmaker.jaxb.settings.Settings;

/**
 * @author sqldalmaker@gmail.com
 *
 */
public class UIEditorPageDAO extends Composite {

	private Filter filter;
	private IEditor2 editor2;

	public void setEditor2(IEditor2 editor2) {
		this.editor2 = editor2;
	}

	// WindowBuilder fails with inheritance
	private final FormToolkit toolkit = new FormToolkit(Display.getCurrent());
	private Text text;
	private TableViewer tableViewer;
	private Table table;
	private Action action_refresh;
	private Action action_generate;
	private Action action_validate;
	private Action action_newXml;
	private Action action_openXml;
	private Action action_getCrudDao;
	private Action action_goto_source;

	private ToolBarManager toolBarManager;
	private ToolBar toolBar1;
	private Composite composite_1;
	private Action action_FK;

	public ToolBar getToolBarManager() {
		return toolBar1;
	}

	/**
	 * Create the composite.
	 *
	 * @param parent
	 * @param style
	 */
	public UIEditorPageDAO(Composite parent, int style) {
		super(parent, style);
		createActions();
		addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				toolkit.dispose();
			}
		});
		toolkit.adapt(this);
		toolkit.paintBordersFor(this);
		GridLayout gridLayout = new GridLayout(1, false);
		gridLayout.verticalSpacing = 8;
		gridLayout.marginHeight = 0;
		gridLayout.horizontalSpacing = 0;
		setLayout(gridLayout);

		composite_1 = new Composite(this, SWT.NONE);
		composite_1.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
		toolkit.adapt(composite_1);
		toolkit.paintBordersFor(composite_1);
		composite_1.setLayout(new FillLayout(SWT.VERTICAL));

		toolBar1 = new ToolBar(composite_1, SWT.NONE);
		toolBarManager = new ToolBarManager(toolBar1);
		toolkit.adapt(toolBar1);
		toolkit.paintBordersFor(toolBar1);
		toolBarManager.add(action_newXml);
		toolBarManager.add(action_openXml);
		toolBarManager.add(action_goto_source);
		toolBarManager.add(action_getCrudDao);
		toolBarManager.add(action_FK);
		toolBarManager.add(action_refresh);
		toolBarManager.add(action_generate);
		toolBarManager.add(action_validate);

		text = toolkit.createText(composite_1, "", SWT.BORDER);
		text.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				updateFilter();
			}
		});

		tableViewer = new TableViewer(this, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.BORDER);
		tableViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				do_on_selection_changed();
			}
		});
		table = tableViewer.getTable();
		table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		// === panedrone: toolkit.adapt(table, false, false) leads to invalid selection
		// look in Linux
		// toolkit.adapt(table, false, false);
		table.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseDoubleClick(MouseEvent e) {
				Point pt = new Point(e.x, e.y);
				TableItem item = table.getItem(pt);
				if (item == null)
					return;
				int clicked_column_index = -1;
				for (int col = 0; col < 3; col++) {
					Rectangle rect = item.getBounds(col);
					if (rect.contains(pt)) {
						clicked_column_index = col;
						break;
					}
				}
				if (clicked_column_index == 0) {
					open_xml();
				} else {
					open_generated_source_file();
				}
			}
		});

		TableViewerColumn tableViewerColumn = new TableViewerColumn(tableViewer, SWT.NONE);
		TableColumn tblclmnNewColumn = tableViewerColumn.getColumn();
		tblclmnNewColumn.setWidth(260);
		tblclmnNewColumn.setText("File");

		TableViewerColumn tableViewerColumn_1 = new TableViewerColumn(tableViewer, SWT.NONE);
		TableColumn tblclmnNewColumn_1 = tableViewerColumn_1.getColumn();
		tblclmnNewColumn_1.setWidth(300);
		tblclmnNewColumn_1.setText("State");
		table.setLinesVisible(true);
		table.setHeaderVisible(true);

		tableViewer.setContentProvider(new ArrayContentProvider());
		IBaseLabelProvider labelProvider = new ItemLabelProvider();
		filter = new Filter();
		tableViewer.setLabelProvider(labelProvider);
		tableViewer.addFilter(filter);

		// http://www.java2s.com/Open-Source/Java-Document/IDE-Eclipse/ui/org/eclipse/ui/forms/examples/internal/rcp/SingleHeaderEditor.java.htm
		toolBarManager.update(true);
	}

	private void createActions() {
		{
			action_refresh = new Action("") {
				@Override
				public void run() {
					reload_table(true);
				}
			};
			action_refresh.setToolTipText("Refesh");
			action_refresh
					.setImageDescriptor(ResourceManager.getImageDescriptor(UIEditorPageDAO.class, "/img/refresh.gif"));
		}
		{
			action_getCrudDao = new Action("") {

				@Override
				public void run() {
					generate_crud_dao_xml();
				}
			};
			action_getCrudDao.setToolTipText("DAO CRUD XML Assistant");
			action_getCrudDao
					.setImageDescriptor(ResourceManager.getImageDescriptor(UIEditorPageDAO.class, "/img/180.png"));
		}
		{
			action_generate = new Action("") {
				@Override
				public void run() {
					generate_with_progress();
				}
			};
			action_generate.setToolTipText("Generate selected");
			action_generate.setImageDescriptor(
					ResourceManager.getImageDescriptor(UIEditorPageDAO.class, "/img/compile-warning.png"));
		}
		{
			action_validate = new Action("") {
				@Override
				public void run() {
					validate_with_progress();
				}
			};
			action_validate.setToolTipText("Validate all");
			action_validate
					.setImageDescriptor(ResourceManager.getImageDescriptor(UIEditorPageDAO.class, "/img/validate.gif"));
		}
		{
			action_newXml = new Action("") {
				@Override
				public void run() {
					create_xml_file();
				}
			};
			action_newXml.setToolTipText("New XML file");
			action_newXml
					.setImageDescriptor(ResourceManager.getImageDescriptor(UIEditorPageDAO.class, "/img/new_xml.gif"));
		}
		{
			action_openXml = new Action("") {
				@Override
				public void run() {
					open_xml();
				}
			};
			action_openXml.setToolTipText("Open XML file");
			action_openXml
					.setImageDescriptor(ResourceManager.getImageDescriptor(UIEditorPageDAO.class, "/img/xmldoc.gif"));
		}
		{
			action_goto_source = new Action("") {
				@Override
				public void run() {
					open_generated_source_file();
				}
			};
			action_goto_source.setImageDescriptor(
					ResourceManager.getImageDescriptor(UIEditorPageDAO.class, "/img/GeneratedFile.gif"));
			action_goto_source.setToolTipText("Go to generated source");
		}
		{
			action_FK = new Action("") {
				@Override
				public void run() {
					try {
						EclipseCrudXmlHelpers.get_fk_access_xml(getShell(), editor2);
					} catch (Throwable e) {
						e.printStackTrace();
						EclipseMessageHelpers.show_error(e);
					}
				}
			};
			action_FK.setImageDescriptor(ResourceManager.getImageDescriptor(UIEditorPageDAO.class, "/img/FK.gif"));
			action_FK.setToolTipText("FK Access Assistant");
		}
	}

	protected void open_generated_source_file() {
		try {
			List<Item> items = prepare_selected_items();
			if (items == null) {
				return;
			}
			IFile file = null;
			String dao_class_name = items.get(0).get_class_name();
			Settings settings = EclipseHelpers.load_settings(editor2);
			file = EclipseTargetLanguageHelpers.find_source_file_in_project_tree(editor2.get_project(), settings,
					dao_class_name, settings.getDao().getScope(), editor2.get_root_file_name());
			EclipseEditorHelpers.open_editor_sync(getShell(), file);
		} catch (Throwable e) {
			e.printStackTrace();
			EclipseMessageHelpers.show_error(e);
		}
	}

	protected void open_xml() {
		try {
			List<Item> items = prepare_selected_items();
			if (items == null) {
				return;
			}
			String class_name = items.get(0).get_class_name();
			List<DaoClass> jaxb_dao_classes = load_sdm_dao();
			if (jaxb_dao_classes.size() > 0) {
				IFile sdm_file = editor2.find_sdm_xml();
				if (sdm_file == null) {
					throw new InternalException("File not found: " + Const.SDM_XML);
				}
				EclipseXmlAttrHelpers.goto_sdm_class_declaration(getShell(), sdm_file, class_name);
			} else {
				String relative = class_name + ".xml";
				IFile dao_file = editor2.find_metaprogram_file(relative);
				if (dao_file != null) {
					EclipseEditorHelpers.open_editor_sync(getShell(), dao_file);
				}
			}
		} catch (Throwable e) {
			e.printStackTrace();
			EclipseMessageHelpers.show_error(e);
		}
	}

	protected void generate_crud_dao_xml() {
		try {
			EclipseCrudXmlHelpers.get_crud_dao_xml(getShell(), editor2);
		} catch (Throwable e) {
			e.printStackTrace();
			EclipseMessageHelpers.show_error(e);
		}
	}

	protected void create_xml_file() {
		try {
			IFile file = UIDialogNewDaoXmlFile.open(getShell(), editor2);
			if (file != null) {
				try {
					reload_table(true);
					EclipseEditorHelpers.open_editor_sync(getShell(), file);

				} catch (Throwable e1) {
					e1.printStackTrace();
					EclipseMessageHelpers.show_error(e1);
				}
			}
		} catch (Throwable e) {
			e.printStackTrace();
			EclipseMessageHelpers.show_error(e);
		}
	}

	private List<Item> prepare_selected_items() {
		@SuppressWarnings("unchecked")
		List<Item> items = (List<Item>) tableViewer.getInput();
		if (items == null || items.size() == 0) {
			return null;
		}
		List<Item> res = new ArrayList<Item>();
		if (items.size() == 1) {
			Item item = items.get(0);
			item.set_status("");
			res.add(item);
			return res;
		}
		int[] indexes = table.getSelectionIndices();
		if (indexes.length == 0) {
			// InternalHelpers.showError("Select DAO configurations");
			return null;
		}
		for (int row : indexes) {
			Item item = items.get(row);
			item.set_status("");
			res.add(item);
		}
		return res;
	}

	@SuppressWarnings("unchecked")
	private List<Item> get_items() {
		List<Item> items = prepare_selected_items();
		if (items == null) {
			items = (List<Item>) tableViewer.getInput();
		}
		return items;
	}

	private List<DaoClass> load_sdm_dao() throws Exception {
		String sdm_folder_abs_path = editor2.get_sdm_folder_abs_path();
		String sdm_xml_abs_path = Helpers.concat_path(sdm_folder_abs_path, Const.SDM_XML);
		String sdm_xsd_abs_path = Helpers.concat_path(sdm_folder_abs_path, Const.SDM_XSD);
		List<DaoClass> jaxb_dao_classes = SdmUtils.get_dao_classes(sdm_xml_abs_path, sdm_xsd_abs_path);
		return jaxb_dao_classes;
	}

	private void generate_with_progress() {
		final List<Item> items = get_items();
		if (items == null) {
			return;
		}
		// //////////////////////////////////////////
		tableViewer.refresh();
		// //////////////////////////////////////////
		EclipseSyncAction action = new EclipseSyncAction() {
			@Override
			public int get_total_work() {
				return items.size();
			}

			@Override
			public String get_name() {
				return "Code generation...";
			}

			@Override
			public void run_with_progress(IProgressMonitor monitor) throws Exception {
				boolean generated = false;
				monitor.subTask("Connecting...");
				Connection conn = EclipseHelpers.get_connection(editor2);
				monitor.subTask("Connected.");
				try {
					EclipseConsoleHelpers.init_console();
					Settings settings = EclipseHelpers.load_settings(editor2);
					StringBuilder output_dir = new StringBuilder();
					// !!!! after 'try'
					IDaoCG gen = EclipseTargetLanguageHelpers.create_dao_cg(conn, editor2.get_project(), editor2,
							settings, output_dir);
					List<DaoClass> jaxb_dao_classes = load_sdm_dao();
					monitor.beginTask(get_name(), get_total_work());
					if (jaxb_dao_classes.size() > 0) {
						generated = generate_for_sdm_xml(monitor, gen, settings, items, jaxb_dao_classes, output_dir);
					} else {
						generated = generate_for_dao_xml(monitor, gen, settings, items, output_dir);
					}
				} finally {
					conn.close();
					if (generated) {
						EclipseHelpers.refresh_project(editor2.get_project());
					}
					// Exception can occur at 3rd line (for example):
					// refresh first 3 lines
					// error lines are not generated but update them too
					Display.getDefault().asyncExec(new Runnable() {
						public void run() {
							tableViewer.refresh();
						}
					});
				}
			}

			private boolean generate_for_sdm_xml(IProgressMonitor monitor, IDaoCG gen, Settings settings,
					List<Item> items, List<DaoClass> jaxb_dao_classes, StringBuilder output_dir) {

				boolean generated = false;
				for (Item item : items) {
					if (monitor.isCanceled()) {
						return generated;
					}
					String dao_class_name = item.get_class_name();
					monitor.subTask(dao_class_name);
					try {
						DaoClass dao_class = JaxbUtils.find_jaxb_dao_class(dao_class_name, jaxb_dao_classes);
						String[] fileContent = gen.translate(dao_class_name, dao_class);
						String fileName = EclipseTargetLanguageHelpers.get_rel_path(editor2, output_dir.toString(),
								dao_class_name);
						EclipseHelpers.save_text_to_file(fileName, fileContent[0]);
						item.set_status(Const.STATUS_GENERATED);
						generated = true;
					} catch (Throwable ex) {
						String msg = ex.getMessage();
						if (msg == null) {
							msg = "???";
						}
						item.set_status(msg);
						// throw ex; // outer 'catch' cannot read the
						// message
						// !!!! not Internal_Exception to show Exception
						// class
						// throw new Exception(ex);
						EclipseConsoleHelpers.add_error_msg(dao_class_name + ": " + msg);
					}
					monitor.worked(1);
				}
				return generated;
			}

			private boolean generate_for_dao_xml(IProgressMonitor monitor, IDaoCG gen, Settings settings,
					List<Item> items, StringBuilder output_dir) throws Exception {

				boolean generated = false;
				String dao_xsd_abs_path = editor2.get_dao_xsd_abs_path();
				String contextPath = DaoClass.class.getPackage().getName();
				XmlParser dao_xml_parser = new XmlParser(contextPath, dao_xsd_abs_path);
				for (Item item : items) {
					if (monitor.isCanceled()) {
						return generated;
					}
					String dao_class_name = item.get_class_name();
					monitor.subTask(dao_class_name);
					try {
						String dao_xml_rel_path = dao_class_name + ".xml";
						String dao_xml_abs_path = editor2.get_metaprogram_file_abs_path(dao_xml_rel_path);
						DaoClass dao_class = dao_xml_parser.unmarshal(dao_xml_abs_path);
						String[] fileContent = gen.translate(dao_class_name, dao_class);
						String fileName = EclipseTargetLanguageHelpers.get_rel_path(editor2, output_dir.toString(),
								dao_class_name);
						EclipseHelpers.save_text_to_file(fileName, fileContent[0]);
						item.set_status(Const.STATUS_GENERATED);
						generated = true;
					} catch (Throwable ex) {
						String msg = ex.getMessage();
						if (msg == null) {
							msg = "???";
						}
						item.set_status(msg);
						// throw ex; // outer 'catch' cannot read the
						// message
						// !!!! not Internal_Exception to show Exception
						// class
						// throw new Exception(ex);
						EclipseConsoleHelpers.add_error_msg(dao_class_name + ": " + msg);
					}
					monitor.worked(1);
				}
				return generated;
			}
		};
		EclipseSyncActionHelper.run_with_progress(getShell(), action);
	}

	protected void validate_with_progress() {
		try {
			validate();
		} catch (Throwable e) {
			e.printStackTrace();
			EclipseMessageHelpers.show_error(e);
		}
	}

	protected void validate() throws Exception {
		final List<Item> items = reload_table();
		// ///////////////////////////////////////
		EclipseSyncAction action = new EclipseSyncAction() {
			@Override
			public int get_total_work() {
				return items.size();
			}

			@Override
			public String get_name() {
				return "Validation...";
			}

			@Override
			public void run_with_progress(IProgressMonitor monitor) throws Exception {
				monitor.subTask("Connecting...");
				Connection con = EclipseHelpers.get_connection(editor2);
				monitor.subTask("Connected.");
				try {

					EclipseConsoleHelpers.init_console();
					Settings settings = EclipseHelpers.load_settings(editor2);
					StringBuilder output_dir = new StringBuilder();
					// !!!! after 'try'
					IDaoCG gen = EclipseTargetLanguageHelpers.create_dao_cg(con, editor2.get_project(), editor2,
							settings, output_dir);
					monitor.beginTask(get_name(), get_total_work());
					List<DaoClass> jaxb_dao_classes = load_sdm_dao();
					if (jaxb_dao_classes.size() > 0) {
						validate_by_sdm(monitor, gen, items, settings, jaxb_dao_classes, output_dir);
					} else {
						validate_by_xml_files(monitor, gen, items, settings, output_dir);
					}

				} finally {
					con.close();
					Display.getDefault().asyncExec(new Runnable() {
						public void run() {
							tableViewer.refresh();
						}
					});
				}
			}

			private void validate_by_sdm(IProgressMonitor monitor, IDaoCG gen, List<Item> items, Settings settings,
					List<DaoClass> jaxb_dao_classes, StringBuilder output_dir) {

				for (int i = 0; i < items.size(); i++) {
					if (monitor.isCanceled()) {
						return;
					}
					String dao_class_name = items.get(i).get_class_name();
					monitor.subTask(dao_class_name);
					try {
						DaoClass dao_class = JaxbUtils.find_jaxb_dao_class(dao_class_name, jaxb_dao_classes);
						String[] file_content = gen.translate(dao_class_name, dao_class);
						String file_name = EclipseTargetLanguageHelpers.get_rel_path(editor2, output_dir.toString(),
								dao_class_name);
						StringBuilder validation_buff = new StringBuilder();
						String old_text = Helpers.load_text_from_file(file_name);
						if (old_text == null) {
							validation_buff.append(Const.OUTPUT_FILE_IS_MISSING);
						} else {
							String text = file_content[0];
							if (!Helpers.equal_ignoring_eol(text, old_text)) {
								validation_buff.append(Const.OUTPUT_FILE_IS_OUT_OF_DATE);
							}
						}
						String status = validation_buff.toString();
						if (status.length() == 0) {
							items.get(i).set_status(Const.STATUS_OK);
						} else {
							items.get(i).set_status(status);
							EclipseConsoleHelpers.add_error_msg(dao_class_name + ": " + status);
						}

					} catch (Throwable ex) {
						ex.printStackTrace();
						String msg = ex.getMessage();
						items.get(i).set_status(msg);
						EclipseConsoleHelpers.add_error_msg(dao_class_name + ": " + msg);
					}
					monitor.worked(1);
				}
			}

			private void validate_by_xml_files(IProgressMonitor monitor, IDaoCG gen, List<Item> items,
					Settings settings, StringBuilder output_dir) throws Exception {

				String daoXsdFileName = editor2.get_dao_xsd_abs_path();
				String contextPath = DaoClass.class.getPackage().getName();
				XmlParser daoXml_Parser = new XmlParser(contextPath, daoXsdFileName);
				for (int i = 0; i < items.size(); i++) {
					if (monitor.isCanceled()) {
						return;
					}
					String dao_class_name = items.get(i).get_class_name();
					monitor.subTask(dao_class_name);
					try {
						String dao_xml_rel_path = dao_class_name + ".xml";
						String dao_xml_abs_path = editor2.get_metaprogram_file_abs_path(dao_xml_rel_path);
						DaoClass dao_class = daoXml_Parser.unmarshal(dao_xml_abs_path);
						String[] file_content = gen.translate(dao_class_name, dao_class);
						String file_name = EclipseTargetLanguageHelpers.get_rel_path(editor2, output_dir.toString(),
								dao_class_name);
						StringBuilder validation_buff = new StringBuilder();
						String old_text = Helpers.load_text_from_file(file_name);
						if (old_text == null) {
							validation_buff.append(Const.OUTPUT_FILE_IS_MISSING);
						} else {
							String text = file_content[0];
							if (!Helpers.equal_ignoring_eol(text, old_text)) {
								validation_buff.append(Const.OUTPUT_FILE_IS_OUT_OF_DATE);
							}
						}
						String status = validation_buff.toString();
						if (status.length() == 0) {
							items.get(i).set_status(Const.STATUS_OK);
						} else {
							items.get(i).set_status(status);
							EclipseConsoleHelpers.add_error_msg(dao_xml_rel_path + ": " + status);
						}

					} catch (Throwable ex) {
						ex.printStackTrace();
						String msg = ex.getMessage();
						items.get(i).set_status(msg);
						EclipseConsoleHelpers.add_error_msg(dao_class_name + ": " + msg);
					}
					monitor.worked(1);
				}
			}
		};
		EclipseSyncActionHelper.run_with_progress(getShell(), action);
	}

	// private IJavaProject getJavaProject() {
	//
	// IJavaProject jproject = JavaCore.create(project);
	//
	// return jproject;
	// }

	private class ItemLabelProvider extends LabelProvider implements ITableLabelProvider, IColorProvider {

		public String getColumnText(Object element, int columnIndex) {
			String result = "";
			Item item = (Item) element;
			switch (columnIndex) {
			case 0:
				result = item.get_class_name();
				break;
			case 1:
				result = item.get_status();
				break;
			default:
				break;
			}
			return result;
		}

		@Override
		public Image getColumnImage(Object arg0, int arg1) {
			return null;
		}

		@Override
		public Color getBackground(Object arg0) {
			return null;
		}

		@Override
		public Color getForeground(Object element) {
			Item item = (Item) element;
			String status = item.get_status();
			if (status != null && status.length() > 0 && Const.STATUS_OK.equals(status) == false
					&& Const.STATUS_GENERATED.equals(status) == false) {

				return PlatformUI.getWorkbench().getDisplay().getSystemColor(SWT.COLOR_RED);
			}
			return null;
		}
	}

	static class Item {

		private String class_name;
		private String status;

		public String get_status() {
			return status;
		}

		public void set_status(String status) {
			this.status = status;
		}

		public String get_class_name() {
			return class_name;
		}

		public void set_class_name(String class_name) {
			this.class_name = class_name;
		}
	}

	private class Filter extends ViewerFilter {
		@Override
		public boolean select(Viewer viewer, Object parentElement, Object element) {
			if (search_string == null || search_string.length() == 0) {
				return true;
			}
			Item item = (Item) element;
			if (item.get_class_name().matches(search_string)) {
				return true;
			}
			return false;
		}

		private String search_string;

		public void setSearchText(String s) {
			// Search must be a substring of the existing value
			this.search_string = ".*" + s + ".*";
		}
	}

	protected void updateFilter() {
		filter.setSearchText(text.getText());
		tableViewer.refresh(); // fires selectionChanged
	}

	@Override
	protected void checkSubclass() {
		// Disable the check that prevents subclassing of SWT components
	}

	private ArrayList<Item> reload_table() throws Exception {
		final ArrayList<Item> items = new ArrayList<Item>();
		try {
			List<DaoClass> jaxb_dao_classes = load_sdm_dao();
			if (jaxb_dao_classes.size() > 0) {
				for (DaoClass cls : jaxb_dao_classes) {
					Item item = new Item();
					item.set_class_name(cls.getName());
					items.add(item);
				}

			} else {
				FileSearchHelpers.IFileList fileList = new FileSearchHelpers.IFileList() {
					@Override
					public void add(String fileName) {
						String dao_class_name = fileName.replace(".xml", "");
						Item item = new Item();
						item.set_class_name(dao_class_name);
						items.add(item);
					}
				};
				FileSearchHelpers.enum_dao_xml_file_names(editor2.get_sdm_folder_abs_path(), fileList);
			}
		} finally {
			tableViewer.setInput(items);
			tableViewer.refresh();
			// tableViewer.refresh(); NOT REQUIRED
			do_on_selection_changed();
			boolean enable = items.size() > 0;
			action_validate.setEnabled(enable);
		}
		return items;
	}

	public void reload_table(boolean showErrorMsg) {
		try {
			reload_table();
		} catch (Throwable e) {
			if (showErrorMsg) {
				e.printStackTrace();
				EclipseMessageHelpers.show_error(e);
			}
		}
	}

	private void do_on_selection_changed() {
		@SuppressWarnings("unchecked")
		List<Item> items = (List<Item>) tableViewer.getInput();
		boolean enabled = items.size() > 0;
//		boolean enabled;
//		if (items.size() == 1) {
//			enabled = true;
//		} else {
//			int[] indexes = table.getSelectionIndices();
//			enabled = indexes.length > 0;
//		}
		action_generate.setEnabled(enabled);
		action_openXml.setEnabled(enabled);
		action_goto_source.setEnabled(enabled);
	}
}
