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
            lastUpdated = Option(entity.getLastUpdatedTimestamp)
        )
