/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2024
 *
 * oni code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential. 
 */

package org.mbari.oni.domain

import org.mbari.oni.jdbc.{ImmutableConcept, SimpleConcept}

import scala.annotation.tailrec
import scala.jdk.CollectionConverters.*

final case class Concept(
    name: String,
    alternativeNames: Option[Seq[String]] = None,
    rank: Option[String] = None,
    children: Option[Seq[Concept]] = None
)

object Concept:

    def from(c: ImmutableConcept): Concept =
        val alternativeNames = if c.alternativeNames.isEmpty then None else Some(c.alternativeNames.asScala.toSeq)
        val children         = if c.children.isEmpty then None else Some(c.children.asScala.map(from).toSeq)
        Concept(c.name, alternativeNames, Option(c.rank), children)

    def from(c: SimpleConcept): Concept =
        val alternativeNames = if c.alternativeNames.isEmpty then None else Some(c.alternativeNames.asScala.toSeq)
        Concept(c.name, alternativeNames, Option(c.rank), None)

    def flatten(c: ImmutableConcept): Seq[Concept] =
        @tailrec
        def builder(node: ImmutableConcept, stack: List[Concept] = Nil): Seq[Concept] =
            val next = Concept(node.name, rank = Option(node.rank)) :: stack
            node.children.asScala.headOption match
                case None        => next
                case Some(child) => builder(child, next)
        builder(c).reverse
