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

import org.mbari.oni.jpa.entities.MediaEntity

import java.net.{URI, URL}

case class RawMedia(
    url: URL,
    caption: Option[String] = None,
    credit: Option[String] = None,
    primaryMedia: Option[Boolean] = None,
    `type`: Option[String] = None,
    id: Option[Long] = None
):

    def toEntity: MediaEntity =
        val entity = new MediaEntity()
        entity.setCaption(caption.orNull)
        entity.setCredit(credit.orNull)
        entity.setPrimary(primaryMedia.getOrElse(false))
        entity.setType(`type`.orNull)
        entity.setUrl(url.toExternalForm)
        id.foreach(v => entity.setId(v.longValue()))
        entity

object RawMedia:
    def from(entity: MediaEntity): RawMedia =
        RawMedia(
            url = URI.create(entity.getUrl).toURL,
            caption = Option(entity.getCaption),
            credit = Option(entity.getCredit),
            primaryMedia = Option(entity.isPrimary),
            `type` = Option(entity.getType),
            id = Option(entity.getId)
        )
