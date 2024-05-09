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

import org.mbari.oni.domain.{ExtendedHistory, RawConcept}
import org.mbari.oni.jpa.{DataInitializer, DatabaseFunSuite}

import scala.jdk.CollectionConverters.*

trait HistoryServiceSuite extends DataInitializer:

    lazy val historyService = new HistoryService(entityManagerFactory)

    test("findAllPending") {
        val root     = init(3, 6)
        assert(root != null)
        val expected = root
            .getDescendants
            .asScala
            .flatMap(ExtendedHistory.from)
            .toSet
            .filter(_.processedTimestamp.isEmpty)
        historyService.findAllPending() match
            case Left(e)         => fail(e.getMessage)
            case Right(obtained) =>
                assertEquals(expected.size, obtained.size)
//                assertEquals(expected, obtained)
    }

    test("findAllApproved") {
        val root     = init(3, 6)
        assert(root != null)
        val expected = root
            .getDescendants
            .asScala
            .flatMap(ExtendedHistory.from)
            .toSet
            .filter(_.approved)
        historyService.findAllApproved() match
            case Left(e)         => fail(e.getMessage)
            case Right(obtained) =>
                assertEquals(expected.size, obtained.size)
    }
