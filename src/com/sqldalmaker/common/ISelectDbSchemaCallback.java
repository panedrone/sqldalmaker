/*
    Copyright 2011-2024 sqldalmaker@gmail.com
    SQL DAL Maker Website: https://sqldalmaker.sourceforge.net/
    Read LICENSE.txt in the root of this project/archive for details.
 */
package com.sqldalmaker.common;

/*
 * @author sqldalmaker@gmail.com
 *
 * 28.12.2023 08:29 1.292
 * 16.11.2022 08:02 1.269
 * 19.07.2022 13:04
 * 17.05.2021 11:28
 */
public interface ISelectDbSchemaCallback {

    void process_ok(
            boolean schema_in_xml,
            String selected_schema,
            boolean skip_used,
            boolean include_views,
            boolean plural_to_singular,
            boolean crud_auto,
            boolean add_fk_access);
}
