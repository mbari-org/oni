/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2024
 *
 * oni code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential.
 */

package org.mbari.oni.config

import com.typesafe.config.ConfigFactory
import com.zaxxer.hikari.HikariDataSource
import jakarta.persistence.EntityManagerFactory
import org.mbari.oni.etc.flyway.FlywayMigrator
import org.mbari.oni.jpa.EntityManagerFactories

/**
 * Parse configuration info from reference.conf and application.conf
 */
object AppConfig:

    private val Config = ConfigFactory.load()

    val Name: String = "oni"

    val Version: String =
        val default = "0.0.0-SNAPSHOT"
        try Option(getClass.getPackage.getImplementationVersion).getOrElse(default)
        catch case _: Exception => default

    val Description: String = "Organism Naming Infrastructure: Knowledge-base and User Accounts"

    val NumberOfThreads: Int = Config.getInt("database.threads")

    /** We should have the same # of max db connections as vertx workers */
    val NumberOfVertxWorkers: Int = NumberOfThreads

    lazy val DefaultJwtConfig: JwtConfig = JwtConfig(
        issuer = Config.getString("basicjwt.issuer"),
        apiKey = Config.getString("basicjwt.client.secret"),
        signingSecret = Config.getString("basicjwt.signing.secret")
    )

    lazy val DefaultHttpConfig: HttpConfig = HttpConfig(
        port = Config.getInt("http.port"),
        stopTimeout = Config.getInt("http.stop.timeout"),
        connectorIdleTimeout = Config.getInt("http.connector.idle.timeout"),
        contextPath = Config.getString("http.context.path")
    )

    lazy val DefaultDatabaseConfig: DatabaseConfig = DatabaseConfig(
        logLevel = Config.getString("database.loglevel"),
        driver = Config.getString("database.driver"),
        url = Config.getString("database.url"),
        user = Config.getString("database.user"),
        password = Config.getString("database.password")
    )

    lazy val DefaultEntityManagerFactory: EntityManagerFactory = {
        EntityManagerFactories("database")
    }


