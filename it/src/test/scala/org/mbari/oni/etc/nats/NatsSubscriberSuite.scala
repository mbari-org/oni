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
import org.mbari.oni.jpa.ChangeEvent.{Action, DataType}
import org.mbari.oni.jpa.{ChangeBus, ChangeEvent}
import org.testcontainers.containers.GenericContainer
import org.testcontainers.utility.DockerImageName

import java.nio.charset.StandardCharsets
import java.util.concurrent.CopyOnWriteArrayList
import scala.jdk.CollectionConverters.*

/**
 * Receives messages through a real NATS server running in a container. A raw NATS client plays the part of "another
 * Oni instance".
 */
class NatsSubscriberSuite extends munit.FunSuite:

    private val container = new GenericContainer(DockerImageName.parse("nats:latest"))
    container.addExposedPort(4222)
    container.start()
    Runtime.getRuntime.addShutdownHook(new Thread(() => container.stop()))

    private val natsUrl = s"nats://localhost:${container.getMappedPort(4222)}"
    private val topic   = "test.oni.subscriber"
    private val config  = NatsConfig(natsUrl, enable = true, topic = topic)

    private val events = new CopyOnWriteArrayList[ChangeEvent]()
    private val unsubscribe = ChangeBus.subscribe(e => events.add(e))

    // Records everything on the topic, so we can see whether anything is echoed back
    private val onTopic    = new CopyOnWriteArrayList[String]()
    private val otherInstance = Nats.connect(natsUrl)
    otherInstance
        .createDispatcher(msg => onTopic.add(new String(msg.getData, StandardCharsets.UTF_8)))
        .subscribe(topic)
    otherInstance.flush(java.time.Duration.ofSeconds(2))

    private val subscriber = NatsSubscriber
        .autowire(Some(config))
        .getOrElse(fail("NatsSubscriber.autowire returned None"))
    // The bridge and publisher are running too, to prove remote events are not echoed back to NATS
    private val publisher  = NatsPublisher
        .autowire(Some(config))
        .getOrElse(fail("NatsPublisher.autowire returned None"))

    // The subscriber connects in the background. Don't start sending until it is listening.
    assert(subscriber.awaitReady(java.time.Duration.ofSeconds(10)), "The subscriber never became ready")

    override def afterAll(): Unit =
        unsubscribe.run()
        publisher.close()
        subscriber.close()
        otherInstance.close()

    override def beforeEach(context: BeforeEach): Unit =
        awaitQuiet()
        events.clear()
        onTopic.clear()

    /** Waits until nothing new has arrived for a while. */
    private def awaitQuiet(): Unit =
        var last = -1
        while events.size() + onTopic.size() != last do
            last = events.size() + onTopic.size()
            Thread.sleep(300)

    private def sendFromOtherInstance(message: NatsMessage): Unit =
        otherInstance.publish(topic, message.stringify.getBytes(StandardCharsets.UTF_8))
        otherInstance.flush(java.time.Duration.ofSeconds(2))

    private def await(predicate: ChangeEvent => Boolean, timeoutMs: Long = 5000): Option[ChangeEvent] =
        val deadline = System.currentTimeMillis() + timeoutMs
        var found    = Option.empty[ChangeEvent]
        while found.isEmpty && System.currentTimeMillis() < deadline do
            found = events.asScala.find(predicate)
            if found.isEmpty then Thread.sleep(50)
        found

    test("a message from another instance becomes a remote event on the ChangeBus") {
        sendFromOtherInstance(NatsMessage("DELETED", "CONCEPT_NAME", Some("12"), Some("other-instance")))
        val event = await(e => e.dataType() == DataType.CONCEPT_NAME)
        assertEquals(event.map(e => (e.action(), e.id(), e.remote())), Some((Action.DELETED, "12", true)))
    }

    test("a message with no id is handled") {
        sendFromOtherInstance(NatsMessage("UPDATED", "LINK_REALIZATION", None, Some("other-instance")))
        val event = await(e => e.dataType() == DataType.LINK_REALIZATION)
        assert(event.exists(e => e.id() == null && e.remote()), s"Unexpected event: $event")
    }

    test("our own messages are ignored") {
        sendFromOtherInstance(NatsMessage("UPDATED", "CONCEPT", Some("1"), Some(NatsMessage.InstanceId)))
        Thread.sleep(1000)
        assert(events.isEmpty, s"Expected our own message to be ignored but got $events")
    }

    test("remote events are not echoed back to NATS") {
        sendFromOtherInstance(NatsMessage("UPDATED", "CONCEPT", Some("3"), Some("other-instance")))
        assert(await(e => e.remote()).isDefined)
        Thread.sleep(1500) // generous time for a (wrong) echo to arrive
        assertEquals(onTopic.size(), 1, s"Only the original message should be on the topic: $onTopic")
    }

    test("an unknown action or data type is treated as a generic concept change") {
        sendFromOtherInstance(NatsMessage("TRUNCATED", "SOMETHING_NEW", Some("5"), Some("other-instance")))
        val event = await(e => e.id() == "5")
        assertEquals(event.map(e => (e.action(), e.dataType(), e.remote())), Some((Action.UPDATED, DataType.CONCEPT, true)))
    }

    test("an unparseable message is ignored and the subscription keeps working") {
        otherInstance.publish(topic, "this is not json".getBytes(StandardCharsets.UTF_8))
        sendFromOtherInstance(NatsMessage("CREATED", "MEDIA", Some("9"), Some("other-instance")))
        assert(await(e => e.dataType() == DataType.MEDIA).isDefined)
    }

    test("connecting publishes a catch-all event to clear caches that may have missed changes") {
        val seen        = new CopyOnWriteArrayList[ChangeEvent]()
        val unsubscribe = ChangeBus.subscribe(e => seen.add(e))
        val another     = new NatsSubscriber(natsUrl, topic)
        try
            val deadline = System.currentTimeMillis() + 5000
            while seen.isEmpty && System.currentTimeMillis() < deadline do Thread.sleep(50)
            assert(seen.asScala.exists(e => e.remote() && e.dataType() == DataType.CONCEPT && e.id() == null))
        finally
            another.close()
            unsubscribe.run()
    }
