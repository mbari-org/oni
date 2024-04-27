/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2024
 *
 * oni code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential. 
 */

package org.mbari.oni.endpoints


import scala.concurrent.ExecutionContext
import sttp.client3.*
import sttp.client3.SttpBackend
import sttp.client3.testing.SttpBackendStub
import sttp.model.StatusCode
import scala.concurrent.Future
import sttp.tapir.server.stub.TapirStubInterpreter
import scala.util.Failure
import scala.util.Success
import sttp.tapir.server.interceptor.exception.ExceptionHandler
import sttp.tapir.server.model.ValuedEndpointOutput
import sttp.tapir.server.interceptor.CustomiseInterceptors
import sttp.tapir.server.nima.NimaServerOptions
import sttp.tapir.server.nima.Id
import org.checkerframework.checker.units.qual.m

class HealthEndpointsSuite extends munit.FunSuite:
    given ExecutionContext = ExecutionContext.global
    val healthEndpoints    = new HealthEndpoints

    test("health"):

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
        // println(HealthStatus.default)

        val backendStub: SttpBackend[Id, Any] =
            TapirStubInterpreter(customOptions, SttpBackendStub.synchronous)
                .whenServerEndpointRunLogic(healthEndpoints.healthEndpointImpl)
                .backend()

        val request  = basicRequest.get(uri"http://test.com/v1/health")
        val response = request.send(backendStub)
        response.body match
            case Left(e) => fail(e)
            case Right(r) => assertEquals(response.code, StatusCode.Ok)

