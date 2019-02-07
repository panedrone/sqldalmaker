/*
 * Copyright 2011-2018 sqldalmaker@gmail.com
 * SQL DAL Maker Website: http://sqldalmaker.sourceforge.net
 * Read LICENSE.txt in the root of this project/archive for details.
 */
package com.sqldalmaker.common;

/**
 *
 * @author sqldalmaker@gmail.com
 *
 */
public interface ISelectDbSchemaCallback {

    void process_ok(String selected_schema, boolean skip_used, boolean include_views, boolean plural_to_singular, boolean crud_auto, boolean add_fk_access);
}
