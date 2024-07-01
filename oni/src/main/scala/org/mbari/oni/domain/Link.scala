/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2024
 *
 * oni code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential. 
 */

package org.mbari.oni.domain

import org.mbari.oni.domain.ILink
import org.mbari.oni.jpa.IPersistentObject
import org.mbari.oni.jpa.entities.{LinkRealizationEntity, LinkTemplateEntity}

import org.mbari.oni.etc.jdk.Numbers.{*, given}

/**
 * @author
 *   Brian Schlining
 * @since 2016-11-17T16:23:00
 */
case class Link(linkName: String, toConcept: String, linkValue: String, id: Option[Long] = None):

    def toLinkTemplateEntity: LinkTemplateEntity =
        val l = new LinkTemplateEntity()
        l.setLinkName(linkName)
        l.setToConcept(toConcept)
        l.setLinkValue(linkValue)
        l

    def toLinkRealizationEntity: LinkRealizationEntity =
        val l = new LinkRealizationEntity()
        l.setLinkName(linkName)
        l.setToConcept(toConcept)
        l.setLinkValue(linkValue)
        l

object Link:
    def from(link: ILink & IPersistentObject): Link =
        Link(link.getLinkName, link.getToConcept, link.getLinkValue, longConverter(link.getId))
