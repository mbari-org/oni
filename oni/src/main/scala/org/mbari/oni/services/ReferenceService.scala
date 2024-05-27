package org.mbari.oni.services

import jakarta.persistence.EntityManagerFactory
import org.mbari.oni.domain.Reference
import org.mbari.oni.jpa.EntityManagerFactories.*
import org.mbari.oni.jpa.repositories.ReferenceRepository

import java.net.URI
import scala.jdk.CollectionConverters.*
import scala.jdk.OptionConverters.*

class ReferenceService(entityManagerFactory: EntityManagerFactory):

    private val log = System.getLogger(getClass.getName)

    def findAll(limit: Int, offset: Int): Either[Throwable, Seq[Reference]] =
        entityManagerFactory.transaction(entityManager =>
            val repo = ReferenceRepository(entityManager)
            repo.findAll(limit, offset)
                .asScala
                .toSeq
                .map(Reference.from)
        )

    def findByReferenceGlob(glob: String, limit: Int, offset: Int): Either[Throwable, Seq[Reference]] =
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

