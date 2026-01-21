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

package org.mbari.oni.domain

import org.mbari.oni.jpa.entities.{ConceptEntity, ConceptNameEntity}
import org.mbari.oni.etc.circe.CirceCodecs.{given, *}

class ConceptMetadataSuite extends munit.FunSuite {

    test("from (entity)") {
        val concept = new ConceptEntity()
        val conceptName = new ConceptNameEntity("root", ConceptNameTypes.PRIMARY.getType)
        concept.addConceptName(conceptName)
        concept.setRankName("rankName")
        concept.setRankLevel("rankLevel")

        val metadata = ConceptMetadata.from(concept)
        assertEquals(metadata.name, "root")
        assertEquals(metadata.rank, Some("rankLevelrankName"))
        assertEquals(metadata.id, None)
        assertEquals(metadata.aphiaId, None)
//        println(metadata.stringify)

        concept.setAphiaId(10)
        val metadata2 = ConceptMetadata.from(concept)
        assertEquals(metadata2.aphiaId, Some(10L))
//        println(metadata2.stringify)

    }

}
