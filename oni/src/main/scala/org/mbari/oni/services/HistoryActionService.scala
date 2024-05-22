/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2024
 *
 * oni code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential. 
 */

package org.mbari.oni.services

import jakarta.persistence.EntityManagerFactory
import org.mbari.oni.domain.ExtendedHistory
import org.mbari.oni.jpa.EntityManagerFactories.*
import org.mbari.oni.jpa.entities.{HistoryEntity, UserAccountEntity}
import org.mbari.oni.jpa.repositories.{HistoryRepository, UserAccountRepository}
import org.mbari.oni.etc.sdk.Eithers.*

import java.time.Instant
import java.util.Date

class HistoryActionService(entityManagerFactory: EntityManagerFactory):

    private val log                = System.getLogger(getClass.getName)
    private val historyService     = HistoryService(entityManagerFactory)
    private val userAccountService = UserAccountService(entityManagerFactory)

    /**
     * Process a history record
     * @param historyId
     *   The history record id (primary key)
     * @param username
     *   The username of the user processing the history
     * @param fn
     *   A function that takes a HistoryEntity and a UserAccountEntity and returns a boolean. A true indicates
     *   successful processing.
     * @param approved
     *   true if approved, false if rejected.
     * @return
     */
    def process(
        historyId: Long,
        username: String,
        fn: (HistoryEntity, UserAccountEntity) => Boolean,
        approved: Boolean
    ): Either[Throwable, ExtendedHistory] =
        entityManagerFactory.transaction(entityManager =>
            // find user account entity
            val historyRepo = new HistoryRepository(entityManager)
            val userRepo    = new UserAccountRepository(entityManager)

            val attempt = for
                userEntity    <- userRepo.findByUserName(username).toEither
                historyEntity <- historyRepo.findByPrimaryKey(classOf[HistoryEntity], historyId).toEither
            yield
                val ok = fn(historyEntity, userEntity)
                if ok then
                    historyEntity.setApproved(approved)
                    historyEntity.setProcessedDate(Date.from(Instant.now()))
                    historyEntity.setProcessorName(userEntity.getUserName)
                    historyEntity
                else throw new Exception("Failed to process history")

            attempt match
                case Left(e)  => throw e
                case Right(v) => ExtendedHistory.from(v.getConceptMetadata.getConcept.getPrimaryConceptName.getName, v)
        )

    private def approve(history: HistoryEntity, user: UserAccountEntity): Boolean =
        if (!user.isAdministrator) then false
        else {
            history.getAction match
                case HistoryEntity.ACTION_ADD => true
                case HistoryEntity.ACTION_DELETE => 
                    history.getField match
                        case HistoryEntity.FIELD_CONCEPT_CHILD => false // TODO
                        case HistoryEntity.FIELD_CONCEPTNAME =>false // TODO
                        case HistoryEntity.FIELD_LINKREALIZATION => false // TODO
                        case HistoryEntity.FIELD_LINKTEMPLATE => false // TODO
                        case HistoryEntity.FIELD_MEDIA => false // TODO
                        case _ => false
                case HistoryEntity.ACTION_REPLACE =>
                    history.getField match
                        case HistoryEntity.FIELD_CONCEPT_PARENT => false // TODO
                        case _ => false
        }
        
        


