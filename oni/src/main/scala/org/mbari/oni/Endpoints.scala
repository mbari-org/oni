/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2024
 *
 * oni code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential. 
 */

package org.mbari.oni

import org.mbari.oni.endpoints.{AuthorizationEndpoints, HealthEndpoints}
import org.mbari.oni.etc.jwt.JwtService
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.server.metrics.prometheus.PrometheusMetrics
import sttp.tapir.server.nima.Id

object Endpoints:

    given JwtService =
        val config = AppConfig.DefaultJwtConfig
        JwtService(config.issuer, config.apiKey, config.signingSecret)

    val authorizationEndpoints: AuthorizationEndpoints = AuthorizationEndpoints()
    val healthEndpoints: HealthEndpoints               = HealthEndpoints()

    val prometheusMetrics: PrometheusMetrics[Id] = PrometheusMetrics.default[Id]()
    val metricsEndpoint: ServerEndpoint[Any, Id] = prometheusMetrics.metricsEndpoint

    val endpoints = List(
        authorizationEndpoints.allImpl,
        healthEndpoints.allImpl
    ).flatten

    val allImpl: List[ServerEndpoint[Any, Id]] = endpoints ++ List(metricsEndpoint)
