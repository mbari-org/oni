/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2024
 *
 * oni code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential. 
 */

package org.mbari.oni.jdbc;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.mbari.oni.etc.jdk.Instants;
import org.mbari.oni.etc.jdk.Logging;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;

public class FastPhylogenyService {

    private volatile FastCache cache = null;
    private final ReentrantLock lock = new ReentrantLock();
    private static final Logging log = new Logging(FastPhylogenyService.class.getName());

    public static final String SQL = """
            SELECT
              c.ID,
              c.PARENTCONCEPTID_FK,
              cn.CONCEPTNAME,
              c.RANKLEVEL,
              c.RANKNAME,
              cn.NAMETYPE,
              c.LAST_UPDATED_TIME AS concept_timestamp,
              cn.LAST_UPDATED_TIME AS conceptname_timestamp
            FROM
              CONCEPT C LEFT JOIN
              ConceptName cn ON cn.CONCEPTID_FK = C.ID
            WHERE
              cn.CONCEPTNAME IS NOT NULL
            """;

    public static final String LAST_UPDATE_SQL = """
            SELECT
              MAX(t.mytime)
            FROM
            (SELECT
              MAX(LAST_UPDATED_TIME) AS mytime
            FROM
              Concept
            UNION
            SELECT
              MAX(LAST_UPDATED_TIME) AS mytime
            FROM
              ConceptName) t
            """;


    EntityManagerFactory entityManagerFactory;

    public FastPhylogenyService(EntityManagerFactory entityManagerFactory) {
        this.entityManagerFactory = entityManagerFactory;
    }


    public List<ConceptRow> findAll() {
        try(var entityManager = entityManagerFactory.createEntityManager()) {
            var transaction = entityManager.getTransaction();
            transaction.begin();
            var query = entityManager.createNativeQuery(SQL);
            var resultList = query.getResultList();
            var results = (List<ConceptRow>) resultList.stream()
                    .map(r -> {
                        Object[] row = (Object[]) r;
                        return new ConceptRow(
                                ((Number) row[0]).longValue(),
                                ((Number) row[1]).longValue(),
                                (String) row[2],
                                (String) row[3],
                                (String) row[4],
                                (String) row[5],
                                Instants.from((Timestamp) row[6]),
                                Instants.from((Timestamp) row[7])
                        );
                    })
                    .toList();
            transaction.commit();
            return results;
        }
        catch (Exception e) {
            log.atError().withCause(e).log("Error running findAll");
            return List.of();
        }
    }

    public Instant findLastUpdate() {
        log.atInfo().log("Running findLastUpdate");
        try(var entityManager = entityManagerFactory.createEntityManager()) {
            var query = entityManager.createNativeQuery(LAST_UPDATE_SQL);
            var last = (Timestamp) query.getSingleResult();
            if (last == null) {
                return Instant.now();
            }
            return Instants.from(last);
        }
        catch (Exception e) {
            log.atError().withCause(e).log("Error running findLastUpdate");
            return Instant.MIN;
        }
    }

    public void clearCache() {
        lock.lock();
        cache = null;
        lock.unlock();
    }

    public void loadCache() {
        var lastUpdateInDb = findLastUpdate();
        lock.lock();
        if (cache == null || lastUpdateInDb.isAfter(cache.lastUpdate())) {
            log.atInfo().log("Loading cache");
            var rows = findAll();
            var lastUpdate = rows.stream()
                    .max(Comparator.comparing(ConceptRow::lastUpdate))
                    .map(ConceptRow::lastUpdate)
                    .orElse(Instant.MIN);
            var treeData = MutableConcept.toTree(rows);
            cache = new FastCache(lastUpdate, treeData.root(), treeData.nodes());
            log.atInfo().log("Cache loaded");
        }
        lock.unlock();
    }

    private Optional<MutableConcept> findMutableConcept(String name) {
        if (cache == null) {
            return Optional.empty();
        }
        return cache.allNodes()
                .stream()
                .filter(mc -> mc.getNames()
                        .stream()
                        .anyMatch(s -> s.name().equals(name)))
                .findFirst();
    }

    public Optional<ImmutableConcept> findUp(String name) {
        loadCache();
        return findMutableConcept(name)
                .map(MutableConcept::copyUp)
                .map(MutableConcept::root)
                .map(ImmutableConcept::from);
    }

    public Optional<ImmutableConcept> findDown(String name) {
        loadCache();
        return findMutableConcept(name)
                .map(ImmutableConcept::from);
    }

    public List<SimpleConcept> findSiblings(String name) {
        loadCache();
        var  opt = findMutableConcept(name);
        if (opt.isEmpty()) {
            return List.of();
        }
        var mc = opt.get();
        return mc.getParent()
                .getChildren()
                .stream()
                .map(SimpleConcept::from)
                .toList();
    }

    public List<String> findDescendantNames(String name) {
        loadCache();
        var concept = findDown(name);
        var names = new ArrayList<String>();
        if (concept.isPresent()) {
            collectDescendantNames(concept.get(), names);
        }
        return names;

    }

    private static void collectDescendantNames(ImmutableConcept concept, List<String> names) {
        names.add(concept.name());
        concept.children().forEach(c -> collectDescendantNames(c, names));
    }


}
