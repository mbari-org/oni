/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2024
 *
 * oni code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential.
 */

package org.mbari.oni.services

import org.mbari.oni.domain.{ConceptCreate, ConceptUpdate}

object RankValidator:

    // Scraped from https://en.wikipedia.org/wiki/Taxonomic_rank
    val ValidRankLevelsAndNames: Seq[(Option[String], Option[String])] = Seq(
        (None, None),
        (None, Some("domain")),
        (Some("sub"), Some("domain")),
        (None, Some("realm")), // virology
        (Some("sub"), Some("realm")),
        (Some("super"), Some("kingdom")),
        (None, Some("kingdom")),
        (Some("sub"), Some("kingdom")),
        (Some("infra"), Some("kingdom")),
        (Some("super"), Some("phylum")),
        (None, Some("phylum")),
        (Some("sub"), Some("phylum")),
        (Some("infra"), Some("phylum")),
        (Some("micro"), Some("phylum")),
        (Some("super"), Some("class")),
        (None, Some("class")),
        (Some("infra"), Some("class")),
        (Some("sub"), Some("class")),
        (Some("super"), Some("division")),
        (None, Some("division")),
        (Some("sub"), Some("division")),
        (Some("infra"), Some("division")),
        (Some("super"), Some("order")),
        (None, Some("order")),
        (Some("sub"), Some("order")),
        (Some("infra"), Some("order")),
        (None, Some("section")),
        (Some("sub"), Some("section")),
        (Some("super"), Some("family")),
        (Some("epi"), Some("family")),
        (None, Some("family")),
        (Some("sub"), Some("family")),
        (Some("infra"), Some("family")),
        (Some("super"), Some("tribe")),
        (None, Some("tribe")),
        (Some("sub"), Some("tribe")),
        (Some("infra"), Some("tribe")),
        (Some("super"), Some("genus")),
        (None, Some("genus")),
        (Some("sub"), Some("genus")),
        (None, Some("species complex")),
        (None, Some("species")),
        (Some("sub"), Some("species")),
        (None, Some("variety")),
        (Some("sub"), Some("variety")),
        (None, Some("form"))
    )

    /**
     * A list of commonlhy accepted, valid ranks scraped from Wikipedia
     */
    val ValidRanks: Seq[String] = ValidRankLevelsAndNames.map { (rankLevel, rankName) =>
        s"${rankLevel.getOrElse("")}${rankName.getOrElse("")}".toLowerCase
    }

    def validate(rank: String): Boolean =
        if rank == null || rank.isEmpty then return true
        ValidRanks.contains(rank)

    def validate(rankLevel: Option[String] = None, rankName: Option[String] = None): Boolean =
        val rank = s"${rankLevel.getOrElse("")}${rankName.getOrElse("")}".toLowerCase
        validate(rank)

    def validate(conceptCreate: ConceptCreate): Boolean =
        validate(conceptCreate.rankLevel, conceptCreate.rankName)

    def validate(conceptUpdate: ConceptUpdate): Boolean =
        validate(conceptUpdate.rankLevel, conceptUpdate.rankName)

    def throwExceptionIfInvalid(rank: String): Unit =
        if !validate(rank) then
            throw new IllegalArgumentException(
                s"Invalid rank ($rank). Should be one of ${RankValidator.ValidRanks.mkString(", ")}"
            )

    def throwExceptionIfInvalid(conceptCreate: ConceptCreate): Unit =
        if !validate(conceptCreate) then
            val rank = s"${conceptCreate.rankLevel.getOrElse("")}${conceptCreate.rankName.getOrElse("")}"
            throw new IllegalArgumentException(
                s"Invalid rank level + rank name ($rank). Should be one of ${RankValidator.ValidRanks.mkString(", ")}"
            )

    def throwExceptionIfInvalid(conceptUpdate: ConceptUpdate): Unit =
        if !validate(conceptUpdate.rankLevel, conceptUpdate.rankName) then
            val rank = s"${conceptUpdate.rankLevel.getOrElse("")}${conceptUpdate.rankName.getOrElse("")}"
            throw new IllegalArgumentException(
                s"Invalid rank level + rank name ($rank). Should be one of ${RankValidator.ValidRanks.mkString(", ")}"
            )
