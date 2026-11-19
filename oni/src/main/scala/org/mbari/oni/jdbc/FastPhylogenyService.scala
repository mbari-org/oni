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

package org.mbari.oni.jdbc

import jakarta.persistence.EntityManagerFactory
import org.mbari.oni.domain.{Concept, SimpleConcept}
import org.mbari.oni.etc.jdk.Loggers.given
import org.mbari.oni.etc.jdk.{JdbcTypes, Loggers}
import org.mbari.oni.jpa.EntityManagerFactories.*

import java.time.Instant
import java.util.concurrent.locks.ReentrantLock
import scala.collection.immutable.ArraySeq

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

    def findUp(name: String): Option[Concept] = up(nodeByName(name))

    def findUpById(id: Long): Option[Concept] = up(nodeById(id))

    def findDown(name: String): Option[Concept] = down(nodeByName(name))

    def findDownById(id: Long): Option[Concept] = down(nodeById(id))

    def findSiblings(name: String): Seq[SimpleConcept] = siblings(nodeByName(name))

    def findSiblingsById(id: Long): Seq[SimpleConcept] = siblings(nodeById(id))

    // Hide the children and walk the branch up to the root
    private def up(node: Option[MutableConcept]): Option[Concept] =
        node.map(_.copyUp()).map(_.root()).map(_.toImmutable)

    // Parent isn't seen so we can walk down from this node
    private def down(node: Option[MutableConcept]): Option[Concept] =
        node.map(_.toImmutable)

    private def siblings(node: Option[MutableConcept]): Seq[SimpleConcept] =
        node.flatMap(n => n.parent.map(p => p.children.map(SimpleConcept.from))).getOrElse(Nil)

    /** Find a node by any of its names, refreshing the cache first if the database has changed */
    private def nodeByName(name: String): Option[MutableConcept] =
        load()
        findMutableNode(name)

    /** Find a node by concept id, refreshing the cache first if the database has changed */
    private def nodeById(id: Long): Option[MutableConcept] =
        load()
        allNodes.find(_.id.contains(id))

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
