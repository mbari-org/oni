/*
 * Copyright 2024 Monterey Bay Aquarium Research Institute
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.mbari.oni.jpa;

import jakarta.persistence.PrePersist;
import jakarta.persistence.PreRemove;
import jakarta.persistence.PreUpdate;

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
