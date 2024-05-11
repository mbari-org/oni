/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2024
 *
 * oni code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential. 
 */

package org.mbari.oni.jdbc;

import java.time.Instant;
import java.util.Optional;

public record ConceptRow(Long id,
                         Long parentId,
                         String name,
                         String rankLevel,
                         String rankName,
                         String nameType,
                         Instant conceptTimestamp,
                         Instant conceptNameTimestamp) {

    public Optional<String> rank() {
        if (rankName == null) {
            return Optional.empty();
        }
        return rankLevel == null ? Optional.of(rankName) : Optional.of(rankLevel + rankName);
    }

    public Instant lastUpdate() {
        return conceptTimestamp.isAfter(conceptNameTimestamp) ? conceptTimestamp : conceptNameTimestamp;
    }

}
