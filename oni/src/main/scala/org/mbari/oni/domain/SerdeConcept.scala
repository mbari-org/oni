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
