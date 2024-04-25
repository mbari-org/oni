/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2024
 *
 * oni code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential. 
 */

package org.mbari.oni.endpoints

import org.mbari.oni.etc.jwt.JwtService
import sttp.model.StatusCode
import sttp.tapir.*
import sttp.tapir.Endpoint
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.server.ServerEndpoint
import org.mbari.oni.domain.{Authorization, BadRequest, ErrorMsg, NotFound, ServerError, Unauthorized}
import org.mbari.oni.etc.circe.CirceCodecs.given
import sttp.tapir.server.nima.Id

class AuthorizationEndpoints()(using jwtService: JwtService) extends Endpoints:

    private val base = "auth"
    private val tag  = "Authorization"

    val authEndpoint: Endpoint[String, Unit, ErrorMsg, Authorization, Any] =
        baseEndpoint
            .post
            .in(base)
            .securityIn(
                header[String]("Authorization").description(
                    "Header format is `Authorization: APIKEY <key>`"
                )
            )
            .out(jsonBody[Authorization])
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

    val authEndpointImpl: ServerEndpoint[Any, Id] =
        authEndpoint
            .serverSecurityLogicPure(authHeader =>
                val parts = authHeader.split(" ")
                if parts.length != 2 || parts(0).toUpperCase() != "APIKEY" then
                    Left(Unauthorized("Invalid Authorization header"))
                else
                    val apiKey = parts(1)
                    jwtService.authorize(apiKey) match
                        case None      => Left(Unauthorized("Invalid API key"))
                        case Some(jwt) => Right(Authorization.bearer(jwt))
            )
            .serverLogic(bearerAuth => Unit => Right(bearerAuth))

    override val all: List[Endpoint[?, ?, ?, ?, ?]]         = List(authEndpoint)
    override val allImpl: List[ServerEndpoint[Any, Id]] =
        List(authEndpointImpl)
