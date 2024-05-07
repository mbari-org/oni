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

package org.mbari.oni.jpa

import org.mbari.oni.jpa.entities.{ConceptEntity, TestEntityFactory}
import org.mbari.oni.services.ConceptService
import org.mbari.oni.etc.jdk.Loggers.given

import java.util.concurrent.atomic.AtomicReference

trait DataInitializer extends DatabaseFunSuite:

    private val log = System.getLogger(getClass.getName)

    lazy val conceptService: ConceptService        = new ConceptService(entityManagerFactory)
    val atomicRoot: AtomicReference[ConceptEntity] = new AtomicReference[ConceptEntity]()

    override def beforeEach(context: BeforeEach): Unit =
        val root = TestEntityFactory.buildRoot(5, 1)
        conceptService.init(root) match
            case Right(entity) =>
                atomicRoot.set(entity)
            case Left(error)   =>
                log.atError.withCause(error).log("Failed to initialize test data")

    override def afterEach(context: AfterEach): Unit =
        val root = atomicRoot.get()
        conceptService.deleteByName(root.getPrimaryConceptName.getName) match
            case Right(_)    =>
                log.atInfo.log("Deleted test data")
            case Left(error) =>
                log.atError.withCause(error).log("Failed to delete test data")
