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

import org.mbari.oni.domain.{ExtendedLink, LinkCreate, LinkUpdate}
import org.mbari.oni.etc.jwt.JwtService
import org.mbari.oni.jpa.DataInitializer
import org.mbari.oni.services.UserAuthMixin
import sttp.model.StatusCode
import org.mbari.oni.etc.circe.CirceCodecs.{*, given}
import org.mbari.oni.etc.jdk.Strings
import org.mbari.oni.jpa.entities.TestEntityFactory

import scala.jdk.CollectionConverters.*

trait LinkTemplateEndpointsSuite extends EndpointsSuite with DataInitializer with UserAuthMixin:

    given jwtService: JwtService         = JwtService("mbari", "foo", "bar")
    lazy val endpoints = new LinkTemplateEndpoints(entityManagerFactory)
    private val password = "foofoofoo"

    def createLinkTemplates(): Seq[ExtendedLink] = {
        val root = init(1, 6)
        root.getDescendants
            .asScala
            .flatMap(_.getConceptMetadata.getLinkTemplates.asScala)
            .toSeq
            .map(ExtendedLink.from)
            .sortBy(_.linkName)
    }

    test("findLinkTemplatesByConceptName") {
        val links = createLinkTemplates()
        val conceptName = links.head.concept
        runGet(
            endpoints.findLinkTemplateByConceptNameImpl,
            s"http://test.com/v1/linktemplates/concept/$conceptName",
            response => {
                println(response.body)
                assertEquals(response.code, StatusCode.Ok)
                val obtained = checkResponse[Seq[ExtendedLink]](response.body)
                    .sortBy(_.linkName)
                assertEquals(obtained.size, links.size)
                assertEquals(obtained, links)
            }
        )
    }

    test("findLinkTemplateByPrototype") {
        val links = createLinkTemplates()
        val link = links.head
        val prototype = link.toLink
        runPost(
            endpoints.findLinkTemplateByPrototypeImpl,
            "http://test.com/v1/linktemplates/prototype",
            prototype.stringify,
            response => {
                println(response.body)
                assertEquals(response.code, StatusCode.Ok)
                val obtained = checkResponse[Seq[ExtendedLink]](response.body)
                assertEquals(obtained, Seq(link))
            }
        )
    }

    test("create") {
        val root = init(1, 0)
        val linkTemplate = TestEntityFactory.createLinkTemplate()
        val linkCreate = LinkCreate(root.getName, linkTemplate.getLinkName, linkTemplate.getLinkValue)
        val attempt = testWithUserAuth(user =>
            runPost(
                endpoints.createLinkTemplateImpl,
                "http://test.com/v1/linktemplates",
                linkCreate.stringify,
                response => {
                    println(response.body)
                    assertEquals(response.code, StatusCode.Ok)
                    val obtained = checkResponse[ExtendedLink](response.body)
                    assertEquals(obtained.toLink, linkCreate.toLink)
                },
                jwt = jwtService.login(user.username, password, user.toEntity)
            ),
            password
        )
        attempt match
            case Left(value)  => fail(value.toString)
            case Right(value) => assert(true)
    }

    test("update") {
        val links = createLinkTemplates()
        val link = links.head
        val linkUpdate = LinkUpdate(linkValue = Some(Strings.random(11)))
        val attempt = testWithUserAuth(user =>
            runPut(
                endpoints.updateLinkTemplateImpl,
                s"http://test.com/v1/linktemplates/${link.id.get}",
                linkUpdate.stringify,
                response => {
                    println(response.body)
                    assertEquals(response.code, StatusCode.Ok)
                    val obtained = checkResponse[ExtendedLink](response.body)
                    assertEquals(obtained.linkValue, linkUpdate.linkValue.getOrElse(""))
                },
                jwt = jwtService.login(user.username, password, user.toEntity)
            ),
            password
        )

    }

    test("delete") {
        val links = createLinkTemplates()
        val link = links.head
        val attempt = testWithUserAuth(user =>
            runDelete(
                endpoints.deleteLinkTemplateImpl,
                s"http://test.com/v1/linktemplates/${link.id.get}",
                response => assertEquals(response.code, StatusCode.Ok),
                jwt = jwtService.login(user.username, password, user.toEntity)
            ),
            password
        )
        attempt match
            case Left(value)  => fail(value.toString)
            case Right(value) => assert(true)
    }

    test("findLinkTemplateById") {
        val links = createLinkTemplates()
        val link = links.head
        runGet(
            endpoints.findLinkTemplateByIdImpl,
            s"http://test.com/v1/linktemplates/${link.id.get}",
            response => {
                println(response.body)
                assertEquals(response.code, StatusCode.Ok)
                val obtained = checkResponse[ExtendedLink](response.body)
                assertEquals(obtained, link)
            }
        )
    }
