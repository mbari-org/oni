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

import jakarta.persistence.{EntityManager, EntityManagerFactory}
import org.mbari.oni.domain.{ExtendedLink, ILink, Link, LinkCreate, LinkRenameToConceptResponse, LinkUpdate, LinkUtilities}
import org.mbari.oni.jpa.EntityManagerFactories.*
import org.mbari.oni.jpa.entities.{HistoryEntity, HistoryEntityFactory, LinkTemplateEntity, UserAccountEntity}
import org.mbari.oni.jpa.repositories.{ConceptRepository, LinkTemplateRepository}
import org.mbari.oni.{ConceptNameNotFound, ItemNotFound, LinkRealizationIdNotFound, LinkTemplateIdNotFound}

import scala.jdk.CollectionConverters.*
import scala.jdk.OptionConverters.*

class LinkTemplateService(entityManagerFactory: EntityManagerFactory):

    private val log                = System.getLogger(getClass.getName)
    private val userAccountService = UserAccountService(entityManagerFactory)

    def countAll(): Either[Throwable, Long] =
        entityManagerFactory.transaction(entityManager =>
            val repo = new LinkTemplateRepository(entityManager)
            repo.countAll()
        )

    def findAll(limit: Int = 100, offset: Int = 0): Either[Throwable, Seq[ExtendedLink]] =
        entityManagerFactory.transaction(entityManager =>
            val repo = new LinkTemplateRepository(entityManager)
            repo.findAll(limit, offset)
                .asScala
                .toSeq
                .map(ExtendedLink.from)
                .sortBy(_.shortStringValue)
        )

    def findById(id: Long): Either[Throwable, ExtendedLink] =
        entityManagerFactory.transaction(entityManager =>
            val repo = new LinkTemplateRepository(entityManager)
            repo.findByPrimaryKey(classOf[LinkTemplateEntity], id).toScala match
                case Some(linkTemplate) => ExtendedLink.from(linkTemplate)
                case None               => throw LinkRealizationIdNotFound(id)
        )

    def countByConcept(conceptName: String): Either[Throwable, Long] =
        entityManagerFactory.transaction(entityManager =>
            val repo = new ConceptRepository(entityManager)
            repo.findByName(conceptName).toScala match
                case Some(concept) =>
                    concept
                        .getConceptMetadata
                        .getLinkTemplates
                        .size()
                        .toLong
                case None          => throw ConceptNameNotFound(conceptName)
        )

    def findByConcept(conceptName: String): Either[Throwable, Seq[ExtendedLink]] =
        entityManagerFactory.transaction(entityManager =>
            val repo = new ConceptRepository(entityManager)
            repo.findByName(conceptName).toScala match
                case Some(concept) =>
                    concept
                        .getConceptMetadata
                        .getLinkTemplates
                        .asScala
                        .map(ExtendedLink.from)
                        .toSeq
                        .sortBy(_.shortStringValue)
                case None          => throw ConceptNameNotFound(conceptName)
        )

    def countByToConcept(toConcept: String): Either[Throwable, Long] =
        entityManagerFactory.transaction(entityManager =>
            val repo         = new LinkTemplateRepository(entityManager)
            val conceptRepo  = new ConceptRepository(entityManager)
            val resolvedName = conceptRepo.findByName(toConcept).toScala match
                case Some(concept) => concept.getPrimaryConceptName().getName()
                case None          => toConcept
            repo.countByToConcept(resolvedName)

            // A ToConcept might not be an actual concept, most notably during development/testing
            // So we check if it's used and if not, we check for the primary concept name.
        )

    def findByToConcept(toConcept: String): Either[Throwable, Seq[ExtendedLink]] =
        entityManagerFactory.transaction(entityManager =>
            val repo         = new LinkTemplateRepository(entityManager)
            val conceptRepo  = new ConceptRepository(entityManager)
            val resolvedName = conceptRepo.findByName(toConcept).toScala match
                case Some(concept) => concept.getPrimaryConceptName().getName()
                case None          => toConcept
            repo.findByToConcept(resolvedName)
                .asScala
                .map(ExtendedLink.from)
                .toSeq
                .sortBy(_.shortStringValue)
        )

    def findByPrototype(link: Link): Either[Throwable, Seq[ExtendedLink]] =
        entityManagerFactory.transaction(entityManager =>
            val repo              = new LinkTemplateRepository(entityManager)
            val conceptRepo       = new ConceptRepository(entityManager)
            val resolvedToConcept = conceptRepo.findByName(link.toConcept).toScala match
                case Some(concept) => concept.getPrimaryConceptName().getName()
                case None          => link.toConcept
            repo.findAllByLinkName(link.linkName)
                .stream()
                .filter(lr =>
                    lr.getLinkValue == link.linkValue && (lr.getToConcept == link.toConcept || lr.getToConcept == resolvedToConcept)
                )
                .map(ExtendedLink.from)
                .toList
                .asScala
                .toSeq
                .sortBy(_.shortStringValue)
        )

    def create(link: LinkCreate, userName: String): Either[Throwable, ExtendedLink] =
        def txn(userEntity: UserAccountEntity): Either[Throwable, ExtendedLink] =
            entityManagerFactory.transaction(entityManager =>
                val repo              = new LinkTemplateRepository(entityManager)
                val conceptRepo       = new ConceptRepository(entityManager)
                val resolvedToConcept = conceptRepo.findByName(link.toConcept).toScala match
                    case Some(concept) => concept.getPrimaryConceptName().getName()
                    case None          => link.toConcept
                val resolvedLink      = link.copy(toConcept = resolvedToConcept)
                conceptRepo.findByName(link.concept).toScala match
                    case Some(concept) =>
                        val linkTemplate = resolvedLink.toLink.toLinkTemplateEntity
                        // Check all link templates applicable to this concept
                        val applicable   = repo.findAllApplicableToConcept(concept)
                        if applicable.contains(linkTemplate) then
                            throw new IllegalArgumentException(
                                s"LinkTemplate, `${linkTemplate.stringValue()} already exists for concept ${link.concept}"
                            )
                        concept.getConceptMetadata.addLinkTemplate(linkTemplate)

                        // Add history
                        val history = HistoryEntityFactory.add(userEntity, linkTemplate)
                        concept.getConceptMetadata.addHistory(history)
                        ExtendedLink.from(linkTemplate)
                    case None          => throw ConceptNameNotFound(link.concept)
            )

        for
            user <- userAccountService.verifyWriteAccess(Option(userName))
            link <- txn(user.toEntity)
        yield link

    def updateById(id: Long, linkUpdate: LinkUpdate, userName: String): Either[Throwable, ExtendedLink] =
        def txn(userEntity: UserAccountEntity): Either[Throwable, ExtendedLink] =
            entityManagerFactory.transaction(entityManager =>
                val repo        = new LinkTemplateRepository(entityManager)
                val conceptRepo = new ConceptRepository(entityManager)
                repo.findByPrimaryKey(classOf[LinkTemplateEntity], id).toScala match
                    case Some(linkTemplate) =>
                        val before             = Link.from(linkTemplate)
                        val resolvedName       = conceptRepo.findByName(linkTemplate.getToConcept).toScala match
                            case Some(concept) => concept.getPrimaryConceptName().getName()
                            case None          => linkUpdate.toConcept.getOrElse(linkTemplate.getToConcept)
                        val resolvedLinkUpdate = linkUpdate.copy(toConcept = Some(resolvedName))
                        resolvedLinkUpdate.updateEntity(linkTemplate)
                        // add history
                        val history            = HistoryEntityFactory.replaceLinkTemplate(
                            userEntity,
                            before.toLinkTemplateEntity,
                            linkTemplate
                        )
                        linkTemplate.getConceptMetadata.addHistory(history)

                        if userEntity.isAdministrator then history.approveBy(userEntity.getUserName)
                        entityManager.flush()

                        ExtendedLink.from(linkTemplate)
                    case None               => throw LinkTemplateIdNotFound(id)
            )
        for
            user <- userAccountService.verifyWriteAccess(Option(userName))
            link <- txn(user.toEntity)
        yield link

    def deleteById(id: Long, userName: String): Either[Throwable, Unit] =
        def txn(userEntity: UserAccountEntity): Either[Throwable, Unit] =
            entityManagerFactory.transaction(entityManager =>
                val repo = new LinkTemplateRepository(entityManager)
                repo.findByPrimaryKey(classOf[LinkTemplateEntity], id).toScala match
                    case Some(linkTemplate) =>
                        // Add history
                        val history = HistoryEntityFactory.delete(userEntity, linkTemplate)
                        linkTemplate.getConceptMetadata.addHistory(history)
                        if userEntity.isAdministrator then
                            history.approveBy(userEntity.getUserName)
                            linkTemplate.getConceptMetadata.removeLinkTemplate(linkTemplate)
                            entityManager.remove(linkTemplate)
                    case None               => throw LinkTemplateIdNotFound(id)
            )

        for
            user <- userAccountService.verifyWriteAccess(Option(userName))
            _    <- txn(user.toEntity)
        yield ()

    def renameToConcept(
        oldConcept: String,
        newConcept: String,
        userName: String
    ): Either[Throwable, LinkRenameToConceptResponse] =
        def txn(userEntity: UserAccountEntity): Either[Throwable, LinkRenameToConceptResponse] =
            entityManagerFactory.transaction(entityManager =>
                val query = entityManager.createNamedQuery("LinkTemplate.updateToConcept")
                query.setParameter(1, newConcept)
                query.setParameter(2, oldConcept)
                val n     = query.executeUpdate()
                LinkRenameToConceptResponse(oldConcept, newConcept, n)
            )

        for
            user     <- userAccountService.verifyWriteAccess(Option(userName))
            response <- txn(user.toEntity)
        yield response

    def inTxnRejectAdd(
        history: HistoryEntity,
        user: UserAccountEntity,
        entityManger: EntityManager
    ): Either[Throwable, Boolean] =
        val conceptMetadata = history.getConceptMetadata
        val concept         = conceptMetadata.getConcept
        val opt             = conceptMetadata
            .getLinkTemplates
            .stream()
            .filter(lr => lr.stringValue() == history.getNewValue)
            .findFirst()
            .toScala

        opt match
            case None     => Left(ItemNotFound(s"${concept.getName}${ILink.DELIMITER}${history.getNewValue}"))
            case Some(lr) =>
                conceptMetadata.removeLinkTemplate(lr)
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
            .getLinkTemplates
            .stream()
            .filter(lr => lr.stringValue() == history.getOldValue)
            .findFirst()
            .toScala

        opt match
            case None     => Left(ItemNotFound(s"${concept.getName}${ILink.DELIMITER}${history.getNewValue}"))
            case Some(lr) =>
                conceptMetadata.removeLinkTemplate(lr)
                Right(true)


    def inTxnRejectReplace(
        history: HistoryEntity,
        user: UserAccountEntity,
        entityManger: EntityManager
    ): Either[Throwable, Boolean] =
        val conceptMetadata = history.getConceptMetadata
        val concept         = conceptMetadata.getConcept
        val opt             = conceptMetadata
            .getLinkTemplates
            .stream()
            .filter(lr => lr.stringValue() == history.getNewValue)
            .findFirst()
            .toScala

        opt match
            case None     => Left(ItemNotFound(s"${concept.getName}${ILink.DELIMITER}${history.getNewValue}"))
            case Some(lr) =>
                // Revert to old value
                val linkNode = LinkUtilities.parseLinkNode(history.getOldValue)
                lr.setLinkName(linkNode.linkName())
                lr.setLinkValue(linkNode.linkValue())
                lr.setToConcept(linkNode.toConcept())
                entityManger.flush()
                Right(true)
    
