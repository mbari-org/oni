/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2024
 *
 * oni code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential.
 */

package org.mbari.oni.domain

import org.mbari.oni.jpa.entities.{ConceptEntity, HistoryEntity}

import java.time.Instant
import scala.jdk.CollectionConverters.*

case class ExtendedHistory(
    concept: String,
    creationTimestamp: Instant,
    creatorName: String,
    action: String,
    field: String,
    oldValue: Option[String] = None,
    newValue: Option[String] = None,
    approved: Boolean = false,
    processedTimestamp: Option[Instant] = None,
    processorName: Option[String] = None,
    id: Option[Long] = None
)

object ExtendedHistory:
    def from(concept: String, entity: HistoryEntity): ExtendedHistory =
        ExtendedHistory(
            concept,
            entity.getCreationDate.toInstant,
            entity.getCreatorName,
            entity.getAction,
            entity.getField,
            Option(entity.getOldValue),
            Option(entity.getNewValue),
            entity.isApproved,
            Option(entity.getProcessedDate).map(_.toInstant),
            Option(entity.getProcessorName),
            Option(entity.getId)
        )

    def from(concept: ConceptEntity): Set[ExtendedHistory] =
        val name = concept.getName
        concept
            .getConceptMetadata
            .getHistories
            .asScala
            .map(h => from(name, h))
            .toSet
