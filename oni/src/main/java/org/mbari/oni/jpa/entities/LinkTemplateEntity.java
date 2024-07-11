/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2024
 *
 * oni code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential.
 */

package org.mbari.oni.jpa.entities;

import java.io.Serializable;
import java.sql.Timestamp;

import jakarta.persistence.*;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.mbari.oni.domain.ILink;
import org.mbari.oni.domain.LinkUtilities;
import org.mbari.oni.jpa.KeyNullifier;
import org.mbari.oni.jpa.TransactionLogger;
import org.mbari.oni.jpa.IPersistentObject;

/**
 * Class description
 *
 *
 * @version        $date$, 2009.11.09 at 04:57:26 PST
 * @author         Brian Schlining [brian@mbari.org]
 */
@Entity(name = "LinkTemplate")
@Table(name = "LinkTemplate",
        indexes = {@Index(name = "idx_LinkTemplate_FK1", columnList = "ConceptDelegateID_FK"),
                @Index(name = "idx_LinkTemplate_LUT", columnList = "LAST_UPDATED_TIME")})
@EntityListeners({ TransactionLogger.class, KeyNullifier.class })
@NamedQueries( {

    @NamedQuery(name = "LinkTemplate.findById", query = "SELECT v FROM LinkTemplate v WHERE v.id = :id") ,
    @NamedQuery(name = "LinkTemplate.findAll",
                query = "SELECT l FROM LinkTemplate l") ,
    @NamedQuery(name = "LinkTemplate.findByLinkName",
                query = "SELECT l FROM LinkTemplate l WHERE l.linkName = :linkName") ,
    @NamedQuery(name = "LinkTemplate.findByToConcept",
                query = "SELECT l FROM LinkTemplate l WHERE l.toConcept = :toConcept") ,
    @NamedQuery(name = "LinkTemplate.findByLinkValue",
                query = "SELECT l FROM LinkTemplate l WHERE l.linkValue = :linkValue") ,
    @NamedQuery(name = "LinkTemplate.findByFields",
                query = "SELECT l FROM LinkTemplate l WHERE l.linkName = :linkName AND l.toConcept = :toConcept AND l.linkValue = :linkValue")

})
@Cacheable
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class LinkTemplateEntity implements Serializable, ILink, IPersistentObject {


    @ManyToOne(optional = false, targetEntity = ConceptMetadataEntity.class, fetch = FetchType.EAGER, cascade = {CascadeType.MERGE, CascadeType.REFRESH})
    @JoinColumn(
            name = "ConceptDelegateID_FK",
            foreignKey = @ForeignKey(name = "fk_LinkTempate__ConceptDelegate_id")
    )
    ConceptMetadataEntity conceptMetadata;

    @Id
    @Column(
        name = "id",
        nullable = false,
        updatable = false
    )
    @GeneratedValue(strategy = GenerationType.TABLE, generator = "LinkTemplate_Gen")
    @TableGenerator(
        name = "LinkTemplate_Gen",
        table = "UniqueID",
        pkColumnName = "TableName",
        valueColumnName = "NextID",
        pkColumnValue = "LinkTemplate",
        allocationSize = 1
    )
    Long id;

    @Column(name = "LinkName", length = 50)
    String linkName;

    @Column(name = "LinkValue", length = 2048)
    String linkValue;
    
    @Column(name = "ToConcept", length = 128)
    String toConcept;

    /** Optimistic lock to prevent concurrent overwrites */
    @SuppressWarnings("unused")
    @Version
    @Column(name = "LAST_UPDATED_TIME")
    private Timestamp updatedTime;

    public LinkTemplateEntity() {
    }

    public LinkTemplateEntity(String linkName, String toConcept, String linkValue) {
        this.linkName = linkName;
        this.linkValue = linkValue;
        this.toConcept = toConcept;
    }

    public ConceptMetadataEntity getConceptMetadata() {
        return conceptMetadata;
    }

    void setConceptMetadata(ConceptMetadataEntity conceptMetadata) {
        this.conceptMetadata = conceptMetadata;
    }
    
    public String getFromConcept() {
        return (conceptMetadata == null) ? null : conceptMetadata.getConcept().getPrimaryConceptName().getName();
    }

    public Long getId() {
        return id;
    }

    public String getLinkName() {
        return linkName;
    }

    public String getLinkValue() {
        return linkValue;
    }

    public String getToConcept() {
        return toConcept;
    }
    
    public Object getPrimaryKey() {
    	return getId();
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setLinkName(String linkName) {
        this.linkName = linkName;
    }

    public void setLinkValue(String linkValue) {
        this.linkValue = linkValue;
    }

    public void setToConcept(String toConcept) {
        this.toConcept = toConcept;
    }

    public String stringValue() {
        return LinkUtilities.formatAsString(this);
    }

    

	@Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((linkName == null) ? 0 : linkName.hashCode());
        result = prime * result
                + ((linkValue == null) ? 0 : linkValue.hashCode());
        result = prime * result
                + ((toConcept == null) ? 0 : toConcept.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        LinkTemplateEntity other = (LinkTemplateEntity) obj;
        if (linkName == null) {
            if (other.linkName != null)
                return false;
        }
        else if (!linkName.equals(other.linkName))
            return false;
        if (linkValue == null) {
            if (other.linkValue != null)
                return false;
        }
        else if (!linkValue.equals(other.linkValue))
            return false;
        if (toConcept == null) {
            if (other.toConcept != null)
                return false;
        }
        else if (!toConcept.equals(other.toConcept))
            return false;
        return true;
    }

    @Override
	public String toString() {
		return "LinkTemplateEntity ([id=" + id + "] linkName=" + linkName
				+ ", toConcept=" + toConcept + ", linkValue=" + linkValue + ")";
	}
    
    
    

}
