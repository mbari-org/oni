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

package org.mbari.oni.endpoints

import org.mbari.oni.domain.{Page, Reference, ReferenceQuery, ReferenceUpdate}
import org.mbari.oni.etc.circe.CirceCodecs.{*, given}
import org.mbari.oni.etc.jdk.Loggers.given
import org.mbari.oni.etc.jwt.JwtService
import org.mbari.oni.jpa.DataInitializer
import org.mbari.oni.jpa.entities.TestEntityFactory
import org.mbari.oni.services.ReferenceService
import sttp.model.StatusCode

import scala.util.Random

trait ReferenceEndpointsSuite extends EndpointsSuite with DataInitializer:

    given jwtService: JwtService = JwtService("mbari", "foo", "bar")

    lazy val endpoints: ReferenceEndpoints      = ReferenceEndpoints(entityManagerFactory)
    lazy val referenceService: ReferenceService = ReferenceService(entityManagerFactory)

    override def beforeEach(context: BeforeEach): Unit =
        super.beforeEach(context)
        referenceService.findAll(10000, 0) match
            case Right(entities) =>
                entities.foreach(entity => referenceService.deleteById(entity.id.get))
            case Left(error)     => log.atDebug.withCause(error).log("Failed to delete all reference entities")

    private def initRefs(n: Int): Seq[Reference] =
        (1 to n).flatMap { i =>
            val entity = TestEntityFactory.createReference()
            val dto    = Reference.from(entity)
            val ref    = referenceService.create(dto)
            assert(ref.isRight)
            ref.toOption
        }

    test("create") {
        val entity = TestEntityFactory.createReference()
        val dto    = Reference.from(entity)

        runPost(
            endpoints.createReferenceEndpointImpl,
            "http://test.com/v1/references",
            dto.stringify,
            response =>
                assertEquals(response.code, StatusCode.Ok)
                val obtained = checkResponse[Reference](response.body)
                assert(obtained.id.isDefined)
                val expected = dto.copy(id = obtained.id)
                assertEquals(obtained, expected)
            ,
            jwt = jwtService.authorize(jwtService.apiKey)
        )
    }

    test("update") {
        val refs   = initRefs(1)
        val ref    = refs.head
        val update = ReferenceUpdate(citation = Some("Updated citation"))
        runPut(
            endpoints.updateReferenceEndpointImpl,
            s"http://test.com/v1/references/${ref.id.getOrElse(-1)}",
            update.stringify,
            response =>
                assertEquals(response.code, StatusCode.Ok)
                val obtained = checkResponse[Reference](response.body)
                assertEquals(obtained.id, ref.id)
                assertEquals(Some(obtained.citation), update.citation)
            ,
            jwt = jwtService.authorize(jwtService.apiKey)
        )
    }

    test("delete") {
        val refs = initRefs(1)
        val id   = refs.head.id.get
        runDelete(
            endpoints.deleteReferenceEndpointImpl,
            s"http://test.com/v1/references/${id}",
            response => assertEquals(response.code, StatusCode.Ok),
            jwt = jwtService.authorize(jwtService.apiKey)
        )

        referenceService.findById(id) match
            case Right(Some(entity)) => fail(s"Reference with id '${id}' was found after it was deleted")
            case Right(None)         => assert(true)
            case Left(error)         => fail(error.toString)
    }

    test("findReferenceById") {
        val refs = initRefs(2)
        val id   = refs.head.id.get
        runGet(
            endpoints.findReferenceByIdEndpointImpl,
            s"http://test.com/v1/references/${id}",
            response =>
                assertEquals(response.code, StatusCode.Ok)
                val obtained = checkResponse[Reference](response.body)
                assertEquals(obtained.id, Some(id))
        )
    }

    test("findAll") {
        val refs = initRefs(2)
        runGet(
            endpoints.findAllEndpointImpl,
            "http://test.com/v1/references",
            response =>
                assertEquals(response.code, StatusCode.Ok)
                val obtained = checkResponse[Page[Seq[Reference]]](response.body)
                assertEquals(obtained.content.size, 2)
        )
    }

    test("findReferencesByCitationGlob") {
        val refs     = initRefs(2)
        val citation = refs.head.citation
        val random   = Random()
        val start    = random.nextInt(citation.length - 2)
        val end      = random.nextInt(citation.length - start) + start
        val glob     = citation.substring(start, end)
        val dto      = ReferenceQuery(citation = Some(glob))
        runPost(
            endpoints.findReferencesByCitationGlobEndpointImpl,
            s"http://test.com/v1/references/query/citation",
            dto.stringify,
            response =>
                assertEquals(response.code, StatusCode.Ok)
                val obtained = checkResponse[Page[Seq[Reference]]](response.body)
                assertEquals(obtained.content.size, 1)
        )
    }

    test("findReferenceByDoi") {
        val refs = initRefs(2)
        val doi  = refs.head.doi.get
        val dto  = ReferenceQuery(doi = Some(doi))
        runPost(
            endpoints.findReferenceByDoiEndpointImpl,
            s"http://test.com/v1/references/query/doi",
            dto.stringify,
            response =>
                assertEquals(response.code, StatusCode.Ok)
                val obtained = checkResponse[Reference](response.body)
                assertEquals(obtained.doi, Some(doi))
        )
    }

    test("addConcept") {
        val entity   = TestEntityFactory.createReference()
        val dto      = Reference.from(entity)
        val id: Long = referenceService.create(dto) match
            case Right(r)    =>
                assert(true)
                r.id.getOrElse(-1)
            case Left(error) =>
                fail(error.toString)
                -1

        val root = init(3, 0)
        val name = root.getPrimaryConceptName.getName
        runPut(
            endpoints.addConceptEndpointImpl,
            s"http://test.com/v1/references/add/${id}/to/${name}",
            "",
            response =>
                assertEquals(response.code, StatusCode.Ok)
                val obtained = checkResponse[Reference](response.body)
                assertEquals(obtained.concepts.size, 1)
                assertEquals(obtained.concepts.head, name)
            ,
            jwt = jwtService.authorize(jwtService.apiKey)
        )
    }

    test("removeConcept") {
        val entity   = TestEntityFactory.createReference()
        val dto      = Reference.from(entity)
        val id: Long = referenceService.create(dto) match
            case Right(r)    =>
                assert(true)
                r.id.getOrElse(-1)
            case Left(error) =>
                fail(error.toString)
                -1

        val root = init(4, 2)
        val name = root.getPrimaryConceptName.getName
        referenceService.addConcept(id, name) match
            case Right(r)    =>
                assertEquals(r.concepts.size, 1)
                assertEquals(r.concepts.head, name)
            case Left(error) =>
                fail(error.toString)

        runPut(
            endpoints.removeConceptEndpointImpl,
            s"http://test.com/v1/references/remove/${id}/from/${name}",
            "",
            response =>
                assertEquals(response.code, StatusCode.Ok)
                val obtained = checkResponse[Reference](response.body)
                assertEquals(obtained.concepts.size, 0)
            ,
            jwt = jwtService.authorize(jwtService.apiKey)
        )
    }
