/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2024
 *
 * oni code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential. 
 */

package org.mbari.oni.services

import jakarta.persistence.EntityManagerFactory
import org.mbari.oni.{ConceptNameNotFound, MissingRootConcept}
import org.mbari.oni.domain.{ConceptMetadata, RawConcept}
import org.mbari.oni.jpa.entities.ConceptEntity
import org.mbari.oni.jpa.EntityManagerFactories.*
import org.mbari.oni.jpa.repositories.ConceptRepository

import scala.jdk.CollectionConverters.*
import scala.jdk.OptionConverters.*

class ConceptService(entityManagerFactory: EntityManagerFactory):

    def init(root: ConceptEntity): Either[Throwable, ConceptEntity] =
        entityManagerFactory.transaction(entityManager =>
            entityManager.persist(root)
            root
        )

    def deleteByName(name: String): Either[Throwable, Unit] =
        entityManagerFactory.transaction(entityManager =>
            val repo = new ConceptRepository(entityManager)
            repo.findByName(name).toScala match
                case None    => throw ConceptNameNotFound(name)
                case Some(c) =>
                    repo.deleteBranchByName(name)
        )

    private def handleByNameQuery[T](name: String, fn: ConceptEntity => T): Either[Throwable, T] =
        entityManagerFactory.transaction(entityManager =>
            val repo = new ConceptRepository(entityManager)
            repo.findByName(name).toScala match
                case None    => throw ConceptNameNotFound(name)
                case Some(c) => fn(c)
        )

    def findByName(name: String): Either[Throwable, ConceptMetadata] =
        handleByNameQuery(name, ConceptMetadata.from)

    def findParentByChildName(name: String): Either[Throwable, ConceptMetadata] =
        handleByNameQuery(name, c => ConceptMetadata.from(c.getParentConcept))

    def findChildrenByParentName(name: String): Either[Throwable, Seq[ConceptMetadata]] =
        handleByNameQuery(name, c => c.getChildConcepts.asScala.map(ConceptMetadata.from).toSeq)

    def findRoot(): Either[Throwable, ConceptMetadata] =
        entityManagerFactory.transaction(entityManager =>
            val repo = new ConceptRepository(entityManager)
            repo.findRoot().toScala match
                case None    => throw MissingRootConcept
                case Some(c) => ConceptMetadata.from(c)
        )

    def findByGlob(glob: String): Either[Throwable, Seq[ConceptMetadata]] =
        entityManagerFactory.transaction(entityManager =>
            val repo = new ConceptRepository(entityManager)
            repo.findAllByNameContaining(glob)
                .asScala
                .map(ConceptMetadata.from)
                .toSeq
        )

    def tree(): Either[Throwable, RawConcept] =
        entityManagerFactory.transaction(entityManager =>
            val repo = new ConceptRepository(entityManager)
            val root = repo.findRoot().toScala match
                case None    => throw MissingRootConcept
                case Some(c) => c
            RawConcept.fromEntity(root)
        )
