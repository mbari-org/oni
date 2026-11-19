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

package org.mbari.oni.config

import com.typesafe.config.ConfigFactory

import scala.concurrent.duration.*

class CacheConfigSuite extends munit.FunSuite:

    private def parse(interval: String, ttl: String = "15 minutes"): CacheConfig =
        CacheConfig.from(
            ConfigFactory.parseString(s"cache.staleness-check-interval = $interval\ncache.ttl = $ttl")
        )

    test("reference.conf defaults are exposed via AppConfig") {
        assertEquals(AppConfig.DefaultCacheConfig.stalenessCheckInterval, 60.seconds)
        assert(AppConfig.DefaultCacheConfig.stalenessCheckEnabled)
        assertEquals(AppConfig.DefaultCacheConfig.ttl, 15.minutes)
    }

    test("ttl accepts HOCON duration formats") {
        assertEquals(parse("60s", ttl = "5 minutes").ttl, 5.minutes)
        assertEquals(parse("60s", ttl = "90s").ttl, 90.seconds)
    }

    test("ttl must be greater than zero") {
        intercept[IllegalArgumentException](parse("60s", ttl = "0"))
        intercept[IllegalArgumentException](parse("60s", ttl = "-1m"))
    }

    test("accepts HOCON duration formats") {
        assertEquals(parse("30s").stalenessCheckInterval, 30.seconds)
        assertEquals(parse("\"2 minutes\"").stalenessCheckInterval, 2.minutes)
        assertEquals(parse("500ms").stalenessCheckInterval, 500.millis)
        assertEquals(parse("60 s").stalenessCheckInterval, 60.seconds)       // unquoted, with a space
        assertEquals(parse("60 seconds").stalenessCheckInterval, 60.seconds) // same form as reference.conf
    }

    test("a bare number is read as milliseconds (HOCON default unit)") {
        assertEquals(parse("1500").stalenessCheckInterval, 1500.millis)
    }

    test("0 disables the check") {
        val config = parse("0")
        assertEquals(config.stalenessCheckInterval, Duration.Zero)
        assert(!config.stalenessCheckEnabled)
    }

    test("a negative interval is rejected") {
        intercept[IllegalArgumentException](parse("-5s"))
    }

    test("an unparseable interval is rejected") {
        intercept[com.typesafe.config.ConfigException](parse("sometimes"))
    }
