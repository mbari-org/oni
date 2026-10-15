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

import io.nats.client.{Connection, ConnectionListener, Nats, Options}

import java.time.Duration

/**
 * Creates NATS connections that reconnect forever, and that are created in the background if the server isn't
 * available yet, so Oni never fails to start (or stops working) because NATS is down.
 */
object NatsClient:

    def connect(url: String, listener: Option[ConnectionListener] = None): Connection =
        val builder = Options
            .builder()
            .server(url)
            .maxReconnects(-1)
            .reconnectWait(Duration.ofSeconds(2))
        listener.foreach(builder.connectionListener)
        Nats.connectReconnectOnConnect(builder.build())
