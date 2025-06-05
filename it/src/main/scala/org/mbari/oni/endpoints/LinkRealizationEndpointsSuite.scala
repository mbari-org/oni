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

import org.mbari.oni.domain.{Count, ExtendedLink, ILink, LinkCreate, LinkUpdate, Page}
import org.mbari.oni.etc.circe.CirceCodecs.{*, given}
import org.mbari.oni.etc.jdk.Strings
import org.mbari.oni.etc.jwt.JwtService
import org.mbari.oni.jpa.DataInitializer
import org.mbari.oni.jpa.entities.TestEntityFactory
import org.mbari.oni.services.UserAuthMixin
import sttp.model.StatusCode

import scala.jdk.CollectionConverters.*

trait LinkRealizationEndpointsSuite extends EndpointsSuite with DataInitializer with UserAuthMixin:

    given jwtService: JwtService = JwtService("mbari", "foo", "bar")

    lazy val endpoints   = new LinkRealizationEndpoints(entityManagerFactory)
    private val password = "foofoofoo"

    def createLinkRealizations(): Seq[ExtendedLink] =
        val root = init(1, 6)
        root.getDescendants
            .asScala
            .flatMap(_.getConceptMetadata.getLinkRealizations.asScala)
            .toSeq
            .map(ExtendedLink.from)
            .sortBy(_.linkName)

    test("findLinkRealizationsByConceptName") {
        val links       = createLinkRealizations()
        val conceptName = links.head.concept
        runGet(
            endpoints.findLinkRealizationsByConceptNameImpl,
            s"http://test.com/v1/linkrealizations/concept/$conceptName",
            response =>
//                println(response.body)
                assertEquals(response.code, StatusCode.Ok)
                val obtained = checkResponse[Seq[ExtendedLink]](response.body)
                    .sortBy(_.linkName)
                assertEquals(obtained.size, links.size)
                assertEquals(obtained, links)
        )
    }

    test("findLinkRealizationsByLinkName") {
        val links    = createLinkRealizations()
        val linkName = links.head.linkName
        runGet(
            endpoints.findLinkRealizationsByLinkNameImpl,
            s"http://test.com/v1/linkrealizations/query/linkname/$linkName",
            response =>
                assertEquals(response.code, StatusCode.Ok)
                val obtained = checkResponse[Seq[ExtendedLink]](response.body)
                    .sortBy(_.linkName)
                var expected = links.filter(_.linkName == linkName)
                assertEquals(obtained.size, expected.size)
                assertEquals(obtained, expected)
        )
    }

    test("findLinkRealizationByPrototype") {
        val links     = createLinkRealizations()
        val link      = links.head
        val prototype = link.toLink
        runPost(
            endpoints.findLinkRealizationByPrototypeImpl,
            s"http://test.com/v1/linkrealizations/prototype",
            prototype.stringify,
            response =>
//                println(response.body)
                assertEquals(response.code, StatusCode.Ok)
                val obtained = checkResponse[Seq[ExtendedLink]](response.body)
                assertEquals(obtained, Seq(link))
        )
    }

    test("countAllLinkRealizations") {
        val links = createLinkRealizations()
        runGet(
            endpoints.countAllLinkRealizationsImpl,
            "http://test.com/v1/linkrealizations/count",
            response =>
                assertEquals(response.code, StatusCode.Ok)
                val count = checkResponse[Count](response.body)
                assertEquals(count.count, links.size.toLong)
        )
    }

    test("findAllLinkRealizations") {
        val links = createLinkRealizations()
        runGet(
            endpoints.findAllLinkRealizationsImpl,
            "http://test.com/v1/linkrealizations",
            response =>
                //                println(response.body)
                assertEquals(response.code, StatusCode.Ok)
                val xs       = checkResponse[Page[Seq[ExtendedLink]]](response.body)
                val obtained = xs.content
                val expected = links.sortBy(_.linkName.toLowerCase())
                assertEquals(obtained.size, expected.size)
                assertEquals(obtained, expected)
        )
    }

    test("create") {
        val root            = init(1, 0)
        val linkRealization = TestEntityFactory.createLinkRealization()
        val linkCreate      =
            LinkCreate(root.getName, linkRealization.getLinkName, ILink.VALUE_SELF, linkRealization.getLinkValue)
        val attempt         = testWithUserAuth(user =>
            runPost(
                endpoints.createImpl,
                "http://test.com/v1/linkrealizations",
                linkCreate.stringify,
                response =>
//                    println(response.body)
                    assertEquals(response.code, StatusCode.Ok)
                    val obtained = checkResponse[ExtendedLink](response.body)
                    assertEquals(obtained, ExtendedLink.from(linkRealization))
            )
        )
    }
    test("update") {
        val links      = createLinkRealizations()
        val link       = links.head
        val linkUpdate = LinkUpdate(linkValue = Some(Strings.random(11)))
        val attempt    = testWithUserAuth(
            user =>
                runPut(
                    endpoints.updateImpl,
                    s"http://test.com/v1/linkrealizations/${link.id.get}",
                    linkUpdate.stringify,
                    response =>
//                    println(response.body)
                        assertEquals(response.code, StatusCode.Ok)
                        val obtained = checkResponse[ExtendedLink](response.body)
                        assertEquals(obtained.linkValue, linkUpdate.linkValue.getOrElse(""))
                    ,
                    jwt = jwtService.login(user.username, password, user.toEntity)
                ),
            password
        )

        attempt match
            case Left(value)  => fail(value.toString)
            case Right(value) => assert(true)
    }

    test("delete") {
        val links   = createLinkRealizations()
        val link    = links.head
        val attempt = testWithUserAuth(
            user =>
                runDelete(
                    endpoints.deleteImpl,
                    s"http://test.com/v1/linkrealizations/${link.id.get}",
                    response => assertEquals(response.code, StatusCode.Ok),
                    jwt = jwtService.login(user.username, password, user.toEntity)
                ),
            password
        )
        attempt match
            case Left(value)  => fail(value.toString)
            case Right(value) => assert(true)
    }

    test("findLinkRealizationById") {
        val links = createLinkRealizations()
        val link  = links.head
        runGet(
            endpoints.findLinkRealizationByIdImpl,
            s"http://test.com/v1/linkrealizations/${link.id.get}",
            response =>
//                println(response.body)
                assertEquals(response.code, StatusCode.Ok)
                val obtained = checkResponse[ExtendedLink](response.body)
                assertEquals(obtained, link)
        )
    }
