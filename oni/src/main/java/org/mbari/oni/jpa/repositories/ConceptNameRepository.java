/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2024
 *
 * oni code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential. 
 */

package org.mbari.oni.jpa.repositories;


import java.util.List;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.mbari.oni.jpa.entities.ConceptNameEntity;

/**
 * Created by IntelliJ IDEA.
 * User: brian
 * Date: Aug 7, 2009
 * Time: 4:45:04 PM
 * To change this template use File | Settings | File Templates.
 */

public class ConceptNameRepository extends Repository {

    public ConceptNameRepository(EntityManager entityManager) {
        super(entityManager);
    }


    public Optional<ConceptNameEntity> findByName(final String name) {
        List<ConceptNameEntity> names = findByNamedQuery("ConceptName.findByName", Map.of("name", name));
        return names.stream().findFirst();
    }

    public Collection<ConceptNameEntity> findAll() {
        return findByNamedQuery("ConceptName.findAll");
    }

    public Collection<ConceptNameEntity> findByNameContaining(final String substring) {
        Map<String, Object> params = Map.of("name", "%" + substring.toLowerCase() + "%");
        return findByNamedQuery("ConceptName.findByNameLike", params);
    }

    public Collection<ConceptNameEntity> findByNameStartingWith(final String s) {
        Map<String, Object> params = Map.of("name", s.toLowerCase() + "%");
        return findByNamedQuery("ConceptName.findByNameLike", params);
    }

    public List<String> findAllNamesAsStrings() {
        return findByNamedQuery("ConceptName.findAllNamesAsStrings");
    }

    public boolean doesConceptNameExist(final String name) {
        final Query query = entityManager.createNamedQuery("ConceptName.countByName");
        query.setParameter(1, name);
        return (boolean) query.getResultList()
                .stream()
                .findFirst()
                .map(i -> ((int) i) > 0)
                .orElse(Boolean.FALSE);
    }

}
