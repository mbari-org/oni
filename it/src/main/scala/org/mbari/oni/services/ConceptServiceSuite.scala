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

import jakarta.persistence.EntityManagerFactory
import org.mbari.oni.jpa.DatabaseFunSuite
import org.mbari.oni.jpa.entities.{EntityUtilities, TestEntityFactory}

trait ConceptServiceSuite extends DatabaseFunSuite:

    lazy val conceptService: ConceptService = new ConceptService(entityManagerFactory)

//  override def beforeEach(context: BeforeEach): Unit =
//    conceptService.

    test("init") {
        val root = TestEntityFactory.buildRoot(3, 3)
        conceptService.init(root) match
            case Left(_)  => fail("Failed to init")
            case Right(e) =>
                assert(root.getId != null)
                println(EntityUtilities.buildTextTree(e))
    }
