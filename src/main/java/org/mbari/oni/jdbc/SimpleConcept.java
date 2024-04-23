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
