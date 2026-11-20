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
import org.mbari.oni.domain.{ConceptMetadata, ConceptNameTypes, ConceptUpdate, MediaCreate, Reference}
import org.mbari.oni.etc.jdk.Strings
import org.mbari.oni.jdbc.{ConceptRow, PhylogenyLoader}
import org.mbari.oni.jpa.DataInitializer
import org.mbari.oni.jpa.EntityManagerFactories.*
import org.mbari.oni.jpa.entities.{ConceptEntity, ConceptNameEntity, TestEntityFactory}
import org.mbari.oni.jpa.repositories.ConceptRepository
import org.mbari.oni.services.{ConceptNameService, ConceptService, MediaService, ReferenceService, UserAuthMixin}

import java.net.URI
import java.sql.Timestamp
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.{CountDownLatch, Executors, TimeUnit}
import scala.concurrent.duration.*
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.jdk.CollectionConverters.*

trait KnowledgebaseCacheSuite extends DataInitializer with UserAuthMixin with KnowledgebaseCacheFixture:

    override def munitTimeout: Duration = 120.seconds

    private lazy val conceptNameService = ConceptNameService(entityManagerFactory)
    private val noCheck                 = KnowledgebaseCacheFixture.TestConfig

    // ---------------------------------------------------------------- Helpers

    /** Creates a cache, runs fn with it and always closes it */
    private def withCache[T](
        config: CacheConfig = noCheck,
        service: ConceptService = conceptService,
        loadRows: EntityManagerFactory => Either[Throwable, Seq[ConceptRow]] = PhylogenyLoader.loadRows,
        fingerprint: EntityManagerFactory => Either[Throwable, Seq[String]] = PhylogenyLoader.fingerprint
    )(fn: KnowledgebaseCache => T): T =
        val cache =
            new KnowledgebaseCache(entityManagerFactory, config, service, conceptNameService, loadRows, fingerprint)
        try fn(cache)
        finally cache.close()

    /** A ConceptService that counts database lookups by name */
    private def countingService(lookups: AtomicInteger): ConceptService =
        new ConceptService(entityManagerFactory):
            override def findByName(name: String): Either[Throwable, ConceptMetadata] =
                lookups.incrementAndGet()
                super.findByName(name)

    /** A tree loader that counts loads */
    private def countingLoader(loads: AtomicInteger): EntityManagerFactory => Either[Throwable, Seq[ConceptRow]] =
        emf =>
            loads.incrementAndGet()
            PhylogenyLoader.loadRows(emf)

    /** Runs native SQL directly, like a tool outside Oni would. It publishes no ChangeEvent. */
    private def externalSql(sql: String, params: Any*): Int =
        entityManagerFactory.transaction(em =>
            val query = em.createNativeQuery(sql)
            params.zipWithIndex.foreach((p, i) => query.setParameter(i + 1, p))
            query.executeUpdate()
        ) match
            case Left(e)  => fail(s"External SQL failed: $e")
            case Right(n) => n

    private def nonPrimaryName(concept: ConceptEntity): String =
        concept.getConceptNames.asScala.find(_.getNameType != ConceptNameTypes.PRIMARY.getType).get.getName

    private def awaitTrue(timeout: FiniteDuration)(condition: => Boolean): Boolean =
        val deadline = System.nanoTime() + timeout.toNanos
        while !condition && System.nanoTime() < deadline do Thread.sleep(25)
        condition

    // ---------------------------------------------------------------- Commit-driven invalidation

    test("concept metadata is served from the cache until a commit clears it") {
        val root    = init(3, 0)
        val name    = root.getPrimaryConceptName.getName
        val lookups = new AtomicInteger(0)
        withCache(service = countingService(lookups)) { cache =>
            cache.findByName(name)
            cache.findByName(name)
            assertEquals(lookups.get, 1, "The second lookup should come from the cache")

            val grandChild = root.getChildConcepts.iterator().next().getChildConcepts.iterator().next()
            assert(conceptService.deleteByName(grandChild.getName).isRight)

            cache.findByName(name)
            assertEquals(lookups.get, 2, "The committed delete should have cleared the cache")
        }
    }

    test("a rolled back transaction does not clear the cache") {
        val root    = init(2, 0)
        val name    = root.getPrimaryConceptName.getName
        val lookups = new AtomicInteger(0)
        withCache(service = countingService(lookups)) { cache =>
            cache.findByName(name)
            val result = entityManagerFactory.transaction(em =>
                val entity = new ConceptNameEntity()
                entity.setName(Strings.random(12))
                entity.setNameType(ConceptNameTypes.SYNONYM.getType)
                new ConceptRepository(em).findRoot().get().addConceptName(entity)
                em.flush()
                throw new RuntimeException("force rollback")
            )
            assert(result.isLeft)
            cache.findByName(name)
            assertEquals(lookups.get, 1, "A rollback must not clear the cache")
        }
    }

    test("the phylogeny tree reflects a committed delete") {
        val root      = init(3, 0)
        val childName = root.getChildConcepts.iterator().next().getName
        val tree      = knowledgebaseCache.findDown(root.getName).get
        assert(tree.descendantNames.contains(childName))
        assert(conceptService.deleteByName(childName).isRight)
        assert(!knowledgebaseCache.findDown(root.getName).get.descendantNames.contains(childName))
        assertEquals(knowledgebaseCache.findUp(childName), None)
    }

    test("adding and removing a reference clears the cache (many-to-many join table)") {
        val root             = init(2, 0)
        val name             = root.getName
        val referenceService = ReferenceService(entityManagerFactory)
        val reference        = referenceService.create(Reference.from(TestEntityFactory.createReference())).toOption.get
        val citation         = reference.citation
        val id               = reference.id.get

        assert(!knowledgebaseCache.findByName(name).toOption.get.references.exists(_.citation == citation))

        assert(referenceService.addConcept(id, name).isRight)
        assert(
            knowledgebaseCache.findByName(name).toOption.get.references.exists(_.citation == citation),
            "Adding a reference did not clear the cache"
        )

        assert(referenceService.removeConcept(id, name).isRight)
        assert(
            !knowledgebaseCache.findByName(name).toOption.get.references.exists(_.citation == citation),
            "Removing a reference did not clear the cache"
        )
        referenceService.deleteById(id)
    }

    test("creating media clears the cache") {
        val root      = init(2, 0)
        val name      = root.getName
        val url       = URI.create(s"https://www.mbari.org/test/${Strings.random(12)}.png").toURL
        val before    = knowledgebaseCache.findByName(name).toOption.get
        val attempt   = runWithUserAuth(user =>
            MediaService(entityManagerFactory, fastPhylogenyService).create(MediaCreate(name, url), user.username)
        )
        assert(attempt.isRight, attempt.toString)
        assert(!before.media.exists(_.url == url))
        assert(knowledgebaseCache.findByName(name).toOption.get.media.exists(_.url == url), "Media not visible")
    }

    test("updating a concept clears the cache") {
        val root    = init(2, 0)
        val child   = root.getChildConcepts.iterator().next()
        val aphiaId = 800000000L + scala.util.Random.nextInt(1000000)
        knowledgebaseCache.findByName(child.getName)
        val attempt = runWithUserAuth(user =>
            conceptService.updateById(child.getId, ConceptUpdate(aphiaId = Some(aphiaId)), user.username)
        )
        assert(attempt.isRight, attempt.toString)
        assertEquals(knowledgebaseCache.findByName(child.getName).toOption.get.aphiaId, Some(aphiaId))
    }

    // ---------------------------------------------------------------- TTL

    test("concept metadata expires after the TTL") {
        val root    = init(2, 0)
        val lookups = new AtomicInteger(0)
        withCache(config = CacheConfig(300.millis, Duration.Zero), service = countingService(lookups)) { cache =>
            cache.findByName(root.getName)
            cache.findByName(root.getName)
            assertEquals(lookups.get, 1)
            Thread.sleep(600)
            cache.findByName(root.getName)
            assertEquals(lookups.get, 2, "The entry should have expired")
        }
    }

    test("the phylogeny tree expires after the TTL") {
        val root  = init(2, 0)
        val loads = new AtomicInteger(0)
        withCache(config = CacheConfig(300.millis, Duration.Zero), loadRows = countingLoader(loads)) { cache =>
            cache.findDown(root.getName)
            cache.findDown(root.getName)
            assertEquals(loads.get, 1)
            Thread.sleep(600)
            cache.findDown(root.getName)
            assertEquals(loads.get, 2, "The tree should have expired")
        }
    }

    // ---------------------------------------------------------------- Concurrency

    test("concurrent reads load the tree only once") {
        val root   = init(3, 2)
        val loads  = new AtomicInteger(0)
        val slow   = (emf: EntityManagerFactory) =>
            loads.incrementAndGet()
            Thread.sleep(300) // make sure the other readers arrive while this load is running
            PhylogenyLoader.loadRows(emf)
        val pool   = Executors.newFixedThreadPool(8)
        given ExecutionContext = ExecutionContext.fromExecutorService(pool)
        try
            withCache(loadRows = slow) { cache =>
                val start   = new CountDownLatch(1)
                val results = (1 to 8).map(_ => Future { start.await(); cache.findDown(root.getName) })
                start.countDown()
                val trees   = Await.result(Future.sequence(results), 30.seconds)
                assert(trees.forall(_.isDefined))
                assertEquals(loads.get, 1, "Only one thread should have loaded the tree")
            }
        finally pool.shutdownNow()
    }

    test("a tree load that overlaps a clear is not reused") {
        val root    = init(2, 0)
        val loads   = new AtomicInteger(0)
        val entered = new CountDownLatch(1)
        val release = new CountDownLatch(1)
        val blocking = (emf: EntityManagerFactory) =>
            if loads.incrementAndGet() == 1 then
                entered.countDown()
                release.await(10, TimeUnit.SECONDS) // the first load is paused until the test releases it
            PhylogenyLoader.loadRows(emf)
        withCache(loadRows = blocking) { cache =>
            val reader = new Thread(() => { cache.findDown(root.getName); () })
            reader.start()
            assert(entered.await(10, TimeUnit.SECONDS))
            cache.clear() // a change commits while the first load is still reading
            release.countDown()
            reader.join(10000)

            cache.findDown(root.getName)
            assertEquals(loads.get, 2, "The load that overlapped the clear must not be reused")
        }
    }

    test("a metadata lookup that overlaps a clear is not reused") {
        val root    = init(2, 0)
        val lookups = new AtomicInteger(0)
        val entered = new CountDownLatch(1)
        val release = new CountDownLatch(1)
        val blocking = new ConceptService(entityManagerFactory):
            override def findByName(name: String): Either[Throwable, ConceptMetadata] =
                if lookups.incrementAndGet() == 1 then
                    entered.countDown()
                    release.await(10, TimeUnit.SECONDS)
                super.findByName(name)
        withCache(service = blocking) { cache =>
            val reader = new Thread(() => { cache.findByName(root.getName); () })
            reader.start()
            assert(entered.await(10, TimeUnit.SECONDS))
            cache.clear()
            release.countDown()
            reader.join(10000)

            cache.findByName(root.getName)
            assertEquals(lookups.get, 2, "The lookup that overlapped the clear must not be reused")
        }
    }

    // ---------------------------------------------------------------- Staleness check (changes made outside Oni)

    test("the staleness check detects an external delete") {
        val root    = init(2, 0)
        val synonym = nonPrimaryName(root)
        withCache() { cache =>
            cache.checkForExternalChanges() // baseline
            assert(cache.findUp(synonym).isDefined)

            externalSql("DELETE FROM ConceptName WHERE CONCEPTNAME = ?", synonym)
            assert(cache.findUp(synonym).isDefined, "External SQL publishes no event, so the cache should still be stale")

            cache.checkForExternalChanges()
            assertEquals(cache.findUp(synonym), None, "The staleness check should have cleared the cache")
        }
    }

    test("the staleness check detects an external update that sets LAST_UPDATED_TIME") {
        val root    = init(2, 0)
        val synonym = nonPrimaryName(root)
        val renamed = Strings.random(16)
        withCache() { cache =>
            cache.checkForExternalChanges() // baseline
            assert(cache.findUp(synonym).isDefined)

            // A future timestamp guarantees the max increases even if the database clock differs from ours
            val later = Timestamp.from(Instant.now().plus(1, ChronoUnit.DAYS))
            externalSql(
                "UPDATE ConceptName SET CONCEPTNAME = ?, LAST_UPDATED_TIME = ? WHERE CONCEPTNAME = ?",
                renamed,
                later,
                synonym
            )
            assert(cache.findUp(renamed).isEmpty, "The cache should still be stale before the check")

            cache.checkForExternalChanges()
            assert(cache.findUp(renamed).isDefined, "The renamed concept name should be visible after the check")
        }
    }

    test("a failed staleness check does not clear the cache") {
        val root      = init(2, 0)
        val loads     = new AtomicInteger(0)
        val responses = Iterator(Right(Seq("a")), Left(new RuntimeException("database down")), Right(Seq("a")))
        withCache(loadRows = countingLoader(loads), fingerprint = _ => responses.next()) { cache =>
            cache.checkForExternalChanges() // baseline
            cache.findDown(root.getName)
            cache.checkForExternalChanges() // fails
            cache.findDown(root.getName)
            cache.checkForExternalChanges() // succeeds, unchanged
            cache.findDown(root.getName)
            assertEquals(loads.get, 1, "Neither the failed nor the unchanged check should have cleared the cache")
        }
    }

    test("the background check runs on its interval and stops on close") {
        val runs  = new AtomicInteger(0)
        val cache = new KnowledgebaseCache(
            entityManagerFactory,
            CacheConfig(15.minutes, 50.millis),
            conceptService,
            conceptNameService,
            fingerprint = _ =>
                runs.incrementAndGet()
                Right(Seq("unchanged"))
        )
        try assert(awaitTrue(5.seconds)(runs.get >= 3), s"Expected at least 3 runs, got ${runs.get}")
        finally cache.close()
        Thread.sleep(100) // let an in-flight run finish
        val afterClose = runs.get
        Thread.sleep(300)
        assertEquals(runs.get, afterClose, "The check kept running after close()")
    }

    test("an interval of 0 disables the background check") {
        val runs = new AtomicInteger(0)
        withCache(fingerprint = _ =>
            runs.incrementAndGet()
            Right(Nil)
        ) { _ =>
            Thread.sleep(300)
            assertEquals(runs.get, 0)
        }
    }

    test("the background check clears the cache after an external change (end to end)") {
        val root    = init(2, 0)
        val synonym = nonPrimaryName(root)
        withCache(config = CacheConfig(15.minutes, 100.millis)) { cache =>
            Thread.sleep(400) // let the first run record a baseline
            assert(cache.findUp(synonym).isDefined)
            externalSql("DELETE FROM ConceptName WHERE CONCEPTNAME = ?", synonym)
            assert(
                awaitTrue(10.seconds)(cache.findUp(synonym).isEmpty),
                "The background check did not pick up the external delete"
            )
        }
    }
