/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2024
 *
 * oni code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential.
 */

package org.mbari.oni.services

import jakarta.persistence.{EntityManager, EntityManagerFactory}
import org.mbari.oni.{AccessDenied, HistoryHasBeenPreviouslyProcessed}
import org.mbari.oni.domain.ExtendedHistory
import org.mbari.oni.jpa.EntityManagerFactories.*
import org.mbari.oni.jpa.entities.{HistoryEntity, UserAccountEntity}
import org.mbari.oni.jpa.repositories.{HistoryRepository, UserAccountRepository}
import org.mbari.oni.etc.sdk.Eithers.*
import org.mbari.oni.jdbc.FastPhylogenyService

type HistoryAction = (HistoryEntity, UserAccountEntity, EntityManager) => Either[Throwable, Boolean]

class HistoryActionService(entityManagerFactory: EntityManagerFactory, fastPhylogenyService: FastPhylogenyService):

    private val log             = System.getLogger(getClass.getName)
    private val okHistoryAction = (history: HistoryEntity, user: UserAccountEntity, entityManger: EntityManager) =>
        Right[Throwable, Boolean](true)

    private val notOkHistoryAction = (history: HistoryEntity, user: UserAccountEntity, entityManger: EntityManager) =>
        Right[Throwable, Boolean](false)

    private val conceptNameService     = ConceptNameService(entityManagerFactory)
    private val conceptService         = ConceptService(entityManagerFactory)
    private val historyService         = HistoryService(entityManagerFactory)
    private val linkRealizationService = LinkRealizationService(entityManagerFactory)
    private val linkTemplateService    = LinkTemplateService(entityManagerFactory)
    private val mediaService           = MediaService(entityManagerFactory, fastPhylogenyService)
    private val userAccountService     = UserAccountService(entityManagerFactory)

    /**
     * Process a history record
     * @param historyId
     *   The history record id (primary key)
     * @param username
     *   The username of the user processing the history
     * @param action
     *   A function that takes a HistoryEntity and a UserAccountEntity and returns a boolean. A true indicates
     *   successful processing.
     * @param approved
     *   true if approved, false if rejected.
     * @return
     */
    private def process(
        historyId: Long,
        username: String,
        approved: Boolean
    ): Either[Throwable, ExtendedHistory] =
        entityManagerFactory.transaction(entityManager =>
            // find user account entity
            val historyRepo = new HistoryRepository(entityManager)
            val userRepo    = new UserAccountRepository(entityManager)

            val attempt =
                for
                    userEntity    <- userRepo.findByUserName(username).toEither
                    _             <- if !userEntity.isAdministrator then Left(AccessDenied(username)) else Right(true)
                    historyEntity <- historyRepo.findByPrimaryKey(classOf[HistoryEntity], historyId).toEither
                    _             <- if historyEntity.isProcessed then Left(HistoryHasBeenPreviouslyProcessed(historyId))
                                     else Right(true)
                    action         = lookupHistoryAction(historyEntity, approved)
                    ok            <- action(historyEntity, userEntity, entityManager)
                yield
                    if ok then
                        if approved then historyEntity.approveBy(userEntity.getUserName)
                        else historyEntity.rejectBy(userEntity.getUserName)
                        historyEntity
                    else throw new Exception("Unable to process history")

            attempt match
                case Left(e)  => throw e
                case Right(v) =>
                    ExtendedHistory.from(v.getConceptMetadata.getConcept.getName, v)
        )

    def approve(historyId: Long, username: String): Either[Throwable, ExtendedHistory] =
        process(historyId, username, true)

    def reject(historyId: Long, username: String): Either[Throwable, ExtendedHistory] =
        process(historyId, username, false)

    private def lookupHistoryAction(historyEntity: HistoryEntity, approved: Boolean): HistoryAction =
        if approved then lookupApproveHistoryAction(historyEntity)
        else lookupRejectHistoryAction(historyEntity)

    private def lookupApproveHistoryAction(historyEntity: HistoryEntity): HistoryAction =
        historyEntity.getAction match
            case HistoryEntity.ACTION_ADD     => okHistoryAction
            case HistoryEntity.ACTION_DELETE  =>
                historyEntity.getField match
                    case HistoryEntity.FIELD_CONCEPT_CHILD   => conceptService.inTxnApproveDelete
                    case HistoryEntity.FIELD_CONCEPTNAME     => conceptNameService.inTxnApproveDelete
                    case HistoryEntity.FIELD_LINKREALIZATION => linkRealizationService.inTxnApproveDelete
                    case HistoryEntity.FIELD_LINKTEMPLATE    => linkTemplateService.inTxnApproveDelete
                    case HistoryEntity.FIELD_MEDIA           => mediaService.inTxnApproveDelete
                    case _                                   => notOkHistoryAction
            case HistoryEntity.ACTION_REPLACE =>
                historyEntity.getField match
                    case HistoryEntity.FIELD_CONCEPT_PARENT => okHistoryAction
                    case _                                  => notOkHistoryAction

    private def lookupRejectHistoryAction(historyEntity: HistoryEntity): HistoryAction =
        historyEntity.getAction match
            case HistoryEntity.ACTION_ADD     =>
                historyEntity.getField match
                    case HistoryEntity.FIELD_CONCEPT_CHILD   => conceptService.inTxnRejectAddChildHistory
                    case HistoryEntity.FIELD_CONCEPTNAME     => conceptNameService.inTxnRejectAddConceptName
                    case HistoryEntity.FIELD_LINKREALIZATION => linkRealizationService.inTxnRejectAdd
                    case HistoryEntity.FIELD_LINKTEMPLATE    => linkTemplateService.inTxnRejectAdd
                    case HistoryEntity.FIELD_MEDIA           => mediaService.inTxnRejectAdd
                    case _                                   => notOkHistoryAction
            case HistoryEntity.ACTION_DELETE  => okHistoryAction
            case HistoryEntity.ACTION_REPLACE =>
                historyEntity.getField match
                    case HistoryEntity.FIELD_CONCEPT_PARENT => conceptService.inTxnRejectReplaceParent
                    case _                                  => notOkHistoryAction
