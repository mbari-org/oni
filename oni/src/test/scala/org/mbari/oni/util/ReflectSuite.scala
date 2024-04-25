/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2024
 *
 * oni code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential. 
 */

package org.mbari.oni.util


case class Foo(a: String, b: Int, c: Option[String] = None)
case class Bar(a: String, b: Int, c: Option[Foo])
class Baz(val a: String, val b: Int)

class ReflectSuite extends munit.FunSuite:

  test("fromMap") {

    val m = Map("a" -> "hello", "b" -> 42, "c" -> "world")
    val foo = Reflect.fromMap[Foo](m)
    assertEquals(foo, Foo("hello", 42, Some("world")))

    val n = Map("a" -> "hello", "b" -> 43)
    val foo2 = Reflect.fromMap[Foo](n)
    assertEquals(foo2, Foo("hello", 43))

    val o = Map("a" -> "yo", "b" -> 44, "c" -> foo)
    val bar = Reflect.fromMap[Bar](o)
    assertEquals(bar, Bar("yo", 44, Some(foo)))

    val p = Map("a" -> "greetings", "b" -> 45)
    val baz = Reflect.fromMap[Baz](p)
    assertEquals(baz.a, "greetings")
    assertEquals(baz.b, 45)

  }

  test("fromMap should fail when required parameter is missing") {
    val m = Map("a" -> "hello", "c" -> "world")
    intercept[java.lang.IllegalArgumentException] {
      Reflect.fromMap[Foo](m)
    }
  }

  test("toMap") {
    val foo = Foo("hello", 42, Some("world"))
    val m = Reflect.toMap(foo)
    assertEquals(m, Map("a" -> "hello", "b" -> 42, "c" -> Some("world")))

    val bar = Bar("yo", 44, Some(foo))
    val n = Reflect.toMap(bar)
    assertEquals(n, Map("a" -> "yo", "b" -> 44, "c" -> Some(foo)))
  }
  