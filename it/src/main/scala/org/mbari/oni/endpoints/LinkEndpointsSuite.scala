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

import org.mbari.oni.domain.{ExtendedLink, Link}
import org.mbari.oni.jpa.DataInitializer
import org.mbari.oni.services.{LinkService, UserAuthMixin}
import org.mbari.oni.etc.circe.CirceCodecs.{*, given}
import sttp.model.StatusCode

import scala.jdk.CollectionConverters.*

trait LinkEndpointsSuite extends EndpointsSuite with DataInitializer with UserAuthMixin:

    lazy val endpoints: LinkEndpoints = LinkEndpoints(entityManagerFactory)
    lazy val linkService = new LinkService(entityManagerFactory)

    def createLinkTemplates(): Seq[ExtendedLink] = {
        val root = init(1, 6)
        root.getDescendants
            .asScala
            .flatMap(_.getConceptMetadata.getLinkTemplates.asScala)
            .toSeq
            .map(ExtendedLink.from)
            .sortBy(_.linkName)

    }

    test("all") {
        val expected = createLinkTemplates().map(_.toLink)
        runGet(
            endpoints.allLinksEndpointImpl,
            "http://test.com/v1/links",
            response => {
                println(response.body)
                assertEquals(response.code, StatusCode.Ok)
                val obtained = checkResponse[Seq[Link]](response.body)
                    .map(_.copy(id = None)) // The expected dont' have the ids but the obtained do
                    .sortBy(_.linkName)
                assertEquals(expected.size, obtained.size)
                assertEquals(obtained, expected)
            }
        )
    }

    test("linksForConcept") {
        val links = createLinkTemplates()
        val conceptName = links.head.concept
        val expected = links.map(_.toLink)
        runGet(
            endpoints.linksForConceptEndpointImpl,
            s"http://test.com/v1/links/$conceptName",
            response => {
                assertEquals(response.code, StatusCode.Ok)
                val obtained = checkResponse[Seq[Link]](response.body)
                    .map(_.copy(id = None)) // The expected dont' have the ids but the obtained do
                    .sortBy(_.linkName)
                assertEquals(links.size, obtained.size)
                assertEquals(obtained, expected)
            }
        )
    }

    test("linksForConceptAndLinkName") {
        val links = createLinkTemplates()
        val link = links.head
        val conceptName = link.concept
        val expected = link.toLink
        runGet(
            endpoints.linksForConceptAndLinkNameEndpointImpl,
            s"http://test.com/v1/links/$conceptName/using/${link.linkName}",
            response => {
                assertEquals(response.code, StatusCode.Ok)
                val obtained = checkResponse[Seq[Link]](response.body)
                    .map(_.copy(id = None)) // The expected dont' have the ids but the obtained do
                    .sortBy(_.linkName)
                assertEquals(obtained.size, 1)
                assertEquals(obtained, Seq(expected))
            }
        )
    }

    test("linkRealizations") {
        val root = init(1, 10)
        val links = root.getConceptMetadata
            .getLinkRealizations
            .asScala
            .map(ExtendedLink.from)
            .toSeq
            .sortBy(_.linkName)
        val expected = links.head
        val linkName = expected.linkName
        println(s"http://test.com/v1/links/query/linkrealizations/$linkName")
        println(expected.stringify)
        runGet(
            endpoints.linkRealizationsEndpointImpl,
            s"http://test.com/v1/links/query/linkrealizations/$linkName",
            response => {
                println(response.body)
                assertEquals(response.code, StatusCode.Ok)
                val obtained = checkResponse[Seq[ExtendedLink]](response.body)
                    .map(_.copy(id = None)) // The expected dont' have the ids but the obtained do
                    .sortBy(_.linkName)

                assertEquals(obtained.size, 1)
                assertEquals(obtained, Seq(expected))
            }
        )
    }
