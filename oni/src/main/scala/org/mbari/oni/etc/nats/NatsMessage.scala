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

package org.mbari.oni.etc.nats

import org.mbari.oni.etc.circe.CirceCodecs.{*, given}
import org.mbari.oni.jpa.ChangeEvent

import java.util.UUID

/**
 * A minimal message describing a committed change. It is used to tell other services that something changed so they
 * can refresh (e.g. invalidate a cache) without sending the entire record. The receiving service queries Oni for the
 * current state.
 *
 * @param action
 *   1 of CREATED, UPDATED, or DELETED
 * @param dataType
 *   The kind of record that changed, e.g. CONCEPT, CONCEPT_NAME, LINK_REALIZATION
 * @param id
 *   The primary key of the changed record. None if many records changed (e.g. a bulk rename of link targets).
 * @param source
 *   Identifies the Oni instance that sent the message, so that an instance can ignore its own messages.
 */
case class NatsMessage(
    action: String,
    dataType: String,
    id: Option[String] = None,
    source: Option[String] = None
)



object NatsMessage:

    /** Identifies this running Oni instance. It is different every time Oni starts. */
    val InstanceId: String = UUID.randomUUID().toString

    def from(event: ChangeEvent, source: Option[String] = Some(InstanceId)): NatsMessage =
        NatsMessage(
            event.action().toString,
            event.dataType().toString,
            Option(event.id()).map(_.toString),
            source
        )
