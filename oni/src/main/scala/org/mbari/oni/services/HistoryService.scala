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

package org.mbari.oni.services

import jakarta.persistence.EntityManagerFactory
import org.mbari.oni.ItemNotFound
import org.mbari.oni.domain.{ExtendedHistory, Sort}
import org.mbari.oni.jpa.EntityManagerFactories.*
import org.mbari.oni.jpa.entities.HistoryEntity
import org.mbari.oni.jpa.repositories.HistoryRepository

import scala.jdk.CollectionConverters.*
import scala.jdk.OptionConverters.*
import scala.util.Try

class HistoryService(entityManagerFactory: EntityManagerFactory):

    def countApproved(): Either[Throwable, Long] =
        entityManagerFactory.readOnlyTransaction(entityManager =>
            val repo = HistoryRepository(entityManager)
            repo.countApprovedHistories()
        )

    def countPending(): Either[Throwable, Long] =
        entityManagerFactory.readOnlyTransaction(entityManager =>
            val repo = HistoryRepository(entityManager)
            repo.countPendingHistories()
        )

    def findAllPending(limit: Int = 100, offset: Int = 0, sort: Option[Sort] = None): Either[Throwable, Seq[ExtendedHistory]] =
        val actualSort = HistoryService.normalizeSort(sort).getOrElse(Sort("creationDate", Sort.Direction.Ascending))
        val sortColumn = actualSort.field
        val direction  = actualSort.direction == Sort.Direction.Ascending

        entityManagerFactory.readOnlyTransaction(entityManager =>
            val repo = HistoryRepository(entityManager)

            // Java entities
            val entities: java.util.Set[HistoryEntity] = repo.findPendingHistories(limit, offset, sortColumn, direction)

            // Scala domain objects
            entities.asScala
                    .toSeq.
                    map(h => 
                        val concept = Try(h.getConceptMetadata().getConcept().getName()).getOrElse("")
                        ExtendedHistory.from(concept, h)
                    )

        )
            

    def findAllApproved(limit: Int = 100, offset: Int = 0, sort: Option[Sort] = None): Either[Throwable, Seq[ExtendedHistory]] =

        val actualSort = HistoryService.normalizeSort(sort).getOrElse(Sort("creationDate", Sort.Direction.Ascending))
        val sortColumn = actualSort.field
        val direction  = actualSort.direction == Sort.Direction.Ascending

        entityManagerFactory.readOnlyTransaction(entityManager =>
            val repo = HistoryRepository(entityManager)

             // Java entities
            val entities: java.util.Set[HistoryEntity] = repo.findApprovedHistories(limit, offset, sortColumn, direction)

            // Scala domain objects
            entities.asScala
                    .toSeq
                    .map(h => 
                        // Fix for https://github.com/mbari-org/kb/issues/12
                        val concept = Try(h.getConceptMetadata().getConcept().getName()).getOrElse("")
                        ExtendedHistory.from(concept, h)
                    ) // TRY because of the potential for nulls during development

        )

    def findById(id: Long): Either[Throwable, ExtendedHistory] =
        entityManagerFactory.readOnlyTransaction(entityManager =>
            val repo = HistoryRepository(entityManager)
            val opt  = repo
                .findByPrimaryKey(classOf[HistoryEntity], id)
                .map(h => ExtendedHistory.from(h.getConceptMetadata.getConcept.getPrimaryConceptName.getName, h))
                .toScala

            opt match
                case Some(history) => history
                case None          => throw ItemNotFound(s"History with id ${id} does not exist")
        )

    def findByConceptName(conceptName: String): Either[Throwable, Seq[ExtendedHistory]] =
        entityManagerFactory.readOnlyTransaction(entityManager =>
            val repo = HistoryRepository(entityManager)
            repo.findByConceptName(conceptName)
                .asScala
                .toSeq
                .map(h => ExtendedHistory.from(h.getConceptMetadata.getConcept.getPrimaryConceptName.getName, h))
                .sortBy(_.creationTimestamp)
        )

    def deleteById(id: Long): Either[Throwable, Unit] =
        entityManagerFactory.transaction(entityManager =>
            val repo = HistoryRepository(entityManager)
            repo.findByPrimaryKey(classOf[HistoryEntity], id).toScala match
                case Some(history) => repo.delete(history)
                case None          => throw ItemNotFound(s"History with id ${id} does not exist")
        )


object HistoryService:

    /**
      * Normalize sort field names to match the database column names. This is necessary because the API may use different
      * field names than the database, and we need to ensure that the sort field is correctly mapped to the database column. For example,
      * the API may use "processedTimestamp" while the database column is "processedDate". This function will convert "processedTimestamp" to "processedDate"
      * and "creationTimestamp" to "creationDate". If the field is not recognized, it will be returned as-is, which may result in a database error if it does not match a valid column.
      * This function can be extended in the future to handle additional field name mappings as needed.
      *
      * @param field
      * @return
      */
    def normalizeSortField(field: String): String =
        field match
            case "processedTimestamp" => "processedDate"
            case "creationTimestamp" => "creationDate"
            case other              => other

    def normalizeSort(sort: Option[Sort]): Option[Sort] =
        sort.map(s => s.copy(field = normalizeSortField(s.field)))