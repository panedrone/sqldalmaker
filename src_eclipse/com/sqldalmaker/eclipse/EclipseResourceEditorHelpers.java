/*
 * Copyright 2011-2020 sqldalmaker@gmail.com
 * SQL DAL Maker Website: http://sqldalmaker.sourceforge.net
 * Read LICENSE.txt in the root of this project/archive for details.
 */
package com.sqldalmaker.eclipse;

import java.io.InputStream;

import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorRegistry;
import org.eclipse.ui.IPersistableElement;
import org.eclipse.ui.IStorageEditorInput;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;

import com.sqldalmaker.cg.Helpers;
import com.sqldalmaker.common.InternalException;

/**
 *
 * @author sqldalmaker@gmail.com
 *
 */
public class EclipseResourceEditorHelpers {

	public static void open_resource_file_in_editor(String full_path, String file_name_to_display) {
		try {
			IEditorInput editor_input = new MyPluginResourceEditorInput(full_path, file_name_to_display);
			EclipseEditorHelpers.open_editor_sync(editor_input, file_name_to_display);
		} catch (final Throwable e) {
			e.printStackTrace();
			EclipseMessageHelpers.show_error(e);
		}
	}

	public static void open_resource_file_in_text_editor(String full_path, String title) {
		try {
			IEditorInput editor_input = new MyPluginResourceEditorInput(full_path, title);
			IEditorRegistry r = PlatformUI.getWorkbench().getEditorRegistry();
			IEditorDescriptor desc = r.getDefaultEditor("*.txt");
			if (desc == null) {
				throw new InternalException("Cannot obtain editor descriptor.");
			}
			IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
			page.openEditor(editor_input, desc.getId());
		} catch (final Throwable e) {
			e.printStackTrace();
			EclipseMessageHelpers.show_error(e);
		}
	}

	private static class MyPluginResourceEditorInput extends PlatformObject implements IStorageEditorInput {
		// ^^ "extends PlatformObject" is from FileEditorInput :)
		private IStorage storage;
		private String title;
		private String full_path;

		private class MyStorage2 implements IStorage {
			@SuppressWarnings({ "rawtypes", "unchecked" })
			@Override
			public Object getAdapter(Class arg0) {
				return null;
			}

			@Override
			public InputStream getContents() throws CoreException {
				try {
					// Return new stream as many times as they want. It
					// prevents
					// java.io.IOException: Read error in
					// org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitDocumentProvider.createFakeCompiltationUnit(CompilationUnitDocumentProvider.java:1090)
					// For example,
					// org.eclipse.debug.core.sourcelookup.containers.LocalFileStorage.getContents()
					// creates it each time;
					return Helpers.get_resource_as_stream_2(full_path);
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
				return true;
			}

		} /////////////////////////////////// end of class

		/////////////////////////////////// MyPluginResourceEditorInput.MyStorage2
		public MyPluginResourceEditorInput(String full_path, String title) {

			this.full_path = full_path;
			this.title = title;
			this.storage = new MyStorage2();
		}

		@Override
		public IStorage getStorage() {
			return storage;
		}

		@Override
		public boolean exists() {
			return false;
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

		// equals(Object obj) is based on implementation from
		// FileEditorInput class.
		// It prevents opening of multiple copies of the same resource
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (!(obj instanceof MyPluginResourceEditorInput)) {
				return false;
			}
			MyPluginResourceEditorInput other = (MyPluginResourceEditorInput) obj;
			return full_path.equals(other.full_path);
		}

	} /////////////////////////////////// end of class MyPluginResourceEditorInput
}