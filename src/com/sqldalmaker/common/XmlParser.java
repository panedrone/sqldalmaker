/*
    Copyright 2011-2022 sqldalmaker@gmail.com
    SQL DAL Maker Website: https://sqldalmaker.sourceforge.net/
    Read LICENSE.txt in the root of this project/archive for details.
 */
package com.sqldalmaker.common;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

/**
 * @author sqldalmaker@gmail.com
 */
public class XmlParser {

    private final Unmarshaller unmarshaller;

    public XmlParser(String context_path,
                     String xsd_file_name) throws Exception {

        ClassLoader cl = XmlParser.class.getClassLoader();
        JAXBContext jc = JAXBContext.newInstance(context_path, cl);
        unmarshaller = jc.createUnmarshaller();
        // http://javafaq.nu/java-example-code-988.html
        SchemaFactory sf = SchemaFactory
                .newInstance("http://www.w3.org/2001/XMLSchema" /*XMLConstants.W3C_XML_SCHEMA_NS_URI*/);
        InputStream is = new FileInputStream(xsd_file_name);
        try {
            // http://www.ibm.com/developerworks/xml/library/x-jaxpval/index.html
            Source schema_source = new StreamSource(is);
            Schema schema = sf.newSchema(schema_source);
            unmarshaller.setSchema(schema);
        } finally {
            is.close(); // !!!! - after newSchema
        }
    }

    @SuppressWarnings("unchecked")
    public <T> T unmarshal(String xml_abs_file_path) throws Exception {
        try {
            File f = new File(xml_abs_file_path);
            return (T) unmarshaller.unmarshal(f);
        } catch (JAXBException e) {
            if (e.getMessage() == null) {
                throw new Exception(e.getLinkedException());
            } else {
                throw new Exception(e);
            }
        }
    }
}
