/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2024
 *
 * oni code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential.
 */

package org.mbari.oni.endpoints

import io.circe.Printer
import org.mbari.oni.ConceptNotFoundException
import org.mbari.oni.config.AppConfig
import org.mbari.oni.domain.*
import org.mbari.oni.etc.circe.CirceCodecs
import org.mbari.oni.etc.circe.CirceCodecs.given
import org.mbari.oni.etc.jdk.Loggers.given
import org.mbari.oni.etc.jwt.JwtService
import sttp.model.StatusCode
import sttp.model.headers.WWWAuthenticateChallenge
import sttp.shared.Identity
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.server.ServerEndpoint

import java.net.{URI, URL}
import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}

case class Paging(offset: Option[Int] = Some(0), limit: Option[Int] = Some(100))

object CustomTapirJsonCirce extends TapirJsonCirce:
    override def jsonPrinter: Printer = CirceCodecs.CustomPrinter

trait Endpoints:

    import CustomTapirJsonCirce.*

    val log: System.Logger = System.getLogger(getClass.getName)

    // --- Schemas
    implicit lazy val sCount: Schema[Count]                                         = Schema.derived[Count]
    implicit lazy val sExtendedHistory: Schema[ExtendedHistory]                     = Schema.derived[ExtendedHistory]
    implicit lazy val sExtendedLink: Schema[ExtendedLink]                           = Schema.derived[ExtendedLink]
    implicit lazy val sLink: Schema[Link]                                           = Schema.derived[Link]
    implicit lazy val sURI: Schema[URI]                                             = Schema.string
    implicit lazy val sURL: Schema[URL]                                             = Schema.string
    implicit lazy val sInstant: Schema[Instant]                                     = Schema.string
    implicit lazy val sDoi: Schema[ReferenceQuery]                                  = Schema.derived[ReferenceQuery]
    implicit lazy val sLinkCreate: Schema[LinkCreate]                               = Schema.derived[LinkCreate]
    implicit lazy val sRenameToConceptRequest: Schema[LinkRenameToConceptRequest]   =
        Schema.derived[LinkRenameToConceptRequest]
    implicit lazy val sRenameToConceptResponse: Schema[LinkRenameToConceptResponse] =
        Schema.derived[LinkRenameToConceptResponse]
    implicit lazy val sLinkUpdate: Schema[LinkUpdate]                               = Schema.derived[LinkUpdate]
    implicit lazy val sMedia: Schema[Media]                                         = Schema.derived[Media]
    implicit lazy val sMediaCreate: Schema[MediaCreate]                             = Schema.derived[MediaCreate]
    implicit lazy val sMediaUpdate: Schema[MediaUpdate]                             = Schema.derived[MediaUpdate]
    implicit lazy val sPaging: Schema[Paging]                                       = Schema.derived[Paging]
    implicit lazy val sPrefNode: Schema[PrefNode]                                   = Schema.derived[PrefNode]
    implicit lazy val sPrefNodeUpdate: Schema[PrefNodeUpdate]                       = Schema.derived[PrefNodeUpdate]
    implicit lazy val sReference: Schema[Reference]                                 = Schema.derived[Reference]
    implicit lazy val sReferenceUpdate: Schema[ReferenceUpdate]                     = Schema.derived[ReferenceUpdate]
    implicit lazy val sConceptCreate: Schema[ConceptCreate]                         = Schema.derived[ConceptCreate]
    implicit lazy val sConceptDelete: Schema[ConceptDelete]                         = Schema.derived[ConceptDelete]
    implicit lazy val sConceptNameCreate: Schema[ConceptNameCreate]                 = Schema.derived[ConceptNameCreate]
    implicit lazy val sConceptNameUpdate: Schema[ConceptNameUpdate]                 = Schema.derived[ConceptNameUpdate]
    implicit lazy val sConceptUpdate: Schema[ConceptUpdate]                         = Schema.derived[ConceptUpdate]
    implicit lazy val sConceptMetadata: Schema[ConceptMetadata]                     = Schema.derived[ConceptMetadata]
    implicit lazy val sConceptName: Schema[RawConceptName]                          = Schema.derived[RawConceptName]
    implicit lazy val sPageSeqExtendedLink: Schema[Page[Seq[ExtendedLink]]]         =
        Schema.derived[Page[Seq[ExtendedLink]]]
    implicit lazy val sPageSeqExtendedHistory: Schema[Page[Seq[ExtendedHistory]]]   =
        Schema.derived[Page[Seq[ExtendedHistory]]]
    implicit lazy val sPageSeqString: Schema[Page[Seq[String]]]                     = Schema.derived[Page[Seq[String]]]
    implicit lazy val sPageSeqReference: Schema[Page[Seq[Reference]]]               = Schema.derived[Page[Seq[Reference]]]
    implicit lazy val sRank: Schema[Rank]                                           = Schema.derived[Rank]
    implicit lazy val sUserAccount: Schema[UserAccount]                             = Schema.derived[UserAccount]
    implicit lazy val sUserAccountCreate: Schema[UserAccountCreate]                 = Schema.derived[UserAccountCreate]
    implicit lazy val sUserAccountUpdate: Schema[UserAccountUpdate]                 = Schema.derived[UserAccountUpdate]

    // Make Tapir recursive types happy by using `implicit def`, not lazy val
    // https://tapir.softwaremill.com/en/latest/endpoint/schemas.html#derivation-for-recursive-types-in-scala3
    implicit def sConcept: Schema[Concept]           = Schema.derived[Concept]
    implicit def sRawConcept: Schema[RawConcept]     = Schema.derived[RawConcept]
    implicit def sSerdeConcept: Schema[SerdeConcept] = Schema.derived[SerdeConcept]

    // --- Abstract methods
    def all: List[Endpoint[?, ?, ?, ?, ?]]
    def allImpl: List[ServerEndpoint[Any, Future]]

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
            .description("Offset for paging")
            .and(query[Option[Int]]("limit").description("Limit for paging"))
            .mapTo[Paging]

    val openEndpoint: Endpoint[Unit, Unit, ErrorMsg, Unit, Any] = baseEndpoint.errorOut(
        oneOf[ErrorMsg](
            oneOfVariant(statusCode(StatusCode.BadRequest).and(jsonBody[BadRequest])),
            oneOfVariant(statusCode(StatusCode.NotFound).and(jsonBody[NotFound])),
            oneOfVariant(statusCode(StatusCode.InternalServerError).and(jsonBody[ServerError]))
        )
    )

    def handleErrors[T](f: => Either[Throwable, T]): Either[ErrorMsg, T] =
        f match
            case Right(concept)                    => Right(concept)
            case Left(c: ConceptNotFoundException) => Left(NotFound(c.getMessage))
            case Left(e)                           =>
                log.atError.withCause(e).log("Error")
                Left(ServerError(e.getMessage))

//        f.fold(
//            e =>
//                log.atError.withCause(e).log("Error")
//                Left(ServerError(e.getMessage))
//            ,
//            Right(_)
//        )

    def handleErrorsAsync[T](f: => Either[Throwable, T])(using ec: ExecutionContext): Future[Either[ErrorMsg, T]] =
        Future(handleErrors(f))

    def handleOption[T](f: => Option[T]): Either[ErrorMsg, T] =
        f match
            case Some(t) => Right(t)
            case None    => Left(NotFound("Not found"))

    def handleOptionAsync[T](f: => Option[T])(using executionContext: ExecutionContext): Future[Either[ErrorMsg, T]] =
        Future(handleOption(f))

    def verify(
        jwtOpt: Option[String]
    )(using jwtService: JwtService): Identity[Either[Unauthorized, Unit]] =
        jwtOpt match
            case None      => Left(Unauthorized("Missing token"))
            case Some(jwt) =>
                if jwtService.verify(jwt) then Right(())
                else Left(Unauthorized("Invalid token"))

    def verifyAsync(
        jwtOpt: Option[String]
    )(using jwtService: JwtService, executionContext: ExecutionContext): Future[Either[Unauthorized, Unit]] =
        Future(verify(jwtOpt))

    def verifyLogin(
        jwtOpt: Option[String]
    )(using jwtService: JwtService): Identity[Either[Unauthorized, UserAccount]] =
        jwtOpt match
            case None      => Left(Unauthorized("Missing token"))
            case Some(jwt) =>
                jwtService.decode(jwt) match
                    case None              => Left(Unauthorized("Invalid token"))
                    case Some(userAccount) => Right(userAccount)

    def verifyLoginAsync(
        jwtOpt: Option[String]
    )(using jwtService: JwtService, executionContext: ExecutionContext): Future[Either[Unauthorized, UserAccount]] =
        Future(verifyLogin(jwtOpt))
