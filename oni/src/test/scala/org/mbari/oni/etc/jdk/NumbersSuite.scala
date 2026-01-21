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
