
package com.sqldalmaker.jaxb.dao;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.CollapsedStringAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;


/**
 * <p>Java class for type-crud complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="type-crud">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="create" type="{}type-method" minOccurs="0"/>
 *         &lt;element name="read-all" type="{}type-method" minOccurs="0"/>
 *         &lt;element name="read" type="{}type-method" minOccurs="0"/>
 *         &lt;element name="update" type="{}type-method" minOccurs="0"/>
 *         &lt;element name="delete" type="{}type-method" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute ref="{}dto use="required""/>
 *       &lt;attribute name="table" use="required" type="{http://www.w3.org/2001/XMLSchema}NCName" />
 *       &lt;attribute name="fetch-generated" type="{http://www.w3.org/2001/XMLSchema}boolean" default="true" />
 *       &lt;attribute name="generated" type="{http://www.w3.org/2001/XMLSchema}string" default="*" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "type-crud", propOrder = {
    "create",
    "readAll",
    "read",
    "update",
    "delete"
})
public class TypeCrud {

    protected TypeMethod create;
    @XmlElement(name = "read-all")
    protected TypeMethod readAll;
    protected TypeMethod read;
    protected TypeMethod update;
    protected TypeMethod delete;
    @XmlAttribute(required = true)
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    @XmlSchemaType(name = "NCName")
    protected String dto;
    @XmlAttribute(required = true)
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    @XmlSchemaType(name = "NCName")
    protected String table;
    @XmlAttribute(name = "fetch-generated")
    protected Boolean fetch_generated;
    @XmlAttribute
    protected String generated;

    /**
     * Gets the value of the create property.
     * 
     * @return
     *     possible object is
     *     {@link TypeMethod }
     *     
     */
    public TypeMethod getCreate() {
        return create;
    }

    /**
     * Sets the value of the create property.
     * 
     * @param value
     *     allowed object is
     *     {@link TypeMethod }
     *     
     */
    public void setCreate(TypeMethod value) {
        this.create = value;
    }

    /**
     * Gets the value of the readAll property.
     * 
     * @return
     *     possible object is
     *     {@link TypeMethod }
     *     
     */
    public TypeMethod getReadAll() {
        return readAll;
    }

    /**
     * Sets the value of the readAll property.
     * 
     * @param value
     *     allowed object is
     *     {@link TypeMethod }
     *     
     */
    public void setReadAll(TypeMethod value) {
        this.readAll = value;
    }

    /**
     * Gets the value of the read property.
     * 
     * @return
     *     possible object is
     *     {@link TypeMethod }
     *     
     */
    public TypeMethod getRead() {
        return read;
    }

    /**
     * Sets the value of the read property.
     * 
     * @param value
     *     allowed object is
     *     {@link TypeMethod }
     *     
     */
    public void setRead(TypeMethod value) {
        this.read = value;
    }

    /**
     * Gets the value of the update property.
     * 
     * @return
     *     possible object is
     *     {@link TypeMethod }
     *     
     */
    public TypeMethod getUpdate() {
        return update;
    }

    /**
     * Sets the value of the update property.
     * 
     * @param value
     *     allowed object is
     *     {@link TypeMethod }
     *     
     */
    public void setUpdate(TypeMethod value) {
        this.update = value;
    }

    /**
     * Gets the value of the delete property.
     * 
     * @return
     *     possible object is
     *     {@link TypeMethod }
     *     
     */
    public TypeMethod getDelete() {
        return delete;
    }

    /**
     * Sets the value of the delete property.
     * 
     * @param value
     *     allowed object is
     *     {@link TypeMethod }
     *     
     */
    public void setDelete(TypeMethod value) {
        this.delete = value;
    }

    /**
     * Gets the value of the dto property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDto() {
        return dto;
    }

    /**
     * Sets the value of the dto property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDto(String value) {
        this.dto = value;
    }

    /**
     * Gets the value of the table property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getTable() {
        return table;
    }

    /**
     * Sets the value of the table property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setTable(String value) {
        this.table = value;
    }

    /**
     * Gets the value of the fetch_generated property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public boolean isFetchGenerated() {
        if (fetch_generated == null) {
            return true;
        } else {
            return fetch_generated;
        }
    }

    /**
     * Sets the value of the fetch_generated property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setFetchGenerated(Boolean value) {
        this.fetch_generated = value;
    }

    /**
     * Gets the value of the generated property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getGenerated() {
        if (generated == null) {
            return "*";
        } else {
            return generated;
        }
    }

    /**
     * Sets the value of the generated property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setGenerated(String value) {
        this.generated = value;
    }

}
