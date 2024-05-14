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

package org.mbari.oni.jdbc

import org.mbari.oni.domain.Concept
import org.mbari.oni.jpa.DataInitializer

import scala.jdk.CollectionConverters.*
import org.mbari.oni.etc.circe.CirceCodecs.{*, given}
import org.mbari.oni.etc.jdk.Loggers.given

import scala.annotation.tailrec

trait FastPhylogenyServiceSuite extends DataInitializer {

    lazy val fastPhylogenyService = new FastPhylogenyService(entityManagerFactory)

    test("findLastUpdate") {
        val root = init(4, 2)
        val lastUpdate = fastPhylogenyService.findLastUpdate()
        assert(lastUpdate != null)
    }

    test("findUp") {
        val root = init(5, 3)

        @tailrec
        def lastConcept(concept: Concept): Concept =
            if (concept.children.isEmpty) concept
            else lastConcept(concept.children.head)

        val concept = Concept.from(root)
        val expectedLast = lastConcept(concept)
        val opt = fastPhylogenyService.findUp(expectedLast.name)
        assert(opt.isDefined)
        val obtained = opt.get
        assertEquals(obtained.name, root.getPrimaryConceptName.getName)
        val obtainedLast = lastConcept(obtained)
        assertEquals(obtainedLast.name, expectedLast.name)
        log.atDebug.log("---FIND UP: " + opt.get.stringify)
    }

    test("findDown") {
        val root = init(5, 3)
        log.atDebug.log("---FIND DOWN - source data: " + Concept.from(root).stringify)
        val opt = fastPhylogenyService.findDown(root.getPrimaryConceptName.getName)
        assert(opt.isDefined)
        log.atDebug.log("---FIND DOWN - query results: " + opt.get.stringify)
    }

    test("findSiblings") {
        val root = init(2, 5)
        val nSiblings = root.getChildConcepts.size()
        assert(nSiblings > 0)
        val candidate = root.getChildConcepts.iterator().next()
        val siblings = fastPhylogenyService.findSiblings(candidate.getPrimaryConceptName.getName)
        assertEquals(siblings.size, nSiblings)
        println(siblings.stringify)
    }

    test("findDescendantNames") {
        val root = init(4, 2)
        val expected = root.getDescendants
            .asScala
            .flatMap(_.getConceptNames.asScala.map(_.getName))
            .toSeq
            .sorted

        val obtained = fastPhylogenyService.findDescendantNames(root.getPrimaryConceptName.getName)
            .sorted
        assertEquals(obtained, expected)

    }

}
