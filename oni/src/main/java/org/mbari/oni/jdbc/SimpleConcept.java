/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2024
 *
 * oni code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential. 
 */

package org.mbari.oni.jdbc;

import java.util.List;

public record SimpleConcept(String name,
                            String rank,
                            List<String> alternativeNames) {

    public boolean containsName(String name) {
        return this.name.equals(name) || alternativeNames.contains(name);
    }

    public static SimpleConcept from(MutableConcept c) {
        var primaryName = c.getPrimaryName();
        var alternativeNames = c.getNames()
                .stream()
                .map(CName::name)
                .filter(n -> !n.equals(primaryName))
                .toList();
        return new SimpleConcept(primaryName,
                                c.getRank(),
                                alternativeNames);
    }
}
