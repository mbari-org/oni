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
