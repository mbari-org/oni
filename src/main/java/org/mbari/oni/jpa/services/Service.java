package org.mbari.oni.jpa.services;

import jakarta.inject.Inject;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.mbari.oni.etc.jdk.Logging;
import org.mbari.oni.jpa.IPersistentObject;

import java.util.List;
import java.util.Map;

public abstract class Service {

    final EntityManager entityManager;

    @Inject
    public Service(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    protected static final Logging log = new Logging(Service.class);

    public <T> List<T> findByNamedQuery(String name,
                                        Map<String, Object> namedParams) {
        Query query = entityManager.createNamedQuery(name);
        namedParams.forEach(query::setParameter);
        return (List<T>) query.getResultList();
    }

    public <T> List<T> findByNamedQuery(String name,
                                        Map<String, Object> namedParams,
                                        int limit,
                                        int offset) {
        Query query = entityManager.createNamedQuery(name);
        namedParams.forEach(query::setParameter);
        query.setMaxResults(limit);
        query.setFirstResult(offset);
        return (List<T>) query.getResultList();
    }



    public <T> List<T> findByNamedQuery(String name) {
        Query query = entityManager.createNamedQuery(name);
        return (List<T>) query.getResultList();
    }

    public <T> List<T> findByNamedQuery(String name, int limit, int offset) {
        Query query = entityManager.createNamedQuery(name);
        query.setMaxResults(limit);
        query.setFirstResult(offset);
        return (List<T>) query.getResultList();
    }

    public <T> T find(T object) {
        final IPersistentObject jpaEntity = (IPersistentObject) object;
        return (T) entityManager.find(jpaEntity.getClass(), jpaEntity.getId());
    }

    public <T> T findByPrimaryKey(Class<T> clazz, Object primaryKey) {
        return entityManager.find(clazz, primaryKey);
    }


}
