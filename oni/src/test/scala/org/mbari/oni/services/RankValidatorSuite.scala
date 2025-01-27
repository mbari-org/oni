/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2024
 *
 * oni code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential.
 */

package org.mbari.oni.services

class RankValidatorSuite extends munit.FunSuite {

    test("validate") {

        val ranks = RankValidator.ValidRanks
        for (rank <- ranks) {
            val result = RankValidator.validate(rank)
            assertEquals(result, true)
        }

        val invalidRanks = Seq("foo", "bar", "baz")
        for (rank <- invalidRanks) {
            val result = RankValidator.validate(rank)
            assertEquals(result, false)
        }

    }
  
}
