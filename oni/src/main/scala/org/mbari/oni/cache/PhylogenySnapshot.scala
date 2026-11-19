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

package org.mbari.oni.cache

import org.mbari.oni.jdbc.{ConceptRow, MutableConcept}

import java.time.Instant

/**
 * An immutable view of the whole concept tree, with constant-time lookups by concept id and by any concept name. It's
 * never modified after it's built; lookups that need to reshape the tree (e.g. findUp) work on copies.
 *
 * @param generation
 *   The cache generation it was loaded in. A snapshot from an older generation is stale.
 */
final case class PhylogenySnapshot(
    root: Option[MutableConcept],
    private val nodesById: Map[Long, MutableConcept],
    private val nodesByName: Map[String, MutableConcept],
    generation: Long,
    loadedAt: Instant
):
    def byId(id: Long): Option[MutableConcept]       = nodesById.get(id)
    def byName(name: String): Option[MutableConcept] = nodesByName.get(name)

object PhylogenySnapshot:

    def from(rows: Seq[ConceptRow], generation: Long, loadedAt: Instant = Instant.now()): PhylogenySnapshot =
        val (root, nodes) = MutableConcept.toTree(rows)
        val byId          = nodes.flatMap(n => n.id.map(_ -> n)).toMap
        val byName        = nodes.flatMap(n => n.names.map(_.name -> n)).toMap
        PhylogenySnapshot(root, byId, byName, generation, loadedAt)
