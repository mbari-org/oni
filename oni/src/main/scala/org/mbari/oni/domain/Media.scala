/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2024
 *
 * oni code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential. 
 */

package org.mbari.oni.domain

import java.net.{URI, URL}
import java.util.regex.Pattern
import org.mbari.oni.jpa.entities.MediaEntity

import scala.util.Success
import scala.util.Try

/**
 * @author
 *   Brian Schlining
 * @since 2016-11-17T16:07:00
 */
case class Media(
    url: URL,
    caption: Option[String] = None,
    credit: Option[String] = None,
    mimeType: String = "application/octet-stream",
    isPrimary: Boolean = false,
    conceptName: Option[String] = None,
    id: Option[Long] = None
)

enum MediaType(name: String):
    case Icon  extends MediaType("Icon")
    case Image extends MediaType("Image")
    case Video extends MediaType("Video")

object Media:

    def from(media: MediaEntity): Media =
        val conceptName = Try(media.getConceptMetadata.getConcept.getName).toOption
        Media(
            URI.create(media.getUrl).toURL,
            Option(media.getCaption),
            Option(media.getCredit),
            resolveMimeType(media.getType, media.getUrl),
            media.isPrimary,
            conceptName,
            Option(media.getId)
        )

    def from(namedMedia: NamedMedia): Media =
        Media(
            namedMedia.url,
            Option(namedMedia.caption),
            Option(namedMedia.credit),
            resolveMimeType(namedMedia.mimeType, namedMedia.url.toExternalForm),
            namedMedia.isPrimary,
            Option(namedMedia.name)
        )

    def resolveMimeType(t: String, url: String): String =
        val ext = url.split(Pattern.quote(".")).last
        Try(MediaType.valueOf(t)) match
            case Success(MediaType.Image) => s"image/$ext"
            case Success(MediaType.Video) =>
                ext match
                    case "mov" => "video/quicktime"
                    case _     => s"video/$ext"
            case _                        => "application/octet-stream"
