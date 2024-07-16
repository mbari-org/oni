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

import org.hibernate.annotations.CacheConcurrencyStrategy;
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
    @NamedQuery(name = "Concept.eagerFindById", query = "SELECT c FROM Concept c JOIN FETCH c.conceptMetadata m WHERE c.id = :id"),
    @NamedQuery(name = "Concept.findAll", query = "SELECT c FROM Concept c"),
    @NamedQuery(name = "Concept.findAllByNameGlob", query = "SELECT DISTINCT c FROM Concept c LEFT JOIN c.conceptNames n WHERE LOWER(n.name) LIKE :name"),
    @NamedQuery(name = "Concept.findAllByNameGlobNew", query = "SELECT c FROM Concept c LEFT JOIN c.conceptNames n WHERE LOWER(n.name) LIKE :name"),
    @NamedQuery(name = "Concept.findByAphiaId", query = "SELECT c FROM Concept c WHERE c.aphiaId = :aphiaId") ,
    @NamedQuery(name = "Concept.findById", query = "SELECT v FROM Concept v WHERE v.id = :id") ,
    @NamedQuery(name = "Concept.findByName", query = "SELECT c FROM Concept c LEFT JOIN c.conceptNames n WHERE n.name = :name"),
//    @NamedQuery(name = "Concept.findByName", query = "SELECT c FROM Concept c, IN (c.conceptNames) AS n WHERE n.name = :name"),
    @NamedQuery(name = "Concept.findByRankLevel", query = "SELECT c FROM Concept c WHERE c.rankLevel = :rankLevel") ,
    @NamedQuery(name = "Concept.findByRankName", query = "SELECT c FROM Concept c WHERE c.rankName = :rankName"),
    @NamedQuery(name = "Concept.findRoot", query = "SELECT c FROM Concept c WHERE c.parentConcept IS NULL")
})
@Cacheable
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class ConceptEntity implements Serializable, IPersistentObject {


//    @SerializedName("children")
    @OneToMany(
        targetEntity = ConceptEntity.class,
        mappedBy = "parentConcept",
        fetch = FetchType.LAZY,
        cascade = { CascadeType.ALL }
    )
    private Set<ConceptEntity> childConcepts;

//    @SerializedName("metadata")
    @OneToOne(
        mappedBy = "concept",
        fetch = FetchType.LAZY,
        cascade = { CascadeType.ALL },
        targetEntity = ConceptMetadataEntity.class,
        orphanRemoval = true
    )
    private ConceptMetadataEntity conceptMetadata;

//    @SerializedName("names")
    @OneToMany(
        targetEntity = ConceptNameEntity.class,
        mappedBy = "concept",
        fetch = FetchType.LAZY,
        cascade = { CascadeType.ALL },
        orphanRemoval = true
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

    @ManyToOne(
        fetch = FetchType.LAZY,
        optional = true,
        targetEntity = ConceptEntity.class,
        cascade = { CascadeType.DETACH, CascadeType.MERGE, CascadeType.REFRESH}
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

    public Set<ConceptEntity> getChildConcepts() {
        if (childConcepts == null) {
            childConcepts = new HashSet<>();
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

    public ConceptEntity getParentConcept() {
        return parentConcept;
    }

    public ConceptNameEntity getPrimaryConceptName() {
//        ConceptNameEntity conceptNameEntity = null;
//        for (ConceptNameEntity cn : getConceptNames()) {
//            if (cn.getNameType().equalsIgnoreCase(ConceptNameTypes.PRIMARY.getType())) {
//                conceptNameEntity = cn;
//                break;
//            }
//        }
//        return conceptNameEntity;


        return getConceptNames().stream()
                .filter(cn -> cn.getNameType().equalsIgnoreCase(ConceptNameTypes.PRIMARY.getType()))
                .findFirst()
                .orElse(null);

    }

    public String getName() {
        return getPrimaryConceptName().getName();
    }

    public List<ConceptNameEntity> getAlternativeConceptNames() {

        return getConceptNames().stream()
                .filter(cn -> !cn.getNameType().equalsIgnoreCase(ConceptNameTypes.PRIMARY.getType()))
                .sorted(Comparator.comparing(ConceptNameEntity::getName))
                .toList();

    }

    public String getRankLevel() {
        return rankLevel;
    }

    public String getRankName() {
        return rankName;
    }

    public String getRank() {
        if (rankName == null && rankLevel == null) {
            return null;
        }
        var a = rankLevel == null ? "" : rankLevel;
        var b = rankName == null ? "" : rankName;
        return a + b;
    }

    public ConceptEntity getRootConcept() {
        ConceptEntity concept = this;
        while (concept.getParentConcept() != null) {
            concept = concept.getParentConcept();
        }

        return concept;
    }

    /**
     * WARNING! Due to lazy loading you will need to explicitly load the
     * childconcepts in a JPA transaction first.
     * @return
     */
    public boolean hasChildConcepts() {
        return !getChildConcepts().isEmpty();
    }

    public boolean hasConceptName(String name) {
        return getConceptName(name) != null;
    }

    /**
     * WARNING! Due to lazy loading you will need to explicitly load the
     * childconcepts in a JPA transaction first.
     * @return
     */
    public Set<ConceptEntity> getDescendants() {
        var accum = new HashSet<ConceptEntity>();
        getDescendants(this, accum);
        return accum;
    }

    private void getDescendants(ConceptEntity concept, Set<ConceptEntity> accum) {
        accum.add(concept);
        for (ConceptEntity child : concept.getChildConcepts()) {
            getDescendants(child, accum);
        }
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

    private void setParentConcept(ConceptEntity parentConcept) {
        this.parentConcept = parentConcept;
    }

    public void setRankLevel(String rankLevel) {
        this.rankLevel = rankLevel;
    }

    public void setRankName(String rankName) {
        this.rankName = rankName;
    }

    public Object getPrimaryKey() {
    	return getId();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " ([id=" + id + "])";
    }


    // WARNING: Using id for equals and hashcode breaks adding child concepts. They are in a set and the id is not set
    // until the entity is persisted. We'll just use object identity for now

//    @Override
//    public boolean equals(Object obj) {
//        if (obj == null) {
//            return false;
//        }
//        if (getClass() != obj.getClass()) {
//            return false;
//        }
//        final ConceptEntity other = (ConceptEntity) obj;
//        if (this.hashCode() != other.hashCode()) {
//            return false;
//        }
//        return true;
//    }
//
//    @Override
//    public int hashCode() {
//        return toString().hashCode();
//    }



}
