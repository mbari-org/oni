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

import org.mbari.oni.domain.{ConceptNameTypes, RawConcept, RawConceptName}
import org.mbari.oni.jpa.entities.ConceptEntity
import org.mbari.oni.jpa.{DataInitializer, DatabaseFunSuite}
import org.mbari.oni.jpa.EntityManagerFactories.*
import org.mbari.oni.etc.circe.CirceCodecs.{*, given}

import scala.jdk.CollectionConverters.*

trait ConceptNameServiceSuite extends DataInitializer:

    lazy val conceptNameService: ConceptNameService = new ConceptNameService(entityManagerFactory)

    test("findAllNames") {
        val root     = init(3, 3)
        assert(root != null)
        val rawRoot  = RawConcept.from(root)
        val expected = rawRoot.descendantNames
        conceptNameService.findAllNames() match
            case Right(names) =>
                val obtained = names.sorted
                assertEquals(obtained, expected)
            case Left(error)  =>
                fail(error.toString)

    }

    test("addName (new primary name)") {
        val root = init(3, 3)
        assert(root != null)
        val rawRoot = RawConcept.from(root)
        val name    = rawRoot.primaryName
        val newName = RawConceptName(name = "newName", nameType = ConceptNameTypes.PRIMARY.getType)
        conceptNameService.addName(name, newName) match
            case Right(rawConcept) =>
                val obtained = rawConcept.names.map(_.name).toSeq
                val expected = rawRoot.names.map(_.name).toSeq :+ newName.name
                assertEquals(obtained.sorted, expected.sorted)
                assertNotEquals(rawConcept.primaryName, name)
                assertEquals(rawConcept.primaryName, newName.name)
            case Left(error)       =>
                fail(error.toString)
    }

    test("addName (not a primary name)") {
        val root = init(3, 3)
        assert(root != null)
        val rawRoot = RawConcept.from(root)
        val name = rawRoot.primaryName
        val newName = RawConceptName(name = "newName22", nameType = ConceptNameTypes.SYNONYM.getType)
        conceptNameService.addName(name, newName) match
            case Right(rawConcept) =>
                val obtained = rawConcept.names.map(_.name).toSeq
                val expected = rawRoot.names.map(_.name).toSeq :+ newName.name
                assertEquals(obtained.sorted, expected.sorted)
                assertNotEquals(rawConcept.primaryName, newName.name)
            case Left(error) =>
                fail(error.toString)
    }

    test("updateName") {
        val root = init(3, 3)
        assert(root != null)
        val rawRoot = RawConcept.from(root)
        val name    = rawRoot.primaryName
        val newName = RawConceptName(name = "newName", nameType = ConceptNameTypes.PRIMARY.getType)
        conceptNameService.updateName(name, newName) match
            case Right(rawConcept) =>
                val obtained = rawConcept.names.map(_.name).toSeq
                assert(!obtained.contains(name))
                assert(obtained.contains(newName.name))
            case Left(error)       =>
                fail(error.toString)
    }

    test("updateName (attempt to change primary to non-primary)") {
        val root = init(3, 3)
        assert(root != null)
        val rawRoot = RawConcept.from(root)
        val name = rawRoot.primaryName
        val newName = RawConceptName(name = "newName", nameType = ConceptNameTypes.COMMON.getType)
        conceptNameService.updateName(name, newName) match
            case Right(rawConcept) =>
                println(rawConcept.stringify)
                fail("Should have thrown an exception")
            case Left(error) =>
                assert(error.isInstanceOf[IllegalArgumentException])
    }

    test("deleteName (attempt to delete primary name)") {
        val root = init(3, 3)
        assert(root != null)
        val rawRoot = RawConcept.from(root)
        val name    = rawRoot.primaryName
        conceptNameService.deleteName(name) match
            case Right(rawConcept) =>
                fail("Should have thrown an exception")
            case Left(error)       =>
                assert(error.isInstanceOf[IllegalArgumentException])
    }

    test("deleteName") {
        val root = init(3, 4)
        assert(root != null)
        val rawRoot = RawConcept.from(root)
        val nameToDelete = rawRoot.names
            .find(_.nameType != ConceptNameTypes.PRIMARY.getType)
            .map(_.name)
            .getOrElse("")
        conceptNameService.deleteName(nameToDelete) match
            case Right(rawConcept) =>
                assert(!rawConcept.names.map(_.name).toSeq.contains(nameToDelete))
            case Left(error) =>
                fail(error.toString)

    }



