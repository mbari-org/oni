/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2024
 *
 * oni code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential.
 */

package org.mbari.oni.jpa.repositories;

import jakarta.persistence.EntityManager;
import org.mbari.oni.domain.NamedMedia;
import org.mbari.oni.jdbc.FastPhylogenyService;
import org.mbari.oni.jpa.entities.MediaEntity;

import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: brian
 * Date: Aug 7, 2009
 * Time: 4:48:03 PM
 * To change this template use File | Settings | File Templates.
 */
public class MediaRepository extends Repository {

    FastPhylogenyService fastPhylogenyService;

    public MediaRepository(EntityManager entityManager, FastPhylogenyService fastPhylogenyService) {
        super(entityManager);
    }

    /**
     * Finds media representative of a concept. If there are not enough media for the concept, it will find media from its
     * descendant concepts.
     * @param conceptName concept name
     * @param count number of media to find
     * @return list of named media
     */
    public List<NamedMedia> findRepresentativeMedia(String conceptName, int count) {
        List<MediaEntity> xs = findByNamedQuery("Media.findByConceptName", Map.of("name", conceptName));
        var n = Math.min(count, xs.size());
        var a = xs.subList(0, n)
                .stream()
                .flatMap(m -> NamedMedia.from(conceptName, m).stream())
                .toList();
        var namedMedia = new ArrayList<>(a);
        if (namedMedia.size() < count) {
            var descendantMedia = findDescendantMedia(conceptName);
            var m = Math.min(count - namedMedia.size(), descendantMedia.size() - namedMedia.size());
            if (m > 0) {
                Collections.shuffle(descendantMedia);
                namedMedia.addAll(descendantMedia.subList(0, m));
            }
        }
        return namedMedia;
    }

    public List<NamedMedia> findDescendantMedia(String conceptName) {
        var conceptNames = fastPhylogenyService.findDescendantNamesAsJava(conceptName);
        var media = new ArrayList<NamedMedia>();
        for (var name : conceptNames) {
            List<MediaEntity> xs = findByNamedQuery("Media.findByConceptName", Map.of("name", name));
            var namedMedia = xs.stream()
                    .flatMap(m -> NamedMedia.from(name, m).stream())
                    .toList();
            media.addAll(namedMedia);
        }
        return media;
    }
}
