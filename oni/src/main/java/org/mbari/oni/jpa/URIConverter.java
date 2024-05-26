/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2024
 *
 * oni code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential. 
 */

package org.mbari.oni.jpa;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Created by brian on 5/12/16.
 */
@Converter(autoApply = true)
public class URIConverter implements AttributeConverter<URI, String> {

    private static final System.Logger log = System.getLogger(URIConverter.class.getName());

    @Override
    public String convertToDatabaseColumn(URI uri) {
        return uri == null ? null : uri.toString();
    }

    @Override
    public URI convertToEntityAttribute(String s) {
        URI uri = null;
        if (s != null) {
            try {
                uri = new URI(s);
            }
            catch (URISyntaxException e) {
                log.log(System.Logger.Level.WARNING, "Bad URI found. Could not convert " + s + " to a URI");
            }
        }

        return uri;
    }
}

