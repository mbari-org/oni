/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2024
 *
 * oni code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential. 
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
