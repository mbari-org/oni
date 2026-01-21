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

package org.mbari.oni.config

import com.typesafe.config.ConfigFactory
import jakarta.persistence.EntityManagerFactory
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

    lazy val DefaultEntityManagerFactory: EntityManagerFactory =
        EntityManagerFactories("database")
