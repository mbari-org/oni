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

import org.mbari.oni.cache.KnowledgebaseCacheFixture

import org.mbari.oni.domain.SerdeConcept
import org.mbari.oni.etc.circe.CirceCodecs.given
import org.mbari.oni.jpa.DataInitializer
import sttp.model.StatusCode

trait PhylogenyEndpointsSuite extends EndpointsSuite with DataInitializer with KnowledgebaseCacheFixture:

    lazy val endpoints = new PhylogenyEndpoints(fastPhylogenyService)

    test("up") {
        val root  = init(4, 1)
        val child = root.getChildConcepts.iterator().next()
        val name  = child.getPrimaryConceptName.getName

        runGet(
            endpoints.upEndpointImpl,
            s"http://test.com/v1/phylogeny/up/$name",
            response =>
                assertEquals(response.code, StatusCode.Ok)
                val concept = checkResponse[SerdeConcept](response.body)
                assertEquals(concept.name, root.getPrimaryConceptName.getName)
        )
    }

    test("down") {
        val root = init(4, 1)
        val name = root.getPrimaryConceptName.getName
        runGet(
            endpoints.downEndpointImpl,
            s"http://test.com/v1/phylogeny/down/$name",
            response =>
                assertEquals(response.code, StatusCode.Ok)
                val concept = checkResponse[SerdeConcept](response.body)
                assertEquals(concept.name, root.getPrimaryConceptName.getName)
        )
    }

    test("siblings") {
        val root  = init(3, 3)
        val child = root.getChildConcepts.iterator().next()
        val name  = child.getPrimaryConceptName.getName
        runGet(
            endpoints.siblingsEndpointImpl,
            s"http://test.com/v1/phylogeny/siblings/$name",
            response =>
                assertEquals(response.code, StatusCode.Ok)
                val concepts = checkResponse[Seq[SerdeConcept]](response.body)
                assertEquals(concepts.size, root.getChildConcepts.size())
                val a        = concepts.find(_.name == name)
                assert(a.isDefined)
        )
    }

    test("basic") {
        val root  = init(4, 1)
        val child = root.getChildConcepts.iterator().next()
        val name  = child.getPrimaryConceptName.getName
        runGet(
            endpoints.basicEndpointImpl,
            s"http://test.com/v1/phylogeny/basic/$name",
            response =>
                assertEquals(response.code, StatusCode.Ok)
                val concepts = checkResponse[Seq[SerdeConcept]](response.body)
                assertEquals(concepts.size, 2)
                val a        = concepts.find(_.name == name)
                assert(a.isDefined)
        )
    }

    test("taxa") {
        val root = init(4, 1)
        val name = root.getPrimaryConceptName.getName
        runGet(
            endpoints.taxaEndpointImpl,
            s"http://test.com/v1/phylogeny/taxa/$name",
            response =>
                assertEquals(response.code, StatusCode.Ok)
                val concepts = checkResponse[Seq[SerdeConcept]](response.body)
                assertEquals(concepts.size, 4)
                val a        = concepts.find(_.name == name)
                assert(a.isDefined)
        )
    }

    // --- By concept id: each must return exactly what the by-name endpoint returns for the same concept

    private def bodyOf(ep: sttp.tapir.server.ServerEndpoint[Any, scala.concurrent.Future], uri: String): String =
        var body = ""
        runGet(
            ep,
            uri,
            response =>
                assertEquals(response.code, StatusCode.Ok, s"GET $uri")
                body = response.body.fold(e => fail(s"GET $uri failed: $e"), identity)
        )
        body

    private def assertSameAsByName[T: io.circe.Decoder](
        byName: sttp.tapir.server.ServerEndpoint[Any, scala.concurrent.Future],
        byId: sttp.tapir.server.ServerEndpoint[Any, scala.concurrent.Future],
        segment: String,
        concept: org.mbari.oni.jpa.entities.ConceptEntity
    ): T =
        val name     = concept.getPrimaryConceptName.getName
        val expected = checkResponse[T](Right(bodyOf(byName, s"http://test.com/v1/phylogeny/$segment/$name")))
        val obtained = checkResponse[T](Right(bodyOf(byId, s"http://test.com/v1/phylogeny/$segment/id/${concept.getId}")))
        assertEquals(obtained, expected)
        obtained

    test("upById") {
        val root  = init(4, 1)
        val child = root.getChildConcepts.iterator().next()
        val c     = assertSameAsByName[SerdeConcept](endpoints.upEndpointImpl, endpoints.upByIdEndpointImpl, "up", child)
        assertEquals(c.name, root.getPrimaryConceptName.getName)
    }

    test("downById") {
        val root = init(4, 1)
        val c    = assertSameAsByName[SerdeConcept](endpoints.downEndpointImpl, endpoints.downByIdEndpointImpl, "down", root)
        assertEquals(c.name, root.getPrimaryConceptName.getName)
    }

    test("siblingsById") {
        val root  = init(3, 3)
        val child = root.getChildConcepts.iterator().next()
        val cs    = assertSameAsByName[Seq[SerdeConcept]](
            endpoints.siblingsEndpointImpl,
            endpoints.siblingsByIdEndpointImpl,
            "siblings",
            child
        )
        assertEquals(cs.size, root.getChildConcepts.size())
    }

    test("basicById") {
        val root  = init(4, 1)
        val child = root.getChildConcepts.iterator().next()
        val cs    =
            assertSameAsByName[Seq[SerdeConcept]](endpoints.basicEndpointImpl, endpoints.basicByIdEndpointImpl, "basic", child)
        assertEquals(cs.size, 2)
    }

    test("taxaById") {
        val root = init(4, 1)
        val cs   =
            assertSameAsByName[Seq[SerdeConcept]](endpoints.taxaEndpointImpl, endpoints.taxaByIdEndpointImpl, "taxa", root)
        assertEquals(cs.size, 4)
    }

    test("byId endpoints return 404 for an unknown id") {
        init(2, 0)
        for (segment, ep) <- Seq(
                                 "up"    -> endpoints.upByIdEndpointImpl,
                                 "down"  -> endpoints.downByIdEndpointImpl,
                                 "basic" -> endpoints.basicByIdEndpointImpl,
                                 "taxa"  -> endpoints.taxaByIdEndpointImpl
                             )
        do
            runGet(
                ep,
                s"http://test.com/v1/phylogeny/$segment/id/${Long.MaxValue}",
                response => assertEquals(response.code, StatusCode.NotFound, segment)
            )
    }

    test("siblingsById returns an empty list for an unknown id (same as siblings by name)") {
        init(2, 0)
        runGet(
            endpoints.siblingsByIdEndpointImpl,
            s"http://test.com/v1/phylogeny/siblings/id/${Long.MaxValue}",
            response =>
                assertEquals(response.code, StatusCode.Ok)
                assertEquals(checkResponse[Seq[SerdeConcept]](response.body), Nil)
        )
    }

    test("byId endpoints return 400 for a non-numeric id") {
        runGet(
            endpoints.upByIdEndpointImpl,
            "http://test.com/v1/phylogeny/up/id/notanumber",
            response => assertEquals(response.code, StatusCode.BadRequest)
        )
    }
