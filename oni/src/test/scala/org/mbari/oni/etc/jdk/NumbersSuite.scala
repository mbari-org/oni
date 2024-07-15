/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2024
 *
 * oni code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential.
 */

package org.mbari.oni.etc.jdk

class NumbersSuite extends munit.FunSuite {

    test("doubleConverter") {
        assertEquals(Numbers.doubleConverter(1.0), Some(1.0))
        assertEquals(Numbers.doubleConverter(1), Some(1.0))
        assertEquals(Numbers.doubleConverter("1.0"), Some(1.0))
        assertEquals(Numbers.doubleConverter(null), None)
        assertEquals(Numbers.doubleConverter(new Object()), None)

        val d: java.lang.Double = 1.0
        assertEquals(Numbers.doubleConverter(d), Some(1.0))
    }

    test("floatConverter") {
        assertEquals(Numbers.floatConverter(1.0f), Some(1.0f))
        assertEquals(Numbers.floatConverter(1), Some(1.0f))
        assertEquals(Numbers.floatConverter("1.0"), Some(1.0f))
        assertEquals(Numbers.floatConverter(null), None)
        assertEquals(Numbers.floatConverter(new Object()), None)

        val f: java.lang.Float = 1.0f
        assertEquals(Numbers.floatConverter(f), Some(1.0f))
    }

    test("longConverter") {
        assertEquals(Numbers.longConverter(1L), Some(1L))
        assertEquals(Numbers.longConverter(1), Some(1L))
        assertEquals(Numbers.longConverter("1"), Some(1L))
        assertEquals(Numbers.longConverter(null), None)
        assertEquals(Numbers.longConverter(new Object()), None)

        val l: java.lang.Long = 1L
        assertEquals(Numbers.longConverter(l), Some(1L))
    }

    test("intConverter") {
        assertEquals(Numbers.intConverter(1), Some(1))
        assertEquals(Numbers.intConverter(1L), Some(1))
        assertEquals(Numbers.intConverter("1"), Some(1))
        assertEquals(Numbers.intConverter(null), None)
        assertEquals(Numbers.intConverter(new Object()), None)

        val i: java.lang.Integer = 1
        assertEquals(Numbers.intConverter(i), Some(1))
    }
  
}
