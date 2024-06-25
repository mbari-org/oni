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
import org.mbari.oni.jdbc.FastPhylogenyService

trait DataInitializer extends DatabaseFunSuite:

    protected val log: System.Logger = System.getLogger(getClass.getName)

    lazy val conceptService: ConceptService = new ConceptService(entityManagerFactory)

    def init(depth: Int, breadth: Int): ConceptEntity =
        val root = TestEntityFactory.buildRoot(depth, breadth)
        conceptService.init(root) match
            case Right(entity) => entity
            case Left(error)   =>
                log.atError.withCause(error).log("Failed to initialize test data")
                throw error

    def initShallowTree(numChildren: Int): ConceptEntity =
        val root = TestEntityFactory.buildShallowTree(numChildren)
        conceptService.init(root) match
            case Right(entity) => entity
            case Left(error)   =>
                log.atError.withCause(error).log("Failed to initialize test data")
                throw error

    override def beforeEach(context: BeforeEach): Unit =
        for
            root <- conceptService.findRoot()
            _    <- Some(log.atDebug.log(s"Deleting root concept: ${root.name}"))
        do conceptService.deleteByName(root.name)
