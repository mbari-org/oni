/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2024
 *
 * oni code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential. 
 */

package org.mbari.oni.domain

import org.mbari.oni.jpa.entities.ConceptMetadataEntity

case class RawConceptMetadata(
    linkRealizations: Option[Seq[RawLink]] = None,
    linkTemplates: Option[Seq[RawLink]] = None,
    medias: Option[Seq[RawMedia]] = None
):

    def toEntity: ConceptMetadataEntity =
        val entity = new ConceptMetadataEntity()

        val lrs = linkRealizations.map(_.map(_.toLinkRealizationEntity)).getOrElse(Seq.empty)
        lrs.foreach(entity.addLinkRealization)

        val lts = linkTemplates.map(_.map(_.toLinkTemplateEntity)).getOrElse(Seq.empty)
        lts.foreach(entity.addLinkTemplate)

        val ms = medias.map(_.map(_.toEntity)).getOrElse(Seq.empty)
        ms.foreach(entity.addMedia)

        entity
