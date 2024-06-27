package org.mbari.oni.domain

import org.mbari.oni.jpa.entities.{ConceptEntity, ConceptNameEntity}

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

        concept.setAphiaId(10)
        val metadata2 = ConceptMetadata.from(concept)
        assertEquals(metadata2.aphiaId, Some(10L))

    }

}
