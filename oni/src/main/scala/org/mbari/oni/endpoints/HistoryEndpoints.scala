/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2024
 *
 * oni code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential.
 */

package org.mbari.oni.endpoints

import jakarta.persistence.EntityManagerFactory
import org.mbari.oni.domain.{Count, ErrorMsg, ExtendedHistory, Page, ServerError, Unauthorized}
import org.mbari.oni.etc.circe.CirceCodecs.given
import org.mbari.oni.etc.jwt.JwtService
import org.mbari.oni.jdbc.FastPhylogenyService
import org.mbari.oni.services.{HistoryActionService, HistoryService}
import sttp.tapir.json.circe.*
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.{Endpoint, *}

import scala.concurrent.{ExecutionContext, Future}

class HistoryEndpoints(entityManagerFactory: EntityManagerFactory, fastPhylogenyService: FastPhylogenyService)(using
    jwtService: JwtService,
    executionContext: ExecutionContext
) extends Endpoints:

    private val service              = HistoryService(entityManagerFactory)
    private val historyActionService = HistoryActionService(entityManagerFactory, fastPhylogenyService)
    private val base                 = "history"
    private val tag                  = "History"
    private val defaultLimit         = 100

    val pendingCountEndpoint: Endpoint[Unit, Unit, ErrorMsg, Count, Any] =
        openEndpoint
            .get
            .in(base / "pending" / "count")
            .out(jsonBody[Count])
            .name("pendingCount")
            .description("Get the count of all pending change requests")
            .tag(tag)

    val pendingCountEndpointImpl: ServerEndpoint[Any, Future] = pendingCountEndpoint.serverLogic { _ =>
        Future {
            val attempt = service.countPending().map(Count.apply)
            handleErrors(attempt)
        }
    }

    val pendingEndpoint: Endpoint[Unit, Paging, ErrorMsg, Page[Seq[ExtendedHistory]], Any] = openEndpoint
        .get
        .in(base / "pending")
        .in(paging)
        .out(jsonBody[Page[Seq[ExtendedHistory]]])
        .name("pending")
        .description("Get all pending change requests")
        .tag(tag)

    val pendingEndpointImpl: ServerEndpoint[Any, Future] = pendingEndpoint.serverLogic { paging =>
        Future {
            val limit   = paging.limit.getOrElse(defaultLimit)
            val offset  = paging.offset.getOrElse(0)
            val attempt =
                for pending <- service.findAllPending(limit, offset)
                yield Page(pending, limit, offset)
            handleErrors(attempt)
        }
    }

    val approvedCountEndpoint: Endpoint[Unit, Unit, ErrorMsg, Count, Any] =
        openEndpoint
            .get
            .in(base / "approved" / "count")
            .out(jsonBody[Count])
            .name("approvedCount")
            .description("Get the count of all approved change requests")
            .tag(tag)

    val approvedCountEndpointImpl: ServerEndpoint[Any, Future] = approvedCountEndpoint.serverLogic { _ =>
        Future {
            val attempt = service.countApproved().map(Count.apply)
            handleErrors(attempt)
        }
    }

    val approvedEndpoints: Endpoint[Unit, Paging, ErrorMsg, Page[Seq[ExtendedHistory]], Any] = openEndpoint
        .get
        .in(base / "approved")
        .in(paging)
        .out(jsonBody[Page[Seq[ExtendedHistory]]])
        .name("approved")
        .description("Get all approved change requests")
        .tag(tag)

    val approvedEndpointsImpl: ServerEndpoint[Any, Future] = approvedEndpoints.serverLogic { paging =>
        Future {
            val limit   = paging.limit.getOrElse(defaultLimit)
            val offset  = paging.offset.getOrElse(0)
            val attempt =
                for approved <- service.findAllApproved(limit, offset)
                yield Page(approved, limit, offset)
            handleErrors(attempt)
        }
    }

    val findByIdEndpoint = openEndpoint
        .get
        .in(base / path[Long])
        .out(jsonBody[ExtendedHistory])
        .name("findHistoryById")
        .description("Find a history record by its id")
        .tag(tag)

    val findByIdEndpointImpl: ServerEndpoint[Any, Future] = findByIdEndpoint
        .serverLogic { id =>
            handleErrorsAsync(service.findById(id))
        }

    val findByConceptNameEndpoint = openEndpoint
        .get
        .in(base / "concept" / path[String])
        .out(jsonBody[Seq[ExtendedHistory]])
        .name("findHistoryByConceptName")
        .description("Find a history record by its concept name")
        .tag(tag)

    val findByConceptNameEndpointImpl: ServerEndpoint[Any, Future] = findByConceptNameEndpoint
        .serverLogic { conceptName =>
            handleErrorsAsync(service.findByConceptName(conceptName))
        }

    val deleteEndpoint: Endpoint[Option[String], Long, ErrorMsg, Unit, Any] = secureEndpoint
        .delete
        .in(base / path[Long])
        .out(jsonBody[Unit])
        .name("deleteHistory")
        .description("Delete a history record")
        .tag(tag)

    val deleteEndpointImpl: ServerEndpoint[Any, Future] = deleteEndpoint
        .serverSecurityLogic(jwtOpt => verifyLoginAsync(jwtOpt))
        .serverLogic { userAccount => id =>
            Future {
                if userAccount.isAdministrator then
                    service
                        .deleteById(id)
                        .fold(
                            error => Left(ServerError(error.getMessage)),
                            _ => Right(())
                        )
                else Left(Unauthorized(s"User, ${userAccount.username} is not an administrator"))
            }
        }

    val approveEndpoint: Endpoint[Option[String], Long, ErrorMsg, ExtendedHistory, Any] = secureEndpoint
        .put
        .in(base / "approve" / path[Long])
        .out(jsonBody[ExtendedHistory])
        .name("approveHistory")
        .description("Approve a history record")
        .tag(tag)

    val approveEndpointImpl: ServerEndpoint[Any, Future] = approveEndpoint
        .serverSecurityLogic(jwtOpt => verifyLoginAsync(jwtOpt))
        .serverLogic { userAccount => id =>
            Future {
                historyActionService
                    .approve(id, userAccount.username)
                    .fold(
                        error => Left(ServerError(error.getMessage)),
                        history => Right(history)
                    )
            }
        }

    val rejectEndpoint: Endpoint[Option[String], Long, ErrorMsg, ExtendedHistory, Any] = secureEndpoint
        .put
        .in(base / "reject" / path[Long])
        .out(jsonBody[ExtendedHistory])
        .name("rejectHistory")
        .description("Reject a history record")
        .tag(tag)

    val rejectEndpointImpl: ServerEndpoint[Any, Future] = rejectEndpoint
        .serverSecurityLogic(jwtOpt => verifyLoginAsync(jwtOpt))
        .serverLogic { userAccount => id =>
            Future {
                historyActionService
                    .reject(id, userAccount.username)
                    .fold(
                        error => Left(ServerError(error.getMessage)),
                        history => Right(history)
                    )
            }
        }

    override def all: List[Endpoint[?, ?, ?, ?, ?]] = List(
        findByConceptNameEndpoint,
        approveEndpoint,
        rejectEndpoint,
        approvedCountEndpoint,
        approvedEndpoints,
        pendingCountEndpoint,
        pendingEndpoint,
        deleteEndpoint,
        findByIdEndpoint
    )

    override def allImpl: List[ServerEndpoint[Any, Future]] = List(
        findByConceptNameEndpointImpl,
        approveEndpointImpl,
        rejectEndpointImpl,
        approvedCountEndpointImpl,
        approvedEndpointsImpl,
        pendingCountEndpointImpl,
        pendingEndpointImpl,
        deleteEndpointImpl,
        findByIdEndpointImpl
    )
