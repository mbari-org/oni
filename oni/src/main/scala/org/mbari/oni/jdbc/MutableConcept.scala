/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2024
 *
 * oni code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential. 
 */

package org.mbari.oni.jdbc

import org.mbari.oni.domain.Concept

import scala.collection.mutable


class MutableConcept {
    var id: Option[Long] = None
    var parent: Option[MutableConcept] = None
    var rank: Option[String] = None
    var names: Seq[CName] = Nil
    var children: Seq[MutableConcept] = Nil

    def primaryName: Option[String] =
        names.find(n => n.nameType.equals("primary"))
            .map(_.name)

    def copyUp(): MutableConcept = copyUp(Nil)

    private def copyUp(newChildren: Seq[MutableConcept]): MutableConcept = {
        val mc = new MutableConcept
        mc.id = id
        mc.rank = rank
        mc.names = names
        mc.children = newChildren
        mc.parent = parent.map(_.copyUp(Seq(mc)))
        mc
    }

    def toImmutable: Concept = {
        //println(s"${this.id} - ${this.names}")
        val primaryName = names.find(_.nameType.equalsIgnoreCase("primary"))
            .getOrElse(names.head)
        val alternativeNames = names.filter(!_.eq(primaryName))
        Concept(
            primaryName.name,
            rank,
            alternativeNames.map(_.name),
            children.map(_.toImmutable)
        )
    }

    def root(): MutableConcept = parent match {
        case None => this
        case Some(p) => p.root()
    }

}

object MutableConcept {

    def newParent(parentId: Long): MutableConcept = {
        val mc = new MutableConcept
        mc.id = Some(parentId)
        mc
    }

    // TODO return the nodes too!!
    def toTree(rows: Seq[ConceptRow]): (Option[MutableConcept], Seq[MutableConcept]) = {
        val nodes = new mutable.ArrayBuffer[MutableConcept]
        for (row <- rows) {

            /*
              Find an existing parent or create one as needed
             */
            val parentOpt = row.parentId.map(parentId =>
                nodes.find(_.id.getOrElse(-1L) == parentId)
                    .getOrElse({
                        val mc = newParent(parentId)
                        nodes += mc
                        mc
                    }))

            if (parentOpt.isEmpty) {
                System.getLogger(getClass.getName)
                    .log(System.Logger.Level.INFO, s"No Parent found for $row")
            }

            /*
              Find the existing concept or create one if needed
             */
            val concept = nodes.find(_.id.getOrElse(-1L) == row.id) match {
                case None =>
                    val mc = new MutableConcept
                    mc.id = Some(row.id)
                    nodes += mc
                    mc
                case Some(mc) => mc
            }

            // Set the parent of the concept!!
            parentOpt.foreach(parent => {
                concept.parent = parentOpt
                if (!parent.children.contains(concept)) {
                    parent.children = parent.children :+ concept
                }
            })

            val cn = CName(row.name, row.nameType)
            concept.rank = row.rank
            concept.names = concept.names :+ cn

        }
        val root = nodes.find(_.parent.isEmpty)
        (root, nodes.toSeq)
    }

}






 
