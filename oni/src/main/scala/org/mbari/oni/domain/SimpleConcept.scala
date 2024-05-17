/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2024
 *
 * oni code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential. 
 */

package org.mbari.oni.domain

import org.mbari.oni.jdbc.MutableConcept

case class SimpleConcept(name: String, rank: Option[String], alternativeNames: Seq[String]):

    def containsName(name: String): Boolean =
        this.name == name || alternativeNames.contains(name)

object SimpleConcept:
    def from(c: MutableConcept): SimpleConcept =
        val primaryName = c.primaryName.getOrElse("")
        SimpleConcept(c.primaryName.getOrElse(""), c.rank, c.alternativeNames)
