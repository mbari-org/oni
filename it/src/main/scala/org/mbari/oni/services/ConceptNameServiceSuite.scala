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
import org.mbari.oni.etc.circe.CirceCodecs.{*, given}
import org.mbari.oni.etc.jdk.Strings
import org.mbari.oni.jpa.DataInitializer

trait ConceptNameServiceSuite extends DataInitializer with UserAuthMixin:

    lazy val conceptNameService: ConceptNameService = new ConceptNameService(entityManagerFactory)

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
        val dto     = ConceptNameCreate(name = name, newName = "newName22", nameType = ConceptNameTypes.SYNONYM.getType)

        val attempt = runWithUserAuth(user => conceptNameService.addName(dto, user.username))

        attempt match
            case Right(rawConcept) =>
                val obtained = rawConcept.names.map(_.name).toSeq
                assert(obtained.contains(dto.name))
                assert(obtained.contains(dto.newName))
            case Left(error)       =>
                fail(error.toString)
    }

    test("updateName") {

        val root    = init(3, 3)
        assert(root != null)
        val rawRoot = RawConcept.from(root)
        val name    = rawRoot.primaryName
        val dto     =
            ConceptNameUpdate(
                newName = Some("newName"),
                nameType = Some(ConceptNameTypes.PRIMARY.getType),
                author = Some(Strings.random(5))
            )

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
