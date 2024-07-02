/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2024
 *
 * oni code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential.
 */

package org.mbari.oni.domain

case class ConceptCreate(
    name: String,
    parentName: Option[String] = None,
    rankLevel: Option[String] = None,
    rankName: Option[String] = None,
    aphiaId: Option[Long] = None
)
