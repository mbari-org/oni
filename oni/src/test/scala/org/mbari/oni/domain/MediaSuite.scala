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

package org.mbari.oni.domain

import org.mbari.oni.jpa.entities.MediaEntity

class MediaSuite extends munit.FunSuite {

    test("resolveMimeType (image)") {
        val a = Media.resolveMimeType("image", "http://foo.com/bar.jpg")
        assertEquals(a, "image/jpg")

        val b = Media.resolveMimeType("IMAGE", "http://foo.com/bax/bar.PNG")
        assertEquals(b, "image/png")
    }

    test("resolveMimeType (video)") {
        val a = Media.resolveMimeType("video", "http://foo.com/bar.mov")
        assertEquals(a, "video/quicktime")

        val b = Media.resolveMimeType("VIDEO", "http://foo.com/bax/bar.mp4")
        assertEquals(b, "video/mp4")
    }

    test("media URL with space") {
        val mediaEntity = new MediaEntity()
        mediaEntity.setUrl("http://foo.com/bar bax.jpg")
        mediaEntity.setType("image/jpg")
        val media = Media.from(mediaEntity)
        assertEquals(media.url.toString, "http://foo.com/bar%20bax.jpg")
    }



}
