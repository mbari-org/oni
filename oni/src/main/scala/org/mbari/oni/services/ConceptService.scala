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
import org.mbari.oni.domain.{ConceptCreate, ConceptMetadata, ConceptUpdate, RawConcept}
import org.mbari.oni.etc.jdk.Loggers.given
import org.mbari.oni.jpa.EntityManagerFactories.*
import org.mbari.oni.jpa.entities.{
    ConceptEntity,
    ConceptNameEntity,
    HistoryEntity,
    HistoryEntityFactory,
    UserAccountEntity
}
import org.mbari.oni.jpa.repositories.ConceptRepository
import org.mbari.oni.{
    AccessDenied,
    ChildConceptNotFound,
    ConceptNameAlreadyExists,
    ConceptNameNotFound,
    HistoryIsInvalid,
    MissingRootConcept,
    ParentConceptNotFound,
    RootAlreadyExists
}

import scala.jdk.CollectionConverters.*
import scala.jdk.OptionConverters.*

class ConceptService(entityManagerFactory: EntityManagerFactory):

    private val log                = System.getLogger(getClass.getName)
//    private val historyService     = HistoryService(entityManagerFactory)
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
        handleByConceptNameQuery(
            name,
            c =>
                if c.getParentConcept == null then throw ParentConceptNotFound(name)
                else ConceptMetadata.from(c.getParentConcept)
        )

    def findChildrenByParentName(name: String): Either[Throwable, Set[ConceptMetadata]] =
        handleByConceptNameQuery(name, c => c.getChildConcepts.asScala.map(ConceptMetadata.from).toSet)

    def findRoot(): Either[Throwable, ConceptMetadata] =
        entityManagerFactory.readOnlyTransaction(entityManager =>
            val repo = new ConceptRepository(entityManager)
            repo.findRoot().toScala match
                case None    => throw MissingRootConcept
                case Some(c) => ConceptMetadata.from(c)
        )

    def findByGlob(glob: String): Either[Throwable, Set[ConceptMetadata]] =
        entityManagerFactory.readOnlyTransaction(entityManager =>
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
        entityManagerFactory.readOnlyTransaction(entityManager =>
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
        val grandChildren   = new java.util.HashSet(child.getChildConcepts)
        grandChildren.forEach(c => child.removeChildConcept(c))
        val grandChildNames = grandChildren.asScala.map(_.getPrimaryConceptName.getName).mkString(", ")

        child.setId(null)

        entityManagerFactory.transaction(entityManager =>
            log.atDebug.log(s"Inserting ${child.getPrimaryConceptName.getName} which has children: $grandChildNames")

            if parent != null then
                entityManager.find(classOf[ConceptEntity], parent.getId) match
                    case null =>
                        throw new RuntimeException(
                            s"Parent ${parent.getPrimaryConceptName.getName} with id=${parent.getId} not found"
                        )
                    case p    => p.addChildConcept(child)
            else entityManager.persist(child)
        )

        grandChildren.forEach(cascadeInsert(child, _))

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
        entityManagerFactory.readOnlyTransaction(entityManager =>
            val repo = new ConceptRepository(entityManager)
            repo.findByName(name).toScala match
                case None    => throw ConceptNameNotFound(name)
                case Some(c) => fn2(c, repo)
        )

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
    def create(conceptCreate: ConceptCreate, userName: String): Either[Throwable, ConceptMetadata] =

        // -- Helper function to build the concept and history in a transaction
        def buildInTxn(
            userEntity: UserAccountEntity,
            parent: ConceptEntity,
            entityManager: EntityManager
        ): ConceptEntity =

            // build concept
            val concept     = new ConceptEntity()
            conceptCreate.aphiaId.foreach(v => concept.setAphiaId(v.longValue()))
            conceptCreate.rankLevel.foreach(v => concept.setRankLevel(v))
            conceptCreate.rankName.foreach(v => concept.setRankName(v))
            val conceptName = new ConceptNameEntity(conceptCreate.name)
            concept.addConceptName(conceptName)
            conceptCreate.author.foreach(conceptName.setAuthor)
            if parent != null then
                parent.addChildConcept(concept)

                // build history
                val history = HistoryEntityFactory.add(userEntity, concept)
                parent.getConceptMetadata.addHistory(history)
                if userEntity.isAdministrator then history.approveBy(userEntity.getUserName)
            else
                findRoot() match
                    case Left(_)  => entityManager.persist(concept)
                    case Right(_) =>
                        throw new IllegalArgumentException(
                            "Root concept already exists. Cannot create a new root concept"
                        )
            concept

        // -- Helper function to run the transaction
        def txn(userEntity: UserAccountEntity): Either[Throwable, ConceptMetadata] =
            entityManagerFactory.transaction(entityManager =>
                val repo = new ConceptRepository(entityManager)
                repo.findByName(conceptCreate.name).toScala match
                    case Some(_) => throw ConceptNameAlreadyExists(conceptCreate.name)
                    case None    =>
                        RankValidator.throwExceptionIfInvalid(conceptCreate)
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
            user    <- userAccountService.verifyWriteAccess(Some(userName))
            concept <- txn(user.toEntity)
            c       <- findByName(conceptCreate.name)
        yield c

    /**
     * Update a concept. This service does not check the users access credentials, that is done by the web service
     * layer.
     * @param conceptUpdate
     *   The update data
     * @return
     *   The updated concept or an exception if any errors occur
     */
    def update(name: String, conceptUpdate: ConceptUpdate, userName: String): Either[Throwable, ConceptMetadata] =

        // -- Helper function to update the parent concept
        def updateParent(userEntity: UserAccountEntity, conceptEntity: ConceptEntity, parentName: Option[String])(using
            repo: ConceptRepository
        ): Unit =
            parentName.foreach(name =>
                // Don't allow the root concept to have a parent
                if !conceptEntity.hasParent then
                    throw new IllegalArgumentException(s"Cannot set the parent of the root concept!")

                // Don't allow cyclic relation
                if conceptEntity.hasDescendent(name) then
                    throw new IllegalArgumentException(
                        s"Cannot set parent ($name) to a descendant of the concept (${conceptEntity.getPrimaryConceptName.getName}). This would create a cyclic relation"
                    )

//                println(s"Updating $name to parent ${conceptEntity.getPrimaryConceptName.getName} with descendants ${conceptEntity.getDescendants.asScala.map(_.getPrimaryConceptName.getName).mkString(", ")}")

                repo.findByName(name).toScala match
                    case None               => throw ConceptNameNotFound(name)
                    case Some(parentEntity) =>
                        // Only update if the parent is different
                        if !conceptEntity.getParentConcept.hasConceptName(name) then
                            val history =
                                HistoryEntityFactory.replaceParentConcept(
                                    userEntity,
                                    conceptEntity.getParentConcept,
                                    parentEntity
                                )
                            conceptEntity.getConceptMetadata.addHistory(history)
                            if userEntity.isAdministrator then history.approveBy(userEntity.getUserName)
                            conceptEntity.getParentConcept match
                                case null   =>
                                    parentEntity.addChildConcept(conceptEntity)
                                case parent =>
                                    parent.removeChildConcept(conceptEntity)
                                    parentEntity.addChildConcept(conceptEntity)
            )

        def updateRank(
            userEntity: UserAccountEntity,
            conceptEntity: ConceptEntity,
            rankLevel: Option[String],
            rankName: Option[String]
        ): Unit =

            val historyOpt = (rankLevel, rankName) match
                case (None, None)        => None // No update
                case (Some(level), None) =>
                    if level != conceptEntity.getRankLevel then
                        val correctedLevel = if level.isBlank then null else level
                        val oldRankLevel   = Option(conceptEntity.getRankLevel).getOrElse("")
                        conceptEntity.setRankLevel(correctedLevel)
                        Some(
                            HistoryEntityFactory.replaceRank(
                                userEntity,
                                oldRankLevel,
                                conceptEntity.getRankName,
                                level,
                                conceptEntity.getRankName
                            )
                        )
                    else None

                case (None, Some(name))        =>
                    if name != conceptEntity.getRankName then
                        val correctedName = if name.isBlank then null else name
                        val oldRankName   = Option(conceptEntity.getRankName).getOrElse("")
                        conceptEntity.setRankName(correctedName)
                        Some(
                            HistoryEntityFactory.replaceRank(
                                userEntity,
                                conceptEntity.getRankLevel,
                                oldRankName,
                                conceptEntity.getRankLevel,
                                name
                            )
                        )
                    else None
                case (Some(level), Some(name)) =>
                    if level != conceptEntity.getRankLevel || name != conceptEntity.getRankName then
                        val correctedLevel = if level.isBlank then null else level
                        val correctedName  = if name.isBlank then null else name
                        val oldRankLevel   = Option(conceptEntity.getRankLevel).getOrElse("")
                        val oldRankName    = Option(conceptEntity.getRankName).getOrElse("")
                        conceptEntity.setRankLevel(correctedLevel)
                        conceptEntity.setRankName(correctedName)
                        Some(HistoryEntityFactory.replaceRank(userEntity, oldRankLevel, oldRankName, level, name))
                    else None

            historyOpt.foreach(history =>
                conceptEntity.getConceptMetadata.addHistory(history)
                if userEntity.isAdministrator then history.approveBy(userEntity.getUserName)
            )

        // -- Helper function to update the rank level
//        def updateRankLevel(
//            userEntity: UserAccountEntity,
//            conceptEntity: ConceptEntity,
//            rankLevel: Option[String]
//        ): Unit =
//            rankLevel.foreach(v =>
//                if v != conceptEntity.getRankLevel then
//                    if v.isBlank then
//                        if userEntity.isAdministrator then conceptEntity.setRankLevel(null)
//                        else throw new IllegalArgumentException("Rank level can only be removed by an administrator")
//                    else
//                        val history = HistoryEntityFactory.replaceRankLevel(userEntity, conceptEntity.getRankLevel, v)
//                        conceptEntity.getConceptMetadata.addHistory(history)
//                        if userEntity.isAdministrator then history.approveBy(userEntity.getUserName)
//                        conceptEntity.setRankLevel(v)
//            )

        // -- Helper function to update the rank name
//        def updateRankName(
//            userEntity: UserAccountEntity,
//            conceptEntity: ConceptEntity,
//            rankLevel: Option[String]
//        ): Unit =
//            rankLevel.foreach(v =>
//                if v != conceptEntity.getRankName then
//                    if v.isBlank then
//                        if userEntity.isAdministrator then conceptEntity.setRankName(null)
//                        else throw new IllegalArgumentException("Rank name can only be removed by an administrator")
//                    else
//                        val history = HistoryEntityFactory.replaceRankName(userEntity, conceptEntity.getRankName, v)
//                        conceptEntity.getConceptMetadata.addHistory(history)
//                        if userEntity.isAdministrator then history.approveBy(userEntity.getUserName)
//                        conceptEntity.setRankName(v)
//            )

        // -- Helper function to update the aphia id
        def updateAphiaId(conceptEntity: ConceptEntity, aphiaId: Option[Long])(using repo: ConceptRepository): Unit =
            aphiaId.foreach(v =>
                if v != conceptEntity.getAphiaId then
                    repo.findByAphiaId(v).toScala match
                        case Some(v) =>
                            if v.getId != conceptEntity.getId then
                                throw new IllegalArgumentException(
                                    s"AphiaId $v already exists for " + v.getPrimaryConceptName.getName
                                )
                        case None    =>
                            conceptEntity.setAphiaId(v)
            )

        def txn(userEntity: UserAccountEntity): Either[Throwable, ConceptMetadata] =
            entityManagerFactory.transaction(entityManager =>
                given repo: ConceptRepository = new ConceptRepository(entityManager)
                repo.findByName(name).toScala match
                    case None                => throw ConceptNameNotFound(name)
                    case Some(conceptEntity) =>
                        updateParent(userEntity, conceptEntity, conceptUpdate.parentName)
                        updateRank(userEntity, conceptEntity, conceptUpdate.rankLevel, conceptUpdate.rankName)
                        RankValidator.throwExceptionIfInvalid(conceptEntity.getRank)
                        updateAphiaId(conceptEntity, conceptUpdate.aphiaId)
                        ConceptMetadata.from(conceptEntity)
            )

        for
            user    <- userAccountService.verifyWriteAccess(Option(userName))
            concept <- txn(user.toEntity)
        yield concept

    /**
     * Delete a concept and all its descendants. This service does not check the users access credentials, that is done
     * by the web service layer. Only administrators can delete concepts. This is a non recoverable operation.
     * @param conceptDelete
     *   Information about the concept to delete
     * @return
     *   The number of concepts deleted. This includes the concept and all its descendants
     */
    def delete(conceptName: String, userName: String): Either[Throwable, Int] =

        // -- Helper function to delete a concept
        def txn(userEntity: UserAccountEntity, name: String): Either[Throwable, Int] =
            if userEntity.isReadOnly then throw AccessDenied(userEntity.getUserName)

            handleByConceptName(
                name,
                (concept, repo) =>
                    val history = HistoryEntityFactory.delete(userEntity, concept)

                    concept.getParentConcept match
                        case null   => // do nothing
                        case parent =>
                            parent.getConceptMetadata.addHistory(history)

                    // Only delete if the user is an admin. Otherwise we just add a history
                    if userEntity.isAdministrator then
                        history.approveBy(userEntity.getUserName)
                        repo.deleteBranchByName(concept.getPrimaryConceptName.getName)
                    else 0
            )

        for
            user  <- userAccountService.verifyWriteAccess(Option(userName))
            count <- txn(user.toEntity, conceptName)
        yield count

    def inTxnRejectAddChildHistory(
        historyEntity: HistoryEntity,
        userEntity: UserAccountEntity,
        entityManager: EntityManager
    ): Either[Throwable, Boolean] =
        try
            val repo          = ConceptRepository(entityManager)
            val parentConcept = historyEntity.getConceptMetadata.getConcept
            val childConcept  = parentConcept
                .getChildConcepts
                .stream()
                .filter(c => c.getPrimaryConceptName.getName == historyEntity.getNewValue)
                .findFirst()
                .toScala

            childConcept match
                case None        => throw ChildConceptNotFound(parentConcept.getName, historyEntity.getNewValue)
                case Some(child) =>
                    repo.deleteBranchByName(child.getName)
                    Right(true)
        catch case e: Throwable => Left(e)

    def inTxnApproveDelete(
        historyEntity: HistoryEntity,
        userEntity: UserAccountEntity,
        entityManager: EntityManager
    ): Either[Throwable, Boolean] =
        try
            val repo          = ConceptRepository(entityManager)
            val parentConcept = historyEntity.getConceptMetadata.getConcept
            val childConcept  = parentConcept
                .getChildConcepts
                .stream()
                .filter(c => c.getName == historyEntity.getOldValue)
                .findFirst()
                .toScala

            childConcept match
                case None        => throw ChildConceptNotFound(parentConcept.getName, historyEntity.getOldValue)
                case Some(child) =>
                    val n = repo.deleteBranchByName(child.getName)
                    if n == 0 then
                        log.atWarn.log(s"Failed to delete child concept ${child.getName}. No nodes were deleted.")
                    Right(n > 0)
        catch case e: Throwable => Left(e)

    def inTxnRejectReplaceParent(
        historyEntity: HistoryEntity,
        userEntity: UserAccountEntity,
        entityManager: EntityManager
    ): Either[Throwable, Boolean] =
        try
            val repo    = ConceptRepository(entityManager)
            val concept = historyEntity.getConceptMetadata.getConcept
            // val parent = concept.getParentConcept

            val parent = repo.findByName(historyEntity.getNewValue).toScala match
                case None    => throw ConceptNameNotFound(historyEntity.getOldValue)
                case Some(p) => p

            val oldParentConcept = repo.findByName(historyEntity.getOldValue).toScala match
                case None    => throw ConceptNameNotFound(historyEntity.getOldValue)
                case Some(p) => p

            // println("CONCEPT:              " + concept + " " + concept.getName)
            // println("HISTORY ENTITY:       " + historyEntity)
            // println("PARENT:               " + parent + " " + parent.getName())
            // println("Parent Children:      " + parent.getChildConcepts.asScala)
            // println("OLD PARENT:           " + oldParentConcept + " " + oldParentConcept.getName)
            // println("Old Parent Children:  " + oldParentConcept.getChildConcepts.asScala)

            // IMPORTANT: the removeChildConcept and addChildConcept methods don't work in this context.
            // parent.removeChildConcept(concept)
            // oldParentConcept.addChildConcept(concept)

            // IMPORTANT: the setParentConcept method does not work in this context.
            parent.getChildConcepts().remove(concept)
            oldParentConcept.getChildConcepts().add(concept)
            concept.setParentConcept(oldParentConcept)

            entityManager.flush() // Ensure the changes are persisted

            // println("Old Parent Children:  " + oldParentConcept.getChildConcepts.asScala)
            // log.atInfo
            //     .log(
            //         s"Rejected replace parent. Moving ${concept.getName} from ${parent.getName} back to ${oldParentConcept.getName} "
            //     )
            Right(true)
        catch case e: Throwable => Left(e)

    def inTxnRejectReplaceRank(
        history: HistoryEntity,
        user: UserAccountEntity,
        entityManger: EntityManager
    ): Either[Throwable, Boolean] =
        if history.getAction.equals(HistoryEntity.ACTION_REPLACE) &&
            history.getField.equals(HistoryEntity.FIELD_CONCEPT_RANK)
        then
            val conceptMetadata = history.getConceptMetadata
            val concept         = conceptMetadata.getConcept
            val oldRank         = history.getOldValue
            val parts           = oldRank.split(" ")
            if parts.length == 1 then
                val rankName = parts(0).trim
                if rankName.isEmpty || rankName.isBlank then concept.setRankName(null)
                else concept.setRankName(rankName)
                concept.setRankLevel(null)
                Right(true)
            else if parts.length == 2 then
                val rankLevel = parts(0).trim
                val rankName  = parts(1).trim
                if rankLevel.isEmpty || rankLevel.isBlank then concept.setRankLevel(null)
                else concept.setRankLevel(rankLevel)
                if rankName.isEmpty || rankLevel.isBlank then concept.setRankName(null)
                else concept.setRankName(rankName)
                Right(true)
            else Left(HistoryIsInvalid(s"History does not contain a valid rank: $oldRank"))
        else Left(HistoryIsInvalid("History is not a replace rank history"))

//    def inTxnRejectReplaceRankLevel(
//        history: HistoryEntity,
//        user: UserAccountEntity,
//        entityManger: EntityManager
//    ): Either[Throwable, Boolean] =
//        if history.getAction.equals(HistoryEntity.ACTION_REPLACE) &&
//            history.getField.equals(HistoryEntity.FIELD_CONCEPT_RANKLEVEL)
//        then
//            val conceptMetadata = history.getConceptMetadata
//            val concept         = conceptMetadata.getConcept
//            val oldRankLevel    = history.getOldValue
//            if oldRankLevel.isEmpty || oldRankLevel.isBlank then concept.setRankLevel(null)
//            else concept.setRankLevel(oldRankLevel)
//            Right(true)
//        else Left(HistoryIsInvalid("History is not a replace rank level history"))
//
//    def inTxnRejectReplaceRankName(
//        history: HistoryEntity,
//        user: UserAccountEntity,
//        entityManger: EntityManager
//    ): Either[Throwable, Boolean] =
//        if history.getAction.equals(HistoryEntity.ACTION_REPLACE) &&
//            history.getField.equals(HistoryEntity.FIELD_CONCEPT_RANKNAME)
//        then
//            val conceptMetadata = history.getConceptMetadata
//            val concept         = conceptMetadata.getConcept
//            val oldRankName     = history.getOldValue
//            if oldRankName.isEmpty || oldRankName.isBlank then concept.setRankName(null)
//            else concept.setRankName(oldRankName)
//            Right(true)
//        else Left(HistoryIsInvalid("History is not a replace rank name history"))
