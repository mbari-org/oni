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

import com.github.benmanes.caffeine.cache.{Cache, Caffeine}
import jakarta.persistence.EntityManagerFactory
import org.mbari.oni.config.{AppConfig, CacheConfig}
import org.mbari.oni.domain.{Concept, ConceptMetadata, SimpleConcept}
import org.mbari.oni.etc.jdk.Loggers.given
import org.mbari.oni.jdbc.{ConceptRow, MutableConcept, PhylogenyLoader}
import org.mbari.oni.jpa.ChangeBus
import org.mbari.oni.services.{ConceptNameService, ConceptService}

import java.time.{Duration as JDuration, Instant}
import java.util.concurrent.atomic.{AtomicLong, AtomicReference}
import java.util.concurrent.locks.ReentrantLock
import java.util.concurrent.{Executors, ScheduledExecutorService, TimeUnit}
import scala.jdk.CollectionConverters.*
import scala.util.control.NonFatal

/**
 * The single in-memory cache of knowledgebase data: concept metadata looked up by name, the list of all names, and the
 * concept tree used by the phylogeny lookups. One instance is created in `org.mbari.oni.Endpoints` and shared.
 *
 * ==Invalidation==
 *   - '''Committed changes''': subscribes to the [[ChangeBus]], which receives an event for every committed Hibernate
 *     change, the native-SQL bulk updates (which publish explicitly) and, through NATS, other Oni instances' changes.
 *     Any event that affects concept data clears everything.
 *   - '''Changes made outside Oni''': a background staleness check compares a fingerprint of the cached tables every
 *     `stalenessCheckInterval` and clears everything if it changed. See [[PhylogenyLoader.fingerprint]].
 *   - '''TTL''': every entry also expires after `ttl`, which bounds staleness from anything the above can't see.
 *
 * ==Consistency==
 * Every entry is stamped with the generation it was loaded in, and `clear()` starts a new generation. An entry from an
 * older generation is never returned. This closes the race where a read that started before a commit finishes after
 * the commit cleared the cache and would otherwise store its stale result.
 *
 * Use the companion `apply` to create one. The services and loaders are constructor parameters only so tests can
 * instrument them; the defaults are the real implementations.
 */
class KnowledgebaseCache(
    entityManagerFactory: EntityManagerFactory,
    val config: CacheConfig,
    conceptService: ConceptService,
    conceptNameService: ConceptNameService,
    loadRows: EntityManagerFactory => Either[Throwable, Seq[ConceptRow]] = PhylogenyLoader.loadRows,
    fingerprint: EntityManagerFactory => Either[Throwable, Seq[String]] = PhylogenyLoader.fingerprint
) extends AutoCloseable:

    private val log = System.getLogger(getClass.getName)

    /** Incremented by every clear. Entries from an older generation are treated as missing. */
    private val generation = new AtomicLong(0)

    private case class Stamped[T](generation: Long, value: T)

    // --- Concept metadata (by requested name) and the list of all names
    private val nameCache: Cache[String, Stamped[ConceptMetadata]] = Caffeine
        .newBuilder()
        .expireAfterWrite(JDuration.ofNanos(config.ttl.toNanos))
        .build[String, Stamped[ConceptMetadata]]()

    private val allNamesCache: Cache[String, Stamped[Seq[String]]] = Caffeine
        .newBuilder()
        .expireAfterWrite(JDuration.ofNanos(config.ttl.toNanos))
        .build[String, Stamped[Seq[String]]]()

    // --- Concept tree
    private val snapshotRef = new AtomicReference[Option[PhylogenySnapshot]](None)
    private val loadLock    = new ReentrantLock()

    // --- Invalidation sources. Everything they touch must be initialized above this point, because the staleness
    // check starts running on another thread as soon as it's scheduled.
    @volatile private var lastFingerprint: Option[Seq[String]] = None

    private val unsubscribe: Runnable =
        ChangeBus.subscribe(event => if event.affectsConceptData() then clear())

    private val stalenessChecker: Option[ScheduledExecutorService] =
        if config.stalenessCheckEnabled then
            val executor = Executors.newSingleThreadScheduledExecutor(r =>
                val t = new Thread(r, "KnowledgebaseCache-staleness-check")
                t.setDaemon(true)
                t
            )
            val millis   = config.stalenessCheckInterval.toMillis
            // Run immediately to record a baseline, then every interval
            executor.scheduleWithFixedDelay(() => checkForExternalChanges(), 0, millis, TimeUnit.MILLISECONDS)
            Some(executor)
        else None

    log.atInfo
        .log(
            s"KnowledgebaseCache created (ttl=${config.ttl}, staleness check=" +
                s"${if config.stalenessCheckEnabled then config.stalenessCheckInterval else "disabled"})"
        )

    // ---------------------------------------------------------------- Concept metadata

    def findByName(name: String): Either[Throwable, ConceptMetadata] =
        current(nameCache.getIfPresent(name)) match
            case Some(value) => Right(value)
            case None        =>
                val gen = generation.get()
                conceptService.findByName(name) match
                    case Left(e)      =>
                        log.atInfo.withCause(e).log(s"Failed to find concept by name: $name")
                        Left(e)
                    case Right(value) =>
                        nameCache.put(name, Stamped(gen, value))
                        Right(value)

    def findAllNames(limit: Int, offset: Int): Either[Throwable, Seq[String]] =
        current(allNamesCache.getIfPresent(KnowledgebaseCache.AllNamesKey)) match
            case Some(names) => Right(names.slice(offset, offset + limit))
            case None        =>
                val gen = generation.get()
                conceptNameService.findAllNames(1000000, 0) match
                    case Left(e)      =>
                        log.atError.withCause(e).log("Failed to find all concept names")
                        Left(e)
                    case Right(names) =>
                        allNamesCache.put(KnowledgebaseCache.AllNamesKey, Stamped(gen, names))
                        Right(names.slice(offset, offset + limit))

    // ---------------------------------------------------------------- Phylogeny

    def findUp(name: String): Option[Concept] = snapshot().flatMap(_.byName(name)).map(up)

    def findUpById(id: Long): Option[Concept] = snapshot().flatMap(_.byId(id)).map(up)

    def findDown(name: String): Option[Concept] = snapshot().flatMap(_.byName(name)).map(down)

    def findDownById(id: Long): Option[Concept] = snapshot().flatMap(_.byId(id)).map(down)

    def findSiblings(name: String): Seq[SimpleConcept] =
        snapshot().flatMap(_.byName(name)).map(siblings).getOrElse(Nil)

    def findSiblingsById(id: Long): Seq[SimpleConcept] =
        snapshot().flatMap(_.byId(id)).map(siblings).getOrElse(Nil)

    def findDescendantNames(name: String): Seq[String] =
        findDown(name).map(_.descendantNames).getOrElse(Nil)

    def findDescendantNamesAsJava(name: String): java.util.List[String] = findDescendantNames(name).asJava

    // Walk the branch up to the root. copyUp() copies the nodes, so the shared snapshot is never modified
    private def up(node: MutableConcept): Concept = node.copyUp().root().toImmutable

    // The parent isn't seen, so we can walk down from this node
    private def down(node: MutableConcept): Concept = node.toImmutable

    private def siblings(node: MutableConcept): Seq[SimpleConcept] =
        node.parent.map(_.children.map(SimpleConcept.from)).getOrElse(Nil)

    // ---------------------------------------------------------------- Lifecycle

    /** Drops everything. The next read of each kind of data reloads it from the database. */
    def clear(): Unit =
        generation.incrementAndGet()
        nameCache.invalidateAll()
        allNamesCache.invalidateAll()
        snapshotRef.set(None)

    /** Stops listening for changes and stops the staleness check. */
    override def close(): Unit =
        unsubscribe.run()
        stalenessChecker.foreach(_.shutdownNow())

    // ---------------------------------------------------------------- Internals

    /** Returns the value only if it was loaded in the current generation. */
    private def current[T](stamped: Stamped[T] | Null): Option[T] =
        Option(stamped).filter(_.generation == generation.get()).map(_.value)

    private def isUsable(s: PhylogenySnapshot): Boolean =
        s.generation == generation.get() && Instant.now().isBefore(s.loadedAt.plusNanos(config.ttl.toNanos))

    /**
     * The current tree, loading it if it's missing, from an older generation or past its TTL. Only one thread loads at
     * a time; others wait for it and then use its result. Returns None (and caches nothing) if loading fails.
     */
    private def snapshot(): Option[PhylogenySnapshot] =
        snapshotRef.get() match
            case Some(s) if isUsable(s) => Some(s)
            case _                      =>
                loadLock.lock()
                try
                    snapshotRef.get() match
                        case Some(s) if isUsable(s) => Some(s) // Another thread loaded it while we waited
                        case _                      =>
                            val gen = generation.get()
                            loadRows(entityManagerFactory) match
                                case Left(e)     =>
                                    log.atError.withCause(e).log("Failed to load the concept tree")
                                    None
                                case Right(rows) =>
                                    val s = PhylogenySnapshot.from(rows, gen)
                                    // If a clear happened during the load, this snapshot is stamped with the old
                                    // generation, so isUsable rejects it and the next read reloads
                                    snapshotRef.set(Some(s))
                                    Some(s)
                finally loadLock.unlock()

    /** Clears the cache if the cached tables changed without Oni knowing (e.g. manual SQL). Runs on its own thread. */
    private[cache] def checkForExternalChanges(): Unit =
        // Never let an exception escape: it would silently cancel all future runs of the scheduled check
        try checkFingerprint()
        catch case NonFatal(e) => log.atWarn.withCause(e).log("Staleness check failed unexpectedly")

    private def checkFingerprint(): Unit =
        fingerprint(entityManagerFactory) match
            case Left(e)            =>
                // Keep the previous fingerprint and don't clear; clearing on every failure would reload constantly
                log.atWarn.withCause(e).log("Staleness check failed; will try again at the next interval")
            case Right(fingerprint) =>
                if lastFingerprint.exists(_ != fingerprint) then
                    log.atInfo.log("Detected database changes made outside Oni; clearing the knowledgebase cache")
                    clear() // Local only: every instance runs its own check, so this isn't sent over NATS
                lastFingerprint = Some(fingerprint)

object KnowledgebaseCache:

    private val AllNamesKey = "all-names"

    def apply(
        entityManagerFactory: EntityManagerFactory,
        config: CacheConfig = AppConfig.DefaultCacheConfig
    ): KnowledgebaseCache =
        new KnowledgebaseCache(
            entityManagerFactory,
            config,
            ConceptService(entityManagerFactory),
            ConceptNameService(entityManagerFactory)
        )
