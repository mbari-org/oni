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

import org.mbari.oni.domain.{ConceptMetadata, RawConcept}
import org.mbari.oni.jpa.DatabaseFunSuite
import org.mbari.oni.jpa.entities.{EntityUtilities, TestEntityFactory}
import org.mbari.oni.etc.circe.CirceCodecs.{*, given}

import scala.jdk.CollectionConverters.*

trait ConceptServiceSuite extends DatabaseFunSuite:

    lazy val conceptService: ConceptService = new ConceptService(entityManagerFactory)

    override def beforeEach(context: BeforeEach): Unit =
        for root <- conceptService.findRoot()
        do conceptService.deleteByName(root.name)

    test("init") {
        val root = TestEntityFactory.buildRoot(3, 3)
        conceptService.init(root) match
            case Left(_)  => fail("Failed to init")
            case Right(e) =>
                assert(root.getId != null)
                println(EntityUtilities.buildTextTree(e))
    }

    test("nonAcidInit") {
        val root = TestEntityFactory.buildRoot(4, 2)
        conceptService.nonAcidInit(root) match
            case Left(_)  => fail("Failed to init")
            case Right(e) =>
                assert(e.children.nonEmpty)
                println(e.stringify)
    }

    test("deleteByName") {
        val root = TestEntityFactory.buildRoot(4, 0)
        for
            rootEntity <- conceptService.init(root)
            n          <- conceptService.deleteByName(rootEntity.getPrimaryConceptName.getName)
        do assertEquals(n, 4)
    }

    test("findByName") {
        val root            = TestEntityFactory.buildRoot(4, 0)
        val greatGrandChild = root
            .getChildConcepts
            .iterator()
            .next()
            .getChildConcepts
            .iterator()
            .next()
            .getChildConcepts
            .iterator()
            .next()
        for
            rootEntity <- conceptService.init(root)
            found      <- conceptService.findByName(greatGrandChild.getPrimaryConceptName.getName)
        do
            assert(rootEntity.getId != null)
            assertEquals(found, ConceptMetadata.from(greatGrandChild))

    }

    test("findParentByChildName") {
        val root  = TestEntityFactory.buildRoot(2, 0)
        val child = root.getChildConcepts.get(0)
        for
            rootEntity <- conceptService.init(root)
            found      <- conceptService.findParentByChildName(child.getPrimaryConceptName.getName)
        do
            assert(rootEntity.getId != null)
            assertEquals(found, ConceptMetadata.from(rootEntity))
    }

    test("findChildrenByParentName") {
        val root     = TestEntityFactory.buildRoot(2, 3)
        val children = root.getChildConcepts
        for
            rootEntity <- conceptService.init(root)
            found      <- conceptService.findChildrenByParentName(root.getPrimaryConceptName.getName)
        do
            assert(rootEntity.getId != null)
            assert(found.nonEmpty)
            val orderedFound    = found.sortBy(_.name)
            val orderedExpected = children.asScala.map(ConceptMetadata.from).toSeq.sortBy(_.name).toSeq
            assertEquals(orderedFound, orderedExpected)
    }

    test("findRoot") {
        val root = TestEntityFactory.buildRoot(2, 0)
        for
            rootEntity <- conceptService.init(root)
            found      <- conceptService.findRoot()
        do
            assert(rootEntity.getId != null)
            assertEquals(found, ConceptMetadata.from(rootEntity))
    }

    test("findByGlob") {
        val glob = "XXX"

        // Insert our search token into a few names
        val root    = TestEntityFactory.buildRoot(4, 2)
        val renamed = root.getChildConcepts.asScala ++ root.getChildConcepts.iterator().next().getChildConcepts.asScala
        renamed.foreach(c =>
            val name    = c.getPrimaryConceptName.getName
            val newName = name.substring(0, 5) + glob + name.substring(5 + glob.length)
            c.getPrimaryConceptName.setName(name)
        )

        for
            rootEntity <- conceptService.init(root)
            found      <- conceptService.findByGlob(glob)
        do
            assert(rootEntity.getId != null)
            assert(found.nonEmpty)
            val orderedFound    = found.sortBy(_.name)
            val orderedExpected = renamed.map(ConceptMetadata.from).sortBy(_.name).toSeq
            assertEquals(orderedFound, orderedExpected)

    }

    test("tree") {
        val root = TestEntityFactory.buildRoot(4, 2)
        for
            rootEntity <- conceptService.init(root)
            obtained   <- conceptService.tree()
        do
            assert(rootEntity.getId != null)
            val expected = RawConcept.fromEntity(rootEntity)
            assertEquals(obtained, expected)
    }
