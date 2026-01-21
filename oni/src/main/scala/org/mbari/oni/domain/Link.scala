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

import org.mbari.oni.etc.jdk.Numbers.*
import org.mbari.oni.jpa.IPersistentObject
import org.mbari.oni.jpa.entities.{LinkRealizationEntity, LinkTemplateEntity}

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
