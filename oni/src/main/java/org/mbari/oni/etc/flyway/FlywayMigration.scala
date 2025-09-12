/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2024
 *
 * oni code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential.
 */

package org.mbari.oni.etc.flyway

import org.flywaydb.core.Flyway
import org.mbari.oni.DatabaseConfig
import org.mbari.oni.etc.jdk.Loggers.given

import scala.util.Try

object FlywayMigration:

    private val log = System.getLogger(getClass.getName)

    def migrate(databaseConfig: DatabaseConfig): Boolean =

        Try {

            log.atInfo.log(s"Starting database migrations on ${databaseConfig.url}")
            val flyway = Flyway
                .configure()
                .dataSource(databaseConfig.url, databaseConfig.user, databaseConfig.password)
                .baselineOnMigrate(true) // <-- this makes Flyway baseline if no metadata table exists
                .load()

            val result = flyway.migrate()
            result.migrationsExecuted match
                case 0          => log.atInfo.log("No database migrations were necessary")
                case n if n > 0 => log.atInfo.log(s"Successfully applied $n database migrations")
                case _          => log.atWarn.log("Database migration result was unexpected")

            log.atInfo
                .log(
                    "Flyway Database migrations applied. Current schema version: " + flyway.info().current().getVersion
                )
        } match
            case scala.util.Success(_) => true
            case scala.util.Failure(e) =>
                log.atError.withCause(e).log("Error during database migration: " + e.getMessage)
                false
