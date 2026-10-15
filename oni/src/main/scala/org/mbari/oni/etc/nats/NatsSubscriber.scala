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

import io.nats.client.{Connection, ConnectionListener, Dispatcher, Message}
import org.mbari.oni.etc.circe.CirceCodecs.{*, given}
import org.mbari.oni.etc.jdk.Loggers.given
import org.mbari.oni.jpa.ChangeBus
import org.mbari.oni.jpa.ChangeEvent.{Action, DataType}

import java.nio.charset.StandardCharsets
import java.time.Duration
import java.util.concurrent.{CountDownLatch, TimeUnit}
import scala.util.Try
import scala.util.control.NonFatal

/**
 * Listens to the NATS topic for changes made by other Oni instances and republishes them on the local ChangeBus as
 * remote events, so things like the ConceptCache are cleared on this instance too. Messages sent by this instance
 * (identified by instanceId) are ignored since the change has already been handled locally.
 *
 * Messages sent while this instance is disconnected from NATS are lost, so we also publish a catch-all remote event
 * whenever the connection is (re)established. That clears caches that may have missed changes.
 *
 * The connection is made on a background thread, so creating a subscriber never blocks (or fails) if NATS is down.
 * It starts listening as soon as NATS is available. See awaitReady.
 *
 * @param instanceId
 *   Messages with this source are ignored. Only overridden in tests, to simulate another instance.
 */
class NatsSubscriber(val url: String, val topic: String, instanceId: String = NatsMessage.InstanceId)
    extends AutoCloseable:

    private val log = System.getLogger(getClass.getName)

    private val connectionListener: ConnectionListener = (_, event) =>
        if event == ConnectionListener.Events.CONNECTED || event == ConnectionListener.Events.RECONNECTED then
            log.atInfo.log(s"NATS $event at '$url'. Clearing caches since changes may have been missed.")
            ChangeBus.publishRemote(Action.UPDATED, DataType.CONCEPT, null)

    private val ready                          = new CountDownLatch(1)
    private var closed                         = false
    @volatile private var connection: Connection = null
    private var dispatcher: Dispatcher         = null

    private val connector = NatsClient.connectInBackground("NatsSubscriber-connect", url, Some(connectionListener)): c =>
        this.synchronized:
            if closed then Try(c.close())
            else
                connection = c
                dispatcher = c.createDispatcher(msg => handle(msg))
                dispatcher.subscribe(topic)
                Try(c.flush(Duration.ofSeconds(2))) // Make sure the server knows about the subscription
                ready.countDown()

    /**
     * Waits until we are connected and subscribed.
     * @return
     *   true if the subscription is active, false if the timeout elapsed first (e.g. NATS is down)
     */
    def awaitReady(timeout: Duration): Boolean = ready.await(timeout.toMillis, TimeUnit.MILLISECONDS)

    private def handle(msg: Message): Unit =
        try
            new String(msg.getData, StandardCharsets.UTF_8).reify[NatsMessage] match
                case Left(e)        => log.atWarn.withCause(e).log("Ignoring an unparseable message from NATS")
                case Right(message) =>
                    if !message.source.contains(instanceId) then
                        // An unrecognized action or type (e.g. from a newer Oni) is treated as a generic concept
                        // change. Clearing too much is safe. Missing a change is not.
                        val action   = Try(Action.valueOf(message.action)).getOrElse(Action.UPDATED)
                        val dataType = Try(DataType.valueOf(message.dataType)).getOrElse(DataType.CONCEPT)
                        ChangeBus.publishRemote(action, dataType, message.id.orNull)
        catch case NonFatal(e) => log.atWarn.withCause(e).log("Failed to handle a message from NATS")

    override def close(): Unit =
        this.synchronized:
            closed = true
            connector.interrupt()
            if dispatcher != null then Try(dispatcher.unsubscribe(topic))
            if connection != null then Try(connection.close())

object NatsSubscriber:

    private val log = System.getLogger(getClass.getName)

    /**
     * Starts listening to NATS if it is enabled in the config.
     *
     * @return
     *   The running subscriber, or None if NATS is not enabled or could not be set up
     */
    def autowire(opt: Option[NatsConfig]): Option[NatsSubscriber] =
        try
            for
                config <- opt
                if config.enable
            yield
                log.atInfo.log(s"Listening for changes from other Oni instances on '${config.topic}' at '${config.url}'")
                new NatsSubscriber(config.url, config.topic)
        catch
            case NonFatal(e) =>
                log.atError.withCause(e).log("Failed to initialize NATS subscriber")
                None
