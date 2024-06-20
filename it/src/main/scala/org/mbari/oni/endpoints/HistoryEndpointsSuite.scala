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

import org.mbari.oni.domain.{ExtendedHistory, Page}
import org.mbari.oni.jpa.DataInitializer
import sttp.model.StatusCode
import org.mbari.oni.etc.circe.CirceCodecs.{*, given}

trait HistoryEndpointsSuite extends EndpointsSuite with DataInitializer:

    lazy val endpoints: HistoryEndpoints = HistoryEndpoints(entityManagerFactory)

    test("pending") {
        init(3, 5)
        runGet(
            endpoints.pendingEndpointImpl,
            "http://test.com/v1/history/pending",
            response =>
                assertEquals(response.code, StatusCode.Ok)
                val histories = checkResponse[Page[Seq[ExtendedHistory]]](response.body)
                assert(histories.content.nonEmpty)
        )
    }

    test("approved") {
        init(3, 5)
        runGet(
            endpoints.approvedEndpointsImpl,
            "http://test.com/v1/history/approved",
            response =>
                assertEquals(response.code, StatusCode.Ok)
                val histories = checkResponse[Page[Seq[ExtendedHistory]]](response.body)
                assert(histories.content.nonEmpty)
        )
    }
