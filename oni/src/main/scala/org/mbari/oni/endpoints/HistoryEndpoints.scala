/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2024
 *
 * oni code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential. 
 */

package org.mbari.oni.endpoints

import jakarta.persistence.EntityManagerFactory
import org.mbari.oni.AccessDenied
import org.mbari.oni.domain.{ErrorMsg, ExtendedHistory, Page, ServerError, Unauthorized}
import org.mbari.oni.etc.circe.CirceCodecs.given
import org.mbari.oni.etc.jwt.JwtService
import org.mbari.oni.jdbc.FastPhylogenyService
import org.mbari.oni.services.{HistoryActionService, HistoryService}
import sttp.tapir.*
import sttp.tapir.Endpoint
import sttp.tapir.json.circe.*
import sttp.tapir.server.ServerEndpoint
import sttp.shared.Identity

class HistoryEndpoints(entityManagerFactory: EntityManagerFactory, fastPhylogenyService: FastPhylogenyService)(using
    jwtService: JwtService
) extends Endpoints:

    private val service              = HistoryService(entityManagerFactory)
    private val historyActionService = HistoryActionService(entityManagerFactory, fastPhylogenyService)
    private val base                 = "history"
    private val tag                  = "History"
    private val defaultLimit         = 100

    val pendingEndpoint: Endpoint[Unit, Paging, ErrorMsg, Page[Seq[ExtendedHistory]], Any] = openEndpoint
        .get
        .in(base / "pending")
        .in(paging)
        .out(jsonBody[Page[Seq[ExtendedHistory]]])
        .name("pending")
        .description("Get all pending change requests")
        .tag(tag)

    val pendingEndpointImpl: ServerEndpoint[Any, Identity] = pendingEndpoint.serverLogic { paging =>
        val limit   = paging.limit.getOrElse(defaultLimit)
        val offset  = paging.offset.getOrElse(0)
        val attempt =
            for pending <- service.findAllPending(limit, offset)
            yield Page(pending, limit, offset)
        handleErrors(attempt)
    }

    val approvedEndpoints: Endpoint[Unit, Paging, ErrorMsg, Page[Seq[ExtendedHistory]], Any] = openEndpoint
        .get
        .in(base / "approved")
        .in(paging)
        .out(jsonBody[Page[Seq[ExtendedHistory]]])
        .name("approved")
        .description("Get all approved change requests")
        .tag(tag)

    val approvedEndpointsImpl: ServerEndpoint[Any, Identity] = approvedEndpoints.serverLogic { paging =>
        val limit   = paging.limit.getOrElse(defaultLimit)
        val offset  = paging.offset.getOrElse(0)
        val attempt =
            for approved <- service.findAllApproved()
            yield Page(approved, limit, offset)
        handleErrors(attempt)
    }

    val findByIdEndpoint = openEndpoint
        .get
        .in(base / path[Long])
        .out(jsonBody[ExtendedHistory])
        .name("findHistoryById")
        .description("Find a history record by its id")
        .tag(tag)

    val findByIdEndpointImpl: ServerEndpoint[Any, Identity] = findByIdEndpoint
        .serverLogic { id =>
            handleErrors(service.findById(id))
        }

    val findByConceptNameEndpoint = openEndpoint
        .get
        .in(base / "concept" / path[String])
        .out(jsonBody[Seq[ExtendedHistory]])
        .name("findHistoryByConceptName")
        .description("Find a history record by its concept name")
        .tag(tag)

    val findByConceptNameEndpointImpl: ServerEndpoint[Any, Identity] = findByConceptNameEndpoint
        .serverLogic { conceptName =>
            handleErrors(service.findByConceptName(conceptName))
        }

    val deleteEndpoint: Endpoint[Option[String], Long, ErrorMsg, Unit, Any] = secureEndpoint
        .delete
        .in(base / path[Long])
        .out(jsonBody[Unit])
        .name("deleteHistory")
        .description("Delete a history record")
        .tag(tag)

    val deleteEndpointImpl: ServerEndpoint[Any, Identity] = deleteEndpoint
        .serverSecurityLogic(jwtOpt => verifyLogin(jwtOpt))
        .serverLogic { userAccount => id =>
            if userAccount.isAdministrator then
                service
                    .deleteById(id)
                    .fold(
                        error => Left(ServerError(error.getMessage)),
                        _ => Right(())
                    )
            else Left(Unauthorized(s"User, ${userAccount.username} is not an administrator"))
        }

    val approveEndpoint: Endpoint[Option[String], Long, ErrorMsg, ExtendedHistory, Any] = secureEndpoint
        .put
        .in(base / "approve" / path[Long])
        .out(jsonBody[ExtendedHistory])
        .name("approveHistory")
        .description("Approve a history record")
        .tag(tag)

    val approveEndpointImpl: ServerEndpoint[Any, Identity] = approveEndpoint
        .serverSecurityLogic(jwtOpt => verifyLogin(jwtOpt))
        .serverLogic { userAccount => id =>
            historyActionService
                .approve(id, userAccount.username)
                .fold(
                    error => Left(ServerError(error.getMessage)),
                    history => Right(history)
                )
        }

    val rejectEndpoint: Endpoint[Option[String], Long, ErrorMsg, ExtendedHistory, Any] = secureEndpoint
        .put
        .in(base / "reject" / path[Long])
        .out(jsonBody[ExtendedHistory])
        .name("rejectHistory")
        .description("Reject a history record")
        .tag(tag)

    val rejectEndpointImpl: ServerEndpoint[Any, Identity] = rejectEndpoint
        .serverSecurityLogic(jwtOpt => verifyLogin(jwtOpt))
        .serverLogic { userAccount => id =>
            historyActionService
                .reject(id, userAccount.username)
                .fold(
                    error => Left(ServerError(error.getMessage)),
                    history => Right(history)
                )
        }

    override def all: List[Endpoint[_, _, _, _, _]] = List(
        findByConceptNameEndpoint,
        approveEndpoint,
        rejectEndpoint,
        approvedEndpoints,
        pendingEndpoint,
        deleteEndpoint,
        findByIdEndpoint
    )

    override def allImpl: List[ServerEndpoint[Any, Identity]] = List(
        findByConceptNameEndpointImpl,
        approveEndpointImpl,
        rejectEndpointImpl,
        approvedEndpointsImpl,
        pendingEndpointImpl,
        deleteEndpointImpl,
        findByIdEndpointImpl
    )
