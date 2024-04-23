package org.mbari.oni.jdbc;

import java.util.List;

public record ImmutableConcept(String name,
                               String rank,
                               List<String> alternativeNames,
                               List<ImmutableConcept> children) {

    public boolean containsName(String name) {
        return this.name.equals(name) || alternativeNames.contains(name);
    }

    public static ImmutableConcept from(MutableConcept c) {
        var primaryName = c.getPrimaryName();
        var alternativeNames = c.getNames()
                .stream()
                .map(CName::name)
                .filter(n -> !n.equals(primaryName))
                .toList();
        var children = c.getChildren()
                .stream()
                .map(ImmutableConcept::from)
                .toList();
        return new ImmutableConcept(primaryName,
                                    c.getRank(),
                                    alternativeNames,
                                    children);
    }

}
