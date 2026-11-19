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

import org.mbari.oni.domain.{ConceptCreate, ConceptMetadata, ConceptUpdate, Rank, UserAccountRoles}
import org.mbari.oni.etc.circe.CirceCodecs.{*, given}
import org.mbari.oni.etc.jdk.Strings
import org.mbari.oni.etc.jwt.JwtService
import org.mbari.oni.jpa.DataInitializer
import org.mbari.oni.jpa.entities.TestEntityFactory
import org.mbari.oni.services.RankValidator
import sttp.model.StatusCode

import scala.jdk.CollectionConverters.*

trait ConceptEndpointsSuite extends EndpointsSuite with DataInitializer with EndpointsAuthMixin:

    given jwtService: JwtService         = JwtService("mbari", "foo", "bar")
    lazy val endpoints: ConceptEndpoints = ConceptEndpoints(entityManagerFactory)
    private val password                 = "foofoofoo"

    test("all") {
        val root  = init(3, 3)
        assert(root.getId != null)
        val names = root
            .getDescendants
            .asScala
            .flatMap(_.getConceptNames.asScala.map(_.getName))
            .toSeq
            .sorted

        runGet(
            endpoints.allEndpointImpl,
            "http://test.com/v1/concept",
            response =>
                assertEquals(response.code, StatusCode.Ok)
                val conceptNames = checkResponse[Seq[String]](response.body).sorted
                assertEquals(conceptNames, names)
        )

    }

    test("findParent") {
        val root  = init(2, 0)
        val child = root.getChildConcepts.iterator().next()
        val name  = child.getPrimaryConceptName.getName

        runGet(
            endpoints.findParentEndpointImpl,
            s"http://test.com/v1/concept/parent/${name}",
            response =>
                assertEquals(response.code, StatusCode.Ok)
                val obtained = checkResponse[ConceptMetadata](response.body)
                assertEquals(obtained.name, root.getPrimaryConceptName.getName)
        )
    }

    test("findParent (on root)") {
        val root = init(2, 0)
        val name = root.getPrimaryConceptName.getName

        runGet(
            endpoints.findParentEndpointImpl,
            s"http://test.com/v1/concept/parent/${name}",
            response => assertEquals(response.code, StatusCode.NotFound)
//                val obtained = checkResponse[ConceptMetadata](response.body)
//                assertEquals(obtained.name, root.getPrimaryConceptName.getName)
        )
    }

    test("findChildren") {
        val root = init(2, 2)
        val name = root.getPrimaryConceptName.getName

        runGet(
            endpoints.findChildrenEndpointImpl,
            s"http://test.com/v1/concept/children/${name}",
            response =>
                assertEquals(response.code, StatusCode.Ok)
                val candidates = checkResponse[Seq[ConceptMetadata]](response.body)
                val obtained   = candidates.map(_.name).sorted
                val expected   = root.getChildConcepts.asScala.map(_.getPrimaryConceptName.getName).toSeq.sorted
                assertEquals(obtained, expected)
        )
    }

    test("findByName") {
        val root  = init(2, 0)
        val child = root.getChildConcepts.iterator().next()
        val name  = child.getPrimaryConceptName.getName

        runGet(
            endpoints.findByNameImpl,
            s"http://test.com/v1/concept/${name}",
            response =>
                assertEquals(response.code, StatusCode.Ok)
                val concept = checkResponse[ConceptMetadata](response.body)
                assertEquals(concept.name, name)
        )
    }

    test("findByName (with escaped characters)") {
        val root = init(2, 0)
        val name = "SomeConceptWith/Slash"

        val entityManager = conceptService.entityManagerFactory.createEntityManager()
        entityManager.getTransaction.begin()
        val concept       = TestEntityFactory.buildNode(1)
        concept.getPrimaryConceptName.setName(name)
        root.addChildConcept(concept)
        entityManager.persist(concept)
        entityManager.getTransaction.commit()
        entityManager.close()

        runGet(
            endpoints.findByNameImpl,
            s"http://test.com/v1/concept/${name.replace("/", "%2F")}",
            response =>
                assertEquals(response.code, StatusCode.Ok)
                val obtained = checkResponse[ConceptMetadata](response.body)
                assertEquals(obtained.name, name)
        )
    }

    test("findByName (no match)") {
        val root = init(2, 0)
        val name = Strings.random(10)

        runGet(
            endpoints.findByNameImpl,
            s"http://test.com/v1/concept/${name}",
            response => assertEquals(response.code, StatusCode.NotFound)
        )
    }

    test("findById") {
        val root  = init(2, 0)
        val child = root.getChildConcepts.iterator().next()
        val id    = child.getId.longValue()

        runGet(
            endpoints.findByIdImpl,
            s"http://test.com/v1/concept/id/${id}",
            response =>
                assertEquals(response.code, StatusCode.Ok)
                val concept = checkResponse[ConceptMetadata](response.body)
                assertEquals(concept.id, Some(id))
                assertEquals(concept.name, child.getPrimaryConceptName.getName)
        )
    }

    test("findById (no match)") {
        val root = init(2, 0)

        runGet(
            endpoints.findByIdImpl,
            s"http://test.com/v1/concept/id/${Long.MaxValue}",
            response => assertEquals(response.code, StatusCode.NotFound)
        )
    }

    test("findById (non-numeric id)") {
        runGet(
            endpoints.findByIdImpl,
            "http://test.com/v1/concept/id/notanumber",
            response => assertEquals(response.code, StatusCode.BadRequest)
        )
    }

    test("findByNameContaining") {
        val root  = init(2, 0)
        val child = root.getChildConcepts.iterator().next()
        val name  = child.getPrimaryConceptName.getName
        val glob  = name.substring(2, 8)

        runGet(
            endpoints.findByNameContainingImpl,
            s"http://test.com/v1/concept/find/${glob}",
            response =>
                assertEquals(response.code, StatusCode.Ok)
                val concepts = checkResponse[Seq[ConceptMetadata]](response.body)
                val obtained = concepts.map(_.name)
                val expected = Seq(name)
                assertEquals(concepts.map(_.name), Seq(name))
        )
    }

    test("findRoot") {
        val root = init(2, 0)
        val name = root.getPrimaryConceptName.getName

        runGet(
            endpoints.findRootImpl,
            s"http://test.com/v1/concept/query/root",
            response =>
                assertEquals(response.code, StatusCode.Ok)
                val concept = checkResponse[ConceptMetadata](response.body)
                assertEquals(concept.name, name)
        )
    }

    test("create") {

        val attempt = testWithUserAuth(
            user =>
                val root                  = init(2, 0)
                val name                  = root.getPrimaryConceptName.getName
                val (rankLevel, rankName) = TestEntityFactory.randomRankLevelAndName()
                val expectedRank          = Some(s"${{ rankLevel.getOrElse("") }}${{ rankName.getOrElse("") }}")

                val conceptCreate = ConceptCreate(
                    "SomeChildConcept",
                    Some(root.getPrimaryConceptName.getName),
                    rankLevel = rankLevel,
                    rankName = rankName,
                    aphiaId = Some(54321L)
                )

                runPost(
                    endpoints.createEndpointImpl,
                    "http://test.com/v1/concept",
                    conceptCreate.stringify,
                    response =>
                        assertEquals(response.code, StatusCode.Ok)
                        val concept = checkResponse[ConceptMetadata](response.body)
                        assertEquals(concept.name, "SomeChildConcept")
                        assertEquals(concept.rank, expectedRank)
                    ,
                    jwt = jwtService.login(user.username, password, user.toEntity)
                )
            ,
            password
        )

        attempt match
            case Right(_)    => () // println("Success")
            case Left(error) => fail(error.toString)

    }

    test("update") {

        val attempt = testWithUserAuth(
            user =>
                val root                  = init(3, 0)
                val grandChild            = root.getChildConcepts.iterator().next().getChildConcepts.iterator().next()
                val (rankLevel, rankName) = TestEntityFactory.randomRankLevelAndName()
                val expectedRank          =
                    if rankLevel.isEmpty && rankName.isEmpty then None
                    else Some(s"${{ rankLevel.getOrElse("") }}${{ rankName.getOrElse("") }}")

                // println(expectedRank)
                val conceptUpdate = ConceptUpdate(
                    Some(root.getPrimaryConceptName.getName),
                    rankLevel = rankLevel.orElse(Some("")),
                    rankName = rankName.orElse(Some("")),
                    aphiaId = Some(543210L)
                )

                runPut(
                    endpoints.updateByNameEndpointImpl,
                    s"http://test.com/v1/concept/${grandChild.getPrimaryConceptName.getName}",
                    conceptUpdate.stringify,
                    response =>
                        assertEquals(response.code, StatusCode.Ok)
                        // println(response.body)
                        val concept = checkResponse[ConceptMetadata](response.body)
                        assertEquals(concept.name, grandChild.getPrimaryConceptName.getName)
                        assertEquals(concept.rank, expectedRank)
                    ,
                    jwt = jwtService.login(user.username, password, user.toEntity)
                )
            ,
            password
        )

        attempt match
            case Right(_)    => () // println("Success")
            case Left(error) => fail(error.toString)

    }

    test("delete") {

        val attempt = testWithUserAuth(
            user =>
                val root = init(3, 0)
                val name = root.getPrimaryConceptName.getName

                runDelete(
                    endpoints.deleteByNameEndpointImpl,
                    s"http://test.com/v1/concept/${name}",
                    response => assertEquals(response.code, StatusCode.Ok),
                    jwt = jwtService.login(user.username, password, user.toEntity)
                )
            ,
            password
        )

        attempt match
            case Right(_)    => () // println("Success")
            case Left(error) => fail(error.toString)
    }

    test("updateById") {

        val attempt = testWithUserAuth(
            user =>
                val root       = init(3, 0)
                val grandChild = root.getChildConcepts.iterator().next().getChildConcepts.iterator().next()
                val id         = grandChild.getId.longValue()

                val conceptUpdate = ConceptUpdate(
                    parentName = Some(root.getPrimaryConceptName.getName),
                    rankLevel = Some("sub"),
                    rankName = Some("genus"),
                    aphiaId = Some(98765L)
                )

                runPut(
                    endpoints.updateByIdEndpointImpl,
                    s"http://test.com/v1/concept/id/${id}",
                    conceptUpdate.stringify,
                    response =>
                        assertEquals(response.code, StatusCode.Ok)
                        val concept = checkResponse[ConceptMetadata](response.body)
                        assertEquals(concept.id, Some(id))
                        assertEquals(concept.name, grandChild.getPrimaryConceptName.getName)
                        assertEquals(concept.rank, Some("subgenus"))
                        assertEquals(concept.aphiaId, Some(98765L))
                    ,
                    jwt = jwtService.login(user.username, password, user.toEntity)
                )

                // Verify the parent was changed
                conceptService.findParentByChildName(grandChild.getPrimaryConceptName.getName) match
                    case Left(error)  => fail(error.toString)
                    case Right(found) => assertEquals(found.name, root.getPrimaryConceptName.getName)
            ,
            password
        )

        attempt match
            case Right(_)    => ()
            case Left(error) => fail(error.toString)

    }

    test("updateById (no match)") {

        val attempt = testWithUserAuth(
            user =>
                init(2, 0)
                runPut(
                    endpoints.updateByIdEndpointImpl,
                    s"http://test.com/v1/concept/id/${Long.MaxValue}",
                    ConceptUpdate(aphiaId = Some(1L)).stringify,
                    response => assertEquals(response.code, StatusCode.NotFound),
                    jwt = jwtService.login(user.username, password, user.toEntity)
                )
            ,
            password
        )

        attempt match
            case Right(_)    => ()
            case Left(error) => fail(error.toString)
    }

    test("updateById (no jwt)") {
        val root = init(2, 0)
        runPut(
            endpoints.updateByIdEndpointImpl,
            s"http://test.com/v1/concept/id/${root.getId}",
            ConceptUpdate(aphiaId = Some(1L)).stringify,
            response => assertEquals(response.code, StatusCode.Unauthorized)
        )
    }

    test("deleteById") {

        val attempt = testWithUserAuth(
            user =>
                val root      = init(3, 0)
                val child     = root.getChildConcepts.iterator().next()
                val childName = child.getPrimaryConceptName.getName

                runDelete(
                    endpoints.deleteByIdEndpointImpl,
                    s"http://test.com/v1/concept/id/${child.getId}",
                    response => assertEquals(response.code, StatusCode.Ok),
                    jwt = jwtService.login(user.username, password, user.toEntity)
                )

                // The child (and its descendants) should be gone, but the root should remain
                assert(conceptService.findByName(childName).isLeft)
                assert(conceptService.findById(child.getId).isLeft)
                conceptService.findRoot() match
                    case Left(error)  => fail(error.toString)
                    case Right(found) => assertEquals(found.name, root.getPrimaryConceptName.getName)
            ,
            password
        )

        attempt match
            case Right(_)    => ()
            case Left(error) => fail(error.toString)
    }

    test("deleteById (no match)") {

        val attempt = testWithUserAuth(
            user =>
                init(2, 0)
                runDelete(
                    endpoints.deleteByIdEndpointImpl,
                    s"http://test.com/v1/concept/id/${Long.MaxValue}",
                    response => assertEquals(response.code, StatusCode.NotFound),
                    jwt = jwtService.login(user.username, password, user.toEntity)
                )
            ,
            password
        )

        attempt match
            case Right(_)    => ()
            case Left(error) => fail(error.toString)
    }

    test("deleteById (non-admin)") {
        val root  = init(3, 0)
        val child = root.getChildConcepts.iterator().next()

        withRole(UserAccountRoles.MAINTENANCE)(jwt =>
            runDelete(
                endpoints.deleteByIdEndpointImpl,
                s"http://test.com/v1/concept/id/${child.getId}",
                response => assertEquals(response.code, StatusCode.Ok),
                jwt = jwt
            )
        )
        // A maintenance user only creates a pending delete; the concept must still exist
        assert(conceptService.findById(child.getId).isRight)
    }

    test("deleteById (read-only user is denied)") {
        val root  = init(3, 0)
        val child = root.getChildConcepts.iterator().next()

        withRole(UserAccountRoles.READONLY)(jwt =>
            runDelete(
                endpoints.deleteByIdEndpointImpl,
                s"http://test.com/v1/concept/id/${child.getId}",
                response => assertEquals(response.code, StatusCode.Unauthorized),
                jwt = jwt
            )
        )
        assert(conceptService.findById(child.getId).isRight)
    }

    test("deleteByName (read-only user is denied)") {
        val root  = init(3, 0)
        val child = root.getChildConcepts.iterator().next()

        withRole(UserAccountRoles.READONLY)(jwt =>
            runDelete(
                endpoints.deleteByNameEndpointImpl,
                s"http://test.com/v1/concept/${child.getName}",
                response => assertEquals(response.code, StatusCode.Unauthorized),
                jwt = jwt
            )
        )
        assert(conceptService.findByName(child.getName).isRight)
    }

    test("deleteByName (no match)") {
        init(2, 0)
        withRole(UserAccountRoles.ADMINISTRATOR)(jwt =>
            runDelete(
                endpoints.deleteByNameEndpointImpl,
                s"http://test.com/v1/concept/${Strings.random(12)}",
                response => assertEquals(response.code, StatusCode.NotFound),
                jwt = jwt
            )
        )
    }

    test("updateById (read-only user is denied)") {
        val root  = init(2, 0)
        val child = root.getChildConcepts.iterator().next()

        withRole(UserAccountRoles.READONLY)(jwt =>
            runPut(
                endpoints.updateByIdEndpointImpl,
                s"http://test.com/v1/concept/id/${child.getId}",
                ConceptUpdate(aphiaId = Some(13579L)).stringify,
                response => assertEquals(response.code, StatusCode.Unauthorized),
                jwt = jwt
            )
        )
        conceptService.findById(child.getId) match
            case Left(error)  => fail(error.toString)
            case Right(found) => assertNotEquals(found.aphiaId, Some(13579L))
    }

    test("updateByName (read-only user is denied)") {
        val root  = init(2, 0)
        val child = root.getChildConcepts.iterator().next()

        withRole(UserAccountRoles.READONLY)(jwt =>
            runPut(
                endpoints.updateByNameEndpointImpl,
                s"http://test.com/v1/concept/${child.getName}",
                ConceptUpdate(aphiaId = Some(13579L)).stringify,
                response => assertEquals(response.code, StatusCode.Unauthorized),
                jwt = jwt
            )
        )
        conceptService.findByName(child.getName) match
            case Left(error)  => fail(error.toString)
            case Right(found) => assertNotEquals(found.aphiaId, Some(13579L))
    }

    test("updateByName (no match)") {
        init(2, 0)
        withRole(UserAccountRoles.ADMINISTRATOR)(jwt =>
            runPut(
                endpoints.updateByNameEndpointImpl,
                s"http://test.com/v1/concept/${Strings.random(12)}",
                ConceptUpdate(aphiaId = Some(1L)).stringify,
                response => assertEquals(response.code, StatusCode.NotFound),
                jwt = jwt
            )
        )
    }

    test("create (read-only user is denied)") {
        val root = init(2, 0)
        val name = s"ReadOnlyChild-${Strings.random(6)}"

        withRole(UserAccountRoles.READONLY)(jwt =>
            runPost(
                endpoints.createEndpointImpl,
                "http://test.com/v1/concept",
                ConceptCreate(name, Some(root.getName)).stringify,
                response => assertEquals(response.code, StatusCode.Unauthorized),
                jwt = jwt
            )
        )
        assert(conceptService.findByName(name).isLeft)
    }

    test("deleteById (no jwt)") {
        val root = init(2, 0)
        runDelete(
            endpoints.deleteByIdEndpointImpl,
            s"http://test.com/v1/concept/id/${root.getId}",
            response => assertEquals(response.code, StatusCode.Unauthorized)
        )
        // Nothing should have been deleted
        assert(conceptService.findById(root.getId).isRight)
    }

    test("listValidRanks") {
        runGet(
            endpoints.listValidRanksImpl,
            "http://test.com/v1/concept/ranks",
            response =>
                assertEquals(response.code, StatusCode.Ok)
                val ranks = checkResponse[Seq[Rank]](response.body)
                assert(ranks.nonEmpty)
                assertEquals(ranks.size, RankValidator.ValidRanks.size)
        )
    }

    // --- Invalid concept changes return 400 Bad Request

    private def assertBadRequestPut(
        ep: sttp.tapir.server.ServerEndpoint[Any, scala.concurrent.Future],
        uri: String,
        update: ConceptUpdate
    ): Unit =
        withRole(UserAccountRoles.ADMINISTRATOR)(jwt =>
            runPut(ep, uri, update.stringify, response => assertEquals(response.code, StatusCode.BadRequest), jwt = jwt)
        )

    test("updateByName (cyclic parent returns 400)") {
        val root            = init(5, 0)
        val child           = root.getChildConcepts.iterator().next()
        val greatGrandChild = child.getChildConcepts.iterator().next().getChildConcepts.iterator().next()
        assertBadRequestPut(
            endpoints.updateByNameEndpointImpl,
            s"http://test.com/v1/concept/${child.getName}",
            ConceptUpdate(parentName = Some(greatGrandChild.getName))
        )
        conceptService.findParentByChildName(child.getName) match
            case Right(parent) => assertEquals(parent.name, root.getName)
            case Left(error)   => fail(error.toString)
    }

    test("updateById (setting the root's parent returns 400)") {
        val root  = init(3, 0)
        val child = root.getChildConcepts.iterator().next()
        assertBadRequestPut(
            endpoints.updateByIdEndpointImpl,
            s"http://test.com/v1/concept/id/${root.getId}",
            ConceptUpdate(parentName = Some(child.getName))
        )
    }

    test("updateById (invalid rank returns 400)") {
        val root  = init(2, 0)
        val child = root.getChildConcepts.iterator().next()
        assertBadRequestPut(
            endpoints.updateByIdEndpointImpl,
            s"http://test.com/v1/concept/id/${child.getId}",
            ConceptUpdate(rankLevel = Some("bogus"), rankName = Some("notarank"))
        )
    }

    test("updateById (duplicate aphiaId returns 400)") {
        val root       = init(3, 0)
        val child      = root.getChildConcepts.iterator().next()
        val grandChild = child.getChildConcepts.iterator().next()
        val aphiaId    = 900000000L + scala.util.Random.nextInt(1000000)

        withRole(UserAccountRoles.ADMINISTRATOR)(jwt =>
            runPut(
                endpoints.updateByIdEndpointImpl,
                s"http://test.com/v1/concept/id/${child.getId}",
                ConceptUpdate(aphiaId = Some(aphiaId)).stringify,
                response => assertEquals(response.code, StatusCode.Ok),
                jwt = jwt
            )
        )
        assertBadRequestPut(
            endpoints.updateByIdEndpointImpl,
            s"http://test.com/v1/concept/id/${grandChild.getId}",
            ConceptUpdate(aphiaId = Some(aphiaId))
        )
    }

    test("create (invalid rank returns 400)") {
        val root = init(2, 0)
        val name = s"BadRank-${Strings.random(6)}"
        withRole(UserAccountRoles.ADMINISTRATOR)(jwt =>
            runPost(
                endpoints.createEndpointImpl,
                "http://test.com/v1/concept",
                ConceptCreate(
                    name,
                    Some(root.getName),
                    rankLevel = Some("bogus"),
                    rankName = Some("notarank")
                ).stringify,
                response => assertEquals(response.code, StatusCode.BadRequest),
                jwt = jwt
            )
        )
        assert(conceptService.findByName(name).isLeft)
    }

    test("create (duplicate name returns 400)") {
        val root  = init(2, 0)
        val child = root.getChildConcepts.iterator().next()
        withRole(UserAccountRoles.ADMINISTRATOR)(jwt =>
            runPost(
                endpoints.createEndpointImpl,
                "http://test.com/v1/concept",
                ConceptCreate(child.getName, Some(root.getName)).stringify,
                response => assertEquals(response.code, StatusCode.BadRequest),
                jwt = jwt
            )
        )
    }

    test("create (second root returns 400)") {
        init(2, 0)
        val name = s"SecondRoot-${Strings.random(6)}"
        withRole(UserAccountRoles.ADMINISTRATOR)(jwt =>
            runPost(
                endpoints.createEndpointImpl,
                "http://test.com/v1/concept",
                ConceptCreate(name, None).stringify,
                response => assertEquals(response.code, StatusCode.BadRequest),
                jwt = jwt
            )
        )
        assert(conceptService.findByName(name).isLeft)
    }

    // --- Rank removal is admin-only

    private def initRootWithRank(): org.mbari.oni.jpa.entities.ConceptEntity =
        val root = TestEntityFactory.buildRoot(2)
        root.setRankName("genus")
        root.setRankLevel("sub")
        conceptService.init(root) match
            case Right(entity) => entity
            case Left(error)   => fail(error.toString)

    test("updateById (maintainer removing a rank is denied)") {
        val root = initRootWithRank()
        withRole(UserAccountRoles.MAINTENANCE)(jwt =>
            runPut(
                endpoints.updateByIdEndpointImpl,
                s"http://test.com/v1/concept/id/${root.getId}",
                ConceptUpdate(rankName = Some(""), rankLevel = Some("")).stringify,
                response => assertEquals(response.code, StatusCode.Unauthorized),
                jwt = jwt
            )
        )
        conceptService.findById(root.getId) match
            case Right(found) => assertEquals(found.rank, Some("subgenus"))
            case Left(error)  => fail(error.toString)
    }

    test("updateById (administrator removing a rank is allowed)") {
        val root = initRootWithRank()
        withRole(UserAccountRoles.ADMINISTRATOR)(jwt =>
            runPut(
                endpoints.updateByIdEndpointImpl,
                s"http://test.com/v1/concept/id/${root.getId}",
                ConceptUpdate(rankName = Some(""), rankLevel = Some("")).stringify,
                response =>
                    assertEquals(response.code, StatusCode.Ok)
                    val concept = checkResponse[ConceptMetadata](response.body)
                    assertEquals(concept.rank, None)
                ,
                jwt = jwt
            )
        )
    }
