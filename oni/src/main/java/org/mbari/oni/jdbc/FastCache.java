/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2024
 *
 * oni code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential. 
 */

package org.mbari.oni.jdbc;

import java.time.Instant;
import java.util.List;

public record FastCache(Instant lastUpdate,
                        MutableConcept rootNode,
                        List<MutableConcept> allNodes) {
}
