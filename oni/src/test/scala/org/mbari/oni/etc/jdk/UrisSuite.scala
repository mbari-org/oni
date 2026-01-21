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

import java.net.URI

class UrisSuite extends munit.FunSuite {

    test("filename") {
        val uri = URI.create("file:///tmp/foo.txt")
        assertEquals(Uris.filename(uri), "foo.txt")
    }

    test("encode") {
        val uri = new java.net.URI("http://www.mbari.org")
        assertEquals(Uris.encode(uri), "http%3A%2F%2Fwww.mbari.org")
    }
  
}
