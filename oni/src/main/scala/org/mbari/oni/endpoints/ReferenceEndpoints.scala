/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2024
 *
 * oni code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential.
 */

package org.mbari.oni.endpoints

import jakarta.persistence.EntityManagerFactory
import org.mbari.oni.domain.{
    BadRequest,
    ErrorMsg,
    NotFound,
    Page,
    Reference,
    ReferenceQuery,
    ReferenceUpdate,
    ServerError
}
import org.mbari.oni.endpoints.ReferenceEndpoints.DefaultLimit
import org.mbari.oni.etc.circe.CirceCodecs.given
import org.mbari.oni.etc.jwt.JwtService
import org.mbari.oni.services.ReferenceService
import sttp.tapir.json.circe.*
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.{Endpoint, *}

import scala.concurrent.{ExecutionContext, Future}

class ReferenceEndpoints(entityManagerFactory: EntityManagerFactory)(using
    jwtService: JwtService,
    executionContext: ExecutionContext
) extends Endpoints:

    private val service = ReferenceService(entityManagerFactory)

    private val base = "references"
    private val tag  = "References"

    val findReferenceByIdEndpoint: Endpoint[Unit, Long, ErrorMsg, Reference, Any] =
        openEndpoint
            .get
            .in(base / path[Long]("id"))
            .out(jsonBody[Reference])
            .name("findReferenceById")
            .description("Find a reference by its ID")
            .tag(tag)

    val findReferenceByIdEndpointImpl: ServerEndpoint[Any, Future] = findReferenceByIdEndpoint.serverLogic { id =>
        Future {
            service.findById(id) match
                case Right(Some(reference)) => Right(reference)
                case Right(None)            => Left(NotFound(s"Reference with ID $id not found"))
                case Left(e)                => Left(ServerError(e.getMessage))
        }
    }

    val findAllEndpoint: Endpoint[Unit, Paging, ErrorMsg, Page[Seq[Reference]], Any] =
        openEndpoint
            .get
            .in(base)
            .in(paging)
            .out(jsonBody[Page[Seq[Reference]]])
            .name("findAllReferences")
            .description("Find all references")
            .tag(tag)

    val findAllEndpointImpl: ServerEndpoint[Any, Future] = findAllEndpoint.serverLogic { paging =>
        val limit  = paging.limit.getOrElse(DefaultLimit)
        val offset = paging.offset.getOrElse(0)
        handleErrorsAsync {
            service
                .findAll(limit, offset)
                .map(xs => Page(xs, limit, offset))
        }
    }

    val findReferencesByCitationGlobEndpoint
        : Endpoint[Unit, (Paging, ReferenceQuery), ErrorMsg, Page[Seq[Reference]], Any] =
        openEndpoint
            .post
            .in(base / "query" / "citation")
            .in(paging)
            .in(jsonBody[ReferenceQuery])
            .out(jsonBody[Page[Seq[Reference]]])
            .name("findReferencesByCitationGlob")
            .description("Find references by citation glob")
            .tag(tag)

    val findReferencesByCitationGlobEndpointImpl: ServerEndpoint[Any, Future] =
        findReferencesByCitationGlobEndpoint.serverLogic { (paging, glob) =>
            val limit  = paging.limit.getOrElse(DefaultLimit)
            val offset = paging.offset.getOrElse(0)
            glob.citation match
                case None               => Future(Left(BadRequest("Citation is required")))
                case Some(citationGlob) =>
                    handleErrorsAsync(
                        service
                            .findByCitationGlob(
                                citationGlob,
                                limit,
                                offset
                            )
                            .map(xs => Page(xs, limit, offset))
                    )
        }

    val findReferenceByDoiEndpoint: Endpoint[Unit, ReferenceQuery, ErrorMsg, Reference, Any] =
        openEndpoint
            .post
            .in(base / "query" / "doi")
            .in(jsonBody[ReferenceQuery])
            .out(jsonBody[Reference])
            .name("findReferenceByDoi")
            .description("Find a reference by DOI")
            .tag(tag)

    val findReferenceByDoiEndpointImpl: ServerEndpoint[Any, Future] = findReferenceByDoiEndpoint.serverLogic { doi =>
        Future {
            doi.doi match
                case None      => Left(BadRequest("DOI is required"))
                case Some(doi) =>
                    service.findByDoi(doi) match
                        case Right(Some(reference)) => Right(reference)
                        case Right(None)            => Left(NotFound(s"Reference with DOI '${doi}' not found"))
                        case Left(e)                => Left(ServerError(e.getMessage))
        }
    }

    val createReferenceEndpoint: Endpoint[Option[String], Reference, ErrorMsg, Reference, Any] =
        secureEndpoint
            .post
            .in(base)
            .in(jsonBody[Reference])
            .out(jsonBody[Reference])
            .name("createReference")
            .description("Create a new reference")
            .tag(tag)

    val createReferenceEndpointImpl: ServerEndpoint[Any, Future] = createReferenceEndpoint
        .serverSecurityLogic(jwtOpt => verifyAsync(jwtOpt))
        .serverLogic { _ => reference =>
            handleErrorsAsync(service.create(reference))
        }

    val updateReferenceEndpoint: Endpoint[Option[String], (Long, ReferenceUpdate), ErrorMsg, Reference, Any] =
        secureEndpoint
            .put
            .in(base / path[Long]("id"))
            .in(jsonBody[ReferenceUpdate])
            .out(jsonBody[Reference])
            .name("updateReference")
            .description("Update a reference")
            .tag(tag)

    val updateReferenceEndpointImpl: ServerEndpoint[Any, Future] = updateReferenceEndpoint
        .serverSecurityLogic(jwtOpt => verifyAsync(jwtOpt))
        .serverLogic { _ => (id, referenceUpdate) =>
            handleErrorsAsync(service.updateById(id, referenceUpdate))
        }

    val deleteReferenceEndpoint: Endpoint[Option[String], Long, ErrorMsg, Unit, Any] =
        secureEndpoint
            .delete
            .in(base / path[Long]("id"))
            .out(jsonBody[Unit])
            .name("deleteReference")
            .description("Delete a reference")
            .tag(tag)

    val deleteReferenceEndpointImpl: ServerEndpoint[Any, Future] = deleteReferenceEndpoint
        .serverSecurityLogic(jwtOpt => verifyAsync(jwtOpt))
        .serverLogic { _ => id =>
            handleErrorsAsync(service.deleteById(id))
        }

    val addConceptEndpoint: Endpoint[Option[String], (Long, String), ErrorMsg, Reference, Any] =
        secureEndpoint
            .put
            .in(base / "add" / path[Long]("id") / "to" / path[String]("concept"))
            .out(jsonBody[Reference])
            .name("addReferenceToConcept")
            .description("Add a reference to a concept")
            .tag(tag)

    val addConceptEndpointImpl: ServerEndpoint[Any, Future] = addConceptEndpoint
        .serverSecurityLogic(jwtOpt => verifyAsync(jwtOpt))
        .serverLogic { _ => (id, concept) =>
            handleErrorsAsync(service.addConcept(id, concept))
        }

    val removeConceptEndpoint: Endpoint[Option[String], (Long, String), ErrorMsg, Reference, Any] =
        secureEndpoint
            .put
            .in(base / "remove" / path[Long]("id") / "from" / path[String]("concept"))
            .out(jsonBody[Reference])
            .name("removeReferenceFromConcept")
            .description("Remove a reference from a concept")
            .tag(tag)

    val removeConceptEndpointImpl: ServerEndpoint[Any, Future] = removeConceptEndpoint
        .serverSecurityLogic(jwtOpt => verifyAsync(jwtOpt))
        .serverLogic { _ => (id, concept) =>
            handleErrorsAsync(service.removeConcept(id, concept))
        }

    override def all: List[Endpoint[?, ?, ?, ?, ?]] = List(
        addConceptEndpoint,                   // PUT    add/:id/to/:concept
        removeConceptEndpoint,                // PUT    remove/:id/from/:concept
        findReferencesByCitationGlobEndpoint, // GET    query/citation/:glob
        findReferenceByDoiEndpoint,           // GET    query/doi
        findReferenceByIdEndpoint,            // GET    :id
        findAllEndpoint,                      // GET    /
        createReferenceEndpoint,              // POST   /
        updateReferenceEndpoint,              // PUT    /
        deleteReferenceEndpoint               // DELETE /:id

    )

    override def allImpl: List[ServerEndpoint[Any, Future]] = List(
        addConceptEndpointImpl,
        removeConceptEndpointImpl,
        findReferencesByCitationGlobEndpointImpl,
        findReferenceByDoiEndpointImpl,
        findReferenceByIdEndpointImpl,
        findAllEndpointImpl,
        createReferenceEndpointImpl,
        updateReferenceEndpointImpl,
        deleteReferenceEndpointImpl
    )

object ReferenceEndpoints:

    val DefaultLimit = 100
