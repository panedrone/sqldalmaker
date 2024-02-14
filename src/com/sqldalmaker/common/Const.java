/*
    Copyright 2011-2024 sqldalmaker@gmail.com
    SQL DAL Maker Website: https://sqldalmaker.sourceforge.net/
    Read LICENSE.txt in the root of this project/archive for details.
 */
package com.sqldalmaker.common;

/*
 * 17.12.2023 02:16 1.292 sdm.xml
 * 16.11.2022 08:02 1.269
 * 07.02.2019 19:50 initial commit
 *
 */
public class Const {

    public static final String DAO_XSD = "dao.xsd";
    public static final String SDM_XSD = "sdm.xsd";

    public static final String SETTINGS_XML = "settings.xml";
    public static final String SETTINGS_XSD = "settings.xsd";

    public static final String EMPTY_DAO_XML = "dao.xml";
    public static final String SDM_XML = "sdm.xml";

    public static final String STATUS_OK = "OK";
    public static final String STATUS_GENERATED = "Generated successfully";

    public static final String OUTPUT_FILE_IS_OUT_OF_DATE = "Generated file is out of date";
    public static final String OUTPUT_FILE_IS_MISSING = "Generated file is missing";

    public static final String COMMENT_GENERATED_SDM_XML = "?>\n\n<!-- This is just an output of utility. Copy-paste 'dto-class' nodes from here to 'sdm.xml'. -->\n";
    public static final String COMMENT_GENERATED_DAO_XML = "?>\n\n<!-- This is just an output of utility. Copy-paste 'crud...' and 'query...' nodes from here to DAO XML. -->\n";

    public static final String GOLANG_SCOPES_ERR = "Error in 'settings.xml': \n" +
            "empty values for '<dto scope' and '<dao scope' are not allowed";

    public static class Root {
        public static final String JAVA = "java.dal";
        public static final String CPP = "cpp.dal";
        public static final String PHP = "php.dal";
        public static final String PYTHON = "python.dal";
        public static final String GO = "go.dal";
    }

    public static final String VALIDATE_SDM_DTO_MODELS = "validating DTO/models ... OK";
    public static final String VALIDATE_SDM_DAO = "validating DAO ... OK";

    public static final String GENERATE_SDM_DTO_MODELS = "generating DTO/model classes ... OK";
    public static final String GENERATE_SDM_DAO = "generating DAO classes ... OK";

    public static final String VALIDATE_DAO_XML = "validating ... OK";
    public static final String GENERATE_DAO_XML = "generating DAO class ... OK";
}
