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

import java.net.URI

class JdbcTypesSuite extends munit.FunSuite {

    test("instantConverter") {
        val instant = java.time.Instant.now()
        assertEquals(JdbcTypes.instantConverter(instant), Some(instant))
        assertEquals(JdbcTypes.instantConverter(null), None)
        // assertEquals(JdbcTypes.instantConverter(new java.sql.Timestamp(instant.toEpochMilli)), Some(instant))
        // assertEquals(JdbcTypes.instantConverter(new microsoft.sql.DateTimeOffset(instant)), Some(instant))
        assertEquals(JdbcTypes.instantConverter(new Object()), None)
    }

    test("uuidConverter") {
        val uuid = java.util.UUID.randomUUID()
        assertEquals(JdbcTypes.uuidConverter(uuid), Some(uuid))
        assertEquals(JdbcTypes.uuidConverter(null), None)
        assertEquals(JdbcTypes.uuidConverter("123e4567-e89b-12d3-a456-426614174000"), Some(java.util.UUID.fromString("123e4567-e89b-12d3-a456-426614174000")))
        assertEquals(JdbcTypes.uuidConverter(new Object()), None)
    }

    test("urlConverter") {
        val url = URI.create("http://www.mbari.org").toURL()
        assertEquals(JdbcTypes.urlConverter(url), Some(url))
        assertEquals(JdbcTypes.urlConverter(null), None)
        assertEquals(JdbcTypes.urlConverter(url.toExternalForm()), Some(url))
        assertEquals(JdbcTypes.urlConverter(new Object()), None)
    }

    test("stringConverter") {
        val str = "Hello World"
        assertEquals(JdbcTypes.stringConverter(str), Some(str))
        assertEquals(JdbcTypes.stringConverter(null), None)
    }

  
}
