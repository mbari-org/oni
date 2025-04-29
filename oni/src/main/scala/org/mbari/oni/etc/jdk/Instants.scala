/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2024
 *
 * oni code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential.
 */

package org.mbari.oni.etc.jdk

import java.time.format.DateTimeFormatter
import java.time.{Instant, ZoneId}
import scala.util.Try

object Instants:

    private val utcZone                           = ZoneId.of("UTC")
    val TimeFormatter: DateTimeFormatter          = DateTimeFormatter.ISO_DATE_TIME.withZone(utcZone)
    val CompactTimeFormatter: DateTimeFormatter   =
        DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmssX").withZone(utcZone)
    val CompactTimeFormatterMs: DateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss.SSSX").withZone(utcZone)
    val CompactTimeFormatterNs: DateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss.SSSSSSX").withZone(utcZone)

    /**
     * Parse a string into an Instant. The string can be in any of the following formats:
     *   - yyyyMMdd'T'HHmmssX
     *   - yyyyMMdd'T'HHmmss.SSSX
     *   - yyyyMMdd'T'HHmmss.SSSSSSX
     *   - yyyy-MM-dd'T'HH:mm:ssX
     *
     * @param s
     * @return
     */
    def parseIso8601(s: String): Either[Throwable, Instant] =
        val tried = Try(Instant.from(CompactTimeFormatter.parse(s))) orElse
            Try(Instant.from(TimeFormatter.parse(s))) orElse
            Try(Instant.from(CompactTimeFormatterMs.parse(s))) orElse
            Try(Instant.from(CompactTimeFormatterNs.parse(s)))
        tried.toEither

    def formatCompactIso8601(instant: Instant): String =
        CompactTimeFormatter.format(instant)

    def from(timestamp: java.sql.Timestamp): Instant =
        if timestamp == null then null
        else timestamp.toInstant()
