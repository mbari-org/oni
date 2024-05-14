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

import org.mbari.oni.domain.{ConceptMetadata, SerdeConcept}
import org.mbari.oni.jpa.DataInitializer

import scala.jdk.CollectionConverters.*
import org.mbari.oni.etc.circe.CirceCodecs.{*, given}
import sttp.model.StatusCode

trait ConceptEndpointsSuite extends EndpointsSuite with DataInitializer:

    lazy val endpoints: ConceptEndpoints = ConceptEndpoints(entityManagerFactory)

    test("all") {
        val root = init(3, 3)
        assert(root.getId != null)
        val names = root.getDescendants
            .asScala
            .flatMap(_.getConceptNames.asScala.map(_.getName))
            .toSeq
            .sorted

        runGet(
            endpoints.allEndpointImpl,
            "http://test.com/v1/concept",
            response => {
                assertEquals(response.code, StatusCode.Ok)
                val conceptNames = checkResponse[Seq[String]](response.body).sorted
                assertEquals(conceptNames, names)
            }
        )

    }

    test("findParent") {
        val root = init(2, 0)
        val child = root.getChildConcepts.iterator().next()
        val name = child.getPrimaryConceptName.getName

        runGet(
            endpoints.findParentEndpointImpl,
            s"http://test.com/v1/concept/parent/${name}",
            response => {
                assertEquals(response.code, StatusCode.Ok)
                val obtained = checkResponse[ConceptMetadata](response.body)
                assertEquals(obtained.name, root.getPrimaryConceptName.getName)
            }
         )
    }

    test("findChildren") {
        val root = init(2, 2)
        val name = root.getPrimaryConceptName.getName

        runGet(
            endpoints.findChildrenEndpointImpl,
            s"http://test.com/v1/concept/children/${name}",
            response => {
                assertEquals(response.code, StatusCode.Ok)
                val candidates = checkResponse[Seq[ConceptMetadata]](response.body)
                val obtained = candidates.map(_.name).sorted
                val expected = root.getChildConcepts.asScala.map(_.getPrimaryConceptName.getName).toSeq.sorted
                assertEquals(obtained, expected)
             }
        )
    }

    test("findByName") {
        val root = init(2, 0)
        val child = root.getChildConcepts.iterator().next()
        val name = child.getPrimaryConceptName.getName


        runGet(
            endpoints.findByNameImpl,
            s"http://test.com/v1/concept/${name}",
            response => {
                assertEquals(response.code, StatusCode.Ok)
                val concept = checkResponse[ConceptMetadata](response.body)
                assertEquals(concept.name, name)
            }
        )
    }

    test("findByNameContaining") {
        val root = init(2, 0)
        val child = root.getChildConcepts.iterator().next()
        val name = child.getPrimaryConceptName.getName
        val glob = name.substring(2, 8)

        runGet(
            endpoints.findByNameContainingImpl,
            s"http://test.com/v1/concept/find/${glob}",
            response => {
                assertEquals(response.code, StatusCode.Ok)
                val concepts = checkResponse[Seq[ConceptMetadata]](response.body)
                val obtained = concepts.map(_.name)
                val expected = Seq(name)
                assertEquals(concepts.map(_.name), Seq(name))
            }
        )
    }

