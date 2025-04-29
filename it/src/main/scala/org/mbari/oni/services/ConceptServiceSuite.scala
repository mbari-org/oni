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

import org.mbari.oni.domain.{ConceptCreate, ConceptMetadata, ConceptUpdate, RawConcept}
import org.mbari.oni.jpa.DatabaseFunSuite
import org.mbari.oni.jpa.entities.TestEntityFactory

import scala.concurrent.duration.Duration
import scala.jdk.CollectionConverters.*

trait ConceptServiceSuite extends DatabaseFunSuite with UserAuthMixin:

    lazy val conceptService: ConceptService = new ConceptService(entityManagerFactory)
    lazy val historyService: HistoryService = new HistoryService(entityManagerFactory)

    override val munitTimeout: Duration = Duration(120, "s")

    override def beforeEach(context: BeforeEach): Unit =
        for root <- conceptService.findRoot()
        do conceptService.deleteByName(root.name)

    test("init") {
        val root = TestEntityFactory.buildRoot(3, 3)
        conceptService.init(root) match
            case Left(_)  => fail("Failed to init")
            case Right(e) =>
                assert(root.getId != null)
                assert(e.getId != null)
                assert(!e.getChildConcepts.isEmpty)
                assertEquals(e.getPrimaryConceptName.getName, root.getPrimaryConceptName.getName)
//                println(EntityUtilities.buildTextTree(e))
    }

    test("nonAcidInit") {
        val root = TestEntityFactory.buildRoot(8, 2)
        conceptService.nonAcidInit(root) match
            case Left(_)  => fail("Failed to init")
            case Right(e) =>
                assert(e.children.nonEmpty)
//                println(e.stringify)
    }

//    test("nonAcidInit (full tree)") {
//        val url = getClass.getResource("/kb/kb-dump.json.zip")
//        val path = Paths.get(url.toURI)
//        assert(Files.exists(path))
//        TestRepository.read(path) match
//            case None => fail("Failed to read test data")
//            case Some(rawConcept) =>
//                conceptService.nonAcidInit(rawConcept.toEntity) match
//                    case Left(_) => fail("Failed to init")
//                    case Right(e) =>
//                        assert(e.children.nonEmpty)
//    }

    test("deleteByName") {
        val root = TestEntityFactory.buildRoot(4)
        for
            rootEntity <- conceptService.init(root)
            n          <- conceptService.deleteByName(rootEntity.getPrimaryConceptName.getName)
        do assertEquals(n, 4)
    }

    test("findByName") {
        val root            = TestEntityFactory.buildRoot(4)
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
            assertEquals(found.rankLevel, Option(greatGrandChild.getRankLevel))
            assertEquals(found.rankName, Option(greatGrandChild.getRankName))

    }

    test("findParentByChildName") {
        val root  = TestEntityFactory.buildRoot(2, 0)
        val child = root.getChildConcepts.iterator().next()
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
            val orderedFound    = found.toSeq.sortBy(_.name)
            val orderedExpected = children.asScala.map(ConceptMetadata.from).toSeq.sortBy(_.name)
            assertEquals(orderedFound, orderedExpected)
    }

    test("findRoot") {
        val root = TestEntityFactory.buildRoot(2)
        conceptService.init(root) match
            case Left(_)           => fail("Failed to init")
            case Right(rootEntity) =>
                conceptService.findRoot() match
                    case Left(_)      => fail("Failed to find root")
                    case Right(found) =>
                        assert(rootEntity.getId != null)
                        assertEquals(found, ConceptMetadata.from(rootEntity))
    }

    test("findByGlob") {
        val glob = "XXXX"

        // Insert our search token into a few names
        val root    = TestEntityFactory.buildRoot(4, 0)
        val renamed = root.getChildConcepts.asScala ++ root.getChildConcepts.iterator().next().getChildConcepts.asScala
        renamed.foreach(c =>
            val primary = c.getPrimaryConceptName
            val name    = primary.getName
            val newName = name.substring(0, 5) + glob + name.substring(5 + glob.length)
            primary.setName(newName)
        )

        for
            rootEntity <- conceptService.init(root)
            found      <- conceptService.findByGlob(glob)
        do
            assert(rootEntity.getId != null)
            assert(found.nonEmpty)
            val orderedFound    = found.toSeq.sortBy(_.name)
            val orderedExpected = renamed.map(ConceptMetadata.from).toSeq.sortBy(_.name)
            assertEquals(orderedFound, orderedExpected)

    }

    test("findRawByName (no children)") {
        val root = TestEntityFactory.buildRoot(3, 2)
        for
            rootEntity <- conceptService.init(root)
            found      <- conceptService.findRawByName(root.getPrimaryConceptName.getName, false)
        do
            val expected = RawConcept.from(rootEntity, includeChildren = false)
            assertEquals(found, expected)
    }

    test("findRawByName (with children)") {
        val root = TestEntityFactory.buildRoot(4, 2)
        for
            rootEntity <- conceptService.init(root)
            found      <- conceptService.findRawByName(root.getPrimaryConceptName.getName, true)
        do
            val expected = RawConcept.from(rootEntity, includeChildren = true)
            assertEquals(found, expected)
//            println(found.stringify)
    }

    test("tree") {
        val root = TestEntityFactory.buildRoot(4, 2)
        for
            rootEntity <- conceptService.init(root)
            obtained   <- conceptService.tree()
        do
            assert(rootEntity.getId != null)
            val expected = RawConcept.from(rootEntity)
            assertEquals(obtained, expected)
    }

    test("create root") {

        val attempt =
            runWithUserAuth(user => conceptService.create(ConceptCreate("root", None), user.username))

        attempt match
            case Left(e)     =>
                fail("Failed to create root")
            case Right(root) =>
                assertEquals(root.name, "root")

        conceptService.findRoot() match
            case Left(e)      => fail("Failed to find root")
            case Right(found) => assertEquals(found.name, "root")
    }

    test("create root and child") {

        val attempt = runWithUserAuth(user =>
            for
                root  <- conceptService.create(ConceptCreate("root", None), user.username)
                child <- conceptService.create(ConceptCreate("child", Some(root.name)), user.username)
            yield child
        )

        attempt match
            case Left(e)      =>
                fail("Failed to create root")
            case Right(child) =>
                assertEquals(child.name, "child")
    }

    test("create 2nd root should fail") {

        val attempt = runWithUserAuth(user =>
            for
                root      <- conceptService.create(ConceptCreate("root", None), user.username)
                otherRoot <- conceptService.create(ConceptCreate("anotherroot", None), user.username)
            yield root
        )

        attempt match
            case Left(e)     =>
            case Right(root) =>
                fail("Should not have been able to create a 2nd root")

        conceptService.findRoot() match
            case Left(e)      => fail("Failed to find root")
            case Right(found) => assertEquals(found.name, "root")
    }

    test("update") {

        val root = TestEntityFactory.buildRoot(2, 0)

        val attempt = runWithUserAuth(user =>
            for
                rootEntity   <- conceptService.init(root)
                child        <- Right(rootEntity.getChildConcepts.iterator().next())
                updatedChild <- conceptService.update(
                                    child.getPrimaryConceptName.getName,
                                    ConceptUpdate(
                                        rankLevel = Some("sub"),
                                        rankName = Some("genus"),
                                        aphiaId = Some(1234)
                                    ),
                                    user.username
                                )
            yield updatedChild
        )

        attempt match
            case Left(e)      =>
                fail("Failed to update")
            case Right(child) =>
                assertEquals(child.rank, Some("subgenus"))
                assertEquals(child.aphiaId, Some(1234L))

                historyService.findByConceptName(child.name) match
                    case Left(e)      => fail("Failed to find history")
                    case Right(found) =>
                        assertEquals(found.size, 2)

    }

    test("update parent") {

        val root       = TestEntityFactory.buildRoot(3)
        val grandChild = root
            .getChildConcepts
            .iterator()
            .next()
            .getChildConcepts
            .iterator()
            .next()

        val attempt = runWithUserAuth(user =>
            for
                rootEntity        <- conceptService.init(root)
                grandChildEntity  <- Right(grandChild)
                updatedGrandChild <- conceptService.update(
                                         grandChildEntity.getPrimaryConceptName.getName,
                                         ConceptUpdate(
                                             parentName = Some(rootEntity.getPrimaryConceptName.getName)
                                         ),
                                         user.username
                                     )
            yield updatedGrandChild
        )

        attempt match
            case Left(e)           =>
                fail("Failed to update")
            case Right(grandChild) =>
                conceptService.findParentByChildName(grandChild.name) match
                    case Left(e)      =>
                        fail("Failed to find parent")
                    case Right(found) =>
                        assertEquals(found.name, root.getPrimaryConceptName.getName)
                historyService.findByConceptName(grandChild.name) match
                    case Left(e)      =>
                        fail("Failed to find history")
                    case Right(found) =>
                        assertEquals(found.size, 1)
    }

    test("update (parent multiple times)") {
        val root         = TestEntityFactory.buildRoot(5)
        val grandChild   = root
            .getChildConcepts
            .iterator()
            .next()
            .getChildConcepts
            .iterator()
            .next()
        val childsParent = grandChild.getParentConcept

        val update1 = ConceptUpdate(parentName = Some(root.getName))
        val update2 = ConceptUpdate(parentName = Some(childsParent.getName))

        val attempt = runWithUserAuth(user =>
            for
                rootEntity         <- conceptService.init(root)
                grandChildEntity   <- Right(grandChild)
                updatedGrandChild1 <- conceptService.update(grandChildEntity.getName, update1, user.username)
                updatedGrandChild2 <- conceptService.update(grandChildEntity.getName, update2, user.username)
            yield updatedGrandChild2
        )

        attempt match
            case Left(e)           =>
                fail("Failed to update")
            case Right(grandChild) =>
                conceptService.findParentByChildName(grandChild.name) match
                    case Left(e)      =>
                        fail("Failed to find parent")
                    case Right(found) =>
                        assertEquals(found.name, childsParent.getName)
                historyService.findByConceptName(grandChild.name) match
                    case Left(e)      =>
                        fail("Failed to find history")
                    case Right(found) =>
                        assertEquals(found.size, 2)

    }

    test("update to remove rank name and rank level") {
        val root    = TestEntityFactory.buildRoot(2)
        root.setRankName("genus")
        root.setRankLevel("sub")
        val attempt = runWithUserAuth(user =>
            for
                rootEntity <- conceptService.init(root)
                updated    <- conceptService.update(
                                  root.getPrimaryConceptName.getName,
                                  ConceptUpdate(rankName = Some(""), rankLevel = Some("")),
                                  user.username
                              )
            yield updated
        )

        attempt match
            case Left(e)  =>
                fail("Failed to update")
            case Right(_) =>
                conceptService.findByName(root.getPrimaryConceptName.getName) match
                    case Left(_)                =>
                    case Right(conceptMetadata) =>
                        assertEquals(conceptMetadata.rankName, None)
                        assertEquals(conceptMetadata.rankLevel, None)
    }

    test("update that creates cyclic relation should fail") {
        val root = TestEntityFactory.buildRoot(5)

        val attempt = runWithUserAuth(user =>
            for
                rootEntity     <- conceptService.init(root)
                child           = rootEntity.getChildConcepts.iterator().next()
                greatGrandChild = child.getChildConcepts.iterator().next().getChildConcepts.iterator().next()
                updated        <- conceptService.update(
                                      child.getName,
                                      ConceptUpdate(parentName = Some(greatGrandChild.getName)),
                                      user.username
                                  )
            yield updated
        )

        attempt match
            case Left(e)  => // expected
            case Right(_) => fail("Should not have been able to create a cyclic relation")
    }

    test("delete") {

        val root        = TestEntityFactory.buildRoot(3, 0)
        val childEntity = root.getChildConcepts.iterator().next()
        val childName   = childEntity.getPrimaryConceptName.getName

        val attempt = runWithUserAuth(user =>
            for
                rootEntity <- conceptService.init(root)
                child      <- Right(childEntity)
                n          <- conceptService.delete(child.getPrimaryConceptName.getName, user.username)
            yield n
        )

        attempt match
            case Left(e)  =>
                fail("Failed to delete")
            case Right(n) =>
                assertEquals(n, 2)
                conceptService.findByName(childName) match
                    case Left(_)    =>
                    case Right(opt) => fail("Should not have found child")
                conceptService.findRoot() match
                    case Left(e)      => fail("Failed to find root")
                    case Right(found) =>
                        assertEquals(found.name, root.getPrimaryConceptName.getName)
    }
