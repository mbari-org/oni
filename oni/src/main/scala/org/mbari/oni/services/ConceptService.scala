/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2024
 *
 * oni code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential. 
 */

package org.mbari.oni.services

import jakarta.persistence.{EntityManager, EntityManagerFactory}
import org.mbari.oni.{ConceptNameNotFound, MissingRootConcept, RootAlreadyExists}
import org.mbari.oni.domain.{ConceptMetadata, RawConcept}
import org.mbari.oni.jpa.entities.ConceptEntity
import org.mbari.oni.jpa.EntityManagerFactories.*
import org.mbari.oni.jpa.repositories.ConceptRepository
import org.mbari.oni.etc.jdk.Loggers.given

import scala.jdk.CollectionConverters.*
import scala.jdk.OptionConverters.*

class ConceptService(entityManagerFactory: EntityManagerFactory):

    private val log = System.getLogger(getClass.getName)

    /**
     * Inserts an entire tree of concepts in the database. Requires that the database is empty. This is an ACID
     * operation.
     * @param root
     *   The root concept of the tree
     * @return
     *   The root concept or an exception if the root already exists or an error occurs
     */
    def init(root: ConceptEntity): Either[Throwable, ConceptEntity] =
        entityManagerFactory.transaction(entityManager =>
            val repo = new ConceptRepository(entityManager)
            repo.findRoot().toScala match
                case None    => entityManager.persist(root)
                case Some(c) => throw RootAlreadyExists
            root
        )

    /**
     * Inserts an entire tree of concepts in the database. Requires that the database is empty. This is a non-ACID as
     * one concept (and it's associated metadata) is inserted at a time. This is useful for testing and for very large
     * trees.
     * @param root
     *   The root concept of the tree
     * @return
     *   The entire tree. We don not return a ConceptEntity as it's lazy relations will not be loaded. Instead we return
     *   a RawConcept which will force the lazy relations to be loaded.
     */
    def nonAcidInit(root: ConceptEntity): Either[Throwable, RawConcept] =
        val either = entityManagerFactory.transaction(entityManager =>
            val repo = new ConceptRepository(entityManager)
            repo.findRoot().toScala
        )

        either match
            case Left(e)             => Left(e)
            case Right(existingRoot) =>
                existingRoot match
                    case None    =>
                        cascadeInsert(null, root)
                        entityManagerFactory.transaction(entityManager =>
                            val repo = new ConceptRepository(entityManager)
                            repo.findRoot().toScala match
                                case None    => throw MissingRootConcept
                                case Some(c) => RawConcept.from(c)
                        )
                    case Some(_) => Left(RootAlreadyExists)

    /**
     * Find a concept by one of its names and delete it and all its descendants.
     * @param name
     *   The name of the concept to delete
     * @return
     *   The number of concepts deleted
     */
    def deleteByName(name: String): Either[Throwable, Int] =
        handleByConceptName(name, (_, repo) => repo.deleteBranchByName(name))

    def findByName(name: String): Either[Throwable, ConceptMetadata] =
        handleByConceptNameQuery(name, ConceptMetadata.from)

    def findParentByChildName(name: String): Either[Throwable, ConceptMetadata] =
        handleByConceptNameQuery(name, c => ConceptMetadata.from(c.getParentConcept))

    def findChildrenByParentName(name: String): Either[Throwable, Set[ConceptMetadata]] =
        handleByConceptNameQuery(name, c => c.getChildConcepts.asScala.map(ConceptMetadata.from).toSet)

    def findRoot(): Either[Throwable, ConceptMetadata] =
        entityManagerFactory.transaction(entityManager =>
            val repo = new ConceptRepository(entityManager)
            repo.findRoot().toScala match
                case None    => throw MissingRootConcept
                case Some(c) => ConceptMetadata.from(c)
        )

    def findByGlob(glob: String): Either[Throwable, Set[ConceptMetadata]] =
        entityManagerFactory.transaction(entityManager =>
            val repo = new ConceptRepository(entityManager)
            repo.findAllByNameContaining(glob)
                .asScala
                .map(ConceptMetadata.from)
                .toSet
        )

    def tree(): Either[Throwable, RawConcept] =
        entityManagerFactory.transaction(entityManager =>
            val repo = new ConceptRepository(entityManager)
            val root = repo.findRoot().toScala match
                case None    => throw MissingRootConcept
                case Some(c) => c
            RawConcept.from(root)
        )

    // -- Helper methods --
    private def cascadeInsert(
        parent: ConceptEntity,
        child: ConceptEntity
    ): Unit =

        // Detach the children so they can be processed separately
        val children   = new java.util.HashSet(child.getChildConcepts)
        children.forEach(c => child.removeChildConcept(c))
        val childNames = children.asScala.map(_.getPrimaryConceptName.getName).mkString(", ")

        child.setId(null)

        entityManagerFactory.transaction(entityManager =>
            log.atDebug.log(s"Inserting ${child.getPrimaryConceptName.getName} which has children: $childNames")

            if parent != null then
                entityManager.find(classOf[ConceptEntity], parent.getId) match
                    case null =>
                        throw new RuntimeException(
                            s"Parent ${parent.getPrimaryConceptName.getName} with id=${parent.getId} not found"
                        )
                    case p    => p.addChildConcept(child)
            else entityManager.persist(child)
        )

        children.forEach(cascadeInsert(child, _))

    /**
     * Looks up a concept by one of it's names and then applies the function to the concept. This is a common pattern in
     * this service.
     *
     * @param name
     *   The concept name to look up
     * @param fn
     *   The function to apply to the concept
     * @tparam T
     *   The return type of the function
     * @return
     *   The result of the function
     */
    private def handleByConceptName[T](
        name: String,
        fn: (ConceptEntity, ConceptRepository) => T
    ): Either[Throwable, T] =
        entityManagerFactory.transaction(entityManager =>
            val repo = new ConceptRepository(entityManager)
            repo.findByName(name).toScala match
                case None    => throw ConceptNameNotFound(name)
                case Some(c) => fn(c, repo)
        )

    /**
     * Handles a query by name. This is a common pattern in this service.
     *
     * @param name
     *   The name of the concept to look up
     * @param fn
     *   The function to apply to the concept
     * @tparam T
     *   The return type of the function
     * @return
     *   The result of the function
     */
    private def handleByConceptNameQuery[T](name: String, fn: ConceptEntity => T): Either[Throwable, T] =
        // Convert the (ConceptEntity) => T function to a (ConceptEntity, ConceptRepository) => T function
        val fn2 = (c: ConceptEntity, _: ConceptRepository) => fn(c)
        handleByConceptName(name, fn2)
