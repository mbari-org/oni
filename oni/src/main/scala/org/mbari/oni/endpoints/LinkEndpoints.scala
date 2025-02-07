/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2024
 *
 * oni code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential.
 */

package org.mbari.oni.endpoints

import jakarta.persistence.EntityManagerFactory
import org.mbari.oni.domain.{ErrorMsg, ExtendedLink, Link}
import org.mbari.oni.etc.circe.CirceCodecs.given
import org.mbari.oni.services.LinkService
import sttp.shared.Identity
import sttp.tapir.*
import sttp.tapir.Endpoint
import sttp.tapir.json.circe.*
import sttp.tapir.server.ServerEndpoint

import scala.concurrent.{ExecutionContext, Future}

class LinkEndpoints(entityManagerFactory: EntityManagerFactory)(using executionContext: ExecutionContext)
    extends Endpoints:

    private val service = LinkService(entityManagerFactory)
    private val base    = "links"
    private val tag     = "Links"
    // get all links

    val allLinksEndpoint: Endpoint[Unit, Unit, ErrorMsg, Seq[Link], Any] = openEndpoint
        .get
        .in(base)
        .out(jsonBody[Seq[Link]])
        .name("links")
        .description("Get all link templates")
        .tag(tag)

    val allLinksEndpointImpl: ServerEndpoint[Any, Future] = allLinksEndpoint.serverLogic { _ =>
        // HACK: 20000 is a hack to get all links. Need to fix this
        handleErrorsAsync(service.findAllLinkTemplates(20000))
    }

    // get links for a concept
    val linksForConceptEndpoint: Endpoint[Unit, String, ErrorMsg, Seq[Link], Any] = openEndpoint
        .get
        .in(base / path[String]("name"))
        .out(jsonBody[Seq[Link]])
        .name("linksForConcept")
        .description("Get all link templates applicable to a concept")
        .tag(tag)

    val linksForConceptEndpointImpl: ServerEndpoint[Any, Future] = linksForConceptEndpoint.serverLogic { name =>
        handleErrorsAsync(service.findAllLinkTemplatesForConcept(name))
    }

    // get links for a concept and linkname
    val linksForConceptAndLinkNameEndpoint: Endpoint[Unit, (String, String), ErrorMsg, Seq[Link], Any] = openEndpoint
        .get
        .in(base / path[String]("name") / "using" / path[String]("linkName"))
        .out(jsonBody[Seq[Link]])
        .name("linksForConceptAndLinkName")
        .description("Get all link templates applicable to a concept and link name")
        .tag(tag)

    val linksForConceptAndLinkNameEndpointImpl: ServerEndpoint[Any, Future] =
        linksForConceptAndLinkNameEndpoint.serverLogic { (name, linkName) =>
            handleErrorsAsync(service.findLinkTemplatesByNameForConcept(name, linkName))
        }

    // get link realizations for a concept
    val linkRealizationsEndpoint: Endpoint[Unit, String, ErrorMsg, Seq[ExtendedLink], Any] = openEndpoint
        .get
        .in(base / "query" / "linkrealizations" / path[String]("linkName"))
        .out(jsonBody[Seq[ExtendedLink]])
        .name("linkRealizations")
        .description("Get all link realizations for a link name")
        .tag(tag)

    val linkRealizationsEndpointImpl: ServerEndpoint[Any, Future] = linkRealizationsEndpoint.serverLogic { linkName =>
        handleErrorsAsync(service.findLinkRealizationsByLinkName(linkName))
    }

    override def all: List[Endpoint[?, ?, ?, ?, ?]] = List(
        linkRealizationsEndpoint,
        linksForConceptAndLinkNameEndpoint,
        linksForConceptEndpoint, // TODO verify this order works
        allLinksEndpoint
    )

    override def allImpl: List[ServerEndpoint[Any, Future]] = List(
        linkRealizationsEndpointImpl,
        linksForConceptAndLinkNameEndpointImpl,
        linksForConceptEndpointImpl,
        allLinksEndpointImpl
    )
