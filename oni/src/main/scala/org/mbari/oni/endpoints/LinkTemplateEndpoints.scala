/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2024
 *
 * oni code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential.
 */

package org.mbari.oni.endpoints

import jakarta.persistence.EntityManagerFactory
import org.mbari.oni.domain.{ErrorMsg, ExtendedLink, Link, LinkCreate, LinkUpdate, ServerError}
import org.mbari.oni.etc.circe.CirceCodecs.given
import org.mbari.oni.etc.jwt.JwtService
import org.mbari.oni.services.LinkTemplateService
import sttp.shared.Identity
import sttp.tapir.*
import sttp.tapir.Endpoint
import sttp.tapir.json.circe.*
import sttp.tapir.server.ServerEndpoint

import scala.concurrent.{ExecutionContext, Future}

class LinkTemplateEndpoints(entityManagerFactory: EntityManagerFactory)(using jwtService: JwtService, executionContext: ExecutionContext) extends Endpoints:

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

    val findLinkTemplateByIdImpl: ServerEndpoint[Any, Future] =
        findLinkTemplateById.serverLogic { id =>
            handleErrorsAsync(service.findById(id))
        }

    val findLinkTemplateByConceptName: Endpoint[Unit, String, ErrorMsg, Seq[ExtendedLink], Any] = openEndpoint
        .get
        .in(base / "concept" / path[String]("conceptName"))
        .out(jsonBody[Seq[ExtendedLink]])
        .name("findLinkTemplateByConceptName")
        .description("Find all link templates by concept name")
        .tag(tag)

    val findLinkTemplateByConceptNameImpl: ServerEndpoint[Any, Future] =
        findLinkTemplateByConceptName.serverLogic { conceptName =>
            handleErrorsAsync(service.findByConcept(conceptName))
        }

    val findLinKTemplateByPrototype: Endpoint[Unit, Link, ErrorMsg, Seq[ExtendedLink], Any] = openEndpoint
        .post
        .in(base / "prototype")
        .in(jsonBody[Link])
        .out(jsonBody[Seq[ExtendedLink]])
        .name("findLinkTemplateByPrototype")
        .description("Find all link templates by prototype")
        .tag(tag)

    val findLinkTemplateByPrototypeImpl: ServerEndpoint[Any, Future] =
        findLinKTemplateByPrototype.serverLogic { link =>
            handleErrorsAsync(service.findByPrototype(link))
        }

    val createLinkTemplate: Endpoint[Option[String], LinkCreate, ErrorMsg, ExtendedLink, Any] = secureEndpoint
        .post
        .in(base)
        .in(jsonBody[LinkCreate])
        .out(jsonBody[ExtendedLink])
        .name("createLinkTemplate")
        .description("Create a new link template")
        .tag(tag)

    val createLinkTemplateImpl: ServerEndpoint[Any, Future] = createLinkTemplate
        .serverSecurityLogic(jwtOpt => verifyLoginAsync(jwtOpt))
        .serverLogic { userAccount => link =>
            handleErrorsAsync(service.create(link, userAccount.username))
        }

    val updateLinkTemplate: Endpoint[Option[String], (Long, LinkUpdate), ErrorMsg, ExtendedLink, Any] = secureEndpoint
        .put
        .in(base / path[Long]("id"))
        .in(jsonBody[LinkUpdate])
        .out(jsonBody[ExtendedLink])
        .name("updateLinkTemplate")
        .description("Update a link template")
        .tag(tag)

    val updateLinkTemplateImpl: ServerEndpoint[Any, Future] = updateLinkTemplate
        .serverSecurityLogic(jwtOpt => verifyLoginAsync(jwtOpt))
        .serverLogic { userAccount => (id, link) =>
            handleErrorsAsync(service.updateById(id, link, userAccount.username))
        }

    val deleteLinkTemplate: Endpoint[Option[String], Long, ErrorMsg, Unit, Any] = secureEndpoint
        .delete
        .in(base / path[Long]("id"))
        .out(jsonBody[Unit])
        .name("deleteLinkTemplate")
        .description("Delete a link template")
        .tag(tag)

    val deleteLinkTemplateImpl: ServerEndpoint[Any, Future] = deleteLinkTemplate
        .serverSecurityLogic(jwtOpt => verifyLoginAsync(jwtOpt))
        .serverLogic { userAccount => id =>
            Future {
                service
                    .deleteById(id, userAccount.username)
                    .fold(
                        error => Left(ServerError(error.getMessage)),
                        _ => Right(())
                    )
            }
        }

    override def all: List[Endpoint[?, ?, ?, ?, ?]] = List(
        findLinkTemplateByConceptName,
        findLinKTemplateByPrototype,
        createLinkTemplate,
        updateLinkTemplate,
        deleteLinkTemplate,
        findLinkTemplateById
    )

    override def allImpl: List[ServerEndpoint[Any, Future]] = List(
        findLinkTemplateByConceptNameImpl,
        findLinkTemplateByPrototypeImpl,
        createLinkTemplateImpl,
        updateLinkTemplateImpl,
        deleteLinkTemplateImpl,
        findLinkTemplateByIdImpl
    )
