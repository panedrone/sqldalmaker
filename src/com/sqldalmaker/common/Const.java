/*
 * Copyright 2011-2020 sqldalmaker@gmail.com
 * SQL DAL Maker Website: http://sqldalmaker.sourceforge.net
 * Read LICENSE.txt in the root of this project/archive for details.
 *
 */
package com.sqldalmaker.common;

/**
 *
 * @author sqldalmaker@gmail.com
 *
 */
public class Const {

	public static final String DAO_XSD = "dao.xsd";
	public static final String DTO_XSD = "dto.xsd";

	public static final String SETTINGS_XML = "settings.xml";
	public static final String SETTINGS_XSD = "settings.xsd";

	public static final String EMPTY_DAO_XML = "dao.xml";
	public static final String DTO_XML = "dto.xml";

	public static final String STATUS_OK = "OK";
	public static final String STATUS_GENERATED = "Generated successfully";

	public static final String OUTPUT_FILE_IS_OUT_OF_DATE = "Generated file is out of date";
	public static final String OUTPUT_FILE_IS_MISSING = "Generated file is missing";

	public static final String COMMENT_GENERATED_DTO_XML = "?>\n\n<!-- This is just an output of utility. Cut 'dto-class' nodes from here and paste them into 'dto.xml'. -->\n";
	public static final String COMMENT_GENERATED_DAO_XML = "?>\n\n<!-- This is just an output of utility. Cut 'crud...' and 'query...' nodes from here and distribute them by your DAO XML as you need. -->\n";
}
