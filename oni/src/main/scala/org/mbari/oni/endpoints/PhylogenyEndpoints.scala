/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2024
 *
 * oni code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential. 
 */

package org.mbari.oni.endpoints

import org.mbari.oni.domain.{Concept, ErrorMsg, Phylogeny}
import org.mbari.oni.jdbc.FastPhylogenyService
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.server.nima.Id
import org.mbari.oni.etc.circe.CirceCodecs.{*, given}
import CustomTapirJsonCirce.*

import scala.jdk.OptionConverters.*
import scala.jdk.CollectionConverters.*
import scala.util.Try
import jakarta.persistence.EntityManagerFactory

class PhylogenyEndpoints(entityManagerFactory: EntityManagerFactory) extends Endpoints:

    private val service = FastPhylogenyService(entityManagerFactory)

    private val base = "phylogeny"
    private val tag  = "Phylogeny"

    val upEndpoint = openEndpoint
        .get
        .in(base / "up" / path[String]("name"))
        .out(jsonBody[Concept])
        .tag(tag)

    val upEndpointImpl: ServerEndpoint[Any, Id] = upEndpoint.serverLogic { name =>
        handleOption(service.findUp(name).map(Concept.from).toScala)
    }

    val downEndpoint: Endpoint[Unit, String, ErrorMsg, Concept, Any] = openEndpoint
        .get
        .in(base / "down" / path[String]("name"))
        .out(jsonBody[Concept])
        .tag(tag)

    val downEndpointImpl: ServerEndpoint[Any, Id] = downEndpoint.serverLogic { name =>
        handleOption(service.findDown(name).map(Concept.from).toScala)
    }

    val siblingsEndpoint: Endpoint[Unit, String, ErrorMsg, Seq[Concept], Any] = openEndpoint
        .get
        .in(base / "siblings" / path[String]("name"))
        .out(jsonBody[Seq[Concept]])
        .tag(tag)

    val siblingsEndpointImpl: ServerEndpoint[Any, Id] = siblingsEndpoint.serverLogic { name =>
        handleErrors(
            Try(service.findSiblings(name).asScala)
                .toEither
                .map(_.map(Concept.from).toSeq)
        )
    }

    val basicEndpoint =
        openEndpoint
            .get
            .in(base / "basic" / path[String]("name"))
            .out(jsonBody[Seq[Concept]])
            .tag(tag)

    val basicEndpointImpl: ServerEndpoint[Any, Id] = basicEndpoint.serverLogic { name =>
        handleOption(service.findUp(name).map(Concept.flatten).toScala)
    }

    val taxaEndpoint =
        openEndpoint
            .get
            .in(base / "taxa" / path[String]("name"))
            .out(jsonBody[Seq[Concept]])
            .tag(tag)

    val taxaEndpointImpl: ServerEndpoint[Any, Id] = taxaEndpoint.serverLogic { name =>
        handleOption(service.findDown(name).map(Concept.flatten).toScala)
    }

    override def all: List[Endpoint[_, _, _, _, _]] = List(
        upEndpoint,
        downEndpoint,
        siblingsEndpoint,
        basicEndpoint,
        taxaEndpoint
    )

    override def allImpl: List[ServerEndpoint[Any, Id]] = List(
        upEndpointImpl,
        downEndpointImpl,
        siblingsEndpointImpl,
        basicEndpointImpl,
        taxaEndpointImpl
    )