/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2024
 *
 * oni code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential.
 */

package org.mbari.oni.jdbc

import jakarta.persistence.EntityManagerFactory
import org.mbari.oni.domain.{Concept, SimpleConcept}
import org.mbari.oni.etc.jdk.Loggers
import org.mbari.oni.etc.jdk.Loggers.given
import org.mbari.oni.jpa.EntityManagerFactories.*

import java.time.Instant
import java.util.concurrent.locks.ReentrantLock
import scala.collection.immutable.ArraySeq
import org.mbari.oni.etc.jdk.JdbcTypes

/**
 * @author
 *   Brian Schlining
 * @since 2018-02-11T11:19:00
 */
class FastPhylogenyService(entityManagerFactory: EntityManagerFactory):

    private val log = Loggers(getClass)

    private var lastUpdate                       = Instant.EPOCH
    private var rootNode: Option[MutableConcept] = None
    private var allNodes: Seq[MutableConcept]    = Nil
    private val lock                             = new ReentrantLock();

    def findUp(name: String): Option[Concept] =
        load()
        // Hide the children
        //  do branch walk
        findMutableNode(name)
            .map(_.copyUp())
            .map(_.root())
            .map(_.toImmutable)

    def findDown(name: String): Option[Concept] =
        load()
        // Parent is't seen so we can walk down from this node
        val mc = findMutableNode(name)
        mc.map(_.toImmutable)

    def findSiblings(name: String): Seq[SimpleConcept] =
        load()
        findMutableNode(name)
            .flatMap(n => n.parent.map(p => p.children.map(SimpleConcept.from)))
            .getOrElse(Nil)

    private def findMutableNode(name: String): Option[MutableConcept] =
        allNodes.find(_.names.map(_.name).contains(name))

    def findDescendantNames(name: String): Seq[String] =
        load()
        findMutableNode(name)
            .map(_.toImmutable.descendantNames)
            .getOrElse(Nil)

    def findDescendantNamesAsJava(name: String): java.util.List[String] =
        import scala.jdk.CollectionConverters.*
        findDescendantNames(name).asJava

    private def load(): Unit =
        val lastUpdateInDb = findLastUpdate()
        if lastUpdateInDb.isAfter(lastUpdate) then

            lock.lock()
            log.atDebug.log("Loading cache ...")
            try
                val cache = executeQuery()
                if cache.nonEmpty then
                    val lu = cache.maxBy(_.lastUpdate.toEpochMilli)
                    lastUpdate = lu.lastUpdate

                val r = MutableConcept.toTree(cache)
                rootNode = r._1
                allNodes = r._2
            finally lock.unlock()

    def findLastUpdate(): Instant =
        val attempt = entityManagerFactory.transaction(entityManager =>
            val query = entityManager.createNativeQuery(FastPhylogenyDAO.LAST_UPDATE_SQL)
            JdbcTypes.instantConverter(query.getSingleResult()) match
                case Some(instant) => instant
                case None          =>
                    log.atWarn
                        .log(
                            "Unexpected type for last update timestamp. Expected Instant, got: " + query
                                .getSingleResult
                                .getClass
                                .getName
                        )
                    Instant.now()
        )
        attempt match
            case Right(result)   => result
            case Left(exception) =>
                log.atError.withCause(exception).log("Failed to execute last update query")
                Instant.now()

    private def executeQuery(): Seq[ConceptRow] =
        val attempt = entityManagerFactory.transaction(entityManager =>
            val query = entityManager.createNativeQuery(FastPhylogenyDAO.SQL)
            query.getResultList
        )
        attempt match
            case Right(results) =>
                for result <- ArraySeq.unsafeWrapArray(results.toArray)
                yield
                    val row                  = result.asInstanceOf[Array[Object]]
                    val id                   = row(0).asLong.getOrElse(-1L)
                    val parentId             = row(1).asLong
                    val name                 = row(2).asString.orNull
                    val rankLevel            = row(3).asString
                    val rankName             = row(4).asString
                    val nameType             = row(5).asString.orNull
                    val conceptTimestamp     = row(6).asInstant.getOrElse(Instant.now())
                    val conceptNameTimestamp = row(7).asInstant.getOrElse(Instant.now())
                    ConceptRow(
                        id,
                        parentId,
                        name,
                        rankLevel,
                        rankName,
                        nameType,
                        conceptTimestamp,
                        conceptNameTimestamp
                    )

            case Left(exception) =>
                log.atError.withCause(exception).log("Failed to execute query")
                Nil

object FastPhylogenyDAO:
    val SQL: String =
        """SELECT
      |  c.ID,
      |  c.PARENTCONCEPTID_FK,
      |  cn.CONCEPTNAME,
      |  c.RANKLEVEL,
      |  c.RANKNAME,
      |  cn.NAMETYPE,
      |  c.LAST_UPDATED_TIME AS concept_timestamp,
      |  cn.LAST_UPDATED_TIME AS conceptname_timestamp
      |FROM
      |  CONCEPT C LEFT JOIN
      |  ConceptName cn ON cn.CONCEPTID_FK = C.ID
      |WHERE
      | cn.CONCEPTNAME IS NOT NULL
    """.stripMargin('|')

    val LAST_UPDATE_SQL: String =
        """SELECT
      |  MAX(t.mytime)
      |FROM
      |(SELECT
      |  MAX(LAST_UPDATED_TIME) AS mytime
      |FROM
      |  Concept
      |UNION
      |SELECT
      |  MAX(LAST_UPDATED_TIME) AS mytime
      |FROM
      |  ConceptName) t
    """.stripMargin('|')
