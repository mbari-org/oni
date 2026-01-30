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
import org.mbari.oni.domain.{Media, MediaCreate, MediaUpdate}
import org.mbari.oni.jdbc.FastPhylogenyService
import org.mbari.oni.jpa.EntityManagerFactories.*
import org.mbari.oni.jpa.entities.{HistoryEntity, HistoryEntityFactory, MediaEntity, UserAccountEntity}
import org.mbari.oni.jpa.repositories.{ConceptRepository, MediaRepository}
import org.mbari.oni.{ConceptNameNotFound, ItemNotFound}

import scala.jdk.CollectionConverters.*
import scala.jdk.OptionConverters.*

class MediaService(entityManagerFactory: EntityManagerFactory, fastPhylogenyService: FastPhylogenyService):

    private val log                = System.getLogger(getClass.getName)
    private val userAccountService = UserAccountService(entityManagerFactory)

    /**
     * Clear primary flag on all media of the given type for a concept, except the specified media entity.
     * @param conceptMetadata The concept metadata containing the media
     * @param mediaType The type of media (Image, Video, Icon)
     * @param exceptMedia The media entity to exclude from clearing (can be null)
     */
    private def clearPrimaryForType(
        conceptMetadata: org.mbari.oni.jpa.entities.ConceptMetadataEntity,
        mediaType: String,
        exceptMedia: MediaEntity
    ): Unit =
        conceptMetadata.getMedias
            .stream()
            .filter(m => m.getType == mediaType && m.getUrl != exceptMedia.getUrl && m.isPrimary)
            .forEach(m => m.setPrimary(false))

    /**
     * Check if this is the only media of its type on the concept.
     * @param conceptMetadata The concept metadata containing the media
     * @param mediaType The type of media to check
     * @return true if there is exactly one media of this type
     */
    private def isOnlyMediaOfType(
        conceptMetadata: org.mbari.oni.jpa.entities.ConceptMetadataEntity,
        mediaType: String
    ): Boolean =
        conceptMetadata.getMedias
            .stream()
            .filter(m => m.getType == mediaType)
            .count() == 1

    /**
     * Find the most recently added media of the given type on the concept.
     * Uses the last updated timestamp as a proxy for creation time.
     * @param conceptMetadata The concept metadata containing the media
     * @param mediaType The type of media to find
     * @param exceptMedia The media entity to exclude from the search
     * @return The most recently added media of the given type, or None if not found
     */
    private def findMostRecentMediaOfType(
        conceptMetadata: org.mbari.oni.jpa.entities.ConceptMetadataEntity,
        mediaType: String,
        exceptMedia: MediaEntity
    ): Option[MediaEntity] =
        import java.util.Comparator
        conceptMetadata.getMedias
            .stream()
            .filter(m => m.getType == mediaType && m.getUrl != exceptMedia.getUrl)
            .max(Comparator.comparing[MediaEntity, java.time.Instant](
                m => Option(m.getLastUpdatedTimestamp).getOrElse(java.time.Instant.EPOCH),
                Comparator.naturalOrder()
            ))
            .toScala

    def findById(id: Long): Either[Throwable, Option[Media]] =
        entityManagerFactory.readOnlyTransaction(entityManager =>
            val repo = MediaRepository(entityManager, fastPhylogenyService)
            repo.findByPrimaryKey(classOf[MediaEntity], id)
                .map(Media.from)
                .toScala
        )

    def findRepresentativeMedia(concept: String, count: Int): Either[Throwable, Seq[Media]] =
        entityManagerFactory.readOnlyTransaction(entityManager =>
            val repo = MediaRepository(entityManager, fastPhylogenyService)
            repo.findRepresentativeMedia(concept, count)
                .asScala
                .toSeq
                .map(Media.from)
        )

    def create(mediaCreate: MediaCreate, userName: String): Either[Throwable, Media] =
        def txn(userEntity: UserAccountEntity): Either[Throwable, Media] =
            entityManagerFactory.transaction(entityManager =>
                val repo        = MediaRepository(entityManager, fastPhylogenyService)
                val conceptRepo = new ConceptRepository(entityManager)
                conceptRepo.findByName(mediaCreate.conceptName).toScala match
                    case Some(concept) =>
                        // Check for existing URL
                        val url      = mediaCreate.url.toExternalForm
                        val existing = concept
                            .getConceptMetadata
                            .getMedias
                            .stream()
                            .filter(_.getUrl == url)
                            .findAny()

                        if existing.isPresent then
                            throw new IllegalArgumentException(
                                s"Media with URL '${url}' already exists for concept '${mediaCreate.conceptName}'"
                            )

                        val entity          = mediaCreate.toEntity
                        val conceptMetadata = concept.getConceptMetadata
                        conceptMetadata.addMedia(entity)
                        repo.create(entity)

                        // Handle primary media logic
                        val mediaType = entity.getType
                        if entity.isPrimary then
                            // If explicitly set as primary, clear other primaries of same type
                            clearPrimaryForType(conceptMetadata, mediaType, entity)
                        else if isOnlyMediaOfType(conceptMetadata, mediaType) then
                            // If this is the only media of its type, make it primary
                            entity.setPrimary(true)

                        // Add history
                        val history = HistoryEntityFactory.add(userEntity, entity)
                        conceptMetadata.addHistory(history)
                        if userEntity.isAdministrator then history.approveBy(userEntity.getUserName)

                        Media.from(entity)

                    case None => throw ConceptNameNotFound(mediaCreate.conceptName)
            )

        for
            user  <- userAccountService.verifyWriteAccess(Option(userName))
            media <- txn(user.toEntity)
        yield media

    def deleteById(id: Long, userName: String): Either[Throwable, Unit] =
        def txn(userEntity: UserAccountEntity): Either[Throwable, Unit] =
            entityManagerFactory.transaction(entityManager =>
                val repo = MediaRepository(entityManager, fastPhylogenyService)
                repo.findByPrimaryKey(classOf[MediaEntity], id).toScala match
                    case None        => throw ItemNotFound(s"Media with id '${id}' not found")
                    case Some(media) =>
                        val history = HistoryEntityFactory.delete(userEntity, media)
                        media.getConceptMetadata.addHistory(history)
                        if userEntity.isAdministrator then
                            history.approveBy(userEntity.getUserName)
                            val conceptMetadata = media.getConceptMetadata
                            val wasPrimary      = media.isPrimary
                            val mediaType       = media.getType
                            conceptMetadata.removeMedia(media)
                            repo.delete(media)

                            // If deleted media was primary, promote the most recent media of the same type
                            if wasPrimary then
                                findMostRecentMediaOfType(conceptMetadata, mediaType, media)
                                    .foreach(_.setPrimary(true))
            )

        for
            user  <- userAccountService.verifyWriteAccess(Option(userName))
            media <- txn(user.toEntity)
        yield ()

    def update(id: Long, mediaUpdate: MediaUpdate, userName: String): Either[Throwable, Media] =
        def txn(userEntity: UserAccountEntity): Either[Throwable, Media] =
            entityManagerFactory.transaction(entityManager =>
                val repo = MediaRepository(entityManager, fastPhylogenyService)
                repo.findByPrimaryKey(classOf[MediaEntity], id).toScala match
                    case None        => throw ItemNotFound(s"Media with id '${id}' not found")
                    case Some(media) =>
                        mediaUpdate.caption.foreach(media.setCaption)
                        mediaUpdate.credit.foreach(media.setCredit)
                        mediaUpdate.url.foreach(url => media.setUrl(url.toExternalForm))
                        mediaUpdate.mediaType.foreach(media.setType)

                        // Handle primary media logic - if setting as primary, clear other primaries of same type
                        mediaUpdate.isPrimary.foreach { isPrimary =>
                            media.setPrimary(isPrimary)
                            if isPrimary then
                                clearPrimaryForType(media.getConceptMetadata, media.getType, media)
                        }

                        repo.update(media)
                        Media.from(media)
            )

        for
            user  <- userAccountService.verifyWriteAccess(Option(userName))
            media <- txn(user.toEntity)
        yield media

    def inTxnRejectAdd(
        history: HistoryEntity,
        user: UserAccountEntity,
        entityManger: EntityManager
    ): Either[Throwable, Boolean] =
        val conceptMetadata = history.getConceptMetadata
        val concept         = conceptMetadata.getConcept
        val opt             = conceptMetadata
            .getMedias
            .stream()
            .filter(e => e.getUrl == history.getNewValue)
            .findFirst()
            .toScala

        opt match
            case None    =>
                Left(ItemNotFound(s"${concept.getName} does not have a media with URL of ${history.getNewValue}"))
            case Some(m) =>
                val wasPrimary = m.isPrimary
                val mediaType  = m.getType
                conceptMetadata.removeMedia(m)
                entityManger.remove(m)

                // If deleted media was primary, promote the most recent media of the same type
                if wasPrimary then
                    findMostRecentMediaOfType(conceptMetadata, mediaType, m)
                        .foreach(_.setPrimary(true))

                Right(true)

    def inTxnApproveDelete(
        history: HistoryEntity,
        user: UserAccountEntity,
        entityManger: EntityManager
    ): Either[Throwable, Boolean] =
        val conceptMetadata = history.getConceptMetadata
        val concept         = conceptMetadata.getConcept
        val opt             = conceptMetadata
            .getMedias
            .stream()
            .filter(e => e.getUrl == history.getOldValue)
            .findFirst()
            .toScala

        opt match
            case None    =>
                Left(ItemNotFound(s"${concept.getName} does not have a media with URL of ${history.getOldValue}"))
            case Some(m) =>
                val wasPrimary = m.isPrimary
                val mediaType  = m.getType
                conceptMetadata.removeMedia(m)
                entityManger.remove(m)

                // If deleted media was primary, promote the most recent media of the same type
                if wasPrimary then
                    findMostRecentMediaOfType(conceptMetadata, mediaType, m)
                        .foreach(_.setPrimary(true))
                        
                Right(true)
