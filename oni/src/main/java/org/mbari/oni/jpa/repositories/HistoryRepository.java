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

package org.mbari.oni.jpa.repositories;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Predicate;
import org.mbari.oni.domain.ConceptNameTypes;
import org.mbari.oni.jpa.entities.ConceptEntity;
import org.mbari.oni.jpa.entities.ConceptMetadataEntity;
import org.mbari.oni.jpa.entities.ConceptNameEntity;
import org.mbari.oni.jpa.entities.HistoryEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;


/**
 * Created by IntelliJ IDEA.
 * User: brian
 * Date: Aug 7, 2009
 * Time: 4:45:43 PM
 * To change this template use File | Settings | File Templates.
 */
public class HistoryRepository extends Repository {


    public HistoryRepository(EntityManager entityManager) {
        super(entityManager);
    }

    public Long countPendingHistories() {
        return countByNamedQuery("History.countPending");
    }

    public Long countApprovedHistories() {
        return countByNamedQuery("History.countApproved");
    }

    public Set<HistoryEntity> findAll() {
        return new HashSet<>(findByNamedQuery("History.findAll"));
    }

    public List<HistoryEntity> findPendingHistories(int limit, int offset) {
        return new ArrayList<>(findByNamedQuery("History.findPendingApproval", limit, offset));
    }

    public List<HistoryEntity> findPendingHistories(int limit, int offset, String sort, boolean ascending) {
        return findHistories(limit, offset, sort, ascending, false);
    }

    public List<HistoryEntity> findApprovedHistories(int limit, int offset) {
        return new ArrayList<>(findByNamedQuery("History.findByApproved", Map.of("approved", 1), limit, offset));
    }

    public List<HistoryEntity> findApprovedHistories(int limit, int offset, String sort, boolean ascending) {
        return findHistories(limit, offset, sort, ascending, true);
    }

    private List<HistoryEntity> findHistories(int limit, int offset, String sort, boolean ascending, boolean approved) {
        var criteriaBuilder = entityManager.getCriteriaBuilder();
        var criteriaQuery = criteriaBuilder.createQuery(HistoryEntity.class);
        var root = criteriaQuery.from(HistoryEntity.class);

        Predicate statusPredicate = approved
                ? criteriaBuilder.equal(root.get("approved"), 1)
                : criteriaBuilder.isNull(root.get("processedDate"));

        if ("concept".equals(sort)) {
            Join<HistoryEntity, ConceptMetadataEntity> conceptMetadata = root.join("conceptMetadata");
            Join<ConceptMetadataEntity, ConceptEntity> concept = conceptMetadata.join("concept");
            Join<ConceptEntity, ConceptNameEntity> conceptName = concept.join("conceptNames");
            Predicate primaryName = criteriaBuilder.equal(
                    criteriaBuilder.lower(conceptName.get("nameType")),
                    ConceptNameTypes.PRIMARY.getType()
            );
            Order order = ascending
                    ? criteriaBuilder.asc(conceptName.get("name"))
                    : criteriaBuilder.desc(conceptName.get("name"));
            criteriaQuery.select(root)
                    .where(criteriaBuilder.and(statusPredicate, primaryName))
                    .orderBy(order);
        } else {
            Order order = ascending
                    ? criteriaBuilder.asc(root.get(sort))
                    : criteriaBuilder.desc(root.get(sort));
            criteriaQuery.select(root)
                    .where(statusPredicate)
                    .orderBy(order);
        }

        var query = entityManager.createQuery(criteriaQuery);
        query.setFirstResult(offset);
        query.setMaxResults(limit);
        return new ArrayList<>(query.getResultList());
    }

    public Set<HistoryEntity> findByConceptName(String name) {
        return new HashSet<>(findByNamedQuery("History.findByConceptName", Map.of("name", name)));
    }
}
