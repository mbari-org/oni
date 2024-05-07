/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2024
 *
 * oni code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential. 
 */

package org.mbari.oni.domain

import org.mbari.oni.jpa.entities.ConceptNameEntity

case class RawConceptName(name: String, nameType: String, author: Option[String] = None):
    def toEntity: ConceptNameEntity =
        val entity = new ConceptNameEntity()
        entity.setName(name)
        entity.setNameType(nameType)
        author.foreach(entity.setAuthor)
        entity

object RawConceptName:
    def fromEntity(entity: ConceptNameEntity): RawConceptName =
        RawConceptName(entity.getName, entity.getNameType, Option(entity.getAuthor))
