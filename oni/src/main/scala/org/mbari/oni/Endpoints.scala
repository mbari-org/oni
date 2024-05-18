/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2024
 *
 * oni code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential. 
 */

package org.mbari.oni

import jakarta.persistence.EntityManagerFactory
import org.mbari.oni.endpoints.{AuthorizationEndpoints, ConceptEndpoints, HealthEndpoints, HistoryEndpoints, LinkEndpoints, PhylogenyEndpoints, PrefNodeEndpoints, UserAccountEndpoints}
import org.mbari.oni.etc.jwt.JwtService
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.server.metrics.prometheus.PrometheusMetrics
import sttp.tapir.server.nima.Id

object Endpoints:

    given JwtService =
        val config = AppConfig.DefaultJwtConfig
        JwtService(config.issuer, config.apiKey, config.signingSecret)

    val entityMangerFactory: EntityManagerFactory = AppConfig.DefaultEntityManagerFactory

    val authorizationEndpoints: AuthorizationEndpoints = AuthorizationEndpoints(entityMangerFactory)
    val conceptEndpoints: ConceptEndpoints     = ConceptEndpoints(entityMangerFactory)
    val healthEndpoints: HealthEndpoints               = HealthEndpoints()
    val historyEndpoints: HistoryEndpoints     = HistoryEndpoints(entityMangerFactory)
    val linkEndpoints: LinkEndpoints           = LinkEndpoints(entityMangerFactory)
    val phylogenyEndpoints: PhylogenyEndpoints = PhylogenyEndpoints(entityMangerFactory)
    val prefNodeEndpoints: PrefNodeEndpoints   = PrefNodeEndpoints(entityMangerFactory)
    val userAccountEndpoints: UserAccountEndpoints = UserAccountEndpoints(entityMangerFactory)





    val prometheusMetrics: PrometheusMetrics[Id] = PrometheusMetrics.default[Id]()
    val metricsEndpoint: ServerEndpoint[Any, Id] = prometheusMetrics.metricsEndpoint

    val endpoints: List[ServerEndpoint[Any, Id]] = List(
        authorizationEndpoints.allImpl,
        conceptEndpoints.allImpl,
        healthEndpoints.allImpl,
        historyEndpoints.allImpl,
        linkEndpoints.allImpl,
        phylogenyEndpoints.allImpl,
        prefNodeEndpoints.allImpl,
        userAccountEndpoints.allImpl
    ).flatten

    val allImpl: List[ServerEndpoint[Any, Id]] = endpoints ++ List(metricsEndpoint)
