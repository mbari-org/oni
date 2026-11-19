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

import com.typesafe.config.Config

import scala.concurrent.duration.{Duration, FiniteDuration}

/**
 * Settings for the in-memory knowledgebase cache.
 *
 * @param ttl
 *   How long cached data is kept before it's reloaded, even if no change was detected. Must be greater than zero.
 * @param stalenessCheckInterval
 *   How often to check the database for changes made outside Oni and clear the cache if any are found. Zero disables
 *   the check.
 */
case class CacheConfig(ttl: FiniteDuration, stalenessCheckInterval: FiniteDuration):
    require(ttl > Duration.Zero, s"cache.ttl must be greater than zero: $ttl")
    require(
        stalenessCheckInterval >= Duration.Zero,
        s"cache.staleness-check-interval must not be negative: $stalenessCheckInterval"
    )

    /** False when the interval is zero, meaning the staleness check is disabled */
    def stalenessCheckEnabled: Boolean = stalenessCheckInterval > Duration.Zero

object CacheConfig:

    /** Reads the `cache` block, e.g. from reference.conf. */
    def from(config: Config): CacheConfig =
        def duration(path: String): FiniteDuration = Duration.fromNanos(config.getDuration(path).toNanos)
        CacheConfig(ttl = duration("cache.ttl"), stalenessCheckInterval = duration("cache.staleness-check-interval"))
