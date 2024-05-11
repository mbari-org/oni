/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2024
 *
 * oni code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential. 
 */

package org.mbari.oni.services

import jakarta.persistence.EntityManagerFactory
import org.mbari.oni.jpa.EntityManagerFactories.*
import org.mbari.oni.jpa.repositories.ConceptNameRepository

import scala.jdk.CollectionConverters.*

class ConceptNameService(entityManagerFactory: EntityManagerFactory):

    def findAllNames(): Either[Throwable, Seq[String]] =
        entityManagerFactory.transaction(entityManger =>
            val repo = new ConceptNameRepository(entityManger)
            repo.findAllNamesAsStrings().asScala.toSeq
        )
