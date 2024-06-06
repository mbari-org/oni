/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2024
 *
 * oni code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential. 
 */

package org.mbari.oni.domain

import org.mbari.oni.jpa.entities.ConceptEntity
import scala.jdk.CollectionConverters.*

case class Concept(
    name: String,
    rank: Option[String] = None,
    alternativeNames: Seq[String] = Nil,
    children: Seq[Concept] = Nil,
    aphiaId: Option[Long] = None
):
    def containsName(n: String): Boolean = name.equals(n) ||
        alternativeNames.contains(n)

    lazy val names: Seq[String] = name +: alternativeNames

    lazy val descendantNames: Seq[String] = descendants
        .flatMap(_.names)
        .toSeq
        .sorted

    lazy val descendants: Set[Concept] = children.toSet.flatMap(_.descendants) + this

    lazy val flatten: Seq[Concept] = Seq(this) ++ children.flatMap(_.flatten)

object Concept:

    def from(c: ConceptEntity): Concept =
        val alternativeNames = c
            .getAlternativeConceptNames
            .asScala
            .map(_.getName)
            .toSeq
        Concept(
            c.getPrimaryConceptName.getName,
            Option(c.getRank),
            alternativeNames,
            c.getChildConcepts.asScala.map(from).toSeq.sortBy(_.name),
            Option(c.getAphiaId).map(_.longValue())
        )
