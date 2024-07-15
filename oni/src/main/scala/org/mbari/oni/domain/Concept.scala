/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2024
 *
 * oni code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential.
 */

package org.mbari.oni.domain

import org.mbari.oni.jpa.entities.ConceptEntity
import org.mbari.oni.etc.jdk.Numbers.*
import scala.jdk.CollectionConverters.*

/**
  * A node in the knoweldgebase tree
  *
  * @param name The primary, or accepted, name of the concept
  * @param rank The pyhologenetic rank
  * @param alternativeNames Synonyms, common names, or former names
  * @param children Child concepts of this concept
  * @param aphiaId The WoRMS AphiaID
  * @param id The database id
  */
case class Concept(
    name: String,
    rank: Option[String] = None,
    alternativeNames: Seq[String] = Nil,
    children: Seq[Concept] = Nil,
    aphiaId: Option[Long] = None,
    id: Option[Long] = None
):

    /**
      * Checks is this concept uses the provided name
      *
      * @param n The name to check
      * @return true if this concept's name or alternative names contain this
      *         name. false if it does not.
      */
    def containsName(n: String): Boolean = name.equals(n) ||
        alternativeNames.contains(n)

    /**
      * All names applicable to this concept. THe first name will
      * be the primary name.
      */
    lazy val names: Seq[String] = name +: alternativeNames

    /**
      * A sorted list all names applicable to this concept and 
      * all of it's descendants.
      */
    lazy val descendantNames: Seq[String] = descendants
        .flatMap(_.names)
        .toSeq
        .sorted

    /**
      * A list of all of this concepts descendants
      */
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
            c.getAphiaId.asLong,
            c.getId.asLong
        )
