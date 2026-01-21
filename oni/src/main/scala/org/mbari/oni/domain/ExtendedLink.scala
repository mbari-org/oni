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
                    Option(l.getLastUpdatedTimestamp)
                )
            case l: LinkRealizationEntity =>
                val concept = l.getConceptMetadata.getConcept.getPrimaryConceptName.getName
                ExtendedLink(
                    concept,
                    l.getLinkName,
                    l.getToConcept,
                    l.getLinkValue,
                    Option(l.getId),
                    Option(l.getLastUpdatedTimestamp)
                )
