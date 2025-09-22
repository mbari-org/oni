/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2024
 *
 * oni code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential.
 */

package org.mbari.oni.services

import jakarta.persistence.EntityManagerFactory
import org.mbari.oni.ItemNotFound
import org.mbari.oni.domain.ExtendedHistory
import org.mbari.oni.jpa.EntityManagerFactories.*
import org.mbari.oni.jpa.entities.HistoryEntity
import org.mbari.oni.jpa.repositories.HistoryRepository

import scala.jdk.CollectionConverters.*
import scala.jdk.OptionConverters.*
import scala.util.Try

class HistoryService(entityManagerFactory: EntityManagerFactory):

    def countApproved(): Either[Throwable, Long] =
        entityManagerFactory.transaction(entityManager =>
            val repo = HistoryRepository(entityManager)
            repo.countApprovedHistories()
        )

    def countPending(): Either[Throwable, Long] =
        entityManagerFactory.transaction(entityManager =>
            val repo = HistoryRepository(entityManager)
            repo.countPendingHistories()
        )

    def findAllPending(limit: Int = 100, offset: Int = 0): Either[Throwable, Seq[ExtendedHistory]] =
        entityManagerFactory.transaction(entityManager =>
            val repo = HistoryRepository(entityManager)
            repo.findPendingHistories(limit, offset)
                .asScala
                .toSeq
                .map(h =>
                    ExtendedHistory.from(h.getConceptMetadata.getConcept.getPrimaryConceptName.getName, h)
                ) // TRY because of the potential for nulls
                .sortBy(_.creationTimestamp)
        )

    def findAllApproved(limit: Int = 100, offset: Int = 0): Either[Throwable, Seq[ExtendedHistory]] =
        entityManagerFactory.transaction(entityManager =>
            val repo = HistoryRepository(entityManager)
            repo.findApprovedHistories(limit, offset)
                .asScala
                .toSeq
                .map(h =>
                    val concept = Try(h.getConceptMetadata().getConcept().getName()).getOrElse("")
                    ExtendedHistory.from(concept, h)
                    // Fix for https://github.com/mbari-org/kb/issues/12
                    // Try(ExtendedHistory.from(h.getConceptMetadata.getConcept.getPrimaryConceptName.getName, h)).toOption
                ) // TRY because of the potential for nulls during development
                .sortBy(_.creationTimestamp)
        )

    def findById(id: Long): Either[Throwable, ExtendedHistory] =
        entityManagerFactory.transaction(entityManager =>
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
        entityManagerFactory.transaction(entityManager =>
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
