/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2024
 *
 * oni code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential.
 */

package org.mbari.oni.etc.jdk

import java.time.Instant

class InstantsSuite extends munit.FunSuite:

    test("parseIso8601"):
        val now     = Instant.now();
        val formats = Seq(
            Instants.TimeFormatter,
            Instants.CompactTimeFormatter,
            Instants.CompactTimeFormatterMs,
            Instants.CompactTimeFormatterNs
        )
        for f <- formats
        do
            val s = f.format(now)
            Instants.parseIso8601(s) match
                case Right(value) => // do nothing
                case Left(e)      => fail(s"Failed to parse $s", e)
