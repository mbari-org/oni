/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2024
 *
 * oni code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential. 
 */

package org.mbari.oni.endpoints

import jakarta.persistence.EntityManagerFactory
import sttp.tapir.*
import sttp.tapir.Endpoint
import sttp.tapir.json.circe.*
import sttp.tapir.server.ServerEndpoint
import org.mbari.oni.domain.{ConceptMetadata, ErrorMsg}
import org.mbari.oni.etc.circe.CirceCodecs.given
import org.mbari.oni.services.{ConceptNameService, ConceptService}
import sttp.tapir.server.nima.Id

class ConceptEndpoints(entityManagerFactory: EntityManagerFactory) extends Endpoints:

    private val service = ConceptService(entityManagerFactory)
    private val conceptNameService = ConceptNameService(entityManagerFactory)
    private val base = "concept"
    private val tag  = "Concept"

    val allEndpoint: Endpoint[Unit, Unit, ErrorMsg, Seq[String], Any] = openEndpoint
        .get
        .in(base)
        .out(jsonBody[Seq[String]])
        .name("allConcepts")
        .description("Get all concept names")
        .tag(tag)

    val allEndpointImpl: ServerEndpoint[Any, Id] = allEndpoint.serverLogic {
        _ => handleErrors(conceptNameService.findAllNames())
    }

    val findParentEndpoint: Endpoint[Unit, String, ErrorMsg, ConceptMetadata, Any] = openEndpoint
        .get
        .in(base / "parent" / path[String]("name"))
        .out(jsonBody[ConceptMetadata])
        .name("findParent")
        .description("Find the parent of a concept")
        .tag(tag)

    val findParentEndpointImpl: ServerEndpoint[Any, Id] = findParentEndpoint.serverLogic { name =>
        handleErrors(service.findParentByChildName(name))
    }

    val findChildrenEndpoint: Endpoint[Unit, String, ErrorMsg, Seq[ConceptMetadata], Any] = openEndpoint
        .get
        .in(base / "children" / path[String]("name"))
        .out(jsonBody[Seq[ConceptMetadata]])
        .name("findChildren")
        .description("Find the children of a concept")
        .tag(tag)

    val findChildrenEndpointImpl: ServerEndpoint[Any, Id] = findChildrenEndpoint.serverLogic { name =>
        handleErrors(service.findChildrenByParentName(name)).map(_.toSeq.sortBy(_.name))
    }

    val findByName: Endpoint[Unit, String, ErrorMsg, ConceptMetadata, Any] = openEndpoint
        .get
        .in(base / path[String]("name"))
        .out(jsonBody[ConceptMetadata])
        .name("findByName")
        .description("Find a concept by name")
        .tag(tag)

    val findByNameImpl: ServerEndpoint[Any, Id] = findByName.serverLogic { name =>
        handleErrors(service.findByName(name))
    }

    val findByNameContaining: Endpoint[Unit, String, ErrorMsg, Seq[ConceptMetadata], Any] = openEndpoint
        .get
        .in(base / "find" / path[String]("name"))
        .out(jsonBody[Seq[ConceptMetadata]])
        .name("findByNameContaining")
        .description("Find concepts by name containing")
        .tag(tag)

    val findByNameContainingImpl: ServerEndpoint[Any, Id] = findByNameContaining.serverLogic { name =>
        handleErrors(service.findByGlob(name)).map(_.toSeq.sortBy(_.name))
    }


    override val all: List[Endpoint[?, ?, ?, ?, ?]]     = List(
        findParentEndpoint,
        findChildrenEndpoint,
        findByNameContaining,
        findByName,
        allEndpoint
    )

    override val allImpl: List[ServerEndpoint[Any, Id]] = List(
        findParentEndpointImpl,
        findChildrenEndpointImpl,
        findByNameContainingImpl,
        findByNameImpl,
        allEndpointImpl
    )
