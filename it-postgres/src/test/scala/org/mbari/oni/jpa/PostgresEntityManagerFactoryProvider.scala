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

package org.mbari.oni.jpa

import jakarta.persistence.EntityManagerFactory
import org.mbari.oni.jpa.entities.ConceptEntity
import org.mbari.oni.jpa.repositories.TestRepository
import org.testcontainers.containers.{JdbcDatabaseContainerProvider, PostgreSQLContainer}

object PostgresEntityManagerFactoryProvider extends EntityManagerFactoryProvider {

  val container = new PostgreSQLContainer("postgres:16")
  container.withInitScript("sql/02_m3_kb.sql")
  container.withReuse(true)
  container.start()


  // NOTE: calling container.stop() after each test causes the tests to lose the connection to the database.
  // I'm using a shutdown hook to close the container at the end of the tests.
  //  override def afterAll(): Unit  = container.stop()
  Runtime.getRuntime.addShutdownHook(new Thread(() => container.stop()))

  val testProps: Map[String, String] =
      Map(
        "hibernate.dialect" -> "org.hibernate.dialect.PostgreSQLDialect",
        "hibernate.hbm2ddl.auto" -> "validate",
        "hibernate.hikari.idleTimeout" -> "1000",
        "hibernate.hikari.maxLifetime" -> "3000",
        "jakarta.persistence.schema-generation.scripts.action" -> "drop-and-create",
        "jakarta.persistence.schema-generation.scripts.create-target" -> "target/test-database-create.ddl",
        "jakarta.persistence.schema-generation.scripts.drop-target"   -> "target/test-database-drop.ddl"
      )

  lazy val entityManagerFactory: EntityManagerFactory =
    val driver = "org.postgresql.Driver"
    Class.forName(driver)
    EntityManagerFactories(
      container.getJdbcUrl,
      container.getUsername,
      container.getPassword,
      container.getDriverClassName,
      testProps
    )

  lazy val init: ConceptEntity = TestRepository.init(entityManagerFactory)

}
