/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2024
 *
 * oni code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential.
 */

package org.mbari.oni.services

import jakarta.persistence.EntityManagerFactory
import org.mbari.oni.domain.{Reference, ReferenceUpdate}
import org.mbari.oni.jpa.EntityManagerFactories.*
import org.mbari.oni.jpa.repositories.{ConceptRepository, ReferenceRepository}

import java.net.URI
import scala.jdk.CollectionConverters.*
import scala.jdk.OptionConverters.*

class ReferenceService(entityManagerFactory: EntityManagerFactory):

    private val log = System.getLogger(getClass.getName)

    def findById(id: Long): Either[Throwable, Option[Reference]] =
        entityManagerFactory.transaction(entityManager =>
            val repo = ReferenceRepository(entityManager)
            repo.findById(id)
                .map(Reference.from)
                .toScala
        )

    def findAll(limit: Int, offset: Int): Either[Throwable, Seq[Reference]] =
        entityManagerFactory.transaction(entityManager =>
            val repo = ReferenceRepository(entityManager)
            repo.findAll(limit, offset)
                .asScala
                .toSeq
                .map(Reference.from)
        )

    def findByCitationGlob(glob: String, limit: Int, offset: Int): Either[Throwable, Seq[Reference]] =
        entityManagerFactory.transaction(entityManager =>
            val repo = ReferenceRepository(entityManager)
            repo.findByGlob(glob, limit, offset)
                .asScala
                .toSeq
                .map(Reference.from)
        )

    def findByDoi(doi: URI): Either[Throwable, Option[Reference]] =
        entityManagerFactory.transaction(entityManager =>
            val repo = ReferenceRepository(entityManager)
            repo.findByDoi(doi)
                .map(Reference.from)
                .toScala
        )

    def create(reference: Reference): Either[Throwable, Reference] =
        entityManagerFactory.transaction(entityManager =>
            val repo   = ReferenceRepository(entityManager)
            reference
                .doi
                .foreach(doi =>
                    if repo.findByDoi(doi).isPresent then
                        throw new IllegalArgumentException(
                            s"A Reference with DOI '${doi}' already exists"
                        )
                )
            val entity = reference.toEntity
            entity.setId(null) // just in case. Hibernate requires that this is null for inserts
            repo.create(entity)
            Reference.from(entity)
        )

    def updateById(id: Long, reference: ReferenceUpdate): Either[Throwable, Reference] =
        entityManagerFactory.transaction(entityManager =>
            val repo = ReferenceRepository(entityManager)

            repo.findById(id).toScala match
                case Some(entity) =>
                    reference.doi.foreach(entity.setDoi)
                    reference.citation.foreach(entity.setCitation)
                    Reference.from(entity)
                case None         =>
                    throw new IllegalArgumentException(
                        s"Reference with id '${id}' not found"
                    )
        )

    def deleteById(id: Long): Either[Throwable, Unit] =
        entityManagerFactory.transaction(entityManager =>
            val repo = ReferenceRepository(entityManager)
            repo.findById(id).toScala match
                case None         => throw new IllegalArgumentException(s"Reference with id '${id}' not found")
                case Some(entity) =>
                    val metadatas = entity.getConceptMetadatas.asScala
                    metadatas.foreach(m => m.removeReference(entity))
                    repo.delete(entity)
        )

    def addConcept(id: Long, concept: String): Either[Throwable, Reference] =
        entityManagerFactory.transaction(entityManager =>
            val repo        = ReferenceRepository(entityManager)
            val conceptRepo = ConceptRepository(entityManager)
            conceptRepo.findByName(concept).toScala match
                case None          => throw new IllegalArgumentException(s"Concept with name '${concept}' not found")
                case Some(concept) =>
                    val reference = repo.findById(id).toScala
                    reference match
                        case None            => throw new IllegalArgumentException(s"Reference with id '${id}' not found")
                        case Some(reference) =>
                            concept.getConceptMetadata.addReference(reference)
                            Reference.from(reference)
        )

    def removeConcept(id: Long, concept: String): Either[Throwable, Reference] =
        entityManagerFactory.transaction(entityManager =>
            val repo        = ReferenceRepository(entityManager)
            val conceptRepo = ConceptRepository(entityManager)
            conceptRepo.findByName(concept).toScala match
                case None          => throw new IllegalArgumentException(s"Concept with name '${concept}' not found")
                case Some(concept) =>
                    val reference = repo.findById(id).toScala
                    reference match
                        case None            => throw new IllegalArgumentException(s"Reference with id '${id}' not found")
                        case Some(reference) =>
                            concept.getConceptMetadata.removeReference(reference)
                            Reference.from(reference)
        )
