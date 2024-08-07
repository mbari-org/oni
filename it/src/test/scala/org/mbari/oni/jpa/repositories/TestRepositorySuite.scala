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

import org.mbari.oni.jpa.repositories.TestRepository

import java.nio.file.{Files, Paths}
import org.mbari.oni.etc.circe.CirceCodecs.{*, given}


class TestRepositorySuite extends munit.FunSuite {

  test("read") {
    val url = getClass.getResource("/kb/kb-dump.json.zip")
    val path = Paths.get(url.toURI)
    assert(Files.exists(path))
    val opt = TestRepository.read(path);
    assert(opt.isDefined)
    val root = opt.get
    val tree = root.stringify
    Files.write(Paths.get("target/kb-dump.json"), tree.getBytes)
  }

}
