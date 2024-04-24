package org.mbari.oni.jpa.services;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import org.mbari.oni.jpa.entities.HistoryEntity;

import java.util.Set;
import java.util.HashSet;

/**
 * Created by IntelliJ IDEA.
 * User: brian
 * Date: Aug 7, 2009
 * Time: 4:45:43 PM
 * To change this template use File | Settings | File Templates.
 */
@ApplicationScoped
public class HistoryService extends Service {


    public HistoryService(EntityManager entityManager) {
        super(entityManager);
    }

    public HistoryService() {
        super();
    }

    public Set<HistoryEntity> findAll() {
        return new HashSet<>(findByNamedQuery("History.findAll"));
    }

    public Set<HistoryEntity> findPendingHistories() {
        return new HashSet<>(findByNamedQuery("History.findPendingApproval"));
    }

    public Set<HistoryEntity> findApprovedHistories() {
        return new HashSet<>(findByNamedQuery("History.findApproved"));
    }
}
