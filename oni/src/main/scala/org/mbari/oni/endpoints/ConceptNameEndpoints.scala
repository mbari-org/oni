/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2024
 *
 * oni code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential.
 */

package org.mbari.oni.endpoints

import jakarta.persistence.EntityManagerFactory
import org.mbari.oni.domain.{ConceptNameCreate, ConceptNameUpdate, ErrorMsg, Page, RawConcept}
import org.mbari.oni.etc.circe.CirceCodecs.given
import org.mbari.oni.etc.jwt.JwtService
import org.mbari.oni.services.ConceptNameService
import sttp.tapir.json.circe.*
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.{Endpoint, *}

import scala.concurrent.{ExecutionContext, Future}

class ConceptNameEndpoints(entityManagerFactory: EntityManagerFactory)(using
    jwtService: JwtService,
    executionContext: ExecutionContext
) extends Endpoints:

    private val service = ConceptNameService(entityManagerFactory)
    private val base    = "names"
    private val tag     = "ConceptName"

    val allEndpoint = openEndpoint
        .get
        .in(base)
        .in(paging)
        .out(jsonBody[Page[Seq[String]]])
        .name("allConceptNames")
        .description("Get all concept names")
        .tag(tag)

    val allEndpointImpl: ServerEndpoint[Any, Future] = allEndpoint.serverLogic { paging =>
        val limit  = paging.limit.getOrElse(10000)
        val offset = paging.offset.getOrElse(0)
        handleErrorsAsync(service.findAllNames(limit, offset).map(s => Page(s, limit, offset)))
    }

    val addConceptNameEndpoint: Endpoint[Option[String], ConceptNameCreate, ErrorMsg, RawConcept, Any] = secureEndpoint
        .post
        .in(base)
        .in(jsonBody[ConceptNameCreate])
        .out(jsonBody[RawConcept])
        .name("addConceptName")
        .description("Add a new concept name")
        .tag(tag)

    val addConceptNameEndpointImpl: ServerEndpoint[Any, Future] = addConceptNameEndpoint
        .serverSecurityLogic(jwtOpt => verifyLoginAsync(jwtOpt))
        .serverLogic { userAccount => dto =>
            handleErrorsAsync(service.addName(dto, userAccount.username))
        }

    val findConceptNameEndpoint: Endpoint[Unit, String, ErrorMsg, RawConcept, Any] = openEndpoint
        .get
        .in(base / path[String]("name"))
        .out(jsonBody[RawConcept])
        .name("findConceptName")
        .description("Find a concept name")
        .tag(tag)

    val findConceptNameEndpointImpl: ServerEndpoint[Any, Future] = findConceptNameEndpoint
        .serverLogic { name =>
            handleErrorsAsync(service.findByName(name))
        }

    val updateConceptNameEndpoint: Endpoint[Option[String], (String, ConceptNameUpdate), ErrorMsg, RawConcept, Any] =
        secureEndpoint
            .put
            .in(base / path[String]("name"))
            .in(jsonBody[ConceptNameUpdate])
            .out(jsonBody[RawConcept])
            .name("updateConceptName")
            .description("Update a concept name. To remove the author, set it to an empty string")
            .tag(tag)

    val updateConceptNameEndpointImpl: ServerEndpoint[Any, Future] = updateConceptNameEndpoint
        .serverSecurityLogic(jwtOpt => verifyLoginAsync(jwtOpt))
        .serverLogic { userAccount => (name, dto) =>
            handleErrorsAsync(service.updateName(name, dto, userAccount.username))
        }

    val deleteConceptNameEndpoint: Endpoint[Option[String], String, ErrorMsg, RawConcept, Any] = secureEndpoint
        .delete
        .in(base / path[String]("name"))
        .out(jsonBody[RawConcept])
        .name("deleteConceptName")
        .description("Delete a concept name")
        .tag(tag)

    val deleteConceptNameEndpointImpl: ServerEndpoint[Any, Future] = deleteConceptNameEndpoint
        .serverSecurityLogic(jwtOpt => verifyLoginAsync(jwtOpt))
        .serverLogic { userAccount => name =>
            handleErrorsAsync(service.deleteName(name, userAccount.username))
        }

    override def all: List[Endpoint[?, ?, ?, ?, ?]] = List(
        findConceptNameEndpoint,
        allEndpoint,
        addConceptNameEndpoint,
        updateConceptNameEndpoint,
        deleteConceptNameEndpoint
    )

    override def allImpl: List[ServerEndpoint[Any, Future]] = List(
        findConceptNameEndpointImpl,
        allEndpointImpl,
        addConceptNameEndpointImpl,
        updateConceptNameEndpointImpl,
        deleteConceptNameEndpointImpl
    )
