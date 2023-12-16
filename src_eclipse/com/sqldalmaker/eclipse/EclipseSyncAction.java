/*
	Copyright 2011-2023 sqldalmaker@gmail.com
	Read LICENSE.txt in the root of this project/archive.
	Project web-site: https://sqldalmaker.sourceforge.net/
*/
package com.sqldalmaker.eclipse;

import org.eclipse.core.runtime.IProgressMonitor;

/**
 * http://www.eclipse.org/articles/Article-Concurrency/jobs-api.html
 *
 * Each method of EclipseSyncAction can query and update the GUI only with
 *
 * Display.getDefault().asyncExec(new Runnable() { public void run() { } });
 *
 * @author sqldalmaker@gmail.com
 *
 */
interface EclipseSyncAction {

	int get_total_work();

	String get_name();

	void run_with_progress(IProgressMonitor monitor) throws Exception;
}
