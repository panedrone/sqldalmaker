/*
 * Copyright 2011-2020 sqldalmaker@gmail.com
 * SQL DAL Maker Website: http://sqldalmaker.sourceforge.net
 * Read LICENSE.txt in the root of this project/archive for details.
 */
package com.sqldalmaker.cg;

/**
 * @author sqldalmaker@gmail.com
 */
public interface IDtoCG {

    String[] translate(String dto_class_name) throws Exception;
}
