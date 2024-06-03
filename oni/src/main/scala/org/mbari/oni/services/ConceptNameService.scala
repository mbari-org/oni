/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2024
 *
 * oni code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential. 
 */

package org.mbari.oni.services

import jakarta.persistence.EntityManagerFactory
import org.mbari.oni.{ConceptNameAlreadyExists, ConceptNameNotFound}
import org.mbari.oni.domain.{ConceptMetadata, ConceptNameTypes, RawConcept, RawConceptName}
import org.mbari.oni.jpa.EntityManagerFactories.*
import org.mbari.oni.jpa.entities.ConceptNameEntity
import org.mbari.oni.jpa.repositories.{ConceptNameRepository, ConceptRepository}

import scala.jdk.CollectionConverters.*
import scala.jdk.OptionConverters.*

class ConceptNameService(entityManagerFactory: EntityManagerFactory):

    def findAllNames(): Either[Throwable, Seq[String]] =
        entityManagerFactory.transaction(entityManger =>
            val repo = new ConceptNameRepository(entityManger)
            repo.findAllNamesAsStrings().asScala.toSeq
        )

    def addName(name: String, newName: RawConceptName): Either[Throwable, RawConcept] =
        entityManagerFactory.transaction(entityManager =>
            val repo = new ConceptNameRepository(entityManager)

            // Make sure the old name exists
            val conceptNameOpt = repo.findByName(name).toScala
            if (conceptNameOpt.isEmpty) {
                throw ConceptNameNotFound(name)
            }

            // Mae sure the new name does not exist
            val newConceptNameOpt = repo.findByName(newName.name).toScala
            if (newConceptNameOpt.isDefined) {
                throw ConceptNameAlreadyExists(newName.name)
            }

            val oldConceptName = conceptNameOpt.get
            val concept = oldConceptName.getConcept
            val newNameType = ConceptNameTypes.fromString(newName.nameType)
            if (newNameType == ConceptNameTypes.PRIMARY) {
                oldConceptName.getConcept
                    .getPrimaryConceptName
                    .setNameType(ConceptNameTypes.FORMER)
            }

            val newConceptName = newName.toEntity
            concept.addConceptName(newConceptName)
            RawConcept.from(concept, false)
        )

    def updateName(name: String, newName: RawConceptName): Either[Throwable, RawConcept] =
        entityManagerFactory.transaction(entityManager =>
            val repo = new ConceptNameRepository(entityManager)

            // Make sure the old name exists
            val conceptNameOpt = repo.findByName(name).toScala
            if (conceptNameOpt.isEmpty) {
                throw ConceptNameNotFound(name)
            }

            // Mae sure the new name does not exist
            val newConceptNameOpt = repo.findByName(newName.name).toScala
            if (newConceptNameOpt.isDefined) {
                throw ConceptNameAlreadyExists(newName.name)
            }

            val conceptName = conceptNameOpt.get
            if (conceptName.getNameType.equalsIgnoreCase(ConceptNameTypes.PRIMARY.getType) &&
                !newName.nameType.equalsIgnoreCase(ConceptNameTypes.PRIMARY.getType)) {
                throw new IllegalArgumentException("Attempting to change a primary name to a non-primary name. Use add name instead.")
            }

            conceptName.setName(newName.name)
            conceptName.setNameType(newName.nameType)
            newName.author.foreach(conceptName.setAuthor)

            RawConcept.from(conceptName.getConcept, false)
        )

    def deleteName(name: String): Either[Throwable, RawConcept] =
        entityManagerFactory.transaction(entityManager =>
            val repo = new ConceptNameRepository(entityManager)

            // Make sure the old name exists
            val conceptNameOpt = repo.findByName(name).toScala
            if (conceptNameOpt.isEmpty) {
                throw ConceptNameNotFound(name)
            }


            val conceptName = conceptNameOpt.get
            if (conceptName.getNameType.equalsIgnoreCase(ConceptNameTypes.PRIMARY.getType)) {
                throw new IllegalArgumentException("Cannot delete a primary name. Use add name instead.")
            }
            val concept = conceptName.getConcept
            concept.removeConceptName(conceptName)
            entityManager.remove(conceptName)
            RawConcept.from(concept, false)
        )


