/*
 * @(#)LinkTemplateService.java   2010.01.26 at 02:12:12 PST
 *
 * Copyright 2009 MBARI
 *
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */



package org.mbari.oni.jpa.services;



import java.util.*;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import org.mbari.oni.domain.ILink;
import org.mbari.oni.domain.LinkComparator;
import org.mbari.kb.core.knowledgebase.Concept;
import org.mbari.kb.core.knowledgebase.LinkTemplate;
import org.mbari.oni.jpa.entities.ConceptEntity;
import org.mbari.oni.jpa.entities.LinkTemplateEntity;

/**
 * Created by IntelliJ IDEA.
 * User: brian
 * Date: Aug 7, 2009
 * Time: 4:47:25 PM
 * To change this template use File | Settings | File Templates.
 */
public class LinkTemplateService extends Service {

    private static final Comparator<ILink> linkComparator = new LinkComparator();

    @Inject
    public LinkTemplateService(EntityManager entityManager) {
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
     * Find {@link LinkTemplate}s containing 'linkName' that are applicable to the
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

    public Collection<LinkTemplateEntity> findAll(int limit, int offset) {
        return findByNamedQuery("LinkTemplate.findAll", limit, offset);
    }


    public void validateToConcept(LinkTemplateEntity object) {
        var conceptDAO = new ConceptService(entityManager);
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
