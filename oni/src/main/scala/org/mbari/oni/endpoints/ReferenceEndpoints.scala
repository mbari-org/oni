/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2024
 *
 * oni code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential. 
 */

package org.mbari.oni.endpoints

import jakarta.persistence.EntityManagerFactory
import org.mbari.oni.ReferenceIdNotFound
import org.mbari.oni.domain.{BadRequest, ErrorMsg, NotFound, Reference, ReferenceQuery, ReferenceUpdate, ServerError}
import org.mbari.oni.endpoints.ReferenceEndpoints.DefaultLimit
import org.mbari.oni.etc.circe.CirceCodecs.{*, given}
import org.mbari.oni.etc.jwt.JwtService
import org.mbari.oni.services.ReferenceService
import sttp.shared.Identity
import sttp.tapir.*
import sttp.tapir.Endpoint
import sttp.tapir.json.circe.*
import sttp.tapir.server.ServerEndpoint
import org.mbari.oni.etc.jdk.Loggers.given

import java.net.{URLDecoder, URLEncoder}

class ReferenceEndpoints(entityManagerFactory: EntityManagerFactory)(using jwtService: JwtService) extends Endpoints:

    private val service = ReferenceService(entityManagerFactory)

    private val base = "reference"
    private val tag = "Reference"


    val findReferenceByIdEndpoint: Endpoint[Unit, Long, ErrorMsg, Reference, Any] =
        openEndpoint
            .get
            .in(base / path[Long]("id"))
            .out(jsonBody[Reference])
            .name("findReferenceById")
            .description("Find a reference by its ID")
            .tag(tag)

    val findReferenceByIdEndpointImpl: ServerEndpoint[Any, Identity] = findReferenceByIdEndpoint.serverLogic { id =>
        service.findById(id) match
            case Right(Some(reference)) => Right(reference)
            case Right(None)            => Left(NotFound(s"Reference with ID $id not found"))
            case Left(e)                => Left(ServerError(e.getMessage))
    }

    val findAllEndpoint: Endpoint[Unit, Paging, ErrorMsg, Seq[Reference], Any] =
        openEndpoint
            .get
            .in(base)
            .in(paging)
            .out(jsonBody[Seq[Reference]])
            .name("findAllReferences")
            .description("Find all references")
            .tag(tag)

    val findAllEndpointImpl: ServerEndpoint[Any, Identity] = findAllEndpoint.serverLogic { paging =>
        handleErrors(service.findAll(paging.limit.getOrElse(DefaultLimit), paging.offset.getOrElse(0)))
    }

    val findReferencesByCitationGlobEndpoint: Endpoint[Unit, (Paging, ReferenceQuery), ErrorMsg, Seq[Reference], Any] =
        openEndpoint
            .post
            .in(base / "query" / "citation")
            .in(paging)
            .in(jsonBody[ReferenceQuery])
            .out(jsonBody[Seq[Reference]])
            .name("findReferencesByCitationGlob")
            .description("Find references by citation glob")
            .tag(tag)

    val findReferencesByCitationGlobEndpointImpl: ServerEndpoint[Any, Identity] = findReferencesByCitationGlobEndpoint.serverLogic { (paging, glob) =>
        glob.citation match
            case None => Left(BadRequest("Citation is required"))
            case Some(citationGlob) =>
                handleErrors(service.findByCitationGlob(citationGlob, paging.limit.getOrElse(DefaultLimit), paging.offset.getOrElse(0)))
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

    val findReferenceByDoiEndpointImpl: ServerEndpoint[Any, Identity] = findReferenceByDoiEndpoint.serverLogic { doi =>
        doi.doi match
            case None => Left(BadRequest("DOI is required"))
            case Some(doi) =>
                service.findByDoi(doi) match
                    case Right(Some(reference)) => Right(reference)
                    case Right(None)            => Left(NotFound(s"Reference with DOI '${doi}' not found"))
                    case Left(e)                => Left(ServerError(e.getMessage))
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

    val createReferenceEndpointImpl: ServerEndpoint[Any, Identity] = createReferenceEndpoint
        .serverSecurityLogic(jwtOpt => verify(jwtOpt))
        .serverLogic { _ => reference =>
            handleErrors(service.create(reference))
        }

    val updateReferenceEndpoint: Endpoint[Option[String], ReferenceUpdate, ErrorMsg, Reference, Any] =
        secureEndpoint
            .put
            .in(base)
            .in(jsonBody[ReferenceUpdate])
            .out(jsonBody[Reference])
            .name("updateReference")
            .description("Update a reference")
            .tag(tag)

    val updateReferenceEndpointImpl: ServerEndpoint[Any, Identity] = updateReferenceEndpoint
        .serverSecurityLogic(jwtOpt => verify(jwtOpt))
        .serverLogic { _ => referenceUpdate =>
            handleErrors(service.update(referenceUpdate))
        }


    val deleteReferenceEndpoint: Endpoint[Option[String], Long, ErrorMsg, Unit, Any] =
        secureEndpoint
            .delete
            .in(base / path[Long]("id"))
            .out(jsonBody[Unit])
            .name("deleteReference")
            .description("Delete a reference")
            .tag(tag)

    val deleteReferenceEndpointImpl: ServerEndpoint[Any, Identity] = deleteReferenceEndpoint
        .serverSecurityLogic(jwtOpt => verify(jwtOpt))
        .serverLogic { _ => id =>
            handleErrors(service.delete(id))
        }

    val addConceptEndpoint: Endpoint[Option[String], (Long, String), ErrorMsg, Reference, Any] =
        secureEndpoint
            .put
            .in(base / "add" / path[Long]("id") / "to" / path[String]("concept"))
            .out(jsonBody[Reference])
            .name("addReferenceToConcept")
            .description("Add a reference to a concept")
            .tag(tag)

    val addConceptEndpointImpl: ServerEndpoint[Any, Identity] = addConceptEndpoint
        .serverSecurityLogic(jwtOpt => verify(jwtOpt))
        .serverLogic { _ => (id, concept) =>
            handleErrors(service.addConcept(id, concept))
        }

    val removeConceptEndpoint: Endpoint[Option[String], (Long, String), ErrorMsg, Reference, Any] =
        secureEndpoint
            .put
            .in(base / "remove" / path[Long]("id") / "from" / path[String]("concept"))
            .out(jsonBody[Reference])
            .name("removeReferenceFromConcept")
            .description("Remove a reference from a concept")
            .tag(tag)

    val removeConceptEndpointImpl: ServerEndpoint[Any, Identity] = removeConceptEndpoint
        .serverSecurityLogic(jwtOpt => verify(jwtOpt))
        .serverLogic { _ => (id, concept) =>
            handleErrors(service.removeConcept(id, concept))
        }

    override def all: List[Endpoint[_, _, _, _, _]] = List(
        addConceptEndpoint,                    // PUT    add/:id/to/:concept
        removeConceptEndpoint,                 // PUT    remove/:id/from/:concept
        findReferencesByCitationGlobEndpoint,  // GET    query/citation/:glob
        findReferenceByDoiEndpoint,            // GET    query/doi
        findReferenceByIdEndpoint,             // GET    :id
        findAllEndpoint,                       // GET    /
        createReferenceEndpoint,               // POST   /
        updateReferenceEndpoint,               // PUT    /
        deleteReferenceEndpoint,               // DELETE /:id

    )

    override def allImpl: List[ServerEndpoint[Any, Identity]] = List(
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