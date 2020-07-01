/*
 * Copyright 2011-2018 sqldalmaker@gmail.com
 * SQL DAL Maker Website: http://sqldalmaker.sourceforge.net
 * Read LICENSE.txt in the root of this project/archive for details.
 */
package com.sqldalmaker.eclipse;

import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleConstants;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.IConsoleView;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;

/**
 *
 * @author sqldalmaker@gmail.com
 *
 */
public class EclipseConsoleHelpers {

	private static final String CONSOLE_NAME = "SQL DAL Maker";

	private static MessageConsole find_console(IConsoleManager con_man) {

		IConsole[] existing = con_man.getConsoles();

		for (int i = 0; i < existing.length; i++) {

			if (CONSOLE_NAME.equals(existing[i].getName())) {

				return (MessageConsole) existing[i];
			}
		}

		return null;
	}

	public static void init_console() {

		ConsolePlugin plugin = ConsolePlugin.getDefault();

		IConsoleManager con_man = plugin.getConsoleManager();

		// https://stackoverflow.com/questions/720963/writing-to-the-eclipse-console
		MessageConsole my_console = find_console(con_man);

		if (my_console != null) {

			my_console.clearConsole();
		}
	}

	private static IConsoleManager get_con_man() {
		// https://wiki.eclipse.org/FAQ_How_do_I_write_to_the_console_from_a_plug-in%3F
		ConsolePlugin plugin = ConsolePlugin.getDefault();
		IConsoleManager con_man = plugin.getConsoleManager();
		return con_man;
	}

	private static MessageConsole activate_message_console() {
		IConsoleManager con_man = get_con_man();
		MessageConsole my_console = find_console(con_man);
		if (my_console == null) {
			// no console found, so create a new one
			my_console = new MessageConsole(CONSOLE_NAME, null);
			// http://useof.org/java-open-source/org.eclipse.ui.console.MessageConsole
			// activate is called before add
			my_console.activate();
			con_man.addConsoles(new IConsole[] { my_console });
		} else {
			my_console.activate();
		}
		con_man.showConsoleView(my_console);
		return my_console;
	}

	public static void add_info_msg(String msg) {
		MessageConsole my_console = activate_message_console();
		MessageConsoleStream out = my_console.newMessageStream();
		out.setActivateOnWrite(true); // // http://useof.org/java-open-source/org.eclipse.ui.console.MessageConsole
		// https://stackoverflow.com/questions/207947/how-do-i-get-a-platform-dependent-new-line-character
		out.println(String.format("[INFO] %s%n", msg));
		// myConsole.activate();
		// === console (tab) may be created, but console view
		// is hidden and located behind other consoles
		bring_to_front(my_console);
	}

	public static void add_error_msg(String msg) {
		MessageConsole my_console = activate_message_console();
		MessageConsoleStream out = my_console.newMessageStream();
		out.setActivateOnWrite(true); // // http://useof.org/java-open-source/org.eclipse.ui.console.MessageConsole
		// https://stackoverflow.com/questions/207947/how-do-i-get-a-platform-dependent-new-line-character
//		Color old_color = out.getColor();
//		Color red = new Color(old_color.getDevice(), 255, 0, 0);
//		out.setColor(red);
		out.println(String.format("[ERROR] %s%n", msg));
//		out.setColor(old_color);
//		red.dispose();
		// myConsole.activate();
		// === console (tab) may be created, but console view
		// is hidden and located behind other consoles
		bring_to_front(my_console);
	}

	private static void bring_to_front(final IConsole console) {
		// https://stackoverflow.com/questions/29783017/add-custom-console-to-eclipse-console-list
		// https://wiki.eclipse.org/FAQ_How_do_I_write_to_the_console_from_a_plug-in%3F
		// https://stackoverflow.com/questions/1265174/nullpointerexception-in-platformui-getworkbench-getactiveworkbenchwindow-get
		Display.getDefault().asyncExec(new Runnable() {
			@Override
			public void run() {
				try {
					IWorkbench wb = PlatformUI.getWorkbench();
					if (wb == null) {
						return;
					}
					IWorkbenchWindow wbw = wb.getActiveWorkbenchWindow();
					if (wbw == null) {
						return;
					}
					IWorkbenchPage page = wbw.getActivePage();
					if (page == null) {
						return;
					}
					String id = IConsoleConstants.ID_CONSOLE_VIEW;
					IConsoleView view = (IConsoleView) page.showView(id);
					view.display(console);
				} catch (PartInitException e) {
					e.printStackTrace();
				}
			}
		});
	}
}