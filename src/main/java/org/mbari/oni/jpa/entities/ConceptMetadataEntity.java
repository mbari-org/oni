/*
 * @(#)ConceptMetadataEntity.java   2009.11.10 at 10:06:20 PST
 *
 * Copyright 2009 MBARI
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
import java.util.*;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.TableGenerator;
import jakarta.persistence.Version;

import org.mbari.kb.core.knowledgebase.*;
import org.mbari.oni.domain.MediaTypes;
import org.mbari.oni.jpa.KeyNullifier;
import org.mbari.oni.jpa.TransactionLogger;
import org.mbari.oni.jpa.IPersistentObject;

/**
 * <pre>
 * CREATE TABLE CONCEPTDELEGATE (
 *   ID                 BIGINT NOT NULL,
 *   CONCEPTID_FK       BIGINT,
 *   USAGEID_FK         BIGINT,
 *   CONSTRAINT PK_CONCEPTDELEGATE PRIMARY KEY(ID)
 * )
 * GO
 * CREATE INDEX IDX_USAGEID
 *   ON CONCEPTDELEGATE(USAGEID_FK)
 * GO
 * CREATE INDEX IDX_CONCEPTID
 *   ON CONCEPTDELEGATE(CONCEPTID_FK)
 * GO
 * </pre>
 */
@Entity(name = "ConceptMetadata")
@Table(name = "ConceptDelegate",
        indexes = {@Index(name = "idx_ConceptDelegate_FK1", columnList = "ConceptID_FK"),
                @Index(name = "idx_ConceptDelegate_LUT", columnList = "LAST_UPDATED_TIME")})
@EntityListeners({TransactionLogger.class, KeyNullifier.class})
@NamedQueries({@NamedQuery(name = "ConceptMetadata.findById",
        query = "SELECT v FROM ConceptMetadata v WHERE v.id = :id")})
public class ConceptMetadataEntity implements Serializable, IPersistentObject {

    @OneToOne(optional = false, targetEntity = ConceptEntity.class, cascade = {CascadeType.MERGE, CascadeType.REFRESH}, fetch = FetchType.LAZY)
    @JoinColumn(name = "ConceptID_FK", nullable = false)
    private ConceptEntity concept;

    @OneToMany(
            targetEntity = HistoryEntity.class,
            mappedBy = "conceptMetadata",
            fetch = FetchType.EAGER,
            cascade = {CascadeType.ALL}
    )
    @OrderBy(value = "creationDate")
    private Set<HistoryEntity> histories;

    @Id
    @Column(
            name = "id",
            nullable = false,
            updatable = false
    )
    @GeneratedValue(strategy = GenerationType.TABLE, generator = "ConceptDelegate_Gen")
    @TableGenerator(
            name = "ConceptDelegate_Gen",
            table = "UniqueID",
            pkColumnName = "TableName",
            valueColumnName = "NextID",
            pkColumnValue = "ConceptDelegate",
            allocationSize = 1
    )
    private Long id;

    @OneToMany(
            targetEntity = LinkRealizationEntity.class,
            mappedBy = "conceptMetadata",
            fetch = FetchType.EAGER,
            cascade = CascadeType.ALL
    )
    private Set<LinkRealizationEntity> linkRealizations;

    @OneToMany(
            targetEntity = LinkTemplateEntity.class,
            mappedBy = "conceptMetadata",
            fetch = FetchType.EAGER,
            cascade = CascadeType.ALL
    )
    private Set<LinkTemplateEntity> linkTemplates;


    @OneToMany(
            targetEntity = MediaEntity.class,
            mappedBy = "conceptMetadata",
            fetch = FetchType.EAGER,
            cascade = CascadeType.ALL
    )
    private Set<MediaEntity> medias;

    /**
     * Optimistic lock to prevent concurrent overwrites
     */
    @SuppressWarnings("unused")
    @Version
    @Column(name = "LAST_UPDATED_TIME")
    private Timestamp updatedTime;

    public void addHistory(HistoryEntity history) {
        if (getHistories().add(history)) {
            history.setConceptMetadata(this);
        }
    }

    public void addLinkRealization(LinkRealizationEntity linkRealization) {
        if (getLinkRealizations().add(linkRealization)) {
            ((LinkRealizationEntity) linkRealization).setConceptMetadata(this);
        }
    }

    public void addLinkTemplate(LinkTemplateEntity linkTemplate) {
        if (getLinkTemplates().add(linkTemplate)) {
            ((LinkTemplateEntity) linkTemplate).setConceptMetadata(this);
        }
    }

    public void addMedia(MediaEntity media) {
        if (getMedias().add(media)) {
            media.setConceptMetadata(this);
        }
    }


    public ConceptEntity getConcept() {
        return concept;
    }


    public Collection<HistoryEntity> getHistories() {
        if (histories == null) {
            histories = new HashSet<>();
        }


        return histories;
    }

    public Long getId() {
        return id;
    }

    public Collection<LinkRealizationEntity> getLinkRealizations() {
        if (linkRealizations == null) {
            linkRealizations = new HashSet<>();
        }

        return linkRealizations;
    }

    public Collection<LinkTemplateEntity> getLinkTemplates() {
        if (linkTemplates == null) {
            linkTemplates = new HashSet<>();
        }

        return linkTemplates;
    }

    public Collection<MediaEntity> getMedias() {
        if (medias == null) {
            medias = new HashSet<>();
        }


        return medias;
    }

    public MediaEntity getPrimaryImage() {
        MediaEntity media = null;
        Collection<MediaEntity> m = new ArrayList<>(getMedias());
        for (MediaEntity media1 : m) {
            if (media1.isPrimary() && media1.getType().equalsIgnoreCase(MediaTypes.IMAGE.toString())) {
                media = media1;

                break;
            }
        }

        return media;
    }

    public MediaEntity getPrimaryMedia(MediaTypes mediaType) {
        MediaEntity primaryMedia = null;
        Set<MediaEntity> ms = new HashSet<>(getMedias());
        for (MediaEntity media : ms) {
            if (media.isPrimary() && media.getType().equals(mediaType.toString())) {
                primaryMedia = media;
            }
        }

        return primaryMedia;
    }

    public Object getPrimaryKey() {
        return getId();
    }

    public boolean hasPrimaryImage() {
        return (getPrimaryImage() != null);
    }


    public boolean isPendingApproval() {
        boolean isPending = false;
        for (HistoryEntity history : getHistories()) {
            if (!history.isApproved() && !history.isRejected()) {
                isPending = true;

                break;
            }
        }

        return isPending;
    }


    public void removeHistory(HistoryEntity history) {
        if (getHistories().remove(history)) {
            ((HistoryEntity) history).setConceptMetadata(null);
        }
    }

    public void removeLinkRealization(LinkRealizationEntity linkRealization) {
        if (getLinkRealizations().remove(linkRealization)) {
            ((LinkRealizationEntity) linkRealization).setConceptMetadata(null);
        }
    }

    public void removeLinkTemplate(LinkTemplate linkTemplate) {
        if (getLinkTemplates().remove(linkTemplate)) {
            ((LinkTemplateEntity) linkTemplate).setConceptMetadata(null);
        }
    }

    public void removeMedia(MediaEntity media) {
        if (getMedias().remove(media)) {
            ((MediaEntity) media).setConceptMetadata(null);
        }
    }

    protected void setConcept(ConceptEntity concept) {
        this.concept = concept;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final ConceptMetadataEntity other = (ConceptMetadataEntity) obj;
        return Objects.equals(this.id, other.id);
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 41 * hash + (this.id != null ? this.id.hashCode() : 0);
        return hash;
    }

    @Override
    public String toString() {
        return "ConceptMetadataEntity ([id=" + id + "] updatedTime=" + updatedTime
                + ")";
    }


}
