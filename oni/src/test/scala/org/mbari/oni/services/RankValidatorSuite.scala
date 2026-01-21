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
