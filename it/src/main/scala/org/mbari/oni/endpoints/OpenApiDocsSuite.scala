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

import org.mbari.oni.etc.jwt.JwtService
import org.mbari.oni.cache.KnowledgebaseCacheFixture
import org.mbari.oni.jpa.DatabaseFunSuite
import sttp.apispec.openapi.{OpenAPI, Operation}
import sttp.tapir.docs.openapi.OpenAPIDocsInterpreter

import scala.concurrent.ExecutionContext

/**
 * Sanity checks on the generated OpenAPI document (the same endpoints that are served via Swagger).
 */
trait OpenApiDocsSuite extends DatabaseFunSuite with KnowledgebaseCacheFixture:

    given JwtService       = JwtService("mbari", "foo", "bar")
    given ExecutionContext = ExecutionContext.global

    lazy val openApi: OpenAPI =
        val emf       = entityManagerFactory
        val endpoints = List(
            AuthorizationEndpoints(emf),
            CacheEndpoints(emf),
            ConceptEndpoints(emf, knowledgebaseCache),
            ConceptNameEndpoints(emf),
            HealthEndpoints(),
            HistoryEndpoints(emf, fastPhylogenyService),
            LinkEndpoints(emf),
            LinkRealizationEndpoints(emf),
            LinkTemplateEndpoints(emf),
            MediaEndpoints(emf, fastPhylogenyService),
            PhylogenyEndpoints(fastPhylogenyService),
            PrefNodeEndpoints(emf),
            RawEndpoints(emf),
            ReferenceEndpoints(emf),
            UserAccountEndpoints(emf)
        ).flatMap(_.allImpl)
        OpenAPIDocsInterpreter().serverEndpointsToOpenAPI(endpoints, "oni", "test")

    /** (method path, operation) for every documented operation */
    lazy val operations: Seq[(String, Operation)] =
        openApi
            .paths
            .pathItems
            .toSeq
            .flatMap((path, item) =>
                Seq("GET" -> item.get, "POST" -> item.post, "PUT" -> item.put, "DELETE" -> item.delete)
                    .collect { case (method, Some(op)) => s"$method $path" -> op }
            )

    test("every operation has a description") {
        assert(operations.nonEmpty)
        val missing = operations.collect { case (key, op) if op.description.forall(_.isBlank) => key }
        assert(missing.isEmpty, s"Operations without a description:\n  ${missing.sorted.mkString("\n  ")}")
    }

    test("operationIds are unique") {
        val duplicates = operations
            .flatMap((key, op) => op.operationId.map(_ -> key))
            .groupBy(_._1)
            .collect { case (id, keys) if keys.size > 1 => s"$id: ${keys.map(_._2).mkString(", ")}" }
        assert(duplicates.isEmpty, s"Duplicate operationIds:\n  ${duplicates.mkString("\n  ")}")
    }

    test("path parameters are named") {
        // Tapir names unnamed path captures (path[Long] without a name) p1, p2, ... which is meaningless to clients
        val unnamed = openApi.paths.pathItems.keys.filter(_.matches(""".*\{p\d+\}.*""")).toSeq
        assert(unnamed.isEmpty, s"Paths with unnamed parameters:\n  ${unnamed.sorted.mkString("\n  ")}")
    }
