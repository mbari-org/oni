/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2024
 *
 * oni code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential. 
 */

package org.mbari.oni.endpoints

import org.mbari.oni.domain.{Concept, ErrorMsg, Phylogeny, SerdeConcept}
import org.mbari.oni.jdbc.FastPhylogenyService
import sttp.tapir.*
import sttp.tapir.server.ServerEndpoint
import sttp.shared.Identity
import org.mbari.oni.etc.circe.CirceCodecs.{*, given}
import CustomTapirJsonCirce.*

import scala.util.Try
import jakarta.persistence.EntityManagerFactory

class PhylogenyEndpoints(entityManagerFactory: EntityManagerFactory) extends Endpoints:

    /** This services does caching so we should share it  */
    val service: FastPhylogenyService = FastPhylogenyService(entityManagerFactory)
    private val base    = "phylogeny"
    private val tag     = "Phylogeny"

    val upEndpoint: Endpoint[Unit, String, ErrorMsg, SerdeConcept, Any] = openEndpoint
        .get
        .in(base / "up" / path[String]("name"))
        .out(jsonBody[SerdeConcept])
        .name("phylogenyUp")
        .description("Find the branch from a given concept up to the root")
        .tag(tag)

    val upEndpointImpl: ServerEndpoint[Any, Identity] = upEndpoint.serverLogic { name =>
        handleOption(service.findUp(name).map(SerdeConcept.from))
    }

    val downEndpoint: Endpoint[Unit, String, ErrorMsg, SerdeConcept, Any] = openEndpoint
        .get
        .in(base / "down" / path[String]("name"))
        .out(jsonBody[SerdeConcept])
        .description("Find the branch from the root down to a given concept")
        .tag(tag)

    val downEndpointImpl: ServerEndpoint[Any, Identity] = downEndpoint.serverLogic { name =>
        handleOption(service.findDown(name).map(SerdeConcept.from))
    }

    val siblingsEndpoint: Endpoint[Unit, String, ErrorMsg, Seq[SerdeConcept], Any] = openEndpoint
        .get
        .in(base / "siblings" / path[String]("name"))
        .out(jsonBody[Seq[SerdeConcept]])
        .tag(tag)

    val siblingsEndpointImpl: ServerEndpoint[Any, Identity] = siblingsEndpoint.serverLogic { name =>
        handleErrors(
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

    val basicEndpointImpl: ServerEndpoint[Any, Identity] = basicEndpoint.serverLogic { name =>
        handleOption(service.findUp(name).map(c => SerdeConcept.from(c).flatten.map(_.copy(children = None))))
    }

    val taxaEndpoint: Endpoint[Unit, String, ErrorMsg, Seq[SerdeConcept], Any] =
        openEndpoint
            .get
            .in(base / "taxa" / path[String]("name"))
            .out(jsonBody[Seq[SerdeConcept]])
            .tag(tag)

    val taxaEndpointImpl: ServerEndpoint[Any, Identity] = taxaEndpoint.serverLogic { name =>
        handleOption(
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

    override def all: List[Endpoint[_, _, _, _, _]] = List(
        upEndpoint,
        downEndpoint,
        siblingsEndpoint,
        basicEndpoint,
        taxaEndpoint
    )

    override def allImpl: List[ServerEndpoint[Any, Identity]] = List(
        upEndpointImpl,
        downEndpointImpl,
        siblingsEndpointImpl,
        basicEndpointImpl,
        taxaEndpointImpl
    )
