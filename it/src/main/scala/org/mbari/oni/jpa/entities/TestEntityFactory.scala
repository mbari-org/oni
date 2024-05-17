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

package org.mbari.oni.jpa.entities

import org.mbari.oni.domain.{ConceptNameTypes, MediaTypes}
import org.mbari.oni.etc.jdk.Strings

import java.time.Instant
import java.util.Date
import java.util.concurrent.atomic.AtomicLong

object TestEntityFactory:

    private val random        = new scala.util.Random
    private val nextConceptId = new AtomicLong(0)

    def buildRoot(depth: Int = 1, maxBreadth: Int = 0): ConceptEntity =
        val root    = buildNode(maxBreadth)
        val synonym = new ConceptNameEntity("root", ConceptNameTypes.SYNONYM.getType)
        val common  = new ConceptNameEntity("object", ConceptNameTypes.COMMON.getType)
        root.addConceptName(synonym)
        root.addConceptName(common)
        if depth > 1 then buildTree(root, depth - 1, maxBreadth)
        root

    private def buildTree(parent: ConceptEntity, depth: Int = 0, maxBreadth: Int = 0): Unit =
        if depth > 0 then
            val numberChildren = if maxBreadth > 1 then random.between(1, maxBreadth + 1) else 1
            for _ <- 0 until numberChildren do
                val entity = buildNode(maxBreadth)
                if entity.getRankLevel != null then // Add a numbert depth to the rank level
                    entity.setRankLevel(s"$depth--${entity.getRankLevel}")
//                println(s"------- Adding " + entity.getPrimaryConceptName.getName + " to " + parent.getPrimaryConceptName.getName)
                parent.addChildConcept(entity)
                buildTree(entity, depth - 1, maxBreadth)

    def buildNode(maxBreadth: Int): ConceptEntity =
        val entity   = createConcept()
        val metadata = entity.getConceptMetadata

        if maxBreadth > 0 then

            val conceptNameBreadth = random.between(0, maxBreadth + 1)
            if conceptNameBreadth > 0 then
                for _ <- 0 until conceptNameBreadth do
                    val conceptName = createConceptName(false)
                    entity.addConceptName(conceptName)

            val mediaBreadth = random.between(1, maxBreadth + 1)
            for _ <- 0 until mediaBreadth do
                val media = createMedia()
                metadata.addMedia(media)

            val linkTemplateBreadth = random.between(1, maxBreadth + 1)
            for _ <- 0 until linkTemplateBreadth do
                val linkTemplate = createLinkTemplate()
                metadata.addLinkTemplate(linkTemplate)

            val linkRealizationBreadth = random.between(1, maxBreadth + 1)
            for _ <- 0 until linkRealizationBreadth do
                val linkRealization = createLinkRealization()
                metadata.addLinkRealization(linkRealization)

            val historyBreadth = random.between(1, maxBreadth + 1)
            for _ <- 0 until historyBreadth do
                val history = createHistory()
                metadata.addHistory(history)

        entity

    def createConceptName(isPrimary: Boolean = true): ConceptNameEntity =
        val entity   = new ConceptNameEntity()
        entity.setName(Strings.random(20))
        val nameType =
            if isPrimary then ConceptNameTypes.PRIMARY
            else
                val i = random.nextInt(4)
                i match
                    case 0 => ConceptNameTypes.ALTERNATE
                    case 1 => ConceptNameTypes.COMMON
                    case 2 => ConceptNameTypes.FORMER
                    case 3 => ConceptNameTypes.SYNONYM
        entity.setNameType(nameType.getType)
        entity

    def createMedia(): MediaEntity =
        val entity = new MediaEntity()
        val url    = s"https://www.mbari.org/oni/${Strings.random(10)}/${Strings.random(50)}.jpg"
        entity.setUrl(url)
        entity.setType(MediaTypes.IMAGE.getType)
        if random.nextBoolean() then entity.setCaption(Strings.random(100))
        if random.nextBoolean() then entity.setCredit(Strings.random(50))
        entity

    def createLinkTemplate(): LinkTemplateEntity =
        val entity = new LinkTemplateEntity(
            Strings.random(20),
            Strings.random(50),
            Strings.random(50)
        )
        entity

    def createLinkRealization(): LinkRealizationEntity =
        val entity = new LinkRealizationEntity(
            Strings.random(20),
            Strings.random(50),
            Strings.random(50)
        )
        entity

    def createHistory(): HistoryEntity =
        val entity = new HistoryEntity()
        entity.setCreationDate(Date.from(Instant.now()))
        entity.setCreatorName(Strings.random(20))
        entity.setField("ConceptName")
        entity.setAction(HistoryEntity.ACTION_REPLACE)
        entity.setOldValue(Strings.random(20))
        entity.setNewValue(Strings.random(20))
        if random.nextBoolean() then
            entity.setApproved(random.nextBoolean())
            entity.setProcessedDate(Date.from(Instant.now()))
            entity.setProcessorName(Strings.random(20))
        entity

    def createConceptMetadata(): ConceptMetadataEntity =
        val entity = new ConceptMetadataEntity()
        entity

    def createConcept(): ConceptEntity =
        val entity = new ConceptEntity()
        entity.addConceptName(createConceptName())
        entity.setConceptMetadata(createConceptMetadata())
        if random.nextBoolean() then
            entity.setRankLevel(Strings.random(6))
            entity.setRankName(Strings.random(12))

        // DON'T DO THIS. The ID should be assigned by the database. Otherwise inserts will fail.
//        entity.setId(nextConceptId.incrementAndGet())
        entity
