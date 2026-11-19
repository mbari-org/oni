# Spec: Unified knowledgebase cache

Status: **Implemented.** See §10 for where the implementation differs from the plan.

## 1. Goal

1. Replace the two independent in-memory caches (`ConceptCache` and the tree inside `FastPhylogenyService`) with a
   single cache object.
2. Create that object once, in `org.mbari.oni.Endpoints`, and pass it to the constructors of the classes that need it.
3. Make sure no write leaves the cache holding stale data. **Decision:** invalidation is commit-driven only, using the
   existing `ChangeBus`. Endpoints don't clear the cache themselves (§5).

## 2. Current state

| | `ConceptCache` | `FastPhylogenyService` |
|---|---|---|
| File | `services/ConceptCache.scala` | `jdbc/FastPhylogenyService.scala` |
| Holds | `ConceptMetadata` by requested name; list of all names | Whole concept tree (ids, names, ranks, parent/child links) |
| Storage | Two Caffeine caches, 15 min `expireAfterWrite` | `var rootNode` / `var allNodes`, no expiry |
| Who creates it | `ConceptEndpoints` (a `private val`, so only that class uses it); tests create their own | `PhylogenyEndpoints`, then `Endpoints.scala` passes `phylogenyEndpoints.service` to `HistoryEndpoints` and `MediaEndpoints`; tests create many |
| Consumers | `ConceptEndpoints` (`GET /concept`, `GET /concept/{name}`) | `PhylogenyEndpoints`, `HistoryActionService`, `MediaService`, `MediaRepository` (Java, via `findDescendantNamesAsJava`) |
| Invalidation | Subscribes to `ChangeBus` and clears on any committed change that `affectsConceptData()`. `ConceptEndpoints` also calls `clear()` after its 5 write endpoints | Before **every** read, runs `SELECT MAX(LAST_UPDATED_TIME)` over `Concept` and `ConceptName`, and reloads the whole tree if that is newer than the last load |
| Sees other Oni instances' writes | Only via NATS (off by default, `messaging.nats.enable = false`); otherwise up to the 15 min TTL | Yes, because it polls the shared database |

### Existing invalidation infrastructure (keep it)

- `CommitNotifier` is a Hibernate post-commit listener. For every entity inserted, updated or deleted by a
  transaction **that committed**, it publishes a `ChangeEvent(action, dataType, id)` on `ChangeBus`. Nothing is
  published on rollback.
- Native-SQL bulk updates bypass Hibernate. The two that exist (`renameToConcept` for link templates and link
  realizations) publish their own `ChangeEvent`.
- `NatsSubscriber` republishes other instances' changes on the local `ChangeBus` as remote events. On every
  (re)connect it also publishes a catch-all event, because changes made while disconnected were missed.
- `ChangeEvent.affectsConceptData()` is true for everything except `USER_ACCOUNT` and `PREFERENCE_NODE`.

## 3. Problems found while researching

| # | Problem | Severity |
|---|---|---|
| P1 | **Deletes probably never refresh the phylogeny tree.** Staleness is detected only by `MAX(LAST_UPDATED_TIME)` increasing. Deleting a concept or concept name removes a row, so the maximum stays the same or goes down. The deleted concept would stay in `/phylogeny/*` results, `HistoryActionService` and media descendant searches until some unrelated update happens. *Not yet verified* (see step 1 in §7). | High |
| P2 | Every phylogeny read costs a database round trip for the `MAX(...)` query, even when nothing changed. If that query fails, `findLastUpdate` returns `Instant.now()`, which forces a **full tree reload on every call** until the database recovers. | Medium |
| P3 | Not thread-safe. `load()` checks `lastUpdate` outside the lock, so concurrent requests can all decide to reload and run the full query one after another. `rootNode` and `allNodes` are plain `var`s (not `@volatile`), so other threads aren't guaranteed to see a completed reload. | Medium |
| P4 | Two caches with two different invalidation strategies and two expiry policies (15 min vs never). | Low |
| P5 | `ConceptCache` is private to `ConceptEndpoints`, so nothing else benefits from it. Its explicit `clear()` calls run even when the write failed. | Low |
| P6 | Lookups scan the whole tree (`allNodes.find(...)`), which is O(n) per request. | Low |

## 4. Proposed design

### 4.1 One cache class

A new class, working name **`KnowledgebaseCache`**, in a new package `org.mbari.oni.cache`. It replaces
`ConceptCache` and takes over the tree from `FastPhylogenyService`.

```scala
class KnowledgebaseCache(
    entityManagerFactory: EntityManagerFactory,
    ttl: Duration = 15.minutes,                     // safety net, see §5
    stalenessCheckInterval: Duration = 60.seconds   // 0 disables the check, see §5.2
) extends AutoCloseable:

    // --- ConceptMetadata (was ConceptCache)
    def findByName(name: String): Either[Throwable, ConceptMetadata]
    def findAllNames(limit: Int, offset: Int): Either[Throwable, Seq[String]]

    // --- Phylogeny (was FastPhylogenyService)
    def findUp(name: String): Option[Concept]
    def findUpById(id: Long): Option[Concept]
    def findDown(name: String): Option[Concept]
    def findDownById(id: Long): Option[Concept]
    def findSiblings(name: String): Seq[SimpleConcept]
    def findSiblingsById(id: Long): Seq[SimpleConcept]
    def findDescendantNames(name: String): Seq[String]
    def findDescendantNamesAsJava(name: String): java.util.List[String]   // for MediaRepository

    // --- Lifecycle
    def clear(): Unit           // drops everything; the next read reloads
    def close(): Unit           // unsubscribes from ChangeBus and stops the staleness check
```

Internals:

- **Concept metadata:** the same two Caffeine caches `ConceptCache` uses today (same keys, same TTL).
- **Phylogeny tree:** an immutable snapshot held in an `AtomicReference[Option[PhylogenySnapshot]]`:
  ```scala
  final case class PhylogenySnapshot(root: Option[MutableConcept],
                                     byId: Map[Long, MutableConcept],
                                     byName: Map[String, MutableConcept],   // every name, not just primary
                                     loadedAt: Instant)
  ```
  - It's loaded lazily on first read after a `clear()`, or once it's older than `ttl`.
  - Loading is single-flight: a lock plus a double-check, so concurrent requests trigger exactly one load and the
    others wait for it. This fixes P3.
  - Readers only ever see a complete snapshot, published atomically.
  - The maps make lookups O(1) instead of a scan (P6).
  - The tree-building code (`MutableConcept.toTree`, the SQL in `FastPhylogenyDAO`) is reused as is. The snapshot is
    never mutated after it's built; `findUp` already works on copies (`copyUp`).
- The per-read `MAX(LAST_UPDATED_TIME)` query goes away (P2). Invalidation is event-driven (§5).

### 4.2 What happens to the old classes

- `ConceptCache`: deleted, replaced by `KnowledgebaseCache`.
- `FastPhylogenyService`: **kept as a thin wrapper** (decision Q1). It holds no state of its own. Every method
  delegates to the cache it's given:
  ```scala
  class FastPhylogenyService(cache: KnowledgebaseCache):
      def findUp(name: String): Option[Concept] = cache.findUp(name)
      // ... findUpById, findDown, findDownById, findSiblings, findSiblingsById,
      //     findDescendantNames, findDescendantNamesAsJava
  ```
  `findLastUpdate()` and the `executeQuery()` loading code move into the cache. `findLastUpdate` is only used by
  `FastPhylogenyServiceSuite`, so it's dropped along with the polling.
- `FastPhylogenyService.java.original` and `FastCache.java.original` are dead files and are deleted (decision Q4).

### 4.3 Wiring

`org.mbari.oni.Endpoints` creates the single cache (and the single wrapper around it) and passes them to everything
that needs them:

```scala
val cache: KnowledgebaseCache                  = KnowledgebaseCache(entityMangerFactory, AppConfig.CacheTtl)
val fastPhylogenyService: FastPhylogenyService = FastPhylogenyService(cache)

val conceptEndpoints   = ConceptEndpoints(entityMangerFactory, cache)
val phylogenyEndpoints = PhylogenyEndpoints(fastPhylogenyService)
val historyEndpoints   = HistoryEndpoints(entityMangerFactory, fastPhylogenyService)   // unchanged signature
val mediaEndpoints     = MediaEndpoints(entityMangerFactory, fastPhylogenyService)     // unchanged signature
```

Constructor changes (production code). Because `FastPhylogenyService` stays as the wrapper, most consumers don't change:

| Class | Before | After |
|---|---|---|
| `FastPhylogenyService` | `(emf)`, owns the tree | `(cache)`, delegates |
| `ConceptEndpoints` | `(emf)`, creates its own `ConceptCache` | `(emf, cache)` |
| `PhylogenyEndpoints` | `(emf)`, creates `FastPhylogenyService`, exposes `val service` | `(fastPhylogenyService)` |
| `HistoryEndpoints`, `HistoryActionService`, `MediaEndpoints`, `MediaService`, `MediaRepository` | take a `FastPhylogenyService` | unchanged |

A new `CacheEndpoints` class adds `POST /cache/clear` (§5.3). It's registered in `org.mbari.oni.Endpoints` with the
others. All other endpoint classes are unchanged.

## 5. Invalidation (decision: commit-driven only)

The cache subscribes to `ChangeBus` and clears everything on any event where `affectsConceptData()` is true. That covers:
- every committed Hibernate insert, update or delete, whichever code path made it (endpoints, history approve/reject,
  services, bulk loads)
- the two native-SQL bulk renames, which publish their own event after commit
- other Oni instances' changes, through `NatsSubscriber`, including the catch-all clear on every NATS (re)connect

Rolled-back writes publish nothing, so they don't clear the cache. Endpoints don't clear the cache themselves; the
explicit `conceptCache.clear()` calls in `ConceptEndpoints` are removed.

Two safety nets cover changes that produce no event, such as writes made directly to the database outside Oni:
- a background **staleness check** that notices most direct database writes within about a minute (§5.2)
- a **TTL** (15 minutes by default) on every entry, which bounds how stale anything can get

An admin can also force a clear on every instance with `POST /cache/clear` (§5.3).

### 5.1 Risks of commit-driven-only, and how each is handled

| # | Risk | Likelihood / impact | Mitigation in this design |
|---|---|---|---|
| R1 | **A read races a write and puts stale data back into the cache.** A read that missed the cache queries the database just *before* a commit, the commit clears the cache, then the read finishes and stores its now-stale result. That entry survives until the next change or the TTL. This exists today too; it isn't new. | Low likelihood (needs a read and a write to overlap within milliseconds); medium impact (stale for up to 15 min) | **Generation counter.** `clear()` increments a counter. A loader records the counter before it reads from the database and only stores its result if the counter hasn't changed. Applies to both the metadata entries and the tree snapshot. Covered by a concurrency test. |
| R2 | **Writes that bypass Hibernate's entity events:** native SQL, JPQL bulk `UPDATE`/`DELETE`, or raw JDBC. They publish nothing. | Today: only the two `toconcept/rename` queries, and both publish manually after commit. Risk is a *future* query that forgets to. | A comment and a test: `ChangeNotificationSuite` asserts that both renames clear the cache. The rule ("any `executeUpdate()` must publish a `ChangeEvent` after commit") goes in a code comment on `ChangeBus`. The TTL bounds the damage if it's ever forgotten. |
| R3 | **Collection-only changes.** `CommitNotifier` listens for entity insert/update/delete, not collection events. Adding or removing a reference on a concept changes only the `@ManyToMany` join table. | Probably fine: `ReferenceEntity` owns the collection and has `@Version`, so Hibernate bumps its version and fires a normal update event. **Not yet proven.** | A test that `PUT /references/add/...` and `/remove/...` clear the cache. If it fails, register `CommitNotifier` for post-commit collection events as well. |
| R4 | **Something other than Oni writes to the database** (a DBA fix, a migration script, a legacy VARS tool). None of these produce events. **This does happen** (Q6). | Medium | The staleness check (§5.2) catches inserts, deletes and timestamped updates within about a minute. The TTL catches the rest within 15 minutes. After a manual fix, an admin can call `POST /cache/clear` (§5.3) for an immediate refresh on every instance. |
| R5 | **NATS drops a message while connected.** Core NATS delivers at most once. | Only matters with more than one instance and NATS enabled | The TTL bounds it. Messages missed while *disconnected* are already handled by the catch-all clear on reconnect. |
| R6 | **The commit listener isn't registered.** `CommitNotifier` is installed by `ChangeNotifierIntegrator`. If an `EntityManagerFactory` were built without it, nothing would ever clear the cache, and nothing would say so. | Low; it's registered for every factory today | `ChangeNotificationSuite` already checks that commits publish events. Add a startup log line confirming the listener is active. |
| R7 | **Many clears during a bulk load.** One event per entity, so loading 10,000 concepts means 10,000 clears. | Harmless: a clear on an already-empty cache is cheap, and the tree only reloads on the next *read* | None needed. Loading is lazy, so clears never trigger reloads. |

**Assessment:** commit-driven-only is the right choice. Endpoint-level clears wouldn't help with any of R1–R7; R1, R4
and R5 affect them equally, and they'd add their own gaps (writes outside endpoints, clearing on failed writes,
forgetting new endpoints). R1 needs the generation counter. R2 and R3 need tests. R4 is the one to think about (Q6).

### 5.2 Staleness check (for writes made outside Oni)

A background task compares a cheap **fingerprint** of the knowledgebase tables with the last one it saw, and clears the
cache if it changed.

- **Fingerprint:** for each table whose data is cached, its row count and `MAX(LAST_UPDATED_TIME)`. Tables: `Concept`,
  `ConceptName`, `ConceptDelegate`, `Media`, `LinkRealization`, `LinkTemplate`, `Reference`. Plus the row count of the
  `Reference_ConceptDelegate` join table, which has no timestamp. It's one query of scalar subqueries
  (`SELECT (SELECT COUNT(*) FROM Concept), (SELECT MAX(LAST_UPDATED_TIME) FROM Concept), ...`), valid on both
  PostgreSQL and SQL Server.
- **What it catches:**
  - inserts and deletes, through the row counts. This includes deletes, which the current polling misses (P1).
  - updates that set `LAST_UPDATED_TIME`, through the max timestamp
- **What it misses:** an external `UPDATE` that leaves `LAST_UPDATED_TIME` alone and changes no row counts. Hibernate
  manages that column (`@Version`), so a hand-written SQL fix usually won't touch it. Those changes are left to the TTL,
  or to `POST /cache/clear` (§5.3). The runbook for manual database fixes should say "then call `/cache/clear`".
- **When it runs:** on its own daemon thread every `cache.staleness-check-interval` (default 60 seconds; 0 disables it).
  It never runs on a request thread, so reads don't wait for it. That avoids P2's per-request database query.
- **Oni's own writes** also change the fingerprint, which causes a redundant second clear. That's harmless and cheap,
  and the generation counter (R1) means it can't store stale data.
- **On failure** (e.g. the database is down) it logs a warning and keeps the previous fingerprint. It does *not* clear
  the cache, which would cause a reload storm like P2.
- **Multiple instances:** each instance runs its own check against the shared database. A clear triggered by the check
  is local and isn't sent over NATS, since every instance will detect the same change itself.
- **Cost:** one query a minute. `MAX(LAST_UPDATED_TIME)` is indexed on `Concept` (`idx_Concept_LUT`). On the other
  tables it may be a scan, which is acceptable once a minute but should be checked against production table sizes. Add
  indexes if needed.

### 5.3 `POST /cache/clear`

- **Access:** administrator login only (403 for other users, 401 without a valid token). API-key tokens are **not**
  accepted, matching the history-delete policy. They can be allowed later if scripts need it.
- **Effect:** publishes `ChangeEvent(UPDATED, CONCEPT, null)` on the `ChangeBus`. The local cache clears through its
  normal subscription, and `NatsBridge` forwards the event, so **every** instance clears when NATS is enabled. It goes
  through exactly the same path as a real change, so there's no special-case code.
- **Response:** `200` with an empty JSON body, like the other action endpoints.
- **Code:** a new `CacheEndpoints` class (tag `Cache`) with an operation name and description, so `OpenApiDocsSuite`
  covers it.

### 5.4 Configuration

| Key | Default | Environment variable |
|---|---|---|
| `cache.ttl` | `15 minutes` | `CACHE_TTL` |
| `cache.staleness-check-interval` | `60 seconds` (`0` disables) | `CACHE_STALENESS_CHECK_INTERVAL` |

`cache.staleness-check-interval` is **implemented**. It's exposed as `AppConfig.DefaultCacheConfig.stalenessCheckInterval`
(`CacheConfig`), and the next step is the cache that reads it. Values use HOCON duration syntax (`60 seconds`, `30s`,
`2 minutes`). A bare number is read as **milliseconds**, so set `CACHE_STALENESS_CHECK_INTERVAL=60s`, not `60`.
Negative or unparseable values stop Oni at startup.

## 6. Mutating endpoints (reference)

With commit-driven invalidation, endpoints don't clear the cache, but this list is still the checklist for the tests in
§7: each ✅ group should have at least one test showing a write clears the cache. ✅ = affects cached data.

| Group | Endpoints | Affects cache |
|---|---|---|
| Concept | `POST /concept`, `PUT /concept/{name}`, `PUT /concept/id/{id}`, `DELETE /concept/{name}`, `DELETE /concept/id/{id}` | ✅ |
| Concept name | `POST /names`, `PUT /names/{name}`, `PUT /names/id/{id}`, `DELETE /names/{name}`, `DELETE /names/id/{id}` | ✅ |
| History | `PUT /history/approve/{id}`, `PUT /history/reject/{id}`, `DELETE /history/{id}` | ✅ (approve/reject change concepts) |
| Link template | `POST /linktemplates`, `PUT /linktemplates/{id}`, `DELETE /linktemplates/{id}`, `PUT /linktemplates/toconcept/rename` | ✅ (part of `ConceptMetadata`) |
| Link realization | `POST /linkrealizations`, `PUT /linkrealizations/{id}`, `DELETE /linkrealizations/{id}`, `PUT /linkrealizations/toconcept/rename` | ✅ (part of `ConceptMetadata`) |
| Media | `POST /media`, `PUT /media/{id}`, `DELETE /media/{id}` | ✅ (part of `ConceptMetadata`) |
| Reference | `POST /references`, `PUT /references/{id}`, `DELETE /references/{id}`, `PUT /references/add/{id}/to/{concept}`, `PUT /references/remove/{id}/from/{concept}` | ✅ (part of `ConceptMetadata`) |
| Preferences | `POST /prefs`, `PUT /prefs`, `DELETE /prefs` | ❌ |
| User accounts | `POST /users`, `PUT /users/{name}`, `DELETE /users/{name}` | ❌ |
| Auth | `POST /auth`, `POST /auth/login` | ❌ (not a data change) |
| Cache | `POST /cache/clear` (new) | ✅ clears it, on every instance |
| Read-only POSTs | `POST /linktemplates/prototype`, `POST /linkrealizations/prototype`, `POST /references/query/citation`, `POST /references/query/doi` | ❌ (queries that use POST) |

The ❌ rows publish no event that clears the cache: user-account and preference changes are filtered out by
`affectsConceptData()`, and the read-only POSTs don't write anything.

## 7. Implementation plan

Each step leaves the build green.

1. **Prove P1 first.** Add a test that loads the phylogeny tree, deletes a concept through the service, and asserts it
   no longer appears in `findDown`. This is expected to fail today. If it doesn't, drop P1 from the motivation.
2. Add `KnowledgebaseCache` in `org.mbari.oni.cache` with:
   - the immutable tree snapshot and single-flight loading
   - the generation counter (R1)
   - the TTL
   - the `ChangeBus` subscription

   Add `KnowledgebaseCacheSuite` covering:
   - by-name and by-id lookups
   - TTL expiry
   - clearing on a committed change, and not clearing on a rollback
   - single-flight loading under concurrent reads
   - the R1 race: a load that overlaps a clear must not store its result
   - the step 1 delete test, now passing
3. Add the two settings in §5.4 to `reference.conf` and `AppConfig`.
4. Add the staleness check (§5.2). Tests:
   - a row inserted with plain SQL outside Hibernate clears the cache
   - a row deleted with plain SQL clears the cache
   - a failed check doesn't clear the cache
   - interval `0` disables the check
   - `close()` stops the background thread
5. Reduce `FastPhylogenyService` to the wrapper (§4.2), wire everything in `org.mbari.oni.Endpoints` (§4.3), and switch
   `ConceptEndpoints` to the shared cache, removing its explicit `clear()` calls.
6. Add `CacheEndpoints` with `POST /cache/clear` (§5.3). Tests:
   - an admin clears the cache
   - a maintainer gets 403
   - an API-key token gets 403
   - no token gets 401
   - the event is forwarded to NATS (`NatsChangeNotificationSuite`)
7. Delete `ConceptCache`, `FastPhylogenyService.java.original` and `FastCache.java.original`.
8. Tests for the risks:
   - R2: both `toconcept/rename` endpoints clear the cache.
   - R3: reference add/remove clears the cache.
   - At least one write per ✅ group in §6 clears the cache: concept, concept name, history approve, link template,
     link realization, media, reference.
   - R6: add the startup log line.
9. Update the tests that construct the old classes directly: `HistoryEndpointsSuite`, `MediaEndpointsSuite`,
   `MediaServiceSuite`, `HistoryActionServiceSuite`, `FastPhylogenyServiceSuite`, `Issue6`, `Oni7`, `OpenApiDocsSuite`,
   `ChangeNotificationSuite` and `NatsChangeNotificationSuite`. Tests must `close()` their cache so `ChangeBus`
   subscriptions don't pile up across suites.
10. Run the full suites on Postgres **and SQL Server.** The phylogeny SQL and timestamp handling have database-specific
   behavior.

## 8. Out of scope

- Enabling Hibernate's second-level cache. It's currently commented out in `EntityManagerFactories` and the entities.
- Finer-grained invalidation (evicting one concept instead of clearing everything). Writes are rare compared with
  reads, so full clears are simple and cheap enough. Revisit if reload time becomes a problem.
- Changing the NATS message format or making NATS enabled by default.

## 9. Decisions

| # | Question | Decision |
|---|---|---|
| Q1 | Delete `FastPhylogenyService` or keep it as a wrapper? | **Keep as a thin wrapper** (§4.2) |
| Q2 | Invalidation strategy | **Commit-driven only** (§5), with the mitigations in §5.1 |
| Q3 | TTL and multiple instances | **15-minute TTL.** One instance today, but must keep working with NATS for multiple instances later |
| Q4 | Delete the dead `.original` files? | **Yes** |
| Q5 | Name and package | **`KnowledgebaseCache` in `org.mbari.oni.cache`** |
| Q6 | Does anything else write to the database? | **Yes, sometimes.** Add the staleness check (§5.2) |
| Q7 | Admin endpoint to force a clear? | **Yes:** `POST /cache/clear`, admin login only (§5.3) |

### Still open

None. Ready to implement once the additions in §5.2–5.4 are approved.

## 10. Implementation notes

Implemented as specified, with these differences and details:

- **P1 confirmed.** The new test "a deleted concept disappears from the phylogeny tree" failed against the old
  `FastPhylogenyService` and passes now.
- **Test seams.** `KnowledgebaseCache` takes the tree loader and fingerprint query as constructor parameters
  (defaulting to `PhylogenyLoader.loadRows` / `PhylogenyLoader.fingerprint`), so tests can count loads and pause a load
  to exercise the R1 race. Production code uses `KnowledgebaseCache(emf, config)`.
- **Generation stamping (R1)** is done by tagging every cached value with the generation it was loaded in, instead of
  "store only if unchanged". Stale results can still be written, but they're never returned. This avoids a
  check-then-store race.
- **Native SQL** moved from `FastPhylogenyService` to `jdbc/PhylogenyLoader.scala` (`PhylogenySql`). The per-read
  `MAX(LAST_UPDATED_TIME)` query and `findLastUpdate()` are gone (along with its test).
- **Staleness check tests** cover an external delete (row count) and an external update that sets `LAST_UPDATED_TIME`
  (max timestamp). They don't cover an external insert, because a portable raw-SQL insert needs database-specific id
  generation, and the delete already exercises the row-count path.
- **Per-group clear tests (§7 step 8):** concept update, concept name add, media create, reference add/remove (R3,
  which passed with no change to `CommitNotifier`) and both link `toconcept/rename` bulk updates (R2). Link templates
  aren't part of `ConceptMetadata`, so there's nothing cached to clear. History approve/reject is covered by the generic
  rule that every committed entity change clears the cache, but has no dedicated test.
- **R6:** the cache logs its settings when it's created, but that doesn't prove `CommitNotifier` is registered.
  `ChangeNotificationSuite` remains the check for that.
- **Tests** use `KnowledgebaseCacheFixture` (one cache per suite, staleness check off, closed in `afterAll`).
- **Verified** with full uncached runs: PostgreSQL 32/32 suites, SQL Server 31/31 suites.
