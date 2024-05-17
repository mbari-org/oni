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

import org.mbari.oni.jpa.DataInitializer

trait PrefNodeServiceSuite extends DataInitializer:

    lazy val prefNodeService = new PrefNodeService(entityManagerFactory)

    test("create") {
        val name  = "test"
        val key   = "key"
        val value = "value"
        prefNodeService.create(name, key, value) match
            case Left(e)         => fail(e.getMessage)
            case Right(prefNode) =>
                assertEquals(prefNode.name, name)
                assertEquals(prefNode.key, key)
                assertEquals(prefNode.value, value)
    }

    test("update") {
        val name  = "test"
        val key   = "key2"
        val value = "value"
        prefNodeService.create(name, key, value) match
            case Left(e)         => fail(e.getMessage)
            case Right(prefNode) =>
                val newValue = "newValue"
                prefNodeService.update(prefNode.copy(value = newValue)) match
                    case Left(e)                => fail(e.getMessage)
                    case Right(updatedPrefNode) =>
                        assertEquals(updatedPrefNode.name, name)
                        assertEquals(updatedPrefNode.key, key)
                        assertEquals(updatedPrefNode.value, newValue)
    }

    test("delete") {
        val name  = "test"
        val key   = "key3"
        val value = "value"
        prefNodeService.create(name, key, value) match
            case Left(e)         => fail(e.getMessage)
            case Right(prefNode) =>
                prefNodeService.delete(name, key)
                prefNodeService.findByNodeNameAndKey(name, key) match
                    case Left(e)               => fail(e.getMessage)
                    case Right(None)           => // expected
                    case Right(Some(prefNode)) => fail(s"Expected None but got $prefNode")
    }

    test("findByNodeNameAndKey") {
        val name  = "test"
        val key   = "key4"
        val value = "value"
        prefNodeService.create(name, key, value) match
            case Left(e)         => fail(e.getMessage)
            case Right(prefNode) =>
                prefNodeService.findByNodeNameAndKey(name, key) match
                    case Left(e)                    => fail(e.getMessage)
                    case Right(Some(foundPrefNode)) =>
                        assertEquals(foundPrefNode.name, name)
                        assertEquals(foundPrefNode.key, key)
                        assertEquals(foundPrefNode.value, value)
                    case Right(None)                => fail(s"Expected Some but got None")
    }

    test("findByNodeName") {
        val name  = "test999"
        val key   = "key5"
        val value = "value"
        prefNodeService.create(name, key, value) match
            case Left(e)         => fail(e.getMessage)
            case Right(prefNode) =>
                assertEquals(name, prefNode.name)
                assertEquals(key, prefNode.key)
                assertEquals(value, prefNode.value)
                prefNodeService.findByNodeName(name) match
                    case Left(e)          => fail(e.getMessage)
                    case Right(prefNodes) =>
                        assertEquals(1, prefNodes.size)
                        val foundPrefNode = prefNodes.head
                        assertEquals(name, foundPrefNode.name)
                        assertEquals(key, foundPrefNode.key)
                        assertEquals(value, foundPrefNode.value)
    }

    test("findByNodeNameLike") {
        val name  = "test000"
        val key   = "key6"
        val value = "value"
        prefNodeService.create(name, key, value) match
            case Left(e)         => fail(e.getMessage)
            case Right(prefNode) =>
                assertEquals(name, prefNode.name)
                assertEquals(key, prefNode.key)
                assertEquals(value, prefNode.value)
                prefNodeService.findByNodeNameLike(name) match
                    case Left(e)          => fail(e.getMessage)
                    case Right(prefNodes) =>
                        assertEquals(1, prefNodes.size)
                        val foundPrefNode = prefNodes.head
                        assertEquals(name, foundPrefNode.name)
                        assertEquals(key, foundPrefNode.key)
                        assertEquals(value, foundPrefNode.value)
    }
