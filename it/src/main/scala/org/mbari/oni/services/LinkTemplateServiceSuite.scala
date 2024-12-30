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

import org.mbari.oni.domain.{ExtendedLink, Link, LinkCreate, LinkRenameToConceptRequest, LinkUpdate}
import org.mbari.oni.etc.jdk.Strings
import org.mbari.oni.jpa.DataInitializer

import scala.jdk.CollectionConverters.*

trait LinkTemplateServiceSuite extends DataInitializer with UserAuthMixin:

    lazy val linkTemplateService: LinkTemplateService = new LinkTemplateService(entityManagerFactory)

    test("findById") {
        val root = init(3, 4)
        assert(root != null)
        root.getDescendants
            .stream()
            .flatMap(_.getConceptMetadata.getLinkTemplates.stream())
            .forEach(linkTemplate =>
                val id       = linkTemplate.getId
                val expected = ExtendedLink.from(linkTemplate)
                linkTemplateService.findById(id) match
                    case Right(obtained) => assertEquals(obtained, expected)
                    case Left(error)     => fail(error.toString)
            )
    }

    test("findByConcept") {
        val root             = init(3, 4)
        assert(root != null)
        val descendants      = root.getDescendants.asScala
        val allNames         = descendants.flatMap(_.getConceptNames.asScala.map(_.getName)).toSeq.sorted
        val allLinkTemplates = descendants.flatMap(_.getConceptMetadata.getLinkTemplates.asScala).toSeq
        for name <- allNames
        do
            linkTemplateService.findByConcept(name) match
                case Right(obtained) =>
                    val expected = allLinkTemplates
                        .filter(t => t.getConceptMetadata.getConcept.getConceptNames.asScala.exists(_.getName == name))
                        .map(ExtendedLink.from)
                        .sortBy(_.linkName)
                    assertEquals(obtained.sortBy(_.linkName), expected)
                case Left(error)     => fail(error.toString)

    }

    test("findByPrototype") {
        val root             = init(3, 4)
        assert(root != null)
        val descendants      = root.getDescendants.asScala
        val allLinkTemplates = descendants.flatMap(_.getConceptMetadata.getLinkTemplates.asScala).toSeq
        for linkTemplate <- allLinkTemplates
        do
            val link = Link.from(linkTemplate)
            linkTemplateService.findByPrototype(link) match
                case Right(obtained) =>
                    val expected = allLinkTemplates
                        .filter(t =>
                            t.getLinkName == link.linkName && t.getLinkValue == link.linkValue && t.getToConcept == link.toConcept
                        )
                        .map(ExtendedLink.from)
                        .sortBy(_.linkName)
                    assertEquals(obtained.sortBy(_.linkName), expected)
                case Left(error)     => fail(error.toString)
    }

    test("countByToConcept") {
        val root             = init(3, 10)
        assert(root != null)
        val descendants      = root.getDescendants.asScala
        val allLinkTemplates = descendants.flatMap(_.getConceptMetadata.getLinkTemplates.asScala).toSeq
        for linkTemplate <- allLinkTemplates
        do
            val toConcept = linkTemplate.getToConcept
            linkTemplateService.countByToConcept(toConcept) match
                case Right(obtained) =>
                    val expected = allLinkTemplates.count(t => t.getToConcept == toConcept)
                    assertEquals(obtained, expected.toLong)
                case Left(error)     => fail(error.toString)
    }

    test("findByToConcept") {
        val root             = init(3, 10)
        assert(root != null)
        val descendants      = root.getDescendants.asScala
        val allLinkTemplates = descendants.flatMap(_.getConceptMetadata.getLinkTemplates.asScala).toSeq
        for linkTemplate <- allLinkTemplates
        do
            val toConcept = linkTemplate.getToConcept
            linkTemplateService.findByToConcept(toConcept) match
                case Right(obtained) =>
                    val expected = allLinkTemplates.filter(t => t.getToConcept == toConcept).map(ExtendedLink.from)
                    assertEquals(obtained.sortBy(_.linkName), expected.sortBy(_.linkName))
                case Left(error)     => fail(error.toString)
    }

    test("renameToConcept") {
        val root             = init(3, 10)
        assert(root != null)
        val descendants      = root.getDescendants.asScala
        val allLinkTemplates = descendants.flatMap(_.getConceptMetadata.getLinkTemplates.asScala).toSeq
        val request = LinkRenameToConceptRequest(allLinkTemplates.head.getToConcept, Strings.random(10))
        val attempt = runWithUserAuth(user => linkTemplateService.renameToConcept(request.old, request.`new`, user.username))
        attempt match
            case Right(obtained) =>
                val expected = allLinkTemplates.count(t => t.getToConcept == request.old)
                assertEquals(obtained.count, expected)
            case Left(error)     => fail(error.toString)
    }

    test("create") {
        val root       = init(3, 0)
        assert(root != null)
        val linkCreate = LinkCreate(root.getPrimaryConceptName.getName, "linkName", "linkValue", "toConcept")
        val attempt    = runWithUserAuth(user => linkTemplateService.create(linkCreate, user.username))
        attempt match
            case Right(extendedLink) =>
                val obtained = extendedLink
                assertEquals(obtained.linkName, linkCreate.linkName)
                assertEquals(obtained.linkValue, linkCreate.linkValue)
                assertEquals(obtained.toConcept, linkCreate.toConcept)
                assert(obtained.id.isDefined)
            case Left(error)         => fail(error.toString)
    }

    test("update") {
        val root             = init(3, 3)
        assert(root != null)
        val descendants      = root.getDescendants.asScala
        val allLinkTemplates = descendants.flatMap(_.getConceptMetadata.getLinkTemplates.asScala).toSeq
        for linkTemplate <- allLinkTemplates
        do
            val linkUpdate = LinkUpdate(Some(Strings.random(10)), Some(Strings.random(10)), Some(Strings.random(10)))
            val attempt    =
                runWithUserAuth(user => linkTemplateService.updateById(linkTemplate.getId, linkUpdate, user.username))
            attempt match
                case Right(obtained) =>
                    assertEquals(obtained.linkName, linkUpdate.linkName.get)
                    assertEquals(obtained.linkValue, linkUpdate.linkValue.get)
                    assertEquals(obtained.toConcept, linkUpdate.toConcept.get)
                    assert(obtained.id.isDefined)
                    assertEquals(obtained.id.get, linkTemplate.getId.longValue())
                case Left(error)     => fail(error.toString)
    }

    test("deleteById") {
        val root             = init(3, 3)
        assert(root != null)
        val descendants      = root.getDescendants.asScala
        val allLinkTemplates = descendants.flatMap(_.getConceptMetadata.getLinkTemplates.asScala).toSeq
        for linkTemplate <- allLinkTemplates
        do
            val id      = linkTemplate.getId
            val attempt = runWithUserAuth(user => linkTemplateService.deleteById(id, user.username))
            attempt match
                case Right(_)    =>
                    println(s"Deleted LinkTemplate with id $id")
                    linkTemplateService.findById(id) match
                        case Right(_)    => fail(s"LinkTemplate with id $id was not deleted")
                        case Left(error) => // Expected
                case Left(error) => fail(error.toString)

    }
