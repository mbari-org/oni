/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2024
 *
 * oni code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential. 
 */

package org.mbari.oni.services

import jakarta.persistence.EntityManagerFactory
import org.mbari.oni.{ConceptNameNotFound, LinkRealizationIdNotFound}
import org.mbari.oni.domain.{ExtendedLink, Link, LinkUpdate}
import org.mbari.oni.jpa.EntityManagerFactories.*
import org.mbari.oni.etc.jdk.Loggers.given
import org.mbari.oni.jpa.entities.LinkRealizationEntity
import org.mbari.oni.jpa.repositories.{ConceptRepository, LinkRealizationRepository}

import scala.jdk.CollectionConverters.*
import scala.jdk.OptionConverters.*

class LinkRealizationService(entityManagerFactory: EntityManagerFactory):

    private val log                = System.getLogger(getClass.getName)
    private val userAccountService = UserAccountService(entityManagerFactory)

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
                case None          => throw ConceptNameNotFound(conceptName)
        )

    def findByPrototype(link: Link): Either[Throwable, Seq[ExtendedLink]] =
        entityManagerFactory.transaction(entityManager =>
            val repo = new LinkRealizationRepository(entityManager)
            repo.findAllByLinkName(link.linkName)
                .stream()
                .filter(lr => lr.getLinkValue == link.linkValue && lr.getToConcept == link.toConcept)
                .map(ExtendedLink.from)
                .toList
                .asScala
                .toSeq
        )

    def create(conceptName: String, link: Link): Either[Throwable, ExtendedLink] =
        entityManagerFactory.transaction(entityManager =>
            val repo        = new LinkRealizationRepository(entityManager)
            val conceptRepo = new ConceptRepository(entityManager)
            conceptRepo.findByName(conceptName).toScala match
                case Some(concept) =>
                    val linkRealization = link.toLinkRealizationEntity
                    if concept.getConceptMetadata.getLinkRealizations.contains(linkRealization) then
                        throw new IllegalArgumentException(
                            s"$conceptName already contains link ${linkRealization.stringValue()}"
                        )
                    concept.getConceptMetadata.addLinkRealization(linkRealization)
                    ExtendedLink.from(linkRealization)
                case None          => throw ConceptNameNotFound(conceptName)
        )

    def update(linkUpdate: LinkUpdate): Either[Throwable, ExtendedLink] =
        linkUpdate.id match
            case None     => Left(new IllegalArgumentException("LinkRealization id is required"))
            case Some(id) =>
                entityManagerFactory.transaction(entityManager =>
                    val repo = new LinkRealizationRepository(entityManager)
                    repo.findByPrimaryKey(classOf[LinkRealizationEntity], id).toScala match
                        case Some(linkRealization) =>
                            linkUpdate.updateEntity(linkRealization)
                            ExtendedLink.from(linkRealization)
                        case None                  => throw LinkRealizationIdNotFound(id)
                )

    def deleteById(id: Long): Either[Throwable, Unit] =
        entityManagerFactory.transaction(entityManager =>
            val repo = new LinkRealizationRepository(entityManager)
            repo.findByPrimaryKey(classOf[LinkRealizationEntity], id).toScala match
                case Some(linkRealization) =>
                    linkRealization.getConceptMetadata.removeLinkRealization(linkRealization)
                    entityManager.remove(linkRealization)
                case None                  => throw new IllegalArgumentException(s"Link with id ${id} does not exist")
        )
