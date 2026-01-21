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
import org.checkerframework.checker.units.qual.m
import sttp.tapir.server.interceptor.CustomiseInterceptors
import sttp.tapir.server.vertx.VertxFutureServerOptions
import org.mbari.oni.etc.sdk.Futures.*

class HealthEndpointsSuite extends munit.FunSuite:
    given ExecutionContext = ExecutionContext.global
    val healthEndpoints    = new HealthEndpoints

    test("health"):

        // --- START: This block adds exception logging to the stub
        val exceptionHandler = ExceptionHandler.pure[Future](ctx =>
            Some(
                ValuedEndpointOutput(
                    sttp.tapir.stringBody.and(sttp.tapir.statusCode),
                    (s"failed due to ${ctx.e.getMessage}", StatusCode.InternalServerError)
                )
            )
        )

        val customOptions: CustomiseInterceptors[Future, VertxFutureServerOptions] =
            VertxFutureServerOptions
                .customiseInterceptors
                .exceptionHandler(exceptionHandler)
        // --- END: This block adds exception logging to the stub
        // println(HealthStatus.default)

        val backendStub: SttpBackend[Future, Any] =
            TapirStubInterpreter(customOptions, SttpBackendStub.asynchronousFuture)
                .whenServerEndpointRunLogic(healthEndpoints.healthEndpointImpl)
                .backend()

        val request  = basicRequest.get(uri"http://test.com/v1/health")
        val response = request.send(backendStub).join
        response.body match
            case Left(e) => fail(e)
            case Right(r) => assertEquals(response.code, StatusCode.Ok)

