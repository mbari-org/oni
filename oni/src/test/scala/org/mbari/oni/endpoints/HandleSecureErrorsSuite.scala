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

import org.mbari.oni.{AccessDenied, AccessDeniedMissingCredentials, ItemNotFound}
import org.mbari.oni.domain.{Forbidden, NotFound, Unauthorized}
import sttp.tapir.server.ServerEndpoint

import scala.concurrent.Future

class HandleSecureErrorsSuite extends munit.FunSuite:

    private object TestEndpoints extends Endpoints:
        override def all: List[sttp.tapir.Endpoint[?, ?, ?, ?, ?]] = Nil
        override def allImpl: List[ServerEndpoint[Any, Future]]  = Nil

    import TestEndpoints.*

    test("missing credentials are a 401 with a message that says so") {
        handleSecureErrors(Left(AccessDeniedMissingCredentials)) match
            case Left(Unauthorized(message, code)) =>
                assertEquals(code, 401)
                assertEquals(message, MissingCredentialsMessage)
                assert(message.toLowerCase.contains("missing credentials"), message)
            case other                             => fail(s"Expected Unauthorized but got $other")
    }

    test("the exception's own message (used in logs) describes missing credentials, not a permission denial") {
        val message = AccessDeniedMissingCredentials.getMessage
        assert(message.toLowerCase.contains("missing credentials"), message)
        assert(!message.toLowerCase.contains("can not let you"), message)
    }

    test("a known user who isn't allowed is a 403 with the exception's message") {
        val denied = AccessDenied("someone")
        assertEquals(handleSecureErrors(Left(denied)), Left(Forbidden(denied.getMessage)))
    }

    test("other errors fall through to handleErrors") {
        assertEquals(handleSecureErrors(Left(ItemNotFound("nope"))), Left(NotFound("nope")))
        assertEquals(handleSecureErrors(Right(42)), Right(42))
    }
