/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2024
 *
 * oni code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential.
 */

package org.mbari.oni.etc.jdk

import java.awt.Color

class Colors:

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
