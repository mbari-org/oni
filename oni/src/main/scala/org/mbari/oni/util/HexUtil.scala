/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2024
 *
 * oni code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential. 
 */

package org.mbari.oni.util

object HexUtil:

    def toHex(bytes: Array[Byte]): String =
        val sb = new StringBuilder
        for b <- bytes do sb.append(String.format("%02x", Byte.box(b)))
        sb.toString

    def fromHex(hex: String): Array[Byte] =
        hex
            .grouped(2)
            .toArray
            .map(i => Integer.parseInt(i, 16).toByte)
