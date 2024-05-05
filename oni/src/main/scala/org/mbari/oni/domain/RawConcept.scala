/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2024
 *
 * oni code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential. 
 */

package org.mbari.oni.domain

import org.mbari.oni.etc.jdk.Numbers.*
import org.mbari.oni.jpa.entities.ConceptEntity

import scala.jdk.CollectionConverters.*

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

    def toEntity: ConceptEntity = toEntityWithId(1)

    /**
     * Every Concept has to have an ID before it's added to a parent concept entity. A concept's hashcode is based on
     * this id and adding to parent will results in hash collisions. The outcome is that only one child concept is added
     * to the parent entity.
     * @param id
     * @return
     */
    private def toEntityWithId(id: Long): ConceptEntity =
        val entity = new ConceptEntity()
        entity.setId(id)
        var nextId = id + 1

        names.map(_.toEntity).foreach(entity.addConceptName)
        entity.setOriginator(originator.orNull)
        metadata.map(_.toEntity).foreach(entity.setConceptMetadata)
        aphiaId.foreach(v => entity.setAphiaId(v.longValue()))
        entity.setRankLevel(rankLevel.orNull)
        entity.setRankName(rankName.orNull)
        entity.setReference(reference.orNull)
        entity.setStructureType(structureType.orNull)
//        children.map(_.toEntity).foreach(entity.addChildConcept)
        children.foreach(c =>
            nextId = nextId + 1
            entity.addChildConcept(c.toEntityWithId(nextId))
        )
        entity

object RawConcept:
    def fromEntity(entity: ConceptEntity): RawConcept =
        RawConcept(
            names = entity.getConceptNames.asScala.map(RawConceptName.fromEntity).toSet,
            originator = Option(entity.getOriginator),
            metadata = Option(entity.getConceptMetadata).map(RawConceptMetadata.fromEntity),
            children = entity.getChildConcepts.asScala.map(RawConcept.fromEntity).toSet,
            aphiaId = Option(entity.getAphiaId).map(_.toLong),
            rankLevel = Option(entity.getRankLevel),
            rankName = Option(entity.getRankName),
            reference = Option(entity.getReference),
            structureType = Option(entity.getStructureType)
        )
