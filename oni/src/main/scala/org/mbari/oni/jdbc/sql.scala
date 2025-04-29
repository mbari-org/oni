/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2024
 *
 * oni code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential.
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
