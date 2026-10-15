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

import io.nats.client.Connection
import org.mbari.oni.etc.circe.CirceCodecs.{*, given}
import org.mbari.oni.etc.jdk.Loggers.given

import java.nio.charset.StandardCharsets
import java.time.Duration
import java.util.concurrent.{LinkedBlockingQueue, TimeUnit}
import scala.jdk.CollectionConverters.*
import scala.util.Try
import scala.util.control.NonFatal
 
/**
 * Publishes NatsMessages to a NATS topic. Messages are queued and sent from a daemon thread so that a slow or
 * unavailable NATS server never blocks the thread that committed a database transaction. Messages that arrive close
 * together are de-duplicated. The jnats client reconnects automatically (forever), and the initial connection is made
 * in the background, so Oni starts normally even if NATS is down.
 *
 * Messages only signal that something changed, and consumers re-query Oni, so if the queue overflows during a long
 * outage the newest message is dropped rather than blocking or growing memory without bound.
 */
class NatsPublisher(val url: String, val topic: String, maxQueueSize: Int = NatsPublisher.MaxQueueSize)
    extends AutoCloseable:

    private val log                         = System.getLogger(getClass.getName)
    private val queue                       = new LinkedBlockingQueue[NatsMessage](maxQueueSize)
    @volatile private var ok                = true
    private val connection: Connection      = NatsClient.connect(url)

    private val thread = new Thread(() => run(), "NatsPublisher")
    thread.setDaemon(true)
    thread.start()

    /** Queue a message for publishing. Never blocks. */
    def offer(msg: NatsMessage): Unit =
        if !queue.offer(msg) then log.atWarn.log(s"NATS queue is full, dropping message: $msg")

    private def run(): Unit =
        while ok do
            try
                val first = queue.poll(1, TimeUnit.SECONDS)
                if first != null then
                    val pending = new java.util.ArrayList[NatsMessage]()
                    pending.add(first)
                    queue.drainTo(pending)
                    // Coalesce duplicates that arrived together, e.g. many rows changed by a single commit
                    pending.asScala.distinct.foreach(publish)
            catch
                case _: InterruptedException => ok = false
                case NonFatal(e)             => log.atWarn.withCause(e).log("Unexpected error in NatsPublisher thread")

    private def publish(msg: NatsMessage): Unit =
        try connection.publish(topic, msg.stringify.getBytes(StandardCharsets.UTF_8))
        catch case NonFatal(e) => log.atWarn.withCause(e).log(s"Failed to publish to NATS, dropping $msg")

    override def close(): Unit =
        ok = false
        thread.interrupt()
        Try(connection.flush(Duration.ofSeconds(1)))
        Try(connection.close())

object NatsPublisher:

    val MaxQueueSize = 10000

    private val log = System.getLogger(getClass.getName)

    /**
     * Creates a NatsPublisher, and a NatsBridge that feeds it from the ChangeBus, if NATS is enabled in the config.
     *
     * @param opt
     *   The NATS config. None (e.g. config was missing) means NATS is not used.
     * @return
     *   A Closeable that stops both, or None if NATS is not enabled or could not be set up
     */
    def autowire(opt: Option[NatsConfig]): Option[AutoCloseable] =
        val result =
            try
                for
                    config <- opt
                    if config.enable
                yield
                    val publisher = new NatsPublisher(config.url, config.topic)
                    val bridge    = new NatsBridge(publisher.offer)
                    new AutoCloseable:
                        override def close(): Unit =
                            bridge.close()
                            publisher.close()
            catch
                case NonFatal(e) =>
                    log.atError.withCause(e).log("Failed to initialize NATS publisher")
                    None
        result match
            case None    => log.atInfo.log("NATS is not enabled/configured")
            case Some(_) =>
                log.atInfo.log(s"NATS is publishing concept changes using topic '${opt.get.topic}' at '${opt.get.url}'")
        result
