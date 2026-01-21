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

import java.net.URL

case class MediaCreate(
    conceptName: String,
    url: URL,
    caption: Option[String] = None,
    credit: Option[String] = None,
    mediaType: Option[String] = Some(MediaTypes.IMAGE.name),
    isPrimary: Option[Boolean] = None
):

    def toEntity: MediaEntity =
        val entity = new MediaEntity()
        entity.setUrl(url.toExternalForm)
        entity.setCaption(caption.orNull)
        entity.setCredit(credit.orNull)
        entity.setType(mediaType.getOrElse(MediaTypes.IMAGE.name))
        entity.setPrimary(isPrimary.getOrElse(false))
        entity
