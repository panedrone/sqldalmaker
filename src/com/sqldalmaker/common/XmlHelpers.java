/*
    Copyright 2011-2023 sqldalmaker@gmail.com
    SQL DAL Maker Website: https://sqldalmaker.sourceforge.net/
    Read LICENSE.txt in the root of this project/archive for details.
 */
package com.sqldalmaker.common;

import com.sqldalmaker.jaxb.sdm.DaoClass;
import com.sqldalmaker.jaxb.sdm.Sdm;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import java.io.ByteArrayOutputStream;

import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.transform.*;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

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

    public static String get_sdm_xml_text(
            com.sqldalmaker.jaxb.sdm.ObjectFactory object_factory,
            Sdm root) throws Exception {

        String text = get_xml_text(object_factory.getClass().getPackage().getName(), root, Const.SDM_XSD);
        return text;
    }

    public static String get_dao_xml_text(
            com.sqldalmaker.jaxb.sdm.ObjectFactory object_factory,
            DaoClass root) throws Exception {

        String text = get_xml_text(object_factory.getClass().getPackage().getName(), root, Const.DAO_XSD);
        return text;
    }

    ////////////////////////////////////////////////////////////////////////////
    // String	formatXML(String unformatted)
    // http://www.java2s.com/example/java-utility-method/xml-format-index-0.html

    private static String formatXML(String unformattedXml) {
        String unformattedNoWhiteSpaces = unformattedXml.replaceAll(">\\s+<", "><");
        Source xmlInput = new StreamSource(new StringReader(unformattedNoWhiteSpaces));
        StreamResult xmlOutput = new StreamResult(new StringWriter());
        try {
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            transformerFactory.setAttribute("indent-number", 2);
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.transform(xmlInput, xmlOutput);
            return xmlOutput.getWriter().toString();
        } catch (Exception e) {
            // e.printStackTrace();
        }
        return unformattedXml;
    }

    public static int compareXml(String xmlStr1, String xmlStr2) {
        xmlStr1 = formatXML(xmlStr1);
        xmlStr2 = formatXML(xmlStr2);
        return xmlStr1.compareTo(xmlStr2);
    }
}
