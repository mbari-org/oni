/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2024
 *
 * oni code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential.
 */

package org.mbari.oni.jpa

import org.mbari.oni.jpa.TrimLastUpdatedTimePrecision.roundToMillis

import java.sql.Timestamp
import java.time.Instant

class TrimLastUpdatedTimePrecisionSuite extends munit.FunSuite {

    test("roundToMillis") {
        val ts = Instant.ofEpochSecond(1696519434567L) // 2023-10-05 15:33:54.567123456
        val ts1 = roundToMillis(ts)
        assertEquals(ts1.toEpochMilli, ts.toEpochMilli)
    }

//    test("roundToMillis 2") {
//        val now = Instant.now()
//        val ts = Timestamp.from(now)
//        assert(ts.getNanos != 0)
//        val ts1 = roundToMillis(ts)
//        assertEquals(ts1.getTime, ts.getTime)
//        assertEquals(ts1.getNanos, 0)
//    }

}
