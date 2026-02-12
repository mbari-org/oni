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

import org.mbari.oni.etc.jdk.Strings
import org.mbari.oni.jpa.entities.MediaEntity

import java.net.{URI, URL, URLConnection}
import java.time.Instant
import java.util.regex.Pattern
import scala.util.{Success, Try}

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
    id: Option[Long] = None,
    lastUpdated: Option[Instant] = None
)

enum MediaType(name: String):
    case Icon  extends MediaType("Icon")
    case Image extends MediaType("Image")
    case Video extends MediaType("Video")

object Media:

    def from(media: MediaEntity): Media =
        val conceptName = Try(media.getConceptMetadata.getConcept.getName).toOption
        val escapedUrl  = media.getUrl.replace(" ", "%20")
        Media(
            URI.create(escapedUrl).toURL,
            Option(media.getCaption),
            Option(media.getCredit),
            resolveMimeType(media.getType, media.getUrl),
            media.isPrimary,
            conceptName,
            Option(media.getId),
            Option(media.getLastUpdatedTimestamp)
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
        URLConnection.guessContentTypeFromName(url) match
            case null     =>
                val ext       = url.split(Pattern.quote(".")).last.toLowerCase
                val mediaType = Strings.initCap(t)
                Try(MediaType.valueOf(mediaType)) match
                    case Success(MediaType.Image) =>
                        ext match
                            case "jpg" => "image/jpeg"
                            case _     => s"image/$ext"
                    case Success(MediaType.Video) =>
                        ext match
                            case "mov" => "video/quicktime"
                            case _     => s"video/$ext"
                    case Success(MediaType.Icon)  => "image/x-icon"
                    case _                        => "application/octet-stream"
            case mimeType => mimeType

    def resolveType(url: String): MediaType =
        resolveMimeType("Image", url) match
            case "application/octet-stream" => MediaType.Image
            case mimeType                   =>
                val parts = mimeType.split("/")
                if parts.length == 2 then
                    parts.headOption match
                        case Some("image") =>
                            parts.last match
                                case "x-icon" => MediaType.Icon
                                case _        => MediaType.Image
                        case Some("video") => MediaType.Video
                        case _             => MediaType.Image
                else MediaType.Image
