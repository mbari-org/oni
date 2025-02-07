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

package org.mbari.oni.issues

import org.mbari.oni.endpoints.{EndpointsSuite, HistoryEndpoints}
import org.mbari.oni.etc.jwt.JwtService
import org.mbari.oni.jdbc.FastPhylogenyService
import org.mbari.oni.jpa.DataInitializer
import org.mbari.oni.services.{HistoryService, LinkTemplateService, UserAuthMixin}

trait Oni7 extends EndpointsSuite with DataInitializer with UserAuthMixin:

    given jwtService: JwtService = JwtService("mbari", "foo", "bar")

    lazy val fastPhylogenyService               = new FastPhylogenyService(entityManagerFactory)
    lazy val historyService                     = new HistoryService(entityManagerFactory)
    lazy val historyEndpoints: HistoryEndpoints = HistoryEndpoints(entityManagerFactory, fastPhylogenyService)
    private val password                        = "foofoofoo"

    test("mbari-org/oni#7 - accept rank level replace") {
        val root = init(2, 0)

    }

    test("mbari-org/oni#7 - reject rank level replace") {
        val root = init(2, 0)

    }

    test("mbari-org/oni#7 - accept rank name replace") {
        val root = init(2, 0)

    }

    test("mbari-org/oni#7 - reject rank name replace") {
        val root = init(2, 0)

    }
