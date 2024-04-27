/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2024
 *
 * oni code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential. 
 */

package org.mbari.oni.endpoints

import sttp.model.StatusCode
import sttp.tapir.*
import sttp.tapir.Endpoint
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.server.ServerEndpoint
import org.mbari.oni.domain.{Authorization, BadRequest, ErrorMsg, NotFound, ServerError, Unauthorized}
import org.mbari.oni.etc.circe.CirceCodecs.given
import sttp.tapir.server.nima.Id

class ConceptEndpoints extends Endpoints:

    override val all: List[Endpoint[?, ?, ?, ?, ?]]     = ???
    override val allImpl: List[ServerEndpoint[Any, Id]] = ???
