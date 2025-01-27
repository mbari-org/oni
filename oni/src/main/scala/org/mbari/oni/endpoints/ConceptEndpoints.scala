/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2024
 *
 * oni code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential.
 */

package org.mbari.oni.endpoints

import jakarta.persistence.EntityManagerFactory
import org.mbari.oni.ConceptNameNotFound
import sttp.tapir.*
import sttp.tapir.Endpoint
import sttp.tapir.json.circe.*
import sttp.tapir.server.ServerEndpoint
import org.mbari.oni.domain.{ConceptCreate, ConceptDelete, ConceptMetadata, ConceptUpdate, ErrorMsg, NotFound, ServerError}
import org.mbari.oni.etc.circe.CirceCodecs.given
import org.mbari.oni.etc.jwt.JwtService
import org.mbari.oni.services.{ConceptCache, ConceptNameService, ConceptService}
import sttp.shared.Identity

import scala.concurrent.{ExecutionContext, Future}
import org.mbari.oni.domain.Rank
import org.mbari.oni.services.RankValidator

class ConceptEndpoints(entityManagerFactory: EntityManagerFactory)(using jwtService: JwtService, executionContext: ExecutionContext) extends Endpoints:

    private val service            = ConceptService(entityManagerFactory)
    private val conceptNameService = ConceptNameService(entityManagerFactory)
    private val conceptCache       = ConceptCache(service, conceptNameService)
    private val base               = "concept"
    private val tag                = "Concept"

    val allEndpoint: Endpoint[Unit, Unit, ErrorMsg, Seq[String], Any] = openEndpoint
        .get
        .in(base)
        .out(jsonBody[Seq[String]])
        .name("allConcepts")
        .description("Get all concept names")
        .tag(tag)

    val allEndpointImpl: ServerEndpoint[Any, Future] = allEndpoint.serverLogic { _ =>
        val limit  = 10000
        val offset = 0
        handleErrorsAsync(conceptCache.findAllNames(limit, offset))
    }

    val createEndpoint: Endpoint[Option[String], ConceptCreate, ErrorMsg, ConceptMetadata, Any] = secureEndpoint
        .post
        .in(base)
        .in(jsonBody[ConceptCreate])
        .out(jsonBody[ConceptMetadata])
        .name("createConcept")
        .description("Create a new concept")
        .tag(tag)

    val createEndpointImpl: ServerEndpoint[Any, Future] = createEndpoint
        .serverSecurityLogic(jwtOpt => verifyLoginAsync(jwtOpt))
        .serverLogic { userAccount => conceptCreate =>
            handleErrorsAsync(service.create(conceptCreate, userAccount.username))
                .andThen(v =>
                    conceptCache.clear()
                    v
                )
        }

    val deleteEndpoint: Endpoint[Option[String], String, ErrorMsg, Unit, Any] = secureEndpoint
        .delete
        .in(base / path[String]("name"))
        .out(jsonBody[Unit])
        .name("deleteConcept")
        .description("Delete a concept")
        .tag(tag)

    val deleteEndpointImpl: ServerEndpoint[Any, Future] = deleteEndpoint
        .serverSecurityLogic(jwtOpt => verifyLoginAsync(jwtOpt))
        .serverLogic { userAccount => name =>
            Future(service
                .delete(name, userAccount.username)
                .fold(
                    error => Left(ServerError(error.getMessage)),
                    _ => Right(())
                )
            ).andThen(v =>
                conceptCache.clear()
                v
            )
        }

    val findParentEndpoint: Endpoint[Unit, String, ErrorMsg, ConceptMetadata, Any] = openEndpoint
        .get
        .in(base / "parent" / path[String]("name"))
        .out(jsonBody[ConceptMetadata])
        .name("findParent")
        .description("Find the parent of a concept")
        .tag(tag)

    val findParentEndpointImpl: ServerEndpoint[Any, Future] = findParentEndpoint.serverLogic { name =>
        handleErrorsAsync(service.findParentByChildName(name))
    }

    val findChildrenEndpoint: Endpoint[Unit, String, ErrorMsg, Seq[ConceptMetadata], Any] = openEndpoint
        .get
        .in(base / "children" / path[String]("name"))
        .out(jsonBody[Seq[ConceptMetadata]])
        .name("findChildren")
        .description("Find the children of a concept")
        .tag(tag)

    val findChildrenEndpointImpl: ServerEndpoint[Any, Future] = findChildrenEndpoint.serverLogic { name =>
        handleErrorsAsync(service.findChildrenByParentName(name).map(_.toSeq.sortBy(_.name)))
    }

    val findByName: Endpoint[Unit, String, ErrorMsg, ConceptMetadata, Any] = openEndpoint
        .get
        .in(base / path[String]("name"))
        .out(jsonBody[ConceptMetadata])
        .name("findByName")
        .description("Find a concept by name")
        .tag(tag)

    val findByNameImpl: ServerEndpoint[Any, Future] = findByName.serverLogic { name =>
        handleErrorsAsync(conceptCache.findByName(name))
    }

    val findByNameContaining: Endpoint[Unit, String, ErrorMsg, Seq[ConceptMetadata], Any] = openEndpoint
        .get
        .in(base / "find" / path[String]("name"))
        .out(jsonBody[Seq[ConceptMetadata]])
        .name("findByNameContaining")
        .description("Find concepts by name containing")
        .tag(tag)

    val findByNameContainingImpl: ServerEndpoint[Any, Future] = findByNameContaining.serverLogic { name =>
        handleErrorsAsync(service.findByGlob(name).map(_.toSeq.sortBy(_.name)))
    }

    val findRoot: Endpoint[Unit, Unit, ErrorMsg, ConceptMetadata, Any] = openEndpoint
        .get
        .in(base / "query" / "root")
        .out(jsonBody[ConceptMetadata])
        .name("findRoot")
        .description("Find the root concept")
        .tag(tag)

    val findRootImpl: ServerEndpoint[Any, Future] = findRoot.serverLogic { _ =>
        handleErrorsAsync(service.findRoot())
    }

    val listValidRanks = openEndpoint
        .get
        .in(base / "ranks")
        .out(jsonBody[Seq[Rank]])
        .name("listValidRanks")
        .description("List valid ranks")
        .tag(tag)

    val listValidRanksImpl: ServerEndpoint[Any, Future] = listValidRanks.serverLogic { _ =>
        Future(Right(RankValidator.ValidRankLevelsAndNames.map {
            (rankLevel, rankName) => Rank(rankLevel, rankName)
        }))
    }

    val updateEndpoint: Endpoint[Option[String], (String, ConceptUpdate), ErrorMsg, ConceptMetadata, Any] =
        secureEndpoint
            .put
            .in(base / path[String]("name"))
            .in(jsonBody[ConceptUpdate])
            .out(jsonBody[ConceptMetadata])
            .name("updateConcept")
            .description("Update a concept. To remove a rank name or level, set it to an empty string. Only administrators can remove rank names and levels.")
            .tag(tag)

    val updateEndpointImpl: ServerEndpoint[Any, Future] = updateEndpoint
        .serverSecurityLogic(jwtOpt => verifyLoginAsync(jwtOpt))
        .serverLogic { userAccount => (name, conceptUpdate) =>
            Future(service
                .update(name, conceptUpdate, userAccount.username)
                .fold(
                    error => Left(ServerError(error.getMessage)),
                    concept => Right(concept)
                )
            )
            .andThen(v =>
                conceptCache.clear()
                v
            )
        }

    override val all: List[Endpoint[?, ?, ?, ?, ?]] = List(
        findRoot,
        findParentEndpoint,
        findChildrenEndpoint,
        findByNameContaining,
        listValidRanks,
        findByName,
        allEndpoint,
        createEndpoint,
        deleteEndpoint,
        updateEndpoint
    )

    override val allImpl: List[ServerEndpoint[Any, Future]] = List(
        findRootImpl,
        findParentEndpointImpl,
        findChildrenEndpointImpl,
        findByNameContainingImpl,
        listValidRanksImpl,
        findByNameImpl,
        allEndpointImpl,
        createEndpointImpl,
        deleteEndpointImpl,
        updateEndpointImpl
    )
