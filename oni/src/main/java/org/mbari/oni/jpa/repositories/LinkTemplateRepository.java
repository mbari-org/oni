/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2024
 *
 * oni code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential.
 */

package org.mbari.oni.jpa.repositories;



import java.util.*;
import java.util.stream.Collectors;


import jakarta.persistence.EntityManager;

import org.mbari.oni.domain.ILink;
import org.mbari.oni.domain.LinkComparator;

import org.mbari.oni.jpa.entities.ConceptEntity;
import org.mbari.oni.jpa.entities.LinkTemplateEntity;

/**
 * Created by IntelliJ IDEA.
 * User: brian
 * Date: Aug 7, 2009
 * Time: 4:47:25 PM
 * To change this template use File | Settings | File Templates.
 */
public class LinkTemplateRepository extends Repository {

    private static final Comparator<ILink> linkComparator = new LinkComparator();


    public LinkTemplateRepository(EntityManager entityManager) {
        super(entityManager);
    }

    /**
     * Call this inside a transaction
     *
     * @param concept
     * @return
     */
    public Collection<LinkTemplateEntity> findAllApplicableToConcept(ConceptEntity concept) {

        List<LinkTemplateEntity> linkTemplates = new ArrayList<>();
        while (concept != null) {
            linkTemplates.addAll(concept.getConceptMetadata().getLinkTemplates());
            concept = concept.getParentConcept();
        }

        linkTemplates.sort(linkComparator);
        return linkTemplates;
    }

    /**
     *
     * @param linkName
     * @param toConcept
     * @param linkValue
     * @return
     */
    @SuppressWarnings("unchecked")
    public Collection<LinkTemplateEntity> findAllByLinkFields(String linkName, String toConcept, String linkValue) {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("linkName", linkName);
        params.put("toConcept", toConcept);
        params.put("linkValue", linkValue);
        return findByNamedQuery("LinkTemplate.findByFields", params);
    }

    /**
     *
     * @param linkName
     * @return
     */
    @SuppressWarnings("unchecked")
    public Collection<LinkTemplateEntity> findAllByLinkName(String linkName) {
        return findByNamedQuery("LinkTemplate.findByLinkName", Map.of("linkName", linkName));
    }

    /**
     * Find {@link LinkTemplateEntity}s containing 'linkName' that are applicable to the
     * provided concept. You should call this within a transaction
     *
     * @param linkName
     * @param concept
     * @return
     */
    public Collection<LinkTemplateEntity> findAllByLinkName(final String linkName, ConceptEntity concept) {

        Collection<LinkTemplateEntity> linkTemplates = findAllApplicableToConcept(concept);
        return  linkTemplates.stream()
                .filter(linkTemplate -> linkTemplate.getLinkName().equals(linkName))
                .collect(Collectors.toList());

    }
    
    public Long countAll() {
        return countByNamedQuery("LinkTemplate.countAll");
    }

    public Collection<LinkTemplateEntity> findAll(int limit, int offset) {
        return findByNamedQuery("LinkTemplate.findAll", limit, offset);
    }

    public Long countByToConcept(String toConcept) {
        return countByNamedQuery("LinkTemplate.countByToConcept", Map.of("toConcept", toConcept));
    }

    public Collection<LinkTemplateEntity> findByToConcept(String toConcept) {
        return findByNamedQuery("LinkTemplate.findByToConcept", Map.of("toConcept", toConcept));
    }

    public Collection<LinkTemplateEntity> findByToConcept(String toConcept, int limit, int offset) {
        return findByNamedQuery("LinkTemplate.findByToConcept", Map.of("toConcept", toConcept), limit, offset);
    }


    public void validateToConcept(LinkTemplateEntity object) {
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
