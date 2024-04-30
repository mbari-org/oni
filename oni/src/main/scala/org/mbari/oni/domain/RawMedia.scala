/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2024
 *
 * oni code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential. 
 */

package org.mbari.oni.domain

import org.mbari.oni.jpa.entities.MediaEntity

import java.net.URL

case class RawMedia(
    url: URL,
    caption: Option[String] = None,
    credit: Option[String] = None,
    primaryMedia: Option[Boolean] = None,
    `type`: Option[String] = None
):

    def toEntity: MediaEntity =
        val entity = new MediaEntity()
        entity.setCaption(caption.orNull)
        entity.setCredit(credit.orNull)
        entity.setPrimary(primaryMedia.getOrElse(false))
        entity.setType(`type`.orNull)
        entity.setUrl(url.toExternalForm)
        entity
