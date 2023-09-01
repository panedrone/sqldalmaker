//
// This file was generated by the Eclipse Implementation of JAXB, v2.3.3 
// See https://eclipse-ee4j.github.io/jaxb-ri 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2023.08.31 at 10:14:10 PM EEST 
//


package com.sqldalmaker.jaxb.settings;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;


/**
 * &lt;p&gt;Java class for anonymous complex type.
 * 
 * &lt;p&gt;The following schema fragment specifies the expected content contained within this class.
 * 
 * &lt;pre&gt;
 * &amp;lt;complexType&amp;gt;
 *   &amp;lt;complexContent&amp;gt;
 *     &amp;lt;extension base="{}root-statements"&amp;gt;
 *       &amp;lt;sequence maxOccurs="unbounded" minOccurs="0"&amp;gt;
 *         &amp;lt;element ref="{}elseif" maxOccurs="unbounded" minOccurs="0"/&amp;gt;
 *         &amp;lt;element ref="{}else" minOccurs="0"/&amp;gt;
 *       &amp;lt;/sequence&amp;gt;
 *       &amp;lt;attribute ref="{}var use="required""/&amp;gt;
 *     &amp;lt;/extension&amp;gt;
 *   &amp;lt;/complexContent&amp;gt;
 * &amp;lt;/complexType&amp;gt;
 * &lt;/pre&gt;
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "elseifAndElse"
})
@XmlRootElement(name = "if")
public class If
    extends RootStatements
{

    @XmlElements({
        @XmlElement(name = "elseif", type = Elseif.class),
        @XmlElement(name = "else", type = Else.class)
    })
    protected List<RootStatements> elseifAndElse;
    @XmlAttribute(name = "var", required = true)
    protected String var;

    /**
     * Gets the value of the elseifAndElse property.
     * 
     * &lt;p&gt;
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a &lt;CODE&gt;set&lt;/CODE&gt; method for the elseifAndElse property.
     * 
     * &lt;p&gt;
     * For example, to add a new item, do as follows:
     * &lt;pre&gt;
     *    getElseifAndElse().add(newItem);
     * &lt;/pre&gt;
     * 
     * 
     * &lt;p&gt;
     * Objects of the following type(s) are allowed in the list
     * {@link Elseif }
     * {@link Else }
     * 
     * 
     */
    public List<RootStatements> getElseifAndElse() {
        if (elseifAndElse == null) {
            elseifAndElse = new ArrayList<RootStatements>();
        }
        return this.elseifAndElse;
    }

    /**
     * Gets the value of the var property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getVar() {
        return var;
    }

    /**
     * Sets the value of the var property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setVar(String value) {
        this.var = value;
    }

}
