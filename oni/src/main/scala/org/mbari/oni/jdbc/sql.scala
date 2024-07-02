/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2024
 *
 * oni code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential.
 */

package org.mbari.oni.jdbc

import java.time.Instant
import java.util.UUID
import scala.util.Try
import java.net.URL
import java.net.URI

/**
 * This is a collection of explicit conversions to convert from java.sql.ResultSet to various types. This is used in the
 * JdbcRepository and associatited SQL classes.
 */
extension (obj: Object)
    def asDouble: Option[Double]   = doubleConverter(obj)
    def asFloat: Option[Float]     = floatConverter(obj)
    def asInstant: Option[Instant] = instantConverter(obj)
    def asInt: Option[Int]         = intConverter(obj)
    def asLong: Option[Long]       = longConverter(obj)
    def asString: Option[String]   = stringConverter(obj)
    def asUrl: Option[URL]         = urlConverter(obj)
    def asUUID: Option[UUID]       = uuidConverter(obj)

def instantConverter(obj: Object): Option[Instant] =
    obj match
        case null                            => None
        case i: Instant                      => Some(i)
        case ts: java.sql.Timestamp          => Some(ts.toInstant)
        case m: microsoft.sql.DateTimeOffset => Some(m.getOffsetDateTime().toInstant())
        case _                               => None // TODO handle postgres

def uuidConverter(obj: Object): Option[UUID] =
    obj match
        case null      => None
        case u: UUID   => Some(u)
        case s: String => Try(UUID.fromString(s)).toOption // TODO this could swallow errors

def stringConverter(obj: Object): Option[String] =
    obj match
        case null      => None
        case s: String => Some(s)
        case _         => Some(obj.toString)

def doubleConverter(obj: Object): Option[Double] =
    obj match
        case null      => None
        case n: Number => Some(n.doubleValue())
        case s: String => Try(s.toDouble).toOption
        case _         => None

def floatConverter(obj: Object): Option[Float] =
    obj match
        case null      => None
        case n: Number => Some(n.floatValue())
        case s: String => Try(s.toFloat).toOption
        case _         => None

def longConverter(obj: Object): Option[Long] =
    obj match
        case null      => None
        case n: Number => Some(n.longValue())
        case s: String => Try(s.toLong).toOption
        case _         => None

def intConverter(obj: Object): Option[Int] =
    obj match
        case null      => None
        case n: Number => Some(n.intValue())
        case s: String => Try(s.toInt).toOption
        case _         => None

def urlConverter(obj: Object): Option[URL] =
    obj match
        case null      => None
        case u: URL    => Some(u)
        case uri: URI  => Try(uri.toURL()).toOption
        case s: String => Try(URI.create(s).toURL()).toOption
        case _         => None
