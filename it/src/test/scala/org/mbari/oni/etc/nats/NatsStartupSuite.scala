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

import io.nats.client.Nats
import org.mbari.oni.etc.circe.CirceCodecs.{*, given}
import org.mbari.oni.jpa.ChangeBus
import org.mbari.oni.jpa.ChangeEvent.{Action, DataType}
import org.testcontainers.containers.GenericContainer
import org.testcontainers.utility.DockerImageName

import java.net.ServerSocket
import java.nio.charset.StandardCharsets
import java.time.Duration
import java.util.concurrent.{CopyOnWriteArrayList, LinkedBlockingQueue, TimeUnit}
import scala.jdk.CollectionConverters.*

/**
 * Oni must start, and keep working, when NATS is enabled but the NATS server is down. Nats.connectReconnectOnConnect
 * never returns while the server is unreachable, so connecting must not happen on the caller's thread.
 */
class NatsStartupSuite extends munit.FunSuite:

    private def freePort(): Int =
        val socket = new ServerSocket(0)
        try socket.getLocalPort
        finally socket.close()

    /** Runs fn and returns its result and how long it took in milliseconds. */
    private def timed[T](fn: => T): (T, Long) =
        val start  = System.currentTimeMillis()
        val result = fn
        (result, System.currentTimeMillis() - start)

    test("autowire, offer and close do not block when the NATS server is down") {
        val config = NatsConfig(s"nats://localhost:${freePort()}", enable = true, topic = "test.oni.startup.down")

        val (publisher, publisherMs)   = timed(NatsPublisher.autowire(Some(config)))
        val (subscriber, subscriberMs) = timed(NatsSubscriber.autowire(Some(config)))
        assert(publisher.isDefined, "NatsPublisher.autowire should succeed even though NATS is down")
        assert(subscriber.isDefined, "NatsSubscriber.autowire should succeed even though NATS is down")
        assert(publisherMs < 2000, s"NatsPublisher.autowire took ${publisherMs}ms")
        assert(subscriberMs < 2000, s"NatsSubscriber.autowire took ${subscriberMs}ms")

        // Changes made while NATS is down must not block the thread that made them
        val (_, publishMs) = timed(ChangeBus.publish(Action.UPDATED, DataType.CONCEPT, java.lang.Long.valueOf(1)))
        assert(publishMs < 2000, s"Publishing a change took ${publishMs}ms")

        assert(!subscriber.get.awaitReady(Duration.ofMillis(500)), "Cannot be subscribed while NATS is down")

        val (_, closeMs) = timed:
            subscriber.foreach(_.close())
            publisher.foreach(_.close())
        assert(closeMs < 3000, s"close took ${closeMs}ms")
    }

    test("publishing and subscribing start working once the NATS server becomes available") {
        val port   = freePort()
        val topic  = "test.oni.startup.recovers"
        val config = NatsConfig(s"nats://localhost:$port", enable = true, topic = topic)

        // NATS is down at startup
        val publisher  = NatsPublisher.autowire(Some(config)).getOrElse(fail("NatsPublisher.autowire returned None"))
        val subscriber = NatsSubscriber.autowire(Some(config)).getOrElse(fail("NatsSubscriber.autowire returned None"))
        val events     = new CopyOnWriteArrayList[org.mbari.oni.jpa.ChangeEvent]()
        val unsubscribe = ChangeBus.subscribe(e => events.add(e))

        // A change is made while NATS is down. It should wait in the queue and be sent once we connect.
        ChangeBus.publish(Action.UPDATED, DataType.CONCEPT_NAME, java.lang.Long.valueOf(77))

        // Now NATS comes up, on the port we were configured with
        val container = new GenericContainer(DockerImageName.parse("nats:latest"))
        container.addExposedPort(4222)
        container.setPortBindings(java.util.List.of(s"$port:4222"))
        container.start()
        try
            val nc       = Nats.connect(config.url)
            val received = new LinkedBlockingQueue[String]()
            nc.createDispatcher(msg => received.offer(new String(msg.getData, StandardCharsets.UTF_8))).subscribe(topic)
            nc.flush(Duration.ofSeconds(2))

            assert(subscriber.awaitReady(Duration.ofSeconds(20)), "The subscriber never subscribed after NATS came up")

            // The publisher connects on its own schedule, so the queued message may predate our raw subscription.
            // Publish again until one arrives.
            var message = Option.empty[NatsMessage]
            val deadline = System.currentTimeMillis() + 20000
            while message.isEmpty && System.currentTimeMillis() < deadline do
                ChangeBus.publish(Action.UPDATED, DataType.CONCEPT_NAME, java.lang.Long.valueOf(77))
                message = Option(received.poll(1, TimeUnit.SECONDS)).flatMap(json => json.reify[NatsMessage].toOption)
            assertEquals(message.map(m => (m.dataType, m.id)), Some(("CONCEPT_NAME", Some("77"))))

            // And the subscriber now hears about changes from other instances
            val fromOther = NatsMessage("DELETED", "MEDIA", Some("5"), Some("another-instance"))
            nc.publish(topic, fromOther.stringify.getBytes(StandardCharsets.UTF_8))
            nc.flush(Duration.ofSeconds(2))
            val deadline2 = System.currentTimeMillis() + 5000
            while !events.asScala.exists(e => e.remote() && e.dataType() == DataType.MEDIA) && System.currentTimeMillis() < deadline2 do
                Thread.sleep(50)
            assert(events.asScala.exists(e => e.remote() && e.dataType() == DataType.MEDIA), "No remote event was received")
            nc.close()
        finally
            unsubscribe.run()
            subscriber.close()
            publisher.close()
            container.stop()
    }
