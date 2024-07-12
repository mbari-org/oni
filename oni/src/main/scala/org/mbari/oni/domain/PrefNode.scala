/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2024
 *
 * oni code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential.
 */

package org.mbari.oni.domain

import org.mbari.oni.jpa.entities.PreferenceNodeEntity

case class PrefNode(name: String, key: String, value: String):
    def toEntity: PreferenceNodeEntity =
        val entity = new PreferenceNodeEntity()
        entity.setNodeName(name)
        entity.setPrefKey(key)
        entity.setPrefValue(value)
        entity

object PrefNode:
    def from(entity: PreferenceNodeEntity): PrefNode = PrefNode(
        entity.getNodeName,
        entity.getPrefKey,
        entity.getPrefValue
    )

case class PrefNodeUpdate(name: Option[String], key: Option[String], value: String)

