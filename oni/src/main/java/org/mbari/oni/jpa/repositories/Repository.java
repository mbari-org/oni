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
import jakarta.persistence.Query;
import org.mbari.oni.etc.jdk.Logging;
import org.mbari.oni.jpa.IPersistentObject;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.lang.System.Logger.Level;

public abstract class Repository {


    final EntityManager entityManager;

    public Repository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    protected static final Logging log = new Logging(Repository.class);

    public Long countByNamedQuery(String name) {
        debugLog(name, Map.of());
        Query query = entityManager.createNamedQuery(name);
        return ((Number) query.getSingleResult()).longValue();
    }

    public Long countByNamedQuery(String name, Map<String, Object> namedParams) {
        debugLog(name, Map.of());
        Query query = entityManager.createNamedQuery(name);
        namedParams.forEach(query::setParameter);
        return ((Number) query.getSingleResult()).longValue();
    }

    public <T> List<T> findByNamedQuery(String name,
                                        Map<String, Object> namedParams) {
        debugLog(name, namedParams);
        Query query = entityManager.createNamedQuery(name);
        namedParams.forEach(query::setParameter);
        return (List<T>) query.getResultList();
    }

    public <T> List<T> findByNamedQuery(String name,
                                        Map<String, Object> namedParams,
                                        int limit,
                                        int offset) {
        debugLog(name, namedParams);
        Query query = entityManager.createNamedQuery(name);
        namedParams.forEach(query::setParameter);
        query.setMaxResults(limit);
        query.setFirstResult(offset);
        return (List<T>) query.getResultList();
    }

    public void delete(Object object) {
        entityManager.remove(object);
    }

    public void create(Object object) {
        entityManager.persist(object);
    }

    public void update(Object object) {
        entityManager.merge(object);
    }

    public <T> List<T> findByNamedQuery(String name) {
        debugLog(name, Map.of());
        Query query = entityManager.createNamedQuery(name);
        return (List<T>) query.getResultList();
    }

    public <T> List<T> findByNamedQuery(String name, int limit, int offset) {
        debugLog(name, Map.of());
        Query query = entityManager.createNamedQuery(name);
        query.setMaxResults(limit);
        query.setFirstResult(offset);
        return (List<T>) query.getResultList();
    }

    public <T> Optional<T> find(T object) {
        final IPersistentObject jpaEntity = (IPersistentObject) object;
        var t = (T) entityManager.find(jpaEntity.getClass(), jpaEntity.getId());
        return Optional.ofNullable(t);
    }

    public <T> Optional<T> findByPrimaryKey(Class<T> clazz, Object primaryKey) {
        var t = entityManager.find(clazz, primaryKey);
        return Optional.ofNullable(t);
    }

    private void debugLog(String name, Map<String, Object> params) {

        if (log.logger().isLoggable(Level.DEBUG)) {
            StringBuilder sb = new StringBuilder("Executing FIND using named query '");
            sb.append(name).append("'");

            if (params.size() > 0) {
                sb.append(" with parameters:\n");

                for (String string : params.keySet()) {
                    sb.append("\t").append(string).append(" = ").append(params.get(string));
                }
            }

            log.atDebug().log(sb.toString());

        }
    }


}
