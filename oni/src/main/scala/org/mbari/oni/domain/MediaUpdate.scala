/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2024
 *
 * oni code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential.
 */

package org.mbari.oni.domain

import java.net.URL

case class MediaUpdate(    url: Option[URL] = None,
    caption: Option[String] = None,
    credit: Option[String] = None,
    mediaType: Option[String] = None,
    isPrimary: Option[Boolean] = None) {
}
