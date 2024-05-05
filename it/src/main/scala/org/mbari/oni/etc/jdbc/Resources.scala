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

import java.io.{BufferedReader, InputStream, InputStreamReader}
import java.nio.file.{Path, Paths}
import scala.util.Using

object Resources:

    def getResourceFiles(path: String): List[String] =
        Using(BufferedReader(InputStreamReader(getResourceAsStream(path)))) { reader =>
            Iterator.continually(reader.readLine()).takeWhile(_ != null).toList
        }.get

    def getResourcePath(path: String): Path =
        val url = getClass.getResource(path)
        Paths.get(url.toURI.getPath)

    def getResourcePaths(path: String): List[Path] =
        for file <- getResourceFiles(path)
        yield getResourcePath(s"$path/$file")

    def getResourceAsStream(resource: String): InputStream =
        val inputStream = Thread.currentThread().getContextClassLoader.getResourceAsStream(resource)
        if inputStream == null then getClass.getResourceAsStream(resource)
        else inputStream
