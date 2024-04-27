/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2024
 *
 * oni code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential. 
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
