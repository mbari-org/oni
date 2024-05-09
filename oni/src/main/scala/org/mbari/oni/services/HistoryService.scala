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
import org.mbari.oni.jpa.repositories.HistoryRepository

import scala.jdk.CollectionConverters.*

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
