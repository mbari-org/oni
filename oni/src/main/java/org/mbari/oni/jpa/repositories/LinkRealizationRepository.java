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

    public Long countAll() {
        return countByNamedQuery("LinkRealization.countAll");
    }

    public Collection<LinkRealizationEntity> findAll(int limit, int offset) {
        return findByNamedQuery("LinkRealization.findAll", limit, offset);
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
