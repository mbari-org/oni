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

import org.mbari.oni.domain.{ConceptNameCreate, ConceptNameTypes, ConceptNameUpdate, RawConcept}
import org.mbari.oni.etc.jdk.Strings
import org.mbari.oni.jpa.DataInitializer

trait ConceptNameServiceSuite extends DataInitializer with UserAuthMixin:

    lazy val conceptNameService: ConceptNameService = new ConceptNameService(entityManagerFactory)
    lazy val historyService: HistoryService         = new HistoryService(entityManagerFactory)

//    override def beforeEach(context: BeforeEach): Unit =
//        for root <- conceptService.findRoot()
//        do conceptService.deleteByName(root.name)

    test("findAllNames") {
        val root     = init(3, 3)
        assert(root != null)
        val rawRoot  = RawConcept.from(root)
        val expected = rawRoot.descendantNames
        conceptNameService.findAllNames(expected.size, 0) match
            case Right(names) =>
                val obtained = names.sorted
                assertEquals(obtained, expected)
            case Left(error)  =>
                fail(error.toString)

    }

    test("findByName") {
        val root    = init(3, 3)
        assert(root != null)
        val rawRoot = RawConcept.from(root)
        val name    = rawRoot.primaryName
        conceptNameService.findByName(name) match
            case Right(rawConcept) =>
                val obtained = rawConcept.names.map(_.name).toSeq
                assert(obtained.contains(name))
            case Left(error)       =>
                fail(error.toString)
    }

    test("addName (new primary name)") {
        val root    = init(3, 3)
        assert(root != null)
        val rawRoot = RawConcept.from(root)
        val name    = rawRoot.primaryName
        val dto     = ConceptNameCreate(name = name, newName = "newName", nameType = ConceptNameTypes.PRIMARY.getType)

        val attempt = runWithUserAuth(user => conceptNameService.addName(dto, user.username))

        attempt match
            case Right(rawConcept) =>
                val obtained = rawConcept.names.map(_.name).toSeq
                assert(obtained.contains(dto.name))
                assert(obtained.contains(dto.newName))
            case Left(error)       =>
                fail(error.toString)
    }

    test("addName (not a primary name)") {

        val root    = init(3, 3)
        assert(root != null)
        val rawRoot = RawConcept.from(root)
        val name    = rawRoot.primaryName
        val dto     =
            ConceptNameCreate(name = name, newName = Strings.random(12), nameType = ConceptNameTypes.SYNONYM.getType)

        val attempt = runWithUserAuth(user => conceptNameService.addName(dto, user.username))

        attempt match
            case Right(rawConcept) =>
                val obtained = rawConcept.names.map(_.name).toSeq
                assert(obtained.contains(dto.name))
                assert(obtained.contains(dto.newName))
            case Left(error)       =>
                fail(error.toString)

        conceptNameService.findByName(dto.newName) match
            case Right(rawConcept) =>
                val obtained = rawConcept.names.map(_.name).toSeq
                assert(obtained.contains(dto.newName))
            case Left(error)       =>
                fail(error.toString)
    }

    test("updateName (primary name)") {

        val root    = init(3, 3)
        assert(root != null)
        val rawRoot = RawConcept.from(root)
        val name    = rawRoot.primaryName

        var newName = Strings.random(12)
        val dto     =
            ConceptNameUpdate(
                newName = Some(newName),
                nameType = Some(ConceptNameTypes.PRIMARY.getType),
                author = Some(Strings.random(5))
            )

        // println(s"Updating name: $name with dto: $dto")

        val attempt = runWithUserAuth(user => conceptNameService.updateName(name, dto, user.username))

        attempt match
            case Right(rawConcept) =>
//                println(rawConcept.stringify)
                val obtained       = rawConcept.names.map(_.name).toSeq
                assert(!obtained.contains(name))
                assert(obtained.contains(dto.newName.getOrElse("")))
                val updatedNameOpt = rawConcept.names.find(_.name == dto.newName.getOrElse(""))
                assert(updatedNameOpt.isDefined)
                val updatedName    = updatedNameOpt.get
                assertEquals(updatedName.nameType, ConceptNameTypes.PRIMARY.getType)
                assertEquals(updatedName.author, dto.author)
            case Left(error)       =>
                fail(error.toString)

        conceptNameService.findByName(newName) match
            case Right(rawConcept) =>
                val obtained = rawConcept.names.map(_.name).toSeq
                // println(s"Obtained names: ${obtained.mkString(", ")}")
                assert(obtained.contains(newName))
            case Left(error)       =>
                fail(error.toString)
    }

    test("updateName with blank author - changes author to null in the database") {

        val root    = init(3, 3)
        assert(root != null)
        val rawRoot = RawConcept.from(root)
        val name    = rawRoot.primaryName
        val dto     =
            ConceptNameUpdate(author = Some(""))

        val attempt = runWithUserAuth(user => conceptNameService.updateName(name, dto, user.username))

        attempt match
            case Right(rawConcept) =>
                val updatedNameOpt = rawConcept.names.find(_.name == name)
                assert(updatedNameOpt.isDefined)
                val updatedName    = updatedNameOpt.get
                assertEquals(updatedName.author, dto.author)
            case Left(error)       =>
                fail(error.toString)
    }

    test("updateName (attempt to change primary to non-primary)") {

        val root    = init(3, 3)
        assert(root != null)
        val rawRoot = RawConcept.from(root)
        val name    = rawRoot.primaryName
        val dto     =
            ConceptNameUpdate(newName = Some("newName"), nameType = Some(ConceptNameTypes.COMMON.getType))

        val attempt = runWithUserAuth(user => conceptNameService.updateName(name, dto, user.username))

        attempt match
            case Right(rawConcept) =>
//                println(rawConcept.stringify)
                fail("Should have thrown an exception")
            case Left(error)       => ()
    }

    test("deleteName (attempt to delete primary name)") {
        // TODO add user account
        val root    = init(3, 3)
        assert(root != null)
        val rawRoot = RawConcept.from(root)
        val name    = rawRoot.primaryName
        runWithUserAuth(user => conceptNameService.deleteName(name, user.username)) match
            case Right(rawConcept) =>
                fail("Should have thrown an exception")
            case Left(error)       => ()
    }

    test("deleteName") {
        val root         = init(3, 4)
        assert(root != null)
        val rawRoot      = RawConcept.from(root)
        val nameToDelete = rawRoot
            .names
            .find(_.nameType != ConceptNameTypes.PRIMARY.getType)
            .map(_.name)
            .getOrElse("")
        runWithUserAuth(user => conceptNameService.deleteName(nameToDelete, user.username)) match
            case Right(rawConcept) =>
                assert(!rawConcept.names.map(_.name).toSeq.contains(nameToDelete))
                conceptService.findByName(nameToDelete) match
                    case Right(_) => fail("Should have been deleted")
                    case Left(e)  => ()
            case Left(error)       =>
                fail(error.toString)

    }

    // --- By id

    private def nameIds(rawConcept: RawConcept): (Long, Long) =
        val primary = rawConcept.names.find(_.nameType == ConceptNameTypes.PRIMARY.getType).flatMap(_.id)
        val other   = rawConcept.names.find(_.nameType != ConceptNameTypes.PRIMARY.getType).flatMap(_.id)
        assert(primary.isDefined && other.isDefined, s"Expected primary and non-primary name ids in $rawConcept")
        (primary.get, other.get)

    test("findById") {
        val rawRoot        = RawConcept.from(init(3, 3))
        val (primaryId, _) = nameIds(rawRoot)
        conceptNameService.findById(primaryId) match
            case Right(rawConcept) =>
                assertEquals(rawConcept.id, rawRoot.id)
                assert(rawConcept.names.flatMap(_.id).contains(primaryId))
            case Left(error)       => fail(error.toString)
    }

    test("findById (no match)") {
        init(2, 0)
        conceptNameService.findById(Long.MaxValue) match
            case Left(e)  => assert(e.isInstanceOf[org.mbari.oni.ConceptNameIdNotFound], e.toString)
            case Right(_) => fail("Should not have found a concept name")
    }

    test("updateNameById") {
        val rawRoot        = RawConcept.from(init(3, 3))
        val (primaryId, _) = nameIds(rawRoot)
        val oldName        = rawRoot.primaryName
        val newName        = Strings.random(12)
        val dto            = ConceptNameUpdate(newName = Some(newName), author = Some(Strings.random(5)))

        runWithUserAuth(user => conceptNameService.updateNameById(primaryId, dto, user.username)) match
            case Right(rawConcept) =>
                val names   = rawConcept.names.map(_.name)
                assert(!names.contains(oldName))
                assert(names.contains(newName))
                val updated = rawConcept.names.find(_.name == newName).get
                assertEquals(updated.id, Some(primaryId)) // same row, renamed
                assertEquals(updated.author, dto.author)
            case Left(error)       => fail(error.toString)

        // History should record the old -> new name, just like updateName
        historyService.findByConceptName(newName) match
            case Right(histories) =>
                assert(histories.exists(h => h.oldValue.contains(oldName) && h.newValue.contains(newName)))
            case Left(error)      => fail(error.toString)
    }

    test("updateNameById (no match)") {
        init(2, 0)
        runWithUserAuth(user =>
            conceptNameService.updateNameById(Long.MaxValue, ConceptNameUpdate(author = Some("x")), user.username)
        ) match
            case Left(e)  => assert(e.isInstanceOf[org.mbari.oni.ConceptNameIdNotFound], e.toString)
            case Right(_) => fail("Should not have updated a non-existent concept name")
    }

    test("deleteNameById") {
        val rawRoot      = RawConcept.from(init(3, 4))
        val (_, otherId) = nameIds(rawRoot)
        val otherName    = rawRoot.names.find(_.id.contains(otherId)).get.name

        runWithUserAuth(user => conceptNameService.deleteNameById(otherId, user.username)) match
            case Right(rawConcept) =>
                assert(!rawConcept.names.flatMap(_.id).contains(otherId))
                assert(conceptNameService.findById(otherId).isLeft)
                assert(conceptNameService.findByName(otherName).isLeft)
            case Left(error)       => fail(error.toString)
    }

    test("deleteNameById (attempt to delete primary name)") {
        val rawRoot        = RawConcept.from(init(3, 3))
        val (primaryId, _) = nameIds(rawRoot)
        runWithUserAuth(user => conceptNameService.deleteNameById(primaryId, user.username)) match
            case Right(_) => fail("Should not be able to delete a primary name")
            case Left(e)  =>
                assert(e.isInstanceOf[org.mbari.oni.InvalidConceptNameChange], e.toString)
                assert(conceptNameService.findById(primaryId).isRight)
    }

    test("deleteNameById (no match)") {
        init(2, 0)
        runWithUserAuth(user => conceptNameService.deleteNameById(Long.MaxValue, user.username)) match
            case Left(e)  => assert(e.isInstanceOf[org.mbari.oni.ConceptNameIdNotFound], e.toString)
            case Right(_) => fail("Should not have deleted a non-existent concept name")
    }
