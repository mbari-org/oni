/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2024
 *
 * oni code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential.
 */

package org.mbari.oni.etc.flyway

import org.flywaydb.core.Flyway
import org.mbari.oni.config.DatabaseConfig
import org.mbari.oni.etc.jdk.Loggers.given

import scala.util.Try

object FlywayMigrator:

    private val log = System.getLogger(getClass.getName)

    def migrate(databaseConfig: DatabaseConfig): Either[Throwable, Unit] =
        // Implementation of Flyway migration logic
        Try {
            val location = if (databaseConfig.isSqlserver) "classpath:/db/migrations/sqlserver"
            else if (databaseConfig.isPostgres) "classpath:/db/migrations/postgres"
            else throw new IllegalArgumentException(s"Unsupported database type: ${databaseConfig.driver}")

            log.atInfo.log("Starting Flyway migration using SQL in " + location)
            val flyway       = Flyway
                .configure()
                .baselineOnMigrate(true)
                .dataSource(databaseConfig.url, databaseConfig.user, databaseConfig.password)
                .locations(location)
                .load()

            val result = flyway.migrate()
            if !result.success then throw new Exception("Migration failed using SQL in " + location)
        }.toEither