/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2024
 *
 * oni code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential.
 */

package org.mbari.oni.endpoints

import jakarta.persistence.EntityManagerFactory
import org.mbari.oni.ItemNotFound
import org.mbari.oni.domain.{ErrorMsg, Media, MediaCreate, MediaUpdate}
import org.mbari.oni.etc.circe.CirceCodecs.given
import org.mbari.oni.etc.jwt.JwtService
import org.mbari.oni.jdbc.FastPhylogenyService
import org.mbari.oni.services.{ConceptService, MediaService}
import sttp.tapir.json.circe.*
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.{Endpoint, *}

import scala.concurrent.{ExecutionContext, Future}

class MediaEndpoints(entityManagerFactory: EntityManagerFactory, fastPhylogenyService: FastPhylogenyService)(using
    jwtService: JwtService,
    executionContext: ExecutionContext
) extends Endpoints:

    private val service        = MediaService(entityManagerFactory, fastPhylogenyService)
    private val conceptService = ConceptService(entityManagerFactory)
    private val base           = "media"
    private val tag            = "Media"

    val mediaForConceptEndpoint: Endpoint[Unit, String, ErrorMsg, Seq[Media], Any] = openEndpoint
        .get
        .in(base / "search" / "concept" / path[String]("name"))
        .out(jsonBody[Seq[Media]])
        .name("mediaForConcept")
        .description("Get all media for a concept")
        .tag(tag)

    val mediaForConceptEndpointImpl: ServerEndpoint[Any, Future] = mediaForConceptEndpoint.serverLogic { name =>
        handleErrorsAsync(conceptService.findByName(name).map(_.media.toSeq.sortBy(_.url.toString)))
    }

    val createMediaEndpoint: Endpoint[Option[String], MediaCreate, ErrorMsg, Media, Any] = secureEndpoint
        .post
        .in(base)
        .in(
            jsonBody[MediaCreate].description(
                "The media record to create. mediaType defaults to 'IMAGE', but can also be set to 'VIDEO'"
            )
        )
        .out(jsonBody[Media])
        .name("createMedia")
        .description("Create a new media record")
        .tag(tag)

    val createMediaEndpointImpl: ServerEndpoint[Any, Future] = createMediaEndpoint
        .serverSecurityLogic(jwtOpt => verifyLoginAsync(jwtOpt))
        .serverLogic { userAccount => mediaCreate =>
            handleErrorsAsync(service.create(mediaCreate, userAccount.username))
        }

    val findMediaByIdEndpoint: Endpoint[Unit, Long, ErrorMsg, Media, Any] = openEndpoint
        .get
        .in(base / path[Long]("id"))
        .out(jsonBody[Media])
        .name("findMediaById")
        .description("Find a media record by ID")
        .tag(tag)

    val findMediaByIdEndpointImpl: ServerEndpoint[Any, Future] = findMediaByIdEndpoint.serverLogic { id =>
        handleErrorsAsync(service.findById(id).map(_.getOrElse(throw ItemNotFound(s"Media with ID $id not found"))))
    }

    val updateMediaEndpoint: Endpoint[Option[String], (Long, MediaUpdate), ErrorMsg, Media, Any] = secureEndpoint
        .put
        .in(base / path[Long]("id"))
        .in(jsonBody[MediaUpdate])
        .out(jsonBody[Media])
        .name("updateMedia")
        .description("Update a media record")
        .tag(tag)

    val updateMediaEndpointImpl: ServerEndpoint[Any, Future] = updateMediaEndpoint
        .serverSecurityLogic(jwtOpt => verifyLoginAsync(jwtOpt))
        .serverLogic { userAccount => (id, mediaUpdate) =>
            handleErrorsAsync(service.update(id, mediaUpdate, userAccount.username))
        }

    val deleteMediaEndpoint: Endpoint[Option[String], Long, ErrorMsg, Unit, Any] = secureEndpoint
        .delete
        .in(base / path[Long]("id"))
        .out(jsonBody[Unit])
        .name("deleteMedia")
        .description("Delete a media record")
        .tag(tag)

    val deleteMediaEndpointImpl: ServerEndpoint[Any, Future] = deleteMediaEndpoint
        .serverSecurityLogic(jwtOpt => verifyLoginAsync(jwtOpt))
        .serverLogic { userAccount => id =>
            handleErrorsAsync(service.deleteById(id, userAccount.username))
        }

    override def all: List[Endpoint[?, ?, ?, ?, ?]] = List(
        findMediaByIdEndpoint,
        mediaForConceptEndpoint,
        createMediaEndpoint,
        updateMediaEndpoint,
        deleteMediaEndpoint
    )

    override def allImpl: List[ServerEndpoint[Any, Future]] = List(
        findMediaByIdEndpointImpl,
        mediaForConceptEndpointImpl,
        createMediaEndpointImpl,
        updateMediaEndpointImpl,
        deleteMediaEndpointImpl
    )
