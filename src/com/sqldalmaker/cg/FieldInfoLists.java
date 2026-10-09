/*
    Copyright 2011-2026 sqldalmaker@gmail.com
    SQL DAL Maker Website: https://sqldalmaker.sourceforge.net/
    Read LICENSE.txt in the root of this project/archive for details.
 */
package com.sqldalmaker.cg;

import java.util.List;
import java.util.Map;

/*
 * 09.10.2026 1.332 Claude
 *
 * FieldInfo is immutable, so refining one means putting the new one in place of the old one
 * everywhere the old one is referenced: a list of fields and a map "column name -> field"
 * built over the same objects.
 */
class FieldInfoLists {

    // identity, not equals(): a list may have two fields with the same column name
    static void replace(List<FieldInfo> list, FieldInfo old_fi, FieldInfo new_fi) {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i) == old_fi) {
                list.set(i, new_fi);
            }
        }
    }

    static void replace(List<FieldInfo> list, Map<String, FieldInfo> map, FieldInfo old_fi, FieldInfo new_fi) {
        replace(list, old_fi, new_fi);
        String col_name = old_fi.getColumnName();
        if (map.get(col_name) == old_fi) {
            map.put(col_name, new_fi);
        }
    }
}
