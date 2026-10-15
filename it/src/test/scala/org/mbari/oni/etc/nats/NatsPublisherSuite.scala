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
import org.mbari.oni.jpa.ChangeBus
import org.mbari.oni.etc.circe.CirceCodecs.given
import org.mbari.oni.jpa.ChangeEvent.{Action, DataType}
import org.testcontainers.containers.GenericContainer
import org.testcontainers.utility.DockerImageName

import java.nio.charset.StandardCharsets
import java.util.concurrent.{LinkedBlockingQueue, TimeUnit}

/**
 * Publishes through a real NATS server running in a container.
 */
class NatsPublisherSuite extends munit.FunSuite:

    private val container = new GenericContainer(DockerImageName.parse("nats:latest"))
    container.addExposedPort(4222)
    container.start()
    Runtime.getRuntime.addShutdownHook(new Thread(() => container.stop()))

    private val natsUrl = s"nats://localhost:${container.getMappedPort(4222)}"
    private val topic   = "test.oni"

    private val autowired = NatsPublisher
        .autowire(Some(NatsConfig(natsUrl, enable = true, topic = topic)))
        .getOrElse(fail("NatsPublisher.autowire returned None"))

    private val nc         = Nats.connect(natsUrl)
    private val received   = new LinkedBlockingQueue[String]()
    private val dispatcher = nc.createDispatcher(msg => received.offer(new String(msg.getData, StandardCharsets.UTF_8)))
    dispatcher.subscribe(topic)
    nc.flush(java.time.Duration.ofSeconds(2)) // make sure the subscription is registered before tests run

    override def afterAll(): Unit =
        autowired.close()
        nc.close()

    override def beforeEach(context: BeforeEach): Unit = received.clear()

    private def poll(): Option[NatsMessage] =
        Option(received.poll(5, TimeUnit.SECONDS)).flatMap(json => io.circe.parser.decode[NatsMessage](json).toOption)

    test("a change published on the ChangeBus arrives on NATS") {
        ChangeBus.publish(Action.UPDATED, DataType.CONCEPT, java.lang.Long.valueOf(99))
        assertEquals(poll(), Some(NatsMessage("UPDATED", "CONCEPT", Some("99"))))
    }

    test("an explicit event with no id arrives on NATS") {
        ChangeBus.publish(Action.UPDATED, DataType.LINK_TEMPLATE, null)
        assertEquals(poll(), Some(NatsMessage("UPDATED", "LINK_TEMPLATE", None)))
    }

    test("user account changes are not published") {
        ChangeBus.publish(Action.CREATED, DataType.USER_ACCOUNT, java.lang.Long.valueOf(1))
        assertEquals(Option(received.poll(1, TimeUnit.SECONDS)), None)
    }

    test("identical messages published together are coalesced") {
        (1 to 20).foreach(_ => ChangeBus.publish(Action.UPDATED, DataType.CONCEPT_NAME, java.lang.Long.valueOf(5)))
        Thread.sleep(500)
        assert(received.size() >= 1 && received.size() < 20, s"Expected coalescing but received ${received.size()}")
    }

    test("autowire returns None when NATS is disabled or unconfigured") {
        assertEquals(NatsPublisher.autowire(None).isEmpty, true)
        assertEquals(NatsPublisher.autowire(Some(NatsConfig(natsUrl, enable = false, topic = topic))).isEmpty, true)
    }
