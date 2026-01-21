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
