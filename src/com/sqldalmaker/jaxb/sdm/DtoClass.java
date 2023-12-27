
package com.sqldalmaker.jaxb.sdm;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
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
 *         &amp;lt;element name="header" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&amp;gt;
 *         &amp;lt;sequence maxOccurs="unbounded" minOccurs="0"&amp;gt;
 *           &amp;lt;element name="field"&amp;gt;
 *             &amp;lt;complexType&amp;gt;
 *               &amp;lt;complexContent&amp;gt;
 *                 &amp;lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&amp;gt;
 *                   &amp;lt;attribute name="column" use="required" type="{http://www.w3.org/2001/XMLSchema}string" /&amp;gt;
 *                   &amp;lt;attribute name="type" use="required" type="{http://www.w3.org/2001/XMLSchema}string" /&amp;gt;
 *                 &amp;lt;/restriction&amp;gt;
 *               &amp;lt;/complexContent&amp;gt;
 *             &amp;lt;/complexType&amp;gt;
 *           &amp;lt;/element&amp;gt;
 *         &amp;lt;/sequence&amp;gt;
 *       &amp;lt;/sequence&amp;gt;
 *       &amp;lt;attribute name="name" use="required" type="{http://www.w3.org/2001/XMLSchema}NCName" /&amp;gt;
 *       &amp;lt;attribute name="ref" use="required" type="{http://www.w3.org/2001/XMLSchema}string" /&amp;gt;
 *       &amp;lt;attribute name="pk" type="{http://www.w3.org/2001/XMLSchema}string" default="*" /&amp;gt;
 *       &amp;lt;attribute name="auto" type="{http://www.w3.org/2001/XMLSchema}string" default="*" /&amp;gt;
 *     &amp;lt;/restriction&amp;gt;
 *   &amp;lt;/complexContent&amp;gt;
 * &amp;lt;/complexType&amp;gt;
 * &lt;/pre&gt;
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "header",
    "field"
})
@XmlRootElement(name = "dto-class")
public class DtoClass {

    protected String header;
    protected List<DtoClass.Field> field;
    @XmlAttribute(name = "name", required = true)
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    @XmlSchemaType(name = "NCName")
    protected String name;
    @XmlAttribute(name = "ref", required = true)
    protected String ref;
    @XmlAttribute(name = "pk")
    protected String pk;
    @XmlAttribute(name = "auto")
    protected String auto;

    /**
     * Gets the value of the header property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getHeader() {
        return header;
    }

    /**
     * Sets the value of the header property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setHeader(String value) {
        this.header = value;
    }

    /**
     * Gets the value of the field property.
     * 
     * &lt;p&gt;
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a &lt;CODE&gt;set&lt;/CODE&gt; method for the field property.
     * 
     * &lt;p&gt;
     * For example, to add a new item, do as follows:
     * &lt;pre&gt;
     *    getField().add(newItem);
     * &lt;/pre&gt;
     * 
     * 
     * &lt;p&gt;
     * Objects of the following type(s) are allowed in the list
     * {@link DtoClass.Field }
     * 
     * 
     */
    public List<DtoClass.Field> getField() {
        if (field == null) {
            field = new ArrayList<DtoClass.Field>();
        }
        return this.field;
    }

    /**
     * Gets the value of the name property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the value of the name property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setName(String value) {
        this.name = value;
    }

    /**
     * Gets the value of the ref property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getRef() {
        return ref;
    }

    /**
     * Sets the value of the ref property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setRef(String value) {
        this.ref = value;
    }

    /**
     * Gets the value of the pk property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getPk() {
        if (pk == null) {
            return "*";
        } else {
            return pk;
        }
    }

    /**
     * Sets the value of the pk property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setPk(String value) {
        this.pk = value;
    }

    /**
     * Gets the value of the auto property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getAuto() {
        if (auto == null) {
            return "*";
        } else {
            return auto;
        }
    }

    /**
     * Sets the value of the auto property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setAuto(String value) {
        this.auto = value;
    }


    /**
     * &lt;p&gt;Java class for anonymous complex type.
     * 
     * &lt;p&gt;The following schema fragment specifies the expected content contained within this class.
     * 
     * &lt;pre&gt;
     * &amp;lt;complexType&amp;gt;
     *   &amp;lt;complexContent&amp;gt;
     *     &amp;lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&amp;gt;
     *       &amp;lt;attribute name="column" use="required" type="{http://www.w3.org/2001/XMLSchema}string" /&amp;gt;
     *       &amp;lt;attribute name="type" use="required" type="{http://www.w3.org/2001/XMLSchema}string" /&amp;gt;
     *     &amp;lt;/restriction&amp;gt;
     *   &amp;lt;/complexContent&amp;gt;
     * &amp;lt;/complexType&amp;gt;
     * &lt;/pre&gt;
     * 
     * 
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "")
    public static class Field {

        @XmlAttribute(name = "column", required = true)
        protected String column;
        @XmlAttribute(name = "type", required = true)
        protected String type;

        /**
         * Gets the value of the column property.
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        public String getColumn() {
            return column;
        }

        /**
         * Sets the value of the column property.
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        public void setColumn(String value) {
            this.column = value;
        }

        /**
         * Gets the value of the type property.
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        public String getType() {
            return type;
        }

        /**
         * Sets the value of the type property.
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        public void setType(String value) {
            this.type = value;
        }

    }

}