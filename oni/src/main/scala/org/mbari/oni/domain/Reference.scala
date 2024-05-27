package org.mbari.oni.domain

import org.mbari.oni.jpa.entities.ReferenceEntity

import java.net.URI
import scala.jdk.CollectionConverters.*

case class Reference(
                        reference: String,
                        doi: Option[URI] = None,
                        concepts: Seq[String] = Nil,
                        id: Option[Long] = None)

object Reference:
    def from(entity: ReferenceEntity): Reference =
        var concepts = entity.getConceptMetadatas
            .asScala
            .map(_.getConcept.getPrimaryConceptName.getName)
            .toSeq
        Reference(
            reference = entity.getReference,
            doi = Option(entity.getDoi),
            concepts = concepts,
            id = Option(entity.getId)
        )
