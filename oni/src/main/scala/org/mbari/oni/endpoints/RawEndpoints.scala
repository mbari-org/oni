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

package org.mbari.oni.endpoints

import jakarta.persistence.EntityManagerFactory
import org.mbari.oni.domain.{ErrorMsg, RawConcept, RawConceptName}
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

    val findRawConceptNamesByName: Endpoint[Unit, String, ErrorMsg, Seq[RawConceptName], Any] = openEndpoint
        .get
        .in(base / "names" / path[String]("name"))
        .out(jsonBody[Seq[RawConceptName]])
        .name("findRawConceptNamesByName")
        .description("Find all concept names for a concept")
        .tag(tag)

    val findRawConceptNamesByNameImpl: ServerEndpoint[Any, Future] =
        findRawConceptNamesByName.serverLogic { name =>
            handleErrorsAsync(service.findRawByName(name).map(_.names.toSeq.sortBy(_.name)))
        }

    override def all: List[Endpoint[?, ?, ?, ?, ?]] = List(findRawConceptByName, findRawConceptNamesByName)

    override def allImpl: List[ServerEndpoint[Any, Future]] =
        List(findRawConceptByNameImpl, findRawConceptNamesByNameImpl)
