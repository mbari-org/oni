/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2024
 *
 * oni code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential. 
 */

package org.mbari.oni.jpa.repositories;

import jakarta.persistence.EntityManager;
import org.mbari.oni.jpa.entities.ReferenceEntity;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ReferenceRepository extends Repository {

    public ReferenceRepository(EntityManager entityManager) {
        super(entityManager);
    }

    public List<ReferenceEntity> findAll(int limit, int offset) {
        return findByNamedQuery("Reference.findAll", limit, offset);
    }

    public Optional<ReferenceEntity> findByDoi(URI doi) {
        List<ReferenceEntity> results = findByNamedQuery("Reference.findByDoi", Map.of("doi", doi));
        return results.stream().findFirst();
    }

    public List<ReferenceEntity> findByGlob(String glob, int limit, int offset) {
        var key = '%' + glob + '%';
        return findByNamedQuery("Reference.findByGlob", Map.of("glob", key), limit, offset);
    }

    public List<ReferenceEntity> findByConceptName(String name) {
        return findByNamedQuery("Reference.findByConceptName", Map.of("name", name));
    }

    public Optional<ReferenceEntity> findById(Long id) {
        List<ReferenceEntity> results = findByNamedQuery("Reference.findById", Map.of("id", id));
        return results.stream().findFirst();
    }

}
