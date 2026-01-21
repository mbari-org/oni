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

import java.awt.Color

class Colors:

    /**
     * Converts any string to a hex color. The color is generated from the hash code of the string.
     * @param s
     *   The string to convert
     * @return
     *   The hex color
     */
    def stringToHexColor(s: String): String =
        val c = intToRGBA(s.hashCode)
        f"#${c.getRed}%02X${c.getGreen}%02X${c.getBlue}%02X" // ignore alpha. Sharktopoda won't parse colors with alpha

    def intToRGBA(i: Int): Color =
        val a = brighten((i >> 24) & 0xff, 128)
        val r = brighten((i >> 16) & 0xff, 32)
        val g = brighten((i >> 8) & 0xff, 32)
        val b = brighten(i & 0xff, 32)

        new Color(r, g, b, a)

    /**
     * Brightens a color (increases its value) so that it is between min and 256.
     * @param v
     *   The value to brighten
     * @param min
     *   the minimum allowed color
     * @return
     *   the brightened color
     */
    def brighten(v: Int, min: Int): Int =
        val inRangeMin =
            if min > 256 then 256
            else if min < 0 then 0
            else min
        math.round(((v + min) / (256d + min)) * 256).toInt
