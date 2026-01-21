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
