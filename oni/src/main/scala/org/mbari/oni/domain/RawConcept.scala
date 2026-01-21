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

import org.mbari.oni.jpa.entities.ConceptEntity

import scala.jdk.CollectionConverters.*

case class RawConcept(
    names: Set[RawConceptName] = Set.empty,
    metadata: Option[RawConceptMetadata] = None,
    children: Set[RawConcept] = Set.empty,
    aphiaId: Option[Long] = None,
    rankLevel: Option[String] = None,
    rankName: Option[String] = None,
    id: Option[Long] = None
):

    lazy val primaryConceptName: Option[RawConceptName] = names.find(_.nameType == ConceptNameTypes.PRIMARY.getType)

    lazy val primaryName: String = primaryConceptName.map(_.name).getOrElse("")

    /**
     * @return
     */
    lazy val descendantNames: Seq[String] = descendants
        .flatMap(_.names.map(_.name))
        .toSeq
        .sorted

    lazy val descendants: Set[RawConcept] = children.flatMap(_.descendants) + this

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
        metadata.map(_.toEntity).foreach(entity.setConceptMetadata)
        aphiaId.foreach(v => entity.setAphiaId(v.longValue()))
        entity.setRankLevel(rankLevel.orNull)
        entity.setRankName(rankName.orNull)
//        children.map(_.toEntity).foreach(entity.addChildConcept)
        children.foreach(c =>
            nextId = nextId + 1
            entity.addChildConcept(c.toEntityWithId(nextId))
        )
        entity

object RawConcept:

    def from(entity: ConceptEntity): RawConcept = from(entity, includeChildren = true)

    def from(entity: ConceptEntity, includeChildren: Boolean): RawConcept =
        val children = if includeChildren then entity.getChildConcepts.asScala.map(RawConcept.from).toSet else Set.empty
        RawConcept(
            names = entity.getConceptNames.asScala.map(RawConceptName.from).toSet,
            metadata = Option(entity.getConceptMetadata).map(RawConceptMetadata.from),
            children = children,
            aphiaId = Option(entity.getAphiaId).map(_.toLong),
            rankLevel = Option(entity.getRankLevel),
            rankName = Option(entity.getRankName),
            id = Option(entity.getId)
        )
