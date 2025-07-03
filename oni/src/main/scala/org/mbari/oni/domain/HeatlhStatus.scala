/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2024
 *
 * oni code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential.
 */

package org.mbari.oni.domain

import org.mbari.oni.config.AppConfig

final case class HealthStatus(
    jdkVersion: String,
    availableProcessors: Int,
    freeMemory: Long,
    maxMemory: Long,
    totalMemory: Long,
    application: String = AppConfig.Name,
    version: String = AppConfig.Version,
    description: String = AppConfig.Description
)

object HealthStatus:

    def Default: HealthStatus =
        val runtime = Runtime.getRuntime
        HealthStatus(
            jdkVersion = Runtime.version.toString,
            availableProcessors = runtime.availableProcessors,
            freeMemory = runtime.freeMemory,
            maxMemory = runtime.maxMemory,
            totalMemory = runtime.totalMemory
        )
