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

import org.mbari.oni.domain.{ConceptNameCreate, LinkRenameToConceptRequest}
import org.mbari.oni.etc.jdk.Strings
import org.mbari.oni.jpa.ChangeEvent.{Action, DataType}
import org.mbari.oni.jpa.EntityManagerFactories.*
import org.mbari.oni.domain.ConceptNameTypes
import org.mbari.oni.jpa.entities.ConceptNameEntity
import org.mbari.oni.jpa.repositories.ConceptRepository
import org.mbari.oni.jpa.{ChangeBus, ChangeEvent, DataInitializer}

import java.util.concurrent.CopyOnWriteArrayList
import scala.jdk.CollectionConverters.*

/**
 * Verifies that committed changes are published on the ChangeBus (and so clear the ConceptCache), and that rolled back
 * transactions publish nothing.
 */
trait ChangeNotificationSuite extends DataInitializer with UserAuthMixin:

    lazy val conceptNameService: ConceptNameService                 = new ConceptNameService(entityManagerFactory)
    lazy val linkTemplateService: LinkTemplateService               = new LinkTemplateService(entityManagerFactory)
    lazy val linkRealizationService: LinkRealizationService         = new LinkRealizationService(entityManagerFactory)

    /** Runs fn and returns the events published on the ChangeBus while it ran. */
    private def capture[T](fn: => T): (T, Seq[ChangeEvent]) =
        val events      = new CopyOnWriteArrayList[ChangeEvent]()
        val unsubscribe = ChangeBus.subscribe(e => events.add(e))
        try
            val result = fn
            (result, events.asScala.toSeq)
        finally unsubscribe.run()

    test("committed inserts are published") {
        val (root, events) = capture(init(2, 2))
        assert(events.exists(e => e.action() == Action.CREATED && e.dataType() == DataType.CONCEPT))
        assert(events.exists(e => e.action() == Action.CREATED && e.dataType() == DataType.CONCEPT_NAME))
    }

    test("rolled back transactions publish nothing") {
        init(1, 0)
        val (result, events) = capture(
            entityManagerFactory.transaction(em =>
                val root   = new ConceptRepository(em).findRoot().get()
                val entity = new ConceptNameEntity()
                entity.setName(Strings.random(12))
                entity.setNameType(ConceptNameTypes.SYNONYM.getType)
                root.addConceptName(entity)
                em.flush() // The insert succeeds in the database transaction...
                throw new RuntimeException("force rollback") // ...and then we roll back
            )
        )
        // Make sure we rolled back because of our exception and not because the flush failed
        assertEquals(result.left.toOption.map(_.getMessage), Some("force rollback"))
        assert(events.isEmpty, s"Expected no events but got $events")
    }

    test("read-only transactions publish nothing") {
        val root             = init(2, 2)
        val (_, events)      = capture(conceptService.findByName(root.getPrimaryConceptName.getName))
        assert(events.isEmpty, s"Expected no events but got $events")
    }

    test("ConceptCache is cleared when a concept name is added through the service") {
        val root  = init(2, 2)
        val name  = root.getPrimaryConceptName.getName
        val cache = new ConceptCache(conceptService, conceptNameService)
        try
            val before = cache.findByName(name).toOption.get
            val names0 = cache.findAllNames(1000000, 0).toOption.get
            val synonym = Strings.random(12)
            val dto     = ConceptNameCreate(name, synonym, ConceptNameTypes.SYNONYM.getType)
            runWithUserAuth(user => conceptNameService.addName(dto, user.username)) match
                case Left(e)  => fail(e.toString)
                case Right(_) =>
                    // No explicit cache.clear(): the post-commit listener must have done it
                    val after = cache.findByName(name).toOption.get
                    assert(after.alternateNames.contains(synonym), "findByName served a stale entry")
                    assert(!before.alternateNames.contains(synonym))
                    assert(cache.findAllNames(1000000, 0).toOption.get.contains(synonym), "findAllNames served a stale entry")
                    assert(!names0.contains(synonym))
        finally cache.close()
    }

    test("LinkRealizationService.renameToConcept publishes an explicit event") {
        val root                = init(3, 10)
        val allLinkRealizations = root.getDescendants.asScala.flatMap(_.getConceptMetadata.getLinkRealizations.asScala).toSeq
        val request             = LinkRenameToConceptRequest(allLinkRealizations.head.getToConcept, Strings.random(10))
        val (attempt, events)   = capture(
            runWithUserAuth(user => linkRealizationService.renameToConcept(request.old, request.`new`, user.username))
        )
        assert(attempt.exists(_.count > 0))
        // Native bulk updates skip Hibernate, so this can only come from the explicit publish
        assert(events.exists(e => e.dataType() == DataType.LINK_REALIZATION && e.action() == Action.UPDATED))
    }

    test("LinkRealizationService.renameToConcept clears the ConceptCache") {
        val root                = init(3, 10)
        val cache               = new ConceptCache(conceptService, conceptNameService)
        try
            val withLinks = root.getDescendants.asScala.filter(!_.getConceptMetadata.getLinkRealizations.isEmpty).head
            val name      = withLinks.getPrimaryConceptName.getName
            val toConcept = withLinks.getConceptMetadata.getLinkRealizations.asScala.head.getToConcept
            val newName   = Strings.random(10)
            assert(cache.findByName(name).toOption.get.linkRealizations.exists(_.toConcept == toConcept))
            runWithUserAuth(user => linkRealizationService.renameToConcept(toConcept, newName, user.username)) match
                case Left(e)  => fail(e.toString)
                case Right(_) =>
                    val links = cache.findByName(name).toOption.get.linkRealizations
                    assert(links.exists(_.toConcept == newName), "ConceptCache served stale link realizations")
        finally cache.close()
    }

    test("LinkTemplateService.renameToConcept publishes an explicit event") {
        val root                = init(3, 10)
        val allLinkTemplates    = root.getDescendants.asScala.flatMap(_.getConceptMetadata.getLinkTemplates.asScala).toSeq
        val request             = LinkRenameToConceptRequest(allLinkTemplates.head.getToConcept, Strings.random(10))
        val (attempt, events)   = capture(
            runWithUserAuth(user => linkTemplateService.renameToConcept(request.old, request.`new`, user.username))
        )
        assert(attempt.exists(_.count > 0))
        assert(events.exists(e => e.dataType() == DataType.LINK_TEMPLATE && e.action() == Action.UPDATED))
    }
