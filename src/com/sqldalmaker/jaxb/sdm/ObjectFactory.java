//
// This file was generated by the Eclipse Implementation of JAXB, v2.3.3 
// See https://eclipse-ee4j.github.io/jaxb-ri 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2024.06.16 at 12:40:09 AM EEST 
//


package com.sqldalmaker.jaxb.sdm;

import javax.xml.bind.annotation.XmlRegistry;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the com.sqldalmaker.jaxb.sdm package. 
 * &lt;p&gt;An ObjectFactory allows you to programatically 
 * construct new instances of the Java representation 
 * for XML content. The Java representation of XML 
 * content can consist of schema derived interfaces 
 * and classes representing the binding of schema 
 * type definitions, element declarations and model 
 * groups.  Factory methods for each of these are 
 * provided in this class.
 * 
 */
@XmlRegistry
public class ObjectFactory {


    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: com.sqldalmaker.jaxb.sdm
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link DtoClass }
     * 
     */
    public DtoClass createDtoClass() {
        return new DtoClass();
    }

    /**
     * Create an instance of {@link Sdm }
     * 
     */
    public Sdm createSdm() {
        return new Sdm();
    }

    /**
     * Create an instance of {@link DtoClass.Field }
     * 
     */
    public DtoClass.Field createDtoClassField() {
        return new DtoClass.Field();
    }

    /**
     * Create an instance of {@link DaoClass }
     * 
     */
    public DaoClass createDaoClass() {
        return new DaoClass();
    }

    /**
     * Create an instance of {@link DaoNode }
     * 
     */
    public DaoNode createDaoNode() {
        return new DaoNode();
    }

    /**
     * Create an instance of {@link Crud }
     * 
     */
    public Crud createCrud() {
        return new Crud();
    }

    /**
     * Create an instance of {@link TypeMethod }
     * 
     */
    public TypeMethod createTypeMethod() {
        return new TypeMethod();
    }

    /**
     * Create an instance of {@link Query }
     * 
     */
    public Query createQuery() {
        return new Query();
    }

    /**
     * Create an instance of {@link QueryList }
     * 
     */
    public QueryList createQueryList() {
        return new QueryList();
    }

    /**
     * Create an instance of {@link QueryDto }
     * 
     */
    public QueryDto createQueryDto() {
        return new QueryDto();
    }

    /**
     * Create an instance of {@link QueryDtoList }
     * 
     */
    public QueryDtoList createQueryDtoList() {
        return new QueryDtoList();
    }

    /**
     * Create an instance of {@link ExecDml }
     * 
     */
    public ExecDml createExecDml() {
        return new ExecDml();
    }

}
