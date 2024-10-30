/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2024
 *
 * oni code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential.
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
