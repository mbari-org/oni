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

import org.mbari.oni.domain.{Reference, ReferenceUpdate}
import org.mbari.oni.etc.jdk.Loggers.given
import org.mbari.oni.jpa.DataInitializer
import org.mbari.oni.jpa.entities.TestEntityFactory

import java.net.URI

trait ReferenceServiceSuite extends DataInitializer:

    lazy val service = ReferenceService(entityManagerFactory)

    override def beforeEach(context: BeforeEach): Unit =
        super.beforeEach(context)
        service.findAll(10000, 0) match
            case Right(entities) =>
                entities.foreach(entity => service.deleteById(entity.id.get))
            case Left(error)     => log.atDebug.withCause(error).log("Failed to delete all reference entities")

    test("create") {
        val ref = Reference(
            doi = Some(URI.create("https://doi.org/10.1109/OCEANS.2006.306879")),
            citation =
                "B. M. Schlining and N. J. Stout, \"MBARI's Video Annotation and Reference System,\" OCEANS 2006, Boston, MA, USA, 2006, pp. 1-5"
        )

        // create should return an entity
        service.create(ref) match
            case Right(entity) => assert(entity != null)
            case Left(error)   => fail(error.toString)

        // create should fail as DOI is already in use
        service.create(ref) match
            case Right(_) => fail("Should have failed")
            case Left(_)  => assert(true)

    }

    test("update") {
        val ref = Reference(
            doi = Some(URI.create("https://doi.org/10.1038/s41598-022-19939-2")),
            citation =
                "Katija, K., Orenstein, E., Schlining, B. et al. FathomNet: A global image database for enabling artificial intelligence in the ocean. Sci Rep 12, 15914 (2022)."
        )

        service.create(ref) match
            case Right(entity) =>
                assert(entity.id.isDefined)

                // Update the DOI
                val update1 =
                    ReferenceUpdate(doi = Some(URI.create("https://doi.org/10.1109/OCEANS.2006.306879")))
                service.updateById(entity.id.get, update1) match
                    case Right(entity) =>
                        assert(entity != null)
                        assertEquals(entity.doi, update1.doi)
                    case Left(error)   => fail(error.toString)

                // Update the citation
                val update2 = ReferenceUpdate(
                    citation = Some(
                        "B. M. Schlining and N. J. Stout, \"MBARI's Video Annotation and Reference System,\" OCEANS 2006, Boston, MA, USA, 2006, pp. 1-5"
                    )
                )
                service.updateById(entity.id.get, update2) match
                    case Right(entity) =>
                        assert(entity != null)
                        assertEquals(entity.citation, update2.citation.get)
                    case Left(error)   => fail(error.toString)
            case Left(error)   => fail(error.toString)
    }

    test("delete") {
        val ref = Reference(
            doi = Some(URI.create("https://doi.org/10.1109/OCEANS.2006.306879")),
            citation =
                "B. M. Schlining and N. J. Stout, \"MBARI's Video Annotation and Reference System,\" OCEANS 2006, Boston, MA, USA, 2006, pp. 1-5"
        )

        service.create(ref) match
            case Right(entity) =>
                assert(entity.id.isDefined)
                service.deleteById(entity.id.get) match
                    case Right(_)    => assert(true)
                    case Left(error) => fail(error.toString)

                service.findById(entity.id.get) match
                    case Right(opt) => assert(opt.isEmpty)
                    case Left(_)    => assert(true)
            case Left(error)   => fail(error.toString)
    }

    test("findAll") {
        val refs = 0 until 10 map { _ => TestEntityFactory.createReference() }
        refs.foreach(ref =>
            service.create(Reference.from(ref)) match
                case Right(_)    => assert(true)
                case Left(error) => fail(error.toString)
        )

        val found = service.findAll(10000, 0) match
            case Right(entities) => entities
            case Left(error)     => fail(error.toString)

        assertEquals(found.size, refs.size)
    }

    test("findByCitationGlob") {
        val refs = 0 until 10 map { _ => TestEntityFactory.createReference() }
        refs.foreach(ref =>
            service.create(Reference.from(ref)) match
                case Right(_)    => assert(true)
                case Left(error) => fail(error.toString)
        )

        // This should match only one reference
        val glob = refs.head.getCitation.split(" ")(1)

        service.findByCitationGlob(glob, 10000, 0) match
            case Right(entities) =>
                assertEquals(entities.size, 1)
            case Left(error)     => fail(error.toString)
    }

    test("findByCitationGlob (glob has spaces)") {
        val ref      = TestEntityFactory.createReference()
        val citation = ref.getCitation
        val glob     = citation.substring(5, citation.length - 5)
//        println(glob)
        service.create(Reference.from(ref)) match
            case Right(_)    => assert(true)
            case Left(error) => fail(error.toString)

        // This should match only one reference
        service.findByCitationGlob(glob, 10000, 0) match
            case Right(entities) =>
                assertEquals(entities.size, 1)
            case Left(error)     => fail(error.toString)
    }

    test("findByDoi") {
        val refs = 0 until 10 map { _ => TestEntityFactory.createReference() }
        refs.foreach(ref =>
            service.create(Reference.from(ref)) match
                case Right(_)    => assert(true)
                case Left(error) => fail(error.toString)
        )

        val expected = refs.head.getDoi

        service.findByDoi(expected) match
            case Right(opt)  =>
                assert(opt.isDefined)
                val reference = opt.get
                assert(reference.doi.isDefined)
                val obtained  = reference.doi.get
                assertEquals(obtained, expected)
                assertEquals(refs.head.getCitation, reference.citation)
            case Left(error) => fail(error.toString)

    }

    test("addConcept") {
        val entity      = TestEntityFactory.createReference()
        val root        = init(4, 2)
        val conceptName = root.getPrimaryConceptName.getName

        // Add to root
        val attempt0 = for
            reference0 <- service.create(Reference.from(entity))
            reference1 <- service.addConcept(reference0.id.get, conceptName)
        yield reference1

        val referenceId = attempt0 match
            case Left(error)      => fail(error.toString)
            case Right(reference) =>
                assert(reference.concepts.contains(conceptName))
                assert(reference.id.isDefined)
                reference.id.get

        // Add to child
        val child     = root.getChildConcepts.iterator().next()
        val childName = child.getPrimaryConceptName.getName

        val attempt1 =
            for reference1 <- service.addConcept(referenceId, childName)
            yield reference1

        attempt1 match
            case Left(error)      => fail(error.toString)
            case Right(reference) =>
                assert(reference.concepts.contains(childName))
                assert(reference.concepts.contains(conceptName))

        conceptService.findByName(conceptName) match
            case Left(error)    => fail(error.toString)
            case Right(concept) =>
                val refIds = concept.references.flatMap(_.id)
                assert(refIds.contains(referenceId))
//                println(s"--- ${concept}")
//                println(s"--- ${concept.stringify}")
    }

    test("removeConcept") {
        val entity      = TestEntityFactory.createReference()
        val root        = init(4, 0)
        val conceptName = root.getPrimaryConceptName.getName
        val child       = root.getChildConcepts.iterator().next()
        val childName   = child.getPrimaryConceptName.getName

        // Add to root
        val attempt0 = for
            reference0 <- service.create(Reference.from(entity))
            reference1 <- service.addConcept(reference0.id.get, conceptName)
            reference2 <- service.addConcept(reference0.id.get, childName)
        yield
            assert(reference0.id.isDefined)
            assert(reference1.concepts.contains(conceptName))
            assert(reference2.concepts.contains(conceptName))
            assert(reference2.concepts.contains(childName))
            reference0.id.get

        val referenceId = attempt0 match
            case Left(error) => fail(error.toString)
            case Right(id)   => id

        // Remove from root
        val attempt1 =
            for reference1 <- service.removeConcept(referenceId, conceptName)
            yield reference1

        attempt1 match
            case Left(error)      => fail(error.toString)
            case Right(reference) =>
                assert(reference.concepts.contains(childName))
                assert(!reference.concepts.contains(conceptName))

    }
