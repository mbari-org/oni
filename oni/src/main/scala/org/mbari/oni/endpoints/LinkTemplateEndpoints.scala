/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2024
 *
 * oni code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential. 
 */

package org.mbari.oni.endpoints

import jakarta.persistence.EntityManagerFactory
import org.mbari.oni.domain.{ErrorMsg, ExtendedLink}
import org.mbari.oni.etc.circe.CirceCodecs.given
import org.mbari.oni.services.LinkTemplateService
import sttp.shared.Identity
import sttp.tapir.*
import sttp.tapir.Endpoint
import sttp.tapir.json.circe.*
import sttp.tapir.server.ServerEndpoint

class LinkTemplateEndpoints(entityManagerFactory: EntityManagerFactory) extends Endpoints:

    private val service = LinkTemplateService(entityManagerFactory)
    private val base    = "linktemplates"
    private val tag     = "LinkTemplates"

    val findLinkTemplateById: Endpoint[Unit, Long, ErrorMsg, ExtendedLink, Any] = openEndpoint
        .get
        .in(base / path[Long]("id"))
        .out(jsonBody[ExtendedLink])
        .name("findLinkTemplateById")
        .description("Find a link template by its id")
        .tag(tag)

    val findLinkTemplateByIdImpl: ServerEndpoint[Any, Identity] =
        findLinkTemplateById.serverLogic { id =>
            handleErrors(service.findById(id))
        }

    override def all: List[Endpoint[_, _, _, _, _]] = ???

    override def allImpl: List[ServerEndpoint[Any, Identity]] = ???
