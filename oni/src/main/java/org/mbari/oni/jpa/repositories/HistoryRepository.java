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
import org.mbari.oni.jpa.entities.HistoryEntity;

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

    public Set<HistoryEntity> findPendingHistories(int limit, int offset) {
        return new HashSet<>(findByNamedQuery("History.findPendingApproval", limit, offset));
    }

    public Set<HistoryEntity> findPendingHistories(int limit, int offset, String sort, boolean ascending) {
        var criteriaBuilder = entityManager.getCriteriaBuilder();
        var criteriaQuery = criteriaBuilder.createQuery(HistoryEntity.class);
        var root = criteriaQuery.from(HistoryEntity.class);
        criteriaQuery.select(root)
            .where(criteriaBuilder.isNull(root.get("processedDate")))
            .orderBy(ascending ? criteriaBuilder.asc(root.get(sort)) : criteriaBuilder.desc(root.get(sort)));
        var query = entityManager.createQuery(criteriaQuery);
        query.setFirstResult(offset);
        query.setMaxResults(limit);
        return new HashSet<>(query.getResultList());
    }

    public Set<HistoryEntity> findApprovedHistories(int limit, int offset) {
        return new HashSet<>(findByNamedQuery("History.findByApproved", Map.of("approved", 1), limit, offset));
    }

    public Set<HistoryEntity> findApprovedHistories(int limit, int offset, String sort, boolean ascending) {
        var criteriaBuilder = entityManager.getCriteriaBuilder();
        var criteriaQuery = criteriaBuilder.createQuery(HistoryEntity.class);
        var root = criteriaQuery.from(HistoryEntity.class);
        criteriaQuery.select(root)
            .where(criteriaBuilder.equal(root.get("approved"), 1))
            .orderBy(ascending ? criteriaBuilder.asc(root.get(sort)) : criteriaBuilder.desc(root.get(sort)));
        var query = entityManager.createQuery(criteriaQuery);
        query.setFirstResult(offset);
        query.setMaxResults(limit);
        return new HashSet<>(query.getResultList());
    }

    public Set<HistoryEntity> findByConceptName(String name) {
        return new HashSet<>(findByNamedQuery("History.findByConceptName", Map.of("name", name)));
    }
}
