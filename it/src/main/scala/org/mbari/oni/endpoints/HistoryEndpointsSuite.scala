package org.mbari.oni.endpoints

import org.mbari.oni.domain.ExtendedHistory
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
            response => {
                assertEquals(response.code, StatusCode.Ok)
                val histories = checkResponse[Seq[ExtendedHistory]](response.body)
                assert(histories.nonEmpty)
            }
        )
    }

    test("approved") {
        init(3, 5)
        runGet(
            endpoints.approvedEndpointsImpl,
            "http://test.com/v1/history/approved",
            response => {
                assertEquals(response.code, StatusCode.Ok)
                val histories = checkResponse[Seq[ExtendedHistory]](response.body)
                assert(histories.nonEmpty)
            }
        )
    }


