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

import java.net.{URI, URL}
import java.time.{Instant, ZoneOffset}
import java.util.UUID
import scala.util.Try

object JdbcTypes:

    /**
     * This is a collection of explicit conversions to convert from java.sql.ResultSet to various types. This is used in
     * the JdbcRepository and associatited SQL classes.
     */
    extension (obj: Object)
        def asDouble: Option[Double]   = Numbers.doubleConverter(obj)
        def asFloat: Option[Float]     = Numbers.floatConverter(obj)
        def asInstant: Option[Instant] = instantConverter(obj)
        def asInt: Option[Int]         = Numbers.intConverter(obj)
        def asLong: Option[Long]       = Numbers.longConverter(obj)
        def asString: Option[String]   = stringConverter(obj)
        def asUrl: Option[URL]         = urlConverter(obj)
        def asUUID: Option[UUID]       = uuidConverter(obj)

    def instantConverter(obj: Object): Option[Instant] =
        obj match
            case null                            => None
            case i: java.time.Instant            => Some(i)
            case l: java.time.LocalDateTime      => Some(l.toInstant(ZoneOffset.UTC))
            case m: microsoft.sql.DateTimeOffset => Some(m.getOffsetDateTime().toInstant())
            case o: java.time.OffsetDateTime     => Some(o.toInstant)
            case ts: java.sql.Timestamp          => Some(ts.toInstant)
            case _                               => None // TODO handle postgres

    def uuidConverter(obj: Object): Option[UUID] =
        obj match
            case null      => None
            case u: UUID   => Some(u)
            case s: String => Try(UUID.fromString(s)).toOption // TODO this could swallow errors
            case _         => None

    def stringConverter(obj: Object): Option[String] =
        obj match
            case null      => None
            case s: String => Some(s)
            case _         => Some(obj.toString)

    def urlConverter(obj: Object): Option[URL] =
        obj match
            case null      => None
            case u: URL    => Some(u)
            case uri: URI  => Try(uri.toURL()).toOption
            case s: String => Try(URI.create(s).toURL()).toOption
            case _         => None
