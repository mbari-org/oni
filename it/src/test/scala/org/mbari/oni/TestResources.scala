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

package org.mbari.oni


import java.net.URI
import java.nio.file.{FileSystem, FileSystemAlreadyExistsException, FileSystems, Files, Path, Paths, StandardCopyOption}
import java.util as ju
import scala.jdk.CollectionConverters.*
import scala.util.Using

/**
 * Resolves a test fixture on the classpath to a real path on the default filesystem.
 *
 * The loaders under test ([[org.fathomnet.worms.io.WormsLoader]], [[org.fathomnet.worms.io.extended.ExtendedLoader]])
 * read from the filesystem, so fixtures have to be handed to them as real paths. Where those fixtures live depends on
 * the build: sbt 1.x put `test-classes`, a directory, on the test classpath, while sbt 2.x defaults `exportJars` to
 * true and puts `*-tests.jar` there instead. In the jar case `getResource` returns a `jar:` URL, which `Paths.get`
 * cannot resolve, so the resource is extracted to a temp directory that is deleted when the JVM exits.
 *
 * @author
 *   Brian Schlining
 * @since 2026-09-10
 */
object TestResources:

    private lazy val extractedRoot: Path =
        val dir = Files.createTempDirectory("worms-test-resources")
        Runtime.getRuntime.addShutdownHook(Thread(() => deleteRecursively(dir)))
        dir

    /**
     * Resolve a classpath resource, either a single file or a directory, to a real filesystem path.
     * @param resource
     *   The absolute classpath name of the resource, e.g. `/faketree`
     * @return
     *   A path that exists on the default filesystem
     */
    def path(resource: String): Path =
        val name = resource.stripPrefix("/")
        val url  = Option(getClass.getClassLoader.getResource(name))
            .getOrElse(throw new IllegalArgumentException(s"Test resource not found on the classpath: /$name"))
        url.getProtocol match
            case "file" => Paths.get(url.toURI)
            case "jar"  => extract(url.toURI, name)
            case other  =>
                throw new UnsupportedOperationException(s"Cannot resolve a `$other` resource to a file: $url")

    private def extract(uri: URI, name: String): Path =
        val target = extractedRoot.resolve(name)
        if Files.exists(target) then target
        else
            withFileSystem(uri)(fs => copyRecursively(fs.getPath("/" + name), target))
            target

    /**
     * Run `fn` against the zip filesystem behind a `jar:` URI. A filesystem we open is ours to close; one that was
     * already open belongs to someone else and is left alone.
     */
    private def withFileSystem[A](uri: URI)(fn: FileSystem => A): A =
        val opened =
            try Some(FileSystems.newFileSystem(uri, ju.Collections.emptyMap[String, String]()))
            catch case _: FileSystemAlreadyExistsException => None
        opened match
            case Some(fs) => Using.resource(fs)(fn)
            case None     => fn(FileSystems.getFileSystem(uri))

    private def copyRecursively(source: Path, target: Path): Unit =
        Using.resource(Files.walk(source)): stream =>
            for src <- stream.iterator().asScala do
                // source and target are on different filesystems, hence the round trip through String
                val dest = target.resolve(source.relativize(src).toString)
                if Files.isDirectory(src) then Files.createDirectories(dest)
                else
                    Files.createDirectories(dest.getParent)
                    Files.copy(src, dest, StandardCopyOption.REPLACE_EXISTING)

    private def deleteRecursively(path: Path): Unit =
        if Files.exists(path) then
            // Files.walk is pre-order, so reversing lists children before their parents
            val paths = Using.resource(Files.walk(path))(_.iterator().asScala.toList)
            paths.reverse.foreach(Files.deleteIfExists)

