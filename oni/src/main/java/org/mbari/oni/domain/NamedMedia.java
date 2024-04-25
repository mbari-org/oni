/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2024
 *
 * oni code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential. 
 */

package org.mbari.oni.domain;

import org.mbari.oni.jpa.entities.MediaEntity;

import java.net.URI;
import java.net.URL;
import java.util.Optional;

public record NamedMedia(String name,
                         URL url,
                         String caption,
                         String credit,
                         String mimeType,
                         Boolean isPrimary) {

    public static Optional<NamedMedia> from(String name, MediaEntity mediaEntity) {
        if (mediaEntity == null || name == null) {
            return Optional.empty();
        }
        try {
            var url = URI.create(mediaEntity.getUrl()).toURL();
            var media = new NamedMedia(name,
                    url,
                    mediaEntity.getCaption(),
                    mediaEntity.getCredit(),
                    mediaEntity.getType(),
                    mediaEntity.isPrimary());
            return Optional.of(media);
        }
        catch (Exception e) {
            return Optional.empty();
        }
    }
}
