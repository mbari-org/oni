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

import org.mbari.oni.etc.jdk.Numbers.*
import org.mbari.oni.jpa.entities.ConceptEntity

import scala.jdk.CollectionConverters.*

/**
 * Detailed information about a concept
 * @author
 *   Brian Schlining
 * @since 2016-11-17T15:54:00
 */
case class ConceptMetadata(
    name: String,
    alternateNames: Set[String] = Set.empty,
    media: Set[Media] = Set.empty,
    linkRealizations: Set[Link] = Set.empty,
    rank: Option[String] = None,
    rankName: Option[String] = None,
    rankLevel: Option[String] = None,
    author: Option[String] = None,
    aphiaId: Option[Long] = None,
    references: Set[Reference] = Set.empty,
    id: Option[Long] = None
) {}

object ConceptMetadata:

    def from(concept: ConceptEntity): ConceptMetadata =
        val name = concept.getPrimaryConceptName.getName

        val alternateNames = concept
            .getAlternativeConceptNames
            .asScala
            .map(_.getName)
            .toSet

        val media = concept
            .getConceptMetadata
            .getMedias
            .asScala
            .toSet
            .map(Media.from)

        val linkRealizations = concept
            .getConceptMetadata
            .getLinkRealizations
            .asScala
            .toSet
            .map(Link.from)

        val references = concept
            .getConceptMetadata
            .getReferences
            .asScala
            .toSet
            .map(Reference.from)

        val rankLevel    = concept.getRankLevel
        val rankName     = concept.getRankName
        val rank: String =
            if rankLevel == null && rankName == null then null
            else if rankLevel == null then rankName
            else rankLevel + rankName

        val author = Option(concept.getPrimaryConceptName.getAuthor)

        ConceptMetadata(
            name,
            alternateNames,
            media,
            linkRealizations,
            Option(rank),
            Option(rankName),
            Option(rankLevel),
            author,
            concept.getAphiaId.asLong,
            references,
            concept.getId.asLong
        )
