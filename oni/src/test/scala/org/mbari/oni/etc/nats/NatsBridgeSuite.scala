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

import org.mbari.oni.jpa.ChangeEvent.{Action, DataType}
import org.mbari.oni.etc.circe.CirceCodecs.{*, given}
import org.mbari.oni.jpa.{ChangeBus, ChangeEvent}

import java.util.concurrent.CopyOnWriteArrayList
import scala.jdk.CollectionConverters.*
import org.mbari.oni.etc.circe.CirceCodecs.stringify

class NatsBridgeSuite extends munit.FunSuite:

    private def withBridge[T](fn: (NatsBridge, CopyOnWriteArrayList[NatsMessage]) => T): T =
        val received = new CopyOnWriteArrayList[NatsMessage]()
        val bridge   = new NatsBridge(m => received.add(m))
        try fn(bridge, received)
        finally bridge.close()

    test("concept changes are translated to messages") {
        withBridge { (bridge, received) =>
            ChangeBus.publish(Action.CREATED, DataType.CONCEPT, java.lang.Long.valueOf(42))
            assertEquals(received.asScala.toSeq, Seq(NatsMessage("CREATED", "CONCEPT", Some("42"), Some(NatsMessage.InstanceId))))
        }
    }

    test("a missing id becomes None") {
        withBridge { (bridge, received) =>
            ChangeBus.publish(Action.UPDATED, DataType.LINK_REALIZATION, null)
            assertEquals(received.asScala.toSeq, Seq(NatsMessage("UPDATED", "LINK_REALIZATION", None, Some(NatsMessage.InstanceId))))
        }
    }

    test("user accounts and preference nodes are not forwarded") {
        withBridge { (bridge, received) =>
            ChangeBus.publish(Action.CREATED, DataType.USER_ACCOUNT, java.lang.Long.valueOf(1))
            ChangeBus.publish(Action.UPDATED, DataType.PREFERENCE_NODE, "x")
            assert(received.isEmpty)
        }
    }

    test("remote events are not forwarded, so instances never echo each other") {
        withBridge { (bridge, received) =>
            ChangeBus.publishRemote(Action.UPDATED, DataType.CONCEPT, "42")
            assert(received.isEmpty)
        }
    }

    test("closing the bridge stops forwarding") {
        val received = new CopyOnWriteArrayList[NatsMessage]()
        val bridge   = new NatsBridge(m => received.add(m))
        bridge.close()
        ChangeBus.publish(Action.CREATED, DataType.CONCEPT, java.lang.Long.valueOf(1))
        assert(received.isEmpty)
    }

    test("messages round trip through JSON") {
        val msg = NatsMessage("DELETED", "CONCEPT_NAME", Some("7"), Some("instance-1"))
        assertEquals(io.circe.parser.decode[NatsMessage](msg.stringify), Right(msg))
        // A missing id is omitted from the JSON
        assertEquals(NatsMessage("UPDATED", "MEDIA").stringify, """{"action":"UPDATED","dataType":"MEDIA"}""")
    }
