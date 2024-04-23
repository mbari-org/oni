package org.mbari.oni.jdbc;

/**
 * Simple Wrapper Around a conceptname
 * @param name The concept name
 * @param nameType It's type.
 * @See ConceptNameType
 */
public record CName(String name, String nameType) {
}
