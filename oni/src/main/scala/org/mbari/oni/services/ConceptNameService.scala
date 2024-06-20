/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2024
 *
 * oni code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential. 
 */

package org.mbari.oni.services

import jakarta.persistence.{EntityManager, EntityManagerFactory}
import org.mbari.oni.{ConceptNameAlreadyExists, ConceptNameNotFound}
import org.mbari.oni.domain.{
    ConceptMetadata,
    ConceptNameCreate,
    ConceptNameTypes,
    ConceptNameUpdate,
    RawConcept,
    RawConceptName
}
import org.mbari.oni.jpa.EntityManagerFactories.*
import org.mbari.oni.jpa.entities.{ConceptNameEntity, HistoryEntity, HistoryEntityFactory, UserAccountEntity}
import org.mbari.oni.jpa.repositories.{ConceptNameRepository, ConceptRepository}

import scala.jdk.CollectionConverters.*
import scala.jdk.OptionConverters.*

trait ConceptNameServiceBase:

    def findAllNames(limit: Int, offset: Int): Either[Throwable, Seq[String]]

    def addName(dto: ConceptNameCreate, userName: String): Either[Throwable, RawConcept]

    def updateName(name: String, dto: ConceptNameUpdate, userName: String): Either[Throwable, RawConcept]

    def deleteName(name: String, userName: String): Either[Throwable, RawConcept]

class ConceptNameService(entityManagerFactory: EntityManagerFactory) extends ConceptNameServiceBase:

    private val log                = System.getLogger(getClass.getName)
    private val userAccountService = UserAccountService(entityManagerFactory)

    def findAllNames(limit: Int, offset: Int): Either[Throwable, Seq[String]] =
        entityManagerFactory.transaction(entityManger =>
            val repo = new ConceptNameRepository(entityManger)
            repo.findAllNamesAsStrings().asScala.toSeq
        )

    def addName(dto: ConceptNameCreate, userName: String): Either[Throwable, RawConcept] =

        // History is OK
        def txn(userEntity: UserAccountEntity): Either[Throwable, RawConcept] =
            entityManagerFactory.transaction(entityManager =>
                val repo = new ConceptNameRepository(entityManager)

                // Make sure the old name exists
                val conceptNameOpt = repo.findByName(dto.name).toScala
                if conceptNameOpt.isEmpty then throw ConceptNameNotFound(dto.name)

                // Mae sure the new name does not exist
                val newConceptNameOpt = repo.findByName(dto.newName).toScala
                if newConceptNameOpt.isDefined then throw ConceptNameAlreadyExists(dto.newName)

                val oldConceptName = conceptNameOpt.get
                val concept        = oldConceptName.getConcept
                val newNameType    = ConceptNameTypes.fromString(dto.nameType)
                if newNameType == ConceptNameTypes.PRIMARY then
                    oldConceptName
                        .getConcept
                        .getPrimaryConceptName
                        .setNameType(ConceptNameTypes.FORMER)

                val newConceptName = dto.toEntity
                concept.addConceptName(newConceptName)

                val history = HistoryEntityFactory.add(userEntity, newConceptName)
                if newNameType == ConceptNameTypes.PRIMARY then history.setOldValue(oldConceptName.getName)
                concept.getConceptMetadata.addHistory(history)

                if userEntity.isAdministrator then history.approveBy(userEntity.getUserName)

                RawConcept.from(concept, false)
            )

        for
            user    <- userAccountService.verifyWriteAccess(Option(userName))
            concept <- txn(user.toEntity)
        yield concept

    def updateName(name: String, dto: ConceptNameUpdate, userName: String): Either[Throwable, RawConcept] =
        def txn(userEntity: UserAccountEntity): Either[Throwable, RawConcept] =
            entityManagerFactory.transaction(entityManager =>
                val repo = new ConceptNameRepository(entityManager)

                // Make sure the old name exists
                val conceptNameOpt      = repo.findByName(name).toScala
                if conceptNameOpt.isEmpty then throw ConceptNameNotFound(name)
                val existingConceptName = conceptNameOpt.get // EXISTING NAME
                val existingNameType    = ConceptNameTypes.fromString(existingConceptName.getNameType)

                // Mae sure the new name does not exist
                dto.newName match
                    case Some(newName) =>
                        val newConceptNameOpt = repo.findByName(newName).toScala
                        if newConceptNameOpt.isDefined then throw ConceptNameAlreadyExists(newName)
                        val history           = HistoryEntityFactory.replaceConceptName(userEntity, name, newName)
                        existingConceptName.getConcept.getConceptMetadata.addHistory(history)
                        if userEntity.isAdministrator then history.approveBy(userEntity.getUserName)
                    case None          => ()

                // If the name type is changing, make sure the primary name is not being changed
                dto.nameType match
                    case Some(nameType) =>
                        val newNameType = ConceptNameTypes.fromString(nameType)
                        if newNameType == null then
                            throw new IllegalArgumentException(
                                s"Invalid name type: $nameType. Must be one of ${ConceptNameTypes.values.mkString(", ")}"
                            )
                        if existingNameType == ConceptNameTypes.PRIMARY && newNameType != ConceptNameTypes.PRIMARY then
                            throw new IllegalArgumentException(
                                "Attempting to change a primary name to a non-primary name. Use add name instead."
                            )
                    case None           => ()

                dto.updateEntity(existingConceptName)

                RawConcept.from(existingConceptName.getConcept, false)
            )

        for
            user    <- userAccountService.verifyWriteAccess(Option(userName))
            concept <- txn(user.toEntity)
        yield concept

    def deleteName(name: String, userName: String): Either[Throwable, RawConcept] =
        def txn(userEntity: UserAccountEntity): Either[Throwable, RawConcept] =
            entityManagerFactory.transaction(entityManager =>
                val repo = new ConceptNameRepository(entityManager)

                // Make sure the old name exists
                val conceptNameOpt = repo.findByName(name).toScala
                if conceptNameOpt.isEmpty then throw ConceptNameNotFound(name)

                val conceptName = conceptNameOpt.get
                if conceptName.getNameType.equalsIgnoreCase(ConceptNameTypes.PRIMARY.getType) then
                    throw new IllegalArgumentException("Cannot delete a primary name. Use add name instead.")

                val concept = conceptName.getConcept
                val history = HistoryEntityFactory.delete(userEntity, conceptName)
                concept.getConceptMetadata.addHistory(history)

                // Only remove the concept name if the user is an admin
                if userEntity.isAdministrator then
                    history.approveBy(userEntity.getUserName)
                    concept.removeConceptName(conceptName)
                    entityManager.remove(conceptName)

                RawConcept.from(concept, false)
            )

        for
            user    <- userAccountService.verifyWriteAccess(Some(userName))
            concept <- txn(user.toEntity)
        yield concept

    def inTxnRejectAddConceptName(
        historyEntity: HistoryEntity,
        userEntity: UserAccountEntity,
        entityManager: EntityManager
    ): Either[Throwable, Boolean] =
        val concept = historyEntity.getConceptMetadata.getConcept
        Option(concept.getConceptName(historyEntity.getNewValue)) match
            case None              => Left(ConceptNameNotFound(historyEntity.getNewValue))
            case Some(conceptName) =>
                if conceptName.getNameType.equalsIgnoreCase(ConceptNameTypes.PRIMARY.getType) then
                    // Find the old primary name
                    Option(concept.getConceptName(historyEntity.getOldValue))
                        .foreach(oldPrimaryName => concept.removeConceptName(oldPrimaryName))
                    // set the concept name to the old primary name
                    conceptName.setName(historyEntity.getOldValue)
                else concept.removeConceptName(conceptName)
                Right(true)

    def inTxnApproveDelete(
        historyEntity: HistoryEntity,
        userEntity: UserAccountEntity,
        entityManager: EntityManager
    ): Either[Throwable, Boolean] =
        val concept = historyEntity.getConceptMetadata.getConcept
        Option(concept.getConceptName(historyEntity.getOldValue)) match
            case None              => Left(ConceptNameNotFound(historyEntity.getOldValue))
            case Some(conceptName) =>
                val repo = ConceptRepository(entityManager)
                concept.removeConceptName(conceptName)
                repo.delete(conceptName)
                Right(true)
