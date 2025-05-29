/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2024
 *
 * oni code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential.
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
