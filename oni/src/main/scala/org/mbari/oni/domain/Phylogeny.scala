/*
 * Copyright 2024 Monterey Bay Aquarium Research Institute
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.mbari.oni.domain

import scala.collection.mutable

/**
 * Represents a node in a phylogeny tree. It maps parent-child relationships.
 *
 * Note that this is an abomination of case class usage, but it works.
 *
 * @author
 *   Brian Schlining
 * @since 2016-11-17T13:41:00
 */
case class Phylogeny(
    name: String,
    rank: Option[String] = None,
    parent: Option[Phylogeny] = None,
    children: mutable.HashSet[Phylogeny] = new mutable.HashSet[Phylogeny]
):

    require(name != null, "Name can not be null")

    override def equals(obj: Any): Boolean = name == obj

    override def hashCode(): Int = name.hashCode

    override def toString: String = getClass.getSimpleName + "=" + name

    lazy val root: Phylogeny = parent match
        case None    => this
        case Some(p) => p.root

    /**
     * Extracts a node from a tree of nodes with a matching name. The ndoe is trimmed so that it's parent is null
     * @param name
     *   The name to find
     * @return
     *   A matching node
     */
    def subnode(name: String): Option[Phylogeny] =
        if this.name.equalsIgnoreCase(name) then Some(this)
        else children.to(LazyList).flatMap(n => n.subnode(name)).headOption
