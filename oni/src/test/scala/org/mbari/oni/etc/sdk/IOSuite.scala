/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2024
 *
 * oni code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential.
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
