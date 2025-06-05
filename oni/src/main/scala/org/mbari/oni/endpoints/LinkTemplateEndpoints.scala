/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2024
 *
 * oni code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential.
 */

package org.mbari.oni.endpoints

import jakarta.persistence.EntityManagerFactory
import org.mbari.oni.domain.{ErrorMsg, ExtendedLink, Link, LinkCreate, LinkRenameToConceptRequest, LinkRenameToConceptResponse, LinkUpdate, Page, ServerError}
import org.mbari.oni.etc.circe.CirceCodecs.given
import org.mbari.oni.etc.jwt.JwtService
import org.mbari.oni.services.{LinkService, LinkTemplateService}
import sttp.tapir.json.circe.*
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.{Endpoint, *}

import scala.concurrent.{ExecutionContext, Future}

class LinkTemplateEndpoints(entityManagerFactory: EntityManagerFactory)(using
    jwtService: JwtService,
    executionContext: ExecutionContext
) extends Endpoints:

    private val service = LinkTemplateService(entityManagerFactory)
    private val linkService = LinkService(entityManagerFactory)
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

    val findLinkTemplateByPrototype: Endpoint[Unit, Link, ErrorMsg, Seq[ExtendedLink], Any] = openEndpoint
        .post
        .in(base / "prototype")
        .in(jsonBody[Link])
        .out(jsonBody[Seq[ExtendedLink]])
        .name("findLinkTemplateByPrototype")
        .description("Find all link templates by prototype")
        .tag(tag)

    val findLinkTemplateByPrototypeImpl: ServerEndpoint[Any, Future] =
        findLinkTemplateByPrototype.serverLogic { link =>
            handleErrorsAsync(service.findByPrototype(link))
        }

    val countByToConcept: Endpoint[Unit, String, ErrorMsg, Long, Any] = openEndpoint
        .get
        .in(base / "toconcept" / "count" / path[String]("toConcept"))
        .out(jsonBody[Long])
        .name("countLinkTemplatesByToConcept")
        .description("Count all link templates by toConcept")
        .tag(tag)

    val countByToConceptImpl: ServerEndpoint[Any, Future] = countByToConcept
        .serverLogic { toConcept =>
            handleErrorsAsync(service.countByToConcept(toConcept))
        }

    val findByToConcept: Endpoint[Unit, String, ErrorMsg, Seq[ExtendedLink], Any] = openEndpoint
        .get
        .in(base / "toconcept" / path[String]("toConcept"))
        .out(jsonBody[Seq[ExtendedLink]])
        .name("findLinkTemplatesByToConcept")
        .description("Find all link templates by toConcept")
        .tag(tag)

    val findByToConceptImpl: ServerEndpoint[Any, Future] = findByToConcept
        .serverLogic { toConcept =>
            handleErrorsAsync(service.findByToConcept(toConcept))
        }

    val renameToConcept
        : Endpoint[Option[String], LinkRenameToConceptRequest, ErrorMsg, LinkRenameToConceptResponse, Any] =
        secureEndpoint
            .put
            .in(base / "toconcept" / "rename")
            .in(jsonBody[LinkRenameToConceptRequest])
            .out(jsonBody[LinkRenameToConceptResponse])
            .name("renameToConcept")
            .description("Bulk rename all linkTemplate toConcepts")
            .tag(tag)

    val renameToConceptImpl: ServerEndpoint[Any, Future] = renameToConcept
        .serverSecurityLogic(jwtOpt => verifyLoginAsync(jwtOpt))
        .serverLogic { userAccount => request =>
            handleErrorsAsync(service.renameToConcept(request.old, request.`new`, userAccount.username))
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

    val findAllLinkTemplates: Endpoint[Unit, Paging, ErrorMsg, Page[Seq[ExtendedLink]], Any] = openEndpoint
        .get
        .in(base)
        .in(paging)
        .out(jsonBody[Page[Seq[ExtendedLink]]])
        .name("findAllLinkTemplates")
        .description("Find all link templates")
        .tag(tag)

    val findAllLinkTemplatesImpl: ServerEndpoint[Any, Future] = findAllLinkTemplates
        .serverLogic { paging =>
            val limit = paging.limit.getOrElse(100)
            val offset = paging.offset.getOrElse(0)
            handleErrorsAsync(service.findAll(limit, offset).map(s => Page(s, limit, offset)))
        }

    val findLinkTemplatesForConceptName: Endpoint[Unit, String, ErrorMsg, Seq[ExtendedLink], Any] = openEndpoint
        .get
        .in(base / "query" / "for" / path[String]("conceptName"))
        .out(jsonBody[Seq[ExtendedLink]])
        .name("findLinkTemplatesForConceptName")
        .description("Find all link templates that can be used to annotate a concept by name")
        .tag(tag)

    val findLinkTemplatesForConceptNameImpl: ServerEndpoint[Any, Future] = findLinkTemplatesForConceptName
        .serverLogic { conceptName =>
            handleErrorsAsync(
                linkService.findAllLinkTemplatesForConcept(conceptName)
            )
        }

    val findLinkTemplatesForConceptAndLinkName: Endpoint[Unit, (String, String), ErrorMsg, Seq[ExtendedLink], Any] = openEndpoint
        .get
        .in(base / "query" / "for" / path[String]("conceptName") / "using" / path[String]("linkName"))
        .out(jsonBody[Seq[ExtendedLink]])
        .name("findLinkTemplatesForConceptAndLinkName")
        .description("Find all link templates that can be used to annotate a concept by name and link name")
        .tag(tag)

    val findLinkTemplatesForConceptAndLinkNameImpl: ServerEndpoint[Any, Future] = findLinkTemplatesForConceptAndLinkName
        .serverLogic { (conceptName, linkName) =>
            handleErrorsAsync(
                linkService.findLinkTemplatesByNameForConcept(conceptName, linkName)
            )
        }

    override def all: List[Endpoint[?, ?, ?, ?, ?]] = List(
        renameToConcept,
        findLinkTemplateByConceptName,
        findLinkTemplateByPrototype,
        findLinkTemplatesForConceptAndLinkName,
        findLinkTemplatesForConceptName,
        countByToConcept,
        findByToConcept,
        findAllLinkTemplates,
        createLinkTemplate,
        updateLinkTemplate,
        deleteLinkTemplate,
        findLinkTemplateById
    )

    override def allImpl: List[ServerEndpoint[Any, Future]] = List(
        renameToConceptImpl,
        findLinkTemplateByConceptNameImpl,
        findLinkTemplateByPrototypeImpl,
        findLinkTemplatesForConceptAndLinkNameImpl,
        findLinkTemplatesForConceptNameImpl,
        countByToConceptImpl,
        findByToConceptImpl,
        findAllLinkTemplatesImpl,
        createLinkTemplateImpl,
        updateLinkTemplateImpl,
        deleteLinkTemplateImpl,
        findLinkTemplateByIdImpl
    )
