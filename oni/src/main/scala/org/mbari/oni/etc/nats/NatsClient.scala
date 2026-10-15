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
import scala.util.control.NonFatal

/**
 * Creates NATS connections that reconnect forever.
 *
 * Careful: Nats.connectReconnectOnConnect retries until it connects. With unlimited reconnects it never returns if the
 * server is down, so it must never be called on Oni's startup thread. Use connectInBackground, which is what lets Oni
 * start (and keep working) when NATS is unavailable.
 */
object NatsClient:

    private val log = System.getLogger(getClass.getName)

    /** BLOCKS until a connection is made, which may be forever if the server is down. See connectInBackground. */
    def connectBlocking(url: String, listener: Option[ConnectionListener] = None): Connection =
        val builder = Options
            .builder()
            .server(url)
            .maxReconnects(-1)
            .reconnectWait(Duration.ofSeconds(2))
        listener.foreach(builder.connectionListener)
        Nats.connectReconnectOnConnect(builder.build())

    /**
     * Connects on a new daemon thread and returns immediately. The thread keeps retrying until the server is available,
     * then passes the connection to onConnected. After that, jnats reconnects automatically if the connection drops.
     *
     * @return
     *   The connecting thread. Interrupt it to stop trying to connect.
     */
    def connectInBackground(name: String, url: String, listener: Option[ConnectionListener] = None)(
        onConnected: Connection => Unit
    ): Thread =
        val thread = new Thread(
            () =>
                try onConnected(connectBlocking(url, listener))
                catch
                    case _: InterruptedException => () // We were closed before NATS became available
                    case NonFatal(e)             =>
                        log.log(System.Logger.Level.ERROR, s"Unable to connect to NATS at '$url'", e)
            ,
            name
        )
        thread.setDaemon(true)
        thread.start()
        thread
