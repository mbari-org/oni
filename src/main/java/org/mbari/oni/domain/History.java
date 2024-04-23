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
