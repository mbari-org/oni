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

package org.mbari.oni.cache

import jakarta.persistence.EntityManagerFactory
import org.mbari.oni.config.CacheConfig
import org.mbari.oni.jdbc.FastPhylogenyService

import scala.concurrent.duration.*

/**
 * Gives a suite one shared KnowledgebaseCache (like the application has), and closes it after the suite so its
 * ChangeBus subscription and background thread don't outlive the suite.
 */
trait KnowledgebaseCacheFixture extends munit.FunSuite:

    def entityManagerFactory: EntityManagerFactory

    @volatile private var created: Option[KnowledgebaseCache] = None

    /** The suite's cache. The staleness check is off so tests only see the invalidation they trigger. */
    lazy val knowledgebaseCache: KnowledgebaseCache =
        val cache = KnowledgebaseCache(entityManagerFactory, KnowledgebaseCacheFixture.TestConfig)
        created = Some(cache)
        cache

    lazy val fastPhylogenyService: FastPhylogenyService = FastPhylogenyService(knowledgebaseCache)

    override def afterAll(): Unit =
        created.foreach(_.close())
        super.afterAll()

object KnowledgebaseCacheFixture:
    /** Normal TTL, staleness check disabled */
    val TestConfig: CacheConfig = CacheConfig(ttl = 15.minutes, stalenessCheckInterval = Duration.Zero)
