/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2024
 *
 * oni code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential.
 */

package org.mbari.oni.endpoints

import jakarta.persistence.EntityManagerFactory
import org.mbari.oni.domain.{ErrorMsg, NotFound, UserAccount, UserAccountCreate, UserAccountUpdate}
import org.mbari.oni.etc.circe.CirceCodecs.{*, given}
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

    // findAll
    val findAllEndpoint: Endpoint[Unit, Unit, ErrorMsg, Seq[UserAccount], Any] =
        openEndpoint
            .get
            .in(base)
            .out(jsonBody[Seq[UserAccount]])
            .name("allUserAccounts")
            .description("Get all user accounts")
            .tag(tag)

    val findAllEndpointImpl: ServerEndpoint[Any, Future] = findAllEndpoint.serverLogic { _ =>
        handleErrorsAsync(service.findAll())
    }

    // findByUserName
    val findByUserNameEndpoint: Endpoint[Unit, String, ErrorMsg, UserAccount, Any] =
        openEndpoint
            .get
            .in(base / path[String]("name"))
            .out(jsonBody[UserAccount])
            .name("userAccountByName")
            .description("Find a user account by username")
            .tag(tag)

    val findByUserNameEndpointImpl: ServerEndpoint[Any, Future] = findByUserNameEndpoint.serverLogic { name =>
        Future {
            handleErrors(service.findByUserName(name)).flatMap {
                case None        => Left(NotFound(s"User account not found: $name"))
                case Some(value) => Right(value)
            }
        }
    }

    // findAllByRole
    val findAllByRoleEndpoint: Endpoint[Unit, String, ErrorMsg, Seq[UserAccount], Any] =
        openEndpoint
            .get
            .in(base / "role" / path[String]("role"))
            .out(jsonBody[Seq[UserAccount]])
            .name("userAccountsByRole")
            .description("Find all user accounts by role")
            .tag(tag)

    val findAllByRoleEndpointImpl: ServerEndpoint[Any, Future] = findAllByRoleEndpoint.serverLogic { role =>
        handleErrorsAsync(service.findAllByRole(role))
    }

    // deleteByUserName
    val deleteByUserNameEndpoint: Endpoint[Option[String], String, ErrorMsg, Unit, Any] =
        secureEndpoint
            .delete
            .in(base / path[String]("name"))
            .out(jsonBody[Unit])
            .name("deleteUserAccount")
            .description("Delete a user account by username")
            .tag(tag)

    val deleteByUserNameEndpointImpl: ServerEndpoint[Any, Future] = deleteByUserNameEndpoint
        .serverSecurityLogic(jwtOpt => verifyAsync(jwtOpt))
        .serverLogic { _ => name =>
            handleErrorsAsync(service.deleteByUserName(name))
        }

    // create
    val createEndpoint: Endpoint[Option[String], UserAccountCreate, ErrorMsg, UserAccount, Any] =
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
            .out(jsonBody[UserAccount])
            .name("createUserAccount")
            .description("Create a new user account")
            .tag(tag)

    val createEndpointImpl: ServerEndpoint[Any, Future] = createEndpoint
        .serverSecurityLogic(jwtOpt => verifyAsync(jwtOpt))
        .serverLogic { _ => userAccount =>
            handleErrorsAsync(service.create(userAccount.toUserAccount))
        }

    // update
    val updateEndpoint: Endpoint[Option[String], (String, UserAccountUpdate), ErrorMsg, UserAccount, Any] =
        secureEndpoint
            .put
            .in(base / path[String]("name"))
            .in(jsonBody[UserAccountUpdate])
            .out(jsonBody[UserAccount])
            .name("updateUserAccount")
            .description("Update a user account")
            .tag(tag)

    val updateEndpointImpl: ServerEndpoint[Any, Future] = updateEndpoint
        .serverSecurityLogic(jwtOpt => verifyAsync(jwtOpt))
        .serverLogic { _ => (name, userAccountUpdate) =>
            handleErrorsAsync(service.update(name, userAccountUpdate))
        }

    override def all: List[Endpoint[?, ?, ?, ?, ?]] = List(
        findAllByRoleEndpoint,
        findByUserNameEndpoint,
        findAllEndpoint,
        deleteByUserNameEndpoint,
        createEndpoint,
        updateEndpoint
    )

    override def allImpl: List[ServerEndpoint[Any, Future]] = List(
        findAllByRoleEndpointImpl,
        findByUserNameEndpointImpl,
        findAllEndpointImpl,
        deleteByUserNameEndpointImpl,
        createEndpointImpl,
        updateEndpointImpl
    )
