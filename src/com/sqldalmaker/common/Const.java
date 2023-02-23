/*
    Copyright 2011-2023 sqldalmaker@gmail.com
    SQL DAL Maker Website: https://sqldalmaker.sourceforge.net/
    Read LICENSE.txt in the root of this project/archive for details.
 */
package com.sqldalmaker.common;

/**
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

    public static final String COMMENT_GENERATED_DTO_XML = "?>\n\n<!-- This is just an output of utility. Copy/Cut 'dto-class' nodes from here and Paste them into 'dto.xml'. -->\n";
    public static final String COMMENT_GENERATED_DAO_XML = "?>\n\n<!-- This is just an output of utility. Copy/Cut 'crud...' and 'query...' nodes from here and Paste them into DAO XML. -->\n";

    public static final String GOLANG_SCOPES_ERR = "Error in 'settings.xml': \n" +
            "empty values for '<dto scope' and '<dao scope' are not allowed";
}
