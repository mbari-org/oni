/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2024
 *
 * oni code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential. 
 */

package org.mbari.oni.jpa.entities;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.List;

import jakarta.persistence.*;
import org.mbari.oni.domain.ILink;
import org.mbari.oni.domain.LinkUtilities;
import org.mbari.oni.jpa.KeyNullifier;
import org.mbari.oni.jpa.IPersistentObject;
import org.mbari.oni.jpa.TransactionLogger;



/**
 * <pre>
 * CREATE TABLE LINKREALIZATION (
 *   ID                        	BIGINT NOT NULL,
 *   CONCEPTDELEGATEID_FK      	BIGINT,
 *   PARENTLINKREALIZATIONID_FK	BIGINT,
 *   LINKNAME                  	VARCHAR(50),
 *   TOCONCEPT                 	VARCHAR(50),
 *   LINKVALUE                 	VARCHAR(255),
 *   CONSTRAINT PK_LINKREALIZATION PRIMARY KEY(ID)
 * )
 * GO
 * CREATE INDEX IDX_CONCEPTDELEGATE3
 *   ON LINKREALIZATION(CONCEPTDELEGATEID_FK)
 * GO
 * </pre>
 */
@Entity(name = "LinkRealization")
@Table(name = "LinkRealization",
        indexes = {@Index(name = "idx_LinkRealization_FK1", columnList = "ConceptDelegateID_FK"),
                @Index(name = "idx_LinkRealization_LUT", columnList = "LAST_UPDATED_TIME")})
@EntityListeners({TransactionLogger.class, KeyNullifier.class})
@NamedQueries({
    @NamedQuery(name = "LinkRealization.findById",
                query = "SELECT v FROM LinkRealization v WHERE v.id = :id"),
    @NamedQuery(name = "LinkRealization.findByLinkName",
                query = "SELECT l FROM LinkRealization l WHERE l.linkName = :linkName") ,
    @NamedQuery(name = "LinkRealization.findByToConcept",
                query = "SELECT l FROM LinkRealization l WHERE l.toConcept = :toConcept") ,
    @NamedQuery(name = "LinkRealization.findByLinkValue",
                query = "SELECT l FROM LinkRealization l WHERE l.linkValue = :linkValue")
})
public class LinkRealizationEntity implements Serializable, ILink, IPersistentObject {

    @Transient
    private static final List<String> PROPS = List.of(ILink.PROP_LINKNAME,
            ILink.PROP_TOCONCEPT, ILink.PROP_LINKVALUE) ;

    @Id
    @Column(name = "id", nullable = false, updatable=false)
    @GeneratedValue(strategy = GenerationType.TABLE, generator = "LinkRealization_Gen")
    @TableGenerator(name = "LinkRealization_Gen", table = "UniqueID",
            pkColumnName = "TableName", valueColumnName = "NextID",
            pkColumnValue = "LinkRealization", allocationSize = 1)
    Long id;

    /** Optimistic lock to prevent concurrent overwrites */
    @Version
    @Column(name = "LAST_UPDATED_TIME")
    private Timestamp updatedTime;

    @Column(name = "LinkName", length = 50)
    String linkName;

    @Column(name = "ToConcept", length = 128)
    String toConcept;

    @Column(name = "LinkValue", length = 2048)
    String linkValue;

    @ManyToOne(optional = false, targetEntity = ConceptMetadataEntity.class, cascade = {CascadeType.MERGE, CascadeType.REFRESH})
    @JoinColumn(
            name = "ConceptDelegateID_FK",
            foreignKey = @ForeignKey(name = "fk_LinkRealization__ConceptDelegate_id")
    )
    ConceptMetadataEntity conceptMetadata;

    public LinkRealizationEntity() {}

    public LinkRealizationEntity(String linkName, String toConcept, String linkValue) {
        this.linkName = linkName;
        this.toConcept = toConcept;
        this.linkValue = linkValue;
    }

    public String getFromConcept() {
        return (conceptMetadata == null) ? null : conceptMetadata.getConcept().getPrimaryConceptName().getName();
    }

    public String stringValue() {
        return LinkUtilities.formatAsString(this);
    }

    

    @Override
    public String toString() {
        return "LinkRealizationEntity ([id=" + id + "] linkName=" + linkName
                + ", toConcept=" + toConcept + ", linkValue=" + linkValue + ")";
    }

    @Override
    public boolean equals(Object that) {

        if (that == null) {
            return false;
        }
        if (getClass() != that.getClass()) {
            return false;
        }
        return stringValue().equals(((LinkRealizationEntity) that).stringValue());
    }

    @Override
    public int hashCode() {
        return stringValue().hashCode() * 11;
    }

    public ConceptMetadataEntity getConceptMetadata() {
        return conceptMetadata;
    }

    public String getLinkName() {
        return linkName;
    }

    public void setLinkName(String linkName) {
        this.linkName = linkName;
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
    
    public void setToConcept(String toConcept) {
        this.toConcept = toConcept;
    }

    public String getLinkValue() {
        return linkValue;
    }

    public void setLinkValue(String linkValue) {
        this.linkValue = linkValue;
    }

    public Long getId() {
        return id;
    }

    public void setConceptMetadata(ConceptMetadataEntity conceptMetadata) {
        this.conceptMetadata = conceptMetadata;
    }


}
