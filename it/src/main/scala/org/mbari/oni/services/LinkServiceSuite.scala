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

import org.mbari.oni.domain.{ExtendedLink, Link}
import org.mbari.oni.jpa.{DataInitializer, DatabaseFunSuite}

import scala.jdk.CollectionConverters.*

trait LinkServiceSuite extends DataInitializer:

    lazy val linkService = new LinkService(entityManagerFactory)

    test("findAllLinkTemplates") {
        val root     = init(3, 3)
        assert(root != null)
        val expected = root
            .getDescendants
            .asScala
            .flatMap(_.getConceptMetadata.getLinkTemplates.asScala)
            .toSeq
            .sortBy(_.getLinkName)
            .map(Link.from)
        linkService.findAllLinkTemplates() match
            case Left(e)       => fail(e.getMessage)
            case Right(actual) =>
                val obtained = actual.sortBy(_.linkName)
                assertEquals(expected.size, obtained.size)
                assertEquals(expected, obtained)
    }

    test("findAllLinkTemplatesForConcept") {
        val root     = init(3, 3)
        assert(root != null)
        val expected = root.getConceptMetadata.getLinkTemplates.asScala.map(Link.from).toSeq.sortBy(_.linkName)
        linkService.findAllLinkTemplatesForConcept(root.getPrimaryConceptName.getName) match
            case Left(e)       => fail(e.getMessage)
            case Right(actual) =>
                val obtained = actual.sortBy(_.linkName)
                assertEquals(expected.size, obtained.size)
                assertEquals(expected, obtained)
    }

    test("findLinkTemplatesByNameForConcept") {
        val root = init(3, 3)
        assert(root != null)
        val opt  = root
            .getDescendants
            .asScala
            .flatMap(_.getConceptMetadata.getLinkTemplates.asScala)
            .toSeq
            .sortBy(_.getLinkName)
            .map(ExtendedLink.from)
            .headOption
        opt match
            case Some(expected) =>
                linkService.findLinkTemplatesByNameForConcept(expected.concept, expected.linkName) match
                    case Left(e)       => fail(e.getMessage)
                    case Right(actual) =>
                        assertEquals(expected.toLink, actual.head.copy(id = None))
            case None           => fail("No link templates found")
    }

    test("findLinkRealizationsByLinkName") {
        val root = init(3, 3)
        assert(root != null)
        val opt  = root
            .getDescendants
            .asScala
            .flatMap(_.getConceptMetadata.getLinkRealizations.asScala)
            .toSeq
            .sortBy(_.getLinkName)
            .map(ExtendedLink.from)
            .headOption
        opt match
            case Some(expected) =>
                linkService.findLinkRealizationsByLinkName(expected.linkName) match
                    case Left(e)       => fail(e.getMessage)
                    case Right(actual) =>
                        assertEquals(actual.head, expected)
            case None           => fail("No link templates found")
    }
