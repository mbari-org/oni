/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2024
 *
 * oni code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential. 
 */

package org.mbari.oni.domain

import org.mbari.oni.jpa.entities.ConceptNameEntity

case class ConceptNameUpdate(
    name: String,
    newName: Option[String] = None,
    nameType: Option[String] = None,
    author: Option[String] = None
):
    def updateEntity(entity: ConceptNameEntity): ConceptNameEntity =
        newName.foreach(entity.setName)
        nameType.foreach(entity.setNameType)
        author.foreach(entity.setAuthor)
        entity
