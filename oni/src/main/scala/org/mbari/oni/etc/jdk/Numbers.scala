/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2024
 *
 * oni code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential. 
 */

package org.mbari.oni.etc.jdk

import scala.util.Try

object Numbers {

    extension (obj: Object | Number)
        def asDouble: Option[Double] = Numbers.doubleConverter(obj)
        def asFloat: Option[Float]   = Numbers.floatConverter(obj)
        def asLong: Option[Long]     = Numbers.longConverter(obj)
        def asInt: Option[Int]       = Numbers.intConverter(obj)

    def doubleConverter(obj: Object | Number | Double): Option[Double] =
        obj match
            case null      => None
            case d: Double => Some(d)
            case n: Number => Some(n.doubleValue())
            case s: String => Try(s.toDouble).toOption
            case _         => None

    def floatConverter(obj: Object | Number | Float): Option[Float] =
        obj match
            case null      => None
            case f: Float  => Some(f)
            case n: Number => Some(n.floatValue())
            case s: String => Try(s.toFloat).toOption
            case _         => None

    def longConverter(obj: Object | Number | Long): Option[Long] =
        obj match
            case null      => None
            case l: Long   => Some(l)
            case n: Number => Some(n.longValue())
            case s: String => Try(s.toLong).toOption
            case _         => None

    def intConverter(obj: Object | Number | Int): Option[Int] =
        obj match
            case null      => None
            case i: Int    => Some(i)
            case n: Number => Some(n.intValue())
            case s: String => Try(s.toInt).toOption
            case _         => None

}
