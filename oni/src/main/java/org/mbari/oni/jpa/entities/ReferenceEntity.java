/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2024
 *
 * oni code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential. 
 */

package org.mbari.oni.jpa.entities;

import jakarta.persistence.*;
import org.hibernate.annotations.NaturalId;
import org.mbari.oni.jpa.IPersistentObject;
import org.mbari.oni.jpa.KeyNullifier;
import org.mbari.oni.jpa.TransactionLogger;
import org.mbari.oni.jpa.URIConverter;

import java.io.Serializable;
import java.net.URI;
import java.sql.Timestamp;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Entity(name = "Reference")
@Table(name = "Reference",
        indexes = {@Index(name = "idx_Reference_name", columnList = "ReferenceName"),
                @Index(name = "idx_Reference_FK1", columnList = "ConceptDelegateID_FK"),
                @Index(name = "idx_Reference_LUT", columnList = "LAST_UPDATED_TIME")})
@EntityListeners({ TransactionLogger.class, KeyNullifier.class })
@NamedQueries( {
    @NamedQuery(name = "Reference.findAll", query = "SELECT r FROM Reference r ORDER BY r.citation ASC"),
    @NamedQuery(name = "Reference.findById", query = "SELECT r FROM Reference r WHERE r.id = :id") ,
    @NamedQuery(name = "Reference.findByGlob", query = "SELECT r FROM Reference r WHERE r.citation LIKE :glob ORDER BY r.citation ASC"),
    @NamedQuery(name = "Reference.findByDoi", query = "SELECT r FROM Reference r WHERE r.doi = :doi") ,
    @NamedQuery(name = "Reference.findByConceptName",
                query = "SELECT r FROM Reference r JOIN r.conceptMetadatas cm JOIN cm.concept c JOIN c.conceptNames cn WHERE cn.name = :name") ,
})
public class ReferenceEntity implements Serializable, IPersistentObject {

    @Id
    @Column(
            name = "id",
            nullable = false,
            updatable = false
    )
    @GeneratedValue(strategy = GenerationType.TABLE, generator = "Reference_Gen")
    @TableGenerator(
            name = "Reference_Gen",
            table = "UniqueID",
            pkColumnName = "TableName",
            valueColumnName = "NextID",
            pkColumnValue = "Reference",
            allocationSize = 1
    )
    Long id;

    @Column(name = "citation", length = 2048, nullable = false)
    String citation;

    @Column(name = "doi", length = 2048, nullable = true, unique = true)
    @Convert(converter = URIConverter.class)
    URI doi;

    @ManyToMany(
            cascade = {CascadeType.PERSIST, CascadeType.MERGE},
            fetch = FetchType.LAZY
    )
    @JoinTable(
            name = "Reference_ConceptDelegate",
            joinColumns = @JoinColumn(name = "ReferenceID_FK"),
            inverseJoinColumns = @JoinColumn(name = "ConceptDelegateID_FK")
    )
    Set<ConceptMetadataEntity> conceptMetadatas;

    /**
     * Optimistic lock to prevent concurrent overwrites
     */
    @SuppressWarnings("unused")
    @Version
    @Column(name = "LAST_UPDATED_TIME")
    private Timestamp updatedTime;

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
    }

    public String getCitation() {
        return citation;
    }

    public void setCitation(String citation) {
        this.citation = citation;
    }

    public URI getDoi() {
        return doi;
    }

    public void setDoi(URI doi) {
        this.doi = doi;
    }

    public Set<ConceptMetadataEntity> getConceptMetadatas() {
        if (conceptMetadatas == null) {
            conceptMetadatas = new HashSet<>();
        }
        return conceptMetadatas;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ReferenceEntity that = (ReferenceEntity) o;
        return Objects.equals(citation, that.citation) && Objects.equals(doi, that.doi);
    }

    @Override
    public int hashCode() {
        return Objects.hash(citation, doi);
    }
}
