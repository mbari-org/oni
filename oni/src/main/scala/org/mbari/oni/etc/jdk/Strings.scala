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

import scala.util.Random

object Strings:

    private val chars           = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
    private val randomGenerator = new Random

    def random(length: Int): String =
        val xs = for (_ <- 0 until length) yield chars.charAt(randomGenerator.nextInt(chars.length))
        new String(xs.toArray)

    def random(origin: Int, bound: Int): String =
        val n = randomGenerator.between(origin, bound)
        random(n)

    /**
     * Change case of a string to init Cap. That is the first letter is capitalized and the rest are lower case.
     * @param s
     *   the string to convert
     * @return
     *   the init cap version of the string
     */
    def initCap(s: String): String =
        val a = s.toLowerCase()
        a.substring(0, 1).toUpperCase() + a.substring(1)
