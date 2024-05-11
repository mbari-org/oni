/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2024
 *
 * oni code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential. 
 */

package org.mbari.oni.domain

case class Concept(
                      name: String,
                      rank: Option[String],
                      alternativeNames: Seq[String],
                      children: Seq[Concept]
                  ) {
    def containsName(n: String): Boolean = name.equals(n) ||
        alternativeNames.contains(n)

    lazy val names: Seq[String] = name +: alternativeNames

    lazy val descendantNames: Seq[String] = descendants
        .flatMap(_.names)
        .toSeq
        .sorted

    lazy val descendants: Set[Concept] = children.toSet.flatMap(_.descendants) + this

}


