/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2024
 *
 * oni code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential.
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
