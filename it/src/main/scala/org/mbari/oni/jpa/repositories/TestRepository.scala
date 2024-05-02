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

package org.mbari.oni.jpa.repositories

import jakarta.persistence.EntityManagerFactory
import org.mbari.oni.domain.RawConcept
import org.mbari.oni.jpa.entities.ConceptEntity
import org.mbari.oni.etc.circe.CirceCodecs.{*, given}
import org.mbari.oni.etc.jdk.Loggers

import java.io.{BufferedInputStream, InputStream}
import java.nio.file.{FileSystems, Files, Path, Paths}
import java.util.Scanner
import java.util.zip.ZipFile
import scala.util.{Failure, Success, Using}
import scala.util.control.NonFatal
import scala.jdk.CollectionConverters.*
import Loggers.given

object TestRepository:

    private val log = System.getLogger(getClass.getName)

    def init(entityManagerFactory: EntityManagerFactory): ConceptEntity = {
      val url = getClass.getResource("/kb/kb-dump.json.zip")
      val path = Paths.get(url.toURI)
      read(path) match
        case None => throw new RuntimeException("Failed to read test data");
        case Some(rawConcept) =>
          log.atInfo.log(s"Inserting a kb tree from $path")
          val conceptEntity = rawConcept.toEntity
          val entityManager = entityManagerFactory.createEntityManager()
          val transaction = entityManager.getTransaction
          transaction.begin()
          entityManager.persist(conceptEntity)
          transaction.commit()
          entityManager.close()
          conceptEntity
    }

    def read(path: Path): Option[RawConcept] =
       log.atDebug.log(s"Reading file: $path")
        if path.getFileName.toString.endsWith(".zip") then
            readZip(path)
        else {
            Using(Files.newInputStream(path)) { stream =>
                read(stream)
            }.toOption.flatten
        }

    def readZip(path: Path): Option[RawConcept] =
      var opt = Option.empty[RawConcept]
      log.atDebug.log(s"Reading zip file: $path")
      Using(new ZipFile(path.toFile)) { zipFile =>
        val entries = zipFile.entries()
        while(entries.hasMoreElements) {
          val entry = entries.nextElement()
          if (!entry.isDirectory && opt.isEmpty) {
            Using(zipFile.getInputStream(entry)) { stream =>
              log.atInfo.log(s"Reading kb from $path using entry ${entry.getName}")
              opt = read(stream)
            }
          }
        }
      }
      opt


    private def read(stream: InputStream): Option[RawConcept] =
        val scanner = Scanner(stream).useDelimiter("\\A")
        val json = if scanner.hasNext then scanner.next() else ""
        val e = json.reify[RawConcept]
        e match
            case Right(rawConcept) => Some(rawConcept)
            case Left(error) =>
                println(s"Failed to parse json: $error")
                error.printStackTrace()
                None
