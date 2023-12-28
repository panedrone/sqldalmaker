/*
    Copyright 2011-2023 sqldalmaker@gmail.com
    SQL DAL Maker Website: https://sqldalmaker.sourceforge.net/
    Read LICENSE.txt in the root of this project/archive for details.
 */
package com.sqldalmaker.cg;

/*
 * 16.11.2022 08:02 1.269
 * 16.04.2022 17:35 1.219
 * 08.05.2021 22:29 1.200
 * 07.02.2019 19:50 initial commit
 *
 */
public interface IDtoCG {

    String[] translate(String dto_class_name) throws Exception;
}
