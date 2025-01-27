/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2024
 *
 * oni code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential.
 */

package org.mbari.oni.domain

final case class Rank(rankLevel: Option[String], rankName: Option[String]):
    def rank: String = s"${rankLevel.getOrElse("")}${rankName.getOrElse("")}".toLowerCase
