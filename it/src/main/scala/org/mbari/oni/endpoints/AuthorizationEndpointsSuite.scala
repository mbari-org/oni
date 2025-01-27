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

import io.circe.parser.decode
import org.mbari.oni.domain.{AuthorizationSC, UserAccount, UserAccountRoles}
import org.mbari.oni.etc.circe.CirceCodecs.given
import org.mbari.oni.etc.jwt.JwtService
import org.mbari.oni.jpa.DatabaseFunSuite
import org.mbari.oni.services.UserAccountService
import sttp.client3.*
import sttp.model.StatusCode
import org.mbari.oni.etc.sdk.Futures.*

import java.util.Base64
import scala.concurrent.ExecutionContext

trait AuthorizationEndpointsSuite extends DatabaseFunSuite with EndpointsSuite:

    given jwtService: JwtService    = JwtService("mbari", "foo", "bar")
    lazy val authorizationEndpoints = new AuthorizationEndpoints(entityManagerFactory)

    test("auth"):

        val backendStub = newBackendStub(authorizationEndpoints.authEndpointImpl)

        val response = basicRequest
            .post(uri"http://test.com/v1/auth")
            .header("Authorization", "APIKEY foo")
            .send(backendStub)
            .join


        response.body match
            case Left(e)     => fail(e)
            case Right(body) =>
                assertEquals(response.code, StatusCode.Ok)
                assert(response.body.isRight)

                // println(body)
                val d          = decode[AuthorizationSC](body)
                assert(d.isRight)
                val bearerAuth = d.getOrElse(throw new Exception("No bearer auth"))
                assert(jwtService.verify(bearerAuth.access_token))

    test("login (ADMINISTRATOR)"):
        val userService = UserAccountService(entityManagerFactory)
        val userAccount = UserAccount(
            "test1234",
            "SuperSecretPassword",
            UserAccountRoles.ADMINISTRATOR.getRoleName,
            isEncrypted = Some(false)
        )
        userService.create(userAccount) match
            case Left(e)   => fail(e.getMessage)
            case Right(ua) =>
                val backendStub = newBackendStub(authorizationEndpoints.loginEndpointImpl)

                val credentials = Base64.getEncoder.encodeToString(s"${ua.username}:${userAccount.password}".getBytes)
                val response    = basicRequest
                    .post(uri"http://test.com/v1/auth/login")
                    .header("Authorization", s"BASIC $credentials")
                    .send(backendStub)
                    .join

                response.body match
                    case Left(e)     =>
                        fail(e)
                    case Right(body) =>
                        assertEquals(response.code, StatusCode.Ok)
                        assert(response.body.isRight)

                        // println(body)
                        val d          = decode[AuthorizationSC](body)
                        assert(d.isRight)
                        val bearerAuth = d.getOrElse(throw new Exception("No bearer auth"))
                        assert(jwtService.verify(bearerAuth.access_token))

    test("login (READONLY)"):
        val userService = UserAccountService(entityManagerFactory)
        val userAccount = UserAccount(
            "test12345",
            "SuperSecretPassword",
            UserAccountRoles.READONLY.getRoleName,
            isEncrypted = Some(false)
        )
        userService.create(userAccount) match
            case Left(e)   => fail(e.getMessage)
            case Right(ua) =>
                val backendStub = newBackendStub(authorizationEndpoints.loginEndpointImpl)

                val credentials = Base64.getEncoder.encodeToString(s"${ua.username}:${userAccount.password}".getBytes)
                val response    = basicRequest
                    .post(uri"http://test.com/v1/auth/login")
                    .header("Authorization", s"BASIC $credentials")
                    .send(backendStub)
                    .join

                response.body match
                    case Left(e)     =>
                        // this is expected. READONLY users cannot login
                    case Right(body) =>
                        fail("READONLY user should not be able to login")
