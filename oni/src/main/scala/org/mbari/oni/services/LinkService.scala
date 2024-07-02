/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2024
 *
 * oni code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential.
 */

package org.mbari.oni.services

import jakarta.persistence.EntityManagerFactory
import org.mbari.oni.ConceptNameNotFound
import org.mbari.oni.domain.{ExtendedLink, Link}
import org.mbari.oni.jpa.EntityManagerFactories.*
import org.mbari.oni.jpa.repositories.{ConceptRepository, LinkRealizationRepository, LinkTemplateRepository}

import scala.jdk.CollectionConverters.*
import scala.jdk.OptionConverters.*

class LinkService(entityManagerFactory: EntityManagerFactory):

    def findAllLinkTemplates(limit: Int = 100, offset: Int = 0): Either[Throwable, Seq[Link]] =
        entityManagerFactory.transaction(entityManager =>
            val repo = new LinkTemplateRepository(entityManager)
            repo.findAll(limit, offset)
                .asScala
                .map(Link.from)
                .toSeq
        )

    def findAllLinkTemplatesForConcept(conceptName: String): Either[Throwable, Seq[Link]] =
        entityManagerFactory.transaction(entityManager =>
            val repo        = new LinkTemplateRepository(entityManager)
            val conceptRepo = new ConceptRepository(entityManager)
            conceptRepo.findByName(conceptName).toScala match
                case Some(concept) =>
                    repo.findAllApplicableToConcept(concept)
                        .stream()
                        .map(Link.from)
                        .toList
                        .asScala
                        .toSeq
                case None          => throw ConceptNameNotFound(conceptName)
        )

    /**
     * Find all link templates for a concept by name and filter by link name
     * @param conceptName
     * @param linkName
     * @return
     */
    def findLinkTemplatesByNameForConcept(conceptName: String, linkName: String): Either[Throwable, Seq[Link]] =
        entityManagerFactory.transaction(entityManager =>
            val repo        = new LinkTemplateRepository(entityManager)
            val conceptRepo = new ConceptRepository(entityManager)
            conceptRepo.findByName(conceptName).toScala match
                case Some(concept) =>
                    repo.findAllByLinkName(linkName, concept)
                        .asScala
                        .map(Link.from)
                        .toSeq
                case None          => throw ConceptNameNotFound(conceptName)
        )

    def findLinkRealizationsByLinkName(linkName: String): Either[Throwable, Seq[ExtendedLink]] =
        entityManagerFactory.transaction(entityManager =>
            val repo = new LinkRealizationRepository(entityManager)
            repo.findAllByLinkName(linkName)
                .asScala
                .map(ExtendedLink.from)
                .toSeq
        )
