/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2024
 *
 * oni code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential.
 */

package org.mbari.oni.endpoints

import sttp.tapir.json.circe.TapirJsonCirce
import io.circe.Printer
import org.mbari.oni.etc.circe.CirceCodecs
import org.mbari.oni.etc.circe.CirceCodecs.{*, given}
import org.mbari.oni.domain.*
import sttp.model.StatusCode
import sttp.model.headers.WWWAuthenticateChallenge
import sttp.tapir.*
import sttp.tapir.Endpoint
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.server.nima.Id
import org.mbari.oni.etc.jdk.Loggers.given
import scala.concurrent.ExecutionContext
import org.mbari.oni.etc.jwt.JwtService
import org.mbari.oni.AppConfig

case class Paging(offset: Option[Int] = Some(0), limit: Option[Int] = Some(100))

object CustomTapirJsonCirce extends TapirJsonCirce:
    override def jsonPrinter: Printer = CirceCodecs.CustomPrinter

trait Endpoints:

    import CustomTapirJsonCirce.*

    val log: System.Logger = System.getLogger(getClass.getName)

    // --- Schemas
    implicit lazy val sConcept: Schema[Concept] = Schema.derived[Concept]

    // --- Abstract methods
    def all: List[Endpoint[?, ?, ?, ?, ?]]
    def allImpl: List[ServerEndpoint[Any, Id]]

    // hard coded ATM, but could be configurable
    val baseEndpoint: Endpoint[Unit, Unit, Unit, Unit, Any] = endpoint.in(AppConfig.DefaultHttpConfig.contextPath)

    val secureEndpoint: Endpoint[Option[String], Unit, ErrorMsg, Unit, Any] = baseEndpoint
        .securityIn(auth.bearer[Option[String]](WWWAuthenticateChallenge.bearer))
        .errorOut(
            oneOf[ErrorMsg](
                oneOfVariant(statusCode(StatusCode.BadRequest).and(jsonBody[BadRequest])),
                oneOfVariant(statusCode(StatusCode.NotFound).and(jsonBody[NotFound])),
                oneOfVariant(statusCode(StatusCode.InternalServerError).and(jsonBody[ServerError])),
                oneOfVariant(statusCode(StatusCode.Unauthorized).and(jsonBody[Unauthorized]))
            )
        )

    val paging: EndpointInput[Paging] =
        query[Option[Int]]("offset")
            .and(query[Option[Int]]("limit"))
            .mapTo[Paging]

    val openEndpoint: Endpoint[Unit, Unit, ErrorMsg, Unit, Any] = baseEndpoint.errorOut(
        oneOf[ErrorMsg](
            oneOfVariant(statusCode(StatusCode.BadRequest).and(jsonBody[BadRequest])),
            oneOfVariant(statusCode(StatusCode.NotFound).and(jsonBody[NotFound])),
            oneOfVariant(statusCode(StatusCode.InternalServerError).and(jsonBody[ServerError]))
        )
    )

    def handleErrors[T](f: => Either[Throwable, T]): Either[ErrorMsg, T] =
        f.fold(
            e =>
                log.atError.withCause(e).log("Error")
                Left(ServerError(e.getMessage))
            ,
            Right(_)
        )

    def handleOption[T](f: => Option[T]): Either[ErrorMsg, T] =
        f match
            case Some(t) => Right(t)
            case None    => Left(NotFound("Not found"))

    def verify(
        jwtOpt: Option[String]
    )(using jwtService: JwtService): Either[Unauthorized, Unit] =
        jwtOpt match
            case None      => Left(Unauthorized("Missing token"))
            case Some(jwt) =>
                if jwtService.verify(jwt) then Right(())
                else Left(Unauthorized("Invalid token"))
