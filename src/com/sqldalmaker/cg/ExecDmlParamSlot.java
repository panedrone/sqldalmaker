/*
    Copyright 2011-2024 sqldalmaker@gmail.com
    SQL DAL Maker Website: https://sqldalmaker.sourceforge.net/
    Read LICENSE.txt in the root of this project/archive for details.
 */
package com.sqldalmaker.cg;

import java.util.List;

/*
 * One parameter of an "<exec-dml..." method, after its declaration is parsed.
 * Data only. It is created and filled by 'ExecDmlParams.parse'.
 */
public class ExecDmlParamSlot {

    public enum Kind {
        PLAIN,        // "p_id"
        MAPPED,       // "on_dto:Dto" or "on_dto~Dto" - one implicit cursor
        CURSOR_ARRAY  // "[on_dto_1:Dto1, on_dto_2:Dto2]" - an array of them
    }

    public Kind kind;

    // position in the declaration; the first parameter is special in Java and Go
    public int index;

    // MAPPED: {"on_dto", "Dto"}
    public String[] mapping;

    // CURSOR_ARRAY: the same, one per element
    public List<String[]> cursor_mappings;
}
