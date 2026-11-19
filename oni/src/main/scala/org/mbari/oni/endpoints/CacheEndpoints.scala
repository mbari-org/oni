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
import org.mbari.oni.domain.ErrorMsg
import org.mbari.oni.etc.circe.CirceCodecs.given
import org.mbari.oni.etc.jwt.JwtService
import org.mbari.oni.jpa.{ChangeBus, ChangeEvent}
import org.mbari.oni.services.UserAccountService
import sttp.tapir.json.circe.*
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.{Endpoint, *}

import scala.concurrent.{ExecutionContext, Future}

/**
 * Administration of the in-memory knowledgebase cache.
 */
class CacheEndpoints(entityManagerFactory: EntityManagerFactory)(using
    jwtService: JwtService,
    executionContext: ExecutionContext
) extends Endpoints:

    private val userAccountService = UserAccountService(entityManagerFactory)
    private val base               = "cache"
    private val tag                = "Cache"

    val clearEndpoint: Endpoint[Option[String], Unit, ErrorMsg, Unit, Any] = secureEndpoint
        .post
        .in(base / "clear")
        .out(jsonBody[Unit])
        .name("clearCache")
        .description(
            "Clear the in-memory knowledgebase cache so the next request reloads from the database. Use this after " +
                "changing the database directly (e.g. a manual SQL fix). When NATS messaging is enabled, every Oni " +
                "instance clears its cache. Requires an administrator login; API key tokens are not accepted"
        )
        .tag(tag)

    val clearEndpointImpl: ServerEndpoint[Any, Future] = clearEndpoint
        .serverSecurityLogic(jwtOpt => verifyUserAsync(jwtOpt, userAccountService.findByUserName, _.isAdministrator))
        .serverLogic { _ => _ =>
            Future {
                // Same path as a real change: the local cache clears through its ChangeBus subscription, and
                // NatsBridge forwards the event so other instances clear too
                ChangeBus.publish(ChangeEvent.Action.UPDATED, ChangeEvent.DataType.CONCEPT, null)
                Right(())
            }
        }

    override def all: List[Endpoint[?, ?, ?, ?, ?]] = List(clearEndpoint)

    override def allImpl: List[ServerEndpoint[Any, Future]] = List(clearEndpointImpl)
