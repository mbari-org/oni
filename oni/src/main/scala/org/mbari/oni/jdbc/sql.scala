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

package org.mbari.oni.jdbc

import org.mbari.oni.etc.jdk.{JdbcTypes, Numbers}

import java.net.URL
import java.time.Instant
import java.util.UUID

/**
 * This is a collection of explicit conversions to convert from java.sql.ResultSet to various types. This is used in the
 * JdbcRepository and associatited SQL classes.
 */
extension (obj: Object)
    def asDouble: Option[Double]   = Numbers.doubleConverter(obj)
    def asFloat: Option[Float]     = Numbers.floatConverter(obj)
    def asInstant: Option[Instant] = JdbcTypes.instantConverter(obj)
    def asInt: Option[Int]         = Numbers.intConverter(obj)
    def asLong: Option[Long]       = Numbers.longConverter(obj)
    def asString: Option[String]   = JdbcTypes.stringConverter(obj)
    def asUrl: Option[URL]         = JdbcTypes.urlConverter(obj)
    def asUUID: Option[UUID]       = JdbcTypes.uuidConverter(obj)
