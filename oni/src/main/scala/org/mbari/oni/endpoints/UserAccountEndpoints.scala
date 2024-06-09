/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2024
 *
 * oni code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential. 
 */

package org.mbari.oni.endpoints

import jakarta.persistence.EntityManagerFactory
import org.mbari.oni.domain.{ErrorMsg, NotFound, UserAccount, UserAccountUpdate}
import org.mbari.oni.etc.jwt.JwtService
import org.mbari.oni.services.UserAccountService
import sttp.tapir.*
import sttp.tapir.Endpoint
import sttp.tapir.json.circe.*
import sttp.tapir.server.ServerEndpoint
import sttp.shared.Identity
import org.mbari.oni.etc.circe.CirceCodecs.{*, given}

class UserAccountEndpoints(entityManagerFactory: EntityManagerFactory)(using jwtService: JwtService) extends Endpoints:

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

    val findAllEndpointImpl: ServerEndpoint[Any, Identity] = findAllEndpoint.serverLogic { _ =>
        handleErrors(service.findAll())
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

    val findByUserNameEndpointImpl: ServerEndpoint[Any, Identity] = findByUserNameEndpoint.serverLogic { name =>
        handleErrors(service.findByUserName(name)).flatMap {
            case None        => Left(NotFound(s"User account not found: $name"))
            case Some(value) => Right(value)
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

    val findAllByRoleEndpointImpl: ServerEndpoint[Any, Identity] = findAllByRoleEndpoint.serverLogic { role =>
        handleErrors(service.findAllByRole(role))
    }

    // deleteByUserName
    val deleteByUserNameEndpoint: Endpoint[Unit, String, ErrorMsg, Unit, Any] =
        openEndpoint
            .delete
            .in(base / path[String]("name"))
            .out(jsonBody[Unit])
            .name("deleteUserAccount")
            .description("Delete a user account by username")
            .tag(tag)

    val deleteByUserNameEndpointImpl: ServerEndpoint[Any, Identity] = deleteByUserNameEndpoint.serverLogic { name =>
        handleErrors(service.deleteByUserName(name))
    }

    // create
    val createEndpoint: Endpoint[Option[String], UserAccount, ErrorMsg, UserAccount, Any] =
        secureEndpoint
            .post
            .in(base)
            .in(jsonBody[UserAccount])
            .out(jsonBody[UserAccount])
            .name("createUserAccount")
            .description("Create a new user account")
            .tag(tag)

    val createEndpointImpl: ServerEndpoint[Any, Identity] = createEndpoint
        .serverSecurityLogic(jwtOpt => verify(jwtOpt))
        .serverLogic { _ => userAccount =>
            handleErrors(service.create(userAccount))
        }

    // update
    val updateEndpoint: Endpoint[Option[String], (String, UserAccountUpdate), ErrorMsg, UserAccount, Any] =
        secureEndpoint
            .put
            .in(base / path[String]("username"))
            .in(jsonBody[UserAccountUpdate])
            .out(jsonBody[UserAccount])
            .name("updateUserAccount")
            .description("Update a user account")
            .tag(tag)

    val updateEndpointImpl: ServerEndpoint[Any, Identity] = updateEndpoint
        .serverSecurityLogic(jwtOpt => verify(jwtOpt))
        .serverLogic { _ => (username, userAccountUpdate) =>
            handleErrors(service.update(username, userAccountUpdate))
        }

    override def all: List[Endpoint[_, _, _, _, _]] = List(
        findAllByRoleEndpoint,
        findByUserNameEndpoint,
        findAllEndpoint,
        deleteByUserNameEndpoint,
        createEndpoint,
        updateEndpoint
    )

    override def allImpl: List[ServerEndpoint[Any, Identity]] = List(
        findAllByRoleEndpointImpl,
        findByUserNameEndpointImpl,
        findAllEndpointImpl,
        deleteByUserNameEndpointImpl,
        createEndpointImpl,
        updateEndpointImpl
    )
