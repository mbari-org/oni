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

import org.mbari.oni.jpa.{ChangeBus, ChangeEvent}

/**
 * Translates ChangeEvents published on the ChangeBus into NatsMessages and forwards them to the sink. Only changes to
 * concept data are forwarded; see ChangeEvent.affectsConceptData.
 *
 * @param sink
 *   Receives the messages, typically NatsPublisher.offer
 */
class NatsBridge(sink: NatsMessage => Unit) extends AutoCloseable:

    private val unsubscribe: Runnable = ChangeBus.subscribe(event => handle(event).foreach(sink))

    def handle(event: ChangeEvent): Option[NatsMessage] =
        if event.affectsConceptData() then Some(NatsMessage.from(event)) else None

    override def close(): Unit = unsubscribe.run()
