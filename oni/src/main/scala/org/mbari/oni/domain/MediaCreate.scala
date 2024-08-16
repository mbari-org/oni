/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2024
 *
 * oni code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential.
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
