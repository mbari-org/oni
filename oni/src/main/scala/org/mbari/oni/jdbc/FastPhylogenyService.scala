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

package org.mbari.oni.jdbc

import org.mbari.oni.cache.KnowledgebaseCache
import org.mbari.oni.domain.{Concept, SimpleConcept}

/**
 * Phylogeny (concept tree) lookups. This is a thin wrapper: the tree itself, its loading and its invalidation all live
 * in the shared [[KnowledgebaseCache]], so every user of this class sees the same, up-to-date tree.
 *
 * @author
 *   Brian Schlining
 * @since 2018-02-11T11:19:00
 */
class FastPhylogenyService(cache: KnowledgebaseCache):

    /** The branch from the named concept up to the root */
    def findUp(name: String): Option[Concept] = cache.findUp(name)

    def findUpById(id: Long): Option[Concept] = cache.findUpById(id)

    /** The named concept and everything below it */
    def findDown(name: String): Option[Concept] = cache.findDown(name)

    def findDownById(id: Long): Option[Concept] = cache.findDownById(id)

    /** The children of the concept's parent, including the concept itself */
    def findSiblings(name: String): Seq[SimpleConcept] = cache.findSiblings(name)

    def findSiblingsById(id: Long): Seq[SimpleConcept] = cache.findSiblingsById(id)

    def findDescendantNames(name: String): Seq[String] = cache.findDescendantNames(name)

    /** For Java callers such as MediaRepository */
    def findDescendantNamesAsJava(name: String): java.util.List[String] = cache.findDescendantNamesAsJava(name)
