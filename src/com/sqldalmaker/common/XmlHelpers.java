package com.sqldalmaker.common;

import com.sqldalmaker.jaxb.dao.DaoClass;
import com.sqldalmaker.jaxb.dto.DtoClasses;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import java.io.ByteArrayOutputStream;

public class XmlHelpers {

	public static Marshaller create_marshaller(String instance_name, String xsd) throws Exception {

		ClassLoader cl = XmlHelpers.class.getClassLoader();

		JAXBContext jc = JAXBContext.newInstance(instance_name, cl);

		// http://docs.oracle.com/javase/6/docs/api/javax/xml/bind/Marshaller.html
		Marshaller marshaller = jc.createMarshaller();
		marshaller.setProperty("jaxb.formatted.output", true);
		// m.setProperty("jaxb.schemaLocation",
		// XMLConstants.W3C_XML_SCHEMA_NS_URI);
		marshaller.setProperty("jaxb.noNamespaceSchemaLocation", xsd);

		return marshaller;
	}

	private static String get_xml_text(String instance_name, Object root, String xsd, boolean remove_java_lang)
			throws Exception {

		Marshaller marshaller = create_marshaller(instance_name, xsd);

		ByteArrayOutputStream out = new ByteArrayOutputStream();

		String text;

		try {

			marshaller.marshal(root, out);

			out.flush();

			text = new String(out.toByteArray());

			if (remove_java_lang) {

				text = text.replace("java.lang.", "");
			}

		} finally {

			out.close();
		}

		return text;
	}

	public static String get_dto_xml_text(com.sqldalmaker.jaxb.dto.ObjectFactory object_factory, DtoClasses root,
			boolean remove_java_lang) throws Exception {

		String text = get_xml_text(object_factory.getClass().getPackage().getName(), root, Const.DTO_XSD,
				remove_java_lang);

		return text;
	}

	public static String get_dao_xml_text(com.sqldalmaker.jaxb.dao.ObjectFactory object_factory, DaoClass root,
			boolean remove_java_lang) throws Exception {

		String text = get_xml_text(object_factory.getClass().getPackage().getName(), root, Const.DAO_XSD,
				remove_java_lang);

		return text;
	}
}
