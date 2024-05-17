/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2024
 *
 * oni code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential. 
 */

package org.mbari.oni

import jakarta.persistence.EntityManagerFactory
import org.mbari.oni.endpoints.{
    AuthorizationEndpoints,
    ConceptEndpoints,
    HealthEndpoints,
    HistoryEndpoints,
    PhylogenyEndpoints
}
import org.mbari.oni.etc.jwt.JwtService
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.server.metrics.prometheus.PrometheusMetrics
import sttp.tapir.server.nima.Id

object Endpoints:

    given JwtService =
        val config = AppConfig.DefaultJwtConfig
        JwtService(config.issuer, config.apiKey, config.signingSecret)

    val entityMangerFactory: EntityManagerFactory = AppConfig.DefaultEntityManagerFactory

    val conceptEndpoints: ConceptEndpoints     = ConceptEndpoints(entityMangerFactory)
    val historyEndpoints: HistoryEndpoints     = HistoryEndpoints(entityMangerFactory)
    val phylogenyEndpoints: PhylogenyEndpoints = PhylogenyEndpoints(entityMangerFactory)

    val authorizationEndpoints: AuthorizationEndpoints = AuthorizationEndpoints()
    val healthEndpoints: HealthEndpoints               = HealthEndpoints()

    val prometheusMetrics: PrometheusMetrics[Id] = PrometheusMetrics.default[Id]()
    val metricsEndpoint: ServerEndpoint[Any, Id] = prometheusMetrics.metricsEndpoint

    val endpoints: List[ServerEndpoint[Any, Id]] = List(
        authorizationEndpoints.allImpl,
        healthEndpoints.allImpl,
        conceptEndpoints.allImpl,
        historyEndpoints.allImpl,
        phylogenyEndpoints.allImpl
    ).flatten

    val allImpl: List[ServerEndpoint[Any, Id]] = endpoints ++ List(metricsEndpoint)
