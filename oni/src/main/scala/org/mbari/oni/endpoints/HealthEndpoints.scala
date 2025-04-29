/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2024
 *
 * oni code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential.
 */

package org.mbari.oni.endpoints

import org.mbari.oni.domain.{ErrorMsg, HealthStatus}
import org.mbari.oni.etc.circe.CirceCodecs.given
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.server.ServerEndpoint

import scala.concurrent.{ExecutionContext, Future}

class HealthEndpoints(using executionContext: ExecutionContext) extends Endpoints:

    val healthEndpoint: Endpoint[Unit, Unit, ErrorMsg, HealthStatus, Any] =
        openEndpoint
            .get
            .in("health")
            .out(jsonBody[HealthStatus])
            .name("health")
            .description("Health check")
            .tag("Health")

    val healthEndpointImpl: ServerEndpoint[Any, Future] =
        healthEndpoint.serverLogic(_ => Future(Right(HealthStatus.Default)))

    override def all: List[Endpoint[?, ?, ?, ?, ?]] =
        List(healthEndpoint)

    override def allImpl: List[ServerEndpoint[Any, Future]] = List(healthEndpointImpl)
