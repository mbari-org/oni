/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2024
 *
 * oni code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential. 
 */

package org.mbari.oni.domain

import scala.annotation.tailrec
import scala.jdk.CollectionConverters.*

final case class SerdeConcept(
    name: String,
    rank: Option[String] = None,
    alternativeNames: Option[Seq[String]] = None,
    children: Option[Set[SerdeConcept]] = None,
    aphiaId: Option[Long] = None
):

    def containsName(n: String): Boolean = name.equals(n) ||
        alternativeNames.getOrElse(Nil).contains(n)

    lazy val flatten: Seq[SerdeConcept] =
        children match
            case None    => Seq(this)
            case Some(c) => Seq(this) ++ c.flatMap(_.flatten)

object SerdeConcept:

    def from(c: SimpleConcept): SerdeConcept =
        val alternativeNames = if c.alternativeNames.isEmpty then None else Some(c.alternativeNames)
        SerdeConcept(c.name, c.rank, alternativeNames)

    def from(c: Concept): SerdeConcept =
        val alternativeNames = if c.alternativeNames.isEmpty then None else Some(c.alternativeNames)
        val children         = if c.children.isEmpty then None else Some(c.children.map(from).toSet)
        SerdeConcept(c.name, c.rank, alternativeNames, children, c.aphiaId)
