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

package org.mbari.oni.etc.sdk

import org.mbari.oni.etc.sdk.IO.*
import Futures.*
import scala.concurrent.ExecutionContext
import scala.concurrent.Await

class IOSuite extends munit.FunSuite:

    val io: IO[Int, String] = i => Right(i.toString())

    test("unit") {
        val a = IO.unit
        val result = a(1)
        assertEquals(result, Right(()))
    }

    test("map") {
        val a = io.map(i => i + 1)
        assertEquals(a(1), Right("11"))
    }

    test("flatMap") {
        val a = io.flatMap(i => Right(i + 1))
        assertEquals(a(1), Right("11"))
    }

    test("foreach") {
        var result = ""
        io(1).foreach(i => result = i)
        assertEquals(result, "1")
    }

    test("async (Success)") {
        import scala.concurrent.ExecutionContext.Implicits.global
        val a = io.async
        val result = Await.result(a(1), scala.concurrent.duration.Duration("1s"))
        assertEquals(result, "1")
    }

    test("async (Failure)") {
        import scala.concurrent.ExecutionContext.Implicits.global
        def testFun(a: Int): Either[Throwable, String] = Left(new RuntimeException("Boom!"))
        val io = testFun.async
        interceptMessage[RuntimeException]("Boom!") {
            Await.result(io(1), scala.concurrent.duration.Duration("1s"))
        }
    }
