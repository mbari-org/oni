/*
 * Copyright 2024 Monterey Bay Aquarium Research Institute
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.mbari.oni.jpa.entities;

import java.io.Serializable;
import java.sql.Timestamp;
import java.time.Instant;

import jakarta.persistence.*;


import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.mbari.oni.jpa.*;

/**
 * Created by IntelliJ IDEA.
 * User: brian
 * Date: Jun 19, 2009
 * Time: 10:02:15 AM
 * To change this template use File | Settings | File Templates.
 */
@Entity(name = "Media")
@Table(name = "Media",
        indexes = {@Index(name = "idx_Media_FK1", columnList = "ConceptDelegateID_FK"),
                @Index(name = "idx_Media_LUT", columnList = "LAST_UPDATED_TIME")})
@EntityListeners({TransactionLogger.class, KeyNullifier.class})
@NamedQueries({
        @NamedQuery(name = "Media.findById", query = "SELECT v FROM Media v WHERE v.id = :id"),
        @NamedQuery(name = "Media.findByUrl", query = "SELECT m FROM Media m WHERE m.url = :url"),
        @NamedQuery(name = "Media.findByType", query = "SELECT m FROM Media m WHERE m.type = :type"),
        @NamedQuery(name = "Media.findByPrimaryMedia",
                query = "SELECT m FROM Media m WHERE m.primaryMedia = :primaryMedia"),
        @NamedQuery(name = "Media.findByCredit", query = "SELECT m FROM Media m WHERE m.credit = :credit"),
        @NamedQuery(name = "Media.findByCaption", query = "SELECT m FROM Media m WHERE m.caption = :caption"),
        @NamedQuery(name = "Media.findByConceptName",
                query = "SELECT m FROM Media m JOIN m.conceptMetadata cm JOIN cm.concept c JOIN c.conceptNames cn WHERE cn.name = :name"),
})
//@Cacheable
//@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class MediaEntity implements Serializable, IPersistentObject, IOptimisticLock {

    @Column(name = "Caption", length = 1000)
    String caption;

    @ManyToOne(optional = false, targetEntity = ConceptMetadataEntity.class, cascade = {CascadeType.MERGE, CascadeType.REFRESH, CascadeType.DETACH})
    @JoinColumn(
            name = "ConceptDelegateID_FK",
            foreignKey = @ForeignKey(name = "fk_Media__ConceptDelegate_id")
    )
    ConceptMetadataEntity conceptMetadata;

    @Column(name = "Credit", length = 255)
    String credit;

    @Id
    @Column(
            name = "id",
            nullable = false,
            updatable = false
    )
    @GeneratedValue(strategy = GenerationType.TABLE, generator = "Media_Gen")
    @TableGenerator(
            name = "Media_Gen",
            table = "UniqueID",
            pkColumnName = "TableName",
            valueColumnName = "NextID",
            pkColumnValue = "Media",
            allocationSize = 1
    )
    Long id;

    @Column(name = "PrimaryMedia")
    Short primaryMedia = 0;

    @Column(name = "MediaType", length = 5)
    String type;

    /**
     * Optimistic lock to prevent concurrent overwrites
     */
    @Version
    @Column(name = "LAST_UPDATED_TIME")
    private Instant updatedTime;

    @Column(name = "Url", length = 1024)
    String url;

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }

        if (getClass() != obj.getClass()) {
            return false;
        }

        final MediaEntity other = (MediaEntity) obj;
        if ((this.url == null) ? (other.url != null) : !this.url.equals(other.url)) {
            return false;
        }

        return true;
    }

    public String getCaption() {
        return caption;
    }

    public ConceptMetadataEntity getConceptMetadata() {
        return conceptMetadata;
    }

    public String getCredit() {
        return credit;
    }

    public Long getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public String getUrl() {
        return url;
    }

    public Object getPrimaryKey() {
        return getId();
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 59 * hash + ((this.url != null) ? this.url.hashCode() : 0);

        return hash;
    }

    public Boolean isPrimary() {
        return primaryMedia != null && primaryMedia == 1;
    }

    public void setCaption(String caption) {
        this.caption = caption;
    }

    void setConceptMetadata(ConceptMetadataEntity conceptMetadata) {
        this.conceptMetadata = conceptMetadata;
    }

    public void setCredit(String credit) {
        this.credit = credit;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setPrimary(Boolean primary) {
        this.primaryMedia = primary ? (short) 1 : 0;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Instant getLastUpdatedTimestamp() {
        return updatedTime;
    }

    public void setLastUpdatedTimestamp(Instant ts) {
        this.updatedTime = ts;
    }

    public String stringValue() {
        return url;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(getClass().getSimpleName());
        sb.append(" ([id=").append(getId()).append("] ");
        sb.append("url=").append(url).append(")");

        return sb.toString();
    }
}
