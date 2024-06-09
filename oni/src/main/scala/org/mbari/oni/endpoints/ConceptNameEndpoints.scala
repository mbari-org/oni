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
import org.mbari.oni.domain.{ConceptNameCreate, ConceptNameUpdate, ErrorMsg, Page, RawConcept, RawConceptName}
import org.mbari.oni.etc.jwt.JwtService
import org.mbari.oni.services.ConceptNameService
import sttp.shared.Identity
import org.mbari.oni.etc.circe.CirceCodecs.given

class ConceptNameEndpoints(entityManagerFactory: EntityManagerFactory)(using jwtService: JwtService) extends Endpoints:

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

    val allEndpointImpl: ServerEndpoint[Any, Identity] = allEndpoint.serverLogic { paging =>
        val limit  = paging.limit.getOrElse(10000)
        val offset = paging.offset.getOrElse(0)
        handleErrors(service.findAllNames(limit, offset).map(s => Page(s, limit, offset)))
    }

    val addConceptNameEndpoint: Endpoint[Option[String], ConceptNameCreate, ErrorMsg, RawConcept, Any] = secureEndpoint
        .post
        .in(base)
        .in(jsonBody[ConceptNameCreate])
        .out(jsonBody[RawConcept])
        .name("addConceptName")
        .description("Add a new concept name")
        .tag(tag)

    val addConceptNameEndpointImpl: ServerEndpoint[Any, Identity] = addConceptNameEndpoint
        .serverSecurityLogic(jwtOpt => verifyLogin(jwtOpt))
        .serverLogic { userAccount => dto =>
            handleErrors(service.addName(dto, userAccount.username))
        }

    val updateConceptNameEndpoint: Endpoint[Option[String], (String, ConceptNameUpdate), ErrorMsg, RawConcept, Any] =
        secureEndpoint
            .put
            .in(base / path[String]("name"))
            .in(jsonBody[ConceptNameUpdate])
            .out(jsonBody[RawConcept])
            .name("updateConceptName")
            .description("Update a concept name")
            .tag(tag)

    val updateConceptNameEndpointImpl: ServerEndpoint[Any, Identity] = updateConceptNameEndpoint
        .serverSecurityLogic(jwtOpt => verifyLogin(jwtOpt))
        .serverLogic { userAccount => (name, dto) =>
            handleErrors(service.updateName(name, dto, userAccount.username))
        }

    val deleteConceptNameEndpoint: Endpoint[Option[String], String, ErrorMsg, RawConcept, Any] = secureEndpoint
        .delete
        .in(base / path[String]("name"))
        .out(jsonBody[RawConcept])
        .name("deleteConceptName")
        .description("Delete a concept name")
        .tag(tag)

    val deleteConceptNameEndpointImpl: ServerEndpoint[Any, Identity] = deleteConceptNameEndpoint
        .serverSecurityLogic(jwtOpt => verifyLogin(jwtOpt))
        .serverLogic { userAccount => name =>
            handleErrors(service.deleteName(name, userAccount.username))
        }

    override def all: List[Endpoint[_, _, _, _, _]] = List(
        allEndpoint,
        addConceptNameEndpoint,
        updateConceptNameEndpoint,
        deleteConceptNameEndpoint
    )

    override def allImpl: List[ServerEndpoint[Any, Identity]] = List(
        allEndpointImpl,
        addConceptNameEndpointImpl,
        updateConceptNameEndpointImpl,
        deleteConceptNameEndpointImpl
    )
