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

package org.mbari.oni.services

import io.nats.client.Nats
import org.mbari.oni.domain.{ConceptNameCreate, ConceptNameTypes, LinkRenameToConceptRequest}
import org.mbari.oni.etc.jdk.Strings
import org.mbari.oni.etc.circe.CirceCodecs.given
import org.mbari.oni.etc.nats.{NatsConfig, NatsMessage, NatsPublisher}
import org.mbari.oni.jpa.DataInitializer
import org.mbari.oni.jpa.EntityManagerFactories.*
import org.mbari.oni.jpa.entities.ConceptNameEntity
import org.mbari.oni.jpa.repositories.ConceptRepository
import org.testcontainers.containers.GenericContainer
import org.testcontainers.utility.DockerImageName

import java.nio.charset.StandardCharsets
import java.util.concurrent.{CopyOnWriteArrayList, TimeUnit}
import scala.jdk.CollectionConverters.*

/**
 * End to end: a real database transaction goes through Hibernate's post-commit listener, the ChangeBus, the NatsBridge
 * and the NatsPublisher, and arrives at a real NATS server. A rolled back transaction must send nothing.
 */
trait NatsChangeNotificationSuite extends DataInitializer with UserAuthMixin:

    lazy val conceptNameService: ConceptNameService         = new ConceptNameService(entityManagerFactory)
    lazy val linkRealizationService: LinkRealizationService = new LinkRealizationService(entityManagerFactory)

    private val topic = "test.oni.e2e"

    private lazy val container =
        val c = new GenericContainer(DockerImageName.parse("nats:latest"))
        c.addExposedPort(4222)
        c.start()
        c

    private lazy val natsUrl = s"nats://localhost:${container.getMappedPort(4222)}"

    private lazy val publisher = NatsPublisher
        .autowire(Some(NatsConfig(natsUrl, enable = true, topic = topic)))
        .getOrElse(fail("NatsPublisher.autowire returned None"))

    private val received = new CopyOnWriteArrayList[NatsMessage]()

    private lazy val subscriber =
        val nc         = Nats.connect(natsUrl)
        val dispatcher = nc.createDispatcher(msg =>
            io.circe.parser
                .decode[NatsMessage](new String(msg.getData, StandardCharsets.UTF_8))
                .foreach(m => received.add(m))
        )
        dispatcher.subscribe(topic)
        nc.flush(java.time.Duration.ofSeconds(2))
        nc

    override def beforeAll(): Unit =
        publisher // starts the container and the publisher
        subscriber

    override def afterAll(): Unit =
        publisher.close()
        subscriber.close()
        container.stop()

    /** Waits until no message has arrived for a while, so events from test setup don't leak into the test. */
    private def settle(): Unit =
        var last = -1
        while received.size() != last do
            last = received.size()
            Thread.sleep(500)
        received.clear()

    private def awaitMessage(predicate: NatsMessage => Boolean, timeoutMs: Long = 5000): Option[NatsMessage] =
        val deadline = System.currentTimeMillis() + timeoutMs
        var found    = Option.empty[NatsMessage]
        while found.isEmpty && System.currentTimeMillis() < deadline do
            found = received.asScala.find(predicate)
            if found.isEmpty then Thread.sleep(50)
        found

    test("a committed concept name reaches NATS") {
        val root = init(2, 2)
        settle()
        val name = root.getPrimaryConceptName.getName
        val dto  = ConceptNameCreate(name, Strings.random(12), ConceptNameTypes.SYNONYM.getType)
        runWithUserAuth(user => conceptNameService.addName(dto, user.username)) match
            case Left(e)  => fail(e.toString)
            case Right(_) =>
                val msg = awaitMessage(m => m.action == "CREATED" && m.dataType == "CONCEPT_NAME")
                assert(msg.exists(_.id.isDefined), s"No CREATED CONCEPT_NAME message with an id. Got: ${received.asScala}")
    }

    test("a rolled back transaction sends nothing to NATS") {
        init(1, 0)
        settle()
        var insertedId = Option.empty[String]
        val result     = entityManagerFactory.transaction(em =>
            val root   = new ConceptRepository(em).findRoot().get()
            val entity = new ConceptNameEntity()
            entity.setName(Strings.random(12))
            entity.setNameType(ConceptNameTypes.SYNONYM.getType)
            root.addConceptName(entity)
            em.flush() // the insert succeeds in the database transaction...
            insertedId = Option(entity.getId).map(_.toString)
            throw new RuntimeException("force rollback") // ...and then we roll back
        )
        assertEquals(result.left.toOption.map(_.getMessage), Some("force rollback"))
        assert(insertedId.isDefined, "The insert should have been flushed before the rollback")
        Thread.sleep(1500) // generous time for a (wrong) message to arrive
        assert(
            !received.asScala.exists(m => m.dataType == "CONCEPT_NAME" && m.id == insertedId),
            s"A message was sent for a rolled back insert: ${received.asScala}"
        )
    }

    test("a bulk link rename (which bypasses Hibernate) reaches NATS") {
        val root                = init(3, 10)
        val allLinkRealizations = root.getDescendants.asScala.flatMap(_.getConceptMetadata.getLinkRealizations.asScala).toSeq
        settle()
        val request             = LinkRenameToConceptRequest(allLinkRealizations.head.getToConcept, Strings.random(10))
        runWithUserAuth(user => linkRealizationService.renameToConcept(request.old, request.`new`, user.username)) match
            case Left(e)  => fail(e.toString)
            case Right(r) =>
                assert(r.count > 0)
                val msg = awaitMessage(m => m.action == "UPDATED" && m.dataType == "LINK_REALIZATION")
                assert(msg.isDefined, s"No UPDATED LINK_REALIZATION message. Got: ${received.asScala}")
                assertEquals(msg.get.id, None)
    }
