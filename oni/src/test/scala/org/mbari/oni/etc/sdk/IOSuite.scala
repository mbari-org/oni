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

class IOSuite extends munit.FunSuite {

    val io: IO[Int, String] = i => Right(i.toString())
  

    test("map") {
        val a = io.map(i => i + 1)
        a(1) match {
            case Right(value) => assertEquals(value, "11")
            case Left(_) => fail("Expected Right")
        }
    }

    test("flatMap") {
        val a = io.flatMap(i => Right(i + 1))
        a(1) match {
            case Right(value) => assertEquals(value, "11")
            case Left(_) => fail("Expected Right")
        }
    }

    test("foreach") {
        var result = ""
        io(1).foreach(i => result = i)
        assertEquals(result, "1")
    }

    test("async") {
        given ExecutionContext = ExecutionContext.global
        val a = io.async
        a(1).join match {
            case Right(value) => assertEquals(value, "1")
            case Left(_) => fail("Expected Right")
        }
    }
    
}
