package org.mbari.oni.jpa.services;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.mbari.kb.core.knowledgebase.ConceptNameDAO;
import org.mbari.kb.core.knowledgebase.ConceptName;

import java.util.List;

import java.util.Collection;
import java.util.Map;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

/**
 * Created by IntelliJ IDEA.
 * User: brian
 * Date: Aug 7, 2009
 * Time: 4:45:04 PM
 * To change this template use File | Settings | File Templates.
 */
@ApplicationScoped
public class ConceptNameService extends Service {

    @Inject
    public ConceptNameService(EntityManager entityManager) {
        super(entityManager);
    }

    public ConceptName findByName(final String name) {
        List<ConceptName> names = findByNamedQuery("ConceptName.findByName", Map.of("name", name));
        return names.isEmpty() ? null : names.getFirst();
    }

    public Collection<ConceptName> findAll() {
        return findByNamedQuery("ConceptName.findAll");
    }

    public Collection<ConceptName> findByNameContaining(final String substring) {
        Map<String, Object> params = Map.of("name", "%" + substring.toLowerCase() + "%");
        return findByNamedQuery("ConceptName.findByNameLike", params);
    }

    public Collection<ConceptName> findByNameStartingWith(final String s) {
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
