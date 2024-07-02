/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2024
 *
 * oni code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential.
 */

package org.mbari.oni.domain

import org.mbari.oni.jpa.entities.{LinkRealizationEntity, LinkTemplateEntity}

case class ExtendedLink(
    concept: String,
    linkName: String,
    toConcept: String,
    linkValue: String,
    id: Option[Long] = None
):
    def toLink: Link =
        Link(linkName, toConcept, linkValue)

    def stringValue: String =
        s"$concept${ILink.DELIMITER}$linkName${ILink.DELIMITER}$toConcept${ILink.DELIMITER}$linkValue"

object ExtendedLink:

    def from(link: LinkTemplateEntity | LinkRealizationEntity): ExtendedLink =
        link match
            case l: LinkTemplateEntity    =>
                val concept = l.getConceptMetadata.getConcept.getPrimaryConceptName.getName
                ExtendedLink(concept, l.getLinkName, l.getToConcept, l.getLinkValue, Option(l.getId))
            case l: LinkRealizationEntity =>
                val concept = l.getConceptMetadata.getConcept.getPrimaryConceptName.getName
                ExtendedLink(concept, l.getLinkName, l.getToConcept, l.getLinkValue, Option(l.getId))
