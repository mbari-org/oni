/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2024
 *
 * oni code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential.
 */

package org.mbari.oni.domain

import org.mbari.oni.jpa.entities.{LinkRealizationEntity, LinkTemplateEntity}
import java.time.Instant

case class ExtendedLink(
    concept: String,
    linkName: String,
    toConcept: String,
    linkValue: String,
    id: Option[Long] = None,
    lastUpdated: Option[Instant] = None
):
    def toLink: Link =
        Link(linkName, toConcept, linkValue, id)

    val shortStringValue: String =
        s"$linkName${ILink.DELIMITER}$toConcept${ILink.DELIMITER}$linkValue"

    val stringValue: String =
        s"$concept${ILink.DELIMITER}$linkName${ILink.DELIMITER}$toConcept${ILink.DELIMITER}$linkValue"

object ExtendedLink:

    def from(link: LinkTemplateEntity | LinkRealizationEntity): ExtendedLink =
        link match
            case l: LinkTemplateEntity    =>
                val concept = l.getConceptMetadata.getConcept.getPrimaryConceptName.getName
                ExtendedLink(
                    concept,
                    l.getLinkName,
                    l.getToConcept,
                    l.getLinkValue,
                    Option(l.getId),
                    Option(l.getLastUpdatedTimestamp).map(_.toInstant)
                )
            case l: LinkRealizationEntity =>
                val concept = l.getConceptMetadata.getConcept.getPrimaryConceptName.getName
                ExtendedLink(
                    concept,
                    l.getLinkName,
                    l.getToConcept,
                    l.getLinkValue,
                    Option(l.getId),
                    Option(l.getLastUpdatedTimestamp).map(_.toInstant)
                )
