/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2024
 *
 * oni code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential. 
 */

package org.mbari.oni.jdbc;

/**
 * Simple Wrapper Around a conceptname
 * @param name The concept name
 * @param nameType It's type.
 * @See ConceptNameType
 */
public record CName(String name, String nameType) {
}
