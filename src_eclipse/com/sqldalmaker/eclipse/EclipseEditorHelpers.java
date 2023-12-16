/*
 * Copyright 2011-2023 sqldalmaker@gmail.com
 * Read LICENSE.txt in the root of this project/archive.
 * Project web-site: https://sqldalmaker.sourceforge.net/
 */
package com.sqldalmaker.eclipse;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.sql.Connection;

import javax.xml.bind.Marshaller;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorRegistry;
import org.eclipse.ui.IPersistableElement;
import org.eclipse.ui.IStorageEditorInput;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.FileEditorInput;

import com.sqldalmaker.common.Const;
import com.sqldalmaker.common.InternalException;
import com.sqldalmaker.common.XmlHelpers;
import com.sqldalmaker.jaxb.sdm.DtoClass;
import com.sqldalmaker.jaxb.sdm.ObjectFactory;
import com.sqldalmaker.jaxb.sdm.Sdm;

/**
 * @author sqldalmaker@gmail.com
 *
 */
public class EclipseEditorHelpers {

	public static void open_editor_sync(ByteArrayOutputStream output_stream, String full_path, String title)
			throws Exception {
		IEditorInput ei = new MyStorageEditorInput(output_stream, full_path, title);
		open_editor_sync(ei, full_path);
	}

	private static String get_not_found_message(String msg, String path) {
		if (path != null) {
			msg += ": " + path;
		}
		msg += "\r\nEnsure upper/lower case.";
		msg += "\r\nTry to refresh the project tree (F5).";
		return msg;
	}

	public static IEditorPart open_editor_sync(Shell shell, IFile file) throws Exception {
		if (file == null) {
			throw new Exception(get_not_found_message("File not found", null));
		}
		boolean exists = file.exists(); // false for Go
		if (!exists) {
			IProject project = file.getProject();
			String path = file.getFullPath().toPortableString();
			if (project == null) {
				throw new Exception("No project for " + path);
			}
			IContainer folder = file.getParent();
			if (folder == null) {
				throw new Exception("No parent detected for " + path);
			}
			String name = file.getName();
			IResource res = folder.findMember(name, true);
			if (!(res instanceof IFile)) {
				throw new Exception(get_not_found_message("Not detected as file", path));
			}
		}
		if (exists || file.getName().endsWith(".go")) {
			return open_editor_sync(new FileEditorInput(file), file.getName());
		}
		String title = file.getFullPath().toPortableString();
		throw new Exception(get_not_found_message("File not found", title));
	}

//	private static void create_new_file_sync(Shell shell, IFile new_file_handle, InputStream initial_contents,
//			String title, IProgressMonitor monitor) {
//		CreateFileOperation op = new CreateFileOperation(new_file_handle, null, initial_contents, title);
//		try {
//			// see bug
//			// https://bugs.eclipse.org/bugs/show_bug.cgi?id=219901
//			// directly execute the operation so that the undo state is
//			// not preserved. Making this undoable resulted in too many
//			// accidental file deletions.
//			//
//			op.execute(monitor, WorkspaceUndoUtil.getUIInfoAdapter(shell));
//		} catch (final ExecutionException e) {
//			shell.getDisplay().syncExec(new Runnable() {
//				public void run() {
//					EclipseMessageHelpers.show_error(e);
//				}
//			});
//		}
//	}

	public static IEditorPart open_editor_sync(IEditorInput editor_input, String file_name)
			throws PartInitException, InternalException {
		IEditorRegistry r = PlatformUI.getWorkbench().getEditorRegistry();
		IEditorDescriptor desc = r.getDefaultEditor(file_name);
		// Eclipse for RCP and RAP Developers' does not have SQL editor
		if (desc == null/* || file_name.endsWith(".go") */) {
			desc = r.getDefaultEditor("*.txt");
		}
		if (desc == null) {
			throw new InternalException("Cannot obtain editor descriptor.");
		}
		IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		IEditorPart editor_part;
		try {
			editor_part = page.openEditor(editor_input, desc.getId());
		} catch (Throwable ex) {
			desc = r.getDefaultEditor("*.txt");
			if (desc == null) {
				throw new InternalException("Cannot obtain editor descriptor.");
			}
			editor_part = page.openEditor(editor_input, desc.getId());
		}
		return editor_part;
	}

	public static void open_dto_xml_in_editor_sync(com.sqldalmaker.jaxb.sdm.ObjectFactory object_factory, Sdm root)
			throws Exception {

		Marshaller marshaller = XmlHelpers.create_marshaller(object_factory.getClass().getPackage().getName(),
				Const.SDM_XSD);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try {
			marshaller.marshal(root, out);
			out.flush();
			String text = new String(out.toByteArray());
			String[] parts = text.split("\\?>");
			text = parts[0] + Const.COMMENT_GENERATED_DTO_XML + parts[1];
			ByteArrayOutputStream out2 = new ByteArrayOutputStream();
			for (int i = 0; i < text.length(); ++i)
				out2.write(text.charAt(i));
			try {
				open_editor_sync(out2, "dto.xml", "_dto.xml"); // '%' throws URI exception in NB
			} finally {
				out2.close();
			}
		} finally {
			out.close();
		}
	}

	public static void open_dao_xml_in_editor_sync(String instance_name, String file_name, Object root)
			throws Exception {
		Marshaller marshaller = XmlHelpers.create_marshaller(instance_name, Const.DAO_XSD);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try {
			marshaller.marshal(root, out);
			out.flush();
			String text = new String(out.toByteArray());
			String[] parts = text.split("\\?>");
			text = parts[0] + Const.COMMENT_GENERATED_DAO_XML + parts[1];
			ByteArrayOutputStream out2 = new ByteArrayOutputStream();
			for (int i = 0; i < text.length(); ++i)
				out2.write(text.charAt(i));
			try {
				open_editor_sync(out2, file_name, file_name); // '%' throws URI exception in NB
			} finally {
				out2.close();
			}
		} finally {
			out.close();
		}
	}

	public static void open_tmp_field_tags_sync(String class_name, String ref, IProject project, final IEditor2 editor2)
			throws Exception {
		String project_root = EclipseHelpers.get_absolute_dir_path_str(project);
		Connection con = EclipseHelpers.get_connection(editor2);
		try {
			ObjectFactory object_factory = new ObjectFactory();
			Sdm sdm = object_factory.createSdm();
			DtoClass cls = object_factory.createDtoClass();
			cls.setName(class_name);
			cls.setRef(ref);
			sdm.getDtoClass().add(cls);
			EclipseHelpers.gen_tmp_field_tags(con, object_factory, cls, project_root, editor2);
			open_dto_xml_in_editor_sync(object_factory, sdm);
		} finally {
			con.close();
		}
	}

	public static class MyStorageEditorInput extends PlatformObject implements IStorageEditorInput {
		// ^^ "extends PlatformObject" is copy-paste from FileEditorInput
		private IStorage storage;
		private String title;
		private String full_path;
		private ByteArrayOutputStream output_stream;

		private class MyStorage extends PlatformObject implements IStorage {
			@SuppressWarnings({ "rawtypes", "unchecked" })
			@Override
			public Object getAdapter(Class adapter) {
				return super.getAdapter(adapter);
			}

			@Override
			public InputStream getContents() throws CoreException {
				try {
					// Return new stream as many times as they want. It prevents
					// java.io.IOException: Read error in
					// org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitDocumentProvider.createFakeCompiltationUnit(CompilationUnitDocumentProvider.java:1090)
					// For example,
					// org.eclipse.debug.core.sourcelookup.containers.LocalFileStorage.getContents()
					// creates it each time;
					ByteArrayInputStream res = new ByteArrayInputStream(output_stream.toByteArray());
					return res;
				} catch (Throwable e) {
					return null;
				}
			}

			@Override
			public IPath getFullPath() {
				return new Path(full_path);
			}

			// Returns the name of this storage.
			@Override
			public String getName() {
				return this.getClass().getName();
			}

			@Override
			public boolean isReadOnly() {
				return false;
			}

		} /////////////////////////////////// end of class MyStorageEditorInput.MyStorage

		public MyStorageEditorInput(ByteArrayOutputStream output_stream, String full_path, String title) {
			this.output_stream = output_stream;
			this.full_path = full_path;
			this.title = title;
			this.storage = new MyStorage();
		}

		@Override
		public IStorage getStorage() {
			return storage;
		}

		@Override
		public boolean exists() {
			return true;
		}

		@Override
		public ImageDescriptor getImageDescriptor() {
			return ImageDescriptor.getMissingImageDescriptor();
		}

		@Override
		public String getName() {
			return title; // title of editor tab
		}

		@Override
		public IPersistableElement getPersistable() {
			return null;
		}

		@Override
		public String getToolTipText() {
			return title;
		}

		@SuppressWarnings({ "rawtypes", "unchecked" })
		@Override
		public Object getAdapter(Class adapter) {
			return super.getAdapter(adapter);
		}

		@Override
		public boolean equals(Object obj) {
			return false; // always create a new one
			// equals(Object obj) is based on implementation from
			// FileEditorInput class.
			// It prevents opening multiple copies of the same resource
			// if (this == obj) {
			// return true;
			// }
			// if (!(obj instanceof PluginResourceEditorInput)) {
			// return false;
			// }
			// ByteArrayOutputStreamEditorInput other =
			// (ByteArrayOutputStreamEditorInput) obj;
			// return resPath.equals(other.resPath);
		}

		@Override
		public int hashCode() {
			return super.hashCode(); // just to avoid FireBug warning
		}

	} //////////////// end of class EclipseEditorUtils.MyStorageEditorInput

} ///////////////////////////////////// end of class EclipseEditorUtils
