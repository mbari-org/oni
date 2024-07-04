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
    ConceptNameEndpoints,
    HealthEndpoints,
    HistoryEndpoints,
    LinkEndpoints,
    PhylogenyEndpoints,
    PrefNodeEndpoints,
    ReferenceEndpoints,
    UserAccountEndpoints
}
import org.mbari.oni.etc.jwt.JwtService
import org.mbari.oni.jdbc.FastPhylogenyService
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.server.metrics.prometheus.PrometheusMetrics
import sttp.shared.Identity
import sttp.tapir.swagger.bundle.SwaggerInterpreter
import org.mbari.oni.endpoints.LinkRealizationEndpoints
import org.mbari.oni.endpoints.LinkTemplateEndpoints

object Endpoints:

    given JwtService =
        val config = AppConfig.DefaultJwtConfig
        JwtService(config.issuer, config.apiKey, config.signingSecret)

    val entityMangerFactory: EntityManagerFactory = AppConfig.DefaultEntityManagerFactory

    val phylogenyEndpoints: PhylogenyEndpoints = PhylogenyEndpoints(entityMangerFactory)

    val authorizationEndpoints: AuthorizationEndpoints = AuthorizationEndpoints(entityMangerFactory)
    val conceptEndpoints: ConceptEndpoints             = ConceptEndpoints(entityMangerFactory)
    val conceptNameEndpoints: ConceptNameEndpoints     = ConceptNameEndpoints(entityMangerFactory)
    val healthEndpoints: HealthEndpoints               = HealthEndpoints()
    val historyEndpoints: HistoryEndpoints             = HistoryEndpoints(entityMangerFactory, phylogenyEndpoints.service)
    val linkEndpoints: LinkEndpoints                   = LinkEndpoints(entityMangerFactory)
    val linkRealizationEndpoints: LinkRealizationEndpoints = LinkRealizationEndpoints(entityMangerFactory)
    val linkTemplateEndpoints: LinkTemplateEndpoints       = LinkTemplateEndpoints(entityMangerFactory)
    val prefNodeEndpoints: PrefNodeEndpoints           = PrefNodeEndpoints(entityMangerFactory)
    val referenceEndpoints: ReferenceEndpoints         = ReferenceEndpoints(entityMangerFactory)
    val userAccountEndpoints: UserAccountEndpoints     = UserAccountEndpoints(entityMangerFactory)

    val endpoints: List[ServerEndpoint[Any, Identity]] = List(
        authorizationEndpoints,
        conceptEndpoints,
        conceptNameEndpoints,
        healthEndpoints,
        historyEndpoints,
        linkEndpoints,
        linkRealizationEndpoints,
        linkTemplateEndpoints,
        phylogenyEndpoints,
        prefNodeEndpoints,
        referenceEndpoints,
        userAccountEndpoints
    ).flatMap(_.allImpl)

    val docEndpoints: List[ServerEndpoint[Any, Identity]] =
        SwaggerInterpreter().fromServerEndpoints(endpoints, AppConfig.Name, AppConfig.Version)

    val prometheusMetrics: PrometheusMetrics[Identity] = PrometheusMetrics.default[Identity]()
    val metricsEndpoint: ServerEndpoint[Any, Identity] = prometheusMetrics.metricsEndpoint

    val allImpl: List[ServerEndpoint[Any, Identity]] = endpoints ++ docEndpoints ++ List(metricsEndpoint)
