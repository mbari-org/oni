/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2024
 *
 * oni code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential.
 */

package org.mbari.oni.domain

import org.mbari.oni.jpa.entities.{LinkRealizationEntity, LinkTemplateEntity}

case class RawLink(linkName: String, toConcept: String, linkValue: String):

    def toLinkRealizationEntity: LinkRealizationEntity =
        LinkRealizationEntity(linkName, toConcept, linkValue)

    def toLinkTemplateEntity: LinkTemplateEntity =
        LinkTemplateEntity(linkName, toConcept, linkValue)

object RawLink:
    def from(entity: ILink): RawLink =
        RawLink(entity.getLinkName, entity.getToConcept, entity.getLinkValue)
