/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2024
 *
 * oni code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential. 
 */

package org.mbari.oni.domain

import org.mbari.oni.etc.jdk.Numbers.*
import org.mbari.oni.jpa.entities.ConceptEntity

case class RawConcept(
    names: Set[RawConceptName] = Set.empty,
    originator: Option[String] = None,
    metadata: Option[RawConceptMetadata] = None,
    children: Set[RawConcept] = Set.empty,
    aphiaId: Option[Long] = None,
    rankLevel: Option[String] = None,
    rankName: Option[String] = None,
    reference: Option[String] = None,
    structureType: Option[String] = None
):

    def toEntity: ConceptEntity =
        val entity = new ConceptEntity()

        names.map(_.toEntity) foreach (entity.addConceptName)
        entity.setOriginator(originator.orNull)
        metadata.map(_.toEntity).foreach(entity.setConceptMetadata)
        children.map(_.toEntity).foreach(entity.addChildConcept)
        aphiaId.foreach(v => entity.setAphiaId(v.longValue()))
        entity.setRankLevel(rankLevel.orNull)
        entity.setRankName(rankName.orNull)
        entity.setReference(reference.orNull)
        entity.setStructureType(structureType.orNull)
        entity
