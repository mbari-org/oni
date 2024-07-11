/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2024
 *
 * oni code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential.
 */

package org.mbari.oni.services

import jakarta.persistence.EntityManagerFactory
import org.mbari.oni.domain.PrefNode
import org.mbari.oni.jpa.EntityManagerFactories.*
import org.mbari.oni.jpa.entities.PreferenceNodeEntity
import org.mbari.oni.jpa.repositories.{PrefNodeRepository, VarsUserPreferencesFactory}
import scala.jdk.OptionConverters.*
import scala.jdk.CollectionConverters.*

class PrefNodeService(entityManagerFactory: EntityManagerFactory):

    def create(prefNode: PrefNode): Either[Throwable, PrefNode] = create(prefNode.name, prefNode.key, prefNode.value)

    def create(name: String, key: String, value: String): Either[Throwable, PrefNode] =
        val entity = new PreferenceNodeEntity()
        entity.setNodeName(name)
        entity.setPrefKey(key)
        entity.setPrefValue(value)
        entityManagerFactory.transaction(entityManager =>
            val repo = new PrefNodeRepository(entityManager)
            repo.create(entity)
            PrefNode.from(entity)
        )

    def update(prefNode: PrefNode): Either[Throwable, PrefNode] = update(prefNode.name, prefNode.key, prefNode.value)

    def update(name: String, key: String, value: String): Either[Throwable, PrefNode] =
        entityManagerFactory.transaction(entityManager =>
            val repo = new PrefNodeRepository(entityManager)
            repo.findByNodeNameAndPrefKey(name, key).toScala match
                case Some(entity) =>
                    entity.setPrefValue(value)
                    repo.update(entity)
                    PrefNode.from(entity)
                case None         =>
                    throw new IllegalArgumentException(s"PrefNode with name ${name} and key ${key} does not exist")
        )

    def delete(name: String, key: String): Either[Throwable, Unit] =
        entityManagerFactory.transaction(entityManager =>
            val repo = new PrefNodeRepository(entityManager)
            repo.findByNodeNameAndPrefKey(name, key).toScala match
                case Some(entity) => repo.delete(entity)
                case None         =>
                    throw new IllegalArgumentException(s"PrefNode with name ${name} and key ${key} does not exist")
        )

    def findByNodeNameAndKey(name: String, key: String): Either[Throwable, Option[PrefNode]] =
        entityManagerFactory.transaction(entityManager =>
            val repo = new PrefNodeRepository(entityManager)
            repo.findByNodeNameAndPrefKey(name, key).map(PrefNode.from).toScala
        )

    def findByNodeName(name: String): Either[Throwable, Seq[PrefNode]] =
        val attempt = entityManagerFactory.transaction(entityManager =>
            val repo = new PrefNodeRepository(entityManager)
            repo.findByNodeName(name)
        )
        attempt.map(_.asScala.map(PrefNode.from).toSeq)

    def findByNodeNameLike(name: String): Either[Throwable, Seq[PrefNode]] =
        val attempt = entityManagerFactory.transaction(entityManager =>
            val repo = new PrefNodeRepository(entityManager)
            repo.findByNodeNameLike(name)
        )
        attempt.map(_.asScala.map(PrefNode.from).toSeq)

    def findAll(limit: Int = 100, offset: Int = 0): Either[Throwable, Seq[PrefNode]] =
        val attempt = entityManagerFactory.transaction(entityManager =>
            val repo = new PrefNodeRepository(entityManager)
            repo.findAll(limit, offset)
        )
        attempt.map(_.asScala
                .map(PrefNode.from)
                .toSeq)
