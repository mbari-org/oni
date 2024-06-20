/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2024
 *
 * oni code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential. 
 */

package org.mbari.oni.endpoints

import jakarta.persistence.EntityManagerFactory
import org.mbari.oni.domain.{ErrorMsg, ExtendedHistory, Page}
import org.mbari.oni.etc.circe.CirceCodecs.given
import org.mbari.oni.services.HistoryService
import sttp.tapir.*
import sttp.tapir.Endpoint
import sttp.tapir.json.circe.*
import sttp.tapir.server.ServerEndpoint
import sttp.shared.Identity

class HistoryEndpoints(entityManagerFactory: EntityManagerFactory) extends Endpoints:

    private val service = HistoryService(entityManagerFactory)
    private val base    = "history"
    private val tag     = "History"
    private val defaultLimit = 100

    val pendingEndpoint: Endpoint[Unit, Paging, ErrorMsg, Page[Seq[ExtendedHistory]], Any] = openEndpoint
        .get
        .in(base / "pending")
        .in(paging)
        .out(jsonBody[Page[Seq[ExtendedHistory]]])
        .name("pending")
        .description("Get all pending change requests")
        .tag(tag)

    val pendingEndpointImpl: ServerEndpoint[Any, Identity] = pendingEndpoint.serverLogic { paging =>
        val limit  = paging.limit.getOrElse(defaultLimit)
        val offset = paging.offset.getOrElse(0)
        val attempt = for
            pending <- service.findAllPending(limit, offset)
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
        val limit  = paging.limit.getOrElse(defaultLimit)
        val offset = paging.offset.getOrElse(0)
        val attempt = for
            approved <- service.findAllApproved()
        yield Page(approved, limit, offset)
        handleErrors(attempt)
    }

    override def all: List[Endpoint[_, _, _, _, _]] = List(
        approvedEndpoints,
        pendingEndpoint
    )

    override def allImpl: List[ServerEndpoint[Any, Identity]] = List(
        approvedEndpointsImpl,
        pendingEndpointImpl
    )
