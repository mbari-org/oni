/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2024
 *
 * oni code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential.
 */

package org.mbari.oni.services

import jakarta.persistence.{EntityManager, EntityManagerFactory}
import org.mbari.oni.domain.{ExtendedLink, ILink, Link, LinkCreate, LinkUpdate}
import org.mbari.oni.jpa.EntityManagerFactories.*
import org.mbari.oni.jpa.entities.{HistoryEntity, HistoryEntityFactory, LinkRealizationEntity, UserAccountEntity}
import org.mbari.oni.jpa.repositories.{ConceptRepository, LinkRealizationRepository}
import org.mbari.oni.{ConceptNameNotFound, ItemNotFound, LinkRealizationIdNotFound}

import scala.jdk.CollectionConverters.*
import scala.jdk.OptionConverters.*

class LinkRealizationService(entityManagerFactory: EntityManagerFactory):

    private val log                = System.getLogger(getClass.getName)
    private val userAccountService = UserAccountService(entityManagerFactory)

    def countAll(): Either[Throwable, Long] =
        entityManagerFactory.transaction(entityManager =>
            val repo = new LinkRealizationRepository(entityManager)
            repo.countAll()
        )

    def findAll(limit: Int = 100, offset: Int = 0): Either[Throwable, Seq[ExtendedLink]] =
        entityManagerFactory.transaction(entityManager =>
            val repo = new LinkRealizationRepository(entityManager)
            repo.findAll(limit, offset)
                .asScala
                .toSeq
                .map(ExtendedLink.from)
                .sortBy(_.shortStringValue)
        )

    def findById(id: Long): Either[Throwable, ExtendedLink] =
        entityManagerFactory.transaction(entityManager =>
            val repo = new LinkRealizationRepository(entityManager)
            repo.findByPrimaryKey(classOf[LinkRealizationEntity], id).toScala match
                case Some(linkRealization) => ExtendedLink.from(linkRealization)
                case None                  => throw LinkRealizationIdNotFound(id)
        )

    def findByConcept(conceptName: String): Either[Throwable, Seq[ExtendedLink]] =
        entityManagerFactory.transaction(entityManager =>
            val repo        = new LinkRealizationRepository(entityManager)
            val conceptRepo = new ConceptRepository(entityManager)
            conceptRepo.findByName(conceptName).toScala match
                case Some(concept) =>
                    concept
                        .getConceptMetadata
                        .getLinkRealizations
                        .asScala
                        .map(ExtendedLink.from)
                        .toSeq
                        .sortBy(_.shortStringValue)
                case None          => throw ConceptNameNotFound(conceptName)
        )

    def findByPrototype(link: Link): Either[Throwable, Seq[ExtendedLink]] =
        entityManagerFactory.transaction(entityManager =>
            val repo = new LinkRealizationRepository(entityManager)
            val conceptRepo = new ConceptRepository(entityManager)
            val resolvedToConcept = conceptRepo.findByName(link.toConcept).toScala match
                case Some(concept) => concept.getPrimaryConceptName().getName()
                case None          => link.toConcept
            repo.findAllByLinkName(link.linkName)
                .stream()
                .filter(lr => lr.getLinkValue == link.linkValue && (lr.getToConcept == link.toConcept || lr.getToConcept == resolvedToConcept))
                .map(ExtendedLink.from)
                .toList
                .asScala
                .toSeq
                .sortBy(_.shortStringValue)
        )

    def create(link: LinkCreate, userName: String): Either[Throwable, ExtendedLink] =
        def txn(userEntity: UserAccountEntity): Either[Throwable, ExtendedLink] =
            entityManagerFactory.transaction(entityManager =>
                val repo        = new LinkRealizationRepository(entityManager)
                val conceptRepo = new ConceptRepository(entityManager)
                val resolvedToConcept = conceptRepo.findByName(link.toConcept).toScala match
                            case Some(concept) => concept.getPrimaryConceptName().getName()
                            case None          => link.toConcept
                val resolvedLink = link.copy(toConcept = resolvedToConcept)
                conceptRepo.findByName(link.concept).toScala match
                    case Some(concept) =>
                        val linkRealization = resolvedLink.toLink.toLinkRealizationEntity
                        if concept.getConceptMetadata.getLinkRealizations.contains(linkRealization) then
                            throw new IllegalArgumentException(
                                s"${link.concept} already contains link ${linkRealization.stringValue()}"
                            )
                        concept.getConceptMetadata.addLinkRealization(linkRealization)
                        // Add history
                        val history         = HistoryEntityFactory.add(userEntity, linkRealization)
                        concept.getConceptMetadata.addHistory(history)
                        if userEntity.isAdministrator then history.approveBy(userEntity.getUserName)
                        ExtendedLink.from(linkRealization)
                    case None          => throw ConceptNameNotFound(link.concept)
            )

        for
            user <- userAccountService.verifyWriteAccess(Option(userName))
            link <- txn(user.toEntity)
        yield link

    def updateById(id: Long, linkUpdate: LinkUpdate, userName: String): Either[Throwable, ExtendedLink] =
        def txn(userEntity: UserAccountEntity): Either[Throwable, ExtendedLink] =
            entityManagerFactory.transaction(entityManager =>
                val repo = new LinkRealizationRepository(entityManager)
                val conceptRepo = new ConceptRepository(entityManager)
                repo.findByPrimaryKey(classOf[LinkRealizationEntity], id).toScala match
                    case Some(linkRealization) =>
                        val before = Link.from(linkRealization)
                        val resolvedName = conceptRepo.findByName(linkRealization.getToConcept).toScala match
                            case Some(concept) => concept.getPrimaryConceptName().getName()
                            case None          => linkUpdate.toConcept.getOrElse(linkRealization.getToConcept)
                        val resolvedLinkUpdate = linkUpdate.copy(toConcept = Some(resolvedName))
                        resolvedLinkUpdate.updateEntity(linkRealization)

                        // add history
                        val history = HistoryEntityFactory.replaceLinkRealization(
                            userEntity,
                            before.toLinkRealizationEntity,
                            linkRealization
                        )
                        linkRealization.getConceptMetadata.addHistory(history)
                        ExtendedLink.from(linkRealization)
                    case None                  => throw LinkRealizationIdNotFound(id)
            )

        for
            user <- userAccountService.verifyWriteAccess(Option(userName))
            link <- txn(user.toEntity)
        yield link

    def deleteById(id: Long, userName: String): Either[Throwable, Unit] =
        def txn(userEntity: UserAccountEntity): Either[Throwable, Unit] =
            entityManagerFactory.transaction(entityManager =>
                val repo = new LinkRealizationRepository(entityManager)
                repo.findByPrimaryKey(classOf[LinkRealizationEntity], id).toScala match
                    case Some(linkRealization) =>
                        // Add history
                        val history = HistoryEntityFactory.delete(userEntity, linkRealization)
                        linkRealization.getConceptMetadata.addHistory(history)
                        if userEntity.isAdministrator then
                            history.approveBy(userEntity.getUserName)
                            linkRealization.getConceptMetadata.removeLinkRealization(linkRealization)
                            entityManager.remove(linkRealization)
                    case None                  => throw new IllegalArgumentException(s"Link with id ${id} does not exist")
            )

        for
            user <- userAccountService.verifyWriteAccess(Option(userName))
            _    <- txn(user.toEntity)
        yield ()

    def inTxnRejectAdd(
        history: HistoryEntity,
        user: UserAccountEntity,
        entityManger: EntityManager
    ): Either[Throwable, Boolean] =
        val conceptMetadata = history.getConceptMetadata
        val concept         = conceptMetadata.getConcept
        val opt             = conceptMetadata
            .getLinkRealizations
            .stream()
            .filter(lr => lr.stringValue() == history.getNewValue)
            .findFirst()
            .toScala

        opt match
            case None     => Left(ItemNotFound(s"${concept.getName}${ILink.DELIMITER}${history.getNewValue}"))
            case Some(lr) =>
                conceptMetadata.removeLinkRealization(lr)
                entityManger.remove(lr)
                Right(true)

    def inTxnApproveDelete(
        history: HistoryEntity,
        user: UserAccountEntity,
        entityManger: EntityManager
    ): Either[Throwable, Boolean] =
        val conceptMetadata = history.getConceptMetadata
        val concept         = conceptMetadata.getConcept
        val opt             = conceptMetadata
            .getLinkRealizations
            .stream()
            .filter(lr => lr.stringValue() == history.getOldValue)
            .findFirst()
            .toScala

        opt match
            case None     => Left(ItemNotFound(s"${concept.getName}${ILink.DELIMITER}${history.getNewValue}"))
            case Some(lr) =>
                conceptMetadata.removeLinkRealization(lr)
                Right(true)
