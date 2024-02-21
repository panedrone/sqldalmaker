/*
	Copyright 2011-2024 sqldalmaker@gmail.com
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
import org.eclipse.jface.resource.ImageDescriptor;

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
	private Action action_openDaoXmlFile;
	private Action action_getCrudDao;
	private Action action_goto_source;
	private Action action_openSdmXml;

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
		createToolbarActions();
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
		toolBarManager.add(action_openSdmXml);
		toolBarManager.add(action_openDaoXmlFile);
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
					open_detailed_dao_xml();
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

	private void createToolbarActions() {
		{
			action_openSdmXml = new Action("") {
				@Override
				public void run() {
					open_sdm_xml();
				}
			};
			action_openSdmXml.setToolTipText("Find selected item in 'sdm.xml'");
			action_openSdmXml
					.setImageDescriptor(ResourceManager.getImageDescriptor(UIEditorPageDAO.class, "/img/xmldoc.gif"));
		}
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
					crud_dao_xml_wizard();
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
					create_dao_xml_file();
				}
			};
			action_newXml.setToolTipText("New DAO XML file");
			action_newXml
					.setImageDescriptor(ResourceManager.getImageDescriptor(UIEditorPageDAO.class, "/img/new_xml.gif"));
		}
		{
			action_openDaoXmlFile = new Action("") {
				@Override
				public void run() {
					open_detailed_dao_xml();
				}
			};
			action_openDaoXmlFile.setToolTipText("Navigate to XML definition (double-click on left-most cell)");
			action_openDaoXmlFile
					.setImageDescriptor(ImageDescriptor.createFromFile(UIEditorPageDAO.class, "/img/xmldoc_16x16.gif"));
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
			action_goto_source.setToolTipText("Navigate to generated code (double-click on right-most cell)");
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
			List<Item> items = get_selected_items();
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

	private void open_sdm_xml() {
		try {
			IFile sdm_xml_file = editor2.find_sdm_xml();
			if (sdm_xml_file == null) {
				throw new InternalException("File not found: " + Const.SDM_XML);
			}
			List<Item> items = get_selected_items();
			if (items == null) {
				EclipseEditorHelpers.open_editor_sync(getShell(), sdm_xml_file);
				return;
			}
			String dao_class_name = items.get(0).get_class_name();
			EclipseXmlAttrHelpers.goto_sdm_class_declaration(getShell(), sdm_xml_file, dao_class_name);
		} catch (Throwable e) {
			e.printStackTrace();
			EclipseMessageHelpers.show_error(e);
		}
	}

	private void open_detailed_dao_xml() {
		List<Item> items = get_selected_items();
		if (items == null) {
			return;
		}
		// if <dao-class 'ref' exists, open by 'ref', else go to declaration in sdm.xml
		String dao_class_name = items.get(0).get_class_name();
		try {
			List<DaoClass> jaxb_dao_classes = load_all_sdm_dao();
			if (jaxb_dao_classes.size() > 0) {
				DaoClass jaxb_dao_class = JaxbUtils.find_jaxb_dao_class(dao_class_name, jaxb_dao_classes);
				String dao_xml_ref = jaxb_dao_class.getRef();
				if (dao_xml_ref != null) {
					IFile external_dao_xml = editor2.find_metaprogram_file(dao_xml_ref);
					if (external_dao_xml != null) {
						EclipseEditorHelpers.open_editor_sync(getShell(), external_dao_xml);
						return;
					}
				}
			}
			IFile sdm_file = editor2.find_sdm_xml();
			if (sdm_file == null) {
				throw new InternalException("File not found: " + Const.SDM_XML);
			}
			EclipseXmlAttrHelpers.goto_sdm_class_declaration(getShell(), sdm_file, dao_class_name);
		} catch (Throwable e) {
			e.printStackTrace();
			EclipseMessageHelpers.show_error(e);
		}
	}

	protected void crud_dao_xml_wizard() {
		try {
			EclipseCrudXmlHelpers.get_crud_dao_xml(getShell(), editor2);
		} catch (Throwable e) {
			e.printStackTrace();
			EclipseMessageHelpers.show_error(e);
		}
	}

	protected void create_dao_xml_file() {
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

	private List<Item> get_selected_items() {
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
		List<Item> items = get_selected_items();
		if (items == null) {
			items = (List<Item>) tableViewer.getInput();
		}
		return items;
	}

	private List<DaoClass> load_all_sdm_dao() throws Exception {
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
					IDaoCG gen = EclipseCG.create_dao_cg(conn, editor2.get_project(), editor2, settings, output_dir);
					monitor.beginTask(get_name(), get_total_work());
					generated = generate_for_selected_dao(monitor, gen, settings, items, output_dir);
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
		};
		EclipseSyncActionHelper.run_with_progress(getShell(), action);
	}

	private String[] generate_single_sdm_dao(IDaoCG gen, XmlParser dao_xml_parser, DaoClass sdm_dao_class)
			throws Exception {
		String dao_class_name = sdm_dao_class.getName();
		String dao_xml_rel_path = dao_class_name + ".xml";
		String dao_xml_abs_path = editor2.get_metaprogram_file_abs_path(dao_xml_rel_path);
		return EclipseCG.generate_single_sdm_dao(gen, dao_xml_parser, sdm_dao_class, dao_xml_abs_path);
	}

	private boolean generate_for_selected_dao(IProgressMonitor monitor, IDaoCG gen, Settings settings, List<Item> items,
			StringBuilder output_dir) throws Exception {

		boolean generated = false;
		String dao_xsd_abs_path = editor2.get_dao_xsd_abs_path();
		String context_path = DaoClass.class.getPackage().getName();
		XmlParser dao_xml_parser = new XmlParser(context_path, dao_xsd_abs_path);
		List<DaoClass> jaxb_dao_classes = load_all_sdm_dao();
		for (Item item : items) {
			if (monitor.isCanceled()) {
				return generated;
			}
			String dao_class_name = item.get_class_name();
			monitor.subTask(dao_class_name);
			try {
				DaoClass sdm_dao_class = JaxbUtils.find_jaxb_dao_class(dao_class_name, jaxb_dao_classes);
				String[] file_content = generate_single_sdm_dao(gen, dao_xml_parser, sdm_dao_class);
				String file_name = EclipseTargetLanguageHelpers.get_rel_path(editor2, output_dir.toString(),
						dao_class_name);
				EclipseHelpers.save_text_to_file(file_name, file_content[0]);
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
					IDaoCG gen = EclipseCG.create_dao_cg(con, editor2.get_project(), editor2, settings, output_dir);
					monitor.beginTask(get_name(), get_total_work());
					List<DaoClass> jaxb_dao_classes = load_all_sdm_dao();
					validate_all(monitor, gen, items, settings, jaxb_dao_classes, output_dir);
				} finally {
					con.close();
					Display.getDefault().asyncExec(new Runnable() {
						public void run() {
							tableViewer.refresh();
						}
					});
				}
			}

			private void validate_all(IProgressMonitor monitor, IDaoCG gen, List<Item> items, Settings settings,
					List<DaoClass> sdm_dao_classes, StringBuilder output_dir) throws Exception {

				String dao_xsd_abs_path = editor2.get_dao_xsd_abs_path();
				String context_path = DaoClass.class.getPackage().getName();
				XmlParser dao_xml_parser = new XmlParser(context_path, dao_xsd_abs_path);
				for (int i = 0; i < items.size(); i++) {
					if (monitor.isCanceled()) {
						return;
					}
					String dao_class_name = items.get(i).get_class_name();
					monitor.subTask(dao_class_name);
					try {
						DaoClass sdm_dao_class = JaxbUtils.find_jaxb_dao_class(dao_class_name, sdm_dao_classes);
						String[] file_content = generate_single_sdm_dao(gen, dao_xml_parser, sdm_dao_class);
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
		};
		EclipseSyncActionHelper.run_with_progress(getShell(), action);
	}

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
			List<DaoClass> jaxb_dao_classes = load_all_sdm_dao();
			if (jaxb_dao_classes.size() > 0) {
				for (DaoClass cls : jaxb_dao_classes) {
					Item item = new Item();
					item.set_class_name(cls.getName());
					items.add(item);
				}

			} else {
				Helpers.IFileList fileList = new Helpers.IFileList() {
					@Override
					public void add(String fileName) {
						String dao_class_name = fileName.replace(".xml", "");
						Item item = new Item();
						item.set_class_name(dao_class_name);
						items.add(item);
					}
				};
				Helpers.enum_dao_xml_file_names(editor2.get_sdm_folder_abs_path(), fileList);
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
		action_openDaoXmlFile.setEnabled(enabled);
		action_goto_source.setEnabled(enabled);
	}
}
