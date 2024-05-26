package org.mbari.oni.jpa.repositories;

import org.mbari.oni.jpa.entities.ReferenceEntity;

import java.util.List;

public class ReferenceRepository extends Repository {

    public ReferenceRepository(EntityManager entityManager) {
        super(entityManager);
    }

    public Optional<ReferenceEntity> findByDoi(URI doi) {
        return findByNamedQuery("Reference.findByDoi", Map.of("doi", doi)).stream().findFirst();
    }

    public List<ReferenceEntity> findByGlob(String glob) {
        var key = '%' + glob + '%';
        return findByNamedQuery("Reference.findByGlob", Map.of("glob", key));
    }

    public List<ReferenceEntity> findByConceptName(String name) {
        return findByNamedQuery("Reference.findByConceptName", Map.of("name", name));
    }


}
