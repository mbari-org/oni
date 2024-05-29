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
import org.mbari.oni.jpa.DataInitializer
import org.mbari.oni.etc.jdk.Loggers.given
import org.mbari.oni.jpa.entities.TestEntityFactory

import java.net.URI

trait ReferenceServiceSuite extends DataInitializer {

    lazy val service = ReferenceService(entityManagerFactory)

    override def beforeEach(context: BeforeEach): Unit =
        super.beforeEach(context)
        service.findAll(10000, 0) match
            case Right(entities) =>
                entities.foreach(entity => service.delete(entity.id.get))
            case Left(error) => log.atDebug.withCause(error).log("Failed to delete all reference entities")

    test("create") {
        val ref = Reference(
            doi = Some(URI.create("https://doi.org/10.1109/OCEANS.2006.306879")),
            citation = "B. M. Schlining and N. J. Stout, \"MBARI's Video Annotation and Reference System,\" OCEANS 2006, Boston, MA, USA, 2006, pp. 1-5",
        )

        // create should return an entity
        service.create(ref) match {
            case Right(entity) => assert(entity != null)
            case Left(error) => fail(error.toString)
        }

        // create should fail as DOI is already in use
        service.create(ref) match {
            case Right(_) => fail("Should have failed")
            case Left(_) => assert(true)
        }

    }

    test("update") {
        val ref = Reference(
            doi = Some(URI.create("https://doi.org/10.1038/s41598-022-19939-2")),
            citation = "Katija, K., Orenstein, E., Schlining, B. et al. FathomNet: A global image database for enabling artificial intelligence in the ocean. Sci Rep 12, 15914 (2022).",
        )

        service.create(ref) match {
            case Right(entity) =>
                assert(entity.id.isDefined)

                // Update the DOI
                val update1 = ReferenceUpdate(entity.id.get, doi = Some(URI.create("https://doi.org/10.1109/OCEANS.2006.306879")))
                service.update(update1) match {
                    case Right(entity) =>
                        assert(entity != null)
                        assertEquals(entity.doi, update1.doi)
                    case Left(error) => fail(error.toString)
                }

                // Update the citation
                val update2 = ReferenceUpdate(entity.id.get, citation = Some("B. M. Schlining and N. J. Stout, \"MBARI's Video Annotation and Reference System,\" OCEANS 2006, Boston, MA, USA, 2006, pp. 1-5"))
                service.update(update2) match {
                    case Right(entity) =>
                        assert(entity != null)
                        assertEquals(entity.citation, update2.citation.get)
                    case Left(error) => fail(error.toString)
                }
            case Left(error) => fail(error.toString)
        }
    }

    test("delete") {
        val ref = Reference(
            doi = Some(URI.create("https://doi.org/10.1109/OCEANS.2006.306879")),
            citation = "B. M. Schlining and N. J. Stout, \"MBARI's Video Annotation and Reference System,\" OCEANS 2006, Boston, MA, USA, 2006, pp. 1-5",
        )

        service.create(ref) match {
            case Right(entity) =>
                assert(entity.id.isDefined)
                service.delete(entity.id.get) match {
                    case Right(_) => assert(true)
                    case Left(error) => fail(error.toString)
                }

                service.findById(entity.id.get) match {
                    case Right(opt) => assert(opt.isEmpty)
                    case Left(_) => assert(true)
                }
            case Left(error) => fail(error.toString)
        }
    }

    test("findAll") {
        val refs = 0 to 10 map { _ => TestEntityFactory.createReference() }
        refs.foreach(ref => service.create(Reference.from(ref)) match {
            case Right(_) => assert(true)
            case Left(error) => fail(error.toString)
        })

        val found = service.findAll(10000, 0) match {
            case Right(entities) => entities
            case Left(error) => fail(error.toString)
        }

        assertEquals(found.size, refs.size)
    }

    test("findByReferenceGlob") {}

    test("findByDoi") {}





}
