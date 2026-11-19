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
import org.mbari.oni.domain.{ConceptNameCreate, ConceptNameUpdate, ErrorMsg, Page, RawConcept}
import org.mbari.oni.etc.circe.CirceCodecs.given
import org.mbari.oni.etc.jwt.JwtService
import org.mbari.oni.services.ConceptNameService
import sttp.tapir.json.circe.*
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.{Endpoint, *}

import scala.concurrent.{ExecutionContext, Future}

class ConceptNameEndpoints(entityManagerFactory: EntityManagerFactory)(using
    jwtService: JwtService,
    executionContext: ExecutionContext
) extends Endpoints:

    private val service = ConceptNameService(entityManagerFactory)
    private val base    = "names"
    private val tag     = "ConceptName"

    private val updateNameDescription =
        "Rename it (`newName`, 400 if that name already exists), change its type, or change its author (set " +
            "`author` to an empty string to remove it). A primary name cannot be changed to another type (400). " +
            "Requires an administrator or maintainer login. Maintainer changes are applied and recorded as pending history."

    val allEndpoint = openEndpoint
        .get
        .in(base)
        .in(paging)
        .out(jsonBody[Page[Seq[String]]])
        .name("allConceptNames")
        .description("Get all concept names")
        .tag(tag)

    val allEndpointImpl: ServerEndpoint[Any, Future] = allEndpoint.serverLogic { paging =>
        val limit  = paging.limit.getOrElse(10000)
        val offset = paging.offset.getOrElse(0)
        handleErrorsAsync(service.findAllNames(limit, offset).map(s => Page(s, limit, offset)))
    }

    val addConceptNameEndpoint: Endpoint[Option[String], ConceptNameCreate, ErrorMsg, RawConcept, Any] = secureEndpoint
        .post
        .in(base)
        .in(jsonBody[ConceptNameCreate])
        .out(jsonBody[RawConcept])
        .name("addConceptName")
        .description(
            "Add a new name (`newName`) to the concept that owns `name`. If `nameType` is primary, the current " +
                "primary name becomes a former name. Returns 400 if `newName` already exists. Requires an administrator or maintainer login. " +
                "Maintainer changes are applied and recorded as pending history."
        )
        .tag(tag)

    val addConceptNameEndpointImpl: ServerEndpoint[Any, Future] = addConceptNameEndpoint
        .serverSecurityLogic(jwtOpt => verifyLoginAsync(jwtOpt))
        .serverLogic { userAccount => dto =>
            handleSecureErrorsAsync(service.addName(dto, userAccount.username))
        }

    val findConceptNameEndpoint: Endpoint[Unit, String, ErrorMsg, RawConcept, Any] = openEndpoint
        .get
        .in(base / path[String]("name"))
        .out(jsonBody[RawConcept])
        .name("findConceptName")
        .description("Find the concept that owns a concept name")
        .tag(tag)

    val findConceptNameEndpointImpl: ServerEndpoint[Any, Future] = findConceptNameEndpoint
        .serverLogic { name =>
            handleErrorsAsync(service.findByName(name))
        }

    val findConceptNameByIdEndpoint: Endpoint[Unit, Long, ErrorMsg, RawConcept, Any] = openEndpoint
        .get
        .in(base / "id" / path[Long]("id"))
        .out(jsonBody[RawConcept])
        .name("findConceptNameById")
        .description("Find the concept that owns the concept name with the given id")
        .tag(tag)

    val findConceptNameByIdEndpointImpl: ServerEndpoint[Any, Future] = findConceptNameByIdEndpoint
        .serverLogic { id =>
            handleErrorsAsync(service.findById(id))
        }

    val updateConceptNameEndpoint: Endpoint[Option[String], (String, ConceptNameUpdate), ErrorMsg, RawConcept, Any] =
        secureEndpoint
            .put
            .in(base / path[String]("name"))
            .in(jsonBody[ConceptNameUpdate])
            .out(jsonBody[RawConcept])
            .name("updateConceptName")
            .description("Update a concept name. " + updateNameDescription)
            .tag(tag)

    val updateConceptNameEndpointImpl: ServerEndpoint[Any, Future] = updateConceptNameEndpoint
        .serverSecurityLogic(jwtOpt => verifyLoginAsync(jwtOpt))
        .serverLogic { userAccount => (name, dto) =>
            handleSecureErrorsAsync(service.updateName(name, dto, userAccount.username))
        }

    val deleteConceptNameEndpoint: Endpoint[Option[String], String, ErrorMsg, RawConcept, Any] = secureEndpoint
        .delete
        .in(base / path[String]("name"))
        .out(jsonBody[RawConcept])
        .name("deleteConceptName")
        .description(
            "Delete a concept name. Primary names cannot be deleted (400). Requires an administrator or maintainer login. Administrators " +
                "delete immediately; other users create a pending delete that must be approved by an administrator."
        )
        .tag(tag)

    val deleteConceptNameEndpointImpl: ServerEndpoint[Any, Future] = deleteConceptNameEndpoint
        .serverSecurityLogic(jwtOpt => verifyLoginAsync(jwtOpt))
        .serverLogic { userAccount => name =>
            handleSecureErrorsAsync(service.deleteName(name, userAccount.username))
        }

    val updateConceptNameByIdEndpoint: Endpoint[Option[String], (Long, ConceptNameUpdate), ErrorMsg, RawConcept, Any] =
        secureEndpoint
            .put
            .in(base / "id" / path[Long]("id"))
            .in(jsonBody[ConceptNameUpdate])
            .out(jsonBody[RawConcept])
            .name("updateConceptNameById")
            .description("Update a concept name by id. " + updateNameDescription)
            .tag(tag)

    val updateConceptNameByIdEndpointImpl: ServerEndpoint[Any, Future] = updateConceptNameByIdEndpoint
        .serverSecurityLogic(jwtOpt => verifyLoginAsync(jwtOpt))
        .serverLogic { userAccount => (id, dto) =>
            handleSecureErrorsAsync(service.updateNameById(id, dto, userAccount.username))
        }

    val deleteConceptNameByIdEndpoint: Endpoint[Option[String], Long, ErrorMsg, RawConcept, Any] = secureEndpoint
        .delete
        .in(base / "id" / path[Long]("id"))
        .out(jsonBody[RawConcept])
        .name("deleteConceptNameById")
        .description(
            "Delete a concept name by id. Primary names cannot be deleted (400). Requires an administrator or maintainer login. " +
                "Administrators delete immediately; other users create a pending delete that must be approved by an " +
                "administrator."
        )
        .tag(tag)

    val deleteConceptNameByIdEndpointImpl: ServerEndpoint[Any, Future] = deleteConceptNameByIdEndpoint
        .serverSecurityLogic(jwtOpt => verifyLoginAsync(jwtOpt))
        .serverLogic { userAccount => id =>
            handleSecureErrorsAsync(service.deleteNameById(id, userAccount.username))
        }

    override def all: List[Endpoint[?, ?, ?, ?, ?]] = List(
        findConceptNameEndpoint,
        findConceptNameByIdEndpoint,
        allEndpoint,
        addConceptNameEndpoint,
        updateConceptNameEndpoint,
        updateConceptNameByIdEndpoint,
        deleteConceptNameEndpoint,
        deleteConceptNameByIdEndpoint
    )

    override def allImpl: List[ServerEndpoint[Any, Future]] = List(
        findConceptNameEndpointImpl,
        findConceptNameByIdEndpointImpl,
        allEndpointImpl,
        addConceptNameEndpointImpl,
        updateConceptNameEndpointImpl,
        updateConceptNameByIdEndpointImpl,
        deleteConceptNameEndpointImpl,
        deleteConceptNameByIdEndpointImpl
    )
