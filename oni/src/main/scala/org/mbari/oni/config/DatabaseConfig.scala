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
