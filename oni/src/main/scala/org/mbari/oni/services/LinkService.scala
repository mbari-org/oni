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

import jakarta.persistence.EntityManagerFactory
import org.mbari.oni.ConceptNameNotFound
import org.mbari.oni.domain.{ExtendedLink, Link}
import org.mbari.oni.jpa.EntityManagerFactories.*
import org.mbari.oni.jpa.repositories.{ConceptRepository, LinkRealizationRepository, LinkTemplateRepository}

import scala.jdk.CollectionConverters.*
import scala.jdk.OptionConverters.*

class LinkService(entityManagerFactory: EntityManagerFactory):

    def findAllLinkTemplates(limit: Int = 100, offset: Int = 0): Either[Throwable, Seq[Link]] =
        entityManagerFactory.readOnlyTransaction(entityManager =>
            val repo = new LinkTemplateRepository(entityManager)
            repo.findAll(limit, offset)
                .asScala
                .map(Link.from)
                .toSeq
        )

    def findAllLinkTemplatesForConcept(conceptName: String): Either[Throwable, Seq[ExtendedLink]] =
        entityManagerFactory.readOnlyTransaction(entityManager =>
            val repo        = new LinkTemplateRepository(entityManager)
            val conceptRepo = new ConceptRepository(entityManager)
            conceptRepo.findByName(conceptName).toScala match
                case Some(concept) =>
                    repo.findAllApplicableToConcept(concept)
                        .stream()
                        .map(ExtendedLink.from)
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
    def findLinkTemplatesByNameForConcept(conceptName: String, linkName: String): Either[Throwable, Seq[ExtendedLink]] =
        entityManagerFactory.readOnlyTransaction(entityManager =>
            val repo        = new LinkTemplateRepository(entityManager)
            val conceptRepo = new ConceptRepository(entityManager)
            conceptRepo.findByName(conceptName).toScala match
                case Some(concept) =>
                    repo.findAllByLinkName(linkName, concept)
                        .asScala
                        .map(ExtendedLink.from)
                        .toSeq
                case None          => throw ConceptNameNotFound(conceptName)
        )

    def findLinkRealizationsByLinkName(linkName: String): Either[Throwable, Seq[ExtendedLink]] =
        entityManagerFactory.readOnlyTransaction(entityManager =>
            val repo = new LinkRealizationRepository(entityManager)
            repo.findAllByLinkName(linkName)
                .asScala
                .map(ExtendedLink.from)
                .toSeq
        )
