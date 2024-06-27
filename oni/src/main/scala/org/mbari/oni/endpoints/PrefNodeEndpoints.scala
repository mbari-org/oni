/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2024
 *
 * oni code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential. 
 */

package org.mbari.oni.endpoints

import jakarta.persistence.EntityManagerFactory
import org.mbari.oni.AccessDenied
import org.mbari.oni.domain.{ErrorMsg, PrefNode, Unauthorized}
import sttp.tapir.*
import sttp.tapir.Endpoint
import sttp.tapir.json.circe.*
import sttp.tapir.server.ServerEndpoint
import sttp.shared.Identity
import org.mbari.oni.etc.circe.CirceCodecs.{*, given}
import org.mbari.oni.etc.jwt.JwtService
import org.mbari.oni.services.PrefNodeService

class PrefNodeEndpoints(entityManagerFactory: EntityManagerFactory)(using jwtService: JwtService) extends Endpoints:

    private val service = PrefNodeService(entityManagerFactory)

    private val base = "prefs"
    private val tag  = "Preferences"

    val findAll: Endpoint[Option[String], Paging, ErrorMsg, Seq[PrefNode], Any] =
        secureEndpoint
            .get
            .in(base)
            .in(paging)
             .out(jsonBody[Seq[PrefNode]])
            .name("allPrefNodes")
            .description("Get all prefNodes. This endpoint is only available to administrators.")
            .tag(tag)

    val findAllImpl: ServerEndpoint[Any, Identity] = findAll
        .serverSecurityLogic(jwtOpt => verifyLogin(jwtOpt))
        .serverLogic { userAccount => paging =>
            if (userAccount.isAdministrator) handleErrors(service.findAll(paging.limit.getOrElse(100), paging.offset.getOrElse(0)))
            else Left(Unauthorized("You must be an admin to access this endpoint"))
        }


    val findByNodeNameAndKey: Endpoint[Unit, (String, Option[String]), ErrorMsg, Seq[PrefNode], Any] =
        openEndpoint
            .get
            .in(base)
            .in(
                query[String]("name")
                    .description("Name of the prefNode")
                    .example("name")
            )
            .in(
                query[Option[String]]("key")
                    .description("Key of the prefNode")
            )
            .out(jsonBody[Seq[PrefNode]])
            .name("byNodeNameAndKey")
            .description("Get all prefNode names matching a node name and key")
            .tag(tag)

    val findByNodeNameAndKeyImpl: ServerEndpoint[Any, Identity] = findByNodeNameAndKey.serverLogic { (name, keyOpt) =>
        handleErrors(keyOpt match
            case Some(key) =>
                service
                    .findByNodeNameAndKey(name, key)
                    .map {
                        case Some(p) => Seq(p)
                        case None    => Nil
                    }
            case None      => service.findByNodeName(name)
        )
    }

    val findByPrefix: Endpoint[Unit, String, ErrorMsg, Seq[PrefNode], Any] =
        openEndpoint
            .get
            .in(base / "startswith")
            .in(
                query[String]("prefix")
                    .description("Prefix of full node name")
                    .example("prefix")
            )
            .out(jsonBody[Seq[PrefNode]])
            .name("findByPrefix")
            .description("Find all preferences with a given prefix")
            .tag(tag)

    val findByPrefixImpl: ServerEndpoint[Any, Identity] = findByPrefix.serverLogic { prefix =>
        handleErrors(service.findByNodeNameLike(prefix))
    }

    val createEndpoint: Endpoint[Option[String], PrefNode, ErrorMsg, PrefNode, Any] = secureEndpoint
        .post
        .in(base)
        .in(oneOfBody(jsonBody[PrefNode], formBody[PrefNode]))
        .out(jsonBody[PrefNode])
        .name("createPrefNode")
        .description("Create a new prefNode")
        .tag(tag)

    val createEndpointImpl: ServerEndpoint[Any, Identity] =
        createEndpoint
            .serverSecurityLogic(jwtOpt => verify(jwtOpt))
            .serverLogic { _ => prefNode => handleErrors(service.create(prefNode)) }

    val updateEndpoint: Endpoint[Option[String], PrefNode, ErrorMsg, PrefNode, Any] =
        secureEndpoint
            .put
            .in(base)
            .in(oneOfBody(jsonBody[PrefNode], formBody[PrefNode]))
            .out(jsonBody[PrefNode])
            .name("updatePrefNode")
            .description("Update a prefNode")
            .tag(tag)

    val updateEndpointImpl: ServerEndpoint[Any, Identity] =
        updateEndpoint
            .serverSecurityLogic(jwtOpt => verify(jwtOpt))
            .serverLogic { _ => prefNode => handleErrors(service.update(prefNode)) }

    val deleteEndpoint: Endpoint[Option[String], (String, String), ErrorMsg, Unit, Any] =
        secureEndpoint
            .delete
            .in(base)
            .in(
                query[String]("name")
                    .description("Name of the prefNode")
                    .example("name")
            )
            .in(
                query[String]("key")
                    .description("Key of the prefNode")
                    .example("key")
            )
            .out(jsonBody[Unit])
            .name("deletePrefNode")
            .description("Delete a prefNode")
            .tag(tag)

    val deleteEndpointImpl: ServerEndpoint[Any, Identity] = deleteEndpoint
        .serverSecurityLogic(jwtOpt => verify(jwtOpt))
        .serverLogic { _ => (name, key) => handleErrors(service.delete(name, key)) }

    override def all: List[Endpoint[_, _, _, _, _]] = List(
        findByPrefix,
        findByNodeNameAndKey,
        createEndpoint,
        updateEndpoint,
        deleteEndpoint
    )

    override def allImpl: List[ServerEndpoint[Any, Identity]] = List(
        findByPrefixImpl,
        findByNodeNameAndKeyImpl,
        createEndpointImpl,
        updateEndpointImpl,
        deleteEndpointImpl
    )
