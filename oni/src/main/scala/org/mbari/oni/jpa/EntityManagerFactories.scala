/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2024
 *
 * oni code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential. 
 */

package org.mbari.oni.jpa

import com.typesafe.config.ConfigFactory
import jakarta.persistence.{EntityManagerFactory, Persistence}
import org.mbari.oni.AppConfig

import scala.jdk.CollectionConverters.*
import org.mbari.oni.etc.jdk.Loggers.given

import java.lang.System.Logger.Level

/**
 * https://stackoverflow.com/questions/4106078/dynamic-jpa-connection
 *
 * THis factory allows us to instantiate an javax.persistence.EntityManager from the basic parameters (url, driver,
 * password, username). You can pass in a map of additional properties to customize the EntityManager.
 *
 * @author
 *   Brian Schlining
 * @since 2016-05-05T17:29:00
 */
object EntityManagerFactories:

    private val log = System.getLogger(getClass.getName)

    private lazy val config = ConfigFactory.load()

    // https://juliuskrah.com/tutorial/2017/02/16/getting-started-with-hikaricp-hibernate-and-jpa/
    val PRODUCTION_PROPS = Map(
        "hibernate.connection.provider_class" -> "org.hibernate.hikaricp.internal.HikariCPConnectionProvider",
        "hibernate.hbm2ddl.auto"              -> "validate",
        "hibernate.hikari.idleTimeout"        -> "30000",
        "hibernate.jdbc.batch_size"           -> "100",
        "hibernate.hikari.maximumPoolSize"    -> "16", // Same as vertx worker pool threads
        "hibernate.hikari.minimumIdle"        -> "2",
        "hibernate.order_inserts"             -> "true",
        "hibernate.order_updates"             -> "true"
    )

    def apply(properties: Map[String, String]): EntityManagerFactory =
        val props = PRODUCTION_PROPS ++ properties
        val emf   = Persistence.createEntityManagerFactory("oni", props.asJava)
        if log.isLoggable(Level.INFO) then
            val props = emf
                .getProperties
                .asScala
                .filter(a => a._1.startsWith("hibernate") || a._1.startsWith("jakarta"))
                .map(a => s"${a._1} : ${a._2}")
                .toList
                .sorted
                .mkString("\n")
            log.atInfo.log(s"EntityManager Properties:\n${props}")
        emf

    def apply(
        url: String,
        username: String,
        password: String,
        driverName: String,
        properties: Map[String, String] = Map.empty
    ): EntityManagerFactory =

        val map = Map(
            "jakarta.persistence.jdbc.url"      -> url,
            "jakarta.persistence.jdbc.user"     -> username,
            "jakarta.persistence.jdbc.password" -> password,
            "jakarta.persistence.jdbc.driver"   -> driverName
        )
        apply(map ++ properties)

    def apply(configNode: String): EntityManagerFactory =
        val driver   = config.getString(configNode + ".driver")
        val password = config.getString(configNode + ".password")
        val url      = config.getString(configNode + ".url")
        val user     = config.getString(configNode + ".user")
        apply(url, user, password, driver)
