
package com.sqldalmaker.jaxb.dao;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for anonymous complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType>
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;choice maxOccurs="unbounded" minOccurs="0">
 *           &lt;element ref="{}crud"/>
 *           &lt;element ref="{}crud-auto"/>
 *           &lt;element ref="{}query"/>
 *           &lt;element ref="{}query-list"/>
 *           &lt;element ref="{}query-dto"/>
 *           &lt;element ref="{}query-dto-list"/>
 *           &lt;element ref="{}exec-dml"/>
 *           &lt;element ref="{}sa-exec-dml"/>
 *           &lt;element ref="{}sa-crud"/>
 *         &lt;/choice>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "crudOrCrudAutoOrQuery"
})
@XmlRootElement(name = "dao-class")
public class DaoClass {

    @XmlElements({
        @XmlElement(name = "exec-dml", type = ExecDml.class),
        @XmlElement(name = "query-dto-list", type = QueryDtoList.class),
        @XmlElement(name = "query-dto", type = QueryDto.class),
        @XmlElement(name = "query", type = Query.class),
        @XmlElement(name = "crud-auto", type = CrudAuto.class),
        @XmlElement(name = "query-list", type = QueryList.class),
        @XmlElement(name = "crud", type = Crud.class)
    })
    protected List<Object> crudOrCrudAutoOrQuery;

    /**
     * Gets the value of the crudOrCrudAutoOrQuery property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the crudOrCrudAutoOrQuery property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getCrudOrCrudAutoOrQuery().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link com.sqldalmaker.jaxb.dao.ExecDml }
     * {@link com.sqldalmaker.jaxb.dao.SaExecDml }
     * {@link com.sqldalmaker.jaxb.dao.QueryDtoList }
     * {@link com.sqldalmaker.jaxb.dao.QueryDto }
     * {@link com.sqldalmaker.jaxb.dao.Query }
     * {@link com.sqldalmaker.jaxb.dao.CrudAuto }
     * {@link SaCrud }
     * {@link com.sqldalmaker.jaxb.dao.QueryList }
     * {@link com.sqldalmaker.jaxb.dao.Crud }
     * 
     * 
     */
    public List<Object> getCrudOrCrudAutoOrQuery() {
        if (crudOrCrudAutoOrQuery == null) {
            crudOrCrudAutoOrQuery = new ArrayList<Object>();
        }
        return this.crudOrCrudAutoOrQuery;
    }

}
