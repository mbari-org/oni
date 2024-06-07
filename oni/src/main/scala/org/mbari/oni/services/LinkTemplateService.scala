/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2024
 *
 * oni code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential. 
 */

package org.mbari.oni.services

import jakarta.persistence.EntityManagerFactory
import org.mbari.oni.{ConceptNameNotFound, LinkRealizationIdNotFound, LinkTemplateIdNotFound}
import org.mbari.oni.domain.{ExtendedLink, Link, LinkCreate, LinkUpdate}
import org.mbari.oni.jpa.EntityManagerFactories.*
import org.mbari.oni.etc.jdk.Loggers.given
import org.mbari.oni.jpa.entities.{HistoryEntityFactory, LinkRealizationEntity, LinkTemplateEntity, UserAccountEntity}
import org.mbari.oni.jpa.repositories.{ConceptRepository, LinkTemplateRepository}

import scala.jdk.CollectionConverters.*
import scala.jdk.OptionConverters.*

class LinkTemplateService(entityManagerFactory: EntityManagerFactory):

    private val log                = System.getLogger(getClass.getName)
    private val userAccountService = UserAccountService(entityManagerFactory)

    def findById(id: Long): Either[Throwable, ExtendedLink] =
        entityManagerFactory.transaction(entityManager =>
            val repo = new LinkTemplateRepository(entityManager)
            repo.findByPrimaryKey(classOf[LinkTemplateEntity], id).toScala match
                case Some(linkTemplate) => ExtendedLink.from(linkTemplate)
                case None               => throw LinkRealizationIdNotFound(id)
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
                case None          => throw ConceptNameNotFound(conceptName)
        )

    def findByPrototype(link: Link): Either[Throwable, Seq[ExtendedLink]] =
        entityManagerFactory.transaction(entityManager =>
            val repo = new LinkTemplateRepository(entityManager)
            repo.findAllByLinkName(link.linkName)
                .stream()
                .filter(lr => lr.getLinkValue == link.linkValue && lr.getToConcept == link.toConcept)
                .map(ExtendedLink.from)
                .toList
                .asScala
                .toSeq
        )

    def create(link: LinkCreate, userName: String): Either[Throwable, ExtendedLink] =
        def txn(userEntity: UserAccountEntity): Either[Throwable, ExtendedLink] =
            entityManagerFactory.transaction(entityManager =>
                val repo        = new LinkTemplateRepository(entityManager)
                val conceptRepo = new ConceptRepository(entityManager)
                conceptRepo.findByName(link.concept).toScala match
                    case Some(concept) =>
                        val linkTemplate = link.toLink.toLinkTemplateEntity
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

    def update(linkUpdate: LinkUpdate, userName: String): Either[Throwable, ExtendedLink] =
        def txn(userEntity: UserAccountEntity): Either[Throwable, ExtendedLink] =
            entityManagerFactory.transaction(entityManager =>
                val repo = new LinkTemplateRepository(entityManager)
                repo.findByPrimaryKey(classOf[LinkTemplateEntity], linkUpdate.id).toScala match
                    case Some(linkTemplate) =>
                        val before = Link.from(linkTemplate)
                        linkUpdate.updateEntity(linkTemplate)
                        // add history
                        val history = HistoryEntityFactory.replaceLinkTemplate(userEntity, before.toLinkTemplateEntity, linkTemplate)
                        linkTemplate.getConceptMetadata.addHistory(history)
                        ExtendedLink.from(linkTemplate)
                    case None               => throw LinkTemplateIdNotFound(linkUpdate.id)
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
                        linkTemplate.getConceptMetadata.removeLinkTemplate(linkTemplate)
                        entityManager.remove(linkTemplate)
                    case None               => throw LinkTemplateIdNotFound(id)
            )

        for
            user <- userAccountService.verifyWriteAccess(Option(userName))
            _    <- txn(user.toEntity)
        yield ()
