/*
	Copyright 2011-2024 sqldalmaker@gmail.com
	Read LICENSE.txt in the root of this project/archive.
	Project web-site: https://sqldalmaker.sourceforge.net/
*/
package com.sqldalmaker.eclipse;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.IProgressService;

/**
 *
 * @author sqldalmaker@gmail.com
 *
 */
public class EclipseSyncActionHelper {

	// http://www.eclipse.org/articles/Article-Concurrency/jobs-api.html

	public static void run_with_progress(Shell shell, final EclipseSyncAction action) {

		IRunnableWithProgress op = new IRunnableWithProgress() {
			@Override
			public void run(IProgressMonitor monitor) throws InvocationTargetException {
				monitor.beginTask(action.get_name(), action.get_total_work());
				try {
					action.run_with_progress(monitor);
				} catch (Throwable ex) {
					throw new InvocationTargetException(ex, ex.getMessage());
				} finally {
					monitor.done();
				}
			}
		};
		// Use the progress service to execute the runnable
		IProgressService service = PlatformUI.getWorkbench().getProgressService();
		try {
			// see javadocs for IRunnableContext.run
			service.run(true, true, op);
		} catch (InvocationTargetException e) {
			e.printStackTrace();
			EclipseMessageHelpers.show_error(e);
		} catch (InterruptedException e) {
			// do nothing
		}
	}
}
