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

package org.mbari.oni.services

import org.mbari.oni.domain.RawConcept
import org.mbari.oni.jpa.entities.ConceptEntity
import org.mbari.oni.jpa.{DataInitializer, DatabaseFunSuite}
import org.mbari.oni.jpa.EntityManagerFactories.*

import scala.jdk.CollectionConverters.*

trait ConceptNameServiceSuite extends DataInitializer:

    lazy val conceptNameService: ConceptNameService = new ConceptNameService(entityManagerFactory)

    test("findAllNames") {
        val root     = atomicRoot.get()
        assert(root != null)
        val rawRoot  = RawConcept.fromEntity(root)
        val expected = rawRoot.descendantNames.toSeq.sorted
        conceptNameService.findAllNames() match
            case Right(names) =>
                val obtained = names.sorted
                assertEquals(obtained, expected)
            case Left(error)  =>
                fail(error.toString)

    }