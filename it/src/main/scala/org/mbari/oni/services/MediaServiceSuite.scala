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

package org.mbari.oni.services

import org.mbari.oni.domain.{Media, MediaCreate, MediaTypes, MediaUpdate}
import org.mbari.oni.etc.jdk.Strings
import org.mbari.oni.jdbc.FastPhylogenyService
import org.mbari.oni.jpa.DataInitializer

import java.net.URI

trait MediaServiceSuite extends DataInitializer with UserAuthMixin:

    lazy val fastPhylogenyService = FastPhylogenyService(entityManagerFactory)
    lazy val mediaService         = MediaService(entityManagerFactory, fastPhylogenyService)

    test("create") {
        val root        = init(2, 0)
        assert(root != null)
        val mediaCreate = MediaCreate(
            conceptName = root.getName,
            url = URI.create(s"http://www.mbari.org/${Strings.random(10)}.png").toURL,
            caption = Some(Strings.random(1000)),
            credit = Some(Strings.random(255)),
            mediaType = Some(MediaTypes.IMAGE.name),
            isPrimary = Some(true)
        )
        val attempt     = runWithUserAuth(user => mediaService.create(mediaCreate, user.username))

        attempt match
            case Left(e)      => fail(e.getMessage)
            case Right(media) =>
                assert(media.id.isDefined)
                assertEquals(mediaCreate.conceptName, media.conceptName.getOrElse(""))
                assertEquals(mediaCreate.url, media.url)
                assertEquals(mediaCreate.caption, media.caption)
                assertEquals(mediaCreate.credit, media.credit)
                val t = Media.resolveMimeType(mediaCreate.mediaType.getOrElse(""), media.url.toExternalForm)
                assertEquals(t, media.mimeType)
                assertEquals(mediaCreate.isPrimary.getOrElse(false), media.isPrimary)

    }

    test("update") {
        val root        = init(2, 0)
        assert(root != null)
        val mediaCreate = MediaCreate(
            conceptName = root.getName,
            url = URI.create(s"http://www.mbari.org/${Strings.random(10)}.png").toURL,
            caption = Some(Strings.random(1000)),
            credit = Some(Strings.random(255)),
            mediaType = Some(MediaTypes.IMAGE.name),
            isPrimary = Some(true)
        )
        val mediaUpdate = MediaUpdate(
            url = Some(URI.create(s"http://www.mbari.org/${Strings.random(10)}.png").toURL),
            caption = Some(Strings.random(1000)),
            credit = Some(Strings.random(255)),
            mediaType = Some(MediaTypes.IMAGE.name),
            isPrimary = Some(true)
        )

        val attempt = runWithUserAuth(user =>
            for
                m0 <- mediaService.create(mediaCreate, user.username)
                m1 <- mediaService.update(m0.id.getOrElse(0L), mediaUpdate, user.username)
            yield m1
        )

        attempt match
            case Left(e)      => fail(e.getMessage)
            case Right(media) =>
                assertEquals(mediaUpdate.url.orNull, media.url)
                assertEquals(mediaUpdate.caption, media.caption)
                assertEquals(mediaUpdate.credit, media.credit)
                val t = Media.resolveMimeType(mediaUpdate.mediaType.getOrElse(""), media.url.toExternalForm)
                assertEquals(t, media.mimeType)
                assertEquals(mediaUpdate.isPrimary.getOrElse(false), media.isPrimary)
    }

    test("delete") {
        val root        = init(2, 0)
        assert(root != null)
        val mediaCreate = MediaCreate(
            conceptName = root.getName,
            url = URI.create(s"http://www.mbari.org/${Strings.random(10)}.png").toURL,
            caption = Some(Strings.random(1000)),
            credit = Some(Strings.random(255)),
            mediaType = Some(MediaTypes.IMAGE.name),
            isPrimary = Some(true)
        )
        val attempt     = runWithUserAuth(user => mediaService.create(mediaCreate, user.username))

        attempt match
            case Left(e)      => fail(e.getMessage)
            case Right(media) =>
                val attempt = runWithUserAuth(user => mediaService.deleteById(media.id.getOrElse(0L), user.username))
                attempt match
                    case Left(e)  => fail(e.getMessage)
                    case Right(_) =>
                        mediaService.findById(media.id.getOrElse(0L)) match
                            case Left(e)    => fail(e.getMessage)
                            case Right(opt) => assert(opt.isEmpty)
    }

    test("findById") {
        val root        = init(2, 0)
        assert(root != null)
        val mediaCreate = MediaCreate(
            conceptName = root.getName,
            url = URI.create(s"http://www.mbari.org/${Strings.random(10)}.png").toURL,
            caption = Some(Strings.random(1000)),
            credit = Some(Strings.random(255)),
            mediaType = Some(MediaTypes.IMAGE.name),
            isPrimary = Some(true)
        )
        val attempt0    = runWithUserAuth(user => mediaService.create(mediaCreate, user.username))

        attempt0 match
            case Left(e)      => fail(e.getMessage)
            case Right(media) =>
                val attempt1 = runWithUserAuth(user => mediaService.findById(media.id.getOrElse(0L)))
                attempt1 match
                    case Left(e)    => fail(e.getMessage)
                    case Right(opt) =>
                        assert(opt.isDefined)
                        val m = opt.get
                        assertEquals(media.id, m.id)
                        assertEquals(media.url, m.url)
                        assertEquals(media.caption, m.caption)
                        assertEquals(media.credit, m.credit)
                        assertEquals(media.mimeType, m.mimeType)
                        assertEquals(media.isPrimary, m.isPrimary)
    }
