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

import org.mbari.oni.domain.{Concept, ErrorMsg, SerdeConcept, SimpleConcept}
import org.mbari.oni.endpoints.CustomTapirJsonCirce.*
import org.mbari.oni.etc.circe.CirceCodecs.given
import org.mbari.oni.jdbc.FastPhylogenyService
import sttp.tapir.*
import sttp.tapir.server.ServerEndpoint

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

/**
 * @param service
 *   The shared phylogeny service, backed by the application's single KnowledgebaseCache
 */
class PhylogenyEndpoints(val service: FastPhylogenyService)(using executionContext: ExecutionContext)
    extends Endpoints:
    private val base                  = "phylogeny"
    private val tag                   = "Phylogeny"

    // --- Shared response builders so the by-name and by-id endpoints always return the same shape
    private def upResponse(concept: Option[Concept]): Either[ErrorMsg, SerdeConcept] =
        handleOption(concept.map(SerdeConcept.from))

    private def downResponse(concept: Option[Concept]): Either[ErrorMsg, SerdeConcept] =
        handleOption(concept.map(SerdeConcept.from))

    private def siblingsResponse(siblings: => Seq[SimpleConcept]): Either[ErrorMsg, Seq[SerdeConcept]] =
        handleErrors(Try(siblings).toEither.map(_.map(SerdeConcept.from).toSeq))

    private def basicResponse(concept: Option[Concept]): Either[ErrorMsg, Seq[SerdeConcept]] =
        handleOption(concept.map(c => SerdeConcept.from(c).flatten.map(_.copy(children = None))))

    private def taxaResponse(concept: Option[Concept]): Either[ErrorMsg, Seq[SerdeConcept]] =
        handleOption(concept.map(c => SerdeConcept.from(c).flatten.map(_.copy(children = None)).sortBy(_.name)))

    private def byIdPath(segment: String) = base / segment / "id" / path[Long]("id").description("Concept id")

    val upEndpoint: Endpoint[Unit, String, ErrorMsg, SerdeConcept, Any] = openEndpoint
        .get
        .in(base / "up" / path[String]("name"))
        .out(jsonBody[SerdeConcept])
        .name("phylogenyUp")
        .description("Find the branch from a given concept up to the root")
        .tag(tag)

    val upEndpointImpl: ServerEndpoint[Any, Future] = upEndpoint.serverLogic { name =>
        Future(upResponse(service.findUp(name)))
    }

    val downEndpoint: Endpoint[Unit, String, ErrorMsg, SerdeConcept, Any] = openEndpoint
        .get
        .in(base / "down" / path[String]("name"))
        .out(jsonBody[SerdeConcept])
        .description("Find the branch from the given concept down to the leaves")
        .tag(tag)

    val downEndpointImpl: ServerEndpoint[Any, Future] = downEndpoint.serverLogic { name =>
        Future(downResponse(service.findDown(name)))
    }

    val siblingsEndpoint: Endpoint[Unit, String, ErrorMsg, Seq[SerdeConcept], Any] = openEndpoint
        .get
        .in(base / "siblings" / path[String]("name"))
        .out(jsonBody[Seq[SerdeConcept]])
        .description("Find the siblings of a concept: the children of its parent, including the concept itself")
        .tag(tag)

    val siblingsEndpointImpl: ServerEndpoint[Any, Future] = siblingsEndpoint.serverLogic { name =>
        Future(siblingsResponse(service.findSiblings(name)))
    }

    val basicEndpoint: Endpoint[Unit, String, ErrorMsg, Seq[SerdeConcept], Any] =
        openEndpoint
            .get
            .in(base / "basic" / path[String]("name"))
            .out(jsonBody[Seq[SerdeConcept]])
            .description("Find the lineage from the root down to a concept, as a flat list (children are omitted)")
            .tag(tag)

    val basicEndpointImpl: ServerEndpoint[Any, Future] = basicEndpoint.serverLogic { name =>
        Future(basicResponse(service.findUp(name)))
    }

    val taxaEndpoint: Endpoint[Unit, String, ErrorMsg, Seq[SerdeConcept], Any] =
        openEndpoint
            .get
            .in(base / "taxa" / path[String]("name"))
            .out(jsonBody[Seq[SerdeConcept]])
            .description("Find a concept and all of its descendants, as a flat list (children are omitted)")
            .tag(tag)

    val taxaEndpointImpl: ServerEndpoint[Any, Future] = taxaEndpoint.serverLogic { name =>
        Future(taxaResponse(service.findDown(name)))
    }

    // --- By concept id

    val upByIdEndpoint: Endpoint[Unit, Long, ErrorMsg, SerdeConcept, Any] = openEndpoint
        .get
        .in(byIdPath("up"))
        .out(jsonBody[SerdeConcept])
        .name("phylogenyUpById")
        .description("Find the branch from the concept with the given id up to the root")
        .tag(tag)

    val upByIdEndpointImpl: ServerEndpoint[Any, Future] = upByIdEndpoint.serverLogic { id =>
        Future(upResponse(service.findUpById(id)))
    }

    val downByIdEndpoint: Endpoint[Unit, Long, ErrorMsg, SerdeConcept, Any] = openEndpoint
        .get
        .in(byIdPath("down"))
        .out(jsonBody[SerdeConcept])
        .name("phylogenyDownById")
        .description("Find the branch from the concept with the given id down to the leaves")
        .tag(tag)

    val downByIdEndpointImpl: ServerEndpoint[Any, Future] = downByIdEndpoint.serverLogic { id =>
        Future(downResponse(service.findDownById(id)))
    }

    val siblingsByIdEndpoint: Endpoint[Unit, Long, ErrorMsg, Seq[SerdeConcept], Any] = openEndpoint
        .get
        .in(byIdPath("siblings"))
        .out(jsonBody[Seq[SerdeConcept]])
        .name("phylogenySiblingsById")
        .description(
            "Find the siblings of the concept with the given id: the children of its parent, including the concept itself"
        )
        .tag(tag)

    val siblingsByIdEndpointImpl: ServerEndpoint[Any, Future] = siblingsByIdEndpoint.serverLogic { id =>
        Future(siblingsResponse(service.findSiblingsById(id)))
    }

    val basicByIdEndpoint: Endpoint[Unit, Long, ErrorMsg, Seq[SerdeConcept], Any] = openEndpoint
        .get
        .in(byIdPath("basic"))
        .out(jsonBody[Seq[SerdeConcept]])
        .name("phylogenyBasicById")
        .description(
            "Find the lineage from the root down to the concept with the given id, as a flat list (children are omitted)"
        )
        .tag(tag)

    val basicByIdEndpointImpl: ServerEndpoint[Any, Future] = basicByIdEndpoint.serverLogic { id =>
        Future(basicResponse(service.findUpById(id)))
    }

    val taxaByIdEndpoint: Endpoint[Unit, Long, ErrorMsg, Seq[SerdeConcept], Any] = openEndpoint
        .get
        .in(byIdPath("taxa"))
        .out(jsonBody[Seq[SerdeConcept]])
        .name("phylogenyTaxaById")
        .description(
            "Find the concept with the given id and all of its descendants, as a flat list (children are omitted)"
        )
        .tag(tag)

    val taxaByIdEndpointImpl: ServerEndpoint[Any, Future] = taxaByIdEndpoint.serverLogic { id =>
        Future(taxaResponse(service.findDownById(id)))
    }

    override def all: List[Endpoint[?, ?, ?, ?, ?]] = List(
        upEndpoint,
        downEndpoint,
        siblingsEndpoint,
        basicEndpoint,
        taxaEndpoint,
        upByIdEndpoint,
        downByIdEndpoint,
        siblingsByIdEndpoint,
        basicByIdEndpoint,
        taxaByIdEndpoint
    )

    override def allImpl: List[ServerEndpoint[Any, Future]] = List(
        upEndpointImpl,
        downEndpointImpl,
        siblingsEndpointImpl,
        basicEndpointImpl,
        taxaEndpointImpl,
        upByIdEndpointImpl,
        downByIdEndpointImpl,
        siblingsByIdEndpointImpl,
        basicByIdEndpointImpl,
        taxaByIdEndpointImpl
    )
