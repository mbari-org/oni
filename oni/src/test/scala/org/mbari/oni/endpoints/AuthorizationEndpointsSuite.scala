/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2024
 *
 * oni code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential. 
 */

package org.mbari.oni.endpoints

import io.circe.parser.decode
import org.mbari.oni.domain.Authorization
import org.mbari.oni.etc.circe.CirceCodecs.given
import org.mbari.oni.etc.jwt.JwtService
import sttp.client3.SttpBackend
import sttp.client3.*
import sttp.client3.circe.*
import sttp.client3.testing.SttpBackendStub
import sttp.model.StatusCode
import sttp.tapir.server.interceptor.CustomiseInterceptors
import sttp.tapir.server.interceptor.exception.ExceptionHandler
import sttp.tapir.server.model.ValuedEndpointOutput
import sttp.tapir.server.stub.TapirStubInterpreter
import sttp.tapir.server.nima.NimaServerOptions
import sttp.tapir.server.nima.Id

import scala.concurrent.Await
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.duration.Duration

class AuthorizationEndpointsSuite extends munit.FunSuite:

    given ExecutionContext     = ExecutionContext.global
    given jwtService: JwtService            = new JwtService("mbari", "foo", "bar")
    val authorizationEndpoints = new AuthorizationEndpoints()

    test("auth"):

        // --- START: This block adds exception logging to the stub
        val exceptionHandler = ExceptionHandler.pure[Id](ctx =>
            Some(
                ValuedEndpointOutput(
                    sttp.tapir.stringBody.and(sttp.tapir.statusCode),
                    (s"failed due to ${ctx.e.getMessage}", StatusCode.InternalServerError)
                )
            )
        )

        val customOptions: CustomiseInterceptors[Id, NimaServerOptions] =
            NimaServerOptions
                .customiseInterceptors
                .exceptionHandler(exceptionHandler)
        // --- END: This block adds exception logging to the stub

        val backendStub: SttpBackend[Id, Any] =
            TapirStubInterpreter(customOptions, SttpBackendStub.synchronous)
                .whenServerEndpointRunLogic(authorizationEndpoints.authEndpointImpl)
                .backend()

        val response = basicRequest
            .post(uri"http://test.com/v1/auth")
            .header("Authorization", "APIKEY foo")
            .send(backendStub)

        response.body match
            case Left(e)  => fail(e)
            case Right(body) =>

                assertEquals(response.code, StatusCode.Ok)
                assert(response.body.isRight)

                // println(body)
                val d = decode[Authorization](body)
                assert(d.isRight)
                val bearerAuth = d.getOrElse(throw new Exception("No bearer auth"))
                assert(jwtService.verify(bearerAuth.accessToken))


