/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2024
 *
 * oni code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential. 
 */

package org.mbari.oni.domain

import org.mbari.oni.jpa.entities.{LinkRealizationEntity, LinkTemplateEntity}

case class LinkUpdate(
    linkName: Option[String] = None,
    toConcept: Option[String] = None,
    linkValue: Option[String] = None
):

    def updateEntity(entity: LinkRealizationEntity): LinkRealizationEntity =
        linkName.foreach(entity.setLinkName)
        toConcept.foreach(entity.setToConcept)
        linkValue.foreach(entity.setLinkValue)
        entity

    def updateEntity(entity: LinkTemplateEntity): LinkTemplateEntity =
        linkName.foreach(entity.setLinkName)
        toConcept.foreach(entity.setToConcept)
        linkValue.foreach(entity.setLinkValue)
        entity
