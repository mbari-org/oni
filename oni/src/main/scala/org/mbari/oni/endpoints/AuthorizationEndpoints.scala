/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2024
 *
 * oni code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential.
 */

package org.mbari.oni.endpoints

import jakarta.persistence.EntityManagerFactory
import org.mbari.oni.etc.jwt.JwtService
import sttp.model.StatusCode
import sttp.tapir.*
import sttp.tapir.Endpoint
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.server.ServerEndpoint
import org.mbari.oni.domain.{AuthorizationSC, BadRequest, ErrorMsg, NotFound, ServerError, Unauthorized}
import org.mbari.oni.etc.circe.CirceCodecs.given
import org.mbari.oni.services.UserAccountService
import sttp.tapir.model.UsernamePassword
import sttp.shared.Identity
import sttp.model.headers.{AuthenticationScheme, WWWAuthenticateChallenge}

import scala.concurrent.{ExecutionContext, Future}

class AuthorizationEndpoints(entityManagerFactory: EntityManagerFactory)(using
    jwtService: JwtService,
    executionContext: ExecutionContext
) extends Endpoints:

    private val service = UserAccountService(entityManagerFactory)
    private val base    = "auth"
    private val tag     = "Authorization"

    val authEndpoint: Endpoint[String, Unit, ErrorMsg, AuthorizationSC, Any] =
        baseEndpoint
            .post
            .in(base)
            .securityIn(
                header[String]("Authorization").description(
                    "Header format is `Authorization: APIKEY <key>`"
                )
            )
            .out(jsonBody[AuthorizationSC])
            .errorOut(
                oneOf[ErrorMsg](
                    oneOfVariant(statusCode(StatusCode.BadRequest).and(jsonBody[BadRequest])),
                    oneOfVariant(statusCode(StatusCode.NotFound).and(jsonBody[NotFound])),
                    oneOfVariant(
                        statusCode(StatusCode.InternalServerError).and(jsonBody[ServerError])
                    ),
                    oneOfVariant(statusCode(StatusCode.Unauthorized).and(jsonBody[Unauthorized]))
                )
            )
            .name("authenticate")
            .description(
                "Exchange an API key for a JWT. Header format is `Authorization: APIKEY <key>`"
            )
            .tag(tag)

    val authEndpointImpl: ServerEndpoint[Any, Future] =
        authEndpoint
            .serverSecurityLogicPure(authHeader =>
                val parts = authHeader.split(" ")
                if parts.length != 2 || parts(0).toUpperCase() != "APIKEY" then
                    Left(Unauthorized("Invalid Authorization header"))
                else
                    val apiKey = parts(1)
                    jwtService.authorize(apiKey) match
                        case None      => Left(Unauthorized("Invalid API key"))
                        case Some(jwt) => Right(AuthorizationSC.bearer(jwt))
            )
            .serverLogic(bearerAuth => Unit => Future(Right(bearerAuth)))

    val loginEndpoint =
        baseEndpoint
            .post
            .in(base / "login")
            .securityIn(auth.basic[UsernamePassword](WWWAuthenticateChallenge.basic(AuthenticationScheme.Basic.name)))
            .out(jsonBody[AuthorizationSC])
            .errorOut(
                oneOf[ErrorMsg](
                    oneOfVariant(statusCode(StatusCode.BadRequest).and(jsonBody[BadRequest])),
                    oneOfVariant(statusCode(StatusCode.NotFound).and(jsonBody[NotFound])),
                    oneOfVariant(
                        statusCode(StatusCode.InternalServerError).and(jsonBody[ServerError])
                    ),
                    oneOfVariant(statusCode(StatusCode.Unauthorized).and(jsonBody[Unauthorized]))
                )
            )
            .name("login")
            .description("Login with username and password")
            .tag(tag)

    val loginEndpointImpl: ServerEndpoint[Any, Future] =
        loginEndpoint
            .serverSecurityLogicPure { usernamePassword =>
                for
                    userAccount <- service
                                       .findByUserName(usernamePassword.username)
                                       .fold(
                                           e => Left(ServerError(e.getMessage)),
                                           _.toRight(NotFound("User account not found"))
                                       )
                    entity      <- Right(userAccount.toEntity)
                    jwt         <-
                        jwtService
                            .login(usernamePassword.username, usernamePassword.password.getOrElse(""), entity)
                            .toRight(
                                Unauthorized(
                                    "Unable to login. Check your username and password and verify that you are an administrator or maintainer"
                                )
                            )
                yield AuthorizationSC.bearer(jwt)
            }
            .serverLogic(bearerAuth => Unit => Future(Right(bearerAuth)))

    override val all: List[Endpoint[?, ?, ?, ?, ?]]         = List(loginEndpoint, authEndpoint)
    override val allImpl: List[ServerEndpoint[Any, Future]] =
        List(loginEndpointImpl, authEndpointImpl)
