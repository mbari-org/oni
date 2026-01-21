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
import org.mbari.oni.domain.{Concept, ErrorMsg, SerdeConcept}
import org.mbari.oni.endpoints.CustomTapirJsonCirce.*
import org.mbari.oni.etc.circe.CirceCodecs.given
import org.mbari.oni.jdbc.FastPhylogenyService
import sttp.tapir.*
import sttp.tapir.server.ServerEndpoint

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class PhylogenyEndpoints(entityManagerFactory: EntityManagerFactory)(using executionContext: ExecutionContext)
    extends Endpoints:

    /** This services does caching so we should share it */
    val service: FastPhylogenyService = FastPhylogenyService(entityManagerFactory)
    private val base                  = "phylogeny"
    private val tag                   = "Phylogeny"

    val upEndpoint: Endpoint[Unit, String, ErrorMsg, SerdeConcept, Any] = openEndpoint
        .get
        .in(base / "up" / path[String]("name"))
        .out(jsonBody[SerdeConcept])
        .name("phylogenyUp")
        .description("Find the branch from a given concept up to the root")
        .tag(tag)

    val upEndpointImpl: ServerEndpoint[Any, Future] = upEndpoint.serverLogic { name =>
        handleOptionAsync(service.findUp(name).map(SerdeConcept.from))
    }

    val downEndpoint: Endpoint[Unit, String, ErrorMsg, SerdeConcept, Any] = openEndpoint
        .get
        .in(base / "down" / path[String]("name"))
        .out(jsonBody[SerdeConcept])
        .description("Find the branch from the given concept down to the leaves")
        .tag(tag)

    val downEndpointImpl: ServerEndpoint[Any, Future] = downEndpoint.serverLogic { name =>
        handleOptionAsync(service.findDown(name).map(SerdeConcept.from))
    }

    val siblingsEndpoint: Endpoint[Unit, String, ErrorMsg, Seq[SerdeConcept], Any] = openEndpoint
        .get
        .in(base / "siblings" / path[String]("name"))
        .out(jsonBody[Seq[SerdeConcept]])
        .tag(tag)

    val siblingsEndpointImpl: ServerEndpoint[Any, Future] = siblingsEndpoint.serverLogic { name =>
        handleErrorsAsync(
            Try(service.findSiblings(name))
                .toEither
                .map(_.map(SerdeConcept.from).toSeq)
        )
    }

    val basicEndpoint: Endpoint[Unit, String, ErrorMsg, Seq[SerdeConcept], Any] =
        openEndpoint
            .get
            .in(base / "basic" / path[String]("name"))
            .out(jsonBody[Seq[SerdeConcept]])
            .tag(tag)

    val basicEndpointImpl: ServerEndpoint[Any, Future] = basicEndpoint.serverLogic { name =>
        handleOptionAsync(service.findUp(name).map(c => SerdeConcept.from(c).flatten.map(_.copy(children = None))))
    }

    val taxaEndpoint: Endpoint[Unit, String, ErrorMsg, Seq[SerdeConcept], Any] =
        openEndpoint
            .get
            .in(base / "taxa" / path[String]("name"))
            .out(jsonBody[Seq[SerdeConcept]])
            .tag(tag)

    val taxaEndpointImpl: ServerEndpoint[Any, Future] = taxaEndpoint.serverLogic { name =>
        handleOptionAsync(
            service
                .findDown(name)
                .map(c =>
                    SerdeConcept
                        .from(c)
                        .flatten
                        .map(_.copy(children = None))
                        .sortBy(_.name)
                )
        )
    }

    override def all: List[Endpoint[?, ?, ?, ?, ?]] = List(
        upEndpoint,
        downEndpoint,
        siblingsEndpoint,
        basicEndpoint,
        taxaEndpoint
    )

    override def allImpl: List[ServerEndpoint[Any, Future]] = List(
        upEndpointImpl,
        downEndpointImpl,
        siblingsEndpointImpl,
        basicEndpointImpl,
        taxaEndpointImpl
    )
