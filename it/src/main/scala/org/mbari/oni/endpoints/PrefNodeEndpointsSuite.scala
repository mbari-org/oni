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

import org.mbari.oni.domain.PrefNode
import org.mbari.oni.etc.jwt.JwtService
import org.mbari.oni.jpa.DataInitializer
import org.mbari.oni.services.{PrefNodeService, UserAuthMixin}
import org.mbari.oni.etc.jdk.Loggers.given
import org.mbari.oni.etc.jdk.Strings
import sttp.model.StatusCode
import org.mbari.oni.etc.circe.CirceCodecs.{*, given}
import org.mbari.oni.util.Reflect

trait PrefNodeEndpointsSuite extends EndpointsSuite with DataInitializer with UserAuthMixin:

    given jwtService: JwtService          = JwtService("mbari", "foo", "bar")
    lazy val endpoints: PrefNodeEndpoints = PrefNodeEndpoints(entityManagerFactory)
    lazy val service: PrefNodeService     = PrefNodeService(entityManagerFactory)
    private val password                  = "supersecretpassword"

    override def beforeEach(context: BeforeEach): Unit =
        super.beforeEach(context)
        service.findAll(10000) match
            case Right(entities) =>
                entities.foreach(node => service.delete(node.name, node.key))
            case Left(error)     => log.atDebug.withCause(error).log("Failed to delete all pref node entities")

    private def createNodes(n: Int): Seq[PrefNode] =
        val nodes =
            for i <- 1 to n
            yield
                val name  = s"name_${Strings.random(5)}_$i}"
                val key   = s"key_${Strings.random(5)}_$i}"
                val value = s"value_${Strings.random(5)}_$i}"
                service.create(name, key, value) match
                    case Right(node) => node
                    case Left(error) =>
                        log.atError.withCause(error).log("Failed to create pref node")
                        fail(error.getMessage)

        nodes.sortBy(_.name)

    test("findByPrefix") {
        val nodes  = createNodes(4)
        val prefix = nodes.head.name
        runGet(
            endpoints.findByPrefixImpl,
            s"http://test.com/v1/prefs/startswith?prefix=$prefix",
            response =>
                assertEquals(response.code, StatusCode.Ok)
                val obtained = checkResponse[Seq[PrefNode]](response.body)
                assertEquals(obtained.size, 1)
                assertEquals(obtained.head.name, prefix)
        )

        runGet(
            endpoints.findByPrefixImpl,
            s"http://test.com/v1/prefs/startswith?prefix=${prefix.substring(0, 4)}", // `name`
            response =>
                assertEquals(response.code, StatusCode.Ok)
                val obtained = checkResponse[Seq[PrefNode]](response.body).sortBy(_.name)
                assertEquals(obtained.size, 4)
                assertEquals(obtained, nodes)
        )

    }

    test("findAllEndpoint") {
        val nodes   = createNodes(4)
        val attempt = testWithUserAuth(
            user =>
                runGet(
                    endpoints.findAllImpl,
                    "http://test.com/v1/prefs",
                    response =>
                        assertEquals(response.code, StatusCode.Ok)
                        val obtained = checkResponse[Seq[PrefNode]](response.body).sortBy(_.name)
                        assertEquals(obtained.size, 4)
                        assertEquals(obtained, nodes)
                    ,
                    jwt = jwtService.login(user.username, password, user.toEntity)
                ),
            password
        )

        attempt match
            case Right(_)    => println("Success")
            case Left(error) => fail(error.toString)
    }

    test("createEndpoint (JSON body)") {
        val name  = "name_" + Strings.random(5)
        val key   = "key" + Strings.random(5)
        val value = "value" + Strings.random(5)
        val node  = PrefNode(name, key, value)
        testWithUserAuth(
            user =>
                runPost(
                    endpoints.createEndpointImpl,
                    "http://test.com/v1/prefs",
                    node.stringify,
                    response =>
                        assertEquals(response.code, StatusCode.Ok)
                        val obtained = checkResponse[PrefNode](response.body)
                        assertEquals(obtained.name, name)
                        assertEquals(obtained.key, key)
                        assertEquals(obtained.value, value)
                    ,
                    jwt = jwtService.login(user.username, password, user.toEntity)
                ),
            password
        )
    }

    test("createEndpoint (form body)") {
        val name  = "name_" + Strings.random(5)
        val key   = "key" + Strings.random(5)
        val value = "value" + Strings.random(5)
        val node  = PrefNode(name, key, value)
        testWithUserAuth(
            user =>
                runPost(
                    endpoints.createEndpointImpl,
                    "http://test.com/v1/prefs",
                    Reflect.toFormBody(node),
                    response =>
                        assertEquals(response.code, StatusCode.Ok)
                        val obtained = checkResponse[PrefNode](response.body)
                        assertEquals(obtained.name, name)
                        assertEquals(obtained.key, key)
                        assertEquals(obtained.value, value)
                    ,
                    jwt = jwtService.login(user.username, password, user.toEntity)
                ),
            password
        )
    }

    test("updateEndpoint (JSON body)") {
        val nodes       = createNodes(1)
        val node        = nodes.head
        val newValue    = "new_value_" + Strings.random(5)
        val updatedNode = node.copy(value = newValue)
        testWithUserAuth(
            user =>
                runPost(
                    endpoints.updateEndpointImpl,
                    s"http://test.com/v1/prefs",
                    updatedNode.stringify,
                    response =>
                        assertEquals(response.code, StatusCode.Ok)
                        val obtained = checkResponse[PrefNode](response.body)
                        assertEquals(obtained.name, node.name)
                        assertEquals(obtained.key, node.key)
                        assertEquals(obtained.value, newValue)
                    ,
                    jwt = jwtService.login(user.username, password, user.toEntity)
                ),
            password
        )
    }

    test("updateEndpoint (form body)") {
        val nodes       = createNodes(1)
        val node        = nodes.head
        val newValue    = "new_value_" + Strings.random(5)
        val updatedNode = node.copy(value = newValue)
        testWithUserAuth(
            user =>
                runPost(
                    endpoints.updateEndpointImpl,
                    s"http://test.com/v1/prefs",
                    Reflect.toFormBody(updatedNode),
                    response =>
                        assertEquals(response.code, StatusCode.Ok)
                        val obtained = checkResponse[PrefNode](response.body)
                        assertEquals(obtained.name, node.name)
                        assertEquals(obtained.key, node.key)
                        assertEquals(obtained.value, newValue)
                    ,
                    jwt = jwtService.login(user.username, password, user.toEntity)
                ),
            password
        )
    }

    test("deleteEndpoint") {
        val nodes = createNodes(1)
        val node  = nodes.head
        testWithUserAuth(
            user =>
                runDelete(
                    endpoints.deleteEndpointImpl,
                    s"http://test.com/v1/prefs?name=${node.name}&key=${node.key}",
                    response =>
                        assertEquals(response.code, StatusCode.Ok)
                        val obtained = checkResponse[PrefNode](response.body)
                        assertEquals(obtained.name, node.name)
                        assertEquals(obtained.key, node.key)
                        assertEquals(obtained.value, node.value)
                    ,
                    jwt = jwtService.login(user.username, password, user.toEntity)
                ),
            password
        )

        service.findByNodeNameAndKey(node.name, node.key) match
            case Right(None)           => assert(true)
            case Right(Some(prefNode)) => fail(s"Expected None but got $prefNode")
            case Left(error)           => fail(error.getMessage)
    }
