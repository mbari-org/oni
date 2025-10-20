/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2024
 *
 * oni code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential.
 */

package org.mbari.oni.etc.flyway

import org.flywaydb.core.Flyway
import org.mbari.oni.config.DatabaseConfig
import org.mbari.oni.etc.jdbc.Databases
import org.mbari.oni.etc.jdk.Loggers.given

import scala.util.Try

object FlywayMigrator:

    private val log = System.getLogger(getClass.getName)

    /**
     * Run Flyway database migrations. It will baseline the database if no metadata table exists.
     * @param databaseConfig
     *   The database configuration
     * @return
     *   true if migrations were successful, false otherwise. The app should exit if false is returned.
     */
    def migrate(databaseConfig: DatabaseConfig): Either[Throwable, Unit] =

        Try {

            val databaseType = Databases.typeFromUrl(databaseConfig.url)
            val location     = databaseType match
                case Databases.DatabaseType.SQLServer  => "classpath:/db/migrations/sqlserver"
                case Databases.DatabaseType.PostgreSQL => "classpath:/db/migrations/postgres"
                case _                                 => throw new IllegalArgumentException(s"Unsupported database type: $databaseType")

            log.atDebug.log(s"Starting database migrations on ${databaseConfig.url}")
            val flyway = Flyway
                .configure()
                .table("schema_history_oni") // name of the metadata table
                .locations(location)         // migration scripts location
                .dataSource(databaseConfig.url, databaseConfig.user, databaseConfig.password)
                .baselineOnMigrate(true)     // this makes Flyway baseline if no metadata table exists
                .load()

            val result = flyway.migrate()
            result.migrationsExecuted match
                case 0          => log.atInfo.log("No database migrations were necessary")
                case n if n > 0 => log.atInfo.log(s"Successfully applied $n database migrations")
                case _          => log.atWarn.log("Database migration result was unexpected")

            if !result.success then throw new Exception("Migration failed using SQL in " + location)

            log.atInfo
                .log(
                    "Flyway Database migrations applied. Current schema version: " + flyway.info().current().getVersion
                )
        }.toEither
