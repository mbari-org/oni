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
