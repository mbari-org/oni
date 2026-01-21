/*
 * Copyright 2024 Monterey Bay Aquarium Research Institute
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.mbari.oni

import jakarta.persistence.EntityManagerFactory
import org.mbari.oni.config.AppConfig
import org.mbari.oni.endpoints.{
    AuthorizationEndpoints,
    ConceptEndpoints,
    ConceptNameEndpoints,
    HealthEndpoints,
    HistoryEndpoints,
    LinkEndpoints,
    LinkRealizationEndpoints,
    LinkTemplateEndpoints,
    MediaEndpoints,
    PhylogenyEndpoints,
    PrefNodeEndpoints,
    RawEndpoints,
    ReferenceEndpoints,
    UserAccountEndpoints
}
import org.mbari.oni.etc.jwt.JwtService
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.server.metrics.prometheus.PrometheusMetrics
import sttp.tapir.swagger.bundle.SwaggerInterpreter

import java.util.concurrent.Executors
import scala.concurrent.{ExecutionContext, Future}

/**
 * Configures all endpoint/api definitions
 */
object Endpoints:

    given JwtService =
        val config = AppConfig.DefaultJwtConfig
        JwtService(config.issuer, config.apiKey, config.signingSecret)

    given ExecutionContext =
        ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(AppConfig.NumberOfThreads))

    val entityMangerFactory: EntityManagerFactory = AppConfig.DefaultEntityManagerFactory

    val phylogenyEndpoints: PhylogenyEndpoints = PhylogenyEndpoints(entityMangerFactory)

    val authorizationEndpoints: AuthorizationEndpoints     = AuthorizationEndpoints(entityMangerFactory)
    val conceptEndpoints: ConceptEndpoints                 = ConceptEndpoints(entityMangerFactory)
    val conceptNameEndpoints: ConceptNameEndpoints         = ConceptNameEndpoints(entityMangerFactory)
    val healthEndpoints: HealthEndpoints                   = HealthEndpoints()
    val historyEndpoints: HistoryEndpoints                 = HistoryEndpoints(entityMangerFactory, phylogenyEndpoints.service)
    val linkEndpoints: LinkEndpoints                       = LinkEndpoints(entityMangerFactory)
    val linkRealizationEndpoints: LinkRealizationEndpoints = LinkRealizationEndpoints(entityMangerFactory)
    val linkTemplateEndpoints: LinkTemplateEndpoints       = LinkTemplateEndpoints(entityMangerFactory)
    val mediaEndpoints: MediaEndpoints                     = MediaEndpoints(entityMangerFactory, phylogenyEndpoints.service)
    val prefNodeEndpoints: PrefNodeEndpoints               = PrefNodeEndpoints(entityMangerFactory)
    val rawEndpoints: RawEndpoints                         = RawEndpoints(entityMangerFactory)
    val referenceEndpoints: ReferenceEndpoints             = ReferenceEndpoints(entityMangerFactory)
    val userAccountEndpoints: UserAccountEndpoints         = UserAccountEndpoints(entityMangerFactory)

    val endpoints: List[ServerEndpoint[Any, Future]] = List(
        authorizationEndpoints,
        conceptEndpoints,
        conceptNameEndpoints,
        healthEndpoints,
        historyEndpoints,
        linkEndpoints,
        linkRealizationEndpoints,
        linkTemplateEndpoints,
        mediaEndpoints,
        phylogenyEndpoints,
        prefNodeEndpoints,
        rawEndpoints,
        referenceEndpoints,
        userAccountEndpoints
    ).flatMap(_.allImpl)

    val docEndpoints: List[ServerEndpoint[Any, Future]] =
        SwaggerInterpreter().fromServerEndpoints(endpoints, AppConfig.Name, AppConfig.Version)

    val prometheusMetrics: PrometheusMetrics[Future] = PrometheusMetrics.default[Future]()
    val metricsEndpoint: ServerEndpoint[Any, Future] = prometheusMetrics.metricsEndpoint

    val allImpl: List[ServerEndpoint[Any, Future]] = endpoints ++ docEndpoints ++ List(metricsEndpoint)
