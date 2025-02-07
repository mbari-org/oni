/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2024
 *
 * oni code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential.
 */

package org.mbari.oni

import com.typesafe.config.ConfigFactory
import scala.util.Try
import org.mbari.oni.jpa.EntityManagerFactories
import jakarta.persistence.EntityManagerFactory

/**
 * Parse configuration info from reference.conf and application.conf
 */
object AppConfig:

    val Config = ConfigFactory.load()

    val Name: String = "oni"

    val Version: String =
        val default = "0.0.0-SNAPSHOT"
        try Option(getClass.getPackage.getImplementationVersion).getOrElse(default)
        catch case _: Exception => default

    val Description: String = "Organism Naming Infrastructure: Knowledge-base and User Accounts"

    val NumberOfThreads: Int = Config.getInt("database.threads")

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

    lazy val DefaultEntityManagerFactory: EntityManagerFactory = EntityManagerFactories("database")

case class HttpConfig(
    port: Int,
    stopTimeout: Int,
    connectorIdleTimeout: Int,
    contextPath: String
)

case class JwtConfig(issuer: String, apiKey: String, signingSecret: String)

case class DatabaseConfig(logLevel: String, driver: String, url: String, user: String, password: String)
