/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2024
 *
 * oni code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential. 
 */

package org.mbari.oni.jpa.repositories;

import jakarta.persistence.EntityManager;
import org.mbari.oni.jpa.VARSPersistenceException;

import java.util.*;

import org.mbari.oni.etc.jdk.Logging;
import org.mbari.oni.jpa.entities.ConceptEntity;

/**
 * Created by IntelliJ IDEA.
 * User: brian
 * Date: Aug 7, 2009
 * Time: 4:43:41 PM
 * To change this template use File | Settings | File Templates.
 */

public class ConceptRepository extends Repository {

    private static final Logging log = new Logging(ConceptRepository.class);

    public ConceptRepository(EntityManager entityManager) {
        super(entityManager);
    }


    public Optional<ConceptEntity> findRoot() {
        List<ConceptEntity> roots = findByNamedQuery("Concept.findRoot");
        if (roots.size() > 1) {
            log.atError().log("ERROR!! More than one root was found in the knowedgebase");
            throw new VARSPersistenceException("ERROR!! More than one root was found in the knowedgebase");
        }
        return roots.stream().findFirst();
    }

    /**
     * This find method should be called inside of a transaction
     * @param name
     * @return
     */
    public Optional<ConceptEntity> findByName(final String name) {
        List<ConceptEntity> concepts =  findByNamedQuery("Concept.findByName", Map.of("name", name));
        return concepts.stream().findFirst();
    }

    public Optional<ConceptEntity> findByAphiaId(final Long aphiaId) {
        List<ConceptEntity> concepts =  findByNamedQuery("Concept.findByAphiaId", Map.of("aphiaId", aphiaId));
        return concepts.stream().findFirst();
    }

    public List<ConceptEntity> findAllByNameContaining(final String nameGlob) {
        final String name = "%" + nameGlob.toLowerCase() + "%";
        return findByNamedQuery("Concept.findAllByNameGlob", Map.of("name", name));
    }

    public List<ConceptEntity> findAllByNameStartingWith(final String nameGlob) {
        final String name = nameGlob.toLowerCase() + "%";
        return findByNamedQuery("Concept.findAllByNameGlob", Map.of("name", name));

    }

    public List<ConceptEntity> findAllByNameEndingWith(final String nameGlob) {
        final String name = '%' + nameGlob.toLowerCase();
        return findByNamedQuery("Concept.findAllByNameGlob", Map.of("name", name));
    }


    public Collection<ConceptEntity> findAll(int limit, int offset) {
        return findByNamedQuery("Concept.findAll", limit, offset);
    }

    /**
     * Should be called within a JPA transaction
     * @param concept
     * @return
     */
    public Collection<ConceptEntity> findDescendents(ConceptEntity concept) {
        Collection<ConceptEntity> concepts = new ArrayList<>();
        findDescendents(concept, concepts);

        return concepts;
    }

    private void findDescendents(ConceptEntity concept, Collection<ConceptEntity> concepts) {
        concepts.add(concept);
        for (var child : concept.getChildConcepts()) {
            findDescendents(child, concepts);
        }
    }



    /**
     * Delete a concept and allImpl of its descendents
     */
    public int deleteBranchByName(String conceptName) {

        var deleteCount = 0;

        // Find the concept
        var opt = findByName(conceptName);
        if (opt.isPresent()) {
            var concept = opt.get();
            var descendants = findDescendents(concept);
            Queue<ConceptEntity> queue = new LinkedList<>(descendants);
            while(!queue.isEmpty()) {
                var c = queue.poll();
                if (c.getChildConcepts().isEmpty()) {
                    // If it doesn't have any children delete the concept
                    var parent = c.getParentConcept();
                    if (parent != null) {
                        parent.removeChildConcept(c);
                    }
                    deleteCount++;
                    entityManager.remove(c);
                }
                else {
                    // If it has children put it at the end of the queue
                    queue.offer(c);
                }
            }
        }
        return deleteCount;
    }

}
