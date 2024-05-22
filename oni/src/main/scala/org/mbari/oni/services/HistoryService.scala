/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2024
 *
 * oni code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential. 
 */

package org.mbari.oni.services

import org.mbari.oni.domain.ExtendedHistory
import jakarta.persistence.EntityManagerFactory
import org.mbari.oni.jpa.EntityManagerFactories.*
import org.mbari.oni.jpa.entities.HistoryEntity
import org.mbari.oni.jpa.repositories.HistoryRepository

import scala.jdk.CollectionConverters.*
import scala.jdk.OptionConverters.*

class HistoryService(entityManagerFactory: EntityManagerFactory):

    def findAllPending(): Either[Throwable, Seq[ExtendedHistory]] =
        entityManagerFactory.transaction(entityManager =>
            val repo = HistoryRepository(entityManager)
            repo.findPendingHistories()
                .asScala
                .toSeq
                .map(h => ExtendedHistory.from(h.getConceptMetadata.getConcept.getPrimaryConceptName.getName, h))
                .sortBy(_.creationTimestamp)
        )

    def findAllApproved(): Either[Throwable, Seq[ExtendedHistory]] =
        entityManagerFactory.transaction(entityManager =>
            val repo = HistoryRepository(entityManager)
            repo.findApprovedHistories()
                .asScala
                .toSeq
                .map(h => ExtendedHistory.from(h.getConceptMetadata.getConcept.getPrimaryConceptName.getName, h))
                .sortBy(_.creationTimestamp)
        )

    def findById(id: Long): Either[Throwable, Option[ExtendedHistory]] =
        entityManagerFactory.transaction(entityManager =>
            val repo = HistoryRepository(entityManager)
            repo.findByPrimaryKey(classOf[HistoryEntity], id)
                .map(h => ExtendedHistory.from(h.getConceptMetadata.getConcept.getPrimaryConceptName.getName, h))
                .toScala
        )


    // TODO: Create

    // TODO: Update (e.g. approve/reject)

    //TODO: Delete?? Do we ever need to delete a history


