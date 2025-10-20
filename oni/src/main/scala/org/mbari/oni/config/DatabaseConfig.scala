/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2024
 *
 * oni code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential.
 */

package org.mbari.oni.config

import com.zaxxer.hikari.HikariDataSource

case class DatabaseConfig(logLevel: String, driver: String, url: String, user: String, password: String):

    lazy val dataSource: HikariDataSource =
        val ds = new com.zaxxer.hikari.HikariDataSource()
        ds.setJdbcUrl(url)
        ds.setUsername(user)
        ds.setPassword(password)
        ds.setDriverClassName(driver)
        ds.setMaximumPoolSize(AppConfig.NumberOfVertxWorkers)
        ds

    def newConnection(): java.sql.Connection =
        dataSource.getConnection()
    // Class.forName(driver)
    // java.sql.DriverManager.getConnection(url, user, password)

    def isPostgres: Boolean  = driver.toLowerCase.contains("postgresql")
    def isSqlserver: Boolean = driver.toLowerCase.contains("sqlserver")
