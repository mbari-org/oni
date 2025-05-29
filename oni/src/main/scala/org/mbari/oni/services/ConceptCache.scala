/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2024
 *
 * oni code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential.
 */

/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2024
 *
 * oni code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential.
 */

package org.mbari.oni.services

import com.github.benmanes.caffeine.cache.{Cache, Caffeine}
import org.mbari.oni.domain.ConceptMetadata
import org.mbari.oni.etc.jdk.Loggers.given

import java.util.concurrent.TimeUnit

class ConceptCache(conceptService: ConceptService, conceptNameService: ConceptNameService):

    private val log = System.getLogger(getClass.getName)

    private val nameCache: Cache[String, ConceptMetadata] = Caffeine
        .newBuilder()
        .expireAfterWrite(15, TimeUnit.MINUTES)
        .build[String, ConceptMetadata]()

    private val allNamesCache: Cache[String, Seq[String]] = Caffeine
        .newBuilder()
        .expireAfterWrite(15, TimeUnit.MINUTES)
        .build[String, Seq[String]]()

    def findByName(name: String): Either[Throwable, ConceptMetadata] =
        Option(nameCache.getIfPresent(name)) match
            case Some(node) => Right(node)
            case None       =>
                conceptService.findByName(name) match
                    case Left(e)            =>
                        log.atInfo.withCause(e).log(s"Failed to find concept by name: $name")
                        Left(e)
                    case Right(conceptNode) =>
                        nameCache.put(name, conceptNode)
                        Right(conceptNode)

    def findAllNames(limit: Int, offset: Int): Either[Throwable, Seq[String]] =
        val allNames = Option(allNamesCache.getIfPresent(ConceptCache.AllNamesCacheKey))
        if allNames.isDefined && allNames.get.nonEmpty then Right(allNames.get.slice(offset, offset + limit))
        else
            conceptNameService.findAllNames(1000000, 0) match
                case Left(e)      =>
                    log.atError.withCause(e).log("Failed to find all concept names")
                    Left(e)
                case Right(names) =>
                    allNamesCache.put(ConceptCache.AllNamesCacheKey, names)
                    Right(names.slice(offset, offset + limit))

    def clear(): Unit =
        nameCache.invalidateAll()
        allNamesCache.invalidateAll()

object ConceptCache:
    val AllNamesCacheKey = "all-names"
