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

package org.mbari.oni.etc.jdk

import java.io.{BufferedInputStream, BufferedOutputStream}
import java.nio.file.{Files as JFiles, Path}
import scala.util.Using

object Files:

    /**
     * Concatenate files into a single file
     * @param files
     *   THE files to concatenate
     * @param output
     *   the output file
     */
    def concatenate(files: Seq[Path], output: Path): Unit =
        Using(BufferedOutputStream(JFiles.newOutputStream(output))) { out =>
            files.foreach { file =>
                Using(BufferedInputStream(JFiles.newInputStream(file))) { in =>
                    val buffer    = new Array[Byte](1024)
                    var bytesRead = in.read(buffer)
                    while bytesRead != -1 do
                        out.write(buffer, 0, bytesRead)
                        bytesRead = in.read(buffer)
                }
            }
        }
