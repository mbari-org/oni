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

import org.mbari.oni.jpa.entities.ConceptMetadataEntity

import scala.jdk.CollectionConverters.*

case class RawConceptMetadata(
    linkRealizations: Option[Seq[RawLink]] = None,
    linkTemplates: Option[Seq[RawLink]] = None,
    media: Option[Seq[RawMedia]] = None,
    id: Option[Long] = None
):

    def toEntity: ConceptMetadataEntity =
        val entity = new ConceptMetadataEntity()

        val lrs = linkRealizations.map(_.map(_.toLinkRealizationEntity)).getOrElse(Seq.empty)
        lrs.foreach(entity.addLinkRealization)

        val lts = linkTemplates.map(_.map(_.toLinkTemplateEntity)).getOrElse(Seq.empty)
        lts.foreach(entity.addLinkTemplate)

        val ms = media.map(_.map(_.toEntity)).getOrElse(Seq.empty)

        ms.foreach(entity.addMedia)

        entity

object RawConceptMetadata:
    def from(entity: ConceptMetadataEntity): RawConceptMetadata =
        RawConceptMetadata(
            linkRealizations = Some(
                entity
                    .getLinkRealizations
                    .asScala
                    .map(RawLink.from)
                    .toSeq
                    .sortBy(_.toString)
            ), // HACK
            linkTemplates = Some(
                entity
                    .getLinkTemplates
                    .asScala
                    .map(RawLink.from)
                    .toSeq
                    .sortBy(_.toString)
            ), // HACK
            media = Some(
                entity
                    .getMedias
                    .asScala
                    .map(RawMedia.from)
                    .toSeq
                    .sortBy(_.url.toExternalForm)
            ),
            id = Some(entity.getId)
        )
