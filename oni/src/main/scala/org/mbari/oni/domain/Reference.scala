/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2024
 *
 * oni code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential.
 */

package org.mbari.oni.domain

import org.mbari.oni.jpa.entities.ReferenceEntity

import java.net.URI
import java.time.Instant
import scala.jdk.CollectionConverters.*

case class Reference(
    citation: String,
    doi: Option[URI] = None,
    concepts: Seq[String] = Nil,
    id: Option[Long] = None,
    lastUpdated: Option[Instant] = None
):

    def toEntity: ReferenceEntity =
        val entity = new ReferenceEntity
        entity.setCitation(citation)
        doi.foreach(entity.setDoi)
        id.foreach(v => entity.setId(v.longValue()))
        entity

object Reference:
    def from(entity: ReferenceEntity): Reference =
        val concepts = entity
            .getConceptMetadatas
            .asScala
            .map(_.getConcept.getPrimaryConceptName.getName)
            .toSeq

        Reference(
            citation = entity.getCitation,
            doi = Option(entity.getDoi),
            concepts = concepts,
            id = Option(entity.getId),
            lastUpdated = Option(entity.getLastUpdatedTimestamp).map(_.toInstant)
        )
