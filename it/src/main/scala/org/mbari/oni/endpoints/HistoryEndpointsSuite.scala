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

import io.circe.parser.decode
import org.mbari.oni.domain.{
    BadRequest,
    ConceptNameCreate,
    ConceptNameTypes,
    Count,
    ExtendedHistory,
    LinkCreate,
    Page,
    UserAccountRoles
}
import org.mbari.oni.etc.circe.CirceCodecs.given
import org.mbari.oni.etc.jdk.Strings
import org.mbari.oni.etc.jwt.JwtService
import org.mbari.oni.jdbc.FastPhylogenyService
import org.mbari.oni.jpa.DataInitializer
import org.mbari.oni.jpa.entities.{ConceptEntity, TestEntityFactory}
import org.mbari.oni.services.{ConceptNameService, HistoryService, LinkTemplateService}
import sttp.model.StatusCode

import java.time.Instant
import java.util.Date

trait HistoryEndpointsSuite extends EndpointsSuite with DataInitializer with EndpointsAuthMixin:

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
                           role = UserAccountRoles.MAINTENANCE.getRoleName
                       )
            history <- historyService.findByConceptName(root.getName).map(_.head)
        yield history

    /**
     * Like `init(3, 5)` but guarantees the tree contains both approved (processed) and pending histories.
     * `TestEntityFactory.createHistory` only processes a history with probability 1/2, and a random tree can be small,
     * so `init(3, 5)` alone occasionally produces no approved (or no pending) histories, which made the history listing
     * tests flaky. Two of each are added so sort-order assertions always compare more than one row.
     */
    private def initWithHistories(): ConceptEntity =
        val root     = TestEntityFactory.buildRoot(3, 5)
        val metadata = root.getConceptMetadata
        for _ <- 1 to 2 do
            val approved = TestEntityFactory.createHistory()
            approved.setApproved(true)
            approved.setProcessedDate(Date.from(Instant.now()))
            approved.setProcessorName(Strings.random(20))
            metadata.addHistory(approved)

            val pending = TestEntityFactory.createHistory()
            pending.setApproved(false)
            pending.setProcessedDate(null)
            pending.setProcessorName(null)
            metadata.addHistory(pending)

        conceptService.init(root) match
            case Right(entity) => entity
            case Left(error)   => fail(s"Failed to initialize test data: $error")

    test("pending") {
        initWithHistories()
        runGet(
            endpoints.pendingEndpointImpl,
            "http://test.com/v1/history/pending",
            response =>
                assertEquals(response.code, StatusCode.Ok)
                val histories = checkResponse[Page[Seq[ExtendedHistory]]](response.body)
                assert(histories.content.nonEmpty)
        )
    }

    test("pending (sort by creatorName)") {
        initWithHistories()
        runGet(
            endpoints.pendingEndpointImpl,
            "http://test.com/v1/history/pending?sort=creatorName,desc",
            response =>
                assertEquals(response.code, StatusCode.Ok)
                val histories = checkResponse[Page[Seq[ExtendedHistory]]](response.body)
                assert(histories.content.nonEmpty)
                val sorted    = histories.content.sortBy(_.creatorName.toLowerCase)(Ordering[String]).reverse
                assertEquals(histories.content, sorted)
        )

        runGet(
            endpoints.pendingEndpointImpl,
            "http://test.com/v1/history/pending?sort=creatorName,asc",
            response =>
                assertEquals(response.code, StatusCode.Ok)
                val histories = checkResponse[Page[Seq[ExtendedHistory]]](response.body)
                assert(histories.content.nonEmpty)
                val sorted    = histories.content.sortBy(_.creatorName.toLowerCase)(Ordering[String])
                assertEquals(histories.content, sorted)
        )
    }

    test("pendingCount") {
        initWithHistories()
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
        initWithHistories()
        runGet(
            endpoints.approvedEndpointsImpl,
            "http://test.com/v1/history/approved",
            response =>
                assertEquals(response.code, StatusCode.Ok)
                val histories = checkResponse[Page[Seq[ExtendedHistory]]](response.body)
                assert(histories.content.nonEmpty)
        )
    }

    test("approved (sort by oldValue)") {
        initWithHistories()
        runGet(
            endpoints.approvedEndpointsImpl,
            "http://test.com/v1/history/approved?sort=oldValue,desc",
            response =>
                assertEquals(response.code, StatusCode.Ok)
                val histories = checkResponse[Page[Seq[ExtendedHistory]]](response.body)
                assert(histories.content.nonEmpty)
                val obtained  = histories.content.map(_.oldValue).flatten
                val expected  = obtained.sortBy(_.toLowerCase)(Ordering[String]).reverse
                assertEquals(obtained, expected)
        )

        runGet(
            endpoints.approvedEndpointsImpl,
            "http://test.com/v1/history/approved?sort=oldValue,asc",
            response =>
                assertEquals(response.code, StatusCode.Ok)
                val histories = checkResponse[Page[Seq[ExtendedHistory]]](response.body)
                assert(histories.content.nonEmpty)
                val obtained  = histories.content.map(_.oldValue).flatten
                val expected  = obtained.sortBy(_.toLowerCase)(Ordering[String])
                assertEquals(obtained, expected)
        )
    }

    test("approved (sort by concept)") {
        initWithHistories()
        runGet(
            endpoints.approvedEndpointsImpl,
            "http://test.com/v1/history/approved?sort=concept,desc",
            response =>
                assertEquals(response.code, StatusCode.Ok)
                val histories = checkResponse[Page[Seq[ExtendedHistory]]](response.body)
                assert(histories.content.nonEmpty)
                val obtained  = histories.content.map(_.concept)
                val expected  = obtained.sortBy(_.toLowerCase)(Ordering[String]).reverse
                assertEquals(obtained, expected)
        )

        runGet(
            endpoints.approvedEndpointsImpl,
            "http://test.com/v1/history/approved?sort=concept",
            response =>
                assertEquals(response.code, StatusCode.Ok)
                val histories = checkResponse[Page[Seq[ExtendedHistory]]](response.body)
                assert(histories.content.nonEmpty)
                val obtained  = histories.content.map(_.concept)
                val expected  = obtained.sortBy(_.toLowerCase)(Ordering[String])
                assertEquals(obtained, expected)
        )
    }

    // ---- Exhaustive sort tests --------------------------------------------------------------
    // Compare only the sort keys (not whole objects) so ties in the key can't cause flaky failures.
    // Optional keys are flattened because SQL Server and Postgres order NULLs differently.

    final private case class SortField[K](name: String, key: ExtendedHistory => Option[K])(using
        val ordering: Ordering[K]
    )

    private val stringKey: (ExtendedHistory => Option[String]) => ExtendedHistory => Option[String] =
        f => h => f(h).map(_.toLowerCase)

    private val pendingSortFields: Seq[SortField[?]] = Seq(
        SortField[Long]("creationTimestamp", h => Some(h.creationTimestamp.toEpochMilli)),
        SortField[String]("creatorName", stringKey(h => Some(h.creatorName))),
        SortField[String]("oldValue", stringKey(_.oldValue)),
        SortField[String]("newValue", stringKey(_.newValue)),
        SortField[String]("concept", stringKey(h => Some(h.concept)))
    )

    private val approvedSortFields: Seq[SortField[?]] = pendingSortFields ++ Seq(
        SortField[Long]("processedTimestamp", _.processedTimestamp.map(_.toEpochMilli)),
        SortField[String]("processorName", stringKey(_.processorName))
    )

    // (query suffix, descending?)
    private val directions = Seq(
        ""      -> false, // no direction => ascending
        ",asc"  -> false,
        ",desc" -> true,
        ",DESC" -> true   // direction is case-insensitive
    )

    private def assertSortedBy[K](histories: Seq[ExtendedHistory], field: SortField[K], descending: Boolean): Unit =
        val obtained = histories.flatMap(field.key)
        val ord      = if descending then field.ordering.reverse else field.ordering
        assertEquals(obtained, obtained.sorted(using ord), s"Not sorted by ${field.name} (descending=$descending)")

    for
        (label, path, endpointImpl, fields) <- Seq(
                                                   (
                                                       "pending",
                                                       "pending",
                                                       () => endpoints.pendingEndpointImpl,
                                                       pendingSortFields
                                                   ),
                                                   (
                                                       "approved",
                                                       "approved",
                                                       () => endpoints.approvedEndpointsImpl,
                                                       approvedSortFields
                                                   )
                                               )
        field                               <- fields
        (suffix, descending)                <- directions
    do
        test(s"$label (sort=${field.name}$suffix)") {
            initWithHistories()
            runGet(
                endpointImpl(),
                s"http://test.com/v1/history/$path?sort=${field.name}$suffix",
                response =>
                    assertEquals(response.code, StatusCode.Ok)
                    val histories = checkResponse[Page[Seq[ExtendedHistory]]](response.body)
                    assert(histories.content.nonEmpty)
                    assertSortedBy(histories.content, field, descending)
            )
        }

    for (label, path, endpointImpl) <- Seq(
                                           ("pending", "pending", () => endpoints.pendingEndpointImpl),
                                           ("approved", "approved", () => endpoints.approvedEndpointsImpl)
                                       )
    do
        test(s"$label (no sort param defaults to creationTimestamp asc)") {
            initWithHistories()
            runGet(
                endpointImpl(),
                s"http://test.com/v1/history/$path",
                response =>
                    assertEquals(response.code, StatusCode.Ok)
                    val histories = checkResponse[Page[Seq[ExtendedHistory]]](response.body)
                    assert(histories.content.nonEmpty)
                    assertSortedBy(histories.content, pendingSortFields.head, descending = false)
            )
        }

        test(s"$label (invalid sort field returns 400 Bad Request)") {
            initWithHistories()
            for (sort, field) <- Seq("notAField" -> "notAField", "notAField,desc" -> "notAField", "" -> "")
            do
                runGet(
                    endpointImpl(),
                    s"http://test.com/v1/history/$path?sort=$sort",
                    response =>
                        assertEquals(response.code, StatusCode.BadRequest, s"sort='$sort'")
                        response.body match
                            case Left(json) =>
                                val error = decode[BadRequest](json).fold(e => fail(e.getMessage), identity)
                                assertEquals(error.message, s"`$field` is not a valid sort field")
                            case Right(_)   => fail(s"Expected an error body for sort='$sort'")
                )
        }

        test(s"$label (invalid sort direction falls back to default sort)") {
            initWithHistories()
            runGet(
                endpointImpl(),
                s"http://test.com/v1/history/$path?sort=creatorName,sideways",
                response =>
                    assertEquals(response.code, StatusCode.Ok)
                    val histories = checkResponse[Page[Seq[ExtendedHistory]]](response.body)
                    assert(histories.content.nonEmpty)
                    // Sort.fromString returns None for an unknown direction, so the default sort is applied
                    assertSortedBy(histories.content, pendingSortFields.head, descending = false)
            )
        }

    test("approvedCount") {
        initWithHistories()
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
        val root     = initShallowTree(4)
        val concept  = root.getChildConcepts.iterator().next()
        val create   = ConceptNameCreate(concept.getName, Strings.random(15), ConceptNameTypes.PRIMARY.getType)
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
        val history  = histories.head
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
                        val updatedConcept =
                            conceptService.findByName(concept.getName).getOrElse(fail("Concept not found"))
                        assertEquals(updatedConcept.name, create.newName)
    }

    test("reject primary concept name change") {
        val root         = initShallowTree(4)
        val concept      = root.getChildConcepts.iterator().next()
        val originalName = concept.getName
        val create       = ConceptNameCreate(concept.getName, Strings.random(15), ConceptNameTypes.PRIMARY.getType)
        val attempt1     = runWithUserAuth(
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
        val history  = histories.head
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
                        val updatedConcept =
                            conceptService.findByName(concept.getName).getOrElse(fail("Concept not found"))
                        assertEquals(updatedConcept.name, originalName)
    }

    test("approve (non-admin is denied)") {
        val history = buildHistory() match
            case Left(e)  => fail(e.getMessage)
            case Right(h) => h

        withRole(UserAccountRoles.MAINTENANCE)(jwt =>
            runPut(
                endpoints.approveEndpointImpl,
                s"http://test.com/v1/history/approve/${history.id.get}",
                "",
                response => assertEquals(response.code, StatusCode.Forbidden),
                jwt = jwt
            )
        )
        historyService.findById(history.id.get) match
            case Left(e)  => fail(e.getMessage)
            case Right(h) => assert(!h.approved)
    }

    test("reject (non-admin is denied)") {
        val history = buildHistory() match
            case Left(e)  => fail(e.getMessage)
            case Right(h) => h

        withRole(UserAccountRoles.MAINTENANCE)(jwt =>
            runPut(
                endpoints.rejectEndpointImpl,
                s"http://test.com/v1/history/reject/${history.id.get}",
                "",
                response => assertEquals(response.code, StatusCode.Forbidden),
                jwt = jwt
            )
        )
        historyService.findById(history.id.get) match
            case Left(e)  => fail(e.getMessage)
            case Right(h) => assert(h.processedTimestamp.isEmpty)
    }

    test("delete (no match)") {
        withRole(UserAccountRoles.ADMINISTRATOR)(jwt =>
            runDelete(
                endpoints.deleteEndpointImpl,
                s"http://test.com/v1/history/${Long.MaxValue}",
                response => assertEquals(response.code, StatusCode.NotFound),
                jwt = jwt
            )
        )
    }

    // --- Delete authorization: only a user who is currently an administrator (no API key)

    private def pendingHistory(): ExtendedHistory =
        buildHistory() match
            case Left(e)  => fail(e.getMessage)
            case Right(h) => h

    test("delete (API key is denied)") {
        val history = pendingHistory()
        runDelete(
            endpoints.deleteEndpointImpl,
            s"http://test.com/v1/history/${history.id.get}",
            response => assertEquals(response.code, StatusCode.Forbidden),
            jwt = jwtService.authorize(jwtService.apiKey)
        )
        assert(historyService.findById(history.id.get).isRight, "History record should still exist")
    }

    test("delete (maintainer is denied)") {
        val history = pendingHistory()
        withRole(UserAccountRoles.MAINTENANCE)(jwt =>
            runDelete(
                endpoints.deleteEndpointImpl,
                s"http://test.com/v1/history/${history.id.get}",
                response => assertEquals(response.code, StatusCode.Forbidden),
                jwt = jwt
            )
        )
        assert(historyService.findById(history.id.get).isRight, "History record should still exist")
    }

    test("delete (admin demoted after login is denied)") {
        // The token still claims role=Admin, but the database says Maint. The database must win.
        val history  = pendingHistory()
        val password = Strings.random(12)
        val entity   = org
            .mbari
            .oni
            .jpa
            .entities
            .TestEntityFactory
            .createUserAccount(UserAccountRoles.ADMINISTRATOR.getRoleName, password)

        userAccountService.create(org.mbari.oni.domain.UserAccount.from(entity)) match
            case Left(error) => fail(error.toString)
            case Right(user) =>
                try
                    val jwt    = jwtService.login(user.username, password, user.toEntity)
                    assert(jwt.isDefined)
                    val demote =
                        org.mbari.oni.domain.UserAccountUpdate(role = Some(UserAccountRoles.MAINTENANCE.getRoleName))
                    assert(userAccountService.update(user.username, demote).isRight)
                    runDelete(
                        endpoints.deleteEndpointImpl,
                        s"http://test.com/v1/history/${history.id.get}",
                        response => assertEquals(response.code, StatusCode.Forbidden),
                        jwt = jwt
                    )
                finally userAccountService.deleteByUserName(user.username)

        assert(historyService.findById(history.id.get).isRight, "History record should still exist")
    }

    test("delete (no token)") {
        val history = pendingHistory()
        runDelete(
            endpoints.deleteEndpointImpl,
            s"http://test.com/v1/history/${history.id.get}",
            response => assertEquals(response.code, StatusCode.Unauthorized)
        )
        assert(historyService.findById(history.id.get).isRight, "History record should still exist")
    }
