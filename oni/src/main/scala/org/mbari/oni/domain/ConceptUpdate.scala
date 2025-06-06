/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2024
 *
 * oni code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential.
 */

package org.mbari.oni.domain

case class ConceptUpdate(
    parentName: Option[String] = None,
    rankLevel: Option[String] = None, // Use Some("") to clear the rank level
    rankName: Option[String] = None,  // Use Some("") to clear the rank name
    aphiaId: Option[Long] = None
) {}
