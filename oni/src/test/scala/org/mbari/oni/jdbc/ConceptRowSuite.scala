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

package org.mbari.oni.jdbc

import java.time.Instant
import org.mbari.oni.domain.ConceptNameTypes

class ConceptRowSuite extends munit.FunSuite:
    test("ConceptRow constructor works as expected") {
        val now        = Instant.now()
        val conceptRow =
            ConceptRow(
                1,
                None,
                "name",
                Some("rankLevel"),
                Some("rankName"),
                ConceptNameTypes.PRIMARY.getType,
                Instant.EPOCH,
                now
            )
        assertEquals(conceptRow.id, 1L)
        assertEquals(conceptRow.rank, Some("rankLevelrankName"))
        assertEquals(conceptRow.nameType, ConceptNameTypes.PRIMARY.getType)
        assertEquals(conceptRow.lastUpdate, now)
    }
