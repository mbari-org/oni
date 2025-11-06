/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2024
 *
 * oni code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential.
 */

package org.mbari.oni.jpa;

import jakarta.persistence.PrePersist;
import jakarta.persistence.PreRemove;
import jakarta.persistence.PreUpdate;

import java.sql.Timestamp;
import java.time.Instant;

public class TrimLastUpdatedTimePrecision {

    @PreRemove
    @PreUpdate
    @PrePersist
    public void roundLockTimestamp(Object object) {
        if (object instanceof IOptimisticLock entity) {
            var ts = entity.getLastUpdatedTimestamp();
            if (ts != null) {
                // This is actually dropping the nanos component
                var roundedTs = roundToMillis(ts);
                entity.setLastUpdatedTimestamp(roundedTs);
            }
        }
    }

    public static Instant roundToMillis(Instant ts) {
        if (ts == null) {
            return null;
        }
        else {
            return ts.truncatedTo(java.time.temporal.ChronoUnit.MILLIS);
        }
    }


}
