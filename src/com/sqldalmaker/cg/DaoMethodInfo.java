/*
    Copyright 2011-2026 sqldalmaker@gmail.com
    SQL DAL Maker Website: https://sqldalmaker.sourceforge.net/
    Read LICENSE.txt in the root of this project/archive for details.
 */
package com.sqldalmaker.cg;

/*
 * Nothing but a common type, so that the ordered plan built by
 * 'JaxbUtils.plan_dao_methods' can hold entries of all three kinds
 * in the order they are declared in XML.
 *
 * The implementations carry data and nothing else - they neither parse XML
 * nor render anything, that is the job of 'JaxbUtils' and of the target languages.
 */
public interface DaoMethodInfo {

    // prefix of the error message: "<query method="..." ref="..."...\n"
    String get_error_context();
}
