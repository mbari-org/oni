package org.mbari.oni.endpoints

import jakarta.persistence.EntityManagerFactory
import org.mbari.oni.domain.{ErrorMsg, ExtendedHistory}
import org.mbari.oni.etc.circe.CirceCodecs.given
import org.mbari.oni.services.HistoryService
import sttp.tapir.*
import sttp.tapir.Endpoint
import sttp.tapir.json.circe.*
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.server.nima.Id

class HistoryEndpoints(entityManagerFactory: EntityManagerFactory) extends Endpoints:

    private val service = HistoryService(entityManagerFactory)
    private val base = "history"
    private val tag  = "History"

    val pendingEndpoint: Endpoint[Unit, Unit, ErrorMsg, Seq[ExtendedHistory], Any] = openEndpoint
        .get
        .in(base / "pending")
        .out(jsonBody[Seq[ExtendedHistory]])
        .name("pending")
        .description("Get all pending change requests")
        .tag(tag)

    val pendingEndpointImpl: ServerEndpoint[Any, Id] = pendingEndpoint.serverLogic {
        _ => handleErrors(service.findAllPending())
    }

    val approvedEndpoints: Endpoint[Unit, Unit, ErrorMsg, Seq[ExtendedHistory], Any] = openEndpoint
        .get
        .in(base / "approved")
        .out(jsonBody[Seq[ExtendedHistory]])
        .name("approved")
        .description("Get all approved change requests")
        .tag(tag)

    val approvedEndpointsImpl: ServerEndpoint[Any, Id] = approvedEndpoints.serverLogic {
        _ => handleErrors(service.findAllApproved())
    }

    override def all: List[Endpoint[_, _, _, _, _]] = List(
        approvedEndpoints,
        pendingEndpoint
    )

    override def allImpl: List[ServerEndpoint[Any, Id]] = List(
        approvedEndpointsImpl,
        pendingEndpointImpl
    )
