
package com.sqldalmaker.jaxb.sdm;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.CollapsedStringAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;


/**
 * &lt;p&gt;Java class for anonymous complex type.
 * 
 * &lt;p&gt;The following schema fragment specifies the expected content contained within this class.
 * 
 * &lt;pre&gt;
 * &amp;lt;complexType&amp;gt;
 *   &amp;lt;complexContent&amp;gt;
 *     &amp;lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&amp;gt;
 *       &amp;lt;sequence&amp;gt;
 *         &amp;lt;element name="create" type="{}type-method" minOccurs="0"/&amp;gt;
 *         &amp;lt;element name="read-all" type="{}type-method" minOccurs="0"/&amp;gt;
 *         &amp;lt;element name="read" type="{}type-method" minOccurs="0"/&amp;gt;
 *         &amp;lt;element name="update" type="{}type-method" minOccurs="0"/&amp;gt;
 *         &amp;lt;element name="delete" type="{}type-method" minOccurs="0"/&amp;gt;
 *       &amp;lt;/sequence&amp;gt;
 *       &amp;lt;attribute ref="{}dto use="required""/&amp;gt;
 *       &amp;lt;attribute name="table" type="{http://www.w3.org/2001/XMLSchema}string" default="*" /&amp;gt;
 *       &amp;lt;attribute name="fetch-generated" type="{http://www.w3.org/2001/XMLSchema}boolean" default="true" /&amp;gt;
 *     &amp;lt;/restriction&amp;gt;
 *   &amp;lt;/complexContent&amp;gt;
 * &amp;lt;/complexType&amp;gt;
 * &lt;/pre&gt;
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "create",
    "readAll",
    "read",
    "update",
    "delete"
})
@XmlRootElement(name = "crud")
public class Crud {

    protected TypeMethod create;
    @XmlElement(name = "read-all")
    protected TypeMethod readAll;
    protected TypeMethod read;
    protected TypeMethod update;
    protected TypeMethod delete;
    @XmlAttribute(name = "dto", required = true)
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    @XmlSchemaType(name = "NCName")
    protected String dto;
    @XmlAttribute(name = "table")
    protected String table;
    @XmlAttribute(name = "fetch-generated")
    protected Boolean fetchGenerated;

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
        if (table == null) {
            return "*";
        } else {
            return table;
        }
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
     * Gets the value of the fetchGenerated property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public boolean isFetchGenerated() {
        if (fetchGenerated == null) {
            return true;
        } else {
            return fetchGenerated;
        }
    }

    /**
     * Sets the value of the fetchGenerated property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setFetchGenerated(Boolean value) {
        this.fetchGenerated = value;
    }

}