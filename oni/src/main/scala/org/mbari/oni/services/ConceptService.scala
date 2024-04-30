/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2024
 *
 * oni code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential. 
 */

package org.mbari.oni.services

import jakarta.persistence.EntityManagerFactory
import org.mbari.oni.jpa.entities.ConceptEntity

import scala.util.Try

class ConceptService(entityManagerFactory: EntityManagerFactory):

    def init(root: ConceptEntity): Either[Throwable, ConceptEntity] =
        Try {
            val entityManager = entityManagerFactory.createEntityManager()
            entityManager.getTransaction.begin()
            entityManager.persist(root)
            entityManager.getTransaction.commit()
            entityManager.close()
            root
        }.toEither
