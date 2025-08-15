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

import org.mbari.oni.domain.{ConceptNameCreate, ConceptNameTypes, Count, ExtendedHistory, LinkCreate, Page, UserAccountRoles}
import org.mbari.oni.etc.circe.CirceCodecs.given
import org.mbari.oni.etc.jdk.Strings
import org.mbari.oni.etc.jwt.JwtService
import org.mbari.oni.jdbc.FastPhylogenyService
import org.mbari.oni.jpa.DataInitializer
import org.mbari.oni.services.{ConceptService, ConceptNameService, HistoryService, LinkTemplateService, UserAuthMixin}
import sttp.model.StatusCode

trait HistoryEndpointsSuite extends EndpointsSuite with DataInitializer with UserAuthMixin:

    given jwtService: JwtService         = JwtService("mbari", "foo", "bar")
    lazy val fastPhylogenyService        = new FastPhylogenyService(entityManagerFactory)
    lazy val linkTemplateService         = LinkTemplateService(entityManagerFactory)
    lazy val historyService              = new HistoryService(entityManagerFactory)
    // lazy val conceptService              = new ConceptService(entityManagerFactory)
    lazy val conceptNameService          = new ConceptNameService(entityManagerFactory)
    lazy val endpoints: HistoryEndpoints = HistoryEndpoints(entityManagerFactory, fastPhylogenyService)
    private val password                 = "foofoofoo"

    def buildHistory(): Either[Throwable, ExtendedHistory] =
        val root = init(1, 0)
        val add  = LinkCreate(
            root.getName,
            Strings.random(10),
            Strings.random(10),
            Strings.random(10)
        )

        for
            _       <- runWithUserAuth(
                           user => linkTemplateService.create(add, user.username),
                           role = UserAccountRoles.ADMINISTRATOR.getRoleName
                       )
            history <- historyService.findByConceptName(root.getName).map(_.head)
        yield history

    test("pending") {
        init(3, 5)
        runGet(
            endpoints.pendingEndpointImpl,
            "http://test.com/v1/history/pending",
            response =>
                assertEquals(response.code, StatusCode.Ok)
                val histories = checkResponse[Page[Seq[ExtendedHistory]]](response.body)
                assert(histories.content.nonEmpty)
        )
    }

    test("pendingCount") {
        init(3, 5)
        runGet(
            endpoints.pendingCountEndpointImpl,
            "http://test.com/v1/history/pending/count",
            response =>
                assertEquals(response.code, StatusCode.Ok)
                val count = checkResponse[Count](response.body)
                assert(count.count > 0)
        )
    }

    test("approved") {
        init(3, 5)
        runGet(
            endpoints.approvedEndpointsImpl,
            "http://test.com/v1/history/approved",
            response =>
                assertEquals(response.code, StatusCode.Ok)
                val histories = checkResponse[Page[Seq[ExtendedHistory]]](response.body)
                assert(histories.content.nonEmpty)
        )
    }

    test("approvedCount") {
        init(3, 5)
        runGet(
            endpoints.approvedCountEndpointImpl,
            "http://test.com/v1/history/approved/count",
            response =>
                assertEquals(response.code, StatusCode.Ok)
                val count = checkResponse[Count](response.body)
//                println(count.stringify)
        )
    }

    test("findById") {
        val attempt1 = buildHistory()

        val history = attempt1 match
            case Left(e)  => fail(e.getMessage)
            case Right(h) => h

        runGet(
            endpoints.findByIdEndpointImpl,
            s"http://test.com/v1/history/${history.id.get}",
            response =>
                assertEquals(response.code, StatusCode.Ok)
                val obtained = checkResponse[ExtendedHistory](response.body)
                assertEquals(obtained, history)
        )
    }

    test("findByConceptName") {
        val attempt1 = buildHistory()

        val history = attempt1 match
            case Left(e)  => fail(e.getMessage)
            case Right(h) => h

        runGet(
            endpoints.findByConceptNameEndpointImpl,
            s"http://test.com/v1/history/concept/${history.concept}",
            response =>
                assertEquals(response.code, StatusCode.Ok)
                val histories = checkResponse[Seq[ExtendedHistory]](response.body)
                assert(histories.nonEmpty)
        )
    }

    test("delete") {
        val attempt1 = buildHistory()

        val history = attempt1 match
            case Left(e)  => fail(e.getMessage)
            case Right(h) => h

        val attempt2 = testWithUserAuth(
            user =>
                runDelete(
                    endpoints.deleteEndpointImpl,
                    s"http://test.com/v1/history/${history.id.get}",
                    response => assertEquals(response.code, StatusCode.Ok),
                    jwt = jwtService.login(user.username, password, user.toEntity)
                ),
            password
        )

        attempt2 match
            case Left(e)  => fail(e.getMessage)
            case Right(_) =>
                historyService.findById(history.id.get) match
                    case Left(e)  => assert(true)
                    case Right(_) => fail("History record was not deleted")

    }

    test("approve") {
        val attempt1 = buildHistory()

        val history = attempt1 match
            case Left(e)  => fail(e.getMessage)
            case Right(h) => h

        val attempt2 = testWithUserAuth(
            user =>
                runPut(
                    endpoints.approveEndpointImpl,
                    s"http://test.com/v1/history/approve/${history.id.get}",
                    "", // empty body
                    response => assertEquals(response.code, StatusCode.Ok),
                    jwt = jwtService.login(user.username, password, user.toEntity)
                ),
            password
        )

        attempt2 match
            case Left(e)  => fail(e.getMessage)
            case Right(_) =>
                historyService.findById(history.id.get) match
                    case Left(e)  => fail(e.getMessage)
                    case Right(h) => assert(h.approved)
    }

    test("reject") {
        val attempt1 = buildHistory()

        val history = attempt1 match
            case Left(e)  => fail(e.getMessage)
            case Right(h) => h

        val attempt2 = testWithUserAuth(
            user =>
                runPut(
                    endpoints.rejectEndpointImpl,
                    s"http://test.com/v1/history/reject/${history.id.get}",
                    "", // empty body
                    response => assertEquals(response.code, StatusCode.Ok),
                    jwt = jwtService.login(user.username, password, user.toEntity)
                ),
            password
        )

        attempt2 match
            case Left(e)  => fail(e.getMessage)
            case Right(_) =>
                historyService.findById(history.id.get) match
                    case Left(e)  => fail(e.getMessage)
                    case Right(h) => assert(!h.approved)
    }

    test("approve primary concept name change") {
        val root = initShallowTree(4)
        val concept = root.getChildConcepts.iterator().next()
        val create = ConceptNameCreate(concept.getName, Strings.random(15), ConceptNameTypes.PRIMARY.getType)
        val attempt1 = runWithUserAuth(
            user => conceptNameService.addName(create, user.username),
            role = UserAccountRoles.MAINTENANCE.getRoleName
        )
        attempt1 match
            case Right(rawConcept) =>
                assertEquals(rawConcept.primaryName, create.newName)
            case Left(error)       =>
                fail(error.toString)


        val histories = historyService.findByConceptName(create.newName) match
            case Left(e)  => fail(e.getMessage)
            case Right(h) => h
        
        assertEquals(histories.size, 1)
        val history = histories.head
        val attempt2 = testWithUserAuth(
            user =>
                runPut(
                    endpoints.approveEndpointImpl,
                    s"http://test.com/v1/history/approve/${history.id.get}",
                    "", // empty body
                    response => assertEquals(response.code, StatusCode.Ok),
                    jwt = jwtService.login(user.username, password, user.toEntity)
                ),
            password
        )

        attempt2 match
            case Left(e)  => fail(e.getMessage)
            case Right(_) =>
                historyService.findById(history.id.get) match
                    case Left(e)  => fail(e.getMessage)
                    case Right(h) =>
                        assert(h.approved)
                        val updatedConcept = conceptService.findByName(concept.getName).getOrElse(fail("Concept not found"))
                        assertEquals(updatedConcept.name, create.newName)
    }

    test("reject primary concept name change") {
        val root = initShallowTree(4)
        val concept = root.getChildConcepts.iterator().next()
        val originalName = concept.getName
        val create = ConceptNameCreate(concept.getName, Strings.random(15), ConceptNameTypes.PRIMARY.getType)
        val attempt1 = runWithUserAuth(
            user => conceptNameService.addName(create, user.username),
            role = UserAccountRoles.MAINTENANCE.getRoleName
        )
        attempt1 match
            case Right(rawConcept) =>
                assertEquals(rawConcept.primaryName, create.newName)
            case Left(error)       =>
                fail(error.toString)


        val histories = historyService.findByConceptName(create.newName) match
            case Left(e)  => fail(e.getMessage)
            case Right(h) => h
        
        assertEquals(histories.size, 1)
        // println(s"histories: ${histories}")
        val history = histories.head
        val attempt2 = testWithUserAuth(
            user =>
                runPut(
                    endpoints.rejectEndpointImpl,
                    s"http://test.com/v1/history/reject/${history.id.get}",
                    "", // empty body
                    response => assertEquals(response.code, StatusCode.Ok),
                    jwt = jwtService.login(user.username, password, user.toEntity)
                ),
            password
        )

        attempt2 match
            case Left(e)  => fail(e.getMessage)
            case Right(_) =>
                historyService.findById(history.id.get) match
                    case Left(e)  => fail(e.getMessage)
                    case Right(h) =>
                        assert(!h.approved)
                        val updatedConcept = conceptService.findByName(concept.getName).getOrElse(fail("Concept not found"))
                        assertEquals(updatedConcept.name, originalName)
    }
