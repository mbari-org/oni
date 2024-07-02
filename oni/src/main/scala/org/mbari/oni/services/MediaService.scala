/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2024
 *
 * oni code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential.
 */

package org.mbari.oni.services

import jakarta.persistence.{EntityManager, EntityManagerFactory}
import org.mbari.oni.{ConceptNameNotFound, ItemNotFound}
import org.mbari.oni.domain.{Media, MediaCreate}
import org.mbari.oni.jpa.EntityManagerFactories.*
import org.mbari.oni.etc.jdk.Loggers.given
import org.mbari.oni.jdbc.FastPhylogenyService
import org.mbari.oni.jpa.entities.{HistoryEntity, HistoryEntityFactory, MediaEntity, UserAccountEntity}
import org.mbari.oni.jpa.repositories.{ConceptRepository, MediaRepository}

import java.net.URL
import scala.jdk.CollectionConverters.*
import scala.jdk.OptionConverters.*

class MediaService(entityManagerFactory: EntityManagerFactory, fastPhylogenyService: FastPhylogenyService):

    private val log                = System.getLogger(getClass.getName)
    private val userAccountService = UserAccountService(entityManagerFactory)

    def findById(id: Long): Either[Throwable, Option[Media]] =
        entityManagerFactory.transaction(entityManager =>
            val repo = MediaRepository(entityManager, fastPhylogenyService)
            repo.findByPrimaryKey(classOf[MediaEntity], id)
                .map(Media.from)
                .toScala
        )

    def findRepresentativeMedia(concept: String, count: Int): Either[Throwable, Seq[Media]] =
        entityManagerFactory.transaction(entityManager =>
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

                        val entity  = mediaCreate.toEntity
                        concept.getConceptMetadata.addMedia(entity)
                        // Add history
                        val history = HistoryEntityFactory.add(userEntity, entity)
                        concept.getConceptMetadata.addHistory(history)
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
                            conceptMetadata.removeMedia(media)
                            repo.delete(media)
            )

        for
            user  <- userAccountService.verifyWriteAccess(Option(userName))
            media <- txn(user.toEntity)
        yield ()

    def update() = ???

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
                conceptMetadata.removeMedia(m)
                entityManger.remove(m)
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
                conceptMetadata.removeMedia(m)
                entityManger.remove(m)
                Right(true)
