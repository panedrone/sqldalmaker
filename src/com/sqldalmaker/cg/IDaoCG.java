/*
    Copyright 2011-2024 sqldalmaker@gmail.com
    SQL DAL Maker Website: https://sqldalmaker.sourceforge.net/
    Read LICENSE.txt in the root of this project/archive for details.
 */
package com.sqldalmaker.cg;

import com.sqldalmaker.jaxb.sdm.DaoClass;

/*
 * 16.11.2022 08:02 1.269
 * 06.08.2022 08:37 1.261
 * 27.05.2022 01:17 1.246
 * 20.04.2022 09:58
 * 08.05.2021 22:29 1.200
 * 03.09.2019 15:55
 * 07.02.2019 19:50 initial commit
 *
 */
public interface IDaoCG {

    String[] translate(DaoClass dao_class) throws Exception;
}
