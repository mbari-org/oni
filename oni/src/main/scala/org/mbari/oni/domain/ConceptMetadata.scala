/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2024
 *
 * oni code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential. 
 */

package org.mbari.oni.domain

import scala.jdk.CollectionConverters.*
import org.mbari.oni.jpa.entities.ConceptEntity

/**
 * @author
 *   Brian Schlining
 * @since 2016-11-17T15:54:00
 */
case class ConceptMetadata(
    name: String,
    alternateNames: Seq[String] = Nil,
    media: Seq[Media] = Nil,
    descriptors: Seq[Link] = Nil,
    rank: Option[String] = None,
    author: Option[String] = None
) {}

object ConceptMetadata:

    def from(concept: ConceptEntity): ConceptMetadata =
        val name = concept.getPrimaryConceptName.getName

        val alternateNames = concept.getConceptNames.asScala.toSeq.map(_.getName).filter(_ != name)

        val media = concept.getConceptMetadata.getMedias.asScala.toSeq.map(Media.from)

        val descriptors = concept.getConceptMetadata.getLinkRealizations.asScala.toSeq.map(Link.from(_))

        val rankLevel    = concept.getRankLevel
        val rankName     = concept.getRankName
        val rank: String =
            if rankLevel == null && rankName == null then null
            else if rankLevel == null then rankName
            else rankLevel + rankName

        val author = Option(concept.getPrimaryConceptName.getAuthor)

        ConceptMetadata(name, alternateNames, media, descriptors, Option(rank), author)
