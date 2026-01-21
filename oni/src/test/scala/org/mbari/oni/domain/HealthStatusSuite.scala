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

package org.mbari.oni.domain

class HealthStatusSuite extends munit.FunSuite {
  
    test("HealthStatus.Default") {
        val healthStatus = HealthStatus.Default
        assertEquals(healthStatus.jdkVersion, Runtime.version.toString)
        assertEquals(healthStatus.availableProcessors, Runtime.getRuntime.availableProcessors)
        assert(healthStatus.freeMemory > 0)
        assert(healthStatus.maxMemory > 0)
        assert(healthStatus.totalMemory > 0)
        assertEquals(healthStatus.application, "oni")
        assert(healthStatus.version != null)
        assert(healthStatus.description != null)
    }
}
