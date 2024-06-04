/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2024
 *
 * oni code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential. 
 */

package org.mbari.oni.services

import jakarta.persistence.{EntityManager, EntityManagerFactory}
import org.mbari.oni.{
    AccessDenied,
    AccessDeniedMissingCredentials,
    ConceptNameAlreadyExists,
    ConceptNameNotFound,
    MissingRootConcept,
    OniException,
    RootAlreadyExists
}
import org.mbari.oni.domain.{ConceptCreate, ConceptDelete, ConceptMetadata, ConceptUpdate, RawConcept, SimpleConcept}
import org.mbari.oni.jpa.entities.{
    ConceptEntity,
    ConceptNameEntity,
    HistoryEntity,
    HistoryEntityFactory,
    UserAccountEntity
}
import org.mbari.oni.jpa.EntityManagerFactories.*
import org.mbari.oni.jpa.repositories.ConceptRepository
import org.mbari.oni.etc.jdk.Loggers.given

import scala.jdk.CollectionConverters.*
import scala.jdk.OptionConverters.*

class ConceptService(entityManagerFactory: EntityManagerFactory):

    private val log                = System.getLogger(getClass.getName)
    private val historyService     = HistoryService(entityManagerFactory)
    private val userAccountService = UserAccountService(entityManagerFactory)

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

    def findRawByName(name: String, includeChildren: Boolean = false): Either[Throwable, RawConcept] =
        val fn = RawConcept.from(_, includeChildren) // eta expansion and curry
        handleByConceptNameQuery(name, fn)

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

    /**
     * Create a concept, including history tracking based on the user who created the concept. The history is added to
     * the parent concept. This service does not check the users access credentials, that is done by the web service
     * layer. It does, however, check that the user has write access.
     * @param conceptCreate
     *   This contains the data needed to create the concept
     * @return
     *   The newly created concept. Or an exception if the concept already exists or the parent concept does not exist
     *   or the user does not exist.
     */
    def create(conceptCreate: ConceptCreate): Either[Throwable, ConceptMetadata] =

        // -- Helper function to build the concept and history in a transaction
        def buildInTxn(userEntity: UserAccountEntity, parent: ConceptEntity, entityManager: EntityManager): ConceptEntity =

            // build concept
            val concept     = new ConceptEntity()
            conceptCreate.aphiaId.foreach(v => concept.setAphiaId(v.longValue()))
            conceptCreate.rankLevel.foreach(v => concept.setRankLevel(v))
            conceptCreate.rankName.foreach(v => concept.setRankName(v))
            val conceptName = new ConceptNameEntity(conceptCreate.name)
            concept.addConceptName(conceptName)
            if (parent != null) {
                parent.addChildConcept(concept)

                // build history
                val history = HistoryEntityFactory.add(userEntity, parent)
                parent.getConceptMetadata.addHistory(history)
            }
            else {
                findRoot() match
                    case Left(_) => entityManager.persist(concept)
                    case Right(_) =>
                        throw new IllegalArgumentException("Root concept already exists. Cannot create a new root concept")
            }
            concept

        // -- Helper function to run the transaction
        def txn(userEntity: UserAccountEntity): Either[Throwable, ConceptMetadata] =
            entityManagerFactory.transaction(entityManager =>
                val repo = new ConceptRepository(entityManager)
                repo.findByName(conceptCreate.name).toScala match
                    case Some(_) => throw ConceptNameAlreadyExists(conceptCreate.name)
                    case None    =>
                        val parent = conceptCreate.parentName match
                            case Some(parentName) =>
                                repo.findByName(parentName).toScala match
                                    case Some(p) => p
                                    case None    => throw ConceptNameNotFound(parentName)
                            case None             => null

                        val conceptEntity = buildInTxn(userEntity, parent, entityManager)
                        ConceptMetadata.from(conceptEntity)
            )

        // -- Create logic. Each step is wrapped in an Either
        for
            user    <- userAccountService.verifyWriteAccess(conceptCreate.userName)
            concept <- txn(user.toEntity)
        yield concept

    /**
     * Update a concept. This service does not check the users access credentials, that is done by the web service layer.
     * @param conceptUpdate The update data
     * @return The updated concept or an exception if any errors occur
     */
    def update(conceptUpdate: ConceptUpdate): Either[Throwable, ConceptMetadata] =

        // -- Helper function to update the parent concept
        def updateParent(userEntity: UserAccountEntity, conceptEntity: ConceptEntity, parentName: Option[String])(using repo: ConceptRepository): Unit =
            parentName.foreach(name =>
                if !conceptEntity.hasParent then
                    throw new IllegalArgumentException(s"Cannot set the parent of the root concept!")

                repo.findByName(name).toScala match
                    case None    => throw ConceptNameNotFound(name)
                    case Some(p) =>

                        // Don't allow cyclic relation
                        if conceptEntity.hasDescendent(name) then
                            throw new IllegalArgumentException(s"Cannot set parent ($name) to a descendant of the concept (${conceptEntity.getPrimaryConceptName.getName}). This would create a cyclic relation")

                        // Only update if the parent is different
                        if !conceptEntity.getParentConcept.hasConceptName(name) then
                            val history = HistoryEntityFactory.replaceParentConcept(userEntity, conceptEntity.getParentConcept, p)
                            conceptEntity.getConceptMetadata.addHistory(history)
                            conceptEntity.getParentConcept match
                                case null =>
                                    p.addChildConcept(conceptEntity)
                                case parent =>
                                    parent.removeChildConcept(conceptEntity)
                                    p.addChildConcept(conceptEntity)
            )


        // -- Helper function to update the rank level
        def updateRankLevel(userEntity: UserAccountEntity, conceptEntity: ConceptEntity, rankLevel: Option[String]): Unit =
            rankLevel.foreach( v =>
                if v != conceptEntity.getRankLevel then
                        val history = HistoryEntityFactory.replaceRankLevel(userEntity, conceptEntity.getRankLevel, v)
                        conceptEntity.getConceptMetadata.addHistory(history)
                        conceptEntity.setRankLevel(v)
            )

        // -- Helper function to update the rank name
        def updateRankName(userEntity: UserAccountEntity, conceptEntity: ConceptEntity, rankLevel: Option[String]): Unit =
            rankLevel.foreach( v =>
                if v != conceptEntity.getRankName then
                        val history = HistoryEntityFactory.replaceRankName(userEntity, conceptEntity.getRankName, v)
                        conceptEntity.getConceptMetadata.addHistory(history)
                        conceptEntity.setRankName(v)
            )

        // -- Helper function to update the aphia id
        def updateAphiaId(conceptEntity: ConceptEntity, aphiaId: Option[Long])(using repo: ConceptRepository): Unit=
            aphiaId.foreach( v =>
                if v != conceptEntity.getAphiaId then
                        repo.findByAphiaId(v).toScala match
                            case Some(v) =>
                                if v.getId != conceptEntity.getId then
                                    throw new IllegalArgumentException(s"AphiaId $v already exists for " + v.getPrimaryConceptName.getName)
                            case None =>
                                conceptEntity.setAphiaId(v)
            )

        def txn(userEntity: UserAccountEntity): Either[Throwable, ConceptMetadata] =
            entityManagerFactory.transaction(entityManager =>
                given repo: ConceptRepository = new ConceptRepository(entityManager)
                repo.findByName(conceptUpdate.name).toScala match
                    case None    => throw ConceptNameNotFound(conceptUpdate.name)
                    case Some(conceptEntity) =>
                        updateParent(userEntity, conceptEntity, conceptUpdate.parentName)
                        updateRankLevel(userEntity, conceptEntity, conceptUpdate.rankLevel)
                        updateRankName(userEntity, conceptEntity, conceptUpdate.rankName)
                        updateAphiaId(conceptEntity, conceptUpdate.aphiaId)
                        ConceptMetadata.from(conceptEntity)
            )

        for
            user <- userAccountService.verifyWriteAccess(conceptUpdate.userName)
            concept <- txn(user.toEntity)
        yield
            concept


    /**
     * Delete a concept and all its descendants. This service does not check the users access credentials, that is done
     * by the web service layer. Only administrators can delete concepts. This is a non recoverable operation.
     * @param conceptDelete Information about the concept to delete
     * @return The number of concepts deleted. This includes the concept and all its descendants
     */
    def delete(conceptDelete: ConceptDelete): Either[Throwable, Int] =

        // -- Helper function to delete a concept
        def txn(userEntity: UserAccountEntity, name: String): Either[Throwable, Int] =
            if !userEntity.isAdministrator then
                throw AccessDenied(userEntity.getUserName)

            handleByConceptName(name, (concept, repo) =>
                val history = HistoryEntityFactory.delete(userEntity, concept)
                history.approveBy(userEntity.getUserName)

                concept.getParentConcept match
                    case null =>
                        repo.deleteBranchByName(name)
                    case parent =>
                        parent.getConceptMetadata.addHistory(history)
                        repo.deleteBranchByName(name)

            )

        for
            user <- userAccountService.verifyWriteAccess(conceptDelete.userName)
            count <- txn(user.toEntity, conceptDelete.name)
        yield
            count
