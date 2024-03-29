/*
    Copyright 2011-2024 sqldalmaker@gmail.com
    SQL DAL Maker Website: https://sqldalmaker.sourceforge.net/
    Read LICENSE.txt in the root of this project/archive for details.
 */
package com.sqldalmaker.common;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

/*
 * @author sqldalmaker@gmail.com
 *
 * 28.12.2023 8:29 1.292
 * 16.11.2022 8:02 1.269
 * 19.07.2022 13:04
 * 17.05.2021 11:28
 *
 */
public class XmlParser {

    private final Unmarshaller unmarshaller;

    public XmlParser(String context_path, String xsd_file_name) throws Exception {
        ClassLoader cl = XmlParser.class.getClassLoader();
        JAXBContext jc = JAXBContext.newInstance(context_path, cl);
        unmarshaller = jc.createUnmarshaller();
        // http://javafaq.nu/java-example-code-988.html
        SchemaFactory sf = SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema" /*XMLConstants.W3C_XML_SCHEMA_NS_URI*/);
        InputStream is = Files.newInputStream(Paths.get(xsd_file_name));
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
