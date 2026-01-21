/*
 * Copyright 2024 Monterey Bay Aquarium Research Institute
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.mbari.oni.endpoints

import jakarta.persistence.EntityManagerFactory
import org.mbari.oni.domain.{ErrorMsg, ExtendedLink, Link}
import org.mbari.oni.etc.circe.CirceCodecs.given
import org.mbari.oni.services.LinkService
import sttp.tapir.json.circe.*
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.{Endpoint, *}

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
        handleErrorsAsync(service.findAllLinkTemplatesForConcept(name).map(_.map(_.toLink)))
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
            handleErrorsAsync(service.findLinkTemplatesByNameForConcept(name, linkName).map(_.map(_.toLink)))
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
