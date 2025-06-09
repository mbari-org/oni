/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2024
 *
 * oni code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential.
 */

package org.mbari.oni.endpoints

import jakarta.persistence.EntityManagerFactory
import org.mbari.oni.domain.{Count, ErrorMsg, ExtendedLink, Link, LinkCreate, LinkUpdate, Page, ServerError}
import org.mbari.oni.etc.circe.CirceCodecs.given
import org.mbari.oni.etc.jwt.JwtService
import org.mbari.oni.services.{LinkRealizationService, LinkService}
import sttp.tapir.json.circe.*
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.{Endpoint, *}

import scala.concurrent.{ExecutionContext, Future}

class LinkRealizationEndpoints(entityManagerFactory: EntityManagerFactory)(using
    jwtService: JwtService,
    executionContext: ExecutionContext
) extends Endpoints:

    private val service     = LinkRealizationService(entityManagerFactory)
    private val linkService = LinkService(entityManagerFactory)
    private val base        = "linkrealizations"
    private val tag         = "LinkRealizations"

    val findLinkRealizationById: Endpoint[Unit, Long, ErrorMsg, ExtendedLink, Any] = openEndpoint
        .get
        .in(base / path[Long]("id"))
        .out(jsonBody[ExtendedLink])
        .name("findLinkRealizationeById")
        .description("Find a link realizations by its id")
        .tag(tag)

    val findLinkRealizationByIdImpl: ServerEndpoint[Any, Future] =
        findLinkRealizationById.serverLogic { id =>
            handleErrorsAsync(service.findById(id))
        }

    val findLinkRealizationsByConceptName: Endpoint[Unit, String, ErrorMsg, Seq[ExtendedLink], Any] = openEndpoint
        .get
        .in(base / "concept" / path[String]("conceptName"))
        .out(jsonBody[Seq[ExtendedLink]])
        .name("findLinkRealizationsByConceptName")
        .description("Find all link realizations by concept name")
        .tag(tag)

    val findLinkRealizationsByConceptNameImpl: ServerEndpoint[Any, Future] =
        findLinkRealizationsByConceptName.serverLogic { conceptName =>
            handleErrorsAsync(service.findByConcept(conceptName))
        }

    val findLinkRealizationByPrototype: Endpoint[Unit, Link, ErrorMsg, Seq[ExtendedLink], Any] = openEndpoint
        .post
        .in(base / "prototype")
        .in(jsonBody[Link])
        .out(jsonBody[Seq[ExtendedLink]])
        .name("findLinkRealizationByPrototype")
        .description("Find all link realizations by prototype")
        .tag(tag)

    val findLinkRealizationByPrototypeImpl: ServerEndpoint[Any, Future] =
        findLinkRealizationByPrototype.serverLogic { link =>
            handleErrorsAsync(service.findByPrototype(link))
        }

    val create: Endpoint[Option[String], LinkCreate, ErrorMsg, ExtendedLink, Any] = secureEndpoint
        .post
        .in(base)
        .in(jsonBody[LinkCreate])
        .out(jsonBody[ExtendedLink])
        .name("createLinkRealization")
        .description("Create a new link realization")
        .tag(tag)

    val createImpl: ServerEndpoint[Any, Future] = create
        .serverSecurityLogic(jwtOpt => verifyLoginAsync(jwtOpt))
        .serverLogic { userAccount => link =>
            handleErrorsAsync(service.create(link, userAccount.username))
        }

    val update: Endpoint[Option[String], (Long, LinkUpdate), ErrorMsg, ExtendedLink, Any] = secureEndpoint
        .put
        .in(base / path[Long]("id"))
        .in(jsonBody[LinkUpdate])
        .out(jsonBody[ExtendedLink])
        .name("updateLinkRealization")
        .description("Update a link realization")
        .tag(tag)

    val updateImpl: ServerEndpoint[Any, Future] = update
        .serverSecurityLogic(jwtOpt => verifyLoginAsync(jwtOpt))
        .serverLogic { userAccount => (id, linkUpdate) =>
            handleErrorsAsync(service.updateById(id, linkUpdate, userAccount.username))
        }

    val delete: Endpoint[Option[String], Long, ErrorMsg, Unit, Any] = secureEndpoint
        .delete
        .in(base / path[Long]("id"))
        .out(jsonBody[Unit])
        .name("deleteLinkRealization")
        .description("Delete a link realization")
        .tag(tag)

    val deleteImpl: ServerEndpoint[Any, Future] = delete
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

    val countAllLinkRealizations: Endpoint[Unit, Unit, ErrorMsg, Count, Any] = openEndpoint
        .get
        .in(base / "count")
        .out(jsonBody[Count])
        .name("countAllLinkRealizations")
        .description("Count all link realizations")
        .tag(tag)

    val countAllLinkRealizationsImpl: ServerEndpoint[Any, Future] =
        countAllLinkRealizations.serverLogic { _ =>
            handleErrorsAsync(service.countAll().map(Count.apply))
        }

    val findAllLinkRealizations: Endpoint[Unit, Paging, ErrorMsg, Page[Seq[ExtendedLink]], Any] = openEndpoint
        .get
        .in(base)
        .in(paging)
        .out(jsonBody[Page[Seq[ExtendedLink]]])
        .name("findAllLinkRealizations")
        .description("Find all link realizations")
        .tag(tag)

    val findAllLinkRealizationsImpl: ServerEndpoint[Any, Future] =
        findAllLinkRealizations.serverLogic { paging =>
            val limit  = paging.limit.getOrElse(100)
            val offset = paging.offset.getOrElse(0)
            handleErrorsAsync(service.findAll(limit, offset).map(s => Page(s, limit, offset)))
        }

    val findLinkRealizationsByLinkName: Endpoint[Unit, String, ErrorMsg, Seq[ExtendedLink], Any] = openEndpoint
        .get
        .in(base / "query" / "linkname" / path[String]("linkName"))
        .out(jsonBody[Seq[ExtendedLink]])
        .name("findLinkRealizationsByLinkName")
        .description("Find all link realizations by link name")
        .tag(tag)

    val findLinkRealizationsByLinkNameImpl: ServerEndpoint[Any, Future] =
        findLinkRealizationsByLinkName.serverLogic { linkName =>
            handleErrorsAsync(linkService.findLinkRealizationsByLinkName(linkName))
        }

    override def all: List[Endpoint[?, ?, ?, ?, ?]] = List(
        findLinkRealizationsByConceptName,
        findLinkRealizationsByLinkName,
        findLinkRealizationByPrototype,
        countAllLinkRealizations,
        findAllLinkRealizations,
        create,
        update,
        delete,
        findLinkRealizationById
    )

    override def allImpl: List[ServerEndpoint[Any, Future]] = List(
        findLinkRealizationsByConceptNameImpl,
        findLinkRealizationsByLinkNameImpl,
        findLinkRealizationByPrototypeImpl,
        countAllLinkRealizationsImpl,
        findAllLinkRealizationsImpl,
        createImpl,
        updateImpl,
        deleteImpl,
        findLinkRealizationByIdImpl
    )
