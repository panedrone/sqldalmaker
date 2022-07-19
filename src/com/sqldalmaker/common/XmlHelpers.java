/*
    Copyright 2011-2022 sqldalmaker@gmail.com
    SQL DAL Maker Website: http://sqldalmaker.sourceforge.net
    Read LICENSE.txt in the root of this project/archive for details.
 */
package com.sqldalmaker.common;

import com.sqldalmaker.jaxb.dao.DaoClass;
import com.sqldalmaker.jaxb.dto.DtoClasses;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import java.io.ByteArrayOutputStream;

/**
 * @author sqldalmaker@gmail.com
 */
public class XmlHelpers {

    public static Marshaller create_marshaller(
            String instance_name,
            String xsd) throws Exception {

        ClassLoader cl = XmlHelpers.class.getClassLoader();
        JAXBContext jc = JAXBContext.newInstance(instance_name, cl);
        // http://docs.oracle.com/javase/6/docs/api/javax/xml/bind/Marshaller.html
        Marshaller marshaller = jc.createMarshaller();
        marshaller.setProperty("jaxb.formatted.output", true);
        // m.setProperty("jaxb.schemaLocation", XMLConstants.W3C_XML_SCHEMA_NS_URI);
        marshaller.setProperty("jaxb.noNamespaceSchemaLocation", xsd);
        return marshaller;
    }

    private static String get_xml_text(
            String instance_name,
            Object root, String xsd) throws Exception {

        Marshaller marshaller = create_marshaller(instance_name, xsd);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        String text;
        try {
            marshaller.marshal(root, out);
            out.flush();
            text = out.toString();
        } finally {
            out.close();
        }
        return text;
    }

    public static String get_dto_xml_text(
            com.sqldalmaker.jaxb.dto.ObjectFactory object_factory,
            DtoClasses root) throws Exception {

        String text = get_xml_text(object_factory.getClass().getPackage().getName(), root, Const.DTO_XSD);
        return text;
    }

    public static String get_dao_xml_text(
            com.sqldalmaker.jaxb.dao.ObjectFactory object_factory,
            DaoClass root) throws Exception {

        String text = get_xml_text(object_factory.getClass().getPackage().getName(), root, Const.DAO_XSD);
        return text;
    }
}
