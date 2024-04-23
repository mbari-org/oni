package org.mbari.oni.jpa.services;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import org.mbari.kb.core.knowledgebase.History;

import java.util.Set;
import java.util.HashSet;

/**
 * Created by IntelliJ IDEA.
 * User: brian
 * Date: Aug 7, 2009
 * Time: 4:45:43 PM
 * To change this template use File | Settings | File Templates.
 */
public class HistoryService extends Service {


    @Inject
    public HistoryService(EntityManager entityManager) {
        super(entityManager);
    }

    public Set<History> findAll() {
        return new HashSet<>(findByNamedQuery("History.findAll"));
    }

    public Set<History> findPendingHistories() {
        return new HashSet<>(findByNamedQuery("History.findPendingApproval"));
    }

    public Set<History> findApprovedHistories() {
        return new HashSet<>(findByNamedQuery("History.findApproved"));
    }
}
