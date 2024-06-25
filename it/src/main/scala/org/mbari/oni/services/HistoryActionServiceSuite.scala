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

import org.mbari.oni.domain.{ConceptCreate, ConceptNameCreate, ConceptNameTypes, ConceptUpdate, LinkCreate, MediaCreate, UserAccountRoles}
import org.mbari.oni.etc.jdk.Strings
import org.mbari.oni.jdbc.FastPhylogenyService
import org.mbari.oni.jpa.DataInitializer

import java.net.URI
import scala.jdk.CollectionConverters.*

trait HistoryActionServiceSuite extends DataInitializer with UserAuthMixin:

    lazy val fastPhylogenyService = new FastPhylogenyService(entityManagerFactory)
    lazy val historyService       = new HistoryService(entityManagerFactory)
    lazy val historyActionService = new HistoryActionService(entityManagerFactory, fastPhylogenyService)

    test("approveAddConceptChild") {
        val root           = init(1, 0)
        val conceptCreate  = ConceptCreate(
            Strings.random(10),
            Some(root.getName)
        )
        val conceptService = ConceptService(entityManagerFactory)
        val attempt = for
            conceptMetadata <- runWithUserAuth(
                                   user => conceptService.create(conceptCreate, user.username),
                                   role = UserAccountRoles.MAINTENANCE.getRoleName
                               )
            history         <- historyService.findByConceptName(root.getName).map(_.head)
            approvedHistory <- runWithUserAuth(user => historyActionService.approve(history.id.get, user.username))
        yield
            assert(approvedHistory.approved)

        attempt match
            case Right(_) => // Succeed
            case Left(e)  => fail(e.getMessage)
    }

    test("rejectAddConceptChild") {
        val root           = init(1, 0)
        val conceptCreate  = ConceptCreate(
            Strings.random(10),
            Some(root.getName)
        )
        val conceptService = ConceptService(entityManagerFactory)
        val attempt = for
            conceptMetadata <- runWithUserAuth(
                                   user => conceptService.create(conceptCreate, user.username),
                                   role = UserAccountRoles.MAINTENANCE.getRoleName
                               )
            history         <- historyService.findByConceptName(root.getName).map(_.head)
            approvedHistory <- runWithUserAuth(user => historyActionService.reject(history.id.get, user.username))
        yield
            assert(!approvedHistory.approved)
            conceptService.findByName(conceptCreate.name) match
                case Right(_) => fail("Concept should not exist after rejection")
                case Left(_)  => // Succeed

        attempt match
            case Right(_) => // Succeed
            case Left(e)  =>
                fail(e.getMessage)
    }

    test("approveAddConceptName") {
        val root = init(1, 0)
        val add = ConceptNameCreate(root.getName, Strings.random(10), ConceptNameTypes.SYNONYM.getType)
        val conceptNameService = ConceptNameService(entityManagerFactory)
        val conceptService = ConceptService(entityManagerFactory)
        val attempt = for
            _ <- runWithUserAuth(user => conceptNameService.addName(add, user.username), role = UserAccountRoles.MAINTENANCE.getRoleName)
            history <- historyService.findByConceptName(root.getName).map(_.head)
            approvedHistory <- runWithUserAuth(user => historyActionService.approve(history.id.get, user.username))
        yield
            assert(approvedHistory.approved)
            conceptService.findByName(add.newName) match
                case Right(_) => // Succeed
                case Left(_)  => fail("Concept name should exist after approval")

        attempt match
            case Right(_) => // Succeed
            case Left(e)  => fail(e.getMessage)
    }

    test("rejectAddConceptName") {
        val root = init(1, 0)
        val add = ConceptNameCreate(root.getName, Strings.random(10), ConceptNameTypes.SYNONYM.getType)
        val conceptNameService = ConceptNameService(entityManagerFactory)
        val conceptService = ConceptService(entityManagerFactory)
        val attempt = for
            _ <- runWithUserAuth(user => conceptNameService.addName(add, user.username), role = UserAccountRoles.MAINTENANCE.getRoleName)
            history <- historyService.findByConceptName(root.getName).map(_.head)
            approvedHistory <- runWithUserAuth(user => historyActionService.reject(history.id.get, user.username))
        yield
            assert(!approvedHistory.approved)
            conceptService.findByName(add.newName) match
                case Right(_) => fail("Concept name should not exist after rejection")
                case Left(_)  => // Succeed

        attempt match
            case Right(_) => // Succeed
            case Left(e)  => fail(e.getMessage)
    }

    test("approveAddLinkRealization") {
        val root = init(1, 0)
        val add = LinkCreate(
            root.getName,
            Strings.random(10),
            Strings.random(10),
            Strings.random(10)
        )
        val service = LinkRealizationService(entityManagerFactory)
        val conceptService = ConceptService(entityManagerFactory)
        val attempt = for
            _ <- runWithUserAuth(user => service.create(add, user.username), role = UserAccountRoles.MAINTENANCE.getRoleName)
            history <- historyService.findByConceptName(root.getName).map(_.head)
            approvedHistory <- runWithUserAuth(user => historyActionService.approve(history.id.get, user.username))
        yield
            assert(approvedHistory.approved)
            conceptService.findByName(root.getName) match
                case Right(concept) =>
                    assert(concept.linkRealizations.exists(_.linkName == add.linkName))
                case Left(_)     => fail("Concept should exist after approval")

        attempt match
            case Right(_) => // Succeed
            case Left(e)  => fail(e.getMessage)
    }

    test("rejectAddLinkRealization") {
        val root = init(1, 0)
        val add = LinkCreate(
            root.getName,
            Strings.random(10),
            Strings.random(10),
            Strings.random(10)
        )
        val service = LinkRealizationService(entityManagerFactory)
        val conceptService = ConceptService(entityManagerFactory)
        val attempt = for
            _ <- runWithUserAuth(user => service.create(add, user.username), role = UserAccountRoles.MAINTENANCE.getRoleName)
            history <- historyService.findByConceptName(root.getName).map(_.head)
            approvedHistory <- runWithUserAuth(user => historyActionService.reject(history.id.get, user.username))
        yield
            assert(!approvedHistory.approved)
            conceptService.findByName(root.getName) match
                case Right(concept) =>
                    assert(!concept.linkRealizations.exists(_.linkName == add.linkName))
                case Left(_)     => fail("Concept should exist after approval")

        attempt match
            case Right(_) => // Succeed
            case Left(e)  => fail(e.getMessage)
    }

    test("approveAddLinkTemplate") {
        val root = init(1, 0)
        val add = LinkCreate(
            root.getName,
            Strings.random(10),
            Strings.random(10),
            Strings.random(10)
        )
        val service = LinkTemplateService(entityManagerFactory)
        val conceptService = ConceptService(entityManagerFactory)
        val attempt = for
            _ <- runWithUserAuth(user => service.create(add, user.username), role = UserAccountRoles.MAINTENANCE.getRoleName)
            history <- historyService.findByConceptName(root.getName).map(_.head)
            approvedHistory <- runWithUserAuth(user => historyActionService.approve(history.id.get, user.username))
        yield
            assert(approvedHistory.approved)
            service.findByConcept(root.getName) match
                case Right(templates) =>
                    assert(templates.exists(_.linkName == add.linkName))
                case Left(_)          => fail("Link template should exist after approval")

        attempt match
            case Right(_) => // Succeed
            case Left(e)  => fail(e.getMessage)
    }

    test("rejectAddLinkTemplate") {
        val root = init(1, 0)
        val add = LinkCreate(
            root.getName,
            Strings.random(10),
            Strings.random(10),
            Strings.random(10)
        )
        val service = LinkTemplateService(entityManagerFactory)
        val conceptService = ConceptService(entityManagerFactory)
        val attempt = for
            _ <- runWithUserAuth(user => service.create(add, user.username), role = UserAccountRoles.MAINTENANCE.getRoleName)
            history <- historyService.findByConceptName(root.getName).map(_.head)
            approvedHistory <- runWithUserAuth(user => historyActionService.reject(history.id.get, user.username))
        yield
            assert(!approvedHistory.approved)
            service.findByConcept(root.getName) match
                case Right(templates) =>
                    assert(!templates.exists(_.linkName == add.linkName))
                case Left(_)          => fail("Link template should not exist after rejection")

        attempt match
            case Right(_) => // Succeed
            case Left(e)  =>
                fail(e.getMessage)
    }

    test("approveAddMedia") {
        val root = init(1, 0)
        val ref = s"https://www.mbari.org/${Strings.random(10)}/${Strings.random(10)}.jpg"
        val url = URI.create(ref).toURL
        val add = MediaCreate(root.getName, url)
        val service = MediaService(entityManagerFactory, fastPhylogenyService)
        val conceptService = ConceptService(entityManagerFactory)
        val attempt = for
            _ <- runWithUserAuth(user => service.create(add, user.username), role = UserAccountRoles.MAINTENANCE.getRoleName)
            history <- historyService.findByConceptName(root.getName).map(_.head)
            approvedHistory <- runWithUserAuth(user => historyActionService.approve(history.id.get, user.username))
        yield
            assert(approvedHistory.approved)
            conceptService.findByName(root.getName) match
                case Right(concept) =>
                    assert(concept.media.exists(_.url == url))
                case Left(_)     => fail("Concept should exist after approval")

        attempt match
            case Right(_) => // Succeed
            case Left(e)  => fail(e.getMessage)
    }

    test("rejectAddMedia") {
        val root = init(1, 0)
        val ref = s"https://www.mbari.org/${Strings.random(10)}/${Strings.random(10)}.jpg"
        val url = URI.create(ref).toURL
        val add = MediaCreate(root.getName, url)
        val service = MediaService(entityManagerFactory, fastPhylogenyService)
        val conceptService = ConceptService(entityManagerFactory)
        val attempt = for
            _ <- runWithUserAuth(user => service.create(add, user.username), role = UserAccountRoles.MAINTENANCE.getRoleName)
            history <- historyService.findByConceptName(root.getName).map(_.head)
            approvedHistory <- runWithUserAuth(user => historyActionService.reject(history.id.get, user.username))
        yield
            assert(!approvedHistory.approved)
            conceptService.findByName(root.getName) match
                case Right(concept) =>
                    assert(!concept.media.exists(_.url == url))
                case Left(_)     => fail("Concept should exist after approval")

        attempt match
            case Right(_) => // Succeed
            case Left(e)  => fail(e.getMessage)
    }

    test("approveReplaceParent") {
        val root = init(2, 6)
        val children = root.getChildConcepts.asScala.toSeq
        assert(children.size > 1)
        val a = children.head
        val b = children.last
        assertNotEquals(a.getName, b.getName)
        val service = ConceptService(entityManagerFactory)
        // move b from root to a
        val update = ConceptUpdate(Some(a.getName))
        val attempt = for
            _ <- runWithUserAuth(user => service.update(b.getName, update, user.username), role = UserAccountRoles.MAINTENANCE.getRoleName)
            history <- historyService.findByConceptName(b.getName).map(_.head)
            _ <- Right(println(history))
            approvedHistory <- runWithUserAuth(user => historyActionService.approve(history.id.get, user.username))
        yield
            assert(approvedHistory.approved)
            service.findChildrenByParentName(a.getName) match
                case Right(concepts) =>
                    val xs = concepts.filter(_.name == b.getName)
                    assert(xs.nonEmpty)
                case Left(_)     => fail("Concept should exist after approval")

        attempt match
            case Right(_) => // Succeed
            case Left(e)  =>
                fail(e.getMessage)
    }

    test("rejectReplaceParent") {
        val root = init(6, 2)
        val children = root.getChildConcepts.asScala.toSeq
        assert(children.size > 1)
        val a = children.head
        val b = children.last
        assertNotEquals(a.getName, b.getName)
        val service = ConceptService(entityManagerFactory)
        // move b from root to a
        val update = ConceptUpdate(Some(a.getName))
        val attempt = for
            _ <- runWithUserAuth(user => service.update(b.getName, update, user.username), role = UserAccountRoles.MAINTENANCE.getRoleName)
            history <- historyService.findByConceptName(b.getName).map(_.head)
            approvedHistory <- runWithUserAuth(user => historyActionService.reject(history.id.get, user.username))
        yield
            assert(!approvedHistory.approved)
            service.findChildrenByParentName(a.getName) match
                case Right(concepts) =>
                    val xs = concepts.filter(_.name == b.getName)
                    assert(xs.isEmpty)
                case Left(_)     => fail("Concept should exist after approval")

        attempt match
            case Right(_) => // Succeed
            case Left(e)  => fail(e.getMessage)
    }

    test("approveDeleteChildConcept") {
        val root = init(2, 0)
        val child = root.getChildConcepts.asScala.head
        val service = ConceptService(entityManagerFactory)
        val attempt = for
            _ <- runWithUserAuth(user => service.delete(child.getName, user.username), role = UserAccountRoles.MAINTENANCE.getRoleName)
            history <- historyService.findByConceptName(root.getName).map(_.head)
            approvedHistory <- runWithUserAuth(user => historyActionService.approve(history.id.get, user.username))
        yield
            assert(approvedHistory.approved)
            service.findByName(child.getName) match
                case Right(_) => fail("Concept should not exist after approval")
                case Left(_)  => // Succeed

        attempt match
            case Right(_) => // Succeed
            case Left(e)  => fail(e.getMessage)
    }

    test("rejectDeleteChildConcept") {
        val root = init(2, 0)
        val child = root.getChildConcepts.asScala.head
        val service = ConceptService(entityManagerFactory)
        val attempt = for
            _ <- runWithUserAuth(user => service.delete(child.getName, user.username), role = UserAccountRoles.MAINTENANCE.getRoleName)
            history <- historyService.findByConceptName(root.getName).map(_.head)
            approvedHistory <- runWithUserAuth(user => historyActionService.reject(history.id.get, user.username))
        yield
            assert(!approvedHistory.approved)
            service.findByName(child.getName) match
                case Right(_) => // Succeed
                case Left(_)  => fail("Concept should exist after rejection")

        attempt match
            case Right(_) => // Succeed
            case Left(e)  => fail(e.getMessage)
    }

    test("approveDeleteConceptName") {
        val root = init(2, 0)
        val conceptService = ConceptService(entityManagerFactory)
        val conceptNameService = ConceptNameService(entityManagerFactory)
        val conceptNameCreate = ConceptNameCreate(
            root.getName,
            Strings.random(10),
            ConceptNameTypes.SYNONYM.getType
        )

        // Add conceptname
        val attempt = for
            _ <- runWithUserAuth(user => conceptNameService.addName(conceptNameCreate, user.username))
            _ <- conceptService.findByName(conceptNameCreate.name)
            _ <- runWithUserAuth(user => conceptNameService.deleteName(conceptNameCreate.name, user.username), role = UserAccountRoles.MAINTENANCE.getRoleName)
            history <- historyService.findByConceptName(root.getName).map(_.head)
            approvedHistory <- runWithUserAuth(user => historyActionService.approve(history.id.get, user.username))
        yield
            assert(approvedHistory.approved)
            conceptService.findByName(conceptNameCreate.name) match
                case Right(_) => fail("Concept name should not exist after approval")
                case Left(_)  => // Succeed


        attempt match
            case Right(_) => // Succeed
            case Left(e)  => fail(e.getMessage)
    }

    test("rejectDeleteConceptName") {
        val root = init(2, 0)
        val conceptService = ConceptService(entityManagerFactory)
        val conceptNameService = ConceptNameService(entityManagerFactory)
        val conceptNameCreate = ConceptNameCreate(
            root.getName,
            Strings.random(10),
            ConceptNameTypes.SYNONYM.getType
        )

        // Add conceptname
        val attempt = for
            _ <- runWithUserAuth(user => conceptNameService.addName(conceptNameCreate, user.username))
            _ <- conceptService.findByName(conceptNameCreate.name)
            _ <- runWithUserAuth(user => conceptNameService.deleteName(conceptNameCreate.name, user.username), role = UserAccountRoles.MAINTENANCE.getRoleName)
            history <- historyService.findByConceptName(root.getName).map(_.head)
            approvedHistory <- runWithUserAuth(user => historyActionService.reject(history.id.get, user.username))
        yield
            assert(!approvedHistory.approved)
            conceptService.findByName(conceptNameCreate.name) match
                case Right(_) => // Succeed
                case Left(_)  => fail("Concept name should exist after rejection")

        attempt match
            case Right(_) => // Succeed
            case Left(e)  => fail(e.getMessage)
    }

    test("approveDeleteLinkRealization") {}

    test("rejectDeleteLinkRealization") {}

    test("approveDeleteLinkTemplate") {}

    test("rejectDeleteLinkTemplate") {}

    test("approveDeleteMedia") {}

    test("rejectDeleteMedia") {}
