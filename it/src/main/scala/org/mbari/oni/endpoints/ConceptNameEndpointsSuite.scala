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

import org.mbari.oni.domain.{ConceptNameCreate, ConceptNameTypes, ConceptNameUpdate, Page, RawConcept}
import org.mbari.oni.etc.jwt.JwtService
import org.mbari.oni.jpa.DataInitializer
import org.mbari.oni.services.UserAuthMixin
import sttp.model.StatusCode
import org.mbari.oni.etc.circe.CirceCodecs.{*, given}

import scala.concurrent.ExecutionContext

trait ConceptNameEndpointsSuite extends EndpointsSuite with DataInitializer with UserAuthMixin:

    given jwtService: JwtService             = JwtService("mbari", "foo", "bar")
    lazy val endpoints: ConceptNameEndpoints = ConceptNameEndpoints(entityManagerFactory)
    private val password                     = "foofoo"

    test("findAll") {
        val root     = init(3, 3)
        assert(root != null)
        val rawRoot  = RawConcept.from(root)
        val expected = rawRoot.descendantNames.sorted
        runGet(
            endpoints.allEndpointImpl,
            "http://test.com/v1/names",
            response =>
                assertEquals(response.code, StatusCode.Ok)
                val conceptNames = checkResponse[Page[Seq[String]]](response.body).content.sorted
                assertEquals(conceptNames, expected)
        )
    }

    test("addConceptName") {
        val root    = init(3, 3)
        assert(root != null)
        val rawRoot = RawConcept.from(root)
        val name    = rawRoot.primaryName
        val dto     = ConceptNameCreate(name = name, newName = "newName", nameType = ConceptNameTypes.PRIMARY.getType)

        val attempt = testWithUserAuth(
            user =>
                runPost(
                    endpoints.addConceptNameEndpointImpl,
                    "http://test.com/v1/names",
                    dto.stringify,
                    response =>
                        assertEquals(response.code, StatusCode.Ok)
                        val rawConcept = checkResponse[RawConcept](response.body)
                        val obtained   = rawConcept.names.map(_.name).toSeq
                        assert(obtained.contains(dto.name))
                        assert(obtained.contains(dto.newName))
//                    println(s"obtained: $obtained")
                    ,
                    jwt = jwtService.login(user.username, password, user.toEntity)
                ),
            password
        )

        attempt match
            case Right(_)    => assert(true)
            case Left(error) => fail(error.toString)
    }

    test("updateConceptName") {
        val root    = init(3, 3)
        assert(root != null)
        val rawRoot = RawConcept.from(root)
        val name    = rawRoot.primaryName
        val dto     = ConceptNameUpdate(newName = Some("newName"))

        val attempt = testWithUserAuth(
            user =>
                runPut(
                    endpoints.updateConceptNameEndpointImpl,
                    s"http://test.com/v1/names/$name",
                    dto.stringify,
                    response =>
                        assertEquals(response.code, StatusCode.Ok)
                        val rawConcept = checkResponse[RawConcept](response.body)
//                        println(rawConcept.stringify)
                        val obtained   = rawConcept.names.map(_.name).toSeq
//                        println(s"obtained: $obtained")
                        assert(!obtained.contains(name))
                        assert(obtained.contains(dto.newName.getOrElse("")))
                    ,
                    jwt = jwtService.login(user.username, password, user.toEntity)
                ),
            password
        )

        attempt match
            case Right(_)    => assert(true)
            case Left(error) => fail(error.toString)
    }

    test("deleteConceptName") {
        val root    = init(3, 3)
        assert(root != null)
        val rawRoot = RawConcept.from(root)
        val name    = rawRoot.names.filterNot(_.name == rawRoot.primaryName).head.name

        val attempt = testWithUserAuth(
            user =>
                runDelete(
                    endpoints.deleteConceptNameEndpointImpl,
                    s"http://test.com/v1/names/$name",
                    response =>
                        assertEquals(response.code, StatusCode.Ok)
                        val rawConcept = checkResponse[RawConcept](response.body)
                        val obtained   = rawConcept.names.map(_.name).toSeq
                        assert(!obtained.contains(name))
                    ,
                    jwt = jwtService.login(user.username, password, user.toEntity)
                ),
            password
        )

        attempt match
            case Right(_)    => assert(true)
            case Left(error) => fail(error.toString)

    }
