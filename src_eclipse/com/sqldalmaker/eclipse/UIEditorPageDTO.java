/*
	Copyright 2011-2022 sqldalmaker@gmail.com
	Read LICENSE.txt in the root of this project/archive.
	Project web-site: https://sqldalmaker.sourceforge.net/
*/
package com.sqldalmaker.eclipse;

import java.io.FileInputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
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
import com.sqldalmaker.cg.IDtoCG;
import com.sqldalmaker.cg.SqlUtils;
import com.sqldalmaker.common.Const;
import com.sqldalmaker.common.InternalException;
import com.sqldalmaker.common.SdmUtils;
import com.sqldalmaker.jaxb.dto.DtoClass;
import com.sqldalmaker.jaxb.settings.Settings;

/**
 * @author sqldalmaker@gmail.com
 *
 */
public class UIEditorPageDTO extends Composite {

	private Filter filter;

	private IEditor2 editor2;

	// WindowBuilder fails with inheritance
	private final FormToolkit toolkit = new FormToolkit(Display.getCurrent());
	private Text text;
	private TableViewer tableViewer;
	private Table table;
	private Action action_refresh;
	private Action action_generate;
	private Action action_validate;
	private Action action_openSQL;
	private Action action_openXml;
	private Action action_import;
	private Action action_open_target;
	private Action action_genTmpFieldTags;

	private Composite composite_1;

	private ToolBarManager toolBarManager;
	private ToolBar toolBar1;

	/**
	 * Create the composite.
	 *
	 * @param parent
	 * @param style
	 */
	public UIEditorPageDTO(Composite parent, int style) {
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
		toolBarManager.add(action_openXml);
		toolBarManager.add(action_openSQL);
		toolBarManager.add(action_open_target);
		toolBarManager.add(action_genTmpFieldTags);
		toolBarManager.add(action_import);
		toolBarManager.add(action_refresh);
		toolBarManager.add(action_generate);
		toolBarManager.add(action_validate);

		text = toolkit.createText(composite_1, "", SWT.BORDER);
		text.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				update_filter();
			}
		});

		tableViewer = new TableViewer(this, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.BORDER);
		tableViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent evt) {
				do_on_selection_changed();
			}
		});

		table = tableViewer.getTable();
		table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 0, 0));
		table.setLinesVisible(true);
		// === panedrone: toolkit.adapt(table, false, false) leads to invalid selection
		// look in Linux
		// toolkit.adapt(table, false, false);
		table.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseDoubleClick(MouseEvent e) {
				// http://git.eclipse.org/c/platform/eclipse.platform.swt.git/tree/examples/org.eclipse.swt.snippets/src/org/eclipse/swt/snippets/Snippet3.java
				Point pt = new Point(e.x, e.y);
				TableItem item = table.getItem(pt);
				if (item == null) {
					open_xml();
					return;
				}
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
				} else if (clicked_column_index == 1) {
					open_sql();
				} else {
					open_generated_source_file();
				}
			}
		});
		table.setHeaderVisible(true);

		TableViewerColumn tableViewerColumn_1 = new TableViewerColumn(tableViewer, SWT.NONE);
		TableColumn tblclmnNewColumn_1 = tableViewerColumn_1.getColumn();
		tblclmnNewColumn_1.setWidth(260);
		tblclmnNewColumn_1.setText("Class");

		TableViewerColumn tableViewerColumn_2 = new TableViewerColumn(tableViewer, SWT.NONE);
		TableColumn tblclmnNewColumn_2 = tableViewerColumn_2.getColumn();
		tblclmnNewColumn_2.setWidth(360);
		tblclmnNewColumn_2.setText("Ref.");

		TableViewerColumn tableViewerColumn_3 = new TableViewerColumn(tableViewer, SWT.NONE);
		TableColumn tblclmnNewColumn_3 = tableViewerColumn_3.getColumn();
		tblclmnNewColumn_3.setWidth(300);
		tblclmnNewColumn_3.setText("State");

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
					.setImageDescriptor(ResourceManager.getImageDescriptor(UIEditorPageDTO.class, "/img/refresh.gif"));
		}
		{
			action_import = new Action("") {
				@Override
				public void run() {
					generate_crud_dto_xml();
				}
			};
			action_import.setToolTipText("DTO CRUD XML Assistant");
			action_import.setImageDescriptor(ResourceManager.getImageDescriptor(UIEditorPageDTO.class, "/img/180.png"));
		}
		{
			action_generate = new Action("") {
				@Override
				public void run() {
					generate();
				}
			};
			action_generate.setToolTipText("Generate selected");
			action_generate
					.setImageDescriptor(ResourceManager.getImageDescriptor(UIEditorPageDTO.class, "/img/compile.png"));
		}
		{
			action_validate = new Action("") {
				@Override
				public void run() {
					try {
						validate();
					} catch (Throwable e) {
						e.printStackTrace();
						EclipseMessageHelpers.show_error(e);
					}
				}
			};
			action_validate.setToolTipText("Validate all");
			action_validate
					.setImageDescriptor(ResourceManager.getImageDescriptor(UIEditorPageDTO.class, "/img/validate.gif"));
		}
		{
			action_openSQL = new Action("") {
				@Override
				public void run() {
					open_sql();
				}
			};
			action_openSQL.setToolTipText("Open SQL file");
			action_openSQL
					.setImageDescriptor(ResourceManager.getImageDescriptor(UIEditorPageDTO.class, "/img/qrydoc.gif"));
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
					.setImageDescriptor(ResourceManager.getImageDescriptor(UIEditorPageDTO.class, "/img/xmldoc.gif"));
		}
		{
			action_open_target = new Action("") {
				@Override
				public void run() {
					open_generated_source_file();
				}
			};
			action_open_target.setImageDescriptor(
					ResourceManager.getImageDescriptor(UIEditorPageDTO.class, "/img/GeneratedFile.gif"));
			action_open_target.setToolTipText("Go to generated source");
		}
		{
			action_genTmpFieldTags = new Action("") {
				@Override
				public void run() {
					gen_tmp_field_tags();
				}
			};
			action_genTmpFieldTags
					.setImageDescriptor(ResourceManager.getImageDescriptor(UIEditorPageDTO.class, "/img/177.png"));
			action_genTmpFieldTags.setToolTipText("Fields Definition Assistant");
		}
	}

	protected void gen_tmp_field_tags() {
		try {
			List<Item> items = prepare_selected_items();
			if (items == null) {
				return;
			}
			String class_name = items.get(0).get_name();
			String ref = items.get(0).getRef();
			EclipseEditorHelpers.open_tmp_field_tags_sync(class_name, ref, editor2.get_project(), editor2);
		} catch (Throwable e) {
			e.printStackTrace();
			EclipseMessageHelpers.show_error(e);
		}
	}

	protected void generate_crud_dto_xml() {
		try {
			EclipseCrudXmlHelpers.get_crud_dto_xml(getShell(), editor2);
		} catch (Throwable e) {
			e.printStackTrace();
			EclipseMessageHelpers.show_error(e);
		}
	}

	protected void validate() throws Exception {
		final List<Item> items = reload_table();
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
					StringBuilder output_dir_abs_path = new StringBuilder();
					// !!!! after 'try'
					IDtoCG gen = EclipseTargetLanguageHelpers.create_dto_cg(con, editor2, settings, output_dir_abs_path);
					for (int i = 0; i < items.size(); i++) {
						if (monitor.isCanceled()) {
							return;
						}
						String dto_class_name = items.get(i).get_name();
						monitor.subTask(dto_class_name);
						try {
							String file_content[] = gen.translate(dto_class_name);
							String file_abs_path = EclipseTargetLanguageHelpers.get_rel_path(editor2, output_dir_abs_path.toString(),
									dto_class_name);
							String old_text = Helpers.load_text_from_file(file_abs_path);
							StringBuilder validation_buff = new StringBuilder();
							if (old_text == null) {
								validation_buff.append("Generated file is missing");
							} else {
								String text = file_content[0];
								if (!old_text.equals(text)) {
									validation_buff.append("Generated file is out of date");
								}
							}
							String status = validation_buff.toString();
							if (status.length() == 0) {
								items.get(i).setStatus(Const.STATUS_OK);
							} else {
								items.get(i).setStatus(status);
								EclipseConsoleHelpers.add_error_msg(dto_class_name + ": " + status);
							}
						} catch (Throwable ex) {
							// ex.printStackTrace();
							String msg = ex.getMessage();
							if (msg == null) {
								msg = "???";
							}
							items.get(i).setStatus(msg);
							EclipseConsoleHelpers.add_error_msg(dto_class_name + ": " + msg);
						}
						monitor.worked(1);
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
		};
		EclipseSyncActionHelper.run_with_progress(getShell(), action);
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
			item.setStatus("");
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
			item.setStatus("");
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
	
	private void generate() {
		final List<Item> items = get_items();
		if (items == null) {
			return;
		}
		tableViewer.refresh();
		// //////////////////////////////////
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
				Connection con = EclipseHelpers.get_connection(editor2);
				monitor.subTask("Connected.");
				try {
					EclipseConsoleHelpers.init_console();
					Settings settings = EclipseHelpers.load_settings(editor2);
					StringBuilder output_dir = new StringBuilder();
					// !!!! after 'try'
					IDtoCG gen = EclipseTargetLanguageHelpers.create_dto_cg(con, editor2, settings, output_dir);
					for (Item item : items) {
						if (monitor.isCanceled()) {
							return;
						}
						String dto_class_name = item.get_name();
						monitor.subTask(dto_class_name);
						try {
							String fileContent[] = gen.translate(dto_class_name);
							String fileName = EclipseTargetLanguageHelpers.get_rel_path(editor2, output_dir.toString(),
									dto_class_name);
							EclipseHelpers.save_text_to_file(fileName, fileContent[0]);
							item.setStatus(Const.STATUS_GENERATED);
							generated = true;
						} catch (Throwable ex) {
							String msg = ex.getMessage();
							if (msg == null) {
								msg = "java.lang.NullPointerException. Try to update XSD files from the tab 'Admin'. Then check existing XML to conform updates.";
							}
							item.setStatus(msg);
							// throw ex; // outer 'catch' cannot read the
							// message
							// !!!! not Internal_Exception to show Exception
							// class
							// throw new Exception(ex);
							EclipseConsoleHelpers.add_error_msg(dto_class_name + ": " + msg);
						}
						monitor.worked(1);
					}
				} finally {
					con.close();
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

	protected void open_sql() {
		try {
			List<Item> items = prepare_selected_items();
			if (items == null) {
				return;
			}
			String relative = items.get(0).getRef();
			if (relative == null || relative.trim().length() == 0) {
				return;
			}
			if (SqlUtils.is_sql_file_ref(relative) == false) {
				return;
			}
			Settings settings = EclipseHelpers.load_settings(editor2);
			String sql_root_folder_relative_path = settings.getFolders().getSql();
			IFolder folder = editor2.get_project().getFolder(sql_root_folder_relative_path);
			if (folder == null) {
				throw new Exception("Folder does not exist: " + sql_root_folder_relative_path);
			}
			IFile file = folder.getFile(relative);
			EclipseEditorHelpers.open_editor_sync(getShell(), file);
		} catch (Throwable e) {
			e.printStackTrace();
			EclipseMessageHelpers.show_error(e);
		}
	}

	protected void open_xml() {
		try {
			IFile file = editor2.find_dto_xml();
			if (file == null) {
				throw new InternalException("File not found: " + Const.DTO_XML);
			}
			final List<Item> items = prepare_selected_items();
			if (items == null || items.size() == 0) {
				EclipseEditorHelpers.open_editor_sync(getShell(), file);
				return;
			}
			EclipseXmlAttrHelpers.goto_dto_class_declaration(getShell(), file, items.get(0).get_name());
		} catch (Throwable e) {
			e.printStackTrace();
			EclipseMessageHelpers.show_error(e);
		}
	}

	protected void open_generated_source_file() {
		try {
			List<Item> items = prepare_selected_items();
			if (items == null) {
				return;
			}
			IFile file = null;
			String dto_class_name = items.get(0).get_name();
			Settings settings = EclipseHelpers.load_settings(editor2);
			file = EclipseTargetLanguageHelpers.find_source_file_in_project_tree(editor2.get_project(), settings,
					dto_class_name, settings.getDto().getScope(), editor2.get_root_file_name());
			EclipseEditorHelpers.open_editor_sync(getShell(), file);
		} catch (Throwable e) {
			e.printStackTrace();
			EclipseMessageHelpers.show_error(e);
		}
	}

	private class ItemLabelProvider extends LabelProvider implements ITableLabelProvider, IColorProvider {

		public String getColumnText(Object element, int columnIndex) {
			String result = "";
			Item item = (Item) element;
			switch (columnIndex) {
			case 0:
				result = item.get_name();
				break;
			case 1:
				result = item.getRef();
				break;
			case 2:
				result = item.getStatus();
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

			if (item.getStatus() != null && item.getStatus().length() > 0
					&& Const.STATUS_OK.equals(item.getStatus()) == false
					&& Const.STATUS_GENERATED.equals(item.getStatus()) == false) {

				return PlatformUI.getWorkbench().getDisplay().getSystemColor(SWT.COLOR_RED);
			}
			return null;
		}
	}

	private static class Item {

		private String name;
		private String ref;
		private String status;

		public String get_name() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getRef() {
			return ref;
		}

		public void setRef(String ref) {
			this.ref = ref;
		}

		public String getStatus() {
			return status;
		}

		public void setStatus(String status) {
			this.status = status;
		}
	}

	private class Filter extends ViewerFilter {

		@Override
		public boolean select(Viewer viewer, Object parentElement, Object element) {

			if (searchString == null || searchString.length() == 0) {
				return true;
			}

			Item item = (Item) element;
			if (item.get_name().matches(searchString)) {
				return true;
			}

			return false;
		}

		private String searchString;

		public void setSearchText(String s) {
			// Search must be a substring of the existing value
			this.searchString = ".*" + s + ".*";
		}
	}

	protected void update_filter() {
		filter.setSearchText(text.getText());
		tableViewer.refresh(); // fires selectionChanged
	}

	@Override
	protected void checkSubclass() {
		// Disable the check that prevents subclassing of SWT components
	}

	private ArrayList<Item> reload_table() throws Exception {
		ArrayList<Item> items = new ArrayList<Item>();
		try {
			String fileName = editor2.get_dto_xml_abs_path();
			InputStream fs = new FileInputStream(fileName);
			try {
				String dto_xml_abs_path = editor2.get_dto_xml_abs_path();
				String dto_xsd_abs_path = editor2.get_dto_xsd_abs_path();
				List<DtoClass> list = SdmUtils.get_dto_classes(dto_xml_abs_path, dto_xsd_abs_path);
				for (DtoClass cls : list) {
					Item item = new Item();
					item.setName(cls.getName());
					item.setRef(cls.getRef());
					items.add(item);
				}
			} finally {
				fs.close();
			}
		} finally {
			tableViewer.setInput(items); // assign anyway
			// tableViewer.refresh();
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
		action_openSQL.setEnabled(enabled);
		action_open_target.setEnabled(enabled);
		action_genTmpFieldTags.setEnabled(enabled);
	}

	public IEditor2 getEditor2() {
		return editor2;
	}

	public void setEditor2(IEditor2 editor2) {
		this.editor2 = editor2;
	}
}
