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
import org.mbari.oni.domain.{
    ErrorMsg,
    NotFound,
    UserAccount,
    UserAccountCreate,
    UserAccountInfo,
    UserAccountRoles,
    UserAccountUpdate
}
import org.mbari.oni.etc.circe.CirceCodecs.given
import org.mbari.oni.etc.jwt.JwtService
import org.mbari.oni.services.UserAccountService
import sttp.tapir.json.circe.*
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.{Endpoint, *}

import scala.concurrent.{ExecutionContext, Future}

class UserAccountEndpoints(entityManagerFactory: EntityManagerFactory)(using
    jwtService: JwtService,
    executionContext: ExecutionContext
) extends Endpoints:

    private val service = UserAccountService(entityManagerFactory)
    private val base    = "users"
    private val tag     = "User Accounts"

    /** Deleting user accounts requires the API key or a user who is currently an administrator */
    private def verifyApiKeyOrAdminAsync(jwtOpt: Option[String]): Future[Either[ErrorMsg, Unit]] =
        verifyApiKeyOrUserAsync(jwtOpt, service.findByUserName, _.isAdministrator)

    // findAll
    val findAllEndpoint: Endpoint[Unit, Unit, ErrorMsg, Seq[UserAccountInfo], Any] =
        openEndpoint
            .get
            .in(base)
            .out(jsonBody[Seq[UserAccountInfo]])
            .name("allUserAccounts")
            .description("Get all user accounts")
            .tag(tag)

    val findAllEndpointImpl: ServerEndpoint[Any, Future] = findAllEndpoint.serverLogic { _ =>
        handleErrorsAsync(service.findAll().map(_.map(UserAccountInfo.from)))
    }

    // findByUserName
    val findByUserNameEndpoint: Endpoint[Unit, String, ErrorMsg, UserAccountInfo, Any] =
        openEndpoint
            .get
            .in(base / path[String]("name"))
            .out(jsonBody[UserAccountInfo])
            .name("userAccountByName")
            .description("Find a user account by username")
            .tag(tag)

    val findByUserNameEndpointImpl: ServerEndpoint[Any, Future] = findByUserNameEndpoint.serverLogic { name =>
        Future {
            handleErrors(service.findByUserName(name)).flatMap {
                case None        => Left(NotFound(s"User account not found: $name"))
                case Some(value) => Right(UserAccountInfo.from(value))
            }
        }
    }

    // findAllByRole
    val findAllByRoleEndpoint: Endpoint[Unit, String, ErrorMsg, Seq[UserAccountInfo], Any] =
        openEndpoint
            .get
            .in(base / "role" / path[String]("role"))
            .out(jsonBody[Seq[UserAccountInfo]])
            .name("userAccountsByRole")
            .description(
                "Find all user accounts with a role. The role is matched exactly, ignoring case; an unrecognized role " +
                    "returns an empty list. Accounts whose stored role isn't recognized are treated as ReadOnly, so " +
                    "they're listed under ReadOnly."
            )
            .tag(tag)

    val findAllByRoleEndpointImpl: ServerEndpoint[Any, Future] = findAllByRoleEndpoint.serverLogic { role =>
        handleErrorsAsync(service.findAllByRole(role).map(_.map(UserAccountInfo.from)))
    }

    // listRoles
    val listRolesEndpoint: Endpoint[Unit, Unit, ErrorMsg, Seq[String], Any] =
        openEndpoint
            .get
            .in(base / "list" / "roles")
            .out(jsonBody[Seq[String]])
            .name("userAccountRoles")
            .description(
                "List the recognized user account roles, e.g. [\"Admin\", \"Maint\", \"ReadOnly\", \"Locked\"]. These " +
                    "are the values to use for `role`. A stored role that isn't one of these is treated as ReadOnly."
            )
            .tag(tag)

    val listRolesEndpointImpl: ServerEndpoint[Any, Future] = listRolesEndpoint.serverLogic { _ =>
        Future.successful(Right(UserAccountRoles.values.toSeq.map(_.getRoleName)))
    }

    // deleteByUserName
    val deleteByUserNameEndpoint: Endpoint[Option[String], String, ErrorMsg, Unit, Any] =
        secureEndpoint
            .delete
            .in(base / path[String]("name"))
            .out(jsonBody[Unit])
            .name("deleteUserAccount")
            .description("Delete a user account by username. Requires an API key token or an administrator login.")
            .tag(tag)

    val deleteByUserNameEndpointImpl: ServerEndpoint[Any, Future] = deleteByUserNameEndpoint
        .serverSecurityLogic(jwtOpt => verifyApiKeyOrAdminAsync(jwtOpt))
        .serverLogic { _ => name =>
            handleSecureErrorsAsync(service.deleteByUserName(name))
        }

    // create
    val createEndpoint: Endpoint[Option[String], UserAccountCreate, ErrorMsg, UserAccountInfo, Any] =
        secureEndpoint
            .post
            .in(base)
            .in(
                oneOfBody(
                    jsonBody[UserAccountCreate].description(
                        "The user account to create. Accepts camelCase or snake_case."
                    ),
                    formBody[UserAccountCreate].description(
                        "The user account to create. Accepts camelCase or snake_case."
                    )
                )
            )
            .out(jsonBody[UserAccountInfo])
            .name("createUserAccount")
            .description(
                "Create a new user account. Accepts a JSON or form body. `role` defaults to ReadOnly and must be a " +
                    "valid role (400 otherwise). Requires an API key token, an administrator login, or a maintainer " +
                    "login; maintainers can only create ReadOnly accounts (403 otherwise)."
            )
            .tag(tag)

    val createEndpointImpl: ServerEndpoint[Any, Future] = createEndpoint
        .serverSecurityLogic(jwtOpt => resolveCallerAsync(jwtOpt, service.findByUserName))
        .serverLogic { caller => userAccount =>
            UserAccountAccess.checkCreate(caller, userAccount.role) match
                case Left(error) => Future.successful(Left(error))
                case Right(role) =>
                    // Store the canonical role name: access checks compare role names exactly
                    val account = userAccount.toUserAccount.copy(role = role.getRoleName)
                    handleSecureErrorsAsync(service.create(account).map(UserAccountInfo.from))
        }

    // update
    val updateEndpoint: Endpoint[Option[String], (String, UserAccountUpdate), ErrorMsg, UserAccountInfo, Any] =
        secureEndpoint
            .put
            .in(base / path[String]("name"))
            .in(jsonBody[UserAccountUpdate])
            .out(jsonBody[UserAccountInfo])
            .name("updateUserAccount")
            .description(
                "Update a user account. API key tokens and administrators can update any account, including its " +
                    "role. Other users can update only their own account and can't change its role (403 otherwise). " +
                    "Locked users can't update anything. `role` must be a valid role (400 otherwise). Returns 404 if " +
                    "the account doesn't exist."
            )
            .tag(tag)

    val updateEndpointImpl: ServerEndpoint[Any, Future] = updateEndpoint
        .serverSecurityLogic(jwtOpt => resolveCallerAsync(jwtOpt, service.findByUserName))
        .serverLogic { caller => (name, userAccountUpdate) =>
            UserAccountAccess.checkUpdate(caller, name, userAccountUpdate.role) match
                case Left(error) => Future.successful(Left(error))
                case Right(role) =>
                    // Store the canonical role name: access checks compare role names exactly
                    val update = userAccountUpdate.copy(role = role.map(_.getRoleName))
                    handleSecureErrorsAsync(service.update(name, update).map(UserAccountInfo.from))
        }

    override def all: List[Endpoint[?, ?, ?, ?, ?]] = List(
        listRolesEndpoint,
        findAllByRoleEndpoint,
        findByUserNameEndpoint,
        findAllEndpoint,
        deleteByUserNameEndpoint,
        createEndpoint,
        updateEndpoint
    )

    override def allImpl: List[ServerEndpoint[Any, Future]] = List(
        listRolesEndpointImpl,
        findAllByRoleEndpointImpl,
        findByUserNameEndpointImpl,
        findAllEndpointImpl,
        deleteByUserNameEndpointImpl,
        createEndpointImpl,
        updateEndpointImpl
    )
