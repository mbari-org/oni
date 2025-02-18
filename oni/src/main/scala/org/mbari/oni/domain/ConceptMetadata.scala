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
