package org.mbari.oni.domain;

import java.util.List;

public record ConceptNode(String name,
                          List<String> alternateNames,
                          List<MediaNode> media,
                          List<LinkNode> descriptors,
                          String rank,
                          String author) {
}
