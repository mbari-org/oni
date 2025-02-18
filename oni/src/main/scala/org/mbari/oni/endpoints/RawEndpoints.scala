/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2024
 *
 * oni code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential.
 */

package org.mbari.oni.endpoints

import jakarta.persistence.EntityManagerFactory
import org.mbari.oni.domain.{ErrorMsg, RawConcept}
import org.mbari.oni.etc.circe.CirceCodecs.given
import org.mbari.oni.services.ConceptService
import sttp.tapir.json.circe.*
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.{Endpoint, *}

import scala.concurrent.{ExecutionContext, Future}

class RawEndpoints(entityManagerFactory: EntityManagerFactory)(using executionContext: ExecutionContext)
    extends Endpoints:

    private val service = ConceptService(entityManagerFactory)
    val base            = "raw"
    val tag             = "Raw"

    val findRawConceptByName: Endpoint[Unit, String, ErrorMsg, RawConcept, Any] = openEndpoint
        .get
        .in(base / "concept" / path[String]("name"))
        .out(jsonBody[RawConcept])
        .name("findRawConceptByName")
        .description("Find a concept by name. Return raw (i.e. database format) concept")
        .tag(tag)

    val findRawConceptByNameImpl: ServerEndpoint[Any, Future] =
        findRawConceptByName.serverLogic { name =>
            handleErrorsAsync(service.findRawByName(name))
        }

    override def all: List[Endpoint[?, ?, ?, ?, ?]] = List(findRawConceptByName)

    override def allImpl: List[ServerEndpoint[Any, Future]] = List(findRawConceptByNameImpl)
