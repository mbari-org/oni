package org.mbari.oni.jdbc;

import java.time.Instant;
import java.util.List;

public record FastCache(Instant lastUpdate,
                        MutableConcept rootNode,
                        List<MutableConcept> allNodes) {
}
