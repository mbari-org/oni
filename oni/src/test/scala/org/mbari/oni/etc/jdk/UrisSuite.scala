/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2024
 *
 * oni code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential.
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
