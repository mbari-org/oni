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

package org.mbari.oni.services

import org.mbari.oni.domain.MediaCreate
import org.mbari.oni.jdbc.FastPhylogenyService
import org.mbari.oni.jpa.DataInitializer

import java.net.URI

trait Issue6 extends DataInitializer with UserAuthMixin:

    lazy val fastPhylogenyService = new FastPhylogenyService(entityManagerFactory)
    lazy val historyService       = new HistoryService(entityManagerFactory)
    lazy val historyActionService = new HistoryActionService(entityManagerFactory, fastPhylogenyService)
    lazy val mediaService         = new MediaService(entityManagerFactory, fastPhylogenyService)

    test("6 - Duplicate ConceptMetadata") {
        val root        = init(2, 0)
        assert(root != null)
        val a           = conceptService.findByName(root.getName)
        val mediaCreate = MediaCreate(
            conceptName = root.getName,
            url = URI.create("http://www.mbari.org").toURL
        )

    }
