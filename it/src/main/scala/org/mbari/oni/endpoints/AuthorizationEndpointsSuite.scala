/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2024
 *
 * oni code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential. 
 */

package org.mbari.oni.endpoints

import io.circe.parser.decode
import org.mbari.oni.domain.{Authorization, UserAccount, UserAccountRoles}
import org.mbari.oni.etc.circe.CirceCodecs.given
import org.mbari.oni.etc.jwt.JwtService
import org.mbari.oni.jpa.DatabaseFunSuite
import org.mbari.oni.jpa.entities.TestEntityFactory
import org.mbari.oni.services.UserAccountService
import sttp.client3.*
import sttp.client3.circe.*
import sttp.client3.testing.SttpBackendStub
import sttp.model.StatusCode
import sttp.tapir.server.interceptor.CustomiseInterceptors
import sttp.tapir.server.interceptor.exception.ExceptionHandler
import sttp.tapir.server.model.ValuedEndpointOutput
import sttp.tapir.server.nima.{Id, NimaServerOptions}
import sttp.tapir.server.stub.TapirStubInterpreter

import java.util.Base64


trait AuthorizationEndpointsSuite extends DatabaseFunSuite with EndpointsSuite:

    given jwtService: JwtService            = JwtService("mbari", "foo", "bar")
    lazy val authorizationEndpoints = new AuthorizationEndpoints(entityManagerFactory)

    test("auth"):

        val backendStub = newBackendStub(authorizationEndpoints.authEndpointImpl)

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

    test("login"):
        val userService = UserAccountService(entityManagerFactory)
        val userAccount = UserAccount(
            "test1234",
            "SuperSecretPassword",
            UserAccountRoles.ADMINISTRATOR.getRoleName,
            isEncrypted = Some(false)
        )
        userService.create(userAccount) match
            case Left(e) => fail(e.getMessage)
            case Right(ua) =>

                val backendStub = newBackendStub(authorizationEndpoints.loginEndpointImpl)

                val credentials = Base64.getEncoder.encodeToString(s"${ua.username}:${userAccount.password}".getBytes)
                val response = basicRequest
                    .post(uri"http://test.com/v1/auth/login")
                    .header("Authorization", s"BASIC $credentials")
                    .send(backendStub)

                response.body match
                    case Left(e)  =>
                        fail(e)
                    case Right(body) =>

                        assertEquals(response.code, StatusCode.Ok)
                        assert(response.body.isRight)

                        // println(body)
                        val d = decode[Authorization](body)
                        assert(d.isRight)
                        val bearerAuth = d.getOrElse(throw new Exception("No bearer auth"))
                        assert(jwtService.verify(bearerAuth.accessToken))


