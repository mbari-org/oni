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
import org.mbari.oni.domain.ConceptNameTypes;
import org.mbari.oni.jpa.IPersistentObject;
import org.mbari.oni.jpa.KeyNullifier;
import org.mbari.oni.jpa.TransactionLogger;

/**
 *
 */
@Entity(name = "ConceptName")
@Table(name = "ConceptName",
        indexes = {@Index(name = "idx_ConceptName_name", columnList = "ConceptName"),
                   @Index(name = "idx_ConceptName_FK1", columnList = "ConceptID_FK"),
                   @Index(name = "idx_ConceptName_LUT", columnList = "LAST_UPDATED_TIME")})
@EntityListeners({ TransactionLogger.class, KeyNullifier.class })
@NamedNativeQueries({
    @NamedNativeQuery(name = "ConceptName.findAllNamesAsStrings",
            query = "SELECT ConceptName FROM ConceptName ORDER BY ConceptName"),
    @NamedNativeQuery(name = "ConceptName.countByName",
    query = "SELECT count(*) FROM ConceptName WHERE ConceptName = ?")
})
@NamedQueries( {
    @NamedQuery(name = "ConceptName.findById", query = "SELECT v FROM ConceptName v WHERE v.id = :id") ,
    @NamedQuery(name = "ConceptName.findByName", query = "SELECT c FROM ConceptName c WHERE c.name = :name") ,
    @NamedQuery(name = "ConceptName.findByAuthor", query = "SELECT c FROM ConceptName c WHERE c.author = :author") ,
    @NamedQuery(name = "ConceptName.findByNameType",
                query = "SELECT c FROM ConceptName c WHERE c.nameType = :nameType") ,
    @NamedQuery(name = "ConceptName.findAll", query = "SELECT c FROM ConceptName c") ,
    @NamedQuery(name = "ConceptName.findByNameLike", query = "SELECT c FROM ConceptName c WHERE lower(c.name) LIKE :name ORDER BY c.name")
})
@Cacheable
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class ConceptNameEntity implements Serializable, IPersistentObject {


    @Column(name = "Author", length = 255)
    String author;

    @ManyToOne(
        optional = false,
        targetEntity = ConceptEntity.class,
        fetch = FetchType.LAZY,
        cascade = {CascadeType.MERGE, CascadeType.REFRESH}
    )
    @JoinColumn(
            name = "ConceptID_FK",
            foreignKey = @ForeignKey(name = "fk_ConceptName__Concept_id")
    )
    ConceptEntity concept;

    @Id
    @Column(
        name = "id",
        nullable = false,
        updatable = false
    )
    @GeneratedValue(strategy = GenerationType.TABLE, generator = "ConceptName_Gen")
    @TableGenerator(
        name = "ConceptName_Gen",
        table = "UniqueID",
        pkColumnName = "TableName",
        valueColumnName = "NextID",
        pkColumnValue = "ConceptName",
        allocationSize = 1
    )
    Long id;

//    @SerializedName("name")
    @Column(
        name = "ConceptName",
        nullable = false,
        length = 128,
        unique = true
    )
    String name;

    @Column(
        name = "NameType",
        nullable = false,
        length = 10
    )
    String nameType;

    /** Optimistic lock to prevent concurrent overwrites */
    @Version
    @Column(name = "LAST_UPDATED_TIME")
    private Timestamp updatedTime;

    public ConceptNameEntity() {
    }

    public ConceptNameEntity(String name) {
        this(name, ConceptNameTypes.PRIMARY.getType());
    }

    public ConceptNameEntity(String name, String nameType) {
        this.name = name;
        this.nameType = nameType;
    }



    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }

        if (getClass() != obj.getClass()) {
            return false;
        }

        final ConceptNameEntity other = (ConceptNameEntity) obj;
        if ((this.name == null) ? (other.name != null) : !this.name.equals(other.name)) {
            return false;
        }

        return true;
    }

    public String getAuthor() {
        return author;
    }

    public ConceptEntity getConcept() {
        return concept;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getNameType() {
        return nameType;
    }
    
    public Object getPrimaryKey() {
    	return getId();
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 79 * hash + ((this.name != null) ? this.name.hashCode() : 0);

        return hash;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public void setConcept(ConceptEntity concept) {
        this.concept = concept;
    }
    
    public void setId(Long id) {
    	this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setNameType(ConceptNameTypes nameType) {
        this.nameType = nameType.getType();
    }

    public void setNameType(String nameType) {
        this.nameType = nameType;
    }

    public String stringValue() {
        return name;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(getClass().getSimpleName());
        sb.append(" ([id=").append(id).append("] name=").append(name).append(")");

        return sb.toString();
    }
}
