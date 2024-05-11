/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2024
 *
 * oni code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential. 
 */

package org.mbari.oni.jdbc

/*
 * Copyright 2017 Monterey Bay Aquarium Research Institute
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

import java.time.Instant
import org.mbari.oni.domain.ConceptNameTypes

import scala.collection.mutable


/**
 * @author Brian Schlining
 * @since 2018-02-11T11:34:00
 */
case class ConceptRow(
                         id: Long,
                         parentId: Option[Long],
                         name: String,
                         rankLevel: Option[String] = None,
                         rankName: Option[String] = None,
                         nameType: String = ConceptNameTypes.PRIMARY.getType,
                         conceptTimestamp: Instant = Instant.EPOCH,
                         conceptNameTimestamp: Instant = Instant.EPOCH
                     ) {

    lazy val rank: Option[String] = rankName.map(n => rankLevel.getOrElse("") + n)

    lazy val lastUpdate: Instant = Seq(conceptTimestamp, conceptNameTimestamp)
        .maxBy(i => i.toEpochMilli)

}

case class CName(name: String, nameType: String)
