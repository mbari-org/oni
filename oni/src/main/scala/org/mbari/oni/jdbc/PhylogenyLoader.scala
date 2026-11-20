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
import org.mbari.oni.jpa.EntityManagerFactories.*

import java.time.Instant
import scala.collection.immutable.ArraySeq

/**
 * Native SQL used by the knowledgebase cache: loading the concept tree, and computing a cheap fingerprint of the tables
 * whose data is cached so changes made outside Oni can be detected.
 */
object PhylogenyLoader:

    /** Loads one row per concept name, with its concept's id, parent id and rank. */
    def loadRows(entityManagerFactory: EntityManagerFactory): Either[Throwable, Seq[ConceptRow]] =
        entityManagerFactory.readOnlyTransaction(entityManager =>
            val results = entityManager.createNativeQuery(PhylogenySql.Tree).getResultList
            for result <- ArraySeq.unsafeWrapArray(results.toArray)
            yield
                val row = result.asInstanceOf[Array[Object]]
                ConceptRow(
                    id = row(0).asLong.getOrElse(-1L),
                    parentId = row(1).asLong,
                    name = row(2).asString.orNull,
                    rankLevel = row(3).asString,
                    rankName = row(4).asString,
                    nameType = row(5).asString.orNull,
                    conceptTimestamp = row(6).asInstant.getOrElse(Instant.now()),
                    conceptNameTimestamp = row(7).asInstant.getOrElse(Instant.now())
                )
        )

    /**
     * A fingerprint of the cached tables: each table's row count and latest LAST_UPDATED_TIME. It changes on any insert or
     * delete, and on any update that sets LAST_UPDATED_TIME (which Hibernate always does). Values are compared as
     * strings so the result is independent of the database's numeric and timestamp types.
     */
    def fingerprint(entityManagerFactory: EntityManagerFactory): Either[Throwable, Seq[String]] =
        entityManagerFactory.readOnlyTransaction(entityManager =>
            val row = entityManager.createNativeQuery(PhylogenySql.Fingerprint).getSingleResult
            row match
                case values: Array[Object] => values.toSeq.map(String.valueOf)
                case value                 => Seq(String.valueOf(value))
        )

object PhylogenySql:

    val Tree: String =
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

    /** Tables whose data ends up in the cache (directly or as part of ConceptMetadata). */
    val FingerprintTables: Seq[String] =
        Seq("Concept", "ConceptName", "ConceptDelegate", "Media", "LinkRealization", "LinkTemplate", "Reference")

    /** Join tables have no LAST_UPDATED_TIME, so only their row count is used. */
    val FingerprintJoinTables: Seq[String] = Seq("Reference_ConceptDelegate")

    /** One row of scalar subqueries; valid on both PostgreSQL and SQL Server. */
    val Fingerprint: String =
        val columns =
            FingerprintTables.flatMap(t => Seq(s"(SELECT COUNT(*) FROM $t)", s"(SELECT MAX(LAST_UPDATED_TIME) FROM $t)")) ++
                FingerprintJoinTables.map(t => s"(SELECT COUNT(*) FROM $t)")
        columns.mkString("SELECT ", ", ", "")
