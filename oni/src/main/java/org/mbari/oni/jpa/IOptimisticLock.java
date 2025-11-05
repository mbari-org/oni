/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2024
 *
 * oni code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential.
 */

package org.mbari.oni.jpa;

import java.sql.Timestamp;
import java.time.Instant;

public interface IOptimisticLock {

    Instant getLastUpdatedTimestamp();

    void setLastUpdatedTimestamp(Instant ts);
}
