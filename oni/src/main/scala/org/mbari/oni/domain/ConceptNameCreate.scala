/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2024
 *
 * oni code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential. 
 */

package org.mbari.oni.domain

import org.mbari.oni.jpa.entities.{ConceptEntity, ConceptNameEntity}

case class ConceptNameCreate(
    name: String,
    newName: String,
    nameType: String,
    author: Option[String] = None,
    userName: Option[String] = None
):

    def toEntity: ConceptNameEntity =
        val entity = new ConceptNameEntity()
        entity.setName(newName)
        entity.setNameType(nameType)
        author.foreach(entity.setAuthor)
        entity
