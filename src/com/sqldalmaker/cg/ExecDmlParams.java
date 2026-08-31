/*
    Copyright 2011-2026 sqldalmaker@gmail.com
    SQL DAL Maker Website: https://sqldalmaker.sourceforge.net/
    Read LICENSE.txt in the root of this project/archive for details.
 */
package com.sqldalmaker.cg;

import java.util.ArrayList;
import java.util.List;

/*
 * The syntax of "<exec-dml method=..." parameters, in one place.
 *
 * It used to be inlined into the four target languages that support implicit
 * cursors, so the rule and its error message existed in four copies. What the
 * languages really differ in is the code they render for each kind of parameter,
 * not in how the declaration is read.
 */
public class ExecDmlParams {

    public static List<ExecDmlParamSlot> parse(String[] param_descriptors) throws Exception {
        List<ExecDmlParamSlot> res = new ArrayList<ExecDmlParamSlot>();
        for (int i = 0; i < param_descriptors.length; i++) {
            String pd = param_descriptors[i].trim();
            ExecDmlParamSlot slot = new ExecDmlParamSlot();
            slot.index = i;
            if (pd.startsWith("[") && pd.endsWith("]")) {
                slot.kind = ExecDmlParamSlot.Kind.CURSOR_ARRAY;
                slot.cursor_mappings = _parse_cursor_array(pd);
            } else {
                String[] mapping = MethodDeclarations.split_mapped_param_descriptor(pd);
                if (mapping == null) {
                    slot.kind = ExecDmlParamSlot.Kind.PLAIN;
                } else {
                    slot.kind = ExecDmlParamSlot.Kind.MAPPED;
                    slot.mapping = mapping;
                }
            }
            res.add(slot);
        }
        return res;
    }

    private static List<String[]> _parse_cursor_array(String pd) throws Exception {
        String inner_list = pd.substring(1, pd.length() - 1);
        String[] implicit_param_descriptors = MethodDeclarations.get_listed_items(inner_list, true);
        List<String[]> res = new ArrayList<String[]>();
        for (String ipd : implicit_param_descriptors) {
            String[] mapping = MethodDeclarations.split_mapped_param_descriptor(ipd);
            if (mapping == null) {
                throw new Exception("Implicit cursors are specified incorrectly."
                        + " Expected syntax: [on_dto_1:Dto1, on_dto_2:Dto2, ...]. Specified: "
                        + "[" + String.join(",", implicit_param_descriptors) + "]");
            }
            res.add(mapping);
        }
        return res;
    }
}
