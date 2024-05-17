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

import jakarta.persistence.EntityManagerFactory
import org.mbari.oni.domain.SerdeConcept
import org.mbari.oni.jpa.EntityManagerFactories.*
import org.mbari.oni.jpa.{DataInitializer, DatabaseFunSuite}
import org.mbari.oni.etc.jdk.Loggers.given
import org.mbari.oni.jpa.entities.{ConceptEntity, TestEntityFactory}
import org.mbari.oni.services.ConceptService
import org.mbari.oni.etc.circe.CirceCodecs.{*, given}
import sttp.model.StatusCode

import java.util.concurrent.atomic.AtomicReference

trait PhylogenyEndpointsSuite extends EndpointsSuite with DataInitializer:

    lazy val endpoints = new PhylogenyEndpoints(entityManagerFactory)

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
