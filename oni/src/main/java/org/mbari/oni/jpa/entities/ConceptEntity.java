/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2024
 *
 * oni code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential. 
 */

package org.mbari.oni.jpa.entities;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.*;

import jakarta.persistence.*;

import org.mbari.oni.domain.ConceptNameTypes;
import org.mbari.oni.jpa.KeyNullifier;
import org.mbari.oni.jpa.TransactionLogger;
import org.mbari.oni.jpa.IPersistentObject;

/**
 *
 * @author brian
 */
@Entity(name = "Concept")
@Table(name = "Concept",
    indexes = {@Index(name = "idx_Concept_FK1", columnList = "ParentConceptID_FK"),
               @Index(name = "idx_Concept_LUT", columnList = "LAST_UPDATED_TIME")})
@EntityListeners({ TransactionLogger.class, KeyNullifier.class})
@NamedQueries( {
    @NamedQuery(name = "Concept.findById", query = "SELECT v FROM Concept v WHERE v.id = :id") ,
    @NamedQuery(name = "Concept.findByOriginator", query = "SELECT c FROM Concept c WHERE c.originator = :originator") ,
    @NamedQuery(name = "Concept.findByStructureType",
                query = "SELECT c FROM Concept c WHERE c.structureType = :structureType") ,
    @NamedQuery(name = "Concept.findByReference", query = "SELECT c FROM Concept c WHERE c.reference = :reference") ,
    @NamedQuery(name = "Concept.findByAphiaId", query = "SELECT c FROM Concept c WHERE c.aphiaId = :aphiaId") ,
    @NamedQuery(name = "Concept.findByRankName", query = "SELECT c FROM Concept c WHERE c.rankName = :rankName") ,
    @NamedQuery(name = "Concept.findByRankLevel", query = "SELECT c FROM Concept c WHERE c.rankLevel = :rankLevel") ,
    @NamedQuery(name = "Concept.findByTaxonomyType",
                query = "SELECT c FROM Concept c WHERE c.taxonomyType = :taxonomyType") ,
    @NamedQuery(name = "Concept.findRoot", query = "SELECT c FROM Concept c WHERE c.parentConcept IS NULL") ,
    @NamedQuery(name = "Concept.findAll", query = "SELECT c FROM Concept c"),
    @NamedQuery(name = "Concept.findByName", query = "SELECT c FROM Concept c, IN (c.conceptNames) AS n WHERE n.name = :name"),
    @NamedQuery(name = "Concept.findAllByNameGlob", query = "SELECT DISTINCT c FROM Concept c, IN (c.conceptNames) AS n WHERE lower(n.name) LIKE :name ORDER BY n.name"),
    @NamedQuery(name = "Concept.eagerFindById", query = "SELECT c FROM Concept c JOIN FETCH c.conceptMetadata m WHERE c.id = :id")

})
public class ConceptEntity implements Serializable, IPersistentObject {


//    @SerializedName("children")
    @OneToMany(
        targetEntity = ConceptEntity.class,
        mappedBy = "parentConcept",
        fetch = FetchType.LAZY,
        cascade = { CascadeType.ALL }
    )
    private List<ConceptEntity> childConcepts;

//    @SerializedName("metadata")
    @OneToOne(
        mappedBy = "concept",
        fetch = FetchType.LAZY,
        cascade = { CascadeType.ALL },
        targetEntity = ConceptMetadataEntity.class
    )
    private ConceptMetadataEntity conceptMetadata;

//    @SerializedName("names")
    @OneToMany(
        targetEntity = ConceptNameEntity.class,
        mappedBy = "concept",
        fetch = FetchType.LAZY,
        cascade = { CascadeType.ALL }
    )
    private Set<ConceptNameEntity> conceptNames;

    @Id
    @Column(
        name = "id",
        nullable = false,
        updatable = false
    )
    @GeneratedValue(strategy = GenerationType.TABLE, generator = "Concept_Gen")
    @TableGenerator(
        name = "Concept_Gen",
        table = "UniqueID",
        pkColumnName = "TableName",
        valueColumnName = "NextID",
        pkColumnValue = "Concept",
        allocationSize = 1
    )
    private Long id;

    @Column(name = "AphiaId")
    private Long aphiaId;

    @Column(name = "Originator", length = 255)
    private String originator;

    @ManyToOne(
        fetch = FetchType.LAZY,
        optional = true,
        targetEntity = ConceptEntity.class
    )
    @JoinColumn(
            name = "ParentConceptID_FK",
            foreignKey = @ForeignKey(name = "fk_Concept__Concept_id")
    )
    private ConceptEntity parentConcept;

    @Column(name = "RankLevel", length = 20)
    private String rankLevel;

    @Column(name = "RankName", length = 20)
    private String rankName;

    @Column(name = "Reference", length = 1024)
    private String reference;

    @Column(name = "StructureType", length = 10)
    private String structureType;

    @Column(name = "TaxonomyType", length = 20)
    private String taxonomyType;

    /** Optimistic lock to prevent concurrent overwrites */
    @Version
    @Column(name = "LAST_UPDATED_TIME")
    private Timestamp updatedTime;


    public ConceptEntity() {
    }

    public void addChildConcept(ConceptEntity child) {
        getChildConcepts().add(child);
        child.setParentConcept(this);
    }

    public void addConceptName(ConceptNameEntity conceptName) {

        // Check that there isn't already a primary name if this one is primary
        if (conceptName.getNameType().equalsIgnoreCase(ConceptNameTypes.PRIMARY.toString()) &&
                    getPrimaryConceptName() != null) {
            throw new IllegalArgumentException("Can't add a second primay conceptname to a concept");
        }

        // Check for matching name
        Collection<ConceptNameEntity> names = new ArrayList<>(getConceptNames());
        for (ConceptNameEntity cn : names) {
            if (cn.getName().equals(conceptName.getName())) {
                throw new IllegalArgumentException(
                    "A ConceptName with the name '${conceptName.name}' already exists in " + this);
            }
        }

        getConceptNames().add(conceptName);
        conceptName.setConcept(this);
    }

    public List<ConceptEntity> getChildConcepts() {
        if (childConcepts == null) {
            childConcepts = new ArrayList<>();
        }

        return childConcepts;
    }

    public ConceptMetadataEntity getConceptMetadata() {
        if (conceptMetadata == null) {
            conceptMetadata = new ConceptMetadataEntity();
            conceptMetadata.setConcept(this);
        }


        return conceptMetadata;
    }

    /**
     * This method shouldn't be called by developers. It's added to support the
     * cascadeDelete method in the ConceptDAO
     * 
     * @param conceptMetadata
     */
    public void setConceptMetadata(ConceptMetadataEntity conceptMetadata) {
        getConceptMetadata().setConcept(null);
        this.conceptMetadata = conceptMetadata;
        conceptMetadata.setConcept(this);
    }

    public ConceptNameEntity getConceptName(String name) {

        ConceptNameEntity conceptName = null;
        Collection<ConceptNameEntity> names = new ArrayList<>(getConceptNames());
        for (ConceptNameEntity cn : names) {
            if (cn.getName().equals(name)) {
                conceptName = cn;

                break;
            }
        }

        return conceptName;
    }

    public Set<ConceptNameEntity> getConceptNames() {
        if (conceptNames == null) {
            conceptNames = new HashSet<>();
        }

        return conceptNames;
    }

    public Long getId() {
        return id;
    }

    public Long getAphiaId() {
        return aphiaId;
    }

    public String getOriginator() {
        return originator;
    }

    public ConceptEntity getParentConcept() {
        return parentConcept;
    }

    public ConceptNameEntity getPrimaryConceptName() {

        return getConceptNames().stream()
                .filter(cn -> cn.getNameType().equalsIgnoreCase(ConceptNameTypes.PRIMARY.toString()))
                .findFirst()
                .orElse(null);

    }

    public String getRankLevel() {
        return rankLevel;
    }

    public String getRankName() {
        return rankName;
    }

    public String getReference() {
        return reference;
    }

    public ConceptEntity getRootConcept() {
        ConceptEntity concept = this;
        while (concept.getParentConcept() != null) {
            concept = concept.getParentConcept();
        }

        return concept;
    }

    public String getStructureType() {
        return structureType;
    }

    /**
     * WARNING! Due to lazy loading you will need to explicitly load the
     * childconcepts in a JPA transaction first.
     * @return
     */
    public boolean hasChildConcepts() {
        return getChildConcepts().size() > 0;
    }

    /**
     * WARNING! Due to lazy loading you will need to explicitly load the
     * childconcepts in a JPA transaction first.
     * @return
     */
    public boolean hasDescendent(String child) {
        return hasDescendent(child, this);
    }

    private boolean hasDescendent(String childName, ConceptEntity concept) {
        boolean match = false;

        // ---- Check the immediate children for a match
        Collection<ConceptEntity> children = new ArrayList<>(concept.getChildConcepts());
        for (ConceptEntity child : children) {
            match = child.getConceptName(childName) != null;
            if (match) {
                break;
            }
        }

        // ---- Iterate down to the grandchildren (and so on
        for (ConceptEntity child : children) {
            match = hasDescendent(childName, child);
            if (match) {
                break;
            }
        }

        return match;
    }

    public boolean hasDetails() {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public boolean hasParent() {
        return parentConcept != null;
    }



    public void removeChildConcept(ConceptEntity childConcept) {
        if (getChildConcepts().remove(childConcept)) {
            childConcept.setParentConcept(null);
        }
    }

    public void removeConceptName(ConceptNameEntity conceptName) {
        if (getConceptNames().remove(conceptName)) {
            conceptName.setConcept(null);
        }
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setAphiaId(Long aphiaId) {
        this.aphiaId = aphiaId;
    }

    public void setOriginator(String originator) {
        this.originator = originator;
    }

    private void setParentConcept(ConceptEntity parentConcept) {
        this.parentConcept = parentConcept;
    }

    public void setRankLevel(String rankLevel) {
        this.rankLevel = rankLevel;
    }

    public void setRankName(String rankName) {
        this.rankName = rankName;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public void setStructureType(String structureType) {
        this.structureType = structureType;
    }
    
    public Object getPrimaryKey() {
    	return getId();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " ([id=" + id + "])";
    }

    public void setTaxonomyType(String taxonomyType) {
        this.taxonomyType = taxonomyType;
    }

    public String getTaxonomyType() {
        return taxonomyType;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final ConceptEntity other = (ConceptEntity) obj;
        if (this.hashCode() != other.hashCode()) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }



}
