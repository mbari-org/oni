/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2024
 *
 * oni code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential. 
 */

package org.mbari.oni.domain;

import java.util.List;

public record ConceptNode(String name,
                          List<String> alternateNames,
                          List<MediaNode> media,
                          List<LinkNode> descriptors,
                          String rank,
                          String author) {
}
