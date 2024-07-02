/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2024
 *
 * oni code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential.
 */

package org.mbari.oni.jpa.repositories;

import jakarta.persistence.EntityManager;
import org.mbari.oni.jpa.entities.LinkRealizationEntity;

import java.util.Map;
import java.util.Collection;

/**
 * Created by IntelliJ IDEA.
 * User: brian
 * Date: Aug 7, 2009
 * Time: 4:46:22 PM
 * To change this template use File | Settings | File Templates.
 */
public class LinkRealizationRepository extends Repository {

    public LinkRealizationRepository(EntityManager entityManager) {
        super(entityManager);
    }


    public Collection<LinkRealizationEntity> findAllByLinkName(String linkName) {
        return findByNamedQuery("LinkRealization.findByLinkName", Map.of("linkName", linkName));
    }
    

    public void validateToConcept(LinkRealizationEntity object) {
        var conceptDAO = new ConceptRepository(entityManager);
        var opt = conceptDAO.findByName(object.getToConcept());
        if (opt.isPresent()) {
            var concept = opt.get();
            object.setToConcept(concept.getPrimaryConceptName().getName());
        }
        else {
            log.atWarn().log(object + " contains a conceptName, " + object.getToConcept() +
                    ", that was not found in the knowlegebase");
        }
    }
    
}
