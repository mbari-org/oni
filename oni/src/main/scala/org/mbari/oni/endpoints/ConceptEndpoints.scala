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
import org.mbari.oni.domain.{ConceptCreate, ConceptDelete, ConceptMetadata, ConceptUpdate, ErrorMsg, ServerError}
import org.mbari.oni.etc.circe.CirceCodecs.given
import org.mbari.oni.etc.jwt.JwtService
import org.mbari.oni.services.{ConceptNameService, ConceptService}
import sttp.shared.Identity

class ConceptEndpoints(entityManagerFactory: EntityManagerFactory)(using jwtService: JwtService) extends Endpoints:

    private val service            = ConceptService(entityManagerFactory)
    private val conceptNameService = ConceptNameService(entityManagerFactory)
    private val base               = "concept"
    private val tag                = "Concept"

    val allEndpoint: Endpoint[Unit, Unit, ErrorMsg, Seq[String], Any] = openEndpoint
        .get
        .in(base)
        .out(jsonBody[Seq[String]])
        .name("allConcepts")
        .description("Get all concept names")
        .tag(tag)

    val allEndpointImpl: ServerEndpoint[Any, Identity] = allEndpoint.serverLogic { _ =>
        val limit  = 10000
        val offset = 0
        handleErrors(conceptNameService.findAllNames(limit, offset))
    }

    val createEndpoint: Endpoint[Option[String], ConceptCreate, ErrorMsg, ConceptMetadata, Any] = secureEndpoint
        .post
        .in(base)
        .in(jsonBody[ConceptCreate])
        .out(jsonBody[ConceptMetadata])
        .name("createConcept")
        .description("Create a new concept")
        .tag(tag)

    val createEndpointImpl: ServerEndpoint[Any, Identity] = createEndpoint
        .serverSecurityLogic(jwtOpt => verifyLogin(jwtOpt))
        .serverLogic { userAccount => conceptCreate =>
            service
                .create(conceptCreate, userAccount.username)
                .fold(
                    error => Left(ServerError(error.getMessage)),
                    concept => Right(concept)
                )
        }

    val deleteEndpoint: Endpoint[Option[String], String, ErrorMsg, Unit, Any] = secureEndpoint
        .delete
        .in(base / path[String]("name"))
        .out(jsonBody[Unit])
        .name("deleteConcept")
        .description("Delete a concept")
        .tag(tag)

    val deleteEndpointImpl: ServerEndpoint[Any, Identity] = deleteEndpoint
        .serverSecurityLogic(jwtOpt => verifyLogin(jwtOpt))
        .serverLogic { userAccount => name =>
            service
                .delete(name, userAccount.username)
                .fold(
                    error => Left(ServerError(error.getMessage)),
                    _ => Right(())
                )
        }

    val findParentEndpoint: Endpoint[Unit, String, ErrorMsg, ConceptMetadata, Any] = openEndpoint
        .get
        .in(base / "parent" / path[String]("name"))
        .out(jsonBody[ConceptMetadata])
        .name("findParent")
        .description("Find the parent of a concept")
        .tag(tag)

    val findParentEndpointImpl: ServerEndpoint[Any, Identity] = findParentEndpoint.serverLogic { name =>
        handleErrors(service.findParentByChildName(name))
    }

    val findChildrenEndpoint: Endpoint[Unit, String, ErrorMsg, Seq[ConceptMetadata], Any] = openEndpoint
        .get
        .in(base / "children" / path[String]("name"))
        .out(jsonBody[Seq[ConceptMetadata]])
        .name("findChildren")
        .description("Find the children of a concept")
        .tag(tag)

    val findChildrenEndpointImpl: ServerEndpoint[Any, Identity] = findChildrenEndpoint.serverLogic { name =>
        handleErrors(service.findChildrenByParentName(name)).map(_.toSeq.sortBy(_.name))
    }

    val findByName: Endpoint[Unit, String, ErrorMsg, ConceptMetadata, Any] = openEndpoint
        .get
        .in(base / path[String]("name"))
        .out(jsonBody[ConceptMetadata])
        .name("findByName")
        .description("Find a concept by name")
        .tag(tag)

    val findByNameImpl: ServerEndpoint[Any, Identity] = findByName.serverLogic { name =>
        handleErrors(service.findByName(name))
    }

    val findByNameContaining: Endpoint[Unit, String, ErrorMsg, Seq[ConceptMetadata], Any] = openEndpoint
        .get
        .in(base / "find" / path[String]("name"))
        .out(jsonBody[Seq[ConceptMetadata]])
        .name("findByNameContaining")
        .description("Find concepts by name containing")
        .tag(tag)

    val findByNameContainingImpl: ServerEndpoint[Any, Identity] = findByNameContaining.serverLogic { name =>
        handleErrors(service.findByGlob(name)).map(_.toSeq.sortBy(_.name))
    }

    val updateEndpoint: Endpoint[Option[String], (String, ConceptUpdate), ErrorMsg, ConceptMetadata, Any] =
        secureEndpoint
            .put
            .in(base / path[String]("name"))
            .in(jsonBody[ConceptUpdate])
            .out(jsonBody[ConceptMetadata])
            .name("updateConcept")
            .description("Update a concept")
            .tag(tag)

    val updateEndpointImpl: ServerEndpoint[Any, Identity] = updateEndpoint
        .serverSecurityLogic(jwtOpt => verifyLogin(jwtOpt))
        .serverLogic { userAccount => (name, conceptUpdate) =>
            service
                .update(name, conceptUpdate, userAccount.username)
                .fold(
                    error => Left(ServerError(error.getMessage)),
                    concept => Right(concept)
                )
        }

    override val all: List[Endpoint[?, ?, ?, ?, ?]] = List(
        findParentEndpoint,
        findChildrenEndpoint,
        findByNameContaining,
        findByName,
        allEndpoint,
        createEndpoint,
        deleteEndpoint,
        updateEndpoint
    )

    override val allImpl: List[ServerEndpoint[Any, Identity]] = List(
        findParentEndpointImpl,
        findChildrenEndpointImpl,
        findByNameContainingImpl,
        findByNameImpl,
        allEndpointImpl,
        createEndpointImpl,
        deleteEndpointImpl,
        updateEndpointImpl
    )
