/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2024
 *
 * oni code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential. 
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

    public Set<HistoryEntity> findAll() {
        return new HashSet<>(findByNamedQuery("History.findAll"));
    }

    public Set<HistoryEntity> findPendingHistories() {
        return new HashSet<>(findByNamedQuery("History.findPendingApproval"));
    }

    public Set<HistoryEntity> findApprovedHistories() {
        return new HashSet<>(findByNamedQuery("History.findByApproved", Map.of("approved", 1)));
    }

    public Set<HistoryEntity> findByConceptName(String name) {
        return new HashSet<>(findByNamedQuery("History.findByConceptName", Map.of("name", name)));
    }
}
