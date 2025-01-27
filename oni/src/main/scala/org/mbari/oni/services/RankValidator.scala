/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2024
 *
 * oni code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential.
 */

package org.mbari.oni.services

import org.mbari.oni.domain.ConceptCreate
import org.mbari.oni.domain.ConceptUpdate

object RankValidator:

    val ValidRankLevelsAndNames: Seq[(Option[String], Option[String])] = Seq(
        (None, None),
        (None, Some("realm")),
        (Some("sub"), Some("realm")),
        (None, Some("kingdom")),
        (Some("sub"), Some("kingdom")),
        (None, Some("phylum")),
        (Some("sub"), Some("phylum")),
        (None, Some("division")),
        (Some("sub"), Some("division")),
        (None, Some("class")),
        (Some("sub"), Some("class")),
        (Some("super"), Some("order")),
        (None, Some("order")),
        (Some("sub"), Some("order")),
        (Some("infra"), Some("order")),
        (Some("super"), Some("family")),
        (Some("epi"), Some("family")),
        (None, Some("family")),
        (Some("sub"), Some("family")),
        (Some("infra"), Some("family")),
        (None, Some("tribe")),
        (Some("sub"), Some("tribe")),
        (Some("infra"), Some("tribe")),
        (None, Some("genus")),
        (Some("sub"), Some("genus")),
        (None, Some("section")),
        (Some("sub"), Some("section")),
        (None, Some("species complex")),
        (None, Some("species")),
        (Some("sub"), Some("species")),
        (None, Some("variety")),
        (None, Some("form"))
    )

    /**
     * A list of commonlhy accepted, valid ranks scraped from Wikipedia
     */
    val ValidRanks: Seq[String] = ValidRankLevelsAndNames.map {
        (rankLevel, rankName) => s"${rankLevel.getOrElse("")}${rankName.getOrElse("")}".toLowerCase
    }

    def validate(rankLevel: Option[String] = None, rankName: Option[String] = None): Boolean = {
        val rank = s"${rankLevel.getOrElse("")}${rankName.getOrElse("")}".toLowerCase
        ValidRanks.contains(rank)
    }

    def validate(conceptCreate: ConceptCreate): Boolean = {
        validate(conceptCreate.rankLevel, conceptCreate.rankName)
    }

     def validate(conceptUpdate: ConceptUpdate): Boolean = {
        validate(conceptUpdate.rankLevel, conceptUpdate.rankName)
    }

    def throwExceptionIfInvalid(conceptCreate: ConceptCreate): Unit = {
        if (!validate(conceptCreate)) {
            val rank = s"${conceptCreate.rankLevel.getOrElse("")}${conceptCreate.rankName.getOrElse("")}"
            throw new IllegalArgumentException(
                s"Invalid rank level + rank name ($rank). Should be one of ${RankValidator.ValidRanks.mkString(", ")}"
            )
        }
    }

    def throwExceptionIfInvalid(conceptUpdate: ConceptUpdate): Unit = {
        if (!validate(conceptUpdate.rankLevel, conceptUpdate.rankName)) {
            val rank = s"${conceptUpdate.rankLevel.getOrElse("")}${conceptUpdate.rankName.getOrElse("")}"
            throw new IllegalArgumentException(
                s"Invalid rank level + rank name ($rank). Should be one of ${RankValidator.ValidRanks.mkString(", ")}"
            )
        }

    }


