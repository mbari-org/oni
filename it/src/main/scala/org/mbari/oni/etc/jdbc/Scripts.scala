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

package org.mbari.oni.etc.jdbc

import jakarta.persistence.EntityManager
import munit.internal.io.PlatformIO.Path
import org.mbari.oni.etc.jdk.Files

import java.nio.file.Files as JFiles
import org.mbari.oni.etc.jpa.EntityManagers.*

object Scripts:

    /**
     * Generate a temporary file that concatenates all the SQL files in the given resource path
     * @param srcPath
     *   the resource path to the SQL files, e.g "/sql". This is not a file path but a path to a directory in the
     *   resources of the java classpath
     * @return
     *   the path to the generated file
     */
    def generate(srcPath: String): Path =
        val resources = Resources.getResourcePaths(srcPath).sortBy(_.toString)
        val target    = JFiles.createTempFile("init", ".sql")
        Files.concatenate(resources, target)
        target

    /**
     * Run the SQL script at the given path. It is assumed that the script contains multiple SQL statements separated by
     * a semicolon.
     * @param scriptPath
     *   the path to the SQL script
     * @param entityManager
     *   the entity manager to use
     * @return
     */
    def run(scriptPath: Path, entityManager: EntityManager): Either[Throwable, Unit] =
        val lines      = JFiles.readAllLines(scriptPath)
        val sql        = String.join("\n", lines)
        val statements = sql
            .split(";")
            .map(_.trim)
            .filter(s => s.nonEmpty || s.startsWith("--"))

        entityManager.runTransaction { em =>
            statements.foreach { statement =>
                val query = em.createNativeQuery(statement)
                query.executeUpdate()
            }
        }
