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
import org.mbari.oni.domain.RawConcept
import org.mbari.oni.jpa.DataInitializer
import sttp.model.StatusCode
import org.mbari.oni.etc.circe.CirceCodecs.{*, given}

trait RawEndpointsSuite extends EndpointsSuite with DataInitializer {

    lazy val endpoints: RawEndpoints = RawEndpoints(entityManagerFactory)


    test("findRawConceptByName") {
        val root = init(2, 2)
        val child = root.getChildConcepts.iterator().next()
        val name  = child.getPrimaryConceptName.getName

        runGet(
            endpoints.findRawConceptByNameImpl,
            s"http://test.com/v1/raw/concept/${name}",
            response =>
                assertEquals(response.code, StatusCode.Ok)
                val obtained = checkResponse[RawConcept](response.body)
                val expected = RawConcept.from(child)
                assertEquals(obtained.primaryName, name)
                assertEquals(obtained, expected)
                // println(rawConcept.stringify)
        )

    }
}
