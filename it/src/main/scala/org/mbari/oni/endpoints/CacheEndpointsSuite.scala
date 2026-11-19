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

package org.mbari.oni.endpoints

import org.mbari.oni.cache.KnowledgebaseCacheFixture
import org.mbari.oni.domain.{ConceptNameTypes, UserAccountRoles}
import org.mbari.oni.etc.jwt.JwtService
import org.mbari.oni.etc.nats.NatsBridge
import org.mbari.oni.jpa.EntityManagerFactories.*
import org.mbari.oni.jpa.entities.ConceptEntity
import org.mbari.oni.jpa.{ChangeBus, ChangeEvent, DataInitializer}
import sttp.model.StatusCode

import java.util.concurrent.CopyOnWriteArrayList
import scala.jdk.CollectionConverters.*

trait CacheEndpointsSuite extends EndpointsSuite with DataInitializer with EndpointsAuthMixin with KnowledgebaseCacheFixture:

    given jwtService: JwtService       = JwtService("mbari", "foo", "bar")
    lazy val endpoints: CacheEndpoints = CacheEndpoints(entityManagerFactory)

    private val url = "http://test.com/v1/cache/clear"

    /** Deletes a concept name with plain SQL, like a manual fix. Publishes no event, so the cache goes stale. */
    private def deleteNameOutsideOni(concept: ConceptEntity): String =
        val name = concept.getConceptNames.asScala.find(_.getNameType != ConceptNameTypes.PRIMARY.getType).get.getName
        entityManagerFactory.transaction(em =>
            em.createNativeQuery("DELETE FROM ConceptName WHERE CONCEPTNAME = ?").setParameter(1, name).executeUpdate()
        )
        name

    /** Runs fn and returns the events published on the ChangeBus while it ran */
    private def capture(fn: => Unit): Seq[ChangeEvent] =
        val events      = new CopyOnWriteArrayList[ChangeEvent]()
        val unsubscribe = ChangeBus.subscribe(e => events.add(e))
        try
            fn
            events.asScala.toSeq
        finally unsubscribe.run()

    test("an administrator can clear the cache") {
        val root = init(2, 0)
        assert(knowledgebaseCache.findUp(root.getName).isDefined)
        val name = deleteNameOutsideOni(root)
        assert(knowledgebaseCache.findUp(name).isDefined, "The cache should be stale after a change made outside Oni")

        val events = capture(
            withRole(UserAccountRoles.ADMINISTRATOR)(jwt =>
                runPost(endpoints.clearEndpointImpl, url, "", r => assertEquals(r.code, StatusCode.Ok), jwt = jwt)
            )
        )

        assertEquals(knowledgebaseCache.findUp(name), None, "The cache should have been cleared")
        // The clear is a normal local change event, so NatsBridge forwards it to other instances
        val clearEvents = events.filter(e => e.dataType() == ChangeEvent.DataType.CONCEPT && !e.remote())
        assertEquals(clearEvents.size, 1)
        val bridge      = NatsBridge(_ => ())
        try assert(bridge.handle(clearEvents.head).isDefined, "NatsBridge should forward the clear")
        finally bridge.close()
    }

    test("a maintainer is forbidden") {
        val root = init(2, 0)
        knowledgebaseCache.findUp(root.getName)
        val name = deleteNameOutsideOni(root)
        withRole(UserAccountRoles.MAINTENANCE)(jwt =>
            runPost(endpoints.clearEndpointImpl, url, "", r => assertEquals(r.code, StatusCode.Forbidden), jwt = jwt)
        )
        assert(knowledgebaseCache.findUp(name).isDefined, "A forbidden request must not clear the cache")
    }

    test("an API key token is forbidden") {
        runPost(
            endpoints.clearEndpointImpl,
            url,
            "",
            r => assertEquals(r.code, StatusCode.Forbidden),
            jwt = jwtService.authorize(jwtService.apiKey)
        )
    }

    test("no token is unauthorized") {
        runPost(endpoints.clearEndpointImpl, url, "", r => assertEquals(r.code, StatusCode.Unauthorized))
    }
