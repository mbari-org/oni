/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2024
 *
 * oni code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential.
 */

package org.mbari.oni.domain;

import java.time.Instant;

public record History(String concept,
                      Instant creationTimestamp,
                      String creatorName,
                      String action,
                      String field,
                      String oldValue,
                      String newValue,
                      Boolean approved,
                      Instant processedTimestamp,
                      String processorName) {
}
